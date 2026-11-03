package de.fisch981.scadroid.engine

import de.fisch981.scadroid.model.CustomizerParam
import java.util.regex.Pattern

/**
 * Leichtgewichtiger, robuster OpenSCAD AST- & CSG-Parser
 */
class ScadParser {

    data class ParseResult(
        val solid: CsgSolid,
        val parameters: List<CustomizerParam>,
        val logMessages: List<String>
    )

    fun parseAndEvaluate(code: String, paramOverrides: Map<String, Any> = emptyMap()): ParseResult {
        val logs = mutableListOf<String>()
        val params = extractParameters(code)

        // Variablen-Werte sammeln (Standardwerte oder Überschreibungen)
        val variables = mutableMapOf<String, Double>()
        val booleanVars = mutableMapOf<String, Boolean>()

        // 1. Aus Code extrahieren
        val varPattern = Pattern.compile("(?m)^\\s*([a-zA-Z0-9_\\$]+)\\s*=\\s*([^;]+);")
        val varMatcher = varPattern.matcher(code)
        while (varMatcher.find()) {
            val name = varMatcher.group(1) ?: continue
            val expr = (varMatcher.group(2) ?: "").trim()
            if (expr.equals("true", ignoreCase = true)) {
                booleanVars[name] = true
            } else if (expr.equals("false", ignoreCase = true)) {
                booleanVars[name] = false
            } else {
                expr.toDoubleOrNull()?.let { variables[name] = it }
            }
        }

        // 2. Überschreibungen aus UI anwenden
        for ((k, v) in paramOverrides) {
            when (v) {
                is Double -> variables[k] = v
                is Float -> variables[k] = v.toDouble()
                is Number -> variables[k] = v.toDouble()
                is Boolean -> booleanVars[k] = v
            }
        }

        // Kommentare entfernen für AST-Auswertung
        val cleanCode = code
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("//.*"), "")

        // Rekursiver Parser
        val tokens = tokenize(cleanCode)
        var pos = 0

        fun evaluateNumber(token: String): Double {
            val t = token.trim()
            if (variables.containsKey(t)) return variables[t]!!
            return t.toDoubleOrNull() ?: 0.0
        }

