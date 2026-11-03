package de.fisch981.scadroid.ui.customizer

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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.fisch981.scadroid.model.CustomizerParam

@Composable
fun CustomizerView(
    parameters: List<CustomizerParam>,
    onParameterChanged: (CustomizerParam) -> Unit,
    modifier: Modifier = Modifier
) {
    if (parameters.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Keine Customizer-Parameter gefunden",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Definiere Variablen im SCAD-Code wie:\n/* [Einstellungen] */\nbreite = 30; // [10:1:100]",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            items(parameters) { param ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
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
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = String.format("%.1f", sliderValue),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                Slider(
                                    value = sliderValue,
                                    onValueChange = {
                                        sliderValue = it
                                        onParameterChanged(param.copy(value = it.toDouble()))
                                    },
                                    valueRange = min..max
                                )
                            }
                            is CustomizerParam.BooleanParam -> {
                                var checked by remember(param.value) { mutableStateOf(param.value) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = param.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = checked,
                                        onCheckedChange = {
                                            checked = it
                                            onParameterChanged(param.copy(value = it))
                                        }
                                    )
                                }
                            }
                            is CustomizerParam.StringParam -> {
                                Text(
                                    text = "${param.label}: ${param.value}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
