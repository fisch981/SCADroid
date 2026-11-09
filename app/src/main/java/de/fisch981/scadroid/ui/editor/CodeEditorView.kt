package de.fisch981.scadroid.ui.editor

import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.fisch981.scadroid.ui.theme.DarkBackground
import de.fisch981.scadroid.ui.theme.DarkBorder
import de.fisch981.scadroid.ui.theme.DarkSurface
import de.fisch981.scadroid.ui.theme.ScadGold
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula

@Composable
fun CodeEditorView(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val editorState = remember { mutableStateOf<CodeEditor?>(null) }
    var currentPrefix by remember { mutableStateOf("") }
    var showAutoDropdown by remember { mutableStateOf(false) }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }

    val defaultSymbols = listOf(";", "{", "}", "[", "]", "(", ")", "=", "#", "!", "%", "*", "\$fn = ", "cube()", "cylinder()", "sphere()", "difference()", "union()")

    // Finde passende Vorschläge basierend auf aktuellem Teilwort unter dem Cursor
    val matchingSnippets = if (currentPrefix.length >= 1) {
        OPENSCAD_SNIPPETS.filter {
            it.label.lowercase().startsWith(currentPrefix.lowercase()) ||
            it.label.lowercase().contains(currentPrefix.lowercase())
        }
    } else emptyList()

    val chipColors = AssistChipDefaults.assistChipColors(
        containerColor = Color(0xFF282828),
        labelColor = ScadGold
    )
    val chipBorder = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    val labelStyle = MaterialTheme.typography.labelSmall

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Quick Symbol & Dynamic Auto-Completion Toolbar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .border(1.dp, DarkBorder)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // 1. Undo-Button (Rückgängig)
            item {
                AssistChip(
                    onClick = {
                        editorState.value?.let { editor ->
                            if (editor.canUndo()) {
                                editor.undo()
                                onCodeChange(editor.text.toString())
                                canUndo = editor.canUndo()
                                canRedo = editor.canRedo()
                            }
                        }
                    },
                    enabled = canUndo,
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Rückgängig",
                            modifier = Modifier.size(16.dp),
                            tint = if (canUndo) ScadGold else Color(0xFF666666)
                        )
                    },
                    label = {
                        Text(
                            "Undo",
                            style = labelStyle,
                            color = if (canUndo) Color.White else Color(0xFF666666),
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (canUndo) Color(0xFF282828) else Color(0xFF1A1A1A),
                        disabledContainerColor = Color(0xFF181818)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (canUndo) DarkBorder else Color(0xFF242424)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            // 2. Redo-Button (Wiederholen)
            item {
                AssistChip(
                    onClick = {
                        editorState.value?.let { editor ->
                            if (editor.canRedo()) {
                                editor.redo()
                                onCodeChange(editor.text.toString())
                                canUndo = editor.canUndo()
                                canRedo = editor.canRedo()
                            }
                        }
                    },
                    enabled = canRedo,
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Wiederholen",
                            modifier = Modifier.size(16.dp),
                            tint = if (canRedo) ScadGold else Color(0xFF666666)
                        )
                    },
                    label = {
                        Text(
                            "Redo",
                            style = labelStyle,
                            color = if (canRedo) Color.White else Color(0xFF666666),
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (canRedo) Color(0xFF282828) else Color(0xFF1A1A1A),
                        disabledContainerColor = Color(0xFF181818)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (canRedo) DarkBorder else Color(0xFF242424)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            // 3. Auto-Vervollständigungs-Button mit Dropdown
            item {
                Box {
                    AssistChip(
                        onClick = {
                            showAutoDropdown = true
                            editorState.value?.let { editor ->
                                editor.getComponent(EditorAutoCompletion::class.java).requireCompletion()
                            }
                        },
                        label = {
                            Text(
                                "✨ Befehle",
                                style = labelStyle,
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF383318),
                            labelColor = Color(0xFFFFD54F)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5A93C)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    DropdownMenu(
                        expanded = showAutoDropdown,
                        onDismissRequest = { showAutoDropdown = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        OPENSCAD_SNIPPETS.forEach { snip ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(snip.label, color = ScadGold, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text(snip.desc, color = Color(0xFFB0B0B0), style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                onClick = {
                                    showAutoDropdown = false
                                    editorState.value?.let { editor ->
                                        editor.requestFocus()
                                        if (currentPrefix.isNotEmpty()) {
                                            val cursor = editor.cursor
                                            editor.text.replace(cursor.leftLine, cursor.leftColumn - currentPrefix.length, cursor.leftLine, cursor.leftColumn, snip.commitText)
                                        } else {
                                            editor.commitText(snip.commitText)
                                        }
                                        onCodeChange(editor.text.toString())
                                        currentPrefix = ""
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 2. Wenn der Nutzer gerade ein Wort tippt: Zeige die passenden Befehlsvorschläge an!
            if (matchingSnippets.isNotEmpty()) {
                items(matchingSnippets) { snip ->
                    AssistChip(
                        onClick = {
                            editorState.value?.let { editor ->
                                editor.requestFocus()
                                val cursor = editor.cursor
                                val pLen = currentPrefix.length
                                editor.text.replace(
                                    cursor.leftLine,
                                    cursor.leftColumn - pLen,
                                    cursor.leftLine,
                                    cursor.leftColumn,
                                    snip.commitText
                                )
                                onCodeChange(editor.text.toString())
                                currentPrefix = ""
                            }
                        },
                        label = {
                            Text(
                                "✨ ${snip.label}",
                                style = labelStyle,
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF332D12),
                            labelColor = Color(0xFFFFD54F)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB38600)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }

            // 3. Standard-Symbole und Klammern
            items(defaultSymbols) { token ->
                AssistChip(
                    onClick = {
                        val editor = editorState.value ?: return@AssistChip
                        editor.requestFocus()
                        when (token) {
                            "{}" -> editor.commitText("{\n    \n}")
                            "()" -> editor.commitText("()")
                            "[]" -> editor.commitText("[]")
                            "cube()" -> editor.commitText("cube([10, 10, 10]);")
                            "cylinder()" -> editor.commitText("cylinder(h = 10, r = 5);")
                            "sphere()" -> editor.commitText("sphere(r = 10);")
                            "difference()" -> editor.commitText("difference() {\n    \n}")
                            "union()" -> editor.commitText("union() {\n    \n}")
                            else -> editor.commitText(token)
                        }
                        onCodeChange(editor.text.toString())
                    },
                    label = {
                        Text(
                            token,
                            style = labelStyle,
                            color = ScadGold,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = chipColors,
                    border = chipBorder,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        // Native Sora Code Editor in Darcula Dark Theme mit OpenSCAD Syntax-Highlighting & Autovervollständigung
        AndroidView(
            factory = { context ->
                CodeEditor(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    colorScheme = OpenScadColorScheme()
                    setBackgroundColor(AndroidColor.parseColor("#14161A"))
                    typefaceText = Typeface.MONOSPACE
                    typefaceLineNumber = Typeface.MONOSPACE
                    setTextSize(15f)
                    isLineNumberEnabled = true
                    isWordwrap = false

                    // Auto-Completion & Highlighting Props
                    props.autoCompletionOnComposing = true
                    props.symbolPairAutoCompletion = true
                    props.autoIndent = true
                    props.disallowSuggestions = false
                    props.highlightMatchingDelimiters = true
                    isUndoEnabled = true

                    // OpenSCAD Sprachunterstützung (Syntax-Highlighting & Vervollständigung)
                    val lang = OpenScadLanguage()
                    setEditorLanguage(lang)

                    val autoCompletion = getComponent(EditorAutoCompletion::class.java)
                    autoCompletion.isEnabled = true
                    autoCompletion.setCompletionWndPositionMode(EditorAutoCompletion.WINDOW_POS_MODE_AUTO)
                    autoCompletion.applyColorScheme()

                    // Setze den Initialcode
                    setText(code)

                    // Text-Change & Cursor-Change Listener zur Erkennung des aktuellen Worts
                    val updatePrefix = {
                        val cursor = cursor
                        val lineIdx = cursor.leftLine
                        val colIdx = cursor.leftColumn
                        val txt = text
                        if (lineIdx < txt.lineCount) {
                            val lineStr = txt.getLineString(lineIdx)
                            val col = colIdx.coerceIn(0, lineStr.length)
                            var start = col
                            while (start > 0 && (lineStr[start - 1].isLetterOrDigit() || lineStr[start - 1] == '$' || lineStr[start - 1] == '_')) {
                                start--
                            }
                            currentPrefix = lineStr.substring(start, col)
                        } else {
                            currentPrefix = ""
                        }
                    }

                    val syncUndoRedo = {
                        canUndo = canUndo()
                        canRedo = canRedo()
                    }

                    subscribeAlways(io.github.rosemoe.sora.event.ContentChangeEvent::class.java) {
                        onCodeChange(text.toString())
                        updatePrefix()
                        syncUndoRedo()
                    }

                    subscribeAlways(io.github.rosemoe.sora.event.SelectionChangeEvent::class.java) {
                        updatePrefix()
                        syncUndoRedo()
                    }

                    editorState.value = this
                    syncUndoRedo()
                }
            },
            update = { editor ->
                editorState.value = editor
                canUndo = editor.canUndo()
                canRedo = editor.canRedo()
                if (editor.text.toString() != code) {
                    val cursor = editor.cursor
                    val line = cursor.leftLine
                    val col = cursor.leftColumn
                    editor.setText(code)
                    try {
                        editor.setSelection(line, col)
                    } catch (_: Exception) {}
                    canUndo = editor.canUndo()
                    canRedo = editor.canRedo()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
