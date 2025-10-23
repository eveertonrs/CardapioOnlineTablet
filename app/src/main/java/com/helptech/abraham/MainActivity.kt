package com.helptech.abraham

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.helptech.abraham.integracao.AuthBootstrapper
import com.helptech.abraham.settings.AppSettings
import com.helptech.abraham.ui.SetupScreen
import com.helptech.abraham.ui.theme.ApiPlayground
import kotlinx.coroutines.launch

// === MOVIDO PARA TOPO: estados de auth ===
sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data object Success : AuthState
    data class Error(val message: String) : AuthState
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ctx = LocalContext.current
            val scope = rememberCoroutineScope()

            // 0) aplica base URL dinâmica salva (online/local)
            LaunchedEffect(Unit) {
                val base = AppSettings.getBaseUrlOnce(ctx)
                Env.RUNTIME_BASE_URL = base
            }

            // 1) checa configuração inicial
            var isConfigured by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(Unit) {
                isConfigured = AppSettings.isConfigured(ctx)
            }

            // 2) estado do auth
            var authState by remember { mutableStateOf<AuthState>(AuthState.Idle) }

            // 3) quando configurado, faz o auth uma vez
            LaunchedEffect(isConfigured) {
                if (isConfigured == true) {
                    authState = AuthState.Loading
                    runCatching {
                        AuthBootstrapper.ensureAuth(context = ctx.applicationContext)
                    }.onSuccess {
                        authState = AuthState.Success
                    }.onFailure { t ->
                        authState = AuthState.Error(t.message ?: "Falha ao autenticar o dispositivo")
                    }
                }
            }

            // 4) UI
            when (isConfigured) {
                null -> LoadingBox()
                false -> SetupScreen(onConfigured = { isConfigured = true })
                true -> when (val s = authState) {
                    AuthState.Idle, AuthState.Loading -> LoadingBox()
                    is AuthState.Error -> ErrorBox(
                        message = s.message,
                        onRetry = {
                            authState = AuthState.Loading
                            scope.launch {
                                runCatching {
                                    AuthBootstrapper.ensureAuth(context = ctx.applicationContext)
                                }.onSuccess { authState = AuthState.Success }
                                    .onFailure { e -> authState = AuthState.Error(e.message ?: "Erro ao autenticar") }
                            }
                        }
                    )
                    AuthState.Success -> ApiPlayground()
                }
            }
        }
    }
}

/* Helpers simples de UI */
@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Tentar novamente") }
        }
    }
}
