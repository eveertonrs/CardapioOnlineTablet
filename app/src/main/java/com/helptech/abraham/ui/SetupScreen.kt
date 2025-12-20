package com.helptech.abraham.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.helptech.abraham.Env
import com.helptech.abraham.integracao.getAndroidId

@Composable
fun SetupScreen(
    errorMessage: String?,
    isLoading: Boolean,
    onAuthenticate: () -> Unit,
    onCloseApp: () -> Unit
) {
    val ctx = LocalContext.current
    var serial by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        serial = Env.DEV_FORCE_SERIAL?.takeIf { it.isNotBlank() } ?: getAndroidId(ctx)
    }

    Surface(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Botão de fechar no canto superior direito
            IconButton(
                onClick = onCloseApp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Fechar Aplicativo", modifier = Modifier.size(32.dp), tint = Color.Gray)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Configurar Dispositivo",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    "Este é o ID único do seu dispositivo. Ele será usado para autenticação.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = TextFieldValue(serial),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Serial do Dispositivo (ID)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Button(
                    onClick = onAuthenticate,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (errorMessage != null) "TENTAR NOVAMENTE" else "SALVAR E AUTENTICAR")
                    }
                }
            }
        }
    }
}
