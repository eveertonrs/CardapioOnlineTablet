package com.helptech.abraham.data.remote

import com.helptech.abraham.BuildConfig
import com.helptech.abraham.Env

/**
 * Camada de compatibilidade p/ código antigo.
 * Agora lê dos BuildConfig + valores em runtime do Env.
 */
object LegacyConfig {

    val BASE_URL: String
        get() = BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }

    val EMPRESA: String
        get() = Env.RUNTIME_EMPRESA.ifBlank {
            BuildConfig.API_EMPRESA.ifBlank { BuildConfig.DEFAULT_EMPRESA }
        }.lowercase()

    val USUARIO: String
        get() = Env.RUNTIME_USUARIO.ifBlank { BuildConfig.API_USUARIO }

    val TOKEN: String
        get() = Env.RUNTIME_TOKEN.ifBlank { BuildConfig.API_TOKEN }
}
