# SCADroid 📐

[![License: GPL-2.0](https://img.shields.io/badge/License-GPL%202.0%20(CGAL%20exception)-blue.svg)](LICENSE)
[![Platform: Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Engine: OpenSCAD WASM](https://img.shields.io/badge/Engine-OpenSCAD%20WASM%20%2B%20Manifold-orange.svg)](https://openscad.org)

**[English](#english) | [Deutsch](#deutsch)**

---

<a name="english"></a>
## English

**SCADroid** is a modern native Android port of **OpenSCAD** — the popular script-only 3D CAD modeling software. It brings solid CSG modeling, real-time code editing, interactive 3D rendering, and direct 3D-printable STL export to mobile devices and tablets.

### Features
- **Official OpenSCAD Core Engine**: Bundled WebAssembly build with the ultra-fast modern **Manifold CSG backend** (`--backend=Manifold`) for fast, robust 3D geometry evaluation without crashes or polygon degradation.
- **Full Support for OpenSCAD Syntax & Modifiers**: Native handling of all standard CSG operations, math routines, loops, modules, user variables, as well as modifier symbols: `#` (highlight/debug with transparency), `%` (transparent background), `!` (root/isolate), and `*` (disable).
- **Embedded 3D Font Engine**: Bundled `LiberationSans` font and fontconfig integration enabling 3D text generation directly via `text()`.
- **Advanced Code Editor**: Powered by `sora-editor` with dedicated OpenSCAD syntax highlighting, bracket matching, auto-completion, quick CAD symbol bar, and handy **Undo / Redo buttons** right in the top toolbar.
- **Interactive 3D Touch Viewport**: High-performance OpenGL ES 3.0 renderer with intuitive multi-touch gestures (1-finger orbit rotation, 2-finger pan & zoom, center reset).
- **One-Tap STL Export for 3D Printing**: Instant generation of standard binary STL files ready for 3D slicers (Cura, PrusaSlicer, Bambu Studio) accessible from the top menu, viewport HUD, and floating action button.
- **OpenSCAD Customizer**: Automatic UI control generation (sliders, drop-downs, checkboxes) parsed directly from SCAD variable annotations with debounced real-time preview updates.
- **File Management & Sharing**: Seamless integration with Android Storage Access Framework (SAF), file sharing (SEND/SHARE intents), and direct opening of `.scad` and `.stl` files from external file managers and messaging apps.

### Architecture & Tech Stack
- **UI Framework**: Modern Jetpack Compose with Material 3 Dark Theme designed for technical CAD workflows.
- **Editor**: Sora Editor (`io.github.Rosemoe.sora-editor:editor`) (LGPL-2.1).
- **Core Engine**: OpenSCAD WebAssembly runtime (`assets/openscad/openscad.js`) executed in a sandboxed, hardware-accelerated headless WebView with Base64/binary bridge.
- **3D Renderer**: Native OpenGL ES 3.0 SurfaceView with custom shaders and binary STL mesh loader.

### Building from Source
Ensure you have the Android SDK (API 35) and JDK 17 installed.

```bash
# Clone the repository
git clone https://github.com/fisch981/SCADroid.git
cd SCADroid

# Build debug or release APK
./gradlew assembleDebug
# or
./gradlew copyReleaseApkToRoot
```

### License & Legal Information
- **SCADroid** is licensed under the **GNU General Public License v2 (GPL-2.0)** with the **CGAL linking exception**, consistent with the upstream OpenSCAD project. See [LICENSE](LICENSE).
- All third-party libraries, fonts, and dependencies are documented in [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).
- **Trademark Disclaimer**: *OpenSCAD* is a trademark of its respective copyright holders (Marius Kintel, Clifford Wolf, and contributors). SCADroid is an independent open-source project and is not affiliated with, sponsored by, or endorsed by the OpenSCAD development team.

---

<a name="deutsch"></a>
## Deutsch

**SCADroid** ist die native Android-Portierung von **OpenSCAD** – der beliebten script-basierten 3D-CAD-Modellierungssoftware. Sie bringt solide CSG-Modellierung, Echtzeit-Code-Bearbeitung, interaktives 3D-Rendering und direkten STL-Export für den 3D-Druck auf Smartphones und Tablets.

### Features
- **Offizieller OpenSCAD Rechenkern**: Integrierte WebAssembly-Engine mit modernem, ultra-schnellem **Manifold-Backend** (`--backend=Manifold`) für exakte und fehlerfreie 3D-Geometrie-Berechnungen in Sekundenbruchteilen.
- **Unterstützung für OpenSCAD-Syntax & Modifikatoren**: Volle Unterstützung aller CSG-Befehle, mathematischer Funktionen, Schleifen, User-Module und der typischen Modifikator-Symbole: `#` (Debug/Highlight mit Transparenz), `%` (Transparenter Hintergrund), `!` (Wurzel/Isolieren) und `*` (Deaktivieren).
- **Integrierte Schriftarten-Engine**: Gebündelte `LiberationSans`-Schriftart für die direkte 3D-Texterzeugung mittels `text()`.
- **Leistungsstarker Code-Editor**: Integrierter `sora-editor` mit maßgeschneidertem OpenSCAD-Syntax-Highlighting, Klammervervollständigung, dynamischer Symbolleiste und bequemen **Undo/Redo-Tasten** direkt in der Werkzeugleiste.
- **Interaktiver 3D-Touch-Viewport**: OpenGL ES 3.0 Rendering mit intuitiven Touch-Gesten (1 Finger für Orbit-Drehung, 2 Finger für Kamera-Pan & Zoom).
- **STL-Export für den 3D-Druck**: Sofortige Erzeugung von binären STL-Dateien direkt für Slicer-Software (Bambu Studio, PrusaSlicer, Cura), erreichbar über das Hauptmenü, das Viewport-HUD und den Floating Action Button.
- **OpenSCAD Customizer**: Automatische Generierung von GUI-Steuerelementen (Schieberegler, Dropdowns, Checkboxen) aus SCAD-Variablen-Annotationen mit entprellter Live-Aktualisierung.
- **Datei- & Freigabe-Verwaltung**: Nahtloses Öffnen und Speichern lokaler Dateien über das Storage Access Framework (SAF) sowie direktes Öffnen (VIEW/EDIT) und Teilen (SEND) von `.scad`- und `.stl`-Dateien aus externen Dateimanagern und Messengern.

### Lizenz & Rechtliche Hinweise
- **SCADroid** ist lizenziert unter der **GNU General Public License v2 (GPL-2.0)** mit der **CGAL-Ausnahmegenehmigung**. Siehe [LICENSE](LICENSE).
- Detaillierte Lizenzangaben und Quellcode-Links aller Drittanbieter-Komponenten befinden sich in [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).
- **Markenhinweis**: *OpenSCAD* ist eine geschützte Marke der jeweiligen Urheber (Marius Kintel, Clifford Wolf und Mitwirkende). SCADroid ist ein unabhängiges Open-Source-Projekt und steht in keiner offiziellen Verbindung zum OpenSCAD-Entwicklerteam.