        fun parseArguments(argString: String): Map<String, String> {
            val res = mutableMapOf<String, String>()
            val parts = splitArgs(argString)
            var anonIdx = 0
            for (p in parts) {
                val trimmed = p.trim()
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
            val inner = vecStr.trim().removeSurrounding("[", "]")
            val parts = inner.split(",")
            val x = if (parts.isNotEmpty()) evaluateNumber(parts[0]).toFloat() else 0f
            val y = if (parts.size > 1) evaluateNumber(parts[1]).toFloat() else 0f
            val z = if (parts.size > 2) evaluateNumber(parts[2]).toFloat() else 0f
            return Vector3(x, y, z)
        }

        fun parseNode(): CsgSolid? {
            if (pos >= tokens.size) return null
            val token = tokens[pos++]

            // Module mit Block { ... }
            if (token.startsWith("difference") || token.startsWith("union") || token.startsWith("intersection")) {
                val op = token.substringBefore("(").trim()
                // Erwarte Block { ... }
                val children = mutableListOf<CsgSolid>()
                if (pos < tokens.size && tokens[pos] == "{") {
                    pos++ // überspringe {
                    while (pos < tokens.size && tokens[pos] != "}") {
                        val child = parseNode()
                        if (child != null) children.add(child)
                    }
                    if (pos < tokens.size && tokens[pos] == "}") pos++ // überspringe }
                }

                if (children.isEmpty()) return null
                var result = children[0]
                for (i in 1 until children.size) {
                    result = when (op) {
                        "difference" -> result.difference(children[i])
                        "intersection" -> result.intersection(children[i])
                        else -> result.union(children[i])
                    }
                }
                return result
            }

            // Transformationen
            if (token.startsWith("translate") || token.startsWith("rotate") || token.startsWith("scale") || token.startsWith("color")) {
                val transName = token.substringBefore("(").trim()
                val argContent = token.substringAfter("(").substringBeforeLast(")")
                val args = parseArguments(argContent)

                // Hole Kind-Element(e)
                val children = mutableListOf<CsgSolid>()
                if (pos < tokens.size && tokens[pos] == "{") {
                    pos++
                    while (pos < tokens.size && tokens[pos] != "}") {
                        val c = parseNode()
                        if (c != null) children.add(c)
                    }
                    if (pos < tokens.size && tokens[pos] == "}") pos++
                } else {
                    val single = parseNode()
                    if (single != null) children.add(single)
                }

                if (children.isEmpty()) return null
                var combined = children[0]
                for (i in 1 until children.size) combined = combined.union(children[i])

                return when (transName) {
                    "translate" -> {
                        val vecStr = args["v"] ?: args["arg0"] ?: "[0,0,0]"
                        combined.translate(parseVector(vecStr))
                    }
                    "rotate" -> {
                        val vecStr = args["a"] ?: args["arg0"] ?: "[0,0,0]"
                        if (vecStr.startsWith("[")) {
                            val v = parseVector(vecStr)
                            combined.rotate(v.x, v.y, v.z)
                        } else {
                            val a = evaluateNumber(vecStr).toFloat()
                            val v = if (args.containsKey("v")) parseVector(args["v"]!!) else Vector3.UNIT_Z
                            combined.rotate(v.x * a, v.y * a, v.z * a)
                        }
                    }
                    "scale" -> {
                        val vecStr = args["v"] ?: args["arg0"] ?: "[1,1,1]"
                        combined.scale(parseVector(vecStr))
                    }
                    "color" -> {
                        val vecStr = args["c"] ?: args["arg0"] ?: "[1,1,1]"
                        val c = parseVector(vecStr)
                        combined.setColor(c)
                    }
                    else -> combined
                }
            }

            // Primitiven
            if (token.startsWith("cube")) {
                val argContent = token.substringAfter("(").substringBeforeLast(")")
                val args = parseArguments(argContent)
                val sizeStr = args["size"] ?: args["arg0"] ?: "[10,10,10]"
                val center = (args["center"] ?: "false").trim() == "true"
                val size = if (sizeStr.startsWith("[")) {
                    parseVector(sizeStr)
                } else {
                    val s = evaluateNumber(sizeStr).toFloat()
                    Vector3(s, s, s)
                }
                return CsgSolid.cube(size, center)
            }

            if (token.startsWith("cylinder")) {
                val argContent = token.substringAfter("(").substringBeforeLast(")")
                val args = parseArguments(argContent)
                val h = evaluateNumber(args["h"] ?: "10").toFloat()
                var r1 = 5f
                var r2 = 5f
                if (args.containsKey("r")) {
                    val r = evaluateNumber(args["r"]!!).toFloat()
                    r1 = r; r2 = r
                } else if (args.containsKey("d")) {
                    val r = (evaluateNumber(args["d"]!!) / 2.0).toFloat()
                    r1 = r; r2 = r
                }
                if (args.containsKey("r1")) r1 = evaluateNumber(args["r1"]!!).toFloat()
                if (args.containsKey("r2")) r2 = evaluateNumber(args["r2"]!!).toFloat()
                if (args.containsKey("d1")) r1 = (evaluateNumber(args["d1"]!!) / 2.0).toFloat()
                if (args.containsKey("d2")) r2 = (evaluateNumber(args["d2"]!!) / 2.0).toFloat()

                val center = (args["center"] ?: "false").trim() == "true"
                val fn = (variables["\$fn"] ?: 32.0).toInt().coerceIn(12, 48)
                return CsgSolid.cylinder(h, r1, r2, fn, center)
            }

            if (token.startsWith("sphere")) {
                val argContent = token.substringAfter("(").substringBeforeLast(")")
                val args = parseArguments(argContent)
                var r = 10f
                if (args.containsKey("r")) r = evaluateNumber(args["r"]!!).toFloat()
                if (args.containsKey("d")) r = (evaluateNumber(args["d"]!!) / 2.0).toFloat()
                return CsgSolid.sphere(r, 16, 12)
            }

            return null
        }

        val rootSolids = mutableListOf<CsgSolid>()
        while (pos < tokens.size) {
            val s = parseNode()
            if (s != null) rootSolids.add(s)
        }

        val finalSolid = if (rootSolids.isEmpty()) {
            CsgSolid.cube(Vector3(20f, 20f, 10f), center = true)
        } else {
            var combined = rootSolids[0]
            for (i in 1 until rootSolids.size) combined = combined.union(rootSolids[i])
            combined
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

                if (name.startsWith("$")) continue // Spezialvariablen wie $fn ignorieren

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

                    // Prüfe ob Kommentar Bereich hat wie [10:1:100] oder [10:100]
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

    private fun tokenize(code: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var parenDepth = 0

        for (i in code.indices) {
            val c = code[i]
            if (c == '(') parenDepth++
            if (c == ')') parenDepth--

            if (parenDepth == 0 && (c == ';' || c == '{' || c == '}')) {
                val t = sb.toString().trim()
                if (t.isNotEmpty()) tokens.add(t)
                sb.clear()
                if (c == '{' || c == '}') tokens.add(c.toString())
            } else {
                sb.append(c)
            }
        }
        val remaining = sb.toString().trim()
        if (remaining.isNotEmpty()) tokens.add(remaining)
        return tokens
    }

    private fun splitArgs(argStr: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var bracketDepth = 0
        for (c in argStr) {
            if (c == '[') bracketDepth++
            if (c == ']') bracketDepth--
            if (c == ',' && bracketDepth == 0) {
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
