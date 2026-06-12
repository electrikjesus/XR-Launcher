# XR Launcher

Spatial workspace launcher for Android XR glasses, headsets, and **3D desktop** on large screens (Android 14+). Your phone doubles as a **touchpad + motion controller** for the workspace.

## Status

Planning phase — **Phase 1 MVP in progress** (builds; manual device testing pending). See **[docs/PLAN.md](docs/PLAN.md)** for phases, tasks, rules, and Git workflow.

## Build

```bash
./gradlew build testDebugUnitTest
```

Install debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Development workflow

- One plan task per branch (`dev/phase1-1.3-…`) and per commit
- Validate with `./gradlew build` and unit tests before committing
- Tag stable milestones on `main` (e.g. `v0.1.0-phase1`)

## Modes

| Mode | When | Input |
|------|------|-------|
| **Tier 0 — Spatial desktop** | Large screen, no glasses | Mouse, touch, keyboard |
| **Tier 0c — Phone shell** | Phone only, no glasses | Touch; opens companion when controlling workspace |
| **Tier 1–3 — XR workspace** | Glasses or spatial headset | Phone touchpad + motion; optional head-mouse on glasses |
| **Companion** | Phone controlling remote workspace | Touchpad + motion pointer |

## Hardware focus

- RayNeo Air 4 Pro (wired display / projected glasses path)
- Tablets & large-screen Android (3D spatial desktop fallback)
- Phone as companion controller (touchpad + gyro pointer)
- Future Android XR headsets (Full Space spatial path)

## References

- [Android xr-codelabs](https://github.com/android/xr-codelabs)
- [Android xr-samples](https://github.com/android/xr-samples)
