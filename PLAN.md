# Camara — work plan

A running plan for the app: what is done, what is outstanding, and how the
remaining work is broken into parts.

Status key: **done** · **in progress** · **planned**

---

## 1. Outstanding work at the start of this round

These were the known gaps before v1.2.0:

| # | Item | Status |
| --- | --- | --- |
| 1 | Beauty applied globally, not to the face | **done** (v1.2.0) |
| 2 | No colour-enhancement blending / filter strength | **done** (v1.2.0) |
| 3 | No real portrait bokeh, no subject selection | **done** (v1.2.0) |
| 4 | AR lenses anchored to the bounding box — drifty | **done** (v1.2.0) |
| 5 | Filter library small, flat, no categories | **done** (v1.2.0) |
| 6 | Live lens/filter not applied to *recorded* video | planned |
| 7 | No undo/redo in the editors | planned |
| 8 | Video: no text/sticker overlays, no transitions, no reverse | planned |
| 9 | No custom filters or saved user presets | planned |
| 10 | No release signing (Play-ready AAB) | planned |
| 11 | No direct share targets for specific apps | planned |
| 12 | APK was ~80 MB | **done** (v1.3.0, now ~5 MB) |
| 13 | No way to update content without a new APK | **done** (v1.3.0, remote catalogue) |
| 14 | Every build was signed with a different key — updates needed an uninstall | **done** (v1.3.1) |

---

## 2. What v1.2.0 changes, part by part

### Part 1 — Filter engine
- [x] Ten categories: Original, Portrait, Classic, Warm, Cool, Mono, Cinematic,
      Vintage, Snap, Social
- [x] Library grown from 42 to ~75 looks
- [x] **Strength / blend control**: a filter is lerped toward identity
      (`Filter.matrixAt`), so any look can be dialled from 0–100%
- [x] Category tabs in the filter browser
- [x] One shared matrix drives the live preview, the saved photo and video

### Part 2 — Face-only beauty
- [x] Face detection on the still image (`FaceMasker.detectFace`)
- [x] Soft-edged face mask from the real face contour, falling back to an ellipse
- [x] Beauty (smoothing + grading) confined to the mask, composited back through it
      (`FaceBeauty` + `MaskCompositor`)
- [x] Colour enhancement blending: the grade is blended, not slammed on
- [x] One-tap **Enhance face** preset

### Part 3 — Portrait depth (real bokeh)
- [x] On-device selfie segmentation (`segmentation-selfie`) → confidence map
- [x] Confidence → smooth alpha ramp, feathered edges
- [x] **Tap to select the subject**: keep the connected blob under the tap, drop
      the rest; largest blob when there is no tap
- [x] Background blurred, subject sharp, blended through the mask
- [x] Strength slider; live preview in the editor, baked on capture from the camera

### Part 4 — Realistic AR lenses
- [x] Face contours enabled (face oval, eyes, lips, cheeks, ears)
- [x] Lenses anchored to **landmarks**, not the bounding box
- [x] Sized in units of the inter-ocular distance (stable as the face moves)
- [x] Rotated by the **eye-line angle** (stable as the head tilts)
- [x] Contour-following pieces: lipstick, lashes, face paint
- [x] Lens set grown from 15 to ~28 (eyewear, animals, makeup, headwear, fun)

### Part 5 — Wiring
- [x] `PhotoProcessor`: one still-image pipeline (geometry → face beauty → colour →
      portrait → lens → overlays), used by both the camera and the editor
- [x] Camera: filter strength, Portrait panel, tap-to-select on the preview
- [x] Editor: filter strength, Portrait tool with live bokeh preview, tap-to-select

---

## 2b. What v1.3.0 changes

### Part 6 — APK size (~80 MB → ~8 MB)
- [x] ML Kit switched to the **unbundled** variants: face detection and subject
      segmentation models now live in Google Play Services instead of the APK
      (~48 MB of native libraries and ~5 MB of models removed)
- [x] **R8 minification + resource shrinking**, collapsing the unminified dex (~68 MB)
- [x] Manifest declares `com.google.mlkit.vision.DEPENDENCIES` so Play Services can fetch
      the models at install time
- [x] ABIs limited to arm64-v8a + armeabi-v7a

### Part 7 — Remote content catalogue
- [x] `content/catalog.json` in the repo: filters, sticker packs and AR lenses
- [x] `ContentStore` fetches, caches and merges remote content over the built-ins,
      with full offline fallback
- [x] Content dialog in the camera top bar: source URL, counts, last sync, *Sync now*
- [x] Filters, stickers and lenses all read through the store

### Why not an OBB

An OBB is a Play Store mechanism (Play downloads it; the app can't), and it cannot carry
what made the APK big:

- Native `.so` files can't be `dlopen`ed from app-writable storage on Android 10+, so ML
  inference code can't be shipped in an OBB and loaded at runtime.
- ML Kit reads its models from the APK's assets; there is no supported way to point it at a
  downloaded file (Google has said self-served model downloads are not supported).

The unbundled-model route achieves the same outcome with a supported mechanism.

**Trade-off:** first use of face lenses / portrait depth needs Play Services and a one-time
model download. Everything else works offline.

### Part 8 — Stable signing (v1.3.1)
- [x] Committed a PKCS#12 signing keystore (`keystore/camara-dev.p12`), generated without a
      JDK (this sandbox has none) using Python's `cryptography`
- [x] Wired it into **both** build types so local and CI builds share one certificate
- [x] CI now builds a **signed release APK** and verifies the v1 signature exists
- [x] Certificate fingerprint verified programmatically against the keystore

**Why it was broken:** CI runners start empty, so Gradle generated a fresh debug keystore
per run. Different certificate → Android rejects the update → uninstall required.

**Caveat:** the committed key is deliberately not secret (see `keystore/README.md`).

## 3. Next up (planned)

1. **Beauty and lenses in recordings** — the filter is baked in now; beauty and AR lenses
   still only appear on the preview, because both need per-frame face tracking. A real-time
   GL pipeline (`CameraEffect`) would do it, but it switches off Camera Extensions.
2. **Video transitions and reverse** — both need a multi-clip timeline.
3. **Custom filters** — build a look from the adjust sliders and save it as a preset.
4. **Play-ready App Bundle** — the keystore is done; `bundleRelease` is not wired yet.
5. **Direct share targets** — one-tap share to specific apps.

---

## 4. Known limitations (honest list)

- Beauty **preview** on the camera is an approximation: the colour part and a capped
  blur render live, while face slimming, eye enlargement and sharpen are applied to
  the captured photo. In the editor, beauty is applied on export.
- Portrait depth is baked into the photo, not previewed live on the camera (per-frame
  segmentation plus a blur pass is too heavy for the preview path). The editor *does*
  preview it.
- Lens tracking uses the face box, landmarks and contours — not a dense 3D face mesh.
  It is stable but not as precise as a mesh-based AR system.
- `segmentation-selfie` and `face-detection` are ML Kit **beta/stable** on-device
  models; they add roughly 10 MB to the APK.
- Nothing has been verified on physical hardware by the author — see the release notes.
- Baking the filter into a recording is a **post-process**: the clip is written unfiltered
  and the look is applied in one pass when recording stops, so there is a short pause
  afterwards. It is not a live effect on the recording stream.
