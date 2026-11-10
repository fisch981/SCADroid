package de.fisch981.scadroid.ui.editor

import android.os.Bundle
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.analysis.StyleReceiver
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.lang.styling.MappedSpans
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.SymbolPairMatch
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

data class ScadSnippet(
    val label: String,
    val desc: String,
    val commitText: String,
    val kind: CompletionItemKind
)

val OPENSCAD_SNIPPETS = listOf(
    // 3D Primitives
    ScadSnippet("cube", "3D Quader erstellen", "cube([10, 10, 10]);", CompletionItemKind.Function),
    ScadSnippet("cube_centered", "3D Quader zentriert", "cube([10, 10, 10], center = true);", CompletionItemKind.Function),
    ScadSnippet("cylinder", "3D Zylinder erstellen", "cylinder(h = 10, r = 5);", CompletionItemKind.Function),
    ScadSnippet("cylinder_cone", "3D Kegel / Kegelstumpf", "cylinder(h = 10, r1 = 5, r2 = 0);", CompletionItemKind.Function),
    ScadSnippet("sphere", "3D Kugel erstellen", "sphere(r = 10);", CompletionItemKind.Function),

    // CSG Booleans
    ScadSnippet("difference", "CSG Differenz (Objekte subtrahieren)", "difference() {\n    \n}", CompletionItemKind.Method),
    ScadSnippet("union", "CSG Vereinigung (Objekte kombinieren)", "union() {\n    \n}", CompletionItemKind.Method),
    ScadSnippet("intersection", "CSG Schnittmenge", "intersection() {\n    \n}", CompletionItemKind.Method),

    // Transformations
    ScadSnippet("translate", "3D Verschiebung entlang Achsen", "translate([0, 0, 0]) ", CompletionItemKind.Keyword),
    ScadSnippet("rotate", "3D Drehung um Achsen", "rotate([0, 0, 0]) ", CompletionItemKind.Keyword),
    ScadSnippet("scale", "3D Skalierung entlang Achsen", "scale([1, 1, 1]) ", CompletionItemKind.Keyword),
    ScadSnippet("mirror", "3D Spiegelung an Normalenebene", "mirror([1, 0, 0]) ", CompletionItemKind.Keyword),
    ScadSnippet("color", "Farbe für Kind-Geometrie zuweisen", "color([1, 0, 0]) ", CompletionItemKind.Keyword),

    // 2D & Extrusions
    ScadSnippet("circle", "2D Kreis erstellen", "circle(r = 10);", CompletionItemKind.Function),
    ScadSnippet("square", "2D Rechteck erstellen", "square([10, 10]);", CompletionItemKind.Function),
    ScadSnippet("polygon", "2D Polygon aus Punktliste", "polygon(points = [[0,0], [10,0], [5,10]]);", CompletionItemKind.Function),
    ScadSnippet("linear_extrude", "2D Kontur linear in 3D extrudieren", "linear_extrude(height = 10) ", CompletionItemKind.Keyword),
    ScadSnippet("rotate_extrude", "2D Kontur rotativ extrudieren", "rotate_extrude(angle = 360) ", CompletionItemKind.Keyword),

    // Features & Modules
    ScadSnippet("module", "Benutzerdefiniertes Modul definieren", "module name() {\n    \n}", CompletionItemKind.Snippet),
    ScadSnippet("for", "Schleife über Wertebereich", "for (i = [0:10]) {\n    \n}", CompletionItemKind.Snippet),
    ScadSnippet("minkowski", "Minkowski-Summe bilden", "minkowski() {\n    \n}", CompletionItemKind.Method),
    ScadSnippet("hull", "Konvexe Hülle über Kinder bilden", "hull() {\n    \n}", CompletionItemKind.Method),

    // Math Functions
    ScadSnippet("sin", "Sinus in Grad (deg)", "sin()", CompletionItemKind.Function),
    ScadSnippet("cos", "Kosinus in Grad (deg)", "cos()", CompletionItemKind.Function),
    ScadSnippet("tan", "Tangens in Grad (deg)", "tan()", CompletionItemKind.Function),
    ScadSnippet("sqrt", "Quadratwurzel", "sqrt()", CompletionItemKind.Function),
    ScadSnippet("abs", "Absoluter Betrag", "abs()", CompletionItemKind.Function),
    ScadSnippet("min", "Minimum zweier Werte", "min()", CompletionItemKind.Function),
    ScadSnippet("max", "Maximum zweier Werte", "max()", CompletionItemKind.Function),
    ScadSnippet("pow", "Potenzfunktion (Basis, Exponent)", "pow()", CompletionItemKind.Function),

    // Includes & External Files
    ScadSnippet("include", "Datei oder Bibliothek einbinden (alles importieren)", "include <filename.scad>;", CompletionItemKind.Keyword),
    ScadSnippet("use", "Module/Funktionen aus Datei einbinden", "use <filename.scad>;", CompletionItemKind.Keyword),
    ScadSnippet("use_mcad_boxes", "MCAD Standardbibliothek (boxes.scad)", "use <MCAD/boxes.scad>;", CompletionItemKind.Snippet),
    ScadSnippet("import", "3D/2D-Datei importieren (STL, DXF, SVG, 3MF)", "import(\"model.stl\");", CompletionItemKind.Function),
    ScadSnippet("surface", "Höhenfeld/Oberfläche aus Bild oder DAT-Datei", "surface(file = \"surface.png\", center = true);", CompletionItemKind.Function),

    // Advanced 3D & 2D
    ScadSnippet("polyhedron", "Benutzerdefiniertes Polyeder aus Punkten und Flächen", "polyhedron(\n    points = [[0,0,0], [10,0,0], [0,10,0], [0,0,10]],\n    faces = [[0,1,2], [0,2,3], [0,3,1], [1,3,2]]\n);", CompletionItemKind.Function),
    ScadSnippet("text", "2D Text erstellen", "text(\"Text\", size = 10, halign = \"center\", valign = \"center\");", CompletionItemKind.Function),
    ScadSnippet("offset", "2D Kontur vergrößern / abrunden / anfasen", "offset(r = 2) ", CompletionItemKind.Function),
    ScadSnippet("projection", "3D Geometrie auf 2D Ebene projizieren", "projection(cut = false) ", CompletionItemKind.Function),
    ScadSnippet("render", "Teilbaum explizit als CSG-Mesh vorberechnen", "render() ", CompletionItemKind.Function),
    ScadSnippet("intersection_for", "Schnittmenge über Schleifen-Objekte", "intersection_for (i = [0:3]) {\n    \n}", CompletionItemKind.Snippet),
    ScadSnippet("echo", "Ausgabe auf der Konsole", "echo(\"Wert:\", x);", CompletionItemKind.Function),
    ScadSnippet("assert", "Bedingung prüfen (Fehler bei false)", "assert(x > 0, \"x muss positiv sein\");", CompletionItemKind.Function),

    // Special Variables
    ScadSnippet("\$fn", "Fragment-Anzahl (Kreisauflösung)", "\$fn = 32;", CompletionItemKind.Variable),
    ScadSnippet("\$fa", "Minimaler Fragment-Winkel", "\$fa = 12;", CompletionItemKind.Variable),
    ScadSnippet("\$fs", "Minimale Fragment-Größe", "\$fs = 2;", CompletionItemKind.Variable)
)

