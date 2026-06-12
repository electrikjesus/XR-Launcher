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

## Phase 1.18 manual checklist (Pixel 8 + RayNeo)

Run after each glasses-session change; mark in commit or PR notes.

- [x] Open on glasses → companion on phone, workspace on secondary display
- [x] Enable accessibility pointer → overlay cursor on glasses
- [x] Drag touchpad → cursor moves only (no accidental click)
- [x] Double-tap → click on launcher app / Settings item
- [x] Double-tap, hold, drag → selection or drag in Settings
- [x] Hold Left + drag → same over third-party app
- [x] Right button on launcher → hotseat pin toggles
- [x] Launch app from launcher → pointer works over launched app
- [x] Show launcher on glasses → returns to workspace; clicks still work
- [ ] Motion calibrate → reach corners; recenter works
- [ ] Logcat `XRLauncher/Display` → note window bounds vs 1920×1080

---

## Tested devices

### Device: `Pixel 8` + `RayNeo SmartGlasses (Desktop Mode)` — Android 15+

| Check | Result | Notes |
|-------|--------|-------|
| Glasses display ID(s) | **4 / 10** (varies on reconnect) | `EXTERNAL`, 1920×1080, name SmartGlasses |
| 2D mode resolution | 1920×1080 | |
| 3D mode resolution / layout | N/A (2D desktop path) | |
| `ProjectedContext.isProjectedDeviceConnected` | ☐ Not tested | Tier 1 `EXTERNAL_DISPLAY`, not `XR_PROJECTED` |
| `android.software.xr.api.spatial` | ☑ No | `preferSubspaceShell=false` → flat 2.5D shell |
| Subspace forced on EXTERNAL (spike) | ☑ **No inner compose** | display 14: `outerComposed=true`, `innerComposed=false`; black + cursor only |
| Launch activity on glasses display | ☑ Yes | `ExternalDisplayActivity` via `setLaunchDisplayId` + launch bounds |
| OEM multi-window / freeform | ☑ Yes | Desktop Mode; `singleTask` + inject frame for click aim |
| IMU via projected `SensorManager` | N/A | Phone gyro for motion pointer |
| Phone companion (touchpad + motion) | ☑ Yes | Unified Desktop gestures; inject on launcher + apps |
| Accessibility pointer | ☑ Yes | Overlay cursor + inject; `LauncherInjectFrame` when launcher foreground |
| Launcher after app return | ☑ Yes | `singleTask` + Compose clickables; no empty hit-test map |
| Recommended tier (glasses) | **1 — EXTERNAL_DISPLAY** | Not Tier 2 `PROJECTED_GLASSES` |

**Quirks:**

- `GlassesLauncherActivity` requires `XR_PROJECTED`; use `ExternalDisplayActivity` on SmartGlasses.
- Display ID changes on glasses reconnect (e.g. 4 → 10); `resolveSecondaryDisplayId()` handles this.
- Desktop Mode may still report centered freeform bounds in `dumpsys` — `LauncherInjectFrame` maps inject coords to window.
- Wi‑Fi `adb install` locks ADB server; transfer APK via file-share app. `adb logcat` OK.

**Test date:** 2026-06-11 / 2026-06-12 (launcher inject fix validated)

---

## Phase 2.16 — AppWidgetHost feasibility (Pixel 8 + RayNeo)

| Check | Result | Notes |
|-------|--------|-------|
| Host requires Activity context | ☑ Yes | `ExternalDisplayActivity` on glasses display is valid host |
| Widget bind on EXTERNAL display | ☐ Device test pending | In-process bind may work when activity fills 1920×1080 |
| VirtualDisplay embed | ☑ Blocked | Desktop Mode replay loop — same as Phase 3 embed blocker |
| v1 recommendation | ☑ | Curated Compose widgets (clock/calendar); optional AppWidgetHost toggle later |

**Code reference:** `AppWidgetHostFeasibility.kt`

**Test date:** 2026-06-12 (code spike + unit tests)
