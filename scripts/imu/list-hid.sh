#!/usr/bin/env bash
# List USB devices that match known AR-glasses vendor IDs (RayNeo / XReal / Viture).
set -euo pipefail

KNOWN=(
  "1bbb:RayNeo"
  "3318:XReal_or_Nreal"
  "35ca:Viture"
)

if ! command -v lsusb >/dev/null 2>&1; then
  echo "lsusb not found — install usbutils" >&2
  exit 1
fi

echo "Known AR-glasses vendors on this bus:"
echo
found=0
while IFS= read -r line; do
  for entry in "${KNOWN[@]}"; do
    vid="${entry%%:*}"
    name="${entry#*:}"
    if echo "$line" | grep -qi "ID ${vid}:"; then
      echo "[$name] $line"
      found=1
    fi
  done
done < <(lsusb)

if [[ "$found" -eq 0 ]]; then
  echo "(none of the known VIDs present)"
  echo
  echo "All USB devices:"
  lsusb
fi

echo
echo "HID raw nodes (if any):"
if compgen -G "/dev/hidraw*" >/dev/null 2>&1; then
  for node in /dev/hidraw*; do
    info=""
    if [[ -r "/sys/class/hidraw/${node##*/}/device/uevent" ]]; then
      info=$(tr '\n' ' ' <"/sys/class/hidraw/${node##*/}/device/uevent" | head -c 200)
    fi
    echo "  $node  $info"
  done
else
  echo "  (no /dev/hidraw*)"
fi
