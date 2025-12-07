package com.helptech.abraham.network

import com.helptech.abraham.BuildConfig
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

/**
 * Remove BOM (\uFEFF) de respostas JSON/texto.
 */
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

    // ⚠️ FLAG PROVISÓRIA: deixar true para o PILOTO.
    // Quando o certificado estiver OK, volte para false ou remova o unsafe.
    private const val INSECURE_SSL_FOR_PILOT = true

    /**
     * Cria um OkHttpClient "normal" com todos os interceptors do app.
     */
    private fun baseClientBuilder(): OkHttpClient.Builder {
        val log = HttpLoggingInterceptor().apply {
            redactHeader("token")
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            // headers mínimos
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .header("Cache-Control", "no-cache")
                        .build()
                )
            }
            // base dinâmica (online/local)
            .addInterceptor(BaseUrlInterceptor())
            // headers de autenticação pós-authdevice
            .addInterceptor(com.helptech.abraham.integracao.AuthHeadersInterceptor())
            // garante ?empresa= consistente com o runtime
            .addInterceptor(EmpresaParamInterceptor())
            // remove BOM
            .addInterceptor(BomStrippingInterceptor())
            .addInterceptor(log)
            // controla rajada de requests
            .dispatcher(okhttp3.Dispatcher().apply {
                maxRequests = 32
                maxRequestsPerHost = 8
            })
    }

    /**
     * Desativa validação de certificado/hostname.
     * Use isso só enquanto o servidor não tiver certificado ok.
     */
    private fun createUnsafeClient(builder: OkHttpClient.Builder): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<X509Certificate>,
                    authType: String
                ) {
                    // no-op
                }

                override fun checkServerTrusted(
                    chain: Array<X509Certificate>,
                    authType: String
                ) {
                    // no-op
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
        )

        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        val sslSocketFactory = sslContext.socketFactory
        val trustManager = trustAllCerts[0] as X509TrustManager

        return builder
            .sslSocketFactory(sslSocketFactory, trustManager)
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .build()
    }

    val client: OkHttpClient by lazy {
        val builder = baseClientBuilder()
        if (INSECURE_SSL_FOR_PILOT) {
            // TODO: remover assim que o certificado do painel estiver OK
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
            // Scalars antes de Gson
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
