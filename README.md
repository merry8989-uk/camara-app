# Camara 📸

**An open-source, all-in-one camera and editing app for Android.**
Shoot, filter, beautify, try on AR lenses, add stickers, read from a teleprompter,
then edit photos and videos — all in one app, built with Kotlin, Jetpack Compose and CameraX.

> **Note on the name.** "Camara" is an original app. It is *not* a copy of, and contains
> no code from, B612, Snapchat, Instagram, Facebook or Meta's editing tools. It is an
> independent implementation of a similar *feature set* — filters, stickers, beauty
> tools, teleprompter, image editing and video editing — written from scratch.

---

## ✨ Features

### Camera (CameraX)
- Live preview with pinch-to-zoom, tap-to-focus and front/back switching
- **Photo** and **Video** capture, with in-app video recording
- Flash modes: off / on / auto
- Self-timer: off / 3s / 10s
- Rule-of-thirds grid overlay
- **Camera extensions**: Auto, Portrait (bokeh), HDR, Night and Face Retouch — used
  automatically when the device/OEM supports them, with a safe fallback when not

### AR lenses
- **On-device face detection with contours** (ML Kit), so lenses follow the real face shape
- Pieces are anchored to **landmarks**, sized in units of the **inter-ocular distance**
  and rotated by the **eye-line angle** — they track the face instead of drifting with
  the bounding box
- **~28 lenses**: eyewear (shades, round, heart, lashes), animals (cat, fox, puppy,
  bunny, panda), makeup (lipstick, blush, glam, face paint), headwear (crown, halo,
  party hat, flower crown, beanie) and fun (hearts, fire, angel, star, moustache)
- **Contour-following** pieces: lipstick traces the lip contour, lashes trace the eye
  contour, face paint fills the face oval
- Mirrored correctly on the selfie camera, and **baked into the captured photo**
- Gracefully disables itself on devices that can't bind face analysis alongside all other
  camera use cases

### Filters
- **~75 built-in looks** across **ten categories**: Original, Portrait, Classic, Warm,
  Cool, Mono, Cinematic, Vintage, Snap and Social
- **Strength control** — every look blends from 0–100%, so a filter can be dialled in
  instead of slammed on (implemented by lerping the matrix toward identity)
- Real-time live preview on the camera on Android 12+ (GPU `RenderEffect`) and baked into
  the saved file so **what you see is what you get**
- The preview runs in `ImplementationMode.COMPATIBLE` (TextureView) because a SurfaceView
  is composited as a separate layer and ignores `RenderEffect` — with the default
  `PERFORMANCE` mode the live filter silently does nothing
- One shared colour-matrix engine powers the camera preview, the image editor and the
  video exporter

### Stickers
- Six emoji sticker packs
- Place, drag, pinch to scale, twist to rotate — on the camera preview *and* in the editor
- Baked into the exported photo at the right position, size and rotation

### Beauty / retouch — **face only**
- Retouch is confined to the **face**, not the whole frame: the face contour becomes a
  feathered mask and the retouched image is composited back through it, so hair,
  clothing and background are untouched
- **Enhance face** — one-tap natural retouch preset
- Smooth, brighten, warmth and skin tone **preview live on the camera**
- **Face slimming** and **eye enlargement** via a real vertex-mesh warp
  (`Canvas.drawBitmapMesh`), not a crop or scale

### Portrait depth (real bokeh)
- On-device **subject segmentation** separates the subject from the background
- **Tap to pick your subject** — the blob under your finger stays sharp and everything
  else is blurred; without a tap the largest subject is kept
- Soft, feathered transition between subject and background, with a blur-strength slider
- Previewed live in the image editor and baked into a camera capture

### Teleprompter
- **Transparent, draggable overlay that sits on top of the camera preview**, so you can
  read your script and watch yourself recording at the same time
- Play/pause, speed, text size, **mirror mode** and panel **opacity**, adjustable from the
  overlay itself; drag the handle to move it anywhere on screen
- A full-screen editor for longer scripts — the script and settings are shared with the
  overlay and persisted between sessions

### Image editor
- **Freeform drag-crop** — drag corners and edges, move the frame, or lock to 1:1, 4:5,
  3:4, 16:9 and 9:16
- Rotate 90° and flip H/V
- Manual adjust: brightness, contrast, saturation, warmth
- Filters, stickers, **text overlays** (drag / scale / rotate) and **freehand drawing**
- Frame styles: White, Rounded, Film, Vignette
- Non-destructive preview, flattened on export to the gallery

### Video editor
- Frame-accurate **trim** with a range slider
- **Speed control** — 0.5x to 3x slow-motion and fast-forward, with live preview and a
  live estimate of the resulting clip length (Media3 `SpeedProvider`)
- Apply any filter to video via Media3 `RgbMatrix`
- Export to H.264 MP4 with a live progress indicator

### Sharing
- Share from the image editor, the video editor or any gallery tile
- Uses the system share sheet (`ACTION_SEND`) so any installed app can receive it
- Exports are written to the shared gallery first, then handed over with a read grant

