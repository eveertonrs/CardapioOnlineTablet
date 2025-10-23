package com.helptech.abraham.network

import com.helptech.abraham.Env
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Permite trocar o host/scheme/port em runtime (ex.: online -> IP local),
 * preservando path e query da requisição original.
 *
 * Basta setar Env.RUNTIME_BASE_URL (ex.: "http://192.168.0.10/").
 */
class BaseUrlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val dynamicBase = Env.RUNTIME_BASE_URL

        if (dynamicBase.isNullOrBlank()) {
            return chain.proceed(req)
        }

        val newBase = dynamicBase.let { if (it.endsWith("/")) it else "$it/" }.toHttpUrl()
        val oldUrl = req.url

        val newUrl = oldUrl.newBuilder()
            .scheme(newBase.scheme)
            .host(newBase.host)
            .port(newBase.port)
            .build()

        val newReq = req.newBuilder().url(newUrl).build()
        return chain.proceed(newReq)
    }
}
