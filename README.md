Motion Andre

A clean room motion graphics editor for Android, written from scratch in Kotlin and Jetpack Compose.

Features
- New project dialog: aspect ratio, resolution, frame rate, duration, background
- Rectangle, circle, triangle, star, hexagon, plus and text layers
- Color and fill: none, solid, two color gradient, hex input, alpha
- Border, blending modes and opacity
- Move and transform: position, rotation, scale and skew with numeric fields and a drag pad
- Keyframes and easing: linear, in, out, in-out, bounce, hold
- Timeline with scrubbing, visibility toggles, draggable clip ends and keyframe markers
- Layer menu: flip, fit, fill, stretch, reorder and convert to outline
- Drag layers directly on the preview
- Undo and redo
- MP4 export through MediaCodec to Movies/MotionAndre
- Project autosave

Build
Open in Android Studio, or run `gradle :app:assembleRelease` with Gradle 8.9 and JDK 17.

CI
Every push builds a release APK, signs it, verifies the signature, uploads it as an artifact and publishes it as a prerelease.
Set KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS and KEY_PASSWORD as repository secrets to use your own key.

Icons
All icons are empty vector placeholders in app/src/main/res/drawable.
