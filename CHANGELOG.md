# Changelog

All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](https://semver.org/) and follows the [Keep a Changelog](https://keepachangelog.com/) standard.

Alle nennenswerten Änderungen an diesem Projekt werden in dieser Datei dokumentiert.
Dieses Projekt folgt [Semantic Versioning](https://semver.org/lang/de/) und dem [Keep a Changelog](https://keepachangelog.com/de/1.0.0/)-Standard.

---

## [0.1.28] - 2026-11-10

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Geteilte Tablet-Ansicht entfernt: Einheitliche Vollbild-Tab-Navigation (Code, 3D-Ansicht, Customizer, Konsole) mit permanenter Navigationsleiste auf allen Bildschirmgrößen
- **EN:** Remove split tablet view: Unified full-screen tab navigation (Code, 3D View, Customizer, Console) with persistent bottom navigation bar across all screen sizes

## [0.1.27] - 2026-11-10

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Vollständige Unterstützung für use <...>, include <...>, import(...) und surface(...) inkl. rekursiver Abhängigkeiten und gebündelter MCAD-Standardbibliothek
- **EN:** Full support for `use <...>`, `include <...>`, `import(...)`, and `surface(...)` including recursive dependencies and bundled official OpenSCAD MCAD library

## [0.1.26] - 2026-11-10

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Dynamische Versionsauslesung via PackageManager zur Laufzeit & sauberer Clean-Build-Workflow
- **EN:** Dynamic runtime version resolution via PackageManager and clean-build release workflow

## [0.1.25] - 2026-11-10

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Zweisprachige In-App-Unterstützung (DE/EN) mit Umschaltung und Gerätesprachen-Standard
- **EN:** Bilingual in-app support (DE/EN) with language switcher and device default

## [0.1.24] - 2026-11-10

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Rechtliche Lizenzierung (GPLv2 & CGAL-Exception, Drittanbieter-Lizenzen), zweisprachige README & CHANGELOG (DE/EN) und Trademark-Hinweis
- **EN:** Legal licensing (GPLv2 & CGAL exception, third-party notices), bilingual README & CHANGELOG (DE/EN), and trademark disclaimer

## [0.1.23] - 2026-11-09

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Transparenz für `#` (Highlight-Modifikator) optimiert: Positive `#`-Objekte aus dem opaken Render-Pass ausgeschlossen, Backface-Culling und optimales Alpha-Blending in OpenGL ES aktiviert.
- **EN:** Fix `#` highlight modifier transparency: exclude positive `#` geometries from opaque pass, enable backface culling and optimal alpha blending in OpenGL ES.

## [0.1.22] - 2026-11-09

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Unterstützung für OpenSCAD-Modifikatoren im 3D-Viewport: `#` (Debug/Highlight mit Transparenz), `%` (Hintergrund), `!` (Wurzel/Isolieren) und `*` (Deaktivieren).
- **EN:** Full support for OpenSCAD modifiers in 3D viewport: `#` (highlight/debug with transparency), `%` (background), `!` (root/isolate), and `*` (disable).

## [0.1.21] - 2026-11-06

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Automatisches Update & Stabilitätsverbesserungen im Render- und UI-Workflow.
- **EN:** Automatic update & stability enhancements in render and UI workflow.

## [0.1.20] - 2026-11-05

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Ermöglicht das direkte Öffnen (VIEW/EDIT) sowie Teilen/Freigeben (SEND) von `.scad`- und `.stl`-Dateien aus Dateimanagern, Downloads und Messengern direkt in SCADroid.
- **EN:** Enable direct opening (VIEW/EDIT) and sharing (SEND) of `.scad` and `.stl` files from external file managers, downloads, and messenger apps directly into SCADroid.

## [0.1.19] - 2026-11-05

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Behebt das Zurückspringen der Schieberegler in der Parameter-Ansicht (Customizer): Parameter-Overrides und Min/Max-Grenzen bleiben beim Neuberechnen erhalten, Slider-Änderungen werden entprellt (300 ms Debounce) und direkt in den SCAD-Code übernommen.
- **EN:** Fix parameter slider bouncing in Customizer view: preserve overrides and min/max bounds during recalculation, debounce slider inputs (300 ms), and update SCAD code seamlessly.

## [0.1.18] - 2026-11-05

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Behebt den SyntaxError (`import.meta.url`) beim Laden von `openscad.js` im Android WebView, aktiviert das schnelle Manifold-Backend (`--backend=Manifold`) sowie Binär-STL-Transfer (`--export-format=binstl`), integriert LiberationSans-Schriftarten inkl. Fontconfig für `text()`, verhindert Abstürze bei Mehrfach-Renders durch gecachtes WebAssembly-Modul und entfernt den alten internen Fallback-Renderer vollständig.
- **EN:** Fix `import.meta.url` SyntaxError during `openscad.js` loading in Android WebView, activate fast Manifold backend (`--backend=Manifold`) and binary STL streaming (`--export-format=binstl`), bundle LiberationSans fonts with fontconfig for `text()`, prevent crashes across multiple renders via cached WebAssembly.Module, and remove legacy fallback renderer.

## [0.1.17] - 2026-11-05

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Zuverlässiges Script-Loading für OpenSCAD WASM Engine im WebView ohne CORS-Restriktionen, erweitertes Logging und transparente Konsolenanzeige.
- **EN:** Reliable script loading for OpenSCAD WASM engine in WebView without CORS restrictions, enhanced error logging, and transparent console message forwarding.

## [0.1.16] - 2026-11-05

### Geändert & Hinzugefügt / Changed & Added
- **DE:** OpenSCAD Core Integration: Offizieller OpenSCAD C++ Rechenkern (WASM) direkt in der App eingebunden für 1:1 Desktop-Berechnung & exakte STL-Geometrie.
- **EN:** OpenSCAD Core Integration: Official OpenSCAD C++ engine (WASM) bundled directly into the app for 1:1 desktop calculation parity and accurate STL geometry.

## [0.1.15] - 2026-11-05

### Geändert & Hinzugefügt / Changed & Added
- **DE:** CSG-Performance & Polygon-Explosion behoben: Schnelle Auswertung in unter 1 Sekunde, korrekte Wandgeometrie ohne Fehlschnitte, Bounding-Box Vorab-Checks und `polygon()`-Support.
- **EN:** CSG performance and polygon explosion resolved: fast evaluation in <1s, solid walls without intersecting artifacts, bounding-box pre-checks, and `polygon()` support.

## [0.1.14] - 2026-11-04

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Eindeutige Versionsanzeige im Menü und versionierte APK-Dateien (`SCADroid-v0.1.14.apk`) im Root und Release.
- **EN:** Explicit version display in the app menu and version-tagged APK artifacts (`SCADroid-v0.1.14.apk`) in root and release output.

## [0.1.13] - 2026-11-04

### Geändert & Hinzugefügt / Changed & Added
- **DE:** CSG-Engine Update: Vollständige Unterstützung für `if`/`else`, `minkowski`, `hull`, String-Variablen, lokale Zuweisungen und verschachtelte Modul-Hierarchien.
- **EN:** CSG engine update: comprehensive support for `if`/`else`, `minkowski`, `hull`, string variables, local assignments, and nested module hierarchies.

## [0.1.12] - 2026-11-04

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Behebe automatische Dummy-Würfel-Generierung bei leerem oder nicht-geometrischem OpenSCAD-Code.
- **EN:** Fix unexpected dummy cube generation for empty or purely mathematical/non-geometric OpenSCAD scripts.

## [0.1.11] - 2026-11-03

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Neues Modell mit minimalem Template und Bestätigungsdialog vor dem endgültigen Löschen.
- **EN:** New model workflow with clean minimal template and confirmation dialog prior to permanent project deletion.

## [0.1.10] - 2026-11-03

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Undo/Redo Tasten im Code-Editor und bereinigtes Menü ohne blauen Untertitel.
- **EN:** Undo and Redo toolbar buttons in code editor and clean top menu without awkward subtitle fragment.

## [0.1.9] - 2026-11-03

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Redundanten Vorschau-Button und Speichersymbol entfernt, Render als einzige zentrale Berechnungsaktion und STL-Export ausschließlich im Menü.
- **EN:** Removed redundant preview button and save icon; unified render action and streamlined STL export.

## [0.1.8] - 2026-11-03

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Prominente STL-Exportfunktion für den 3D-Druck im Menü, 3D-Viewport-HUD und Floating Action Button.
- **EN:** Prominent STL export feature for 3D printing in top menu, 3D viewport HUD, and floating action button.

## [0.1.7] - 2026-11-03

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Header-Titel ohne Zeilenumbruch fixiert und fehlertolerantes Syntax-Highlighting gegen ungeschlossene Blockkommentare.
- **EN:** Fixed single-line header title wrapping and fault-tolerant syntax highlighting against unclosed block comments.

## [0.1.6] - 2026-11-03

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Header-Titel bereinigt, lebendiges OpenSCAD CAD Syntax-Farbschema und 2-Finger-Touch-Pan für die 3D-Kamera.
- **EN:** Polished header title, vibrant OpenSCAD CAD syntax color palette, and 2-finger touch panning for 3D camera.

## [0.1.5] - 2026-11-03

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Saubere TopAppBar (abgeschnittenes Textfragment entfernt), Darcula CAD Farbschema für OpenSCAD-Code und stufenloser 2-Finger-Touch-Pan für die 3D-Ansicht.
- **EN:** Clean TopAppBar without truncated subtitle text, Darcula CAD color scheme for OpenSCAD syntax, and smooth 2-finger touch panning for 3D camera pivot.

## [0.1.4] - 2026-11-03

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Vollständiges OpenSCAD Syntax-Highlighting, dynamische Auto-Completion-Leiste über der Tastatur, Befehle-Dropdown, Direktwechsel zur 3D-Ansicht bei Render und SAF-Speicherdialog.
- **EN:** Comprehensive OpenSCAD syntax highlighting, dynamic on-screen auto-completion bar, command dropdown menu, instant viewport tab-switching on render, and SAF file save dialog.

## [0.1.3] - 2026-11-03

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Persistente Code-Speicherung (Auto-Save & Öffnen/Speichern von `.scad`), Symbolleiste mit Fokus-Commit, Auto-Completion bei Tastatureingabe mit Vorschlags-Button und automatischer Klammer-Paarung.
- **EN:** Persistent code storage (auto-save and open/save `.scad` files), functional symbol toolbar with focus preservation, typing completion suggestions, and auto-pairing brackets.

## [0.1.2] - 2026-11-03

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Erweiterte OpenSCAD-Kompatibilität: Mathematische Ausdrücke (Grad-Trigonometrie, Potenzen, Konstanten), Vektor-Variablen & Indizierung, CSG difference/union/intersection mit verketteten Transformationen, Mirror, Color, For-Schleifen und User-Module.
- **EN:** Extended OpenSCAD compatibility: mathematical functions (degree trigonometry, powers, constants), vector variables and indexing, CSG boolean operations with chained transformations, mirror, color, loops, and user modules.

## [0.1.1] - 2026-11-03

### Geändert & Hinzugefügt / Changed & Added
- **DE:** Echtes 3D-CSG-Rendering mit OpenGL ES 3.0, CAD-Lines App-Icon und vollwertiges Dark Theme.
- **EN:** Real 3D CSG rendering with OpenGL ES 3.0, technical lines app launcher icon, and full CAD dark theme.

## [0.1.0] - 2026-11-03

### Hinzugefügt / Added
- **DE:** Initiales Projekt-Setup für SCADroid (OpenSCAD auf Android), Konfiguration der App-ID `de.fisch981.scadroid`, Build- und Push-Workflow (`push.ps1`) und Basis-Architektur mit Jetpack Compose UI.
- **EN:** Initial project setup for SCADroid (OpenSCAD on Android), application ID `de.fisch981.scadroid`, automated build and release workflow (`push.ps1`), and base architecture with Jetpack Compose UI.
