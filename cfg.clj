(require '[babashka.process :refer [sh]]
         '[babashka.fs :as fs]
         '[babashka.process :refer [process]]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[clojure.edn :as edn])

(def home (System/getenv "HOME"))
(def user (System/getenv "USER"))
(def script-dir (fs/parent *file*))
(def state-file (fs/path home ".config/lawyer/state.edn"))

(defn read-state [file]
  (if (fs/exists? file)
    (edn/read-string (slurp (str file)))
    {}))

(defn write-state! [st file]
  (fs/create-dirs (fs/parent file))
  (spit (str file) (pr-str st)))

(defn get-cpu-vendor []
  (let [cpuinfo (sh {:out :string} "grep" "vendor_id" "/proc/cpuinfo")]
    (when-let [line (first (str/split-lines (:out cpuinfo)))]
      (second (str/split line #"\s+: ")))))

(defn stir-up-dir
  "Creates a map of {source-path -> dest-path} for all files in config-dir.
   Source paths are absolute, dest paths are in HOME with a dot prefix."
  [home config-dir-name]
  (let [config-dir (fs/path script-dir config-dir-name)]
    (when (fs/exists? config-dir)
      (into {}
            (for [file (filter fs/regular-file? (fs/glob config-dir "**"))
                  :let [rel-path (fs/relativize config-dir file)
                        abs-src (fs/absolutize file)
                        dest (fs/path home (str "." rel-path))]]
              [(str abs-src) dest])))))

(defn link-dotfiles 
  "Idempotently links dotfiles"
  [links]
  (doseq [[src dest] links
          :let [parent (fs/parent dest)
                abs-src (fs/absolutize src)]]
    (fs/create-dirs parent)

    (cond
      (and (fs/sym-link? dest)
           (= (str (fs/read-link dest)) (str abs-src)))
      (println "Already linked:" dest)

      (fs/exists? dest)
      (println "Conflict:" dest "-> exists but not a symlink to" src)

      :else
      (do (fs/create-sym-link dest abs-src)
          (println "Linked:" abs-src "->" dest)))))

(defn same-file? [file1 file2]
  (zero? (:exit (sh {:throw false} "cmp" "-s" (str file1) (str file2)))))

(defn do-push-system-config [cfg-map]
  (let [installed-paths
        (for [[src dest-dir] cfg-map
              :let [abs-src (fs/absolutize (fs/path script-dir src))
                    filename (fs/file-name abs-src)
                    dest-path (fs/path dest-dir filename)]]
          (cond
            (not (fs/exists? dest-path))
            (do (sh "sudo" "cp" (str abs-src) (str dest-path))
                (println "  Installed:" dest-path)
                (str dest-path))

            (same-file? dest-path abs-src)
            (do (println "  Already up to date:" dest-path)
                nil)

            :else
            (throw (Exception.
                    (str "Conflict at " dest-path " - file exists and differs")))))]
    (->> installed-paths (remove nil?) vec)))

(defn undo-system-config [paths]
  (doseq [p paths]
    (when (fs/exists? p)
      (println "  Removing system config:" p)
      (sh "sudo" "rm" (str p)))))

(defn enable-service
  ([service] (enable-service service {}))
  ([service {:keys [now?] :or {now? false}}]
   (sh "sudo" "systemctl" "enable" (str service))
   (when now? 
     (let [{:keys [exit]} (sh {:throw false} "sudo" "systemctl" "start" (str service))]
       (if (zero? exit)
         (println "Service" service "started")
         (println "Unable to start" service))))
   (println "Enabled:" service)))

(defn disable-service 
  ([service] (disable-service service {}))
  ([service {:keys [stop?] :or {stop? false}}]
   (sh "sudo" "systemctl" "disable" (str service))
   (when stop?
     (let [{:keys [exit]} (sh {:throw false} "sudo" "systemctl" "stop" (str service))]
       (if (zero? exit)
         (println "Service" service "stopped")
         (println "Unable to stop" service))))
   (println "Disabled:" service)))

(defn user-in-group? [user group]
  (let [{:keys [out]} 
        (sh {:out :string} "groups" user)]
    (some #(= % group) (str/split (str/trim out) #"\s+"))))

(defn remove-user-from-group [user group]
  (cond
    (user-in-group? user group)
    (let [{:keys [exit err]}
          (sh {:throw false} "sudo" "gpasswd" "-d" user group)]
      (if (zero? exit)
        (println "User removed from" group "successfully.")
        (println "Failed to remove user" user "from group" group ":" err)))

    :else
    (println "User" user "not in group" group "!")))

(defn do-add-user-to-group [user group]
  (if (user-in-group? user group)
    (do (println "User already in group.") false)
    (let [{:keys [exit err]}
          (sh {:throw false} "sudo" "usermod" "-aG" group user)]
      (if (zero? exit)
        (do (println "Added user to group.") true)
        (throw (Exception. err))))))

(defn undo-add-user-to-group [did-add? user group]
  (when did-add?
    (println "Removing user from group")
    (remove-user-from-group user group)))

(defn repo-exists? [title]
  (let [{:keys [out]} (sh {:throw false :out :string} "zypper" "lr")]
    (some (fn [line]
            (let [[_ alias name & _] (map str/trim (str/split line #"\|"))]
              (or (= alias title)
                  (= name title))))
          (str/split-lines out))))

(defn repo-enabled? [title]
  (let [{:keys [out]} (sh {:out :string :throw false} "zypper" "lr")]
    (some (fn [line]
            (let [[_ alias name enabled & _]
                  (map str/trim (str/split line #"\|"))]
              (when (or (= alias title) (= name title))
                (= enabled "Yes"))))
          (str/split-lines out))))

(defn enable-repo [title]
  (let [{:keys [exit err]}
        (sh {:throw false}
            "sudo" "zypper" "--non-interactive" "mr" "-e" title)]
    (if (zero? exit)
      (println "Enabled repo:" title)
      (println "Failed to enable repo:" err))))

(defn ensure-repo [title url]
  (cond
    (and (repo-exists? title) (repo-enabled? title))
    (println "Repo" title "already exists and is enabled.")

    (repo-exists? title)
    (do
      (println "Repo" title "exists but is disabled. Enabling...")
      (enable-repo title))

    :else
    (do 
      (println "Adding repo:" title)
      (let [{:keys [exit err]}
            (sh {:throw false}
                "sudo" "zypper" "--non-interactive" "ar" url title)]
        (if (zero? exit)
          (println "Added repo:" title)
          (println "Failed to add repo:" title ":" err))))))

(defn pkg-installed? [pkg]
  (let [{:keys [exit]}
        (sh {:throw false} "rpm" "-q" pkg)]
    (zero? exit)))

(defn do-install-pkgs [pkgs]
  (let [missing (remove pkg-installed? pkgs)]
    (cond
      (empty? missing)
      (do (println "  Nothing to install.") [])

      :else
      (let [{:keys [exit err]}
            (apply sh {:throw false}
                   "sudo" "zypper" "--non-interactive" "install" missing)]
        (if (zero? exit)
          (do (println "  Installed:" missing) missing)
          (throw (Exception. (str "Package install failed: " err))))))))

(defn undo-install-pkgs [pkgs]
  (when (seq pkgs)
    (println "  Removing:" pkgs)
    (let [{:keys [exit err]}
          (apply sh {:throw false}
                 "sudo" "zypper" "--non-interactive" "remove" "--clean-deps" pkgs)]
      (when-not (zero? exit)
        (println "  Warning: package removal failed:" err)))))

(defn run-step! [{:keys [title dof state-key]}]
  (println "Running step:" title)
  (let [state (read-state state-file)]
    (if (contains? state state-key)
      (println "   Already applied. Skipping.")
      (let [result (dof)]
        (write-state! (assoc state state-key result) state-file)
        (println "   Done.")))))

(defn undo-step! [{:keys [title undof state-key]}]
  (println "Undoing step:" title)
  (let [state (read-state state-file)]
    (if-let [stored (get state state-key)]
      (do
        (undof stored)
        (write-state! (dissoc state state-key) state-file)
        (println "   Reverted."))
      (println "   Not applied before. Skipping."))))

;; ===== CONFIG =====

(def config
  {:pkgs ["tlp"
          "atop"
          "pavucontrol-qt"
          "pipewire-pulseaudio"
          "pulseaudio-utils"
          "pipewire-tools"
          "i3"
          "i3status"
          "i3lock"
          "dmenu"
          "bc"
          "sakura"
          "xterm"
          "tmux"
          "gvim"
          "xrandr"
          "arandr"
          "mc"
          "opi"
          "chromium"
          "samba-client"
          "neofetch"
          "xwallpaper"
          "pcmanfm"
          "dunst"
          "libnotify-tools"
          "flatpak"
          "sysfsutils"
          "bluez"
          "blueman"
          "git"
          "xbacklight"
          "mtr"
          "openvpn"
          "wireguard-tools"
          "xf86-video-intel"
          "engrampa"
          "screengrab"
          "xss-lock"
          "noto-fonts"
          "intlfonts-japanese-bitmap-fonts"
          "iosevka-fonts"
          "google-noto-coloremoji-fonts"]

   :dotfiles-symlinks (merge 
                        {(str (fs/path script-dir "bashrc")) (fs/path home ".bashrc")
                         (str (fs/path script-dir "bash_profile")) (fs/path home ".bash_profile")
                         (str (fs/path script-dir "xinitrc")) (fs/path home ".xinitrc")
                         (str (fs/path script-dir "Xresources")) (fs/path home ".Xresources")
                         (str (fs/path script-dir "vimrc")) (fs/path home ".vimrc")
                         (str (fs/path script-dir "alias")) (fs/path home ".alias")
                         (str (fs/path script-dir "scripts")) (fs/path home ".scripts")}
                        (stir-up-dir home "local")
                        (stir-up-dir home "config"))

   :system-config-map {"system/00-keyboard.conf" "/etc/X11/xorg.conf.d"
                       (if (= (get-cpu-vendor) "GenuineIntel") 
                         "system/20-intel.conf"
                         "system/20-amd.conf") "/etc/X11/xorg.conf.d"}})

;; ===== STEPS =====

(def step-ensure-x11-repo
  {:title "Ensure X11 utilities repo"
   :state-key :repo-x11
   :dof #(do (ensure-repo "X11:Utilities" "https://download.opensuse.org/repositories/X11:/Utilities/openSUSE_Tumbleweed/")
             true)
   :undof (fn [_]
            (println "Cannot automatically undo repo add. Skipping."))})

(def step-install-packages
  {:title "Install packages"
   :state-key :installed-packages
   :dof #(do-install-pkgs (:pkgs config))
   :undof undo-install-pkgs})

(def step-system-config
  {:title "Push system X11 config"
   :state-key :system-config
   :dof #(do-push-system-config (:system-config-map config))
   :undof undo-system-config})

(def step-pipewire-enable
  {:title "Enable pipewire-pulse"
   :state-key :pipewire
   :dof #(do
          (enable-service "pipewire-pulse.service")
          (enable-service "pipewire.service")
          true)
   :undof (fn [_]
            (disable-service "pipewire-pulse.service")
            (disable-service "pipewire.service"))})

(def step-disable-display-manager
  {:title "Disable display-manager"
   :state-key :display-manager-disabled
   :dof #(do (disable-service "display-manager.service" {:stop? true}) true)
   :undof (fn [_]
            (enable-service "display-manager.service" {:now? false}))})

(def step-dotfiles
  {:title "Configure dotfiles"
   :state-key :dotfiles
   :dof #(do (link-dotfiles (:dotfiles-symlinks config)) true)
   :undof (fn [_]
            (println "Dotfile symlinks not automatically removed."))})

(def step-user-video-group
  {:title "Add user to video group"
   :state-key :video-group
   :dof #(do-add-user-to-group user "video")
   :undof #(undo-add-user-to-group % user "video")})

;; Define step order
(def step-order
  [:repo-x11
   :installed-packages
   :system-config
   :video-group
   :pipewire
   :display-manager-disabled
   :dotfiles])

(def step-registry
  {:repo-x11 step-ensure-x11-repo
   :installed-packages step-install-packages
   :system-config step-system-config
   :video-group step-user-video-group
   :pipewire step-pipewire-enable
   :display-manager-disabled step-disable-display-manager
   :dotfiles step-dotfiles})

(def step-name-to-key
  {"install-packages" :installed-packages
   "system-config" :system-config
   "video-group" :video-group
   "repo-x11" :repo-x11
   "pipewire" :pipewire
   "display-manager" :display-manager-disabled
   "dotfiles" :dotfiles})

(defn apply-all! []
  (doseq [step-key step-order]
    (when-let [step (get step-registry step-key)]
      (run-step! step))))

(defn undo-all! []
  (doseq [step-key (reverse step-order)]
    (when-let [step (get step-registry step-key)]
      (undo-step! step))))

(defn -main [& args]
  (when (empty? args)
    (println "Usage: bb cfg.clj [all|undo-all|<step-name>|undo <step-name>]")
    (println "Available steps:" (keys step-name-to-key))
    (System/exit 1))
  
  (let [cmd (first args)]
    (cond
      (= cmd "all")
      (apply-all!)

      (= cmd "undo-all")
      (undo-all!)

      (= cmd "undo")
      (if-let [step-key (get step-name-to-key (second args))]
        (undo-step! (get step-registry step-key))
        (do (println "Unknown step:" (second args))
            (println "Available steps:" (keys step-name-to-key))))

      (contains? step-name-to-key cmd)
      (run-step! (get step-registry (get step-name-to-key cmd)))

      :else
      (do (println "Unknown command:" cmd)
          (println "Available commands: all, undo-all, undo <step-name>")
          (println "Available steps:" (keys step-name-to-key))))))

(apply -main *command-line-args*)
