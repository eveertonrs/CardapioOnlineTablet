package com.helptech.abraham.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions as TextKeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.helptech.abraham.settings.AppSettings
import com.helptech.abraham.settings.DeviceRole
import androidx.compose.material3.FilterChip


@Composable
fun SetupScreen(
    onConfigured: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var role by remember { mutableStateOf(DeviceRole.MESA) }
    var tableText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    Surface {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                colors = CardDefaults.cardColors()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Configurar dispositivo", style = MaterialTheme.typography.headlineSmall)
                    Text("Escolha se este tablet é uma mesa (com número) ou o balcão.")

                    // Escolha de função
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RoleChip(
                            selected = role == DeviceRole.MESA,
                            label = "Mesa",
                            onClick = { role = DeviceRole.MESA }
                        )
                        RoleChip(
                            selected = role == DeviceRole.BALCAO,
                            label = "Balcão",
                            onClick = { role = DeviceRole.BALCAO }
                        )
                    }

                    // Campo número da mesa (só aparece se for mesa)
                    if (role == DeviceRole.MESA) {
                        OutlinedTextField(
                            value = tableText,
                            onValueChange = { text ->
                                // mantém apenas dígitos
                                tableText = text.filter { it.isDigit() }.take(4)
                            },
                            label = { Text("Número da mesa") },
                            singleLine = true,
                            keyboardOptions = TextKeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            isError = error != null,
                            supportingText = {
                                if (error != null) Text(error!!)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            error = null
                            if (role == DeviceRole.MESA) {
                                val n = tableText.toIntOrNull()
                                if (n == null || n <= 0) {
                                    error = "Informe um número de mesa válido."
                                    return@Button
                                }
                                scope.launch {
                                    saving = true
                                    AppSettings.saveMesa(ctx, n)
                                    saving = false
                                    onConfigured()
                                }
                            } else {
                                scope.launch {
                                    saving = true
                                    AppSettings.saveBalcao(ctx)
                                    saving = false
                                    onConfigured()
                                }
                            }
                        },
                        enabled = !saving && (role == DeviceRole.BALCAO || tableText.isNotBlank()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 8.dp)
                            )
                        }
                        Text("Salvar e continuar")
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}
