package com.helptech.abraham.network

import com.helptech.abraham.BuildConfig
import com.helptech.abraham.integracao.AuthHeadersInterceptor
import com.helptech.abraham.integracao.EmpresaParamInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import com.google.gson.GsonBuilder


private class BomStrippingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body ?: return response
        val asString = try { body.string() } catch (_: Throwable) { return response }
        val cleaned = if (asString.startsWith('\uFEFF')) asString.substring(1) else asString
        val newBody = cleaned.toResponseBody(body.contentType())
        return response.newBuilder().body(newBody).build()
    }
}

object Http {

    private const val INSECURE_SSL_FOR_PILOT = true

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        redactHeader("token")
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.BASIC
    }

    private fun createUnsafeClient(builder: OkHttpClient.Builder): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
        )

        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        return builder
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .build()
    }

    // --- Client para AUTHDEVICE (sem interceptors de sessão) ---
    private fun baseAuthClientBuilder(): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .build()
                )
            }
            .addInterceptor(BaseUrlInterceptor())
            .addInterceptor(loggingInterceptor)
    }

    val authClient: OkHttpClient by lazy {
        val builder = baseAuthClientBuilder()
        if (INSECURE_SSL_FOR_PILOT) {
            createUnsafeClient(builder)
        } else {
            builder.build()
        }
    }

    // --- Client PADRÃO (com interceptors de sessão) ---
    private fun baseClientBuilder(): OkHttpClient.Builder {
        return baseAuthClientBuilder() // Começa com o builder de auth
            .addInterceptor(AuthHeadersInterceptor()) // Adiciona headers de sessão
            .addInterceptor(EmpresaParamInterceptor()) // Adiciona ?empresa= a todas as chamadas
            .addInterceptor(BomStrippingInterceptor())
            .dispatcher(okhttp3.Dispatcher().apply {
                maxRequests = 32
                maxRequestsPerHost = 8
            })
    }

    val client: OkHttpClient by lazy {
        val builder = baseClientBuilder()
        if (INSECURE_SSL_FOR_PILOT) {
            createUnsafeClient(builder)
        } else {
            builder.build()
        }
    }

    val retrofit: Retrofit by lazy {
        val base = BuildConfig.API_BASE_URL.let { if (it.endsWith('/')) it else "$it/" }
        Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().serializeNulls().create()))
            .build()
    }
}
