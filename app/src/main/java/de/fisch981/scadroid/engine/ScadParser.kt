package de.fisch981.scadroid.engine

import de.fisch981.scadroid.model.CustomizerParam
import kotlin.math.*

/**
 * Mathematischer Ausdrucks-Evaluator für OpenSCAD Berechnungen
 * Unterstützt:
 * - Grundrechenarten (+, -, *, /, %)
 * - Potenzoperator (^)
 * - Unäres Plus und Minus
 * - Klammerung
 * - Trigonometrische Funktionen in Grad (sin, cos, tan, asin, acos, atan, atan2)
 * - Mathematische Standardfunktionen (sqrt, abs, min, max, pow, exp, log, ln, sign, round, floor, ceil)
 * - Konstanten (PI) und Booleans (true, false)
 * - Variablen & Vektor-Indizierung (z.B. v[0])
 * - Vergleichsoperatoren (<, <=, >, >=, ==, !=) & Ternärer Operator (? :)
 */
class ScadMathEvaluator(
    private val variables: Map<String, Double> = emptyMap(),
    private val vectorVariables: Map<String, List<Double>> = emptyMap()
) {

    private var clean = ""
    private var pos = 0

    private fun peek(): Char = if (pos < clean.length) clean[pos] else '\u0000'
    private fun get(): Char = if (pos < clean.length) clean[pos++] else '\u0000'

    fun eval(expression: String): Double {
        clean = expression.filter { !it.isWhitespace() }
        pos = 0
        return try {
            parseTernary()
        } catch (_: Exception) {
            0.0
        }
    }

    private fun parseTernary(): Double {
        val condition = parseComparison()
        if (peek() == '?') {
            get() // '?'
            val trueVal = parseTernary()
            if (peek() == ':') get() // ':'
            val falseVal = parseTernary()
            return if (condition != 0.0) trueVal else falseVal
        }
        return condition
    }

    private fun parseComparison(): Double {
        var v = parseExpr()
        while (true) {
            val c = peek()
            if (c == '<') {
                get()
                if (peek() == '=') {
                    get()
                    v = if (v <= parseExpr()) 1.0 else 0.0
                } else {
                    v = if (v < parseExpr()) 1.0 else 0.0
                }
            } else if (c == '>') {
                get()
                if (peek() == '=') {
                    get()
                    v = if (v >= parseExpr()) 1.0 else 0.0
                } else {
                    v = if (v > parseExpr()) 1.0 else 0.0
                }
            } else if (c == '=' && pos + 1 < clean.length && clean[pos + 1] == '=') {
                get(); get()
                v = if (abs(v - parseExpr()) < 1e-6) 1.0 else 0.0
            } else if (c == '!' && pos + 1 < clean.length && clean[pos + 1] == '=') {
                get(); get()
                v = if (abs(v - parseExpr()) >= 1e-6) 1.0 else 0.0
            } else {
                break
            }
        }
        return v
    }

    private fun parseExpr(): Double {
        var v = parseTerm()
        while (true) {
            val c = peek()
            if (c == '+') {
                get()
                v += parseTerm()
            } else if (c == '-') {
                get()
                v -= parseTerm()
            } else {
                break
            }
        }
        return v
    }

    private fun parseTerm(): Double {
        var v = parsePower()
        while (true) {
            val c = peek()
            if (c == '*') {
                get()
                v *= parsePower()
            } else if (c == '/') {
                get()
                val denom = parsePower()
                v = if (denom != 0.0) v / denom else 0.0
            } else if (c == '%') {
                get()
                val denom = parsePower()
                v = if (denom != 0.0) v % denom else 0.0
            } else {
                break
            }
        }
        return v
    }

    private fun parsePower(): Double {
        val base = parseFactor()
        if (peek() == '^') {
            get()
            val exp = parsePower() // Rechts-assoziativ
            return base.pow(exp)
        }
        return base
    }

    private fun parseFactor(): Double {
        val c = peek()
        if (c == '+') {
            get()
            return parseFactor()
        }
        if (c == '-') {
            get()
            return -parseFactor()
        }
        if (c == '!') {
            get()
            return if (parseFactor() == 0.0) 1.0 else 0.0
        }
        if (c == '(') {
            get() // '('
            val v = parseTernary()
            if (peek() == ')') get() // ')'
            return v
        }

        // Variable, Funktion oder Bezeichner
        if (c.isLetter() || c == '$' || c == '_') {
            val start = pos
            while (pos < clean.length && (clean[pos].isLetterOrDigit() || clean[pos] == '$' || clean[pos] == '_')) {
                pos++
            }
            val id = clean.substring(start, pos)

            // Funktionsaufruf z.B. sin(45), sqrt(16), max(a, b)
            if (peek() == '(') {
                get() // '('
                val args = mutableListOf<Double>()
                if (peek() != ')') {
                    while (true) {
                        args.add(parseTernary())
                        if (peek() == ',') {
                            get()
                        } else {
                            break
                        }
                    }
                }
                if (peek() == ')') get() // ')'

                return evalFunction(id, args)
            }

            // Vektor-Indizierung z.B. v[0]
            if (peek() == '[') {
                get() // '['
                val idx = parseTernary().toInt()
                if (peek() == ']') get() // ']'
                val vec = vectorVariables[id]
                return if (vec != null && idx in vec.indices) vec[idx] else 0.0
            }

            // Konstanten & Booleans
            if (id.equals("PI", ignoreCase = true)) return Math.PI
            if (id.equals("true", ignoreCase = true)) return 1.0
            if (id.equals("false", ignoreCase = true)) return 0.0

            return variables[id] ?: 0.0
        }

        // Numerischer Wert
        val start = pos
        while (pos < clean.length && (clean[pos].isDigit() || clean[pos] == '.')) {
            pos++
        }
        val numStr = clean.substring(start, pos)
        return numStr.toDoubleOrNull() ?: 0.0
    }

    private fun evalFunction(name: String, args: List<Double>): Double {
        val a = args.getOrElse(0) { 0.0 }
        val b = args.getOrElse(1) { 0.0 }
        return when (name.lowercase()) {
            // OpenSCAD Winkelfunktionen operieren in Grad (Degrees)!
            "sin" -> sin(Math.toRadians(a))
            "cos" -> cos(Math.toRadians(a))
            "tan" -> tan(Math.toRadians(a))
            "asin" -> Math.toDegrees(asin(a.coerceIn(-1.0, 1.0)))
            "acos" -> Math.toDegrees(acos(a.coerceIn(-1.0, 1.0)))
            "atan" -> Math.toDegrees(atan(a))
            "atan2" -> Math.toDegrees(atan2(a, b))

            "sqrt" -> if (a >= 0) sqrt(a) else 0.0
            "abs" -> abs(a)
            "min" -> if (args.isEmpty()) 0.0 else args.minOrNull() ?: 0.0
            "max" -> if (args.isEmpty()) 0.0 else args.maxOrNull() ?: 0.0
            "pow" -> a.pow(b)
            "exp" -> exp(a)
            "log" -> if (a > 0) log10(a) else 0.0
            "ln" -> if (a > 0) ln(a) else 0.0
            "sign" -> sign(a)
            "round" -> round(a)
            "floor" -> floor(a)
            "ceil" -> ceil(a)
            else -> 0.0
        }
    }
}