// Schneller Syntax-Highlighter für OpenSCAD Code:
// - Keywords (module, function, if, else, for, true, false)
// - Funktionen & CSG (cube, cylinder, sphere, difference, union, translate, etc.)
// - Variablen & Spezialvariablen ($fn, $fa, $fs)
// - Zahlen & Strings
// - Kommentare
class OpenScadAnalyzeManager : AnalyzeManager {
    private var receiver: StyleReceiver? = null
    private var content: ContentReference? = null

    private val commentEndMarker = "*" + "/"

    private val keywords = setOf("module", "function", "if", "else", "for", "let", "each", "true", "false", "include", "use")
    private val functions = setOf(
        "cube", "cylinder", "sphere", "circle", "square", "polygon", "polyhedron",
        "difference", "union", "intersection",
        "translate", "rotate", "scale", "mirror", "color", "multmatrix", "resize",
        "linear_extrude", "rotate_extrude", "minkowski", "hull", "render",
        "offset", "projection", "surface", "text", "import",
        "sin", "cos", "tan", "asin", "acos", "atan", "atan2",
        "sqrt", "abs", "min", "max", "pow", "exp", "log", "ln", "sign", "round", "floor", "ceil",
        "concat", "lookup", "str", "chr", "search", "version", "version_num", "parent_module",
        "echo", "assert"
    )

