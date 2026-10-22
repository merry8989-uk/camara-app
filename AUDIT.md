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
| 17 | `editor/video/` | No transitions or reverse, and still single-clip | Feature gap — needs a multi-clip timeline. |
| 13 | `editor/` | No undo/redo | Feature gap — the single most-requested editing affordance. **fixed** (v1.4.2) |
| 14 | `camera/` | Live look isn't applied to the **recorded** video stream | Feature gap (needs a GL pipeline, not `RenderEffect`). |
| 15 | `editor/video/` | No undo/redo; no trim handles beyond the range slider | You could not take back a trim, filter or speed change. **undo/redo fixed** (v1.5.3) |
| 16 | `ui/components/FilterStrip.kt` | Filter chips were a colour gradient derived from the matrix, which says almost nothing about what the filter does | You had to try every filter to see it. **fixed** (v1.5.2: chips render the filter on a real photo) |

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
- **Step 8 (feature):** apply the live look to the recorded video stream (GL pipeline).

Each step ends with a signed release so the fixes are installable.
