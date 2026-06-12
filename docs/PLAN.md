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

## Rules

These rules apply to all design and implementation decisions. When in doubt, follow the rule that keeps scope smaller and permissions lower.

### Product & permissions

1. **Play Store first.** Do not depend on signature, system, or root-only APIs. If a feature requires `CREATE_VIRTUAL_DEVICE`, Shell, or `MANAGE_ACTIVITY_TASKS`, it is out of scope for the main product path.
2. **Launch by default, embed when possible.** Start apps with standard `Intent` + display targeting. Use `ActivityPanelEntity` / activity embedding only when the platform grants `EMBED_ACTIVITY` and the target app opts in.
3. **Zero dangerous permissions in v1.** No `QUERY_ALL_PACKAGES`. Discover apps via `ACTION_MAIN` + `CATEGORY_LAUNCHER`. Defer `PACKAGE_USAGE_STATS` to a later phase and disclose it clearly if added.
4. **Graceful degradation.** Every spatial feature must have a fallback. Never assume glasses or Full Space APIs exist. **No glasses on large screen → Tier 0 3D spatial desktop.** **No glasses on phone → Tier 0c compact shell**, with optional companion mode.

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

15. **3D, not 2D, on large screens.** On `WindowSizeClass.Expanded` (tablet, unfolded foldable, DeX, Chromebook), Tier 0 is a **3D spatial workspace** — same panel model as XR tiers — rendered on the host display. Navigation uses **mouse**, **touch** (drag to orbit/pan panels, pinch where applicable), and **keyboard shortcuts** (focus next panel, move, resize, launch app).
16. **Compact phone is the launcher shell + companion.** On phones without an external display, show a flat app drawer / search UI for everyday HOME duty. When glasses connect or the user opens “Control workspace,” the phone becomes a **companion controller** (touchpad + motion pointer) — not a scaled-down 3D desktop.
17. **Activity embedding on large screens.** Tier 0 may embed activities in spatial panels via [activity embedding](https://developer.android.com/develop/ui/views/layout/activity-embedding) or `ActivityPanelEntity` when APIs allow — same opt-in constraints as other tiers.

### Phone companion controller

18. **Touchpad mode.** Phone screen acts as a relative touchpad: drag moves the workspace cursor; tap = click; two-finger tap = right-click or context (if needed).
19. **Motion pointer mode.** Phone gyro/accelerometer drives cursor movement (air-mouse style), like RayNeo mouse control on Linux. Toggle between touchpad and motion; provide recenter and sensitivity settings.
20. **Companion pairs with remote workspace.** Companion input targets the focused panel on the **glasses display** or **large-screen 3D workspace**, sent over local connection (same app, two activities / display routing — no network permission required for same-device control).

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
| **0 — Spatial desktop** | No glasses; large screen (`Expanded`) | Standard `startActivity` + spatial panels; optional embed | **3D spatial workspace** on host display — mouse, touch, keyboard |
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
else if (spatialHeadset && no separate glasses)   → Tier 3
else if (windowSizeClass == Expanded)            → Tier 0 (3D spatial desktop)
else                                              → Tier 0c (compact phone shell)
```

Within Tier 0 / 0c:

| Form factor | Workspace UI | Primary input |
|-------------|--------------|---------------|
| **Expanded** (tablet, unfolded foldable, DeX, Chromebook) | **3D spatial panels** in `Subspace` / SceneCore (or equivalent on host display) | Mouse, touch gestures, keyboard shortcuts |
| **Compact** (phone portrait, no external display) | Flat app drawer + search (HOME shell) | Direct touch on phone |
| **Phone as companion** (workspace on glasses or large screen) | Companion UI on phone; workspace on remote display | **Touchpad + motion pointer** on phone |

### Input by tier

| Tier | Pointer sources | Navigation / shortcuts |
|------|-----------------|------------------------|
| **0 — Spatial desktop** | Mouse, trackpad, touch (panel drag/orbit), keyboard | `Tab` / arrow keys focus panels; shortcuts for launch, close, snap layout; scroll/pinch zoom workspace |
| **0c — Phone shell** | Touch on phone | Standard launcher; button to open companion mode |
| **1–2 — Glasses** | Phone **touchpad + motion** (primary), optional glasses head-mouse | Companion buttons: recenter, back, keyboard toggle |
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
│   ├── desktop/            # Tier 0: 3D spatial workspace (large screen)
│   ├── phone/              # Tier 0c compact HOME shell
│   ├── companion/          # Phone touchpad + motion controller UI
│   ├── glimmer/            # Tier 2 glasses UI
│   └── spatial/            # Tier 3 Subspace, SpatialPanel, orbiters
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
| 1.3 | **Tier 0:** 3D spatial shell on Expanded — `Subspace` / spatial panels, empty workspace scene. | ☑ |
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

### Phase 2 — Workspace layout

**Goal:** Multiple launcher-owned panels with layout persistence.

**Exit criteria:** User can arrange 3+ panels, resize/move them, save and restore one workspace.

| # | Task | Done |
|---|------|------|
| 2.1 | Define `Workspace`, `PanelState`, `EmbedMode` data classes in `core/workspace`. | ☐ |
| 2.2 | Persist workspace JSON via DataStore (single default workspace). | ☐ |
| 2.3 | Render static panels: app drawer, clock/status, placeholder “empty slot.” | ☐ |
| 2.4 | Add panel focus model (mouse click / touch / companion tap / keyboard focus next). | ☐ |
| 2.5 | Implement move (drag) for panels — tier-appropriate API (`movable` modifier, Glimmer, or drag handles). | ☐ |
| 2.6 | Implement resize with min/max bounds and optional fixed aspect ratio. | ☐ |
| 2.7 | Add layout presets: Single, Dual, Triptych (apply preset → update poses). | ☐ |
| 2.8 | Save on change; restore workspace on activity start (all tiers). | ☐ |
| 2.9 | **Tier 0:** Spatial app-drawer panel + dock orbiter in 3D scene (not flat grid). | ☐ |
| 2.10 | **Tier 0:** Keyboard shortcut map (focus panels, launch, close, snap preset) + help overlay. | ☐ |
| 2.11 | **Companion:** Wire touchpad pointer → focused panel on glasses / large-screen workspace. | ☐ |
| 2.12 | **Companion:** Motion pointer mode using phone `SensorManager` (toggle, sensitivity, recenter). | ☐ |

---

### Phase 3 — App panels & embedding

**Goal:** Open apps inside workspace panels where the platform allows; fall back cleanly elsewhere.

**Exit criteria:** At least one embeddable test app runs in-panel on Tier 3; all other apps launch as full-window fallback without crash.

| # | Task | Done |
|---|------|------|
| 3.1 | Implement `launchInPanel(panel, intent)` with embed attempt then fallback. | ☐ |
| 3.2 | Check `SpatialCapability.EMBED_ACTIVITY` before embed path. | ☐ |
| 3.3 | Tier 3: create `ActivityPanelEntity` per panel; wire `startActivity(intent)`. | ☐ |
| 3.4 | Add orbiters / chrome: Close, Focus, Resize, Pop out to full window. | ☐ |
| 3.5 | Dispose panel entity + activity when panel closed (no leaked activities). | ☐ |
| 3.6 | Create internal **test harness app** module with `allowUntrustedActivityEmbedding=true` for CI/device testing. | ☐ |
| 3.7 | Surface embed support in UI (icon or label: “Spatial window” vs “Full launch”). | ☐ |
| 3.8 | Tier 1/2: launch app on glasses display in focused “slot” (pseudo-panel) until true embed works. | ☐ |
| 3.9 | **Tier 0:** Embed or launch-in-panel on large-screen 3D workspace where platform allows. | ☐ |

---

### Phase 4 — Input & display modes

**Goal:** Complete input model per tier — keyboard/mouse on desktop, head-mouse on glasses, touchpad + motion on phone companion; 2D/3D glasses output profiles.

**Exit criteria:** Tier 0 navigable by mouse, touch, and keyboard; companion touchpad + motion pointer reliable on glasses; head-mouse optional on glasses; 2D/3D display profiles on RayNeo.

| # | Task | Done |
|---|------|------|
| 4.1 | **Tier 0:** Mouse hover focus, scroll-to-zoom workspace, drag-to-orbit (configurable). | ☐ |
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

---

## Task priority (if cutting scope)

Must ship before public beta:

- Phase 0 (complete device matrix)
- Phase 1 (all tasks — Tier 0 3D desktop + Tier 0c phone shell + companion stub)
- Phase 2: tasks 2.1–2.12 (includes spatial panels, keyboard map, companion touchpad + motion)
- Phase 4: tasks 4.1–4.6, 4.8 (desktop input + companion + glasses head-mouse optional)
- Phase 6: tasks 6.1–6.3, 6.6–6.8

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

---

*Last updated: 2026-06-11*