/**
 * Vollständiger, robuster OpenSCAD Parser mit Unterstützung für:
 * - Chained Transformations (translate(...) cube(...);)
 * - Positional & Named Arguments
 * - Mathematische Ausdrücke & Variable References
 * - Vektor-Variablen (box = [10, 20, 30]; cube(box);)
 * - Difference, Union, Intersection
 * - Translate, Rotate, Scale, Mirror, Color
 * - Cube, Cylinder, Cone, Sphere
 * - Square, Circle, Linear_extrude
 * - For-Schleifen (for (i = [0:5]))
 * - Benutzerdefinierte Module (module name(...) { ... })
 */
class ScadParser {

    data class ParseResult(
        val solid: CsgSolid,
        val parameters: List<CustomizerParam>,
        val logMessages: List<String>
    )

    data class UserModule(
        val paramNames: List<String>,
        val defaultValues: Map<String, String>,
        val bodyTokens: List<String>
    )

    val userModules = mutableMapOf<String, UserModule>()
    val stringVariables = mutableMapOf<String, String>()
    val vectorVariables = mutableMapOf<String, List<Double>>()
    val variables = mutableMapOf<String, Double>()

    fun parseAndEvaluate(code: String, paramOverrides: Map<String, Any> = emptyMap()): ParseResult {
        val logs = mutableListOf<String>()
        val params = extractParameters(code)

        if (!variables.containsKey("\$fn")) {
            variables["\$fn"] = 32.0
        }

        // Kommentare entfernen (reguläre mehrzeilige sowie fehlertolerante einzeilige /* ... Kommentare)
        val cleanCode = code
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("/\\*[^\\n]*"), "")
            .replace(Regex("//.*"), "")

        // 0. Vorab Overrides einspielen, damit Referenzen im Code sofort aufgelöst werden können
        for ((k, v) in paramOverrides) {
            when (v) {
                is Double -> variables[k] = v
                is Float -> variables[k] = v.toDouble()
                is Number -> variables[k] = v.toDouble()
                is String -> stringVariables[k] = v
                is Boolean -> variables[k] = if (v) 1.0 else 0.0
            }
        }

        // 1. Initialer Lauf für Variablen: 3 Durchläufe für Vorwärts-/Rückwärts-Referenzen
        val varRegex = Regex("(?s)([a-zA-Z0-9_\\$]+)\\s*=\\s*([^;]+);")
        for (pass in 0 until 3) {
            for (match in varRegex.findAll(cleanCode)) {
                val name = match.groupValues[1]
                val expr = match.groupValues[2].trim()

                if ((expr.startsWith("\"") && expr.endsWith("\"")) || (expr.startsWith("'") && expr.endsWith("'"))) {
                    stringVariables[name] = expr.substring(1, expr.length - 1)
                } else if (expr.startsWith("[") && expr.endsWith("]")) {
                    val inner = expr.removeSurrounding("[", "]")
                    val parts = splitArgs(inner)
                    val tempMath = ScadMathEvaluator(variables, vectorVariables)
                    val vec = parts.map { tempMath.eval(it) }
                    vectorVariables[name] = vec
                    if (vec.isNotEmpty()) variables[name] = vec[0]
                } else {
                    val tempMath = ScadMathEvaluator(variables, vectorVariables)
                    val v = tempMath.eval(expr)
                    variables[name] = v
                }
            }
        }

        // 2. UI-Overrides nochmals verbindlich anwenden
        for ((k, v) in paramOverrides) {
            when (v) {
                is Double -> variables[k] = v
                is Float -> variables[k] = v.toDouble()
                is Number -> variables[k] = v.toDouble()
                is String -> stringVariables[k] = v
                is Boolean -> variables[k] = if (v) 1.0 else 0.0
            }
        }

        val mathEval = ScadMathEvaluator(variables, vectorVariables)

        // Tokenizer
        val tokens = tokenizeCode(cleanCode)
        var pos = 0

        fun peek(): String = if (pos < tokens.size) tokens[pos] else ""
        fun next(): String = if (pos < tokens.size) tokens[pos++] else ""

        fun parseArguments(argString: String): Map<String, String> {
            val res = mutableMapOf<String, String>()
            val args = splitArgs(argString)
            var anonIdx = 0
            for (a in args) {
                val trimmed = a.trim()
                if (trimmed.contains("=")) {
                    val kv = trimmed.split("=", limit = 2)
                    res[kv[0].trim()] = kv[1].trim()
                } else {
                    res["arg$anonIdx"] = trimmed
                    anonIdx++
                }
            }
            return res
        }