### Gallery
- Grid of captured photos and videos from the shared media store
- Tap any item to open it in the right editor

---

## 🧱 Tech stack

| Area | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Camera | CameraX (core, camera2, lifecycle, view, video, extensions, effects) |
| Navigation | Navigation Compose |
| Video | Media3 1.11 (ExoPlayer, Transformer, Effect) |
| Face / AR | ML Kit **unbundled** face detection + subject segmentation (models via Play Services) |
| Colour pipeline | Custom 4×5 colour-matrix engine + `RenderEffect` / `RgbMatrix` |
| SDK | min 24 · target 34 · compile 36 |
| Build | Gradle 8.7, AGP 8.5.2, Kotlin 2.2.10 |

---

## 🚀 Getting started

### Prerequisites
- Android Studio (Ladybug or newer) **or** the Android SDK command-line tools
- JDK 17
- An Android device or emulator running **API 24+**

### Build & run
```bash
git clone https://github.com/<your-username>/camara-app.git
cd camara-app

# Build a debug APK
./gradlew assembleDebug

# Or install straight onto a connected device
./gradlew installDebug
```

The debug APK lands in `app/build/outputs/apk/debug/app-debug.apk`.
You can also just open the folder in Android Studio and hit **Run**.

### Grab a prebuilt APK
Every push to `main` builds a debug APK on GitHub Actions. Open the
**Actions** tab, pick the latest **Android CI** run, and download the
`camara-debug-apk` artifact from the run summary.

> The build compiles against **API 36** because Media3 1.10+ requires it, and uses
> **Kotlin 2.2.10** to match Media3's Kotlin metadata. Both are set in
> `app/build.gradle.kts`, `build.gradle.kts` and `gradle.properties`.

> The Gradle wrapper is included, so `./gradlew` works out of the box. If your
> environment blocks the wrapper, run `gradle wrapper` once to regenerate it.

---

## 📦 Download size

The APK is **~8 MB**, down from ~80 MB. There is no OBB and no expansion file.

Where the bytes went:

| | v1.2.0 | v1.3.1 |
| --- | --- | --- |
| Unminified dex | ~68 MB | minified by R8 |
| ML Kit native libraries | ~48 MB (2 ABIs) | ~0.1 MB |
| Bundled ML models | ~5 MB | 0 |
| **Total APK** | **~80 MB** | **4.94 MB** (signed release) |

Two changes did it:

1. **Unbundled ML Kit.** Face detection and subject segmentation have a "thin" variant
   whose models live in Google Play Services instead of inside the APK — face detection
   drops from ~6.9 MB to ~0.8 MB of app size, and subject segmentation removes tens of
   MB of native inference code in favour of ~200 KB. Same API, same behaviour; the model
   is fetched by Play Services (at install time, via the manifest declaration).
2. **R8 minification + resource shrinking**, which collapses the unminified dex.

### Why not an OBB?

An OBB (expansion file) is a Play Store mechanism — Play downloads it at install; the app
cannot fetch its own. More importantly it cannot carry what actually made this APK big:

- **Native `.so` libraries** can't be reliably loaded from an OBB. From Android 10 the
  platform blocks `dlopen` of libraries from app-writable storage, so shipping ML inference
  code in an OBB and loading it at runtime is not supported.
- **ML Kit models** are read from the APK's assets. There is no supported way to point the
  SDK at a downloaded file (ML Kit has explicitly said self-served model downloads are not
  supported).

So the OBB route would have been fragile and would not have removed the big items. The
unbundled-model route reaches the same goal — a small APK whose heavy models are fetched
by the platform — using a mechanism Google supports.

**Trade-off:** the first use of face lenses or portrait depth needs Google Play Services
and a one-time model download. Without them the app still runs; those two features just do
nothing until the model lands.

## 🔑 Signing

**No signing material is committed to this repository.** The keystore lives in GitHub
Actions secrets and is decoded only inside the CI runner.

That matters because Android refuses to install an APK as an update unless it carries the
same certificate as the installed copy. CI runners start empty, so if no key is provided
they generate a fresh one per run — a different certificate every time, forcing an
uninstall before each install.

| Secret | Contents |
| --- | --- |
| `CAMARA_KEYSTORE_BASE64` | the `.p12` keystore, base64-encoded |
| `CAMARA_STORE_PASSWORD` | keystore password |
| `CAMARA_KEY_PASSWORD` | key password |

Locally, set the same environment variables or drop your own keystore at
`keystore/camara-dev.p12` (git-ignored). If no keystore is found the build still succeeds,
just unsigned.

CI builds `assembleRelease` and fails if the v1 signature is missing, so a broken signing
setup can't slip through.

> **History note:** an earlier version committed a *throwaway* keystore to the repo. The
> file has been removed from the tree, but it still exists in git history — deleting a file
> does not rewrite past commits. That key is no longer used by any build and should be
> treated as public. The current key was generated afterwards and has never been committed.
> See `keystore/README.md`.

