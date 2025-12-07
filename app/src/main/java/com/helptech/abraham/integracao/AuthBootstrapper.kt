// AuthBootstrapper.kt
package com.helptech.abraham.integracao

import android.content.Context
import android.provider.Settings   // <- add isso
import com.helptech.abraham.Env
import com.helptech.abraham.settings.AppSettings

object AuthBootstrapper {
    suspend fun ensureAuth(context: Context, serialOverride: String? = Env.DEV_FORCE_SERIAL) {
        val serial: String = serialOverride
            ?: Settings.Secure.getString(   // <- sem helper
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            ?: error("Não foi possível obter o ANDROID_ID")

        val resp = IntegracaoService.authDevice(
            serialNumber = serial,
            nome = Env.AUTHDEVICE_TOKEN
        )

        if (!resp.sucesso) error(resp.mensagem ?: "Falha no authdevice")

        Env.RUNTIME_EMPRESA = resp.empresa.orEmpty()
        Env.RUNTIME_USUARIO = resp.usuario.orEmpty()
        Env.RUNTIME_TOKEN   = resp.token.orEmpty()

        AppSettings.saveEmpresa(context, Env.RUNTIME_EMPRESA)
        AppSettings.saveUsuario(context, Env.RUNTIME_USUARIO)
        AppSettings.saveApiToken(context, Env.RUNTIME_TOKEN)
        AppSettings.saveDeviceSerial(context, serial)
    }
}
