package com.helptech.abraham.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.helptech.abraham.Env
import com.helptech.abraham.integracao.getAndroidId
import com.helptech.abraham.settings.AppSettings
import com.helptech.abraham.settings.DeviceRole
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    onConfigured: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Serial mostrado na tela
    var serial by remember { mutableStateOf("") }

    // Carrega o serial ao abrir a tela
    LaunchedEffect(Unit) {
        val forced = Env.DEV_FORCE_SERIAL
        serial = if (!forced.isNullOrBlank()) {
            forced
        } else {
            getAndroidId(ctx)
        }
    }

    var loading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    fun saveConfiguration() {
        val serialParaSalvar = serial.ifBlank {
            msg = "Não foi possível obter o serial do dispositivo."
            return
        }

        loading = true
        msg = null

        scope.launch {
            try {
                // Persiste no DataStore
                AppSettings.saveDeviceSerial(ctx, serialParaSalvar)
                // Papel padrão
                AppSettings.setRole(ctx, DeviceRole.BALCAO)

                // Sinaliza que a configuração foi concluída
                onConfigured()
            } catch (t: Throwable) {
                msg = "Falha ao salvar a configuração: ${t.message}"
            } finally {
                loading = false
            }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Configurar dispositivo",
                    style = MaterialTheme.typography.titleLarge
                )

                // Campo somente leitura com o serial
                OutlinedTextField(
                    value = TextFieldValue(serial),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Serial do Dispositivo (ID)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                )

                if (msg != null) {
                    Text(
                        text = msg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = { saveConfiguration() },
                    enabled = !loading
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Salvar e Continuar")
                }
            }
        }
    }
}
