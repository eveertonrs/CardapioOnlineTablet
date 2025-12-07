package com.helptech.abraham.integracao

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.helptech.abraham.Env
import com.helptech.abraham.BuildConfig
import com.helptech.abraham.network.Http
import com.helptech.abraham.network.TolonApi
import com.helptech.abraham.data.remote.ApiEnvelope
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class AuthDeviceResp(
    val sucesso: Boolean = false,
    val mensagem: String? = null,
    val token: String? = null,
    val empresa: String? = null,
    val usuario: String? = null
)

/* ========= APIs ========= */

private interface TolonAuthApi {
    @POST("integracao.php")
    suspend fun authDevice(
        @Header("funcao") funcao: String = "authdevice",
        @Header("token") vendorToken: String,          // ex.: "MIT"
        // IMPORTANTE: não mandar "empresa" aqui
        @Body body: Map<String, String>
    ): AuthDeviceResp
}

/**
 * API “raw” para pegar o corpo inteiro de endpoints que **não** usam
 * o envelope padrão (como empresa/consultarConsumo).
 * Os cabeçalhos de empresa/usuario/token são inseridos pelo interceptor do Http.retrofit.
 */
private interface TolonRawApi {
    @POST("integracao.php")
    suspend fun callRaw(
        @Header("modulo") modulo: String,
        @Header("funcao") funcao: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): ResponseBody
}

/* ========= Serviço ========= */

object IntegracaoService {

    // ---------- GSON / logging ----------

    private val gson = GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create()

    private val logging = HttpLoggingInterceptor().apply {
        redactHeader("token")
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * Cria o OkHttpClient para o authdevice.
     * Em DEBUG, usamos um client “unsafe” para contornar problemas de certificado
     * no painel; em RELEASE, usamos o client padrão (seguro).
     */
    private fun buildAuthClient(): OkHttpClient {
        val baseBuilder = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(req)
            })
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        return if (BuildConfig.DEBUG) {
            // ⚠️ SOMENTE PARA DESENVOLVIMENTO
            createUnsafeClient(baseBuilder)
        } else {
            baseBuilder.build()
        }
    }

    /**
     * Ajusta o builder para aceitar qualquer certificado / hostname.
     * NÃO usar em produção – apenas para ambiente de desenvolvimento.
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

    // Client efetivo usado no Retrofit do auth
    private val authClient: OkHttpClient = buildAuthClient()

    // base dinâmica para auth: usa RUNTIME_BASE_URL se setada; senão base do build
    private fun authBaseUrl(): String {
        val base = (Env.RUNTIME_BASE_URL ?: Env.PANEL_BASE_URL)
        return if (base.endsWith("/")) base else "$base/"
    }

    private val authRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(authBaseUrl())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(authClient)
            .build()
    }

    private val authApi by lazy { authRetrofit.create(TolonAuthApi::class.java) }

    suspend fun authDevice(
        serialNumber: String,
        nome: String
    ): AuthDeviceResp {
        Log.i("IntegracaoService", "authdevice SN=$serialNumber, nome=$nome")

        val body = mapOf(
            "serialnumber" to serialNumber,
            "Nome" to nome
        )

        return authApi.authDevice(
            vendorToken = Env.AUTHDEVICE_TOKEN,
            body = body
        )
    }

    // ---------- Demais chamadas ----------

    private val api: TolonApi by lazy { Http.retrofit.create(TolonApi::class.java) }

    // Envelope padrão (mantido para quem já usa)
    suspend fun consultarConsumo(conta: String, empresaQuery: String? = null): ApiEnvelope {
        val body = mapOf<String, Any?>(
            "conta" to conta
        )
        return api.call(
            modulo = "empresa",
            funcao = "consultarConsumo",
            empresa = empresaQuery, // pode ser null se já vier do interceptor
            body = body
        )
    }

    // ---------- Variante RAW que devolve o JSON completo ----------

    private val rawApi: TolonRawApi by lazy { Http.retrofit.create(TolonRawApi::class.java) }

    /**
     * Chamada recomendada para “Minha conta”.
     * Retorna o objeto completo do servidor (com campos: sucesso, Conta, Cliente, Itens, ...).
     * Faz limpeza de BOM e parse manual para maior robustez.
     */
    suspend fun consultarConsumoJson(conta: String): Result<JsonObject> = runCatching {
        val body = mapOf<String, Any?>("conta" to conta)
        val respText = rawApi
            .callRaw(modulo = "empresa", funcao = "consultarConsumo", body = body)
            .string()

        // Remove BOM, se houver, e parseia
        val clean = if (respText.isNotEmpty() && respText[0] == '\uFEFF') {
            respText.substring(1)
        } else {
            respText
        }
        JsonParser.parseString(clean).asJsonObject
    }
}