        fun parseVector(vecStr: String): Vector3 {
            val trimmed = vecStr.trim()
            // Falls es ein Variablen-Name eines Vektors ist
            if (vectorVariables.containsKey(trimmed)) {
                val v = vectorVariables[trimmed]!!
                val x = if (v.isNotEmpty()) v[0].toFloat() else 0f
                val y = if (v.size > 1) v[1].toFloat() else 0f
                val z = if (v.size > 2) v[2].toFloat() else 0f
                return Vector3(x, y, z)
            }
            // Falls ein Skalar übergeben wurde z.B. bei scale(2)
            if (!trimmed.startsWith("[")) {
                val s = mathEval.eval(trimmed).toFloat()
                return Vector3(s, s, s)
            }
            val inner = trimmed.removeSurrounding("[", "]")
            val parts = splitArgs(inner)
            val x = if (parts.isNotEmpty()) mathEval.eval(parts[0]).toFloat() else 0f
            val y = if (parts.size > 1) mathEval.eval(parts[1]).toFloat() else 0f
            val z = if (parts.size > 2) mathEval.eval(parts[2]).toFloat() else 0f
            return Vector3(x, y, z)
        }

        fun parseColor(str: String): Vector3 {
            val cleanStr = str.trim().trim('"', '\'')
            return when (cleanStr.lowercase()) {
                "red" -> Vector3(1f, 0.2f, 0.2f)
                "green" -> Vector3(0.2f, 0.8f, 0.2f)
                "blue" -> Vector3(0.2f, 0.5f, 1f)
                "yellow" -> Vector3(1f, 0.85f, 0.1f)
                "gold" -> Vector3(0.97f, 0.74f, 0.22f)
                "orange" -> Vector3(1f, 0.55f, 0f)
                "cyan" -> Vector3(0f, 0.8f, 0.8f)
                "magenta", "pink" -> Vector3(0.9f, 0.2f, 0.6f)
                "white" -> Vector3(0.95f, 0.95f, 0.95f)
                "black" -> Vector3(0.15f, 0.15f, 0.15f)
                "gray", "grey" -> Vector3(0.5f, 0.5f, 0.5f)
                "darkgray", "darkgrey" -> Vector3(0.3f, 0.3f, 0.3f)
                "lightgray", "lightgrey" -> Vector3(0.75f, 0.75f, 0.75f)
                else -> {
                    if (cleanStr.startsWith("[")) parseVector(cleanStr)
                    else Vector3(0.97f, 0.74f, 0.22f)
                }
            }
        }

        fun skipStatement() {
            if (pos >= tokens.size) return
            val t = next()
            if (t == "{") {
                var depth = 1
                while (pos < tokens.size && depth > 0) {
                    val tok = next()
                    if (tok == "{") depth++
                    else if (tok == "}") depth--
                }
            } else if (t.matches(Regex("(?s)^if\\s*\\(.*\\)$"))) {
                skipStatement()
                if (pos < tokens.size && peek() == "else") {
                    next() // consume 'else'
                    skipStatement()
                }
            } else if (t.matches(Regex("(?s)^(translate|rotate|scale|mirror|color|linear_extrude|minkowski|hull|difference|union|intersection)\\s*\\(.*\\)$"))) {
                skipStatement()
            } else {
                if (t != ";") {
                    while (pos < tokens.size && peek() != ";" && peek() != "}") {
                        next()
                    }
                    if (pos < tokens.size && peek() == ";") next()
                }
            }
        }

        fun evalBool(condStr: String, currentVars: Map<String, Double>, currentStrings: Map<String, String>): Boolean {
            val trimmed = condStr.trim()
            if (trimmed.contains("==")) {
                val parts = trimmed.split("==", limit = 2)
                val left = parts[0].trim()
                val right = parts[1].trim()
                val lStr = if ((left.startsWith("\"") && left.endsWith("\"")) || (left.startsWith("'") && left.endsWith("'"))) left.substring(1, left.length - 1).trim()
                           else (currentStrings[left] ?: stringVariables[left] ?: "").trim()
                val rStr = if ((right.startsWith("\"") && right.endsWith("\"")) || (right.startsWith("'") && right.endsWith("'"))) right.substring(1, right.length - 1).trim()
                           else (currentStrings[right] ?: stringVariables[right] ?: "").trim()
                if (lStr.isNotEmpty() || rStr.isNotEmpty() || left.startsWith("\"") || right.startsWith("\"") || left.startsWith("'") || right.startsWith("'")) {
                    return lStr == rStr
                }
            }
            if (trimmed.contains("!=")) {
                val parts = trimmed.split("!=", limit = 2)
                val left = parts[0].trim()
                val right = parts[1].trim()
                val lStr = if ((left.startsWith("\"") && left.endsWith("\"")) || (left.startsWith("'") && left.endsWith("'"))) left.substring(1, left.length - 1).trim()
                           else (currentStrings[left] ?: stringVariables[left] ?: "").trim()
                val rStr = if ((right.startsWith("\"") && right.endsWith("\"")) || (right.startsWith("'") && right.endsWith("'"))) right.substring(1, right.length - 1).trim()
                           else (currentStrings[right] ?: stringVariables[right] ?: "").trim()
                if (lStr.isNotEmpty() || rStr.isNotEmpty() || left.startsWith("\"") || right.startsWith("\"") || left.startsWith("'") || right.startsWith("'")) {
                    return lStr != rStr
                }
            }

            val math = ScadMathEvaluator(currentVars, vectorVariables)
            val num = math.eval(trimmed)
            return abs(num) > 1e-6
        }