    override fun setReceiver(receiver: StyleReceiver?) {
        this.receiver = receiver
    }

    override fun reset(content: ContentReference, extra: Bundle) {
        this.content = content
        rerun()
    }

    override fun insert(start: CharPosition, end: CharPosition, text: CharSequence) {
        rerun()
    }

    override fun delete(start: CharPosition, end: CharPosition, text: CharSequence) {
        rerun()
    }

    override fun rerun() {
        val ref = content ?: return
        val builder = MappedSpans.Builder()

        // Vorab-Scan: Prüfe für jede Zeile, ob weiter unten ein schließendes */ existiert.
        // Verhindert, dass Tippfehler (z.B. /* [Qualitaet] *}}}{{{;;;}}}/) oder ungeschlossene Kommentare
        // den gesamten restlichen OpenSCAD-Code als Kommentar grau färben!
        val hasClosingAfter = BooleanArray(ref.lineCount)
        var seenClose = false
        for (idx in (ref.lineCount - 1) downTo 0) {
            val l = ref.getLine(idx) ?: ""
            if (l.contains(commentEndMarker)) {
                seenClose = true
            }
            hasClosingAfter[idx] = seenClose
        }

        var inBlockComment = false

        for (lineIdx in 0 until ref.lineCount) {
            val line = ref.getLine(lineIdx) ?: ""
            var i = 0
            val len = line.length

            if (inBlockComment) {
                val endIdx = line.indexOf(commentEndMarker)
                if (endIdx != -1) {
                    builder.addIfNeeded(lineIdx, 0, TextStyle.makeStyle(EditorColorScheme.COMMENT))
                    builder.addIfNeeded(lineIdx, endIdx + 2, TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL))
                    i = endIdx + 2
                    inBlockComment = false
                } else {
                    builder.addIfNeeded(lineIdx, 0, TextStyle.makeStyle(EditorColorScheme.COMMENT))
                    continue
                }
            }

            while (i < len) {
                val c = line[i]

                // Whitespace
                if (c.isWhitespace()) {
                    i++
                    continue
                }

                // Einzeiliger Kommentar //
                if (c == '/' && i + 1 < len && line[i + 1] == '/') {
                    builder.addIfNeeded(lineIdx, i, TextStyle.makeStyle(EditorColorScheme.COMMENT))
                    break
                }

                // Mehrzeiliger Kommentar /*
                if (c == '/' && i + 1 < len && line[i + 1] == '*') {
                    // 1. Schließt er regulär mit */ auf derselben Zeile ab?
                    val endIdx = line.indexOf(commentEndMarker, i + 2)
                    if (endIdx != -1) {
                        builder.addIfNeeded(lineIdx, i, TextStyle.makeStyle(EditorColorScheme.COMMENT))
                        builder.addIfNeeded(lineIdx, endIdx + 2, TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL))
                        i = endIdx + 2
                        continue
                    }

                    // 2. Schließt die Zeile später mit einem Slash '/' ab (z.B. /* [Qualitaet] *}}}{{{;;;}}}/)?
                    val lastSlash = line.lastIndexOf('/')
                    if (lastSlash > i + 1) {
                        builder.addIfNeeded(lineIdx, i, TextStyle.makeStyle(EditorColorScheme.COMMENT))
                        builder.addIfNeeded(lineIdx, lastSlash + 1, TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL))
                        i = lastSlash + 1
                        continue
                    }

                    // 3. Gibt es in einer nachfolgenden Zeile überhaupt ein schließendes */?
                    if (lineIdx + 1 < ref.lineCount && hasClosingAfter[lineIdx + 1]) {
                        builder.addIfNeeded(lineIdx, i, TextStyle.makeStyle(EditorColorScheme.COMMENT))
                        inBlockComment = true
                        break
                    } else {
                        // Kein */ im gesamten Rest der Datei: Behandle es nur als einzeiligen Kommentar,
                        // damit nachfolgender OpenSCAD-Code (difference, cube, etc.) NICHT grau gefärbt wird!
                        builder.addIfNeeded(lineIdx, i, TextStyle.makeStyle(EditorColorScheme.COMMENT))
                        break
                    }
                }

