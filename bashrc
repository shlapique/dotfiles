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

export PROMPT_COMMAND="history -a; history -n"

# append to history file
shopt -s histappend

export PATH=$PATH:~/.scripts/
export BOOKMARKS=$HOME/.bookmarks
export EDITOR=/usr/bin/vim
export HISTSIZE=1000000
export HISTFILESIZE=4096000

# ignore common commands
export HISTIGNORE=":pwd:id:uptime:resize:ls:clear:history:htop:top:glances:"

# ignore spaces before commands an duplicate entries
export HISTCONTROL=ignoredups

#####
face="$(shuf -e -n 1 🐸 🐈 🌞 🧐 🛀 🐷 🐰 👞 👑 🌈 👷 🐠 🍥 🫠 🌜 🐭 🥸 🥰 🌴 🫴 🎈)"
PS1="${face}:\W> "
