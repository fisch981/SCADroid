package de.fisch981.scadroid.ui.editor

import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula

/**
 * Dediziertes Farbschema für OpenSCAD in SCADroid:
 * Angelehnt an die authentische OpenSCAD Desktop-Farbgebung und JetBrains CAD Dark Theme.
 */
class OpenScadColorScheme : SchemeDarcula() {

    override fun applyDefault() {
        super.applyDefault()

        // Editor Grundfarben
        setColor(WHOLE_BACKGROUND, 0xFF14161A.toInt())
        setColor(LINE_NUMBER_BACKGROUND, 0xFF16181D.toInt())
        setColor(LINE_NUMBER, 0xFF5C6370.toInt())
        setColor(LINE_NUMBER_CURRENT, 0xFFFFD54F.toInt())
        setColor(CURRENT_LINE, 0xFF1E222B.toInt())
        setColor(SELECTION_INSERT, 0xFFFFD54F.toInt())
        setColor(SELECTION_HANDLE, 0xFFFFD54F.toInt())
        setColor(SELECTED_TEXT_BACKGROUND, 0xFF28446B.toInt())
        setColor(TEXT_NORMAL, 0xFFD4D4D4.toInt())

        // 1. OpenSCAD Keywords (module, function, if, else, for, let, each, true, false, include, use)
        // Kräftiges Orange / Koralle wie im originalen OpenSCAD
        setColor(KEYWORD, 0xFFE5A93C.toInt())

        // 2. OpenSCAD CSG & Transformationen (cube, cylinder, sphere, difference, union, translate, rotate, scale, etc.)
        // Leuchtendes Cyan / Türkis für alle 3D-Primitive und Operatoren
        setColor(FUNCTION_NAME, 0xFF4EC9B0.toInt())

        // 3. Variablen und Bezeichner ($fn, breite, hoehe, bohrung, etc.)
        // Helles Sonnengelb für Variablen und Parameter
        setColor(IDENTIFIER_NAME, 0xFF61AFEF.toInt())

        // 4. Literale (Zahlen wie 30, 15, 7 und Strings)
        // Frisches Hellgrün
        setColor(LITERAL, 0xFF98C379.toInt())

        // 5. Operatoren und Trennzeichen (=, +, -, *, /, [, ], {, }, (, ))
        // Warmes Lachs / Amber
        setColor(OPERATOR, 0xFFE06C75.toInt())

        // 6. Kommentare (// ..., /* ... */)
        // Dezentes Schiefergrau
        setColor(COMMENT, 0xFF7F848E.toInt())
    }
}
