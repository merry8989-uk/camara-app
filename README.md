# Camara 📸

**An open-source, all-in-one camera and editing app for Android.**
Shoot, filter, beautify, add stickers, read from a teleprompter, then edit photos and
videos — all in one app, built with Kotlin, Jetpack Compose and CameraX.

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

### Filters
- **40+ built-in looks** across families: Classic, Warm, Cool, Mono, Cinematic, Snap-style
  and social-feed styles
- Real-time live preview on Android 12+ (GPU `RenderEffect`) and baked into the saved file
  so **what you see is what you get**
- One shared colour-matrix engine powers the camera preview, the image editor and the
  video exporter

### Stickers
- Six emoji sticker packs
- Place, drag, pinch to scale, twist to rotate — on the camera preview *and* in the editor
- Baked into the exported photo at the right position, size and rotation

### Beauty / retouch
- Skin smoothing, brighten, warmth, skin tone, sharpen
- **Face slimming** and **eye enlargement** via a real vertex-mesh warp
  (`Canvas.drawBitmapMesh`), not a crop or scale

### Teleprompter
- Paste or type a script, set speed and text size, hit play
- Optional **mirror mode** for beam-splitter rigs

### Image editor
- Crop to aspect (Free, 1:1, 4:5, 3:4, 16:9, 9:16)
- Rotate 90° and flip H/V
- Manual adjust: brightness, contrast, saturation, warmth
- Filters, stickers, **text overlays** (drag / scale / rotate) and **freehand drawing**
- Frame styles: White, Rounded, Film, Vignette
- Non-destructive preview, flattened on export to the gallery

### Video editor
- Frame-accurate **trim** with a range slider
- Apply any filter to video via Media3 `RgbMatrix`
- Export to H.264 MP4 with a live progress indicator

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
| Video | Media3 (ExoPlayer, Transformer, Effect) |
| Colour pipeline | Custom 4×5 colour-matrix engine + `RenderEffect` / `RgbMatrix` |
| Min / target SDK | 24 / 34 |
| Build | Gradle 8.7, AGP 8.5.2, Kotlin 2.0.20 |

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

> The Gradle wrapper is included, so `./gradlew` works out of the box. If your
> environment blocks the wrapper, run `gradle wrapper` once to regenerate it.

---

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

- [ ] Non-destructive edit history with undo/redo
- [ ] Freeform (drag-handle) crop
- [ ] Video speed and reverse effects
- [ ] Video text/sticker overlays and transitions
- [ ] AR face-tracking masks and lenses
- [ ] Live filter on the recorded video stream (not just the preview)
- [ ] Custom filter creation and user presets
- [ ] Save/share sheet and direct social export

---

## 🤝 Contributing

Issues and pull requests are welcome. If you add a filter, drop it into
`FilterPresets.ALL`; if you add a tool, follow the existing package layout.

## 📄 License

MIT — see [LICENSE](LICENSE).
