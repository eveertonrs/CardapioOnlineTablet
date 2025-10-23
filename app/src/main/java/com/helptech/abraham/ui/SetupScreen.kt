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
import com.helptech.abraham.integracao.IntegracaoService
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

    var empresa by remember { mutableStateOf(TextFieldValue("")) }
    var nomeDisp by remember { mutableStateOf(TextFieldValue("POS")) }

    // Usa serial fixo no dev; se null, pega o ANDROID_ID do dispositivo
    val serialParaAuth = remember { Env.DEV_FORCE_SERIAL ?: getAndroidId(ctx) }

    var loading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    fun autenticar() {
        loading = true
        msg = null
        scope.launch {
            try {
                val resp = IntegracaoService.authDevice(
                    serialNumber = serialParaAuth,
                    nome = nomeDisp.text.ifBlank { "POS" }
                )

                if (!resp.sucesso) {
                    msg = resp.mensagem ?: "Equipamento não habilitado."
                    loading = false
                    return@launch
                }

                // Empresa final: prioriza servidor; senão o digitado; senão default
                val empresaFinal = (resp.empresa ?: empresa.text.ifBlank { Env.DEFAULT_EMPRESA }).lowercase()

                // Persiste no DataStore
                AppSettings.saveEmpresa(ctx, empresaFinal)
                resp.token?.let { AppSettings.saveApiToken(ctx, it) }

                // Preenche o ambiente em runtime
                Env.RUNTIME_EMPRESA = empresaFinal
                Env.RUNTIME_USUARIO = resp.usuario ?: ""
                Env.RUNTIME_TOKEN   = resp.token ?: ""

                // Papel padrão: BALCÃO
                AppSettings.setRole(ctx, DeviceRole.BALCAO)

                onConfigured()
            } catch (t: Throwable) {
                msg = "Falha ao autenticar: ${t.message}"
            } finally {
                loading = false
            }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Configurar dispositivo", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = empresa,
                    onValueChange = { empresa = it },
                    label = { Text("Empresa (tenant)") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = nomeDisp,
                    onValueChange = { nomeDisp = it },
                    label = { Text("Nome do dispositivo") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = TextFieldValue(serialParaAuth),
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Serial (ANDROID_ID)") },
                    singleLine = true
                )

                if (msg != null) {
                    Text(
                        text = msg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = { autenticar() },
                    enabled = !loading
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Autenticar")
                }
            }
        }
    }
}
