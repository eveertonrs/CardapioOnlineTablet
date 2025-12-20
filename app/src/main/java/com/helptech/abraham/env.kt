package com.helptech.abraham

/**
 * Centraliza variáveis de ambiente/config e valores de runtime.
 */
object Env {
    /** Base URL lida do BuildConfig (gradle). */
    val PANEL_BASE_URL: String
        get() = BuildConfig.API_BASE_URL

    /** Empresa padrão (tenant) de fallback — do BuildConfig. */
    val DEFAULT_EMPRESA: String
        get() = BuildConfig.DEFAULT_EMPRESA

    // --- Contexto Master para a chamada inicial authdevice ---
    // Estes valores são usados para obter o token específico do device.
    const val MASTER_EMPRESA: String = "mit"
    const val MASTER_USUARIO: String = "marchioreit"
    const val MASTER_TOKEN: String = "1697425409689f9ccda794d9.81360355"

    /**
     * Útil no emulador: force um serial específico.
     * Deixe null para usar o ANDROID_ID real.
     * ccb98bfb83cf9ddf (Yinki)
     * eveerton (MIT)
     */
    val DEV_FORCE_SERIAL: String? = ""

    // === Preenchidos em runtime após o sucesso do authdevice ===
    @Volatile var RUNTIME_EMPRESA: String = ""
    @Volatile var RUNTIME_USUARIO: String = ""
    @Volatile var RUNTIME_TOKEN:   String = ""

    // === Base URL dinâmica (online x local) preenchida na inicialização ===
    @Volatile var RUNTIME_BASE_URL: String? = null
}
