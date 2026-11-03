package de.fisch981.scadroid.ui.customizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.fisch981.scadroid.model.CustomizerParam
import de.fisch981.scadroid.ui.theme.DarkBackground
import de.fisch981.scadroid.ui.theme.DarkBorder
import de.fisch981.scadroid.ui.theme.DarkSurface
import de.fisch981.scadroid.ui.theme.ScadGold

@Composable
fun CustomizerView(
    parameters: List<CustomizerParam>,
    onParameterChanged: (CustomizerParam) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (parameters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Keine Customizer-Parameter gefunden",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Definiere Variablen im SCAD-Code wie:\n\n/* [Geometrie] */\nbreite = 30; // [10:1:100]\nhoehe = 15; // [5:1:50]\nmit_bohrung = true;",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB0B0B0),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                items(parameters) { param ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            when (param) {
                                is CustomizerParam.NumberParam -> {
                                    var sliderValue by remember(param.value) {
                                        mutableFloatStateOf(param.value.toFloat())
                                    }
                                    val min = param.min?.toFloat() ?: 0f
                                    val max = param.max?.toFloat() ?: 100f

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = param.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = if (sliderValue % 1f == 0f) "${sliderValue.toInt()}" else String.format("%.1f", sliderValue),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ScadGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = sliderValue,
                                        onValueChange = {
                                            sliderValue = it
                                            onParameterChanged(param.copy(value = it.toDouble()))
                                        },
                                        valueRange = min..max,
                                        colors = SliderDefaults.colors(
                                            thumbColor = ScadGold,
                                            activeTrackColor = ScadGold,
                                            inactiveTrackColor = Color(0xFF383838)
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Min: ${min.toInt()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF888888)
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "Max: ${max.toInt()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF888888)
                                        )
                                    }
                                }
                                is CustomizerParam.BooleanParam -> {
                                    var checked by remember(param.value) { mutableStateOf(param.value) }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = param.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = checked,
                                            onCheckedChange = {
                                                checked = it
                                                onParameterChanged(param.copy(value = it))
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF121212),
                                                checkedTrackColor = ScadGold,
                                                uncheckedThumbColor = Color(0xFFAAAAAA),
                                                uncheckedTrackColor = Color(0xFF383838)
                                            )
                                        )
                                    }
                                }
                                is CustomizerParam.StringParam -> {
                                    Text(
                                        text = "${param.label}: ${param.value}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
