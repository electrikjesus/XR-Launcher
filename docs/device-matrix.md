# Device Matrix

Record hardware and software findings from Phase 0 testing. Update this file whenever you test a new phone, glasses firmware, or Android version.

## Template (copy per device combo)

### Device: `[Phone model]` + `[Glasses model]` — Android `[version]`

| Check | Result | Notes |
|-------|--------|-------|
| Glasses display ID(s) | | |
| 2D mode resolution | | |
| 3D mode resolution / layout | | SBS, ultra-wide split, etc. |
| `ProjectedContext.isProjectedDeviceConnected` | ☐ Yes / ☐ No | |
| `android.software.xr.api.spatial` | ☐ Yes / ☐ No | |
| Launch activity on glasses display | ☐ Yes / ☐ No | |
| OEM multi-window / freeform | ☐ Yes / ☐ No | |
| IMU via projected `SensorManager` | ☐ Yes / ☐ No | Sample rate, axes |
| Default HOME replaceable | ☐ Yes / ☐ No | |
| Tier 0 usable without glasses | ☐ Yes / ☐ No | 3D spatial desktop on tablet / DeX emulator |
| Phone companion (touchpad + motion) | ☐ Yes / ☐ No | Controls workspace on glasses or large screen |
| Window size class (no glasses) | Compact → 0c / Expanded → 0 | |
| Recommended tier (glasses) | 1 / 2 / 3 | |

**Quirks:**

-

**Test date:**

---

## Tested devices

### Device: `Pixel 8` + `RayNeo SmartGlasses (Desktop Mode)` — Android 15+

| Check | Result | Notes |
|-------|--------|-------|
| Glasses display ID(s) | **4** (was 3 in earlier session) | `EXTERNAL`, 1920×1080, name SmartGlasses |
| 2D mode resolution | 1920×1080 | |
| 3D mode resolution / layout | N/A (2D desktop path) | |
| `ProjectedContext.isProjectedDeviceConnected` | ☐ Not tested | Uses `EXTERNAL_DISPLAY` tier, not `XR_PROJECTED` |
| Launch activity on glasses display | ☑ Yes | `ExternalDisplayActivity` via `setLaunchDisplayId` |
| OEM multi-window / freeform | ☑ Yes | Desktop Mode; external task bounds ~1382×777 centered |
| IMU via projected `SensorManager` | N/A | Phone gyro used for motion pointer |
| Phone companion (touchpad + motion) | ☑ Partial | Dual launch OK; tap-click broken; motion OK; calibrate TBD |
| Recommended tier (glasses) | **1 — EXTERNAL_DISPLAY** | Not Tier 2 `PROJECTED_GLASSES` |

**Quirks:**

- `GlassesLauncherActivity` aborts (requires `XR_PROJECTED`); use `ExternalDisplayActivity`.
- Desktop Mode freeform window on external display despite fullscreen windowing mode label.
- Wi‑Fi `adb install` locks ADB server; use file transfer for APK.

**Test date:** 2026-06-11 / 2026-06-12
