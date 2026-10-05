#!/usr/bin/env python3
"""Dump HID reports from AR glasses and optionally enable RayNeo / XReal IMU streams.

Examples:
  python3 dump-hidraw.py --vendor rayneo --seconds 3
  python3 dump-hidraw.py --vid 0x3318 --seconds 5 --hex
"""

from __future__ import annotations

import argparse
import struct
import sys
import time

try:
    import hid  # type: ignore
except ImportError:
    print("Missing dependency: pip install hidapi  (or apt install python3-hid)", file=sys.stderr)
    sys.exit(1)

# Decimal VID/PID used in XR Launcher RayNeoUsbConstants
RAYNEO_VID = 0x1BBB  # 7099
RAYNEO_PID = 0xAF50  # 44880
VITURE_VID = 0x35CA
# Common XReal / Nreal family (confirm with lsusb — some SKUs differ)
XREAL_VID = 0x3318

VENDOR_PRESETS = {
    "rayneo": (RAYNEO_VID, RAYNEO_PID),
    "viture": (VITURE_VID, None),
    "xreal": (XREAL_VID, None),
}


def parse_vid(text: str) -> int:
    return int(text, 0)


def rayneo_imu_on() -> bytes:
    frame = bytearray(64)
    frame[0] = 0x66
    frame[1] = 0x01  # CMD_IMU_ON
    return bytes(frame)


def rayneo_imu_off() -> bytes:
    frame = bytearray(64)
    frame[0] = 0x66
    frame[1] = 0x02  # CMD_IMU_OFF
    return bytes(frame)


def xreal_imu_on() -> bytes:
    # Monado / ar-drivers-rs style: msg 0x19 start IMU, data 0x01
    # Exact framing varies; this is a best-effort probe packet.
    return bytes([0xAA, 0x19, 0x01, 0x00])


def try_parse_rayneo(data: bytes) -> str | None:
    if len(data) < 32 or data[0] != 0x99 or data[1] != 0x65:
        return None
    accel = struct.unpack_from("<fff", data, 4)
    gyro = struct.unpack_from("<fff", data, 16)
    return (
        f"rayneo accel=({accel[0]:+.3f},{accel[1]:+.3f},{accel[2]:+.3f}) "
        f"gyro_dps=({gyro[0]:+.2f},{gyro[1]:+.2f},{gyro[2]:+.2f})"
    )


def open_device(vid: int, pid: int | None):
    devices = hid.enumerate(vid, pid if pid is not None else 0)
    if not devices:
        raise SystemExit(f"No HID device for vid=0x{vid:04x} pid={pid}")
    # Prefer an interface that looks like IMU (class 3); fall back to first.
    pick = devices[0]
    for d in devices:
        if d.get("interface_number") in (2, 3, 4):
            pick = d
            break
    print(
        f"Opening {pick.get('manufacturer_string')} {pick.get('product_string')} "
        f"vid=0x{pick['vendor_id']:04x} pid=0x{pick['product_id']:04x} "
        f"iface={pick.get('interface_number')} path={pick.get('path')}",
        file=sys.stderr,
    )
    dev = hid.device()
    dev.open_path(pick["path"])
    dev.set_nonblocking(True)
    return dev, pick


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--vendor", choices=sorted(VENDOR_PRESETS), help="Preset VID/PID")
    ap.add_argument("--vid", type=parse_vid, help="USB vendor id (e.g. 0x1bbb)")
    ap.add_argument("--pid", type=parse_vid, default=None, help="Optional product id")
    ap.add_argument("--seconds", type=float, default=3.0, help="Capture duration")
    ap.add_argument("--hex", action="store_true", help="Print raw hex instead of parsers")
    ap.add_argument("--no-enable", action="store_true", help="Do not send IMU-on command")
    args = ap.parse_args()

    if args.vendor:
        vid, pid = VENDOR_PRESETS[args.vendor]
        if args.pid is not None:
            pid = args.pid
    elif args.vid is not None:
        vid, pid = args.vid, args.pid
    else:
        ap.error("Provide --vendor or --vid")

    dev, meta = open_device(vid, pid)
    vendor_name = args.vendor or "custom"

    try:
        if not args.no_enable:
            if vendor_name == "rayneo" or vid == RAYNEO_VID:
                dev.write(rayneo_imu_on())
                print("Sent RayNeo IMU ON (0x66/1)", file=sys.stderr)
            elif vendor_name == "xreal" or vid == XREAL_VID:
                try:
                    dev.write(xreal_imu_on())
                    print("Sent XReal IMU ON probe (0xAA 0x19 0x01)", file=sys.stderr)
                except Exception as exc:  # noqa: BLE001
                    print(f"XReal enable write failed (try another iface): {exc}", file=sys.stderr)

        deadline = time.time() + args.seconds
        count = 0
        while time.time() < deadline:
            data = dev.read(64, timeout_ms=50)
            if not data:
                continue
            raw = bytes(data)
            count += 1
            if args.hex:
                print(raw.hex(" "))
                continue
            parsed = try_parse_rayneo(raw)
            if parsed:
                print(parsed)
            else:
                print(f"len={len(raw)} {raw[:16].hex(' ')}…")

        print(f"Done — {count} reports in {args.seconds}s", file=sys.stderr)
    finally:
        if vendor_name == "rayneo" or vid == RAYNEO_VID:
            try:
                if not args.no_enable:
                    dev.write(rayneo_imu_off())
            except Exception:  # noqa: BLE001
                pass
        dev.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
