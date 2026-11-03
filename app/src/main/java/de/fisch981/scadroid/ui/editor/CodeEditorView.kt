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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    val shortcutTokens = listOf("✨ Auto", ";", "{", "}", "[", "]", "(", ")", "=", "\$fn = ", "cube()", "cylinder()", "sphere()", "difference()", "union()")

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
        // Quick Symbol & Snippet Toolbar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .border(1.dp, DarkBorder)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(shortcutTokens) { token ->
                val isAuto = token == "✨ Auto"
                AssistChip(
                    onClick = {
                        val editor = editorState.value ?: return@AssistChip
                        editor.requestFocus()
                        if (isAuto) {
                            val autoComp = editor.getComponent(EditorAutoCompletion::class.java)
                            autoComp.requireCompletion()
                        } else {
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
                        }
                    },
                    label = {
                        Text(
                            token,
                            style = labelStyle,
                            color = if (isAuto) Color(0xFFFFD54F) else ScadGold,
                            fontWeight = if (isAuto) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = if (isAuto) androidx.compose.ui.text.font.FontFamily.Default else androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    },
                    colors = if (isAuto) {
                        AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF383318),
                            labelColor = Color(0xFFFFD54F)
                        )
                    } else chipColors,
                    border = chipBorder,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        // Native Sora Code Editor in Darcula Dark Theme mit OpenSCAD Autovervollständigung
        AndroidView(
            factory = { context ->
                CodeEditor(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    colorScheme = SchemeDarcula()
                    setBackgroundColor(AndroidColor.parseColor("#14161A"))
                    typefaceText = Typeface.MONOSPACE
                    typefaceLineNumber = Typeface.MONOSPACE
                    setTextSize(15f)
                    isLineNumberEnabled = true
                    isWordwrap = false

                    // Auto-Completion & Editing Props
                    props.autoCompletionOnComposing = true
                    props.symbolPairAutoCompletion = true
                    props.autoIndent = true
                    props.disallowSuggestions = false
                    props.highlightMatchingDelimiters = true

                    // OpenSCAD Autovervollständigung aktivieren
                    setEditorLanguage(OpenScadLanguage())
                    val autoCompletion = getComponent(EditorAutoCompletion::class.java)
                    autoCompletion.isEnabled = true
                    autoCompletion.setCompletionWndPositionMode(EditorAutoCompletion.WINDOW_POS_MODE_AUTO)
                    autoCompletion.applyColorScheme()

                    // Setze den Initialcode
                    setText(code)

                    // Text-Change Listener
                    subscribeAlways(io.github.rosemoe.sora.event.ContentChangeEvent::class.java) {
                        onCodeChange(text.toString())
                    }

                    editorState.value = this
                }
            },
            update = { editor ->
                editorState.value = editor
                if (editor.text.toString() != code) {
                    val cursor = editor.cursor
                    val line = cursor.leftLine
                    val col = cursor.leftColumn
                    editor.setText(code)
                    try {
                        editor.setSelection(line, col)
                    } catch (_: Exception) {}
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
