# 🐰 My dotfiles :)

## When booted

### Install babashka

```
sudo bash < <(curl -s https://raw.githubusercontent.com/babashka/babashka/master/install)
```

### then clean some suse defaults

to avoid conflicts with tlp

```
sudo zypper rm --clean-deps tuned
```

to avoid conflicts with my x11 configs:

```
sudo rm /etc/X11/xorg.conf.d/00-keyboard.conf
```

## Then

```
sudo zypper in git
```

clone this repo

```
git clone https://github.com/shlapique/dotfiles.git
```

```
cd dotfiles
```

and then RUN!

```
bb cfg.clj all
```
