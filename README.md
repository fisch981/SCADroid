# SCADroid

**SCADroid** ist die native Android-Portierung von OpenSCAD – der beliebten soliden 3D-CAD-Modellierungssoftware.

## Features
- **Vollwertige native C++ OpenSCAD Engine**: Ausführung via Android NDK mit klassischem CGAL-Backend und OpenCSG Schnellvorschau.
- **Leistungsstarker Code-Editor**: Integrierter `sora-editor` mit Syntax-Highlighting für OpenSCAD-Code und Code-Completion.
- **Interaktiver 3D-Touch-Viewport**: OpenGL ES 3.0 Rendering mit intuitiven Touch-Gesten (Orbit, Pan, Zoom).
- **OpenSCAD Customizer**: Automatische GUI-Generierung (Slider, Dropdowns, Checkboxen) aus SCAD-Variablen-Annotationen.
- **Bibliotheksverwaltung**: Integrierte Unterstützung für Bibliotheken wie BOSL2 und MCAD.
- **Storage Access Framework (SAF)**: Reibungsloses Öffnen und Speichern lokaler Projekt- und Export-Dateien (STL, 3MF).

## Entwicklung & Standards
Entwickelt unter Einhaltung der globalen Antigravity-Android-Standards (`AGENTS.md`):
- Package & Application ID: `de.fisch981.scadroid`
- Automatisierte Release-APK-Kopierung ins Projekt-Root (`copyReleaseApkToRoot`)
- Vollautomatischer Versions- und Push-Workflow via `push.ps1`
