package com.helptech.abraham.integracao

import com.helptech.abraham.BuildConfig
import com.helptech.abraham.Env
import okhttp3.Interceptor
import okhttp3.Response

class EmpresaParamInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val url = req.url

        val isIntegracao = url.encodedPath.endsWith("/integracao.php") ||
                url.encodedPath.endsWith("integracao.php")
        if (!isIntegracao) return chain.proceed(req)

        // Se for authdevice, não injeta empresa
        val isAuthDevice = req.header("funcao")?.equals("authdevice", ignoreCase = true) == true
        if (isAuthDevice) return chain.proceed(req)

        // 1) valor obtido após o authdevice
        val empresaRuntime = Env.RUNTIME_EMPRESA.trim()
        // 2) valor de build
        val empresaBuild   = (BuildConfig.API_EMPRESA ?: "").trim()
        // 3) fallback default
        val empresaDefault = (BuildConfig.DEFAULT_EMPRESA ?: "").trim().ifEmpty { "mit" }

        val empresa = listOf(empresaRuntime, empresaBuild, empresaDefault)
            .firstOrNull { it.isNotEmpty() } ?: "mit"

        val newUrl = url.newBuilder()
            .removeAllQueryParameters("empresa")
            .addQueryParameter("empresa", empresa)
            .build()

        val newReq = req.newBuilder()
            .url(newUrl)
            .apply {
                if (req.header("empresa").isNullOrBlank()) header("empresa", empresa)
                if (req.header("Accept") == null) header("Accept", "application/json")
                if (req.header("Content-Type") == null) header("Content-Type", "application/json")
                if (req.header("Cache-Control") == null) header("Cache-Control", "no-cache")
            }
            .build()

        return chain.proceed(newReq)
    }
}
