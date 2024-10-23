# Sample .bashrc for SUSE Linux
# Copyright (c) SUSE Software Solutions Germany GmbH

# There are 3 different types of shells in bash: the login shell, normal shell
# and interactive shell. Login shells read ~/.profile and interactive shells
# read ~/.bashrc; in our setup, /etc/profile sources ~/.bashrc - thus all
# settings made here will also take effect in a login shell.
#
# NOTE: It is recommended to make language settings in ~/.profile rather than
# here, since multilingual X sessions would not work properly if LANG is over-
# ridden in every subshell.

test -s ~/.alias && . ~/.alias || true
test -s ~/.keys && . ~/.keys || true

# set bg
. ~/.fehbg

export PROMPT_COMMAND="history -a; history -n"

# append to history file
shopt -s histappend

export PATH=$PATH:$HOME/.scripts/:$HOME/.local/bin
export BOOKMARKS=$HOME/.bookmarks
export EDITOR=/usr/bin/vim

export XDG_CONFIG_HOME="$HOME/.config"

export QT_QPA_PLATFORMTHEME=qt5ct

export HISTSIZE=-1
export HISTFILESIZE=-1

# ignore common commands
export HISTIGNORE=":pwd:id:uptime:resize:ls:l:clear:history:htop:top:glances:"

# ignore spaces before commands an duplicate entries
export HISTCONTROL=ignoredups

#####
face="$(shuf -e -n 1 🐸 🐈 🌞 🧐 🛀 🐷 🐰 👞 👑 🌈 👷 🐠 🍥 🫠 🌜 🐭 🥸 🥰 🌴 🫴 🎈)"
PS1="${face}:\W> "
