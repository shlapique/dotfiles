(require '[babashka.process :refer [shell]]
         '[babashka.fs :as fs]
         '[babashka.process :refer [process]]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(def home (System/getenv "HOME"))

(defn get-cpu-vendor []
  (let [cpuinfo (shell {:out :string} "grep" "vendor_id" "/proc/cpuinfo")]
    (when-let [line (first (str/split-lines (:out cpuinfo)))]
      (second (str/split line #"\s+: ")))))

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
  (map (fn [[k v]]
         (fs/create-dirs (fs/parent v)) 
         (if (or (fs/exists? v) (fs/sym-link? v))
           (println "Already exists:" v)
           (do 
             (fs/create-sym-link v k)
             (println "Linked ", k, " ", v)))) links))

(defn disable-service 
  ([service] (disable-service service {}))
  ([service {:keys [stop] :or {stop false}}]
  ; (shell "sudo" "systemctl" "mask" service)
  (shell "sudo" "systemctl" "disable" service)
  (when stop 
    (if (zero? (:exit (shell "sudo" "systemctl" "stop" service)))
      (println "Service " service " stopped successfully")
      (println "Unable to stop " service)))
  (println "Masked and disabled:" service)))

(defn user-in-group? [user group]
  (let [{:keys [out]} 
        (shell {:out :string} "groups" user)]
    (some #(= % group) (str/split (str/trim out) #"\s+"))))

(defn add-user-to-group [user group]
  (let [{:keys [exit out err]} 
        (shell {:throw false} "sudo" "usermod" "-aG" group user)]
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
                                               "system/20-amdconf") "/etc/X11/xorg.conf.d"}})

(def steps
  [{:name "Push system config"
    :do   #(map (fn [[k v]] (fs/copy k v)) (:system-config-map config))
    :undo #()}])
