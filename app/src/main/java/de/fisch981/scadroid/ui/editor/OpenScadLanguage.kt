package de.fisch981.scadroid.ui.editor

import android.os.Bundle
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.SymbolPairMatch

class OpenScadLanguage : EmptyLanguage() {

    private data class ScadSnippet(
        val label: String,
        val desc: String,
        val commitText: String,
        val kind: CompletionItemKind
    )

    private val snippets = listOf(
        // CSG Booleans
        ScadSnippet("difference", "CSG Differenz (Objekte subtrahieren)", "difference() {\n    \n}", CompletionItemKind.Method),
        ScadSnippet("union", "CSG Vereinigung (Objekte kombinieren)", "union() {\n    \n}", CompletionItemKind.Method),
        ScadSnippet("intersection", "CSG Schnittmenge (Überlappung behalten)", "intersection() {\n    \n}", CompletionItemKind.Method),

        // 3D Primitives
        ScadSnippet("cube", "3D Quader erstellen", "cube([10, 10, 10]);", CompletionItemKind.Function),
        ScadSnippet("cube_centered", "3D Quader zentriert", "cube([10, 10, 10], center = true);", CompletionItemKind.Function),
        ScadSnippet("cylinder", "3D Zylinder erstellen", "cylinder(h = 10, r = 5);", CompletionItemKind.Function),
        ScadSnippet("cylinder_cone", "3D Kegel / Kegelstumpf", "cylinder(h = 10, r1 = 5, r2 = 2);", CompletionItemKind.Function),
        ScadSnippet("sphere", "3D Kugel erstellen", "sphere(r = 10);", CompletionItemKind.Function),

        // Transformations
        ScadSnippet("translate", "3D Verschiebung entlang Achsen", "translate([0, 0, 0]) ", CompletionItemKind.Keyword),
        ScadSnippet("rotate", "3D Drehung um Achsen", "rotate([0, 0, 0]) ", CompletionItemKind.Keyword),
        ScadSnippet("scale", "3D Skalierung entlang Achsen", "scale([1, 1, 1]) ", CompletionItemKind.Keyword),
        ScadSnippet("mirror", "3D Spiegelung an Normalenebene", "mirror([1, 0, 0]) ", CompletionItemKind.Keyword),
        ScadSnippet("color", "Farbe für Kind-Geometrie zuweisen", "color([1, 0, 0]) ", CompletionItemKind.Keyword),

        // Math Functions
        ScadSnippet("sin", "Sinus in Grad (deg)", "sin()", CompletionItemKind.Function),
        ScadSnippet("cos", "Kosinus in Grad (deg)", "cos()", CompletionItemKind.Function),
        ScadSnippet("tan", "Tangens in Grad (deg)", "tan()", CompletionItemKind.Function),
        ScadSnippet("sqrt", "Quadratwurzel", "sqrt()", CompletionItemKind.Function),
        ScadSnippet("abs", "Absoluter Betrag", "abs()", CompletionItemKind.Function),
        ScadSnippet("min", "Minimum zweier oder mehrerer Werte", "min()", CompletionItemKind.Function),
        ScadSnippet("max", "Maximum zweier oder mehrerer Werte", "max()", CompletionItemKind.Function),
        ScadSnippet("pow", "Potenzfunktion (Basis, Exponent)", "pow()", CompletionItemKind.Function),

        // 2D & Extrusions
        ScadSnippet("circle", "2D Kreis erstellen", "circle(r = 10);", CompletionItemKind.Function),
        ScadSnippet("square", "2D Rechteck erstellen", "square([10, 10]);", CompletionItemKind.Function),
        ScadSnippet("polygon", "2D Polygon aus Punktliste", "polygon(points = [[0,0], [10,0], [5,10]]);", CompletionItemKind.Function),
        ScadSnippet("linear_extrude", "2D Kontur linear in 3D extrudieren", "linear_extrude(height = 10) ", CompletionItemKind.Keyword),
        ScadSnippet("rotate_extrude", "2D Kontur rotativ extrudieren", "rotate_extrude(angle = 360) ", CompletionItemKind.Keyword),

        // Features & Modules
        ScadSnippet("minkowski", "Minkowski-Summe / Kantenverrundung", "minkowski() {\n    \n}", CompletionItemKind.Method),
        ScadSnippet("hull", "Konvexe Hülle über Kinder bilden", "hull() {\n    \n}", CompletionItemKind.Method),
        ScadSnippet("module", "Benutzerdefiniertes Modul definieren", "module name() {\n    \n}", CompletionItemKind.Snippet),
        ScadSnippet("for", "Schleife über Wertebereich", "for (i = [0:10]) {\n    \n}", CompletionItemKind.Snippet),

        // Special Variables
        ScadSnippet("\$fn", "Fragment-Anzahl (Kreisauflösung)", "\$fn = 32;", CompletionItemKind.Variable),
        ScadSnippet("\$fa", "Minimaler Fragment-Winkel", "\$fa = 12;", CompletionItemKind.Variable),
        ScadSnippet("\$fs", "Minimale Fragment-Größe", "\$fs = 2;", CompletionItemKind.Variable)
    )

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
        if (prefix.isEmpty()) return

        val pLower = prefix.lowercase()
        for (snip in snippets) {
            if (snip.label.lowercase().startsWith(pLower) || snip.label.lowercase().contains(pLower)) {
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
