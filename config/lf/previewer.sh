#!/bin/sh
draw() {
  ~/.config/lf/draw_img.sh "$@"
  exit 1
}

hash() {
  printf '%s/.cache/lf/%s' "$HOME" \
    "$(stat --printf '%n\0%i\0%F\0%s\0%W\0%Y' -- "$(readlink -f "$1")" | sha256sum | awk '{print $1}')"
}

cache() {
  if [ -f "$1" ]; then
    draw "$@"
  fi
}

file="$1"
shift

if [ -n "$FIFO_UEBERZUG" ]; then
  case "$(file -Lb --mime-type -- "$file")" in
    image/vnd.djvu)
      cache="$(hash "$file").jpg"
      cache "$cache" "$@"
      ddjvu -format=pnm -page=1 -size="$(djvused "$file" -e 'select 1; size' | awk '{split($1, arr, "="); split($2, arr2, "="); printf "%sx%s", arr[2], arr2[2]}')" "$file" | convert pnm:- "$cache"
      draw "$cache" "$@"
      ;;
    image/*)
      orientation="$(identify -format '%[EXIF:Orientation]\n' -- "$file")"
      if [ -n "$orientation" ] && [ "$orientation" != 1 ]; then
        cache="$(hash "$file").jpg"
        cache "$cache" "$@"
        convert -- "$file" -auto-orient "$cache"
        draw "$cache" "$@"
      else
        draw "$file" "$@"
      fi
      ;;
    video/*)
      cache="$(hash "$file").jpg"
      cache "$cache" "$@"
      ffmpegthumbnailer -i "$file" -o "$cache" -s 0
      draw "$cache" "$@"
      ;;
    application/pdf)
      cache="$(hash "$file").jpg"
      cache "$cache" "$@"
      gs -o "$cache" -sDEVICE=pngalpha -dLastPage=1 "$file" >/dev/null
      draw "$cache" "$@"
      ;;
    audio/*)
      mediainfo "$file" || exit 1 
      ;;
    text/* | application/json | application/x-ndjson)
      # bat --terminal-width $(stty -a | grep -oE 'columns.[0-9]+' | cut -d\  -f2) -f "$file"
      bat -f "$file"
  esac
fi

file -Lb -- "$1" | fold -s -w "$width"
exit 0
