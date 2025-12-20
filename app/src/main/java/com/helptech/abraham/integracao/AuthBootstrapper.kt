package com.helptech.abraham.integracao

import android.content.Context
import android.provider.Settings
import com.helptech.abraham.Env
import com.helptech.abraham.settings.AppSettings

object AuthBootstrapper {
    suspend fun ensureAuth(context: Context, serialOverride: String? = Env.DEV_FORCE_SERIAL) {
        val serial: String = serialOverride
            ?.takeIf { it.isNotBlank() }
            ?: Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: error("Não foi possível obter o ANDROID_ID")

        val resp = IntegracaoService.authDevice(serialNumber = serial)

        if (!resp.sucesso) error(resp.mensagem ?: "Falha no authdevice")

        val empresa = resp.empresa?.trim().orEmpty()
        val usuario = resp.usuario?.trim().orEmpty()
        val token   = resp.token?.trim().orEmpty()

        if (empresa.isBlank() || token.isBlank()) {
            error("Authdevice retornou dados incompletos (empresa/token).")
        }

        Env.RUNTIME_EMPRESA = empresa
        Env.RUNTIME_USUARIO = usuario
        Env.RUNTIME_TOKEN   = token

        AppSettings.saveEmpresa(context, empresa)
        AppSettings.saveUsuario(context, usuario)
        AppSettings.saveApiToken(context, token)
        AppSettings.saveDeviceSerial(context, serial)
    }
}
