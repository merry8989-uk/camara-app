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

## 3. Next up (planned)

1. **Recorded-video effects** — apply the live look to the recorded stream, not just
   the preview (needs a GL pipeline rather than `RenderEffect`).
2. **Undo/redo** — an edit stack in both editors.
3. **Video overlays** — text, stickers and transitions in the video editor.
4. **Custom filters** — build a look from the adjust sliders and save it as a preset.
5. **Release signing** — a proper keystore and a Play-ready AAB.
6. **Direct share targets** — one-tap share to specific apps.

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
