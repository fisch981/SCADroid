package de.fisch981.scadroid.ui.editor

import android.graphics.Typeface
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

@Composable
fun CodeEditorView(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editorRef: CodeEditor? = remember { null }

    val shortcutTokens = listOf(";", "{", "}", "[", "]", "(", ")", "=", "\$fn = ", "cube()", "cylinder()", "sphere()", "difference()", "union()")

    Column(modifier = modifier.fillMaxSize()) {
        // Quick Symbol Toolbar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            items(shortcutTokens) { token ->
                AssistChip(
                    onClick = {
                        editorRef?.let { editor ->
                            val cursor = editor.cursor
                            editor.text.insert(cursor.leftLine, cursor.leftColumn, token)
                            onCodeChange(editor.text.toString())
                        }
                    },
                    label = { Text(token, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        // Native Sora Code Editor
        AndroidView(
            factory = { context ->
                CodeEditor(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    typefaceText = Typeface.MONOSPACE
                    typefaceLineNumber = Typeface.MONOSPACE
                    setTextSize(15f)
                    isLineNumberEnabled = true
                    isWordwrap = false

                    // Setze den Initialcode
                    setText(code)

                    // Text-Change Listener
                    subscribeAlways(io.github.rosemoe.sora.event.ContentChangeEvent::class.java) {
                        onCodeChange(text.toString())
                    }

                    editorRef = this
                }
            },
            update = { editor ->
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