## 🔄 Content catalogue (update content without an app release)

`content/catalog.json` is a **remote catalogue** of filters, sticker packs and AR lenses.
The app fetches it at runtime (Content button in the camera top bar → *Sync now*), caches
it, and merges it with its built-in content:

- a remote entry whose `id` matches a built-in **replaces** it
- a new `id` is **appended**
- everything falls back to the built-ins, so the app works fully offline

Edit the JSON, push, and every install picks it up on the next sync — no new APK.

The schema is documented at the top of the file. Example:

```json
{
  "filters": [
    { "id": "aurora", "name": "Aurora", "category": "COOL",
      "matrix": [0.95,0.05,0.05,0,4, 0.02,1.08,0.02,0,6, 0.05,0.05,1.15,0,10, 0,0,0,1,0] }
  ],
  "stickerPacks": [ { "name": "Remote Pack", "stickers": ["🛸", "🌌"] } ],
  "lenses": [
    { "id": "vapor", "name": "Vapor", "art": [
      { "type": "drawn", "kind": "ROUND_GLASSES", "anchor": "EYE_MID", "size": 2.5 }
    ] }
  ]
}
```

> **Note on size:** filters are 20 floats of data, so they cost essentially nothing in the
> APK either way. The catalogue is about *updating content without a release*, not about
> shrinking the download — that is handled by the unbundled models and R8 above.
>
> **Note on privacy:** the catalogue lives in this repo, so it is public. Making it truly
> private would mean shipping a token inside the app, which anyone can extract — the usual
> answer is a tiny backend that holds the secret. For content like filters, public is fine.

## 🗂️ Project structure

```
app/src/main/java/com/camara/app/
├── MainActivity.kt            # entry point
├── CamaraApp.kt               # Application
├── camera/                    # CameraX controller + camera screen + extensions
│   ├── CameraController.kt
│   ├── CameraScreen.kt
│   ├── CamaraExtension.kt
│   └── CameraExtensionResolver.kt
├── filters/                   # colour-matrix engine, presets, swatches, live effect
│   ├── ColorMatrixMath.kt
│   ├── FilterPresets.kt
│   ├── FilterRenderer.kt
│   └── LiveFilterEffect.kt
├── beauty/                    # beauty settings + renderer (blur blend, mesh warp)
├── stickers/                  # sticker model, gesture layer, renderer
├── teleprompter/              # teleprompter screen
├── editor/
│   ├── image/                 # crop, rotate, adjust, text, draw, frames + exporter
│   └── video/                 # Media3 Transformer trim + filter + export
├── gallery/                   # media-store repository + grid screen
├── ui/                        # theme, navigation, shared components
└── util/                      # media store, bitmaps, permissions, thumbnails
```

---

## 🎨 How the filter engine works

Every filter is a **4×5 colour matrix** (20 floats). Small primitives — saturation,
contrast, brightness, temperature, tint, channel gain, sepia — are chained with matrix
multiplication in `ColorMatrixMath`, so new looks are one line of code:

```kotlin
f("juno", "Juno", FilterCategory.CLASSIC,
    M.saturation(1.35f), M.contrast(1.10f), M.temperature(0.08f))
```

That single matrix is reused everywhere:

- **Live preview** — wrapped in an `android.graphics.RenderEffect` colour filter (API 31+)
- **Saved photo** — applied with `ColorMatrixColorFilter` on the export canvas
- **Video** — transposed to the 4×4 column-major form Media3's `RgbMatrix` expects

---

## 🗺️ Roadmap

- [x] Freeform (drag-handle) crop
- [x] Video speed effects
- [x] AR face-tracking masks and lenses
- [x] Share sheet
- [x] Undo/redo in both editors
- [x] Video text and sticker overlays, with per-overlay timing
- [x] Filter baked into the recorded video (applied when recording stops)
- [x] Remote content catalogue (filters / stickers / lenses synced without an app release)
- [x] Small APK via unbundled ML Kit + R8 (no OBB needed)
- [x] Stable signing so builds update in place
- [x] Signing key moved to repository secrets (never committed)
- [x] Unit tests for the filter maths, blur, undo history and frame fitting
- [ ] Beauty and AR lenses baked into the recorded video (needs per-frame face tracking)
- [x] Multi-clip timeline — add, trim, reorder and join clips into one video
- [x] Fade-to-black transitions between clips
- [ ] Crossfade (needs a hand-written GL shader) and reverse
- [x] Play-ready App Bundle (a `play` flavour with the self-updater removed)
- [x] Save your own filters (the look on screen, kept in a "Mine" tab)
- [ ] Direct share targets for specific social apps

See [`PLAY.md`](PLAY.md) for how the Play bundle is built and signed.

---

## 🤝 Contributing

Issues and pull requests are welcome. If you add a filter, drop it into
`FilterPresets.ALL`; if you add a tool, follow the existing package layout.

## 📄 License

MIT — see [LICENSE](LICENSE).
