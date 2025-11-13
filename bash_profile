# bash_profile

test -z "$PROFILEREAD" && . /etc/profile || true

umask 022

[[ -f ~/.bashrc ]] && . ~/.bashrc
export PATH="$PATH:$HOME/bin"
[[ $(/usr/bin/tty) == '/dev/tty1' ]] && startx
