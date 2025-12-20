package com.helptech.abraham

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.helptech.abraham.integracao.AuthBootstrapper
import com.helptech.abraham.settings.AppSettings
import com.helptech.abraham.ui.SetupScreen
import com.helptech.abraham.ui.theme.ApiPlayground
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Status do auth (saveable sem Parcelize)
    // 0 = Idle | 1 = Loading | 2 = Success | 3 = Error
    private companion object {
        const val AUTH_IDLE = 0
        const val AUTH_LOADING = 1
        const val AUTH_SUCCESS = 2
        const val AUTH_ERROR = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val ctx = LocalContext.current
            val scope = rememberCoroutineScope()

            // Aplica base URL dinâmica salva (online/local)
            LaunchedEffect(Unit) {
                val base = AppSettings.getBaseUrlOnce(ctx)
                Env.RUNTIME_BASE_URL = base
            }

            // Checa configuração inicial
            var isConfigured by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(Unit) {
                isConfigured = AppSettings.isConfigured(ctx)
            }

            // Estado do auth (salvo entre recriações, sem Parcelable)
            var authStatus by rememberSaveable { mutableIntStateOf(AUTH_IDLE) }
            var authError by rememberSaveable { mutableStateOf<String?>(null) }

            // Quando configurado, faz o auth uma única vez
            LaunchedEffect(isConfigured) {
                if (isConfigured == true && authStatus != AUTH_SUCCESS) {
                    authStatus = AUTH_LOADING
                    authError = null

                    runCatching {
                        AuthBootstrapper.ensureAuth(context = ctx.applicationContext)
                    }.onSuccess {
                        authStatus = AUTH_SUCCESS
                    }.onFailure { t ->
                        authStatus = AUTH_ERROR
                        authError = t.message ?: "Falha ao autenticar o dispositivo"
                    }
                }
            }

            // UI
            when (isConfigured) {
                null -> LoadingBox()

                false -> SetupScreen(
                    onConfigured = {
                        // A tela de setup deve salvar a config no AppSettings.
                        // Aqui a gente só sinaliza para iniciar o fluxo de auth.
                        isConfigured = true
                    }
                )

                true -> when (authStatus) {
                    AUTH_IDLE, AUTH_LOADING -> LoadingBox()

                    AUTH_ERROR -> ErrorBox(
                        message = authError ?: "Erro ao autenticar",
                        onRetry = {
                            authStatus = AUTH_LOADING
                            authError = null

                            scope.launch {
                                runCatching {
                                    AuthBootstrapper.ensureAuth(context = ctx.applicationContext)
                                }.onSuccess {
                                    authStatus = AUTH_SUCCESS
                                }.onFailure { e ->
                                    authStatus = AUTH_ERROR
                                    authError = e.message ?: "Erro ao autenticar"
                                }
                            }
                        }
                    )

                    AUTH_SUCCESS -> ApiPlayground()

                    else -> LoadingBox()
                }
            }
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Tentar novamente") }
        }
    }
}
