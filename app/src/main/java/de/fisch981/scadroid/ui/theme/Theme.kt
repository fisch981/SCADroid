package de.fisch981.scadroid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SCADroidDarkColorScheme = darkColorScheme(
    primary = ScadGold,
    onPrimary = DarkBackground,
    primaryContainer = Color(0xFF3E3114),
    onPrimaryContainer = ScadGold,
    secondary = ScadBlue,
    onSecondary = DarkBackground,
    secondaryContainer = Color(0xFF132F4C),
    onSecondaryContainer = Color(0xFF90CAF9),
    tertiary = ScadGreen,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF424242),
    error = ScadRed,
    onError = TextPrimary
)

@Composable
fun SCADroidTheme(
    content: @Composable () -> Unit
) {
    // Erzwingt konsistentes, hochkontrastreiches Dark Theme für CAD/IDE
    MaterialTheme(
        colorScheme = SCADroidDarkColorScheme,
        content = content
    )
}
