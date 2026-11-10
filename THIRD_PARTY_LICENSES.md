# Third-Party Licenses and Notices / Drittanbieter-Lizenzen

SCADroid incorporates, links to, or redistributes third-party open-source components. This document provides an overview of all included third-party libraries, fonts, and tools, their respective authors, license terms, and upstream source locations.

---

## 1. OpenSCAD Engine (WebAssembly Build)

- **Component:** OpenSCAD C++ Engine compiled to WebAssembly / JavaScript (`assets/openscad/openscad.js`)
- **Upstream Projects:**
  - OpenSCAD: [https://github.com/openscad/openscad](https://github.com/openscad/openscad)
  - OpenSCAD WASM Port: [https://github.com/openscad/openscad-wasm](https://github.com/openscad/openscad-wasm)
- **Copyright:** (C) 2009-2026 Clifford Wolf, Marius Kintel, and the OpenSCAD contributors.
- **License:** **GNU General Public License, Version 2 (GPL-2.0)** with special exception to link with the CGAL library.
- **Source Code Availability:** As required by Section 3 of the GPLv2, the corresponding source code for OpenSCAD is publicly available via the official OpenSCAD GitHub repositories listed above.

---

## 2. Sora Editor

- **Component:** Code Editor View & Syntax Highlighting (`io.github.Rosemoe.sora-editor`)
- **Upstream Project:** [https://github.com/Rosemoe/sora-editor](https://github.com/Rosemoe/sora-editor)
- **Copyright:** (C) Rosemoe and contributors.
- **License:** **GNU Lesser General Public License, Version 2.1 (LGPL-2.1)**.
- **Terms:** As permitted by Section 3 of the LGPLv2.1, this library may also be used in conjunction with works under the GNU General Public License.

---

## 3. Liberation Sans Font

- **Component:** TrueType font bundled for 3D text generation (`assets/openscad/LiberationSans-Regular.ttf`)
- **Upstream Project:** [https://github.com/liberationfonts/liberation-fonts](https://github.com/liberationfonts/liberation-fonts)
- **Copyright:** (C) 2012 Red Hat, Inc. and others.
- **License:** **SIL Open Font License, Version 1.1 (OFL-1.1)**.
- **Permission:** OFL grants permission to bundle, modify, and redistribute the font software with any software, subject to the condition that fonts cannot be sold individually.

---

## 4. AndroidX & Jetpack Compose

- **Components:** AndroidX libraries, Jetpack Compose UI toolkit, Android Lifecycle, Coroutines Android
- **Upstream Projects:**
  - Android Open Source Project (AOSP): [https://source.android.com](https://source.android.com)
  - Kotlin / Kotlinx Coroutines: [https://github.com/Kotlin/kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines)
- **Copyright:** (C) The Android Open Source Project, Google LLC, JetBrains s.r.o.
- **License:** **Apache License, Version 2.0 (Apache-2.0)**.
- **Terms:** Licensed under the Apache License, Version 2.0; you may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0).

---

## 5. Manifold CAD Geometry Library

- **Component:** High-performance manifold boolean geometry engine (integrated into OpenSCAD WASM backend `--backend=Manifold`)
- **Upstream Project:** [https://github.com/elalish/manifold](https://github.com/elalish/manifold)
- **Copyright:** (C) 2021-2026 Emmett Lalish and Manifold contributors.
- **License:** **Apache License, Version 2.0 (Apache-2.0)**.

---

## Trademark Disclaimer

**OpenSCAD** is a trademark / registered mark of its respective owners (Marius Kintel and the OpenSCAD development team). SCADroid is an independent open-source project and is **not** endorsed by, directly affiliated with, or maintained by the official OpenSCAD project team.
