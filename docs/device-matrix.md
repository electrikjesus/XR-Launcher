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

*(None yet — complete Phase 0.)*
