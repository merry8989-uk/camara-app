# Camara — audit & improvement plan

A code audit of the app, with what's broken, what's fragile, and the order to fix it.
Written by reading the source, not from memory.

Status key: **fixed** · **in progress** · **planned**

---

## App Factory compliance

The App Factory skill defines a fixed Definition of Done for every app. Camara's status
against it:

| # | Requirement | Status |
| --- | --- | --- |
| 1 | Public repo under the user's account | ✅ `merry8989-uk/camara-app` |
| 2 | Release keystore, backed up, repo secret, never committed | ✅ v1.4.0 |
| 3 | Package ID fixed and documented | ✅ `com.camara.app` |
| 4 | Builds green via CI | ✅ |
| 5 | Signed release + tag + notes, published automatically | ✅ build job + tag-triggered publish job |
| 6 | Full-screen / immersive mode | ✅ v1.5.0 (toggle in Settings) |
| 7 | Rich Settings screen, wired, persistent | ✅ v1.5.0 (DataStore) |
| 8 | Update installs over the previous version | ✅ |
| 9 | Public GitHub Pages page documenting every feature | ✅ v1.5.0 (`docs/`) |
| 10 | Settings ends with a tappable @osintgram_io link | ✅ v1.5.0 |
| 11 | In-app "Check for updates" installs without a browser | ✅ v1.5.0 |
| 12 | Diagnostics section, log collection on by default | ✅ v1.5.0 |
| 13 | Logs shared / exported (.txt/.json/.md/.zip) / cleared | ✅ v1.5.0 |

Rule 1 was also tightened: a release build with no signing key now fails hard instead of
silently producing an unsigned APK.

---

## Second pass — audit at v1.11.0

A second read of the source, this time prompted by a report that the preview looks blurred
and the filter chips look blank. Everything below comes from the code and the shipped
binary, not from the screenshots — see "What is *not* a bug" for why the screenshots
themselves are inconclusive.

| # | Sev | Where | Problem |
| --- | --- | --- | --- |
| 19 | 🔴 P0 | `filters/LiveFilterEffect.kt` | Live skin smoothing blurs the **whole preview**. A `RenderEffect` on a `View` cannot be masked to a face, so the blur lands on every pixel. The saved photo *is* masked (`FaceBeauty`), so the preview and the result disagree. |
| 20 | 🔴 P0 | `filters/LiveFilterEffect.kt` | Below API 31 the function does nothing at all, and there is no other live mechanism in the app — no `CameraEffect`, no GL surface. On Android 7–11 the preview is always unfiltered while the capture is filtered, silently. |
| 21 | 🟠 P1 | `beauty/FaceBeauty.kt` | When no face is found, beauty is applied to the **whole frame** instead of not at all. "Beauty on the face only" becomes "beauty everywhere" exactly when detection is least reliable. |
| 22 | 🟠 P1 | `util/Blur.kt`, `portrait/PortraitBokeh.kt` | `Blur.blur` allocates three full-size `IntArray`s plus the blurred bitmap. On a 12 MP photo that is about 190 MB in one go — an OOM on a 2 GB device. |
| 23 | 🟡 P2 | `util/Permissions.kt` | Android 14 partial media access is unhandled: no `READ_MEDIA_VISUAL_USER_SELECTED`, so a user who picks "Select photos" gets a partial gallery with no explanation and no way to widen it. |
| 24 | 🟡 P2 | `app/build.gradle.kts` | `targetSdk = 34`. Play has required API 35 for new apps and updates since August 2025, so this bundle would be refused. `PLAY.md` claimed 34 was fine — that was wrong, and is corrected there. |
| 25 | 🟡 P2 | `SettingsScreen`, `CamaraRoot` | App-lock availability is cached once per process (`remember { AppLock.isAvailable(...) }`). Set up a PIN while the app is running and neither the toggle nor the gate appears until a restart. |
| 26 | 🔵 P3 | `filters/UserFilterStore.kt` | `load()` reads and parses JSON on the main thread at startup — small, but it is file I/O inside composition. |

### What is *not* a bug

The screenshots show grey filter chips. They are dated before v1.5.2 — the release that
replaced the gradient swatch with a real photo — and the bundled `filter_sample.jpg` is
present in the shipped APK's resource table. The chips in the current build are very
likely fine, so no fix is claimed for something that is probably already fixed.

The blurred preview is different: **#19 explains it, and #19 is real.**

---

## Findings

### 🔴 P0 — real bugs (wrong behaviour or crashes)

| # | Where | Problem | Impact |
| --- | --- | --- | --- |
| 1 | `content/ContentStore.kt` | `sync()` writes Compose snapshot state (`status`, `message`, `remote`, counts) from inside `withContext(Dispatchers.IO)` | Compose state must be written on the main thread. Background writes can throw `IllegalStateException` or leave the UI showing stale/incorrect status. **fixed** |
| 2 | `editor/image/ImageEditorScreen.kt` | The portrait preview effect is keyed on `edit.portrait`, which changes on every strength-slider tick, so **segmentation re-runs per tick** | ~200 ms of ML inference per slider frame — the panel becomes unusable. **fixed** |
| 3 | `util/Permissions.kt` + capture/edit save paths | `WRITE_EXTERNAL_STORAGE` is declared (maxSdk 28) but **never requested at runtime** | On Android 7–9 the shared media store can't be written without it, so **captures and edits fail to save**. **fixed** |
| 4 | `camera/CameraScreen.kt` | Permanent permission denial shows a rationale screen whose button re-requests the same permission, which Android no longer displays | User is stuck on a dead screen with no way to recover. **fixed** |