        // Parst ein einzelnes Statement oder einen Block { ... }
        fun parseStatement(
            scopeVars: MutableMap<String, Double> = mutableMapOf(),
            scopeStrings: MutableMap<String, String> = mutableMapOf()
        ): List<CsgSolid> {
            if (pos >= tokens.size) return emptyList()
            val mathEval = ScadMathEvaluator(HashMap(variables).apply { putAll(scopeVars) }, vectorVariables)

            val t = next()

            // 0. assert(...) ignorieren
            if (t.matches(Regex("(?s)^assert\\s*\\(.*\\)$"))) {
                if (peek() == ";") next()
                return emptyList()
            }

            // 0.1 if-Bedingung: if (cond) { ... } else if (...) ... else ...
            if (t.matches(Regex("(?s)^if\\s*\\(.*\\)$"))) {
                val condExpr = t.substringAfter("(").substringBeforeLast(")")
                val cond = evalBool(condExpr, scopeVars, scopeStrings)

                val result = if (cond) {
                    parseStatement(scopeVars, scopeStrings)
                } else {
                    skipStatement()
                    emptyList()
                }

                if (pos < tokens.size && peek() == "else") {
                    next() // consume 'else'
                    if (cond) {
                        skipStatement()
                        return result
                    } else {
                        return parseStatement(scopeVars, scopeStrings)
                    }
                }
                return result
            }

            if (t == "else") {
                skipStatement()
                return emptyList()
            }

            // 0.2 Lokale Variablen-Zuweisung innerhalb Modulen / Blöcken (z.B. x = a + 5;)
            if (peek() == "=") {
                val varName = t
                next() // '='
                val exprTokens = mutableListOf<String>()
                while (pos < tokens.size && peek() != ";") {
                    exprTokens.add(next())
                }
                if (pos < tokens.size && peek() == ";") next() // ';'

                val exprStr = exprTokens.joinToString(" ").trim()
                if ((exprStr.startsWith("\"") && exprStr.endsWith("\"")) || (exprStr.startsWith("'") && exprStr.endsWith("'"))) {
                    val s = exprStr.substring(1, exprStr.length - 1).trim()
                    scopeStrings[varName] = s
                    stringVariables[varName] = s
                } else if (exprStr.startsWith("[") && exprStr.endsWith("]")) {
                    val inner = exprStr.removeSurrounding("[", "]")
                    val parts = splitArgs(inner)
                    val vec = parts.map { mathEval.eval(it) }
                    vectorVariables[varName] = vec
                    if (vec.isNotEmpty()) {
                        scopeVars[varName] = vec[0]
                        variables[varName] = vec[0]
                    }
                } else {
                    val evaluatedVal = mathEval.eval(exprStr)
                    scopeVars[varName] = evaluatedVal
                    variables[varName] = evaluatedVal
                }
                return emptyList()
            }

            if (t.contains("=") && !t.contains("(") && !t.contains("[")) {
                val p = t.split("=", limit = 2)
                val varName = p[0].trim()
                val exprRight = if (p.size > 1) p[1].trim() else ""
                val exprTokens = mutableListOf<String>()
                if (exprRight.isNotEmpty()) exprTokens.add(exprRight)
                while (pos < tokens.size && peek() != ";") {
                    exprTokens.add(next())
                }
                if (pos < tokens.size && peek() == ";") next() // ';'

                val exprStr = exprTokens.joinToString(" ").trim()
                if ((exprStr.startsWith("\"") && exprStr.endsWith("\"")) || (exprStr.startsWith("'") && exprStr.endsWith("'"))) {
                    val s = exprStr.substring(1, exprStr.length - 1).trim()
                    scopeStrings[varName] = s
                    stringVariables[varName] = s
                } else if (exprStr.startsWith("[") && exprStr.endsWith("]")) {
                    val inner = exprStr.removeSurrounding("[", "]")
                    val parts = splitArgs(inner)
                    val vec = parts.map { mathEval.eval(it) }
                    vectorVariables[varName] = vec
                    if (vec.isNotEmpty()) {
                        scopeVars[varName] = vec[0]
                        variables[varName] = vec[0]
                    }
                } else {
                    val evaluatedVal = mathEval.eval(exprStr)
                    scopeVars[varName] = evaluatedVal
                    variables[varName] = evaluatedVal
                }
                return emptyList()
            }

            // 1. Block { statement1; statement2; ... }
            if (t == "{") {
                val blockVars = HashMap(variables).apply { putAll(scopeVars) }
                val blockStrings = HashMap(stringVariables).apply { putAll(scopeStrings) }
                val solids = mutableListOf<CsgSolid>()
                while (pos < tokens.size && peek() != "}") {
                    solids.addAll(parseStatement(blockVars, blockStrings))
                }
                if (pos < tokens.size && peek() == "}") next() // '}'
                return solids
            }

            // 2. Modul-Definition: module name(params) { ... }
            if (t == "module" || t.startsWith("module ")) {
                val header = if (t == "module") next() else t.removePrefix("module").trim()
                val modName = header.substringBefore("(").trim()
                val paramStr = header.substringAfter("(", "").substringBeforeLast(")", "")
                val argDefs = splitArgs(paramStr)
                val paramNames = mutableListOf<String>()
                val defaultValues = mutableMapOf<String, String>()

                for (arg in argDefs) {
                    val trimmed = arg.trim()
                    if (trimmed.isEmpty()) continue
                    if (trimmed.contains("=")) {
                        val p = trimmed.split("=", limit = 2)
                        val pName = p[0].trim()
                        paramNames.add(pName)
                        defaultValues[pName] = p[1].trim()
                    } else {
                        paramNames.add(trimmed)
                    }
                }

                // Sammle Body-Tokens
                val bodyTokens = mutableListOf<String>()
                if (peek() == "{") {
                    bodyTokens.add(next()) // '{'
                    var depth = 1
                    while (pos < tokens.size && depth > 0) {
                        val tok = next()
                        if (tok == "{") depth++
                        else if (tok == "}") depth--
                        bodyTokens.add(tok)
                    }
                }
                userModules[modName] = UserModule(paramNames, defaultValues, bodyTokens)
                return emptyList()
            }

            // 3. For-Schleife: for (i = [0:5]) { ... } oder for (i = [start:step:end]) { ... }
            if (t.matches(Regex("(?s)^for\\s*\\(.*\\)$"))) {
                val inside = t.substringAfter("(").substringBeforeLast(")")
                val eqIdx = inside.indexOf('=')
                if (eqIdx != -1) {
                    val loopVar = inside.substring(0, eqIdx).trim()
                    val loopRangeStr = inside.substring(eqIdx + 1).trim()

                    val loopValues = mutableListOf<Double>()
                    if (loopRangeStr.startsWith("[") && loopRangeStr.endsWith("]")) {
                        val innerRange = loopRangeStr.removeSurrounding("[", "]")
                        if (innerRange.contains(":")) {
                            val parts = innerRange.split(":")
                            val start = mathEval.eval(parts[0])
                            val (step, end) = if (parts.size == 2) {
                                val e = mathEval.eval(parts[1])
                                val s = if (e >= start) 1.0 else -1.0
                                Pair(s, e)
                            } else {
                                val s = mathEval.eval(parts[1])
                                val e = mathEval.eval(parts[2])
                                Pair(s, e)
                            }

                            if (step > 0) {
                                var curr = start
                                while (curr <= end + 1e-6) {
                                    loopValues.add(curr)
                                    curr += step
                                }
                            } else if (step < 0) {
                                var curr = start
                                while (curr >= end - 1e-6) {
                                    loopValues.add(curr)
                                    curr += step
                                }
                            }
                        } else {
                            val items = splitArgs(innerRange)
                            for (item in items) {
                                loopValues.add(mathEval.eval(item))
                            }
                        }
                    }

                    // Sammle Tokens des For-Bodys
                    val bodyTokens = mutableListOf<String>()
                    if (peek() == "{") {
                        bodyTokens.add(next()) // '{'
                        var depth = 1
                        while (pos < tokens.size && depth > 0) {
                            val tok = next()
                            if (tok == "{") depth++
                            else if (tok == "}") depth--
                            bodyTokens.add(tok)
                        }
                    } else {
                        // Ein einzelnes Statement
                        while (pos < tokens.size) {
                            val tok = next()
                            bodyTokens.add(tok)
                            if (tok == ";" || tok.matches(Regex("(?s)^(cube|cylinder|sphere|circle|square)\\s*\\(.*\\)$"))) {
                                if (peek() == ";") bodyTokens.add(next())
                                break
                            }
                        }
                    }

                    val loopSolids = mutableListOf<CsgSolid>()
                    for (lv in loopValues) {
                        val subParser = ScadParser()
                        subParser.userModules.putAll(userModules)
                        subParser.stringVariables.putAll(scopeStrings)
                        subParser.vectorVariables.putAll(vectorVariables)
                        val newVars = HashMap(variables)
                        newVars.putAll(scopeVars)
                        newVars[loopVar] = lv

                        val evaluated = subParser.parseAndEvaluate(
                            code = bodyTokens.joinToString(" "),
                            paramOverrides = newVars
                        )
                        loopSolids.add(evaluated.solid)
                    }

                    if (loopSolids.isEmpty()) return emptyList()
                    var res = loopSolids[0]
                    for (i in 1 until loopSolids.size) res = res.union(loopSolids[i])
                    return listOf(res)
                }
            }

            // 4. CSG Operationen: difference(), union(), intersection(), minkowski(), hull()
            if (t.matches(Regex("(?s)^(difference|union|intersection|minkowski|hull)\\s*\\(.*\\)$"))) {
                val op = t.substringBefore("(").trim()
                val children = mutableListOf<CsgSolid>()
                children.addAll(parseStatement(scopeVars, scopeStrings))

                if (children.isEmpty()) return emptyList()
                var res = children[0]
                when (op) {
                    "minkowski" -> {
                        for (i in 1 until children.size) res = res.minkowski(children[i])
                    }
                    "hull" -> {
                        res = CsgSolid.hull(children)
                    }
                    else -> {
                        for (i in 1 until children.size) {
                            res = when (op) {
                                "difference" -> res.difference(children[i])
                                "intersection" -> res.intersection(children[i])
                                else -> res.union(children[i])
                            }
                        }
                    }
                }
                return listOf(res)
            }

            // 5. Transformationen: translate(...), rotate(...), scale(...), mirror(...), color(...)
            if (t.matches(Regex("(?s)^(translate|rotate|scale|mirror|color)\\s*\\(.*\\)$"))) {
                val transName = t.substringBefore("(").trim()
                val argContent = t.substringAfter("(").substringBeforeLast(")")
                val args = parseArguments(argContent)

                // Hole Kind-Elemente
                val children = parseStatement(scopeVars, scopeStrings)
                if (children.isEmpty()) return emptyList()

                return children.map { child ->
                    when (transName) {
                        "translate" -> {
                            val vecStr = args["v"] ?: args["arg0"] ?: "[0,0,0]"
                            child.translate(parseVector(vecStr))
                        }
                        "rotate" -> {
                            val vecStr = args["a"] ?: args["arg0"] ?: "[0,0,0]"
                            if (vecStr.startsWith("[")) {
                                val v = parseVector(vecStr)
                                child.rotate(v.x, v.y, v.z)
                            } else {
                                val a = mathEval.eval(vecStr).toFloat()
                                val v = if (args.containsKey("v")) parseVector(args["v"]!!) else Vector3.UNIT_Z
                                child.rotate(v.x * a, v.y * a, v.z * a)
                            }
                        }
                        "scale" -> {
                            val vecStr = args["v"] ?: args["arg0"] ?: "[1,1,1]"
                            child.scale(parseVector(vecStr))
                        }
                        "mirror" -> {
                            val vecStr = args["v"] ?: args["arg0"] ?: "[1,0,0]"
                            child.mirror(parseVector(vecStr))
                        }
                        "color" -> {
                            val cStr = args["c"] ?: args["arg0"] ?: "gold"
                            child.setColor(parseColor(cStr))
                        }
                        else -> child
                    }
                }
            }

            // 6. Extrusionen: linear_extrude(...)
            if (t.matches(Regex("(?s)^linear_extrude\\s*\\(.*\\)$"))) {
                val argContent = t.substringAfter("(").substringBeforeLast(")")
                val args = parseArguments(argContent)
                val h = mathEval.eval(args["height"] ?: args["arg0"] ?: "10").toFloat()
                val center = (args["center"] ?: args["arg1"] ?: "false").trim() == "true"

                val children = parseStatement(scopeVars, scopeStrings)
                if (children.isEmpty()) return emptyList()

                return children.map { child ->
                    val finalSolid = child.scale(Vector3(1f, 1f, h))
                    if (center) finalSolid.translate(Vector3(0f, 0f, -h / 2f)) else finalSolid
                }
            }

            // 7. Primitives: cube(...); cylinder(...); sphere(...); square(...); circle(...);
            if (t.matches(Regex("(?s)^cube\\s*\\(.*\\)$"))) {
                val argContent = t.substringAfter("(").substringBeforeLast(")")
                val args = parseArguments(argContent)
                val sizeStr = args["size"] ?: args["arg0"] ?: "[10,10,10]"
                val center = (args["center"] ?: args["arg1"] ?: "false").trim() == "true"
                val size = parseVector(sizeStr)

                if (peek() == ";") next()
                return listOf(CsgSolid.cube(size, center))
            }

            if (t.matches(Regex("(?s)^cylinder\\s*\\(.*\\)$"))) {
                val argContent = t.substringAfter("(").substringBeforeLast(")")
                val args = parseArguments(argContent)

                val h = mathEval.eval(args["h"] ?: args["arg0"] ?: "10").toFloat()
                var r1 = 5f
                var r2 = 5f

                if (args.containsKey("r")) {
                    val r = mathEval.eval(args["r"]!!).toFloat()
                    r1 = r; r2 = r
                } else if (args.containsKey("d")) {
                    val r = (mathEval.eval(args["d"]!!) / 2.0).toFloat()
                    r1 = r; r2 = r
                } else if (args.containsKey("arg1") && args.containsKey("arg2")) {
                    r1 = mathEval.eval(args["arg1"]!!).toFloat()
                    r2 = mathEval.eval(args["arg2"]!!).toFloat()
                } else if (args.containsKey("arg1")) {
                    val r = mathEval.eval(args["arg1"]!!).toFloat()
                    r1 = r; r2 = r
                }

                if (args.containsKey("r1")) r1 = mathEval.eval(args["r1"]!!).toFloat()
                if (args.containsKey("r2")) r2 = mathEval.eval(args["r2"]!!).toFloat()
                if (args.containsKey("d1")) r1 = (mathEval.eval(args["d1"]!!) / 2.0).toFloat()
                if (args.containsKey("d2")) r2 = (mathEval.eval(args["d2"]!!) / 2.0).toFloat()

                val center = (args["center"] ?: args["arg3"] ?: "false").trim() == "true"
                val fn = (args["\$fn"]?.let { mathEval.eval(it) } ?: (scopeVars["\$fn"] ?: variables["\$fn"] ?: 32.0)).toInt().coerceIn(3, 128)

                if (peek() == ";") next()
                return listOf(CsgSolid.cylinder(h, r1, r2, fn, center))
            }

            if (t.matches(Regex("(?s)^sphere\\s*\\(.*\\)$"))) {
                val argContent = t.substringAfter("(").substringBeforeLast(")")
                val args = parseArguments(argContent)
                var r = 10f
                if (args.containsKey("r")) r = mathEval.eval(args["r"]!!).toFloat()
                else if (args.containsKey("d")) r = (mathEval.eval(args["d"]!!) / 2.0).toFloat()
                else if (args.containsKey("arg0")) r = mathEval.eval(args["arg0"]!!).toFloat()

                val fn = (args["\$fn"]?.let { mathEval.eval(it) } ?: (scopeVars["\$fn"] ?: variables["\$fn"] ?: 24.0)).toInt().coerceIn(6, 64)
                val slices = fn
                val stacks = (fn * 0.75).toInt().coerceAtLeast(4)

                if (peek() == ";") next()
                return listOf(CsgSolid.sphere(r, slices, stacks))
            }

            if (t.matches(Regex("(?s)^square\\s*\\(.*\\)$"))) {
                val argContent = t.substringAfter("(").substringBeforeLast(")")
                val args = parseArguments(argContent)
                val sizeStr = args["size"] ?: args["arg0"] ?: "[10,10]"
                val center = (args["center"] ?: args["arg1"] ?: "false").trim() == "true"
                val v = parseVector(sizeStr)
                val size = Vector3(v.x, v.y, 1f)
                if (peek() == ";") next()
                return listOf(CsgSolid.cube(size, center))
            }

            if (t.matches(Regex("(?s)^circle\\s*\\(.*\\)$"))) {
                val argContent = t.substringAfter("(").substringBeforeLast(")")
                val args = parseArguments(argContent)
                var r = 10f
                if (args.containsKey("r")) r = mathEval.eval(args["r"]!!).toFloat()
                else if (args.containsKey("d")) r = (mathEval.eval(args["d"]!!) / 2.0).toFloat()
                else if (args.containsKey("arg0")) r = mathEval.eval(args["arg0"]!!).toFloat()

                val fn = (args["\$fn"]?.let { mathEval.eval(it) } ?: (scopeVars["\$fn"] ?: variables["\$fn"] ?: 32.0)).toInt().coerceIn(3, 128)
                if (peek() == ";") next()
                return listOf(CsgSolid.cylinder(1f, r, r, fn, center = false))
            }

            if (t.matches(Regex("(?s)^polygon\\s*\\(.*\\)$"))) {
                val argContent = t.substringAfter("(").substringBeforeLast(")")
                val args = parseArguments(argContent)
                val pointsStr = args["points"] ?: args["arg0"] ?: "[]"
                val pts = mutableListOf<Vector3>()
                val ptRegex = Regex("\\[\\s*([^,\\]]+)\\s*,\\s*([^\\]]+)\\s*\\]")
                for (m in ptRegex.findAll(pointsStr)) {
                    val x = mathEval.eval(m.groupValues[1]).toFloat()
                    val y = mathEval.eval(m.groupValues[2]).toFloat()
                    pts.add(Vector3(x, y, 0f))
                }
                if (peek() == ";") next()
                return listOf(CsgSolid.polygon2d(pts))
            }

            // 8. Aufruf eines benutzerdefinierten Moduls
            val modCallName = t.substringBefore("(").trim()
            if (userModules.containsKey(modCallName)) {
                val module = userModules[modCallName]!!
                val argContent = t.substringAfter("(", "").substringBeforeLast(")", "")
                val passedArgs = parseArguments(argContent)

                val callVars = HashMap(variables)
                callVars.putAll(scopeVars)
                val callStrings = HashMap(stringVariables)
                callStrings.putAll(scopeStrings)

                // Standardwerte setzen
                for ((pName, pDef) in module.defaultValues) {
                    val trimmedDef = pDef.trim()
                    if ((trimmedDef.startsWith("\"") && trimmedDef.endsWith("\"")) || (trimmedDef.startsWith("'") && trimmedDef.endsWith("'"))) {
                        callStrings[pName] = trimmedDef.substring(1, trimmedDef.length - 1)
                    } else {
                        callVars[pName] = mathEval.eval(trimmedDef)
                    }
                }
                // Übergebene Argumente (positional & named)
                for ((idx, pName) in module.paramNames.withIndex()) {
                    val rawArg = passedArgs[pName] ?: passedArgs["arg$idx"]
                    if (rawArg != null) {
                        val argVal = rawArg.trim()
                        if ((argVal.startsWith("\"") && argVal.endsWith("\"")) || (argVal.startsWith("'") && argVal.endsWith("'"))) {
                            callStrings[pName] = argVal.substring(1, argVal.length - 1)
                        } else if (callStrings.containsKey(argVal)) {
                            callStrings[pName] = callStrings[argVal]!!
                        } else {
                            callVars[pName] = mathEval.eval(argVal)
                        }
                    }
                }

                if (peek() == ";") next()

                val subParser = ScadParser()
                subParser.userModules.putAll(userModules)
                subParser.stringVariables.putAll(callStrings)
                subParser.vectorVariables.putAll(vectorVariables)
                val evaluated = subParser.parseAndEvaluate(
                    code = module.bodyTokens.joinToString(" "),
                    paramOverrides = callVars
                )
                return listOf(evaluated.solid)
            }

            // Ignoriere alleinstehende Semikolons oder Zuweisungen
            if (t == ";" || t.contains("=")) {
                return emptyList()
            }

            return emptyList()
        }

