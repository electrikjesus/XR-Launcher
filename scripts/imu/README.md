# Glasses IMU probe scripts (Linux)

Host-side helpers to list known AR-glasses HID devices and dump IMU-ish traffic.
These do **not** install anything into the Android app.

## Dependencies

```bash
# Debian/Ubuntu
sudo apt install python3-hid python3-usb usbutils

# Or: pip install hidapi
```

Prefer a udev rule so you do not need root. Example for RayNeo + Viture + common XReal:

```bash
# /etc/udev/rules.d/99-xr-glasses-imu.rules
SUBSYSTEM=="hidraw", ATTRS{idVendor}=="1bbb", MODE="0660", TAG+="uaccess"
SUBSYSTEM=="hidraw", ATTRS{idVendor}=="35ca", MODE="0660", TAG+="uaccess"
SUBSYSTEM=="hidraw", ATTRS{idVendor}=="3318", MODE="0660", TAG+="uaccess"
```

Then `sudo udevadm control --reload-rules && sudo udevadm trigger`.

## Usage

```bash
cd scripts/imu
./list-hid.sh
python3 dump-hidraw.py --vendor rayneo --seconds 3
python3 dump-hidraw.py --vid 0x3318 --seconds 5 --hex   # XReal-ish; confirm VID with lsusb
```

See [docs/glasses-imu.md](../../docs/glasses-imu.md) for protocol notes and attribution.
