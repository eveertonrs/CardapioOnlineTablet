package com.helptech.abraham

/**
 * Centraliza variáveis de ambiente/config e valores de runtime
 * (preenchidos após o authdevice).
 */
object Env {
    /** Base URL lida do BuildConfig (gradle). */
    val PANEL_BASE_URL: String
        get() = BuildConfig.API_BASE_URL

    /** Empresa padrão (tenant) de fallback — do BuildConfig. */
    val DEFAULT_EMPRESA: String
        get() = BuildConfig.DEFAULT_EMPRESA

    /**
     * Token de vendor para o endpoint authdevice.
     * Se o seu for outro, troque aqui (ex.: "MIT").
     */
    const val AUTHDEVICE_TOKEN: String = "MIT"

    /**
     * Útil no emulador: force um serial específico.
     * Deixe null para usar o ANDROID_ID real.
     *ccb98bfb83cf9ddf               POSA7DCVAD987AS*/
    val DEV_FORCE_SERIAL: String? = "ccb98bfb83cf9ddf"

    // === Preenchidos em runtime após o authdevice ===
    @Volatile var RUNTIME_EMPRESA: String = ""
    @Volatile var RUNTIME_USUARIO: String = ""
    @Volatile var RUNTIME_TOKEN:   String = ""

    // === NOVO: base URL dinâmica (online x local) preenchida ao iniciar/apply settings ===
    @Volatile var RUNTIME_BASE_URL: String? = null
}
