# Glasses IMU / accelerometer notes

How XR Launcher (and Linux tools) talk to USB-C AR glasses IMUs. **Only RayNeo is
implemented in the Android app today.** Other vendors are documented so contributors
can capture traffic and evaluate an in-app port later.

Also see [DEVELOPER.md — Head tracking](DEVELOPER.md#head-tracking-rayneo-usb) and
[`scripts/imu/`](../scripts/imu/).

## Vendor summary

| Vendor | VID (decimal / hex) | Typical PID notes | Open sources | In XR Launcher |
|--------|---------------------|-------------------|--------------|----------------|
| **RayNeo** | `7099` / `0x1BBB` | `44880` / `0xAF50` (Air / SmartGlasses Desktop) | [verncat/RayNeo-Air-3S-Pro-OpenVR](https://github.com/verncat/RayNeo-Air-3S-Pro-OpenVR) | **Shipped** — `RayNeoImuProtocol` |
| **XReal / Nreal Air** | often `0x3318` (confirm with `lsusb`) | Air / Air 2 / Air 2 Pro / Ultra — IMU on a dedicated HID interface (iface # varies by model) | [XRLinuxDriver](https://github.com/wheaney/XRLinuxDriver), [nrealAirLinuxDriver](https://gitlab.com/wheaney/nrealAirLinuxDriver), [ar-drivers-rs](https://github.com/badicsalex/ar-drivers-rs), Monado `xreal_air` | Docs + Linux scripts only |
| **Viture** | `0x35CA` | Many PIDs; validate with vendor SDK helper | Official [Glasses SDK](https://www.viture.com/developer/glasses-sdk/glasses); RE Pro path in [viture_virtual_display](https://github.com/mgschwan/viture_virtual_display) | Docs + Linux scripts only |

Play / in-app ports need a **stable open protocol**, Android USB host permission UX, and a
row in [device-matrix.md](device-matrix.md). Closed SDKs (Viture `.so` / `.dll`) are fine
for Linux research but awkward for a Play-distributed launcher.

## RayNeo (shipped)

Outbound HID frame (64 bytes):

- `[0] = 0x66`
- `[1] = command` — `1` IMU on, `2` IMU off
- rest zero / payload

Inbound sample (`0x99`, type `0x65`): little-endian floats for accel XYZ, gyro XYZ (°/s),
temperature, tick — see `RayNeoImuProtocol.parseInboundFrame`.

Android: `UsbManager` + HID interface class 3; companion sets
`GlassesXrInputMode.GLASSES_HEAD_TRACKING` to start the reader.

## XReal / Nreal Air (research)

Community drivers treat IMU and MCU as **separate HID interfaces**. Air / Air 2 / Air 2 Pro
often use IMU interface **3** and MCU **4**; Air 2 Ultra differs (check ar-drivers-rs /
XRLinuxDriver tables).

Common control: start IMU stream with message id **`0x19`** and data **`0x01`** (Monado /
ar-drivers-rs). Packets are versioned; decode with the open parsers above rather than
guessing field offsets from a single capture.

## Viture (research)

All current Viture glasses share VID **`0x35CA`**. The official SDK opens IMU via
`xr_device_provider_open_imu` (pose or raw modes). Reverse-engineered Pro HID exists in
some open projects but is model-limited — prefer the SDK for multi-SKU Linux tools, and
do **not** bundle closed binaries in this repo.

## Capturing descriptors

**Android (device with glasses plugged in):**

```bash
adb shell dumpsys usb
# Look for vendor-id / product-id and interface class 3 (HID)
```

**Linux:**

```bash
lsusb
lsusb -v -d 1bbb:af50   # example RayNeo
./scripts/imu/list-hid.sh
```

## What “good” samples look like

While nodding the head slowly:

- Gyro Y/X (depending on axis map) should swing away from ~0 and return.
- Accel should stay near gravity (~9.8 m/s² or device-native scale) with tilt changes.
- Rate typically tens to hundreds of Hz; large gaps or stuck zeros mean wrong interface
  or IMU not enabled.

## Adding a vendor to the app later

1. Capture with `scripts/imu/dump-hidraw.py` (and Linux references).
2. Port a pure Kotlin parser next to `RayNeoImuProtocol` (no closed `.so` in the Play APK).
3. Gate behind USB VID/PID detection; keep RayNeo as default.
4. Update this doc + `device-matrix.md` + README Credits.