        // Vorab-Scan: Registriere alle Module, damit sie aufgerufen werden können,
        // selbst wenn sie am Ende der Datei definiert sind (Standard in OpenSCAD)
        var prePos = 0
        while (prePos < tokens.size) {
            val tok = tokens[prePos]
            if (tok == "module" || tok.startsWith("module ")) {
                val header = if (tok == "module") {
                    prePos++
                    if (prePos < tokens.size) tokens[prePos] else ""
                } else tok.removePrefix("module").trim()
                val modName = header.substringBefore("(").trim()
                val paramStr = header.substringAfter("(", "").substringBeforeLast(")", "")
                val argDefs = splitArgs(paramStr)
                val paramNames = mutableListOf<String>()
                val defaultValues = mutableMapOf<String, String>()
                for (arg in argDefs) {
                    val trimmed = arg.trim()
                    if (trimmed.isEmpty()) continue
                    if (trimmed.contains("=")) {
                        val p = trimmed.split("=", limit = 2)
                        val pName = p[0].trim()
                        paramNames.add(pName)
                        defaultValues[pName] = p[1].trim()
                    } else {
                        paramNames.add(trimmed)
                    }
                }
                prePos++
                val bodyTokens = mutableListOf<String>()
                if (prePos < tokens.size && tokens[prePos] == "{") {
                    bodyTokens.add(tokens[prePos])
                    prePos++
                    var depth = 1
                    while (prePos < tokens.size && depth > 0) {
                        val bTok = tokens[prePos]
                        if (bTok == "{") depth++
                        else if (bTok == "}") depth--
                        bodyTokens.add(bTok)
                        prePos++
                    }
                }
                userModules[modName] = UserModule(paramNames, defaultValues, bodyTokens)
            } else {
                prePos++
            }
        }