                // String Literal "..."
                if (c == '"') {
                    val start = i
                    i++
                    while (i < len && line[i] != '"') {
                        if (line[i] == '\\' && i + 1 < len) i++
                        i++
                    }
                    if (i < len) i++ // Schließendes "
                    builder.addIfNeeded(lineIdx, start, TextStyle.makeStyle(EditorColorScheme.LITERAL))
                    builder.addIfNeeded(lineIdx, i, TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL))
                    continue
                }

                // Numerischer Wert
                if (c.isDigit() || (c == '.' && i + 1 < len && line[i + 1].isDigit())) {
                    val start = i
                    while (i < len && (line[i].isDigit() || line[i] == '.')) {
                        i++
                    }
                    builder.addIfNeeded(lineIdx, start, TextStyle.makeStyle(EditorColorScheme.LITERAL))
                    builder.addIfNeeded(lineIdx, i, TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL))
                    continue
                }

                // Bezeichner, Keyword, Funktion oder Variable
                if (c.isLetter() || c == '$' || c == '_') {
                    val start = i
                    while (i < len && (line[i].isLetterOrDigit() || line[i] == '$' || line[i] == '_')) {
                        i++
                    }
                    val word = line.substring(start, i)
                    val colorId = when {
                        word.startsWith("$") -> EditorColorScheme.KEYWORD
                        keywords.contains(word) -> EditorColorScheme.KEYWORD
                        functions.contains(word) -> EditorColorScheme.FUNCTION_NAME
                        else -> EditorColorScheme.IDENTIFIER_NAME
                    }

                    builder.addIfNeeded(lineIdx, start, TextStyle.makeStyle(colorId))
                    builder.addIfNeeded(lineIdx, i, TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL))
                    continue
                }

                // Operatoren & Begrenzer (+, -, *, /, =, <, >, !, ?, :, ,, ;, [, ], (, ), {, })
                if (c in "+-*/%=<>!&|?:,;[](){}") {
                    builder.addIfNeeded(lineIdx, i, TextStyle.makeStyle(EditorColorScheme.OPERATOR))
                    builder.addIfNeeded(lineIdx, i + 1, TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL))
                    i++
                    continue
                }

                i++
            }
        }

        builder.determine(ref.lineCount)
        val styles = Styles(builder.build())
        receiver?.setStyles(this, styles)
    }

    override fun destroy() {
        content = null
        receiver = null
    }
}

class OpenScadLanguage : EmptyLanguage() {

    private val analyzeManager = OpenScadAnalyzeManager()

    override fun getAnalyzeManager(): AnalyzeManager {
        return analyzeManager
    }

    override fun getSymbolPairs(): SymbolPairMatch {
        return SymbolPairMatch().apply {
            putPair('(', SymbolPairMatch.SymbolPair("(", ")"))
            putPair('[', SymbolPairMatch.SymbolPair("[", "]"))
            putPair('{', SymbolPairMatch.SymbolPair("{", "}"))
            putPair('"', SymbolPairMatch.SymbolPair("\"", "\""))
        }
    }

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extra: Bundle
    ) {
        if (position.line >= content.lineCount) return
        val line = content.getLine(position.line) ?: return
        val col = position.column.coerceIn(0, line.length)

        var start = col
        while (start > 0 && (line[start - 1].isLetterOrDigit() || line[start - 1] == '$' || line[start - 1] == '_')) {
            start--
        }

        val prefix = line.substring(start, col)
        val pLower = prefix.lowercase()

        for (snip in OPENSCAD_SNIPPETS) {
            if (prefix.isEmpty() || snip.label.lowercase().startsWith(pLower) || snip.label.lowercase().contains(pLower)) {
                val item = SimpleCompletionItem(
                    snip.label,
                    snip.desc,
                    prefix.length,
                    snip.commitText
                ).kind(snip.kind)

                publisher.addItem(item)
            }
        }
        publisher.updateList()
    }
}
