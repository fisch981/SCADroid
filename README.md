# SCADroid

**SCADroid** ist die native Android-Portierung von OpenSCAD – der beliebten soliden 3D-CAD-Modellierungssoftware.

## Features
- **Vollwertige native C++ OpenSCAD Engine**: Ausführung via Android NDK mit klassischem CGAL-Backend und OpenCSG Schnellvorschau.
- **Leistungsstarker Code-Editor**: Integrierter `sora-editor` mit Syntax-Highlighting für OpenSCAD-Code, Code-Completion und praktischen **Undo/Redo-Tasten** direkt in der Werkzeugleiste.
- **Interaktiver 3D-Touch-Viewport**: OpenGL ES 3.0 Rendering mit intuitiven Touch-Gesten (Orbit, Pan, Zoom).
- **STL-Export für 3D-Druck**: Prominente Exportfunktion direkt im Menü, im 3D-Viewport-HUD und über den Floating Action Button (FAB).
- **OpenSCAD Customizer**: Automatische GUI-Generierung (Slider, Dropdowns, Checkboxen) aus SCAD-Variablen-Annotationen.
- **Bibliotheksverwaltung**: Integrierte Unterstützung für Bibliotheken wie BOSL2 und MCAD.
- **Projekt- & Dateiverwaltung**: Neues Modell mit sauberem Minimal-Template, Bestätigungsdialog vor dem endgültigen Löschen und Reibungsloses Öffnen/Speichern lokaler Dateien über das Storage Access Framework (SAF).
- **Robuste Render-Pipeline**: Saubere Auswertung ohne unerwünschte Dummy-Würfel-Generierung bei leerem oder rein mathematischem/nicht-geometrischem OpenSCAD-Code.

## Entwicklung & Standards
Entwickelt unter Einhaltung der globalen Antigravity-Android-Standards (`AGENTS.md`):
- Package & Application ID: `de.fisch981.scadroid`
- Automatisierte Release-APK-Kopierung ins Projekt-Root (`copyReleaseApkToRoot`)
- Vollautomatischer Versions- und Push-Workflow via `push.ps1`
