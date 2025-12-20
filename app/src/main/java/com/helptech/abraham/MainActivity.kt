package com.helptech.abraham

import android.app.Activity
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

            var isConfigured by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(Unit) {
                isConfigured = AppSettings.isConfigured(ctx)
            }

            var authStatus by rememberSaveable { mutableIntStateOf(AUTH_IDLE) }
            var authError by rememberSaveable { mutableStateOf<String?>(null) }

            val startAuth: () -> Unit = {
                authStatus = AUTH_LOADING
                authError = null
                scope.launch {
                    runCatching {
                        AuthBootstrapper.ensureAuth(context = ctx.applicationContext)
                    }.onSuccess {
                        authStatus = AUTH_SUCCESS
                        if (isConfigured == false) {
                            isConfigured = true
                        }
                    }.onFailure { t ->
                        authStatus = AUTH_ERROR
                        authError = t.message ?: "Falha ao autenticar o dispositivo"
                        // Se a autenticação falhar, o app não está mais configurado.
                        if (isConfigured == true) {
                            scope.launch { AppSettings.clearAuth(ctx) }
                            isConfigured = false
                        }
                    }
                }
            }

            LaunchedEffect(isConfigured) {
                if (isConfigured == true && authStatus != AUTH_SUCCESS) {
                    startAuth()
                }
            }

            when (isConfigured) {
                null -> LoadingBox("Verificando configuração...")

                false -> {
                    val activity = (LocalContext.current as? Activity)
                    SetupScreen(
                        errorMessage = authError,
                        isLoading = authStatus == AUTH_LOADING,
                        onAuthenticate = { startAuth() },
                        onCloseApp = { activity?.finish() }
                    )
                }

                true -> {
                    // Se o app está configurado, mas o auth não deu certo, exibe o Loading.
                    // A lógica em `onFailure` irá reverter para `isConfigured = false` e mostrar a SetupScreen.
                    if (authStatus == AUTH_SUCCESS) {
                        ApiPlayground()
                    } else {
                        LoadingBox("Autenticando...")
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingBox(message: String = "") {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            if (message.isNotBlank()) {
                Text(message, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

// Esta tela não é mais usada no fluxo principal de falha de autenticação,
// mas pode ser útil para outros tipos de erro no futuro.
@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = message)
            Button(onClick = onRetry) { Text("Tentar novamente") }
        }
    }
}
