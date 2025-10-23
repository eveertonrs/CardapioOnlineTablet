package com.helptech.abraham.integracao

import com.helptech.abraham.Env
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Injeta automaticamente headers exigidos pela integração da Tolon:
 *  - empresa (se não estiver presente)
 *  - usuario (se não estiver presente)
 *  - token   (se não estiver presente)
 *
 * Fontes: Env.RUNTIME_EMPRESA / RUNTIME_USUARIO / RUNTIME_TOKEN (preenchidos após authdevice).
 * Observações:
 * - Não injeta nada quando a função é "authdevice".
 * - Atua em QUALQUER host, desde que o path termine com /integracao.php (suporta IP local).
 */
class AuthHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val url = req.url

        val isIntegracao = url.encodedPath.endsWith("/integracao.php")
        if (!isIntegracao) return chain.proceed(req)

        val isAuthDevice = req.header("funcao")?.equals("authdevice", ignoreCase = true) == true
        if (isAuthDevice) return chain.proceed(req)

        val empresa = Env.RUNTIME_EMPRESA.trim()
        val usuario = Env.RUNTIME_USUARIO.trim()
        val token   = Env.RUNTIME_TOKEN.trim()

        val b = req.newBuilder()
        if (req.header("empresa").isNullOrBlank() && empresa.isNotBlank()) b.header("empresa", empresa)
        if (req.header("usuario").isNullOrBlank() && usuario.isNotBlank()) b.header("usuario", usuario)
        if (req.header("token").isNullOrBlank()   && token.isNotBlank())   b.header("token", token)

        return chain.proceed(b.build())
    }
}