### 🟠 P1 — fragile / poor UX

| # | Where | Problem | Impact |
| --- | --- | --- | --- |
| 5 | ML Kit usage (`lenses/`, `beauty/`, `portrait/`) | Zero handling of the unbundled models not being present. If Google Play Services is missing or the model hasn't downloaded, face lenses, face-only beauty and portrait depth **silently do nothing** | Looks like the feature is broken, with no explanation. **fixed** (Play Services availability check + user-facing message) |
| 6 | `ui/components/FilterStrip.kt` | `remember(selected) { mutableStateOf(selected.category) }` re-initialises the category tab whenever the selected filter changes | The carousel jumps back to another category while browsing. **fixed** |
| 7 | `util/BitmapUtils.kt` | Editor decodes at `maxDim = 4096` | A 4096×3072 ARGB bitmap is ~50 MB; with preview + mask + export intermediates this can OOM on 2 GB devices. **fixed** (capped at 2560) |

### 🟡 P2 — quality / performance

| # | Where | Problem | Impact |
| --- | --- | --- | --- |
| 8 | `util/Blur.kt` | `boxBlur` was O(w·h·r²) — a 1024² image at radius 4 was ~84 M operations | Beauty smoothing and bokeh were slow. **fixed** (separable blur, O(w·h) per pass, with tests) |
| 9 | `editor/`, `core/` | Every pipeline stage allocated a bitmap and dropped the previous one, so an export held six full-size bitmaps at once | OOM risk on a 12 MP photo on a 2 GB device. **fixed** (v1.5.1: ownership-aware [ReleasingChain], unit tested) |
| 10 | whole app | **No unit tests at all** | The colour-matrix maths — the core of every filter — has never been verified by a test. **fixed** (JVM tests added) |
| 11 | `camera/CameraController.kt` | `rebind()` ran on every lens toggle (analysis was only bound while a lens was active) | Visible preview flicker each time a lens was turned on or off. **fixed** (v1.5.1: analysis always bound, only the analyzer is attached/detached) |
| 12 | `editor/video/` | No text/sticker overlays | **fixed** (v1.5.4: timed text and sticker overlays, baked into the export) |
| 17 | `editor/video/` | Single-clip only, so no way to join clips and nothing to transition between | **multi-clip done** (v1.7.0). **Fade-to-black transitions done** (v1.9.0). Crossfade and reverse still pending. |
| 13 | `editor/` | No undo/redo | Feature gap — the single most-requested editing affordance. **fixed** (v1.4.2) |
| 14 | `camera/` | The look wasn't applied to the **recorded** video — the clip was written straight to the gallery, unfiltered | **fixed** (v1.6.0: recorded to a temp file, filter baked in on stop). Beauty and lenses are still preview-only — they need per-frame face tracking. |
| 15 | `editor/video/` | No undo/redo; no trim handles beyond the range slider | You could not take back a trim, filter or speed change. **undo/redo fixed** (v1.5.3) |
| 16 | `ui/components/FilterStrip.kt` | Filter chips were a colour gradient derived from the matrix, which says almost nothing about what the filter does | You had to try every filter to see it. **fixed** (v1.5.2: chips render the filter on a real photo) |
| 18 | `filters/` | No way to make or keep your own filter — the 75 built-ins were all you got | **fixed** (v1.8.0: save the look on screen as your own filter, in a "Mine" tab, persisted) |

---

## Plan

Worked in priority order, one commit per group, each verified by a CI build.

- **Step 1 (P0):** fix the four real bugs — main-thread state writes, segmentation debounce/split, legacy write permission, permission recovery. ✅ v1.4.1
- **Step 2 (P1):** ML Kit availability messaging, filter-strip state, image size cap. ✅ v1.4.1
- **Step 3 (quality):** unit tests for the colour-matrix engine + a CI step that runs them. ✅ v1.4.1
- **Step 4 (P2):** faster separable blur (+ tests) and undo/redo in the image editor. ✅ v1.4.2
- **Step 5 (P2):** bitmap recycling in the editor; reduce camera rebind flicker. ✅ v1.5.1
- **Step 5b (UX):** filter chips render the actual filter on a real photo. ✅ v1.5.2
- **Step 6 (P2):** undo/redo in the video editor, on a shared + tested [EditHistory]. ✅ v1.5.3
- **Step 7 (feature):** video text/sticker overlays, baked into the export. ✅ v1.5.4 (transitions need a multi-clip timeline — not done)
- **Step 8 (feature):** the filter is baked into the recorded video. ✅ v1.6.0 (post-process on stop, not live — see the note in `RecordedVideoProcessor`)
- **Step 9 (feature):** multi-clip timeline — add, trim, reorder and join clips. ✅ v1.7.0 (the foundation transitions need)
- **Step 10 (feature):** save your own filters from the adjust sliders. ✅ v1.8.0
- **Step 11 (feature):** fade-to-black transitions between clips. ✅ v1.9.0 — Media3's
  `RgbMatrix` is evaluated per frame, so a real time-varying fade needs no custom shader.
- **Step 12 (distribution):** a Play-ready App Bundle. ✅ v1.10.0 — a `play` flavour that
  drops the self-updater and the install-packages permission, plus `PLAY.md`.
- **Step 13 (feature):** an app lock on the device biometric or PIN. ✅ v1.11.0
- **Step 14 (feature):** crossfade (needs a time-varying *alpha*, i.e. a hand-written GL
  shader); reverse; beauty and lenses into the recording; direct share targets.

Each step ends with a signed release so the fixes are installable.
