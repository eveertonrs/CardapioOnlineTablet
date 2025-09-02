package com.helptech.abraham

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.helptech.abraham.settings.AppSettings
import com.helptech.abraham.ui.theme.ApiPlayground
import com.helptech.abraham.ui.setup.SetupScreen
// Se for passar o label depois, descomente estes imports:
// import com.helptech.abraham.settings.DeviceRole
// import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val ctx = LocalContext.current

            // verifica se o dispositivo já foi configurado (mesa/balcão)
            var isConfigured by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(Unit) {
                isConfigured = AppSettings.isConfigured(ctx)
            }

            when (isConfigured) {
                null -> { // loading inicial
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                false -> {
                    // primeiro uso -> mostra a tela de configuração
                    SetupScreen(onConfigured = { isConfigured = true })
                }
                true -> {
                    // já configurado -> abre seu app normal
                    // (se quiser mostrar o rótulo dinâmico da mesa aqui,
                    //  a gente passa no próximo passo para o ApiPlayground/MenuScreen)
                    ApiPlayground()

                    // Exemplo (para depois): obter o label dinâmico
                    // val role by AppSettings.roleFlow(ctx).collectAsState(initial = DeviceRole.MESA)
                    // val mesa by AppSettings.tableFlow(ctx).collectAsState(initial = null)
                    // val mesaLabel = if (role == DeviceRole.BALCAO) "BALCÃO" else "MESA ${mesa ?: "-"}"
                    // ApiPlayground(mesaLabel = mesaLabel)
                }
            }
        }
    }
}
