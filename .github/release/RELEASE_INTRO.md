Spatial workspace launcher for Android XR glasses, headsets, and **3D desktop** on large screens (Android 14+). Your phone doubles as a **touchpad + motion controller** for the workspace.

**0.1.34** — Companion multitouch (scroll / pinch-zoom / look), tray brightness + notification scroll handle, world-locked Recents, wallpaper / HDRI surrounds, host HUD look modes.

### Highlights

- Phone HOME shell plus companion touchpad / motion pointer for the glasses display
- 2-/3-finger companion gestures: scroll, sphere zoom, look pan, right-click tap
- 3D Home Space on glasses, tablets, and desktop-mode hosts (BumpDesk GLES)
- Tray QS + notifications with pointer-friendly dismiss / scrub controls
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
