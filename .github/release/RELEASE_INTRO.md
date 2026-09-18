Spatial workspace launcher for Android XR glasses, headsets, and **3D desktop** on large screens (Android 14+). Your phone doubles as a **touchpad + motion controller** for the workspace.

**Early development** (`0.1.0`). Primary hardware: RayNeo Air 4 Pro and large-screen Android.

### Highlights

- Phone HOME shell plus companion touchpad / motion pointer for the glasses display
- 3D spatial workspace on glasses, tablets, and desktop-mode hosts
- Optional RayNeo USB head tracking with calibration
- Registers as `LAUNCHER` / `HOME` (minSdk 34)

## Install

1. Download **`app-release.apk`** below.
2. Install on an Android 14+ phone (or tablet). Connect glasses if you have them.
3. Optionally set **XR Launcher** as the default Home app.

```bash
adb install -r app-release.apk
```

Enable **Settings → Accessibility → XR Launcher display pointer** so the companion cursor can click on the glasses display.
