# XR Launcher — Project Plan

A Play Store–friendly Android launcher that provides a **3D spatial workspace** on XR glasses, headsets, and large-screen devices — plus a compact phone HOME shell and **phone-as-controller** (touchpad + motion pointer) when driving a remote workspace. Targets Android 14–17 (API 34+). Primary hardware: RayNeo Air 4 Pro and future Android XR devices.

---

## Goals

- Act as a **HOME launcher** on all supported devices — phones, tablets, foldables, and desktop-mode hosts.
- Act as a **projected launcher** on connected glasses when XR output is available.
- Provide a **3D spatial workspace** on every tier — glasses, headsets, and **large-screen desktop fallback** — where panels can be arranged, moved, resized, and saved.
- Launch third-party apps **without system-level permissions** (no VirtualDisplay, Shell, or TaskView hacks).
- Support **capability tiers** so the same app works as everyday launcher, RayNeo display, and full Android XR spatial device.
- Use the **phone as a companion controller** — touchpad plus motion-controlled pointer — when driving a workspace on glasses or a large screen.

## Non-goals (v1)

- Embedding every Play Store app inside our panels (most apps do not opt in).
- Replacing the system compositor or requiring root / ADB tweaks.
- Unity / OpenXR runtime (see [xr-unity-samples](https://github.com/android/xr-unity-samples) for reference only).
- Compound-style virtual displays ([Compound](https://github.com/Xtr126/Compound)).

---

## Current direction (2026-09-18)

**XR-Launcher’s glasses Home Space is BumpDesk, remapped onto the inner Home Space sphere.** We are not borrowing a couple of visual tricks. We port the **majority of BumpDesk** (`/home/electrikjesus/AndroidStudioProjects/BumpDesk`) into this GLES engine and then build the XR workspace (Home / Tray panes, companion pointer, embed) on top of that.

BumpDesk already has the product: physical icons, All Apps drawer tile, piles, lasso, radial menu, widgets, physics/DND, `DeskRepository`, themes. Those stay the interaction model. The surface changes: **everything lives on the inner Home Space sphere** (BumpDesk’s floor/walls become that sphere wall).

**Panels are pinned BumpDesk widgets.** Home, Tray, and each app plane are the same kind of object as a BumpDesk `WidgetItem` with `isPinned = true`, posed on the sphere (the analog of pinning a widget to the floor or a wall). They are large textured boxes on the inner wall, not a separate Compose carousel and not a second renderer. Desktop icons, piles, tiles, and the All Apps drawer are the *unpinned / movable* items on that same surface. Empty sphere space is the desktop (drag, right/long-click, lasso). Looking left faces Desktop items the same way looking at Home faces the Home widget.

| BumpDesk piece | XR-Launcher Home Space |
|----------------|-------------------------|
| `BumpRenderer` + `CameraManager` | Already: `perspectiveM` + `setLookAtM` FPS camera; companion cursor mouse-looks |
| `RoomRenderer` | Surround wallpaper room |
| `Box` / `ItemRenderer` / `TextureUtils` | Icons, drawer tile, shortcuts as thin boxes on the sphere |
| `WidgetItem` (`isPinned`, wall/floor) | **Home / Tray / app planes** — large pinned widgets on the inner sphere wall |
| `APP_DRAWER` | Default Desktop item; click expands All Apps; pull apps onto the sphere |
| `Pile` + `PileRenderer` | Groups on the sphere (stack/grid/carousel) |
| `Lasso` + `LassoRenderer` | Multi-select on the sphere |
| `RadialMenuView` | Right/long-click empty space and items (replace 2D context menus over time) |
| `WidgetRenderer` | Live app-widget bitmaps; same path as pinned Home/Tray faces |
| `InteractionManager` + `PhysicsEngine` | Ray-pick, drag, bump, arrange; pinned widgets stay put until unpinned |
| `DeskRepository` | Persist desk + panel poses (yaw/pitch on the sphere instead of floor XYZ) |
| `UIRenderer` / `OverlayRenderer` | Close, pagination, Edit chrome — GLES when we get there |
| Edit → **Desktop page** | Toggle which BumpDesk types are live: icons, piles, tiles, widgets — do not crowd the Perspective page |

There is one scene graph. Do not keep a “panel renderer” and a “desktop renderer.” Compose is capture/texture source for a pinned widget until that widget’s contents are native GLES items.

**Build order**

1. **2.22** — GLES room + FPS camera + sphere-pinned widgets (today’s panes are the first pinned widgets).
2. **2.21** — Port BumpDesk desktop onto the same sphere: drawer tile, DND, piles, lasso, radial menu, live widgets, persist. Edit dialog page 2 for those types.
3. **2.20** — Recreate Home/Tray *contents* as child BumpDesk items on those pinned widgets.
4. **2.24** — Edit stays two pages: Perspective (panel/sphere/icon scale) vs Desktop (BumpDesk types). Do not dump both onto one card.
5. **2.23** — Keep-awake (partial).
6. **2.26–2.28** — Expanded host defaults into this same Home Space + top HUD (companion chrome + Settings); do not keep a second large-screen shell forever.

**Do not chase root, signature, or privileged system permissions.** Play-distributed builds stay on the normal launcher / accessibility path (HOME role + user-enabled display pointer). Do not add `MANAGE_EXTERNAL_STORAGE`, `READ_WALLPAPER_INTERNAL`, or other privileged wallpaper/storage APIs — use gradient fallbacks when the system wallpaper is unavailable.

**Large-screen host (planned 2.26–2.28):** On `WindowSizeClass.Expanded` with no glasses, the default HOME surface is this **same GLES Home Space**, not the legacy Compose `SpatialDesktopScreen` Subspace shell. Screen-locked HUD: companion-style icons along the **top** (input / mouse-look / recenter [/ keyboard] + Settings); Edit stays **bottom-end**. Compact phone remains Tier 0c (`PhoneShellScreen` / companion).

**Host input (BumpDesk absolute):** Expanded host Home Space uses **BumpDesk-derived absolute mouse/touch** (`HostBumpDeskInput` + `BumpDeskHostGesture`), not companion FPS center-lock. `HostInputMethod.BUMPDESK` (default) vs `COMPANION_BUS` (`HostPointerBridge`) for A/B. Glasses / phone companion keep `CompanionPointerBus` FPS semantics.

---

## Rules

These rules apply to all design and implementation decisions. When in doubt, follow the rule that keeps scope smaller and permissions lower.

### Product & permissions

1. **Play Store first.** Do not depend on signature, system, or root-only APIs. If a feature requires `CREATE_VIRTUAL_DEVICE`, Shell, `MANAGE_ACTIVITY_TASKS`, `MANAGE_EXTERNAL_STORAGE`, or other privileged wallpaper/storage APIs, it is out of scope for the main product path. Assume Play treats us as a launcher for HOME role and normal accessibility; never require root or OEM privileges.
2. **Launch by default, embed when possible.** Start apps with standard `Intent` + display targeting. Use `ActivityPanelEntity` / activity embedding only when the platform grants `EMBED_ACTIVITY` and the target app opts in.
3. **Zero dangerous permissions in v1.** No `QUERY_ALL_PACKAGES`. Discover apps via `ACTION_MAIN` + `CATEGORY_LAUNCHER`. Defer `PACKAGE_USAGE_STATS` to a later phase and disclose it clearly if added.
4. **Graceful degradation.** Every spatial feature must have a fallback. Never assume glasses or Full Space APIs exist. **No glasses on large screen → Tier 0 GLES Home Space (2.26).** **No glasses on phone → Tier 0c compact shell**, with optional companion mode.

### Architecture

5. **Capability tiers, not device hacks.** Branch on detected runtime tier (`SPATIAL_DESKTOP`, `PHONE_SHELL`, `EXTERNAL_DISPLAY`, `PROJECTED_GLASSES`, `FULL_SPATIAL`), not hard-coded RayNeo logic scattered through the codebase.
6. **One workspace model.** Panel `Pose`, size, focus, and app identity live in a single `Workspace` / `PanelState` model persisted in DataStore or Room. **All tiers** share the same 3D layout model (meters). Only the **shell UI** on compact phones (no external display) uses a flat app list — not the workspace itself.
7. **Single HOME entry, multiple surfaces.** One `MainActivity` holds the HOME role and routes to the correct UI mode. Glasses use a dedicated projected activity when connected. The phone can render a compact launcher **or** act as a **companion touchpad + motion controller** for a workspace on another display. Shared logic lives in `core/` modules.
8. **Black backgrounds on glasses only.** Display glasses use additive optics; projected activities use pure black (`Color.Black`). Tier 0 large-screen 3D workspace uses normal Material surfaces, optional environment backdrop, or passthrough-style neutral skybox — not flat 2D tiles.
9. **Auto mode switch.** When glasses connect, offer seamless transition to XR workspace (user prompt or setting). When glasses disconnect, return to Tier 0 without losing the HOME role or killing unrelated apps abruptly.
10. **Input matches the surface.** Large-screen Tier 0 is **3D navigated with mouse, touch, and keyboard** — not head tracking. Glasses tiers may use head-mouse (optional). The **phone companion** (touchpad + motion pointer) is a first-class input path whenever the workspace runs on glasses or a host large screen.

### UX & spatial design

11. **Comfort defaults (XR only).** Spawn primary content ~1.5 m away, slightly below eye level. Avoid placing critical UI behind the user or outside ~41° FOV (see [Spatial UI guidance](https://developer.android.com/design/ui/xr/guides/spatial-ui)).
12. **Head-mouse is glasses-only and optional.** IMU on the **glasses** drives head-mouse when enabled. Must be toggleable, recenterable, and sensitivity-tuned. Never use head tracking as the primary input on large-screen Tier 0.
13. **One focused panel.** Only one panel receives primary input at a time. Orbiters / title bars expose close, focus, resize, and “open as full window.”
14. **Persist what we own.** Save workspace layouts ourselves. Do not rely on system window pin persistence across reboots.

### Tier 0 (large-screen spatial launcher)

15. **Same Home Space as glasses, on the host display.** On `WindowSizeClass.Expanded` (tablet, unfolded foldable, DeX, Chromebook), Tier 0 is the **BumpDesk GLES Home Space** (task **2.26**), not a separate Compose `Subspace` / `SpatialDesktopScreen` shell. Navigation uses **mouse**, **touch**, and **keyboard**; companion actions also appear as a **top screen-locked HUD** (**2.27–2.28**). Edit stays bottom-end.
16. **Compact phone is the launcher shell + companion.** On phones without an external display, show a flat app drawer / search UI for everyday HOME duty. When glasses connect or the user opens “Control workspace,” the phone becomes a **companion controller** (touchpad + motion pointer) — not a scaled-down 3D desktop.
17. **Activity embedding on large screens.** Tier 0 may embed activities in spatial panels via [activity embedding](https://developer.android.com/develop/ui/views/layout/activity-embedding) or `ActivityPanelEntity` when APIs allow — same opt-in constraints as other tiers.

### Phone companion controller

18. **Touchpad mode.** Phone screen acts as a relative touchpad: drag moves the workspace cursor; tap = click; two-finger tap = right-click or context (if needed).
19. **Motion pointer mode.** Phone gyro/accelerometer drives cursor movement (air-mouse style), like RayNeo mouse control on Linux. Toggle between touchpad and motion; provide recenter and sensitivity settings.
20. **Companion pairs with remote workspace.** Companion input targets the focused panel on the **glasses display** or **large-screen Home Space**, sent over local connection (same app, two activities / display routing — no network permission required for same-device control). On the host (Tier 0), the same companion actions are also available as **top HUD** icons (**2.27–2.28**).

### Code & process

21. **Kotlin + Compose.** Match Android XR sample style ([xr-codelabs](https://github.com/android/xr-codelabs), [xr-samples](https://github.com/android/xr-samples)). Min SDK 34 unless a hard blocker appears.
22. **Minimal diffs.** One concern per change. No speculative abstractions; extract only after the second use.
23. **Log capability probes in debug.** Tier detection results, window size class, and spatial capability flags should be easy to inspect on device during Phase 0.
24. **Document device findings.** Update `docs/device-matrix.md` when testing a new phone or glasses firmware — display IDs, modes, API availability, OEM quirks.

### Workflow, Git & testing

25. **One task, one commit.** Work through [Phases & tasks](#phases--tasks) in order. Each numbered task (e.g. 1.3, 2.5) gets its own branch and its own commit — do not batch unrelated tasks.
26. **Validate before commit.** A task is not done until `./gradlew build` passes **and** any new or affected tests pass. Manual device/emulator checks required when the task touches UI, displays, or tier detection.
27. **Tests for every component.** New or changed code in `core/` and `ui/` ships with unit tests (JUnit + Robolectric or similar where needed). Add instrumented or Compose UI tests when behavior is visual or integration-heavy. No component merges without corresponding tests.
28. **Git branches and tags.** Use branches and tags to mark stable points, ongoing dev work, and releases (see [Git workflow](#git-workflow) below).
29. **Stable points are tagged.** When a phase exit criteria is met, tag the merge commit on `main` (e.g. `v0.1.0-phase1`).
30. **Keep this plan current.** After every landed task, update the matching row, [Current direction](#current-direction-2026-09-18), [Phase 2 — Next steps](#phase-2--next-steps-immediate), and the decision log. Do not leave PLAN.md describing a path we have already abandoned (e.g. Compose `graphicsLayer` as the FPS camera).

---

## Git workflow

### Branches

| Branch | Purpose |
|--------|---------|
| `main` | Stable, validated history only. Every merge passed full Gradle build + tests. |
| `dev/<phase>-<task>` | One branch per plan task, e.g. `dev/phase1-1.3-tier0-app-grid` |
| `dev/<feature>` | Optional umbrella only if a task is split across PRs; prefer one branch per task |

Flow for each task:

1. Branch from latest `main`: `git checkout -b dev/phase1-1.3-tier0-app-grid`
2. Implement the single task + tests
3. Run validation (see below)
4. Commit with message referencing task ID
5. Merge to `main` (or open PR if collaborating)
6. Delete the dev branch after merge

### Tags & versions

Use [Semantic Versioning](https://semver.org/) once public releases begin. Until then, phase tags mark stable milestones:

| Tag pattern | When |
|-------------|------|
| `v0.0.1-phase0` | Phase 0 exit criteria met |
| `v0.1.0-phase1` | Phase 1 exit criteria met |
| `v0.2.0-phase2` | … |
| `v1.0.0` | First Play Store release (Phase 6) |

Optional: lightweight tags on `main` after each phase; annotated tags for releases.

### Commit messages

Format:

```
<phase>.<task>: <short imperative summary>

Optional body: what was validated (gradle, unit tests, manual steps).
```

Examples:

```
1.3: Add Tier 0 expanded app grid on large screens

Validated: ./gradlew build testDebugUnitTest
Manual: app grid on tablet emulator (sw600dp)
```

```
0.1: Initialize Android project with Compose and minSdk 34

Validated: ./gradlew build
```

### Validation checklist (required before every commit)

Run from project root:

```bash
./gradlew build
./gradlew testDebugUnitTest
```

When the project has instrumented tests:

```bash
./gradlew connectedDebugAndroidTest   # or CI equivalent
```

For tasks that need device verification, note what you ran in the commit body (emulator API level, physical device, glasses connected, etc.).

### Testing conventions

| Layer | Location | Covers |
|-------|----------|--------|
| **Unit** | `module/src/test/` | `core/capability`, `core/workspace`, `core/launcher`, parsers, state logic |
| **UI / Compose** | `module/src/test/` or `androidTest/` | Tier routing, panel layout math, composable smoke tests |
| **Instrumented** | `module/src/androidTest/` | HOME intent, display launch, glasses connect flows (when hardware available) |

Rules:

- New file in `core/` → new `*Test.kt` in the same change (same task commit).
- Bug fix → regression test in the same commit when feasible.
- Task checklist in PLAN.md: mark **Done** only after tests exist and Gradle is green.

---

## Runtime tiers

| Tier | When | App window strategy | UI |
|------|------|---------------------|-----|
| **0 — Spatial desktop** | No glasses; large screen (`Expanded`) | Standard `startActivity` + optional embed | **GLES Home Space** on host (same as glasses; **2.26**) — mouse, touch, keyboard + top HUD (**2.27–2.28**) |
| **0c — Phone shell** | No glasses; compact phone only | Standard launch from flat app drawer | Compact 2D HOME + **companion controller** when paired to workspace |
| **1 — External display** | Glasses appear as USB-C monitor; no XR APIs | `ActivityOptions.setLaunchDisplayId()` + OEM multi-window if available | Custom Compose shell on glasses display |
| **2 — Projected glasses** | `ProjectedContext`, `XR_PROJECTED` display category | Projected activity + launch to glasses display | Jetpack Compose **Glimmer** |
| **3 — Full spatial** | `android.software.xr.api.spatial`, Full Space | `ActivityPanelEntity` + Home Space fallback | Jetpack **Compose for XR** + **SceneCore** |

### Mode selection

Tier is chosen at runtime whenever HOME is shown or glasses topology changes:

```
if (glassesConnected && hasProjectedDisplay)     → Tier 2 (or 3 if spatial APIs present)
else if (glassesConnected && externalDisplayOnly) → Tier 1
else if (spatialHeadset && no separate glasses)   → Tier 3
else if (windowSizeClass == Expanded)            → Tier 0 (GLES Home Space on host — 2.26)
else                                              → Tier 0c (compact phone shell)
```

Within Tier 0 / 0c:

| Form factor | Workspace UI | Primary input |
|-------------|--------------|---------------|
| **Expanded** (tablet, unfolded foldable, DeX, Chromebook) | **GLES Home Space** (shared with glasses; retire `SpatialDesktopScreen` as default) + top HUD chrome | Mouse, touch, keyboard; optional phone companion |
| **Compact** (phone portrait, no external display) | Flat app drawer + search (HOME shell) | Direct touch on phone |
| **Phone as companion** (workspace on glasses or large screen) | Companion UI on phone; workspace on remote display | **Touchpad + motion pointer** on phone |

### Input by tier

| Tier | Pointer sources | Navigation / shortcuts |
|------|-----------------|------------------------|
| **0 — Spatial desktop** | Mouse, trackpad, touch, keyboard; top HUD for look/input/settings (**2.27–2.28**) | Orbit / mouse-look / panel focus; shortcuts for launch, close, snap |
| **0c — Phone shell** | Touch on phone | Standard launcher; button to open companion mode |
| **1–2 — Glasses** | Phone **touchpad + motion** (primary), optional glasses head-mouse | Companion buttons: recenter, back, keyboard toggle (same actions mirrored on host HUD when Tier 0) |
| **3 — Full spatial** | Hands/controllers (platform), phone companion (fallback) | Platform XR gestures + companion |

Detect tier at startup; re-evaluate on `DisplayManager` display changes, `ProjectedContext.isProjectedDeviceConnected` updates, and window size class changes.

---

## Module layout (target)

```
xrlauncher/
├── app/                    # Application entry, manifest, DI wiring
├── core/
│   ├── capability/         # Tier detection, SpatialCapability checks
│   ├── workspace/          # Workspace / PanelState, persistence, presets
│   ├── launcher/           # App resolution, launch intents, embed attempts
│   └── input/              # Pointer routing, keyboard shortcuts, companion touchpad + motion
├── ui/
│   ├── desktop/            # Tier 0 host routing (2.26: default into shared GLES Home Space; legacy SpatialDesktopScreen until retired)
│   ├── phone/              # Tier 0c compact HOME shell
│   ├── companion/          # Touchpad + motion controller (actions mirrored on host top HUD)
│   ├── glasses/            # GLES Home Space + Edit (bottom-end) + planned top HUD on host
│   ├── glimmer/            # Tier 2 glasses UI
│   ├── spatial/            # GLES backdrop / shared XR scene helpers; Tier 3 Subspace where needed
│   └── settings/           # SettingsActivity (companion + host HUD 2.28)
└── docs/
    ├── PLAN.md             # This file
    └── device-matrix.md    # Per-device test results
```

---

## Phases & tasks

Each task follows the [Git workflow](#git-workflow): one branch, tests included, `./gradlew build` green, then one commit. Mark **Done** only when validated.

### Phase 0 — Device lab

**Goal:** Know exactly what RayNeo + your phone support before building features.

**Exit criteria:** `docs/device-matrix.md` filled in; tier detection returns a real value on hardware.

| # | Task | Done |
|---|------|------|
| 0.1 | Initialize empty Android Studio project (Kotlin, Compose, minSdk 34, Git). Include `./gradlew build` + empty test source sets. | ☑ |
| 0.2 | Add Jetpack XR SDK + Glimmer dependencies; confirm build on API 34 emulator. | ☑ |
| 0.3 | Connect RayNeo; log all `DisplayManager` displays (id, name, size, modes). | ☐ |
| 0.4 | Document 2D vs 3D mode resolutions and aspect ratios. | ☐ |
| 0.5 | Test `ProjectedContext.isProjectedDeviceConnected` (pass/fail — record result). | ☐ |
| 0.6 | Test `PackageManager.hasSystemFeature("android.software.xr.api.spatial")`. | ☐ |
| 0.7 | Launch a blank activity on glasses via `setLaunchDisplayId`; confirm visibility. | ☐ |
| 0.8 | Sample IMU from projected context (or phone fallback): axes, rate, drift. | ☐ |
| 0.9 | Note OEM multi-window / freeform / DeX behavior on your phone. | ☐ |
| 0.10 | Test Tier 0: 3D spatial shell on tablet/emulator (sw ≥ 600dp); mouse/touch navigation smoke test. | ☐ |
| 0.11 | Write `docs/device-matrix.md` with findings and recommended lead tier. | ☐ |

---

### Phase 1 — MVP shell

**Goal:** Usable HOME on compact phone, **3D spatial workspace** on large screen (no glasses), app launch on glasses when connected, companion controller stub.

**Exit criteria:** HOME on phone; 3D workspace with spatial panels on tablet emulator; glasses launch works; companion touchpad sends pointer events.

| # | Task | Done |
|---|------|------|
| 1.1 | Create `MainActivity` with `HOME` + `LAUNCHER` intent filters (single HOME entry). | ☑ |
| 1.2 | Implement `CapabilityDetector`: tier enum, glasses-connected flow, `WindowSizeClass`. | ☑ |
| 1.3 | **Tier 0:** 3D spatial shell on Expanded — `Subspace` / spatial panels, empty workspace scene. | ☑ Superseded by **2.26** (GLES Home Space as Expanded default) |
| 1.4 | **Tier 0:** Mouse + touch navigation — orbit/pan workspace, click to focus panel (stub panels OK). | ☑ |
| 1.5 | **Tier 0c:** Compact phone layout — app drawer, search/filter, tap to launch on phone display. | ☑ |
| 1.6 | Implement app list via launcher intent query (no `QUERY_ALL_PACKAGES`). | ☑ |
| 1.7 | **Companion:** `CompanionControllerActivity` — touchpad surface, pointer events to workspace session. | ☑ |
| 1.8 | Create `GlassesLauncherActivity`: `requiredDisplayCategory=XR_PROJECTED`, black root. | ☑ |
| 1.9 | Register `XR_PROJECTED_LAUNCHER` on glasses activity; `HOME` stays on `MainActivity` only. | ☑ |
| 1.10 | Implement `AppLauncher.launchOnGlasses(component)` with display-id targeting. | ☑ |
| 1.11 | “Connect glasses” / “Open XR workspace” + “Use as controller” affordances on phone shell. | ☑ |
| 1.12 | Basic spatial app picker panel in glasses workspace (Tier 1/2). | ☑ |
| 1.13 | Mode switch: glasses connect → offer XR workspace; disconnect → return to Tier 0/0c. | ☑ |
| 1.14 | Manual test: 3D workspace on tablet + companion touchpad from phone + 5 app launches on glasses. | ☐ |

---

### Phase 1.5 — SmartGlasses / Desktop Mode hardening

**Goal:** Reliable glasses session on Pixel 8 + RayNeo SmartGlasses (`EXTERNAL` display, Desktop Mode): one window, unified companion pointer, spatial launcher shell.

**Phase 1 exit — open items**

| # | Task | Status |
|---|------|--------|
| 1.14 | Manual regression: tablet Tier 0 + glasses session + 5 app launches | ☐ User test (glasses ☑ 2026-06-12; tablet pending) |
| 1.17 | Fullscreen on Desktop Mode freeform (launch bounds + CLEAR_TOP workaround) | ☑ Partial — code + auto relaunch; Pixel may still letterbox |
| 1.18 | Re-test checklist + update `device-matrix.md` | ☑ Glasses session (2026-06-12); tablet Tier 0 pending |
| 1.20 | Same as 1.17 — immersive edge-to-edge on secondary display | ☑ Partial (merged with 1.17) |
| 1.22 | 3D XR desktop on glasses | ☑ Partial — see [1.22 roadmap](#task-122--3d-xr-desktop-on-glasses-not-flat-black-list) |

**Phase 1 exit — done**

| # | Task | Notes |
|---|------|-------|
| 1.15 | Tap / pointer click on companion | ☑ Unified Desktop gestures + hit-test on launcher |
| 1.16 | Motion calibrate + recenter | ☑ |
| 1.19 | Single glasses activity | ☑ |
| 1.21 | System cursor + inject over all apps | ☑ Inject-always + launcher window frame mapping |
| 1.22a | Visual launcher shell (wallpaper, grid, hotseat, clock) | ☑ |

**Device snapshot (Pixel 8 + RayNeo, 2026-06-12)**

| Check | Result |
|-------|--------|
| Dual launch | ☑ Companion on phone + workspace on secondary display |
| Cursor sync | ☑ Overlay cursor when accessibility enabled |
| Companion input | ☑ Move / double-tap click / double-tap-drag / Left hold-drag |
| Launcher clicks | ☑ Inject + Compose clickables; `LauncherInjectFrame` for freeform/fullscreen |
| Third-party apps | ☑ Gesture inject when launcher in background |
| Show launcher return | ☑ `singleTask` + `onNewIntent`; clicks work after return from apps |
| Motion pointer | ☑ Calibrate + recenter |
| Single window | ☑ No separate overlay activity |
| Freeform bounds | ☐ OEM may still center window — log `XRLauncher/Display` |
| 3D Subspace on EXTERNAL | ☑ **No** — spike: outer shell only; inner Subspace never composes without spatial API |

**Task index**

| # | Task | Done |
|---|------|------|
| 1.15 | Fix companion pointer / click delivery | ☑ |
| 1.16 | Motion pointer calibration + recenter | ☑ |
| 1.17 | External display immersive fullscreen | ☑ Partial |
| 1.18 | Re-test full glasses session; device matrix | ☑ Glasses (2026-06-12) |
| 1.19 | Single glasses activity | ☑ |
| 1.20 | Fullscreen immersive secondary display | ☑ Partial |
| 1.21 | System-style cursor on secondary display | ☑ |
| 1.22 | 3D XR desktop on glasses | ☐ Partial |

<details>
<summary>Task detail archive (1.15–1.21 implementation notes)</summary>

#### Task 1.15 — Fix tap-to-click (archive)

**Symptom:** In default touchpad mode, tap moves the cursor to the tap location on the touchpad but does not click (no app launch / no hover confirm on glasses).

**Likely root causes (from code review):**

1. **Gesture conflict (primary):** `CompanionTouchpadScreen` registers both `detectDragGestures` (with `onDragStart` → `setCursorPosition`) and `detectTapGestures` on the same surface. A tap is often consumed as a zero-distance drag — `onDragStart` repositions the cursor, but `detectTapGestures` never fires, so `CompanionPointerBus.click()` is never called.
2. **Semantic mismatch with plan rule 18:** Touchpad should be *relative* (drag moves cursor; tap = click at *current* cursor). Current tap handler *also* jumps cursor to touchpad-normalized coordinates, which maps touchpad aspect ratio onto the 16:9 glasses screen — confusing even when click works.
3. **Click delivery (secondary):** `CompanionPointerBus.clicks` is a `SharedFlow` with no replay. If the external display collector starts late, a click could be dropped (less likely during normal use, but worth hardening).

**Planned fix:**

| Step | Change | Files |
|------|--------|-------|
| 1 | Replace dual gesture detectors with a single `awaitEachGesture` handler: pointer down → wait for touch slop → if no slop exceeded, emit click; if slop exceeded, enter relative drag mode (delta only, **no** cursor jump on down). | `ui/companion/CompanionTouchpadScreen.kt` |
| 2 | Add `CompanionPointerBus.clickAt(x, y, button)` that sets cursor and emits click atomically (for optional “tap-to-move-and-click” mode later). | `core/input/CompanionPointerBus.kt` |
| 3 | Give `_clicks` replay = 1 or use a `Channel` so the glasses activity never misses the latest click during startup. | `core/input/CompanionPointerBus.kt` |
| 4 | Add debug logging (debug build only): log click emit + hit-test result on external display. | `ExternalDisplayWorkspaceScreen.kt` |
| 5 | Unit tests: click at normalized coords; gesture handler does not call move on tap. | `CompanionPointerBusTest.kt`, optional Compose UI test |

**Acceptance test:** With motion off, drag moves cursor relatively; single tap fires left-click without moving cursor; item under glasses cursor highlights then launches; Left/Right buttons still work.

---

#### Task 1.16 — Motion pointer calibration + recenter

**Symptom:** Motion mode direction is good, but pointer loses accuracy reaching screen corners (gyro integration drift / limited rotation range).

**Likely root causes:**

1. Raw gyro integration with fixed sensitivity — no zero reference; drift accumulates over time.
2. Linear sensitivity does not account for phone pose at session start (user may not hold phone level).
3. Cursor clamped to `[0,1]` — user runs out of “virtual room” before reaching corners.

**Planned fix:**

| Step | Change | Files |
|------|--------|-------|
| 1 | Add **Recenter** button on companion UI — resets cursor to center `(0.5, 0.5)`. | `CompanionTouchpadScreen.kt`, strings |
| 2 | Add **Calibrate** flow: on button press, sample gyro for ~500 ms while user holds neutral pose; store as bias offset subtracted in `MotionPointerController`. | `MotionPointerController.kt`, new `MotionCalibrationState` |
| 3 | Persist last calibration bias in `SharedPreferences` (optional, same session minimum). | `core/input/` |
| 4 | Auto-calibrate when motion mode is toggled on (quick zero — user holds phone aimed at screen). | `CompanionControllerActivity.kt` |
| 5 | Expose **sensitivity** slider (reuse constant from `CompanionPointerBus.MOTION_SENSITIVITY`). | companion UI + bus |
| 6 | Unit tests for bias subtraction and recenter. | tests |

**Acceptance test:** Enable motion → calibrate → reach all four corners without recenter; recenter restores center; sensitivity adjustable.

---

#### Task 1.17 — External display immersive fullscreen (Desktop Mode)

**Symptom:** Glasses display shows workspace in a desktop freeform window (~1382×777 centered on 1920×1080) instead of edge-to-edge immersive.

**Evidence (`dumpsys activity activities`, display #4):**

```
Task{… mode=fullscreen …}
mBounds=Rect(269, 113 - 1651, 890)
mLastNonFullscreenBounds=Rect(269, 113 - 1651, 890)
```

Desktop Mode on Pixel treats secondary-display activities as resizable freeform tasks even when windowing mode is “fullscreen”.

**Workaround (verified 2026-06-12):** Launch companion + workspace, then relaunch workspace with `FLAG_ACTIVITY_CLEAR_TOP` (“Show launcher on glasses”). `openGlassesSession()` now does this automatically after the initial launch.

**Planned fix (try in order; record results in `device-matrix.md`):**

| Step | Approach | Files |
|------|----------|-------|
| 1 | At launch: `ActivityOptions.setLaunchWindowingMode(WINDOWING_MODE_FULLSCREEN)` **and** `setLaunchBounds(Rect(0, 0, displayWidth, displayHeight))` using `DisplayManager` metrics for target display ID. | `DisplayLaunchHelper.kt` |
| 2 | Manifest: `android:resizeableActivity="false"`, glasses theme `@style/Theme.XRLauncher.Glasses` with `windowFullscreen` / no action bar. | `AndroidManifest.xml`, themes |
| 3 | In `ExternalDisplayActivity.onCreate` / `onResume`: `WindowCompat.setDecorFitsSystemWindows(false)`; `WindowInsetsController.hide(navigationBars|statusBars)`; `window.setLayout(MATCH_PARENT, MATCH_PARENT)`; re-apply on `onWindowFocusChanged`. | `ExternalDisplayActivity.kt` |
| 4 | If still freeform: post-create `WindowManager.LayoutParams` update — set width/height to display size, gravity top-left, flags `FLAG_LAYOUT_IN_SCREEN`. | `ExternalDisplayActivity.kt` |
| 5 | Log resulting bounds via debug tag `XRLauncher/Display` after layout for device matrix. | activity + helper |
| 6 | **Fallback (document only):** If Pixel Desktop Mode cannot be overridden without system permissions, document “user must maximize window” in device matrix and explore `Presentation` API as Phase 2 spike. | `device-matrix.md` |

**Acceptance test:** After “Open on glasses”, external activity bounds match full display (`0,0 – 1920,1080` or current mode resolution); no visible desktop window chrome; workspace background fills glasses view.

---

#### Task 1.18 — Re-test & device matrix

| Step | Action |
|------|--------|
| 1 | Manual test checklist: dual launch, relative drag, tap click, button click, motion calibrate + corners, fullscreen bounds. |
| 2 | Fill Pixel 8 + SmartGlasses row in `docs/device-matrix.md` (display ID, freeform behavior, what worked). |
| 3 | Mark 1.14 ☐ → ☑ when all pass. |

**Deploy note:** Do not use `adb install` over Wi‑Fi (locks ADB server on this setup). Build APK locally; transfer via file-share app.

---

#### Task 1.19 — Single glasses activity (no dual freeform windows)

**Symptom:** “Open on glasses” spawned **two** Desktop Mode freeform windows: transparent cursor overlay + black XR Launcher workspace.

**Root cause:** `ExternalCursorOverlayActivity` launched as a second task on the same display ID. Desktop Mode treats each activity as its own resizable window.

**Fix:** Draw the cursor inside `ExternalDisplayActivity` via `ExternalCursorDot` composable. Only one activity launches on the glasses display. Removed separate overlay activity.

**Follow-up (1.21):** When a third-party app covers the launcher, the in-activity cursor is hidden — a *system-level* cursor overlay (not a second Activity) is required for desktop-style control.

**Acceptance test:** “Open on glasses” creates exactly **one** window on SmartGlasses display.

---

#### Task 1.20 — Fullscreen immersive on Desktop secondary display

**Goal:** Glasses activity fills 1920×1080 with no freeform window frame or letterboxing.

**Approaches to try:** `ActivityOptions.setLaunchBounds(full display)`, `setLaunchWindowingMode(FULLSCREEN)` where API allows, `resizeableActivity=false`, immersive insets in activity, log bounds to device matrix. Document Pixel Desktop Mode limits if OS enforces freeform.

---

#### Task 1.21 — System-style cursor on secondary display

**Goal:** Phone companion drives a **real** pointer on the glasses display that works over Settings, Play Store, and other apps — not only XR Launcher’s Compose UI.

**Implementation (2026-06-12):** `DisplayPointerAccessibilityService` draws a `TYPE_ACCESSIBILITY_OVERLAY` cursor on the glasses display and injects clicks via `dispatchGesture` + `setDisplayId`. In-app `ExternalCursorDot` is hidden when the overlay is active.

**Unified input (2026-06-12):** One touchpad gesture set when accessibility is enabled (move-only drag, double-tap click, double-tap-hold-drag, holdable Left). Launcher/Desktop mode toggle removed from companion UI. Fallback: tap-to-click when accessibility is off.

**Launcher inject fix (2026-06-12):** Left clicks always inject when accessibility is on; Compose `clickable` on icon cells handles launch. `LauncherInjectFrame` maps normalized cursor to launcher window pixels (fixes freeform offset). `ExternalDisplayActivity` uses `singleTask` + `onNewIntent` so “Show launcher on glasses” reuses the instance. Right-click still hit-tests for hotseat pin.

**Constraints (Play Store):** No `InputManager.injectInputEvent` (system). Freeform app windows may offset click coordinates — `LauncherInjectFrame` + fullscreen relaunch mitigate.

**Companion UX:** Enable accessibility service once; same gestures on launcher and third-party apps.

---

#### Task 1.22 — 3D XR desktop on glasses (not flat black list)

**Goal:** Glasses show spatial workspace (panels, depth, optional environment) per product vision — not a 2D text app list on black.

**Current state (2026-06-12):** Tier 1 default = flat `GlassesSpatialWorkspaceScreen` with **cursor parallax** (2.5D depth). When `android.software.xr.api.spatial` is present, `GlassesSessionState.preferSubspaceShell` selects `GlassesWorkspaceScreen` (`Subspace` + movable panel). Shared `LauncherWorkspacePointerEffects` handles companion hit-testing for both shells.

**Phased delivery:**

| Step | Deliverable | Tier | Status |
|------|-------------|------|--------|
| **1.22a** | Visual launcher shell — wallpaper, icons, hotseat, widget stub; companion hit-test | 1 EXTERNAL | ☑ |
| **1.22b** | `Subspace` shell wired on external display when spatial API present | 1 / 2 | ☑ Partial — needs device test on Tier 2 hardware |
| **1.22b′** | 2.5D parallax wallpaper + content layer on flat Tier 1 path | 1 EXTERNAL | ☑ |
| **1.22c** | Virtual environment / passthrough backdrop (Phase 5.5) | 2 / 3 | ☐ → Phase 5 |
| **1.22d** | Real widgets (AppWidgetHost or curated composables) | 2 | ☐ → Phase 2.15 |
| **1.22e** | User-pinned hotseat + workspace persistence | 2 | ☑ (Phase 2.1–2.2, 2.13) |
| **1.22f** | Movable/resizable spatial panels; orbit camera via companion on glasses | 0 / 3 | ☐ → Phase 2.5–2.8 |

**Constraints:** SmartGlasses = `EXTERNAL` display, not `XR_PROJECTED` — `Subspace` may be limited; keep 2.5D fallback. Additive optics → dark wallpaper, high-contrast icons (rule 14).

**Depends on:** Phase 2 workspace layout + Phase 5 spatial polish for full vision.

</details>

---

### Phase 2 — Workspace layout

**Goal:** Multiple launcher-owned panels with layout persistence — turn the glasses shell into a **real launcher** (wallpaper, dock/hotseat, app drawer, widgets, panel chrome).

**Entry (2026-06-12):** Tier 1 external display navigation works — Desktop overlay cursor + gesture inject on launcher and third-party apps; fullscreen relaunch on session open. Phase 1.22a visual shell is the starting layout.

**Exit criteria:** User can arrange 3+ panels, resize/move them, save and restore one workspace; hotseat pins persist; at least one live widget on glasses.

**Lead platform order (RayNeo Tier 1 — do these first):**

| Order | Task | Why now |
|-------|------|---------|
| 1 | **2.1–2.2** | `Workspace` / `PanelState` model + DataStore — foundation for hotseat pins and layout |
| 2 | **2.13** | User-pinned hotseat (replace hard-coded Settings/Play Store list) | 
| 3 | **2.14** | App drawer panel polish — search/filter, lazy grid perf, app icon cache |
| 4 | **2.3 / 2.15** | Widget panel v1 — clock done; add weather or calendar composable |
| 5 | **2.4 / 2.11** | Panel focus ring + companion pointer targets focused panel |
| 6 | **2.5–2.7** | Move/resize panels + layout presets on `GlassesSpatialWorkspaceScreen` |
| 7 | **2.8** | Restore workspace on `ExternalDisplayActivity` start |
| 8 | **2.9–2.10** | Tier 0 spatial parity (parallel, not blocking glasses) |

| # | Task | Done |
|---|------|------|
| 2.1 | Define `Workspace`, `PanelState`, `PanelKind`, `EmbedMode` in `core/workspace`. | ☑ |
| 2.2 | Persist workspace JSON via DataStore (default workspace + hotseat pins). | ☑ |
| 2.3 | Render static panels: app drawer, clock/status, placeholder “empty slot.” | ☑ |
| 2.4 | Add panel focus model (mouse click / touch / companion tap / keyboard focus next). | ☑ |
| 2.5 | Implement move (drag) for panels — tier-appropriate API (`movable` modifier, Glimmer, or drag handles). | ☑ (freeform title drag) |
| 2.6 | Implement resize with min/max bounds and optional fixed aspect ratio. | ☑ (corner handle; min size) |
| 2.7 | Add layout presets: Single, Dual, Triptych (apply preset → update poses). | ☑ |
| 2.8 | Save on change; restore workspace on activity start (all tiers). | ☑ |
| 2.9 | **Tier 0:** Spatial app-drawer panel + dock orbiter in 3D scene (not flat grid). | ☑ |
| 2.10 | **Tier 0:** Keyboard shortcut map (focus panels, launch, close, snap preset) + help overlay. | ☑ |
| 2.11 | **Companion:** Wire touchpad pointer → focused panel on glasses / large-screen workspace. | ☑ |
| 2.12 | **Companion:** Motion pointer calibration flow (neutral hold → recenter). | ☑ (1.16) |
| 2.13 | **Tier 1:** User-pinned hotseat — long-press / companion right-click to pin; persist in DataStore. | ☑ |
| 2.14 | **Tier 1:** App drawer — search bar, alphabetical sections, icon lazy-load cache. | ☑ |
| 2.15 | **Tier 1:** Widget slot v1 — composable widgets (clock ☑); add at least one more; `AppWidgetHost` spike in 2.16. | ☑ (calendar composable) |
| 2.16 | **Tier 1:** `AppWidgetHost` feasibility on external display (document in device-matrix). | ☑ |
| 2.17 | **Tier 1:** Wallpaper — selectable presets (gradient ☑); optional user image later. | ☑ |
| 2.18 | **Tier 1:** Panel chrome — title bar, focus highlight, close/minimize for widget slots. | ☑ |
| 2.20 | **Recreate pinned-widget contents with BumpDesk items.** Home / Tray / app-plane **faces** are pinned `WidgetItem`s; their chrome/icons/widgets are child `ItemRenderer` objects. Port `TextureUtils` + `WidgetRenderer`. | ☑ Partial — Desktop drawer tile is a GLES box on the sphere; Home/Tray still captured Compose onto pinned pane meshes |
| 2.21 | **BumpDesk desktop on the same sphere.** Port movable items: `APP_DRAWER`, drag/drop, `Pile`, lasso, radial menu, live widgets, physics, `DeskRepository`. All Apps pill can stay on the Home widget. | ☑ Partial — desk DND + physics + persist + GLES lasso stroke/selection + radial context menu; still missing piles |
| 2.22 | **BumpDesk GLES Home Space (blocking).** `perspectiveM` + `setLookAtM`, room. Panes are **pinned widgets** on the inner sphere wall (BumpDesk wall/floor analog). | ☑ Partial — 0.1.9 sphere-ray cursor + tessellated pane meshes; not yet the same class as desktop items |
| 2.23 | **Keep glasses awake.** `FLAG_KEEP_SCREEN_ON` / `SessionWake`. | ☑ Partial — 0.1.7 on-device keep-awake; override display can still report OFF |
| 2.24 | **In-scene Edit mode.** Two pages so the focus range stays small: **Perspective** (panel / sphere / icon scale) and **Desktop** (BumpDesk icons, piles, tiles, widgets). Persist via `WorkspaceAppearance`. Desktop icon size tracks **Icons & elements** via pane-matched half-extents (same 92.dp Home face). Min uiScale 0.5. | ☑ Partial — 0.1.16 Look page has FPS toggle; defaults panel 0.70 / sphere 1.00 / icons 1.20 |
| 2.25 | **Look mode.** A = gradient mouse-look (current). B = FPS capture (cursor centered, deltas rotate view); C = gesture (Scheme A: one-finger desk; two-finger pan **or** pinch zoom with mutex lock). Revert to A when an app launches. Persist. | ☑ Partial — HUD eye / hand / mouse; Scheme A two-finger look + pan/zoom lock; one-finger lasso restored |
| 2.25a | **FPS mouse-look Hold-Left drag/drop.** Unlock cursor while pressed; finalize desk at endPos before center re-lock; sync move-while-pressed. | ☑ Partial — release ordering fixed; touchpad still lacked a true press until finger-up |
| 2.25b | **Touchpad touch-and-hold = press.** Long-press on the companion touchpad starts the same Hold-Left gesture (origin); drag while held; release = drop/click. Always use holdable Left (not click-only Button gated on accessibility). | ☑ |
| 2.25c | **Mouse-look + motion drag.** While FPS mouse-look is on and a grab is active, phone **motion** should drive the same unlocked-cursor desk drag as the touchpad (or a clear look-follow grab). Today motion+FPS registers the press/click but the drag phase moves the cursor without a usable view/grab feel — touchpad path only is reliable. | ☐ Partial — touchpad OK; motion+FPS DnD broken/awkward |
| 2.25d | **Launcher surface (Play path).** `FLAG_SHOW_WALLPAPER` on host; tray notifications via `NotificationListenerService`; curated QS panels; launcher-owned recents; `AppWidgetHost` desk items follow-on. | ☑ Partial — wallpaper flag + live tray notifications/QS/recents; AppWidgetHost still open |
| 2.26 | **Large screen → XR Home Space default.** When `WindowSizeClass` is Expanded (tablet / unfold / DeX / Chromebook) and no glasses session, open the same BumpDesk GLES Home Space used on glasses (`GlassesSpatialWorkspaceScreen` path), not the older Compose `SpatialDesktopScreen` Subspace shell. Compact phone stays Tier 0c. | ☑ |
| 2.27 | **Host XR chrome bar (top HUD).** Mirror the companion touchpad top actions as screen-locked HUD icons along the **top** of the XR workspace (same pattern as Edit locked to bottom-end): input mode (touchpad / head), mouse-look toggle, recenter look/home, optional keyboard. Hit-test via `GlassesHomeHits` like Edit. | ☑ |
| 2.28 | **Settings on host XR chrome.** Add a Settings icon on that top HUD that launches `SettingsActivity` (same destination as the companion “Open settings” button). Keep the bottom-end Edit control. | ☑ Partial — host opens **in-engine Settings dialog**; phone/companion keep `SettingsActivity` |
| 2.28a | **Host BumpDesk input path.** Port BumpDesk `LauncherActivity` gesture model (absolute coords, touch-slop, middle-drag look, scroll/pinch zoom) as second host input method; skip companion FPS press/release re-lock while `hostImmersiveSession`. | ☑ Partial — `HostBumpDeskMotionBridge` + Compose `pointerInteropFilter` catcher (AndroidView ate swipes); HUD wrap-content above catcher; eye/mouse force look mode |
| 2.29 | **Repair Home sprocket SettingsActivity.** The settings screen opened from the Home panel gear (`GlassesWorkspaceTitleBar` / `DisplayLaunchHelper.openSettings`) has broken sections after Home Space / look-mode / desk changes — audit and fix look mode, sensitivity, wallpaper, All Apps grid, and head-tracking controls so they match current runtime behavior. | ☑ Partial — host immersive Settings is Home Space-only (wallpaper / All Apps / look); wallpaper re-uploads on choice; modal clicks own the pointer; phone SettingsActivity still full list |

#### Phase 2.19 — Glasses UX polish (2026-06-12, decisions locked)

**Goal:** Fix wallpaper parallax, make All Apps usable in XR, improve panel handles, grid-snap layout, and declutter companion UI.

| # | Workstream | Decision | Status |
|---|------------|----------|--------|
| 1 | **Wallpaper** | Map device wallpaper onto the **inner cylinder** in GLES; vertical vignette softens top/bottom edges | ☑ |
| 2 | **All Apps pagination** | Default **5×5** grid per page; page size configurable in Settings; prev/next + bottom page buttons; companion page controls when overlay open | ☑ |
| 3 | **Panel handles** | Larger drag/resize affordances (48dp+) for companion pointer | ☑ |
| 4 | **Grid layout** | **Freeform with snap** to cylinder grid; presets stay; shared grid for Compose + GL guides; widget move handles | ☑ (STANDARD preset uses grid bounds; legacy null-bounds stack still supported) |
| 5 | **Settings activity** | Move rarely-changed workspace/options to dedicated Settings activity; hamburger in glasses title bar | ☑ |

**Implementation order:** 1 → 2 → 3 → 4a (grid helper) → 4b (widget drag) → 5 (can parallelize after 2).

#### Phase 2 — Next steps (immediate)

Landed **host BumpDesk input slice:** absolute mouse/touch via `HostBumpDeskInput` (default); companion FPS center-lock skipped on host; `HostPointerBridge` retained behind `HostInputMethod.COMPANION_BUS`.

**Do this next. One concern per change.**

**Launcher surface / BumpDesk widgets:**
0a. **AppWidgetHost on desk** — host live widgets as sphere desk items (BumpDesk `WidgetRenderer` path); picker + persist. Tray notifications / QS / launcher recents and `FLAG_SHOW_WALLPAPER` are landed.

**Pointer / mouse-look:**
0d. **2.25c — Mouse-look + motion drag** — ☐ make FPS grab/drag work with phone motion the same way as touchpad.
0e. **Host BumpDesk input next** — ☐ OS mouse capture / pointer-lock option for FPS look; optional Settings toggle for `HostInputMethod`. ☑ partial: Scheme A gesture look (two-finger pan/zoom lock; one-finger desk/lasso); absolute host no longer double-applies cursor into camera.

**BumpDesk desktop (sphere):**
1. **Lasso draw + selection chrome** — ☑ GLES line strip for the active stroke; selected desk icons use the hover highlight. Hold-Left on empty desktop waits for touch-slop before the stroke (BumpDesk pending); Scheme A one-finger lasso in GESTURE. Release with a capture opens the radial (arrange / clear / remove).
2. **Lasso → pile / arrange** — ☑ Partial: radial Stack (pile stand-in), Folder layout, Row, Column, Grid rearrange selected desk icons around their centroid. True Smart Pile objects still open.
3. **Radial menu** — ☑ right-click / empty long-press / lasso release open a ring; host catcher drops while open so chips receive clicks. Selection ≥2: Stack / Folder / Row / Column / Grid + Clear / Remove. Empty: All apps.
4. **Desk icon size polish** — ☑ round faces restored (on-canvas adaptive bake + Home `CircleShape`); open-drawer = Desktop scale; no GLES plate; labeled mesh height matches texture aspect (1.25) so circles are not vertical ovals.

**Large-screen host:**
4. **2.26–2.28** — ☑ Expanded → GLES Home Space + top HUD + immersive Settings/Edit dialogs.
5. **Polish host pointer** — ☑ partial: free-circle yaw; FPS drag look via `pointerInteropFilter` catcher; HUD eye=GRADIENT / hand=GESTURE / mouse=FPS (force, not toggle); still open: OS mouse capture.
6. **2.29 — Repair Settings content** — ☑ Partial: host Settings dialog is Home Space-trimmed; back/scroll/clicks work (catcher removed while modal); wallpaper choice re-uploads to the surround room. Still open: phone SettingsActivity full audit.
7. **6.9 — Onboarding permissions** — ☑ Partial: missing Home / Accessibility / Notification listener re-shows the guide; App Info Restricted-settings path; no privileged wallpaper/storage APIs — `FLAG_SHOW_WALLPAPER` + gradient GLES surround.

8. **Stop** — Do not start unrelated Phase 6 work in this pass.

**BumpDesk references (port, don’t reinvent):** `LauncherActivity` gestures (host input), `InteractionManager` lasso capture, `Lasso`/`LassoRenderer`, `RadialMenuView` / `RadialMenuGeometry`, `MenuManager`.

**Host chrome model:** Top HUD and Edit toggle stay **viewport-locked** (Minecraft hotbar). Edit/Settings open as **screen-space modals** (Minecraft inventory), not sphere panels. Desk/world picks only when no modal and cursor is off chrome. Host **scroll / pinch** zooms [sphereScale]; **right-click** opens the existing context menu via [CompanionPointerBus]. **Host input default** = BumpDesk absolute (`HostBumpDeskInput`); companion bus FPS re-lock is glasses/phone only.

---

### Phase 3 — App panels & embedding

**Goal:** Open apps inside workspace panels where the platform allows; fall back cleanly elsewhere.

**Exit criteria:** At least one embeddable test app runs in-panel on Tier 3; all other apps launch as full-window fallback without crash.

| # | Task | Done |
|---|------|------|
| 3.1 | Implement `launchInPanel(panel, intent)` with embed attempt then fallback. | ☑ Partial — `PanelAppLauncher` + full-window fallback |
| 3.2 | Check `SpatialCapability.EMBED_ACTIVITY` before embed path. | ☑ Partial — `SpatialEmbedCapability` (spatial API gate) |
| 3.3 | Tier 3: create `ActivityPanelEntity` per panel; wire `startActivity(intent)`. | ☑ Partial — `PanelEmbedRegistry` on XR session |
| 3.4 | Add orbiters / chrome: Close, Focus, Resize, Pop out to full window. | ☑ Partial — `PanelEmbedOrbiterBar` on empty slot (glasses + Tier 0) |
| 3.5 | Dispose panel entity + activity when panel closed (no leaked activities). | ☑ Partial — detach on panel hide / activity destroy |
| 3.6 | Create internal **test harness app** module with `allowUntrustedActivityEmbedding=true` for CI/device testing. | ☑ — `:embed-test-app` module |
| 3.7 | Surface embed support in UI (icon or label: “Spatial window” vs “Full launch”). | ☑ Partial — capability helper + strings |
| 3.8 | Tier 1/2: launch app on glasses display in focused “slot” (pseudo-panel) until true embed works. | ☑ Partial — empty slot host + full-window launch |
| 3.9 | **Tier 0:** Embed or launch-in-panel on large-screen 3D workspace where platform allows. | ☑ Partial — `WorkspaceAppLaunchCoordinator` + empty slot orbiters |

---

### Phase 4 — Input & display modes

**Goal:** Complete input model per tier — keyboard/mouse on desktop, head-mouse on glasses, touchpad + motion on phone companion; 2D/3D glasses output profiles.

**Exit criteria:** Tier 0 navigable by mouse, touch, and keyboard; companion touchpad + motion pointer reliable on glasses; head-mouse optional on glasses; 2D/3D display profiles on RayNeo.

| # | Task | Done |
|---|------|------|
| 4.1 | **Tier 0:** Mouse hover focus, scroll-to-zoom / orbit on host Home Space (after **2.26**). | ☑ Partial — host scroll + pinch zoom sphere; right-click → context menu; orbit / hover polish still open |
| 4.2 | **Tier 0:** Keyboard shortcut polish + rebinding settings. | ☐ |
| 4.3 | **Companion:** Refine touchpad (inertial scroll, tap zones, haptic on click). | ☐ |
| 4.4 | **Companion:** Motion pointer calibration flow (neutral hold → recenter). | ☐ |
| 4.5 | **Glasses:** Read IMU from projected `SensorManager`; optional head-mouse (off by default). | ☐ |
| 4.6 | **Glasses:** Head-mouse toggle, sensitivity, recenter; defer to companion when phone connected. | ☐ |
| 4.7 | Detect display mode (2D 1080p vs 3D SBS / ultra-wide); store in `DisplayProfile`. | ☐ |
| 4.8 | 2D glasses profile: single viewport layout. | ☐ |
| 4.9 | 3D glasses profile: dual viewport or SBS rendering path (IPD / depth as settings). | ☐ |
| 4.10 | Doff/on-head lifecycle: pause head-mouse; companion stays active if phone in hand. | ☐ |

---

### Phase 5 — Full spatial (Tier 3 polish)

**Goal:** Rich experience on Android XR headsets and spatial-capable hosts.

**Exit criteria:** Full Space workspace with environments, movable/resizable spatial panels, Home ↔ Full toggle.

| # | Task | Done |
|---|------|------|
| 5.1 | Add `Subspace` + `SpatialPanel` path parallel to Glimmer (shared workspace state). | ☐ |
| 5.2 | Manifest: `XR_ACTIVITY_START_MODE_FULL_SPACE_MANAGED` for spatial activity. | ☐ |
| 5.3 | `uses-feature android.software.xr.api.spatial` (`required=false` on mobile track). | ☐ |
| 5.4 | Integrate `SpaceToggleButton` or equivalent Home ↔ Full Space transition. | ☐ |
| 5.5 | Optional virtual environment (passthrough or scene from [xr-samples](https://github.com/android/xr-samples)). | ☐ |
| 5.6 | `MovableComponent` / `ResizableComponent` or Compose `movable` / `resizable` modifiers on app panels. | ☐ |
| 5.7 | Multiple named workspaces (save/switch/delete). | ☐ |

---

### Phase 6 — Play Store readiness

**Goal:** Ship a stable, explainable v1 on Google Play (mobile track first).

**Exit criteria:** Internal testing track published; privacy policy; no crashes on tier fallback paths.

| # | Task | Done |
|---|------|------|
| 6.1 | HOME role onboarding flow with clear “Set as default launcher” steps. | ☐ |
| 6.2 | First-run tutorial: 3D desktop (mouse/keyboard), phone companion, connect glasses path. | ☐ |
| 6.3 | Error states: glasses optional (Tier 0 always works), embed failed, display lost. | ☐ |
| 6.4 | Performance pass: limit live activities, lazy-load app icons, profile jank. | ☐ |
| 6.5 | Accessibility: TalkBack on phone path; large touch targets on companion. | ☐ |
| 6.6 | Privacy policy (minimal collection; no analytics or explicit opt-in only). | ☐ |
| 6.7 | Play listing: screenshots, supported devices, honest “embedding limitations” note. | ☐ |
| 6.8 | Beta via internal / closed testing; file bugs against `device-matrix.md` gaps. | ☐ |
| 6.9 | **Onboarding permissions audit.** Wizard checks **every** permission we use, including the desktop-cursor accessibility service and notification listener. Copy must tell the user to enable **restricted settings** from this app’s **App Info** page first, then turn on Accessibility from there (sideload / unknown-source installs hide the service until that unlock). Open App Info + Accessibility + Notification listener settings from the step. Re-check grants when the user returns. Do **not** request root/system/wallpaper privileges — Play launcher path only (`FLAG_SHOW_WALLPAPER`). | ☑ Partial — host + phone re-show when Home / Accessibility / Notification listener missing; App Info CTA; no privileged wallpaper APIs |

---

## Task priority (if cutting scope)

Must ship before public beta:

- Phase 0 (complete device matrix)
- Phase 1 (all tasks — Tier 0 3D desktop + Tier 0c phone shell + companion stub)
- Phase 2: tasks **2.1–2.8**, **2.11**, **2.13–2.15** (glasses real launcher: model, persistence, hotseat, drawer, widgets)
- Phase 4: tasks 4.1–4.6, 4.8 (desktop input + companion + glasses head-mouse optional)
- Phase 6: tasks 6.1–6.3, 6.6–6.9

Can defer post-v1:

- Phase 3 embedding (if Tier 1 is lead platform)
- Phase 4.9 (3D SBS stereo rendering on glasses)
- Phase 5 (Full spatial polish)
- Phase 6 analytics / usage stats

---

## Key references

| Resource | Use |
|----------|-----|
| [xr-codelabs](https://github.com/android/xr-codelabs) | Modes, spatial panels, resize/move |
| [xr-samples](https://github.com/android/xr-samples) | Hello XR: panels, orbiters, environments |
| [Jetpack XR SDK](https://developer.android.com/develop/xr/jetpack-xr-sdk) | SceneCore, capabilities, manifest |
| [Display glasses](https://developer.android.com/develop/xr/jetpack-xr-sdk/glasses/support-different-types) | Projected activity lifecycle, sensors |
| [ActivityPanelEntity](https://developer.android.com/reference/androidx/xr/scenecore/ActivityPanelEntity) | Embed activities in spatial panels |
| [Activity embedding](https://developer.android.com/develop/ui/views/layout/activity-embedding) | Cross-app opt-in; spatial panels all tiers |
| [Spatial UI design](https://developer.android.com/design/ui/xr/guides/spatial-ui) | Placement, comfort, orbiters |
| [Large screen guidance](https://developer.android.com/develop/ui/compose/layouts/adaptive) | Window size class routing; Tier 0 vs 0c |

---

## Decision log

Record major choices here as they are made.

| Date | Decision | Rationale |
|------|----------|-----------|
| — | Lead platform: RayNeo Tier 1/2 unless Phase 0 proves Tier 3 | Matches available hardware; avoids blocked embed path |
| — | Single app module initially; split `core/` when second activity lands | YAGNI until Phase 1.2 |
| 2026-06-11 | **Tier 0 standard launcher** when no glasses | Play Store viability + usable on tablets/DeX without XR hardware |
| 2026-06-11 | Single `MainActivity` holds HOME; glasses use separate projected activity | Avoid competing HOME handlers; clean mode switch |
| 2026-06-11 | **One task = one branch + one commit** after `./gradlew build` + tests | Traceable history; stable tags per phase |
| 2026-06-11 | **Tests required** for every `core/` and `ui/` component | Prevent regressions across tier modes |
| 2026-06-11 | **Tier 0 large screen = 3D spatial**, not 2D tiles | Same workspace model; mouse/touch/keyboard navigation |
| 2026-06-11 | **Phone = touchpad + motion companion** | Primary pointer for glasses; also controls large-screen workspace |
| 2026-06-11 | Tier 0c = compact flat HOME on phone only | 3D workspace lives on Expanded display or remote glasses |
| 2026-06-11 | **Dual launch pins display ID** | Companion → `DEFAULT_DISPLAY`; workspace → secondary ID; Desktop Mode otherwise routes both to glasses |
| 2026-06-11 | **Phase 1.5** tracks SmartGlasses Desktop Mode bugs | Tap click, motion calibrate, freeform → fullscreen |
| 2026-06-12 | **Unified glasses pointer** — one Desktop gesture set; launcher foreground hit-test | Removed mode toggle; Subspace shell when spatial API present |
| 2026-06-11 | **No `adb install` over Wi‑Fi** on dev machine | Use file-transfer app; ADB for logcat/dumpsys only |
| 2026-09-18 | **Onboarding 6.9 (backlog):** full permission check + App Info restricted-settings path for desktop cursor accessibility | Sideloaded builds cannot enable the accessibility service until Restricted settings is allowed on App Info |
| 2026-09-18 | **2.20:** recreate icons/shortcuts/controls/widgets with BumpDesk `ItemRenderer` / `WidgetRenderer` **on GLES panels** | Compose grids cannot sit in FPS perspective |
| 2026-09-18 | **2.21:** Desktop pane with BumpDesk DND, piles, arrange, widgets | After GLES panels + items exist |
| 2026-09-18 | **2.22 blocking:** BumpDesk GLES engine is the Home Space view | Compose `graphicsLayer` keeps straight/isometric edges; no inner bevel |
| 2026-09-18 | **2.22 GLES panes in 0.1.8:** tessellated sphere patches + thickness, Compose captured as textures | graphicsLayer kept only as an invisible hit overlay |
| 2026-09-18 | **2.23 keep-awake** + **2.24 GLES Edit mode** after the engine | Caffeine proved phone sleep blanks glasses; scale sliders belong in the 3D scene |
| 2026-09-18 | **0.1.9:** camera ray onto the inner sphere is the cursor; pane faces mostly transparent; glasses forced fullscreen; Edit in the corner | HUD-plane hover only worked near view center; Desktop Mode freeform; Edit overlay was unwired |
| 2026-09-18 | **0.1.10:** mouse-look yaw+pitch restored; Edit chrome scaled up; hover stays sphere-ray | 0.1.9 look only edge-panned and lost up/down |
| 2026-09-18 | **0.1.11:** Edit dialog centered over the focused pane | Corner stack hid the panel behind the controls |
| 2026-09-18 | **0.1.12:** sphere scale is distance; Edit close; Home pagination; Desktop floor of physical icons | Sphere scale used to keep angular size fixed; Home arrows updated All Apps pages; All Apps pane hid the desk |
| 2026-09-18 | **0.1.13:** Desktop faces the left look; All Apps drawer tile only; thin boxes; desk-local hover | 0.1.12 desk was a side-on infinite floor of thick cubes; hover used world AABB |
| 2026-09-18 | **BumpDesk is the engine.** Port the majority of BumpDesk (items, piles, lasso, radial menu, drawer, widgets, physics, persist) onto the inner Home Space sphere; XR workspace is built on that | Floor/top-down was a dead end; looking left must face Desktop like Home |
| 2026-09-18 | **0.1.14:** All Apps tile on the inner sphere; Edit has a second Desktop page for BumpDesk types | Desk was hidden below FOV as a look-down floor |
| 2026-09-18 | **Panels = pinned BumpDesk widgets** on the inner sphere wall (Home, Tray, app planes). Desktop icons/piles are movable items on the same surface | One scene graph; BumpDesk `WidgetItem` + `isPinned` is the panel model |
| 2026-09-18 | **0.1.15:** All Apps tile upright; click expands BumpDesk 4×4 drawer on the sphere; desktop icons use Icons & elements scale 1:1 | Tile was 180° Z; Compose All Apps overlay is not the drawer |
| 2026-09-18 | **Desk tile facing:** inward pancake looks at the camera (right=+viewX, up=+viewY); shader 1−v UVs put bitmap top on camera top | The box was not 180° Z; the texture UVs were |
| 2026-09-18 | **0.1.16:** hover lift + selection; smaller desktop tiles; closer All Apps widget Z-stack; drag onto desktop; look mode A/B (FPS while launcher is in front) | Grey pad, huge tiles, same-radius drawer, no DND, only gradient look |
| 2026-09-18 | **All Apps expand:** no camera snap to Desktop; open-drawer icons 1.4× with backing fitted to 4×4 + pager | Forced `lookAt(PANE_LEFT)` stole view; widget/icons read too small on glasses |
| 2026-09-18 | **All Apps widget refit:** content-sized backing, pager gap, max 5 page dots, hide closed tile while open; pick prefers pager; drops collide with tile/icons/panes | Clipped top row, cramped/stray pager, backing ate clicks, free overlap on drop |
| 2026-09-18 | **Desk drag pointer-up:** snapshot/suppress companion click before clearing `isPressed` | Compose release raced and clicked at the drop point |
| 2026-09-18 | **All Apps frame + pager chrome + DeskPhysics:** wider/squarer backing, row gap; Hold-Left pager via pending chrome; BumpDesk mass/impulse on sphere (panes pinned) | Tall cramped widget; highlight without click; icons passed through each other/panels |
| 2026-09-18 | **Desk grab/pager/pose:** square backing; unplaced pageCount; sync Left-down grab; movable All Apps tile; zero release velocity | Pager no-op; jump on drop; Hold-Left only after lift; All Apps tile stuck |
| 2026-09-18 | **Shrink open All Apps** so pager stays in cursor pitch FOV | Widget too tall to reach bottom pagination |
| 2026-09-18 | **Hide All Apps backing texture** — pick/physics only, no stretched panel | Low-res rounded panel looked mottled / blotchy |
| 2026-09-18 | **Mouse-look pitch sign + companion toggle** — finger up looks up; 3D-rotation icon above touchpad | FPS look inverted; no phone control for look mode |
| 2026-09-18 | **Desktop look range** — `minPan` = PANE_LEFT − 1.15 so empty space / left All Apps is reachable | Pan clamped at Desktop center; left half of drawer unreachable |
| 2026-09-18 | **0.1.17:** persist Desktop icons + All Apps tile pose via `desk_json` in workspace DataStore | Positions were in-memory only |
| 2026-09-18 | **Desk pager + open speed:** incremental icon bitmaps; drag effect ignores mouse-look; desk owns pageCount | Full rebuild on page; FPS look restarted drag and ate pager clicks |
| 2026-09-18 | **0.1.18 release:** README screenshots (phone companion + glasses Home Space / All Apps) | Docs lagged the desk UI |
| 2026-09-18 | **Tray look range** — `maxPan` = tray + SIDE_LOOK_EXTRA (mirror of Desktop left) | Mouse-look locked at tray center; right half unreachable |
| 2026-09-18 | **FPS click-drag** — touchpad looks only when unpressed; Hold-Left unlocks cursor from center, then re-locks | Look ate every move so taps became no-op drags and grabs never traveled |
| 2026-09-18 | **Desk GLES dirty render** — bus callback syncs icons/textures onto the renderer before `requestRender` | All Apps expand waited for the next cursor move to recompose AndroidView |
| 2026-09-18 | **Home→Desktop copy-drag** — Hold-Left on Home pane app places a Desktop icon; Home list unchanged | Only All Apps drawer could seed the desk |
| 2026-09-18 | **Lasso foundation** — `DeskLassoState` sphere yaw/pitch polygon + empty-Desktop Hold-Left stroke | Next: GLES stroke, selection chrome, pile-from-lasso, radial menu |
| 2026-09-18 | **2.26–2.28 planned:** Expanded → GLES Home Space default; top HUD mirrors companion touchpad + Settings; Edit stays bottom-end | Large screen still opens legacy `SpatialDesktopScreen`; no host chrome for look/input/settings |
| 2026-09-18 | **Plan sync for 2.26–2.28** — Current direction, Tier 0 rules, runtime tiers, mode selection, module layout, Phase 4.1 note | Earlier plan still described Expanded as Subspace `SpatialDesktopScreen` |
| 2026-09-18 | **2.25a planned:** FPS Hold-Left DND — release at endPos before center re-lock; sync move-while-pressed | `fpsReleaseCursor` made `onDesktop` false when still facing Home; drops discarded |
| 2026-09-18 | **2.25a:** sync `onPointerMoveWhilePressed` + `onPointerGestureFinalize` before FPS center re-lock | Compose only saw (0.5,0.5,up); Desktop drops discarded |
| 2026-09-18 | **2.25b + 2.29 planned:** touchpad long-press = Hold-Left origin; repair Home sprocket `SettingsActivity` | Touch'n'hold still only clicked on finger-up; settings UI drifted from Home Space |
| 2026-09-18 | **2.25b:** touchpad long-press begins Hold-Left; Left always holdable (not a11y-gated) | Finger-up-only clicks; no grab origin while mouse-looking |
| 2026-09-18 | **2.25c planned + README partial:** FPS mouse-look desk DnD works on touchpad; motion+FPS drag still awkward | Grab click registers; drag phase moves cursor without usable view/motion grab |
| 2026-09-18 | **All Apps pager clicks:** finalize must not re-press after chrome; near-miss → `pickNearestPager` + open-drawer zone (no lasso / no scrim dismiss) | 2.25a finalize re-press + empty-click dismiss closed the widget on pagination |
| 2026-09-18 | **Return to All Apps** — drop a Desktop icon on the All Apps tile (or open backing) removes it | Drop only pushed away from the tile / rejected on backing |
| 2026-09-18 | **0.1.19 release** — pager click fix, return-to-drawer remove, Hold-Left / mouse-look desk polish | Post-0.1.18 desk interaction fixes |
| 2026-09-19 | **2.26–2.28 host:** Expanded → `HostHomeSpaceScreen` GLES Home Space; top HUD; Edit/Settings as view-locked GLES dialog textures; phone keeps SettingsActivity | Large screen opened legacy SpatialDesktopScreen; overlays left immersion |
| 2026-09-19 | **Host chrome = Minecraft layers:** viewport HUD + modal Edit/Settings (Compose `drawToScreen`); desk bus only off-chrome; FPS look paused in modals | FPS center-lock + GLES-only Edit skewed hits; HUD/desk fought for clicks |
| 2026-09-19 | **4.1 partial:** host scroll-wheel + 2-finger pinch → sphere zoom; mouse right-click → existing context menus | Host had left-only pointer; no zoom gestures |
| 2026-09-19 | **Desk/Home icon uniformity:** `AppIconCache` normalizes adaptive insets to a square bitmap; Desktop GLES tiles share a plate; `DRAWER_OPEN_ICON_SCALE = 1.0`; base `ICON_HALF_*` raised to 0.064×0.077 | Transparent adaptive icons looked tiny vs filled ones; open-drawer was 1.05× Desktop; desk tiles were hard to read |
| 2026-09-19 | **Icon plates removed:** circular-mask adaptive bake (no white square bg); drop Desktop GLES plate; Home cells use masked bitmaps at one `iconSize` | Inset expand without mask showed adaptive white backgrounds; Home logos still uneven fill |
| 2026-09-19 | **2.28a host BumpDesk input:** `HostBumpDeskInput` + `BumpDeskHostGesture`; skip FPS center-lock when `hostImmersiveSession`; `HostInputMethod` switch (default BUMPDESK) | Companion FPS press/release re-lock fought absolute mouse on Expanded host |
| 2026-09-19 | **Round icon faces restored:** stop expanding adaptive bounds past canvas (that squared the OEM mask); clipPath circle + mild fill zoom; Home `CircleShape`; desk atlas circle clip; app meshes front-face only | Extra-inset expand filled square boxes; rectangular pancake sides read as plates |
| 2026-09-19 | **Host absolute pick alignment:** `applyCursorOffset=false` for BumpDesk host; desk/camera/physics use live viewport; disable `pickNearestPager` magnet on host | Gesture path was BumpDesk but GRADIENT camera+ray double-offset + 1920×1080 layout vs real aspect caused inches-off launches |
| 2026-09-19 | **Free-circle look + HUD under FPS:** unclamp `panNorm`; pitch ±89°; pause mouse-look over screen chrome; no host cursor snap on FPS enable | Look locked past All Apps / mid-tray; FPS ate top HUD look-mode clicks |
| 2026-09-19 | **Free-look yaw degrees + 2-finger pan + flat icon light:** camera uses `lookYawDegrees`; pinch mid-drag pans; desk icons low diffuse | Mouse-look still felt FOV-clamped via panNorm*arc; pinch zoom only; lit shader crushed icon colors |
| 2026-09-19 | **Mouse-look drag + HUD eye=normal:** FPS look pans while pressed; eye sets GRADIENT, mouse sets FPS; collect `lookYawDegFlow` | Screenrecord: mouse stayed selected; eye was head-track no-op; FPS ignored finger-drag look |
| 2026-09-19 | **Host look unstuck:** `pointerInteropFilter` catcher + transparent hit target; HUD wrap-content above catcher; `effective()` honors `hostImmersiveSession`; HUD bus force GRADIENT/FPS (no toggle race) | GLES AndroidView ate swipes; fillMaxSize HUD overlay blocked catcher; delayed LeftClick toggled look back to FPS |
| 2026-09-19 | **Host look feel:** FPS hover mouse-look (no click); GRADIENT absolute host pitches from cursor Y again | Catcher required primary-down; `applyCursorOffset=false` had zeroed gradient pitch |
| 2026-09-19 | **Gradient look curve:** `cursorEdgeWeight` (`1-cos`) for pitch and horizontal pan — flat at center, steepest at the edges | Linear pitch and edge-only yaw dead zone |
| 2026-09-19 | **Lasso stroke + radial menu:** GLES line strip and selection highlight from `DeskLassoState`; context actions are a screen-space ring above the host catcher | Stroke existed only as yaw/pitch state; right-click was a list under the catcher |
| 2026-09-19 | **2.29 host Settings/Edit:** drop pointer catcher while modals open; Home Space-only settings; wallpaper upload keys include choice ordinal | Catcher ate back/scroll/Edit clicks; Settings listed 2D cylinder options; wallpaper choice never re-uploaded |
| 2026-09-19 | **Host Edit modal hoist:** compact unscaled `HostEditDialogLayer` above catcher; skip in-scene Edit card on host; drop empty InteractionLayer Box | Edit stayed under InteractionLayer + 1.2× uiScale; X/+/- never received Compose clicks |
| 2026-09-19 | **Settings slider stuck press:** host Settings is direct Compose; scrim is a sibling, not a clickable parent, and no graphics-layer capture | Slider drag left the parent clickable pressed, so only sliders still received events |
| 2026-09-19 | **Look deadzone sliders:** horizontal and vertical center band (0–50% of center-to-edge) zeros `cursorEdgeWeight` before the sine curve | Normal look moved as soon as the cursor left the exact center |
| 2026-09-19 | **Stale Edit hits:** ignore Edit +/- unless Edit is open, and clear those bounds when the card hides | A Home pager miss landed on a leftover Icons & elements minus rect and shrank uiScale |
| 2026-09-19 | **Gesture look:** third HUD icon; camera ignores cursor/touch position; drag, two-finger, and middle pan look; empty drag is look, not lasso | Edge look and mouse-look both aim where the pointer rests |
| 2026-09-19 | **Onboarding 6.9 partial:** re-show each launch when Home or Accessibility missing (host + phone); App Info Restricted-settings CTA; permission-only pages after first complete | Completed flag hid the wizard forever; host Expanded never showed it |
| 2026-09-19 | **No privileged wallpaper/storage APIs** — Play launcher path only; gradient fallback when system wallpaper is unavailable | Avoided MANAGE_EXTERNAL_STORAGE / root-style wallpaper access |
| 2026-09-19 | **Gesture look natural scroll:** LookPan sign inverted in GESTURE (drag background / news-feed style); FPS stays classic mouse-look | Gesture drag felt like FPS mouse-look |
| 2026-09-19 | **Gesture look ignores path icons:** mid-pan does not grab desk/Home icons under the finger; icon drag still works when the press starts on an icon | Crossing icons mid-look stole the pan and started a drag |
| 2026-09-19 | **Scheme A gesture look:** one-finger = desk (tap/drag/lasso/radial); two-finger only for camera with PAN\|ZOOM mutex on first decisive motion; natural-scroll LookPan | One-finger look blocked lasso; pan+pinch fought |
| 2026-09-19 | **Launcher surface Play path:** `FLAG_SHOW_WALLPAPER`; `NotificationListenerService` tray; curated QS intents; launcher-owned recents; onboarding for notification access; AppWidgetHost deferred | Tray was stub empty cards; no privileged wallpaper APIs |
| 2026-09-19 | **Desk/Home icon glue:** Desktop GLES half-extents from Home 92.dp + live pane geometry/density; Icons & elements min 0.5 | Desk faces ~½ Home icons and drifted vs sphere/panel scale |
| 2026-09-19 | **Home pane icon gamma:** pane lit shader uses desk-flat ambient/diffuse (0.92 / 0.12) | Strong pane diffuse (0.28 / 1.35) made Home icons look higher-contrast than Desktop |
| 2026-09-19 | **Pinch aborts lasso:** second finger cancels desk hold/lasso without finalize; 2-finger MOVE always enters pinch | One-finger slop started lasso before POINTER_DOWN; endLeftButton finalized stroke |
| 2026-09-19 | **Lasso release opens radial:** non-empty capture opens Desktop context ring (clear / remove from desk) | Selection highlighted but no group actions on finger-up |
| 2026-09-19 | **Empty long-press + arrange radial:** pending lasso until slop; long-press / secondary open radial; catcher drops while menu open; Stack/Folder/Row/Column/Grid rearrange | Long-press started lasso; catcher ate radial clicks; no BumpDesk layout actions |
| 2026-09-19 | **Labeled desk mesh aspect:** APP/drawer halfHeight = halfWidth × 1.25 to match icon+label atlas; pager bitmaps stay square | 160×200 texture on square mesh squashed circles into vertical ovals |

---

*Last updated: 2026-09-19 (desk icon aspect)*
