#!/usr/bin/env sh
# Copies what the app runs of narsil-claude (its content scripts, styles and icon) into the app's assets, and notes
# the commit it came from. Usage: tools/sync-extension.sh [path to a narsil-claude checkout, ../narsil-claude by default]
set -eu
source_dir=${1:-../narsil-claude}
target=app/src/main/assets/extension
files="skin.css band.css lib/watch.js lib/mode.js mode.js lib/restore.js lib/state.js lib/live.js content.js icons/icon-192.png"
rm -rf "$target"
for file in $files; do
  mkdir -p "$target/$(dirname "$file")"
  cp "$source_dir/extension/$file" "$target/$file"
done
printf 'narsil-claude %s\n' "$(git -C "$source_dir" rev-parse HEAD)" > "$target/SOURCE"
echo "Copied narsil-claude $(git -C "$source_dir" rev-parse --short HEAD) into $target"
