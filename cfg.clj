(require '[babashka.process :refer [sh]]
         '[babashka.fs :as fs]
         '[babashka.process :refer [process]]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(def home (System/getenv "HOME"))

(defn get-cpu-vendor []
  (let [cpuinfo (sh {:out :string} "grep" "vendor_id" "/proc/cpuinfo")]
    (when-let [line (first (str/split-lines (:out cpuinfo)))]
      (second (str/split line #"\s+: ")))))

; FIXME
; too many str calls
(defn stir-up-dir
  "a map { key 'path', value 'HOME + '.' + path' } for every file in [dir]"
  [dir] 
  (into {} 
        (map (fn [x] 
               [(str x) (fs/path home (str "." (str x)))])
             (filter fs/regular-file? (fs/glob dir "**")))))

(defn link-dotfiles 
  "Idempotently links dotfiles"
  [links]
  (doseq [[src dest] links
          :let [parent (fs/parent dest)]]
    (fs/create-dirs parent)

    (cond
      (and (fs/sym-link? dest)
           (= (fs/read-link dest) src))
      (println "Already linked:" dest)

      (fs/exists? dest)
      (println "Conflict:" dest "-> remove")

      :else
      (do (fs/create-sym-link dest src)
          (println "Linked:" src "->" dest)))))

(defn same-file? [file1 file2]
  (zero? (:exit (sh {:throw false} "cmp" "-s" file1 file2))))

; FIXME
(defn push-system-config
  "Idempotently writes system config files.
   [cfg] is {'system/file' '/some/system/path/'}"
  ([cfg] (push-system-config cfg {}))
  ([cfg {:keys [force?] :or {force? false}}]
   (doseq [[src dest-dir] cfg
           :let [filename  (fs/file-name src)
                 dest-path (fs/path dest-dir filename)]]
     (cond 
       force? 
       (do (fs/copy src dest-path {:replace-existing true})
           (println "Forced overwrite:" dest-path))

       (not (fs/exists? dest-path))
       (do (fs/copy src dest-path)
           (println "Installed:" dest-path))
       
       (same-file? (str dest-path) src)
       (println "Already up to date:" dest-path)

       :else
       (println "Conflict:" dest-path
                "-> use {:force true} to overwrite")))))

(defn purge-system-config 
  "Purges system config files."
  [cfg]
  (doseq [[src dest-dir] cfg
          :let [filename  (fs/file-name src)
                dest-path (fs/path dest-dir filename)]]
    (if (fs/exists? dest-path)
      (fs/delete dest-path)
      (println "Unable to delete file:" dest-path
               "file doesnt exist"))))

(defn enable-service
  ([service] (enable-service service {}))
  ([service {:keys [now?] :or {now? false}}]
  (sh "sudo" "systemctl" "enable" service)
  (when now? 
    (if (zero? (:exit (sh "sudo" "systemctl" "start" service)))
      (println "Service " service " started")
      (println "Unable to start" service)))
  (println "Enabled:" service)))

(defn disable-service 
  ([service] (disable-service service {}))
  ([service {:keys [stop?] :or {stop? false}}]
  (sh "sudo" "systemctl" "disable" service)
  (when stop?
    (if (zero? (:exit (sh "sudo" "systemctl" "stop" service)))
      (println "Service " service " stopped")
      (println "Unable to stop " service)))
  (println "Disabled:" service)))

(defn user-in-group? [user group]
  (let [{:keys [out]} 
        (sh {:out :string} "groups" user)]
    (some #(= % group) (str/split (str/trim out) #"\s+"))))

(defn add-user-to-group [user group]
  (let [{:keys [exit out err]} 
        (sh {:throw false} "sudo" "usermod" "-aG" group user)]
    (if (zero? exit)
      (println "User added to" group " group successfully.")
      (println "Failed to add user to" group " group:" err))))

; (defn install-packages [pkgs]

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
                        {"bashrc" (fs/path home ".bashrc")
                         "bash_profile" (fs/path home ".bash_profile")
                         "xinitrc" (fs/path home ".xinitrc")
                         "Xresources" (fs/path home ".Xresources")
                         "vimrc" (fs/path home ".vimrc")
                         "alias" (fs/path home ".alias")
                         "scripts" (fs/path home ".scripts")}
                        (stir-up-dir "local")
                        (stir-up-dir "config"))

  :system-config-map {"system/00-keyboard.conf" "/etc/X11/xorg.conf.d"
                      (if (= (get-cpu-vendor) "GenuineIntel") 
                                               "system/20-intel.conf"
                                               "system/20-amd.conf") "/etc/X11/xorg.conf.d"}})

(def steps
  [{:name "Push system X11 config"
    :do   #(push-system-config (:system-config-map config))
    :undo #()}])

; (push-system-config (:system-config-map config))