        val allSolids = mutableListOf<CsgSolid>()
        val topVars = HashMap(variables)
        val topStrings = HashMap(stringVariables)
        while (pos < tokens.size) {
            allSolids.addAll(parseStatement(topVars, topStrings))
        }

        val finalSolid = if (allSolids.isEmpty()) {
            CsgSolid(emptyList())
        } else {
            val allPolys = ArrayList<CsgPolygon>()
            for (s in allSolids) allPolys.addAll(s.polygons)
            CsgSolid(allPolys)
        }

        logs.add("OpenSCAD Modell erfolgreich berechnet: ${finalSolid.polygons.size} Polygone.")
        return ParseResult(finalSolid, params, logs)
    }

    private fun extractParameters(code: String): List<CustomizerParam> {
        val result = mutableListOf<CustomizerParam>()
        var currentGroup = "Allgemein"

        val lines = code.lines()
        val groupRegex = Regex("^/\\*\\s*\\[(.*?)\\]\\s*\\*/")
        val paramRegex = Regex("^([a-zA-Z0-9_]+)\\s*=\\s*([^;]+);(?:\\s*//\\s*(.*))?")

        for (line in lines) {
            val trimmed = line.trim()
            val groupMatch = groupRegex.find(trimmed)
            if (groupMatch != null) {
                currentGroup = groupMatch.groupValues[1].trim()
                continue
            }

            val paramMatch = paramRegex.find(trimmed)
            if (paramMatch != null) {
                val name = paramMatch.groupValues[1]
                val rawValue = paramMatch.groupValues[2].trim()
                val comment = if (paramMatch.groupValues.size > 3) paramMatch.groupValues[3].trim() else ""

                if (name.startsWith("$")) continue

                if (rawValue.equals("true", ignoreCase = true) || rawValue.equals("false", ignoreCase = true)) {
                    result.add(
                        CustomizerParam.BooleanParam(
                            name = name,
                            label = comment.ifEmpty { name },
                            group = currentGroup,
                            value = rawValue.toBoolean()
                        )
                    )
                } else if (rawValue.toDoubleOrNull() != null) {
                    val num = rawValue.toDouble()
                    var min: Double? = null
                    var max: Double? = null
                    var step: Double? = null

                    if (comment.startsWith("[") && comment.endsWith("]")) {
                        val rangeInner = comment.removeSurrounding("[", "]")
                        val parts = rangeInner.split(":")
                        if (parts.size == 2) {
                            min = parts[0].toDoubleOrNull()
                            max = parts[1].toDoubleOrNull()
                        } else if (parts.size == 3) {
                            min = parts[0].toDoubleOrNull()
                            step = parts[1].toDoubleOrNull()
                            max = parts[2].toDoubleOrNull()
                        }
                    }

                    if (min == null) min = (num * 0.2).coerceAtMost(0.0)
                    if (max == null) max = (num * 2.5).coerceAtLeast(100.0)

                    result.add(
                        CustomizerParam.NumberParam(
                            name = name,
                            label = name,
                            group = currentGroup,
                            value = num,
                            min = min,
                            max = max,
                            step = step ?: 1.0
                        )
                    )
                }
            }
        }
        return result
    }

    /**
     * Zerlegt OpenSCAD Quellcode sauber in syntaktische Einheiten (Module, Operatoren, Blöcke).
     * Behandelt Leerzeichen zwischen Bezeichner und Klammern (z.B. "translate ([1, 2, 3])" oder "for (i = [0:5])")
     */
    private fun tokenizeCode(code: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        val len = code.length

        while (i < len) {
            val c = code[i]

            if (c.isWhitespace()) {
                i++
                continue
            }

            // String-Literale "..." oder '...'
            if (c == '"' || c == '\'') {
                val quote = c
                val start = i
                i++
                while (i < len && code[i] != quote) {
                    if (code[i] == '\\' && i + 1 < len) i++
                    i++
                }
                if (i < len && code[i] == quote) i++
                tokens.add(code.substring(start, i))
                continue
            }

            if (c == '{' || c == '}' || c == ';') {
                tokens.add(c.toString())
                i++
                continue
            }

            // Wenn es ein Bezeichner oder Modulname ist (z.B. translate, rotate, for, cube, module, variable)
            if (c.isLetter() || c == '$' || c == '_') {
                val start = i
                while (i < len && (code[i].isLetterOrDigit() || code[i] == '$' || code[i] == '_')) {
                    i++
                }
                val id = code.substring(start, i)

                // Prüfe, ob danach (evtl. mit Leerzeichen) eine Klammer '(' folgt
                var peekIdx = i
                while (peekIdx < len && code[peekIdx].isWhitespace()) {
                    peekIdx++
                }

                // Ausnahme: "module" soll als einzelner Token bleiben, da der Modulname erst danach kommt!
                if (id != "module" && peekIdx < len && code[peekIdx] == '(') {
                    i = peekIdx // überspringe Whitespace zur Klammer
                    var parenDepth = 0
                    var inBracket = 0
                    val argStart = i
                    while (i < len) {
                        val ch = code[i]
                        if (ch == '(') parenDepth++
                        else if (ch == ')') {
                            parenDepth--
                            if (parenDepth == 0) {
                                i++ // schließe ')' mit ein
                                break
                            }
                        } else if (ch == '[') inBracket++
                        else if (ch == ']') inBracket--
                        i++
                    }
                    val argPart = code.substring(argStart, i)
                    tokens.add("$id$argPart")
                } else {
                    tokens.add(id)
                }
                continue
            }

            // Klammerblock '(' ... ')'
            if (c == '(') {
                val start = i
                var depth = 0
                while (i < len) {
                    if (code[i] == '(') depth++
                    else if (code[i] == ')') {
                        depth--
                        if (depth == 0) {
                            i++
                            break
                        }
                    }
                    i++
                }
                tokens.add(code.substring(start, i))
                continue
            }

            // Vektorblock '[' ... ']'
            if (c == '[') {
                val start = i
                var depth = 0
                while (i < len) {
                    if (code[i] == '[') depth++
                    else if (code[i] == ']') {
                        depth--
                        if (depth == 0) {
                            i++
                            break
                        }
                    }
                    i++
                }
                tokens.add(code.substring(start, i))
                continue
            }

            // Sonstige Token (Zuweisungen, Operatoren, etc.)
            val start = i
            while (i < len && !code[i].isWhitespace() && code[i] != '{' && code[i] != '}' && code[i] != ';' && code[i] != '(' && code[i] != '[' && !code[i].isLetter() && code[i] != '$' && code[i] != '_') {
                i++
            }
            val tok = code.substring(start, i).trim()
            if (tok.isNotEmpty()) {
                tokens.add(tok)
            }
        }
        return tokens
    }

    private fun splitArgs(argStr: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var bracketDepth = 0
        var parenDepth = 0
        for (c in argStr) {
            if (c == '[') bracketDepth++
            if (c == ']') bracketDepth--
            if (c == '(') parenDepth++
            if (c == ')') parenDepth--

            if (c == ',' && bracketDepth == 0 && parenDepth == 0) {
                result.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
        }
        if (sb.isNotEmpty()) result.add(sb.toString().trim())
        return result
    }
}
