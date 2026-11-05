# Changelog

Alle nennenswerten Änderungen an diesem Projekt werden in dieser Datei dokumentiert.
Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/),
und dieses Projekt folgt [Semantic Versioning](https://semver.org/lang/de/).

---

## [0.1.18] - 2026-11-05

### Geändert / Hinzugefügt
- Behebt den SyntaxError (import.meta.url) beim Laden von openscad.js im Android WebView, aktiviert das schnelle Manifold-Backend (--backend=Manifold) sowie Binaer-STL-Transfer (--export-format=binstl), integriert LiberationSans-Schriftarten inkl. Fontconfig fuer text(), verhindert den Absturz bei Mehrfach-Renders durch gecachtes WebAssembly.Module und entfernt den fehlerhaften internen Fallback-Renderer vollstaendig.

## [0.1.17] - 2026-11-05

### Geändert / Hinzugefügt
- Zuverlaessiges Script-Loading fuer OpenSCAD WASM Engine in WebView ohne CORS-Restriktionen, erweitertes Logging und transparente Konsolenanzeige

## [0.1.16] - 2026-11-05

### Geändert / Hinzugefügt
- OpenSCAD Core Integration: Offizieller OpenSCAD C++ Rechenkern (WASM) direkt in der App eingebunden fuer 1:1 Desktop-Berechnung & exakte STL-Geometrie

## [0.1.15] - 2026-11-05

### Geändert / Hinzugefügt
- CSG Performance & Polygon Explosion behoben: Schnelle Auswertung in <1s, korrekte Wandgeometrie ohne Schnitte, Bounding-Box Vorab-Checks und polygon()-Support

## [0.1.14] - 2026-11-04

### Geändert / Hinzugefügt
- Release v0.1.14: Eindeutige Versionsanzeige im Menue und versionierte APK-Dateien (SCADroid-v0.1.14.apk) im Root und Release

## [0.1.13] - 2026-11-04

### Geändert / Hinzugefügt
- CSG-Engine Update: Vollstaendige Unterstuetzung fuer if/else, minkowski, hull, String-Variablen, lokale Zuweisungen und verschachtelte Modul-Hierarchien

## [0.1.12] - 2026-11-04

### Geändert / Hinzugefügt
- Release v0.1.12: Behebe automatische Dummy-Wuerfel-Generierung bei leerem oder nicht-geometrischem OpenSCAD-Code

## [0.1.11] - 2026-11-03

### Geändert / Hinzugefügt
- Release v0.1.11: Neues Modell mit minimalem Template und Bestaetigungsdialog vor dem Loeschen

## [0.1.10] - 2026-11-03

### Geändert / Hinzugefügt
- Release v0.1.10: Undo/Redo Buttons im Code-Editor und bereinigtes Menue ohne blauen Untertitel

## [0.1.9] - 2026-11-03

### Geändert / Hinzugefügt
- Redundanten Vorschau-Button und Speichersymbol entfernt, Render als einzige zentrale Berechnungsaktion und STL-Export ausschliesslich im Menue

## [0.1.8] - 2026-11-03

### Geändert / Hinzugefügt
- Prominente STL-Exportfunktion fuer 3D-Druck in Menue, 3D-Viewport-HUD und Floating Action Button

## [0.1.7] - 2026-11-03

### Geändert / Hinzugefügt
- Header-Titel ohne Zeilenumbruch fixiert und fehlertolerantes Syntax-Highlighting gegen ungeschlossene Blockkommentare

## [0.1.6] - 2026-11-03

### Geändert / Hinzugefügt
- Clean Header Title ohne Textfragment, lebendiges OpenSCAD CAD Syntax-Farbschema und 2-Finger-Touch-Pan fuer 3D-Kamera

## [0.1.5] - 2026-11-03

### Geändert / Hinzugefügt
- Saubere TopAppBar: Entfernen des abgeschnittenen Subtitle-Textfragments ('SCADroid Nati...')
- OpenSCAD Darcula CAD Farbschema: Vollständige, lebendige Syntax-Einfärbung für Funktionen/CSG (Cyan), Keywords (Orange/Gold), Variablen/Bezeichner (Blau/Gelb), Literale/Zahlen (Hellgrün), Operatoren/Klammern (Lachsfarben) und Kommentare (Schiefergrau)
- 3D-Kamera Touch-Pan: Drehpunkt der 3D-Ansicht lässt sich jetzt mit 2 Fingern per Touch stufenlos im Raum verschieben (1 Finger für Orbit-Drehung, 2 Finger für Drehpunkt-Pan & Zoom)

## [0.1.4] - 2026-11-03

### Geändert / Hinzugefügt
- Vollständiges OpenSCAD Syntax-Highlighting (Keywords, Funktionen, Zahlen, Strings, Variablen, Kommentare), dynamische Auto-Completion-Leiste direkt über der Tastatur, Befehle-Dropdown-Menü, Direktwechsel zur 3D-Ansicht bei Vorschau/Render, SAF-Speicherdialog und persistente Code-Speicherung ohne Header-Überlauf

## [0.1.3] - 2026-11-03

### Geändert / Hinzugefügt
- Persistente Code-Speicherung (Auto-Save & Öffnen/Speichern von .scad-Dateien), funktionierende Symbolleiste & Snippet-Buttons mit Fokus-Commit, Auto-Completion bei Tastatureingabe aktiviert mit Vorschlags-Button und automatischer Klammer-Paarung

## [0.1.2] - 2026-11-03

### Geändert / Hinzugefügt
- Vollständige OpenSCAD-Kompatibilität: Mathematische Ausdrücke (Trigonometrie in Grad, Potenzen, Konstanten), Vektor-Variablen & Indizierung, CSG difference/union/intersection mit verketteten Transformationen, Mirror, Color, For-Schleifen, User-Module, Auto-Completion und Symbol-Toolbar Fix

## [0.1.1] - 2026-11-03

### Geändert / Hinzugefügt
- Echtes 3D-CSG-Rendering mit OpenGL ES 3.0, Lines-App-Icon und vollwertiges CAD-Dark-Theme

## [0.1.0] - 2026-11-03

### Hinzugefügt
- Initiales Projekt-Setup für SCADroid (OpenSCAD auf Android)
- Konfiguration der App-ID und Namespace `de.fisch981.scadroid`
- Automatisierungs-Workflow (`push.ps1`, `copyReleaseApkToRoot`)
- Basis-Architektur für C++ NDK Engine, sora-editor und Jetpack Compose UI
