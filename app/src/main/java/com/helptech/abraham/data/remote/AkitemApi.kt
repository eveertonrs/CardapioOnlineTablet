package com.helptech.abraham.data.remote

import android.util.Log
import com.helptech.abraham.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Interface da integração Tolon/Akitem.
 * - empresa vai na query (?empresa=xxx)
 * - modulo/funcao em headers
 * - body em JSON
 */
interface AkitemApi {
    @POST("integracao.php")
    suspend fun call(
        @Query("empresa") empresa: String,
        @Header("modulo") modulo: String,
        @Header("funcao") funcao: String,
        @Body body: Any
    ): ApiEnvelope
}

object AkitemClient {

    // Gson tolerante (útil por causa de respostas com BOM e tipos inconsistentes)
    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .setLenient()
        .create()

    // Headers fixos de autenticação em TODA chamada
    private val authHeaders = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .addHeader("empresa", BuildConfig.API_EMPRESA)
            .addHeader("usuario", BuildConfig.API_USUARIO)
            .addHeader("token", BuildConfig.API_TOKEN)
            .addHeader("cache-control", "no-cache")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()
        chain.proceed(req)
    }

    // Logging (sem vazar token)
    private val logging = HttpLoggingInterceptor().apply {
        redactHeader("token")
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(authHeaders)
        .addInterceptor(logging)
        .retryOnConnectionFailure(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Usa a base do BuildConfig (ex.: https://painel.tolon.com.br/)
    private val baseUrl: String by lazy {
        val raw = BuildConfig.API_BASE_URL
        val normalized = if (raw.endsWith("/")) raw else "$raw/"
        Log.i("AkitemClient", "BASE_URL=$normalized")
        normalized
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okHttp)
        .build()

    val api: AkitemApi by lazy { retrofit.create(AkitemApi::class.java) }

    /** Atalho para usar BuildConfig.API_EMPRESA automaticamente. */
    suspend fun call(
        modulo: String,
        funcao: String,
        body: Any
    ): ApiEnvelope {
        return api.call(
            empresa = BuildConfig.API_EMPRESA,
            modulo = modulo,
            funcao = funcao,
            body = body
        )
    }

    /* ---------------------------------------------------------------------
       Helper opcional para criar/enviar pedido (MESA).
       Não substitui seu fluxo atual; é só um atalho se quiser usar.
       Tenta combinações comuns de modulo/funcao diferentes instalações.
       Body inclui cliente + tipo_entrega + produtos no padrão mais aceito.
       --------------------------------------------------------------------- */
    suspend fun enviarPedidoMesa(
        mesaLabel: String,
        itens: List<ItemPedidoReq>
    ): ApiEnvelope {
        // 1) formata itens no padrão "produtos" aceito pelo backend
        val produtosPayload = itens.map {
            mapOf(
                "produtos_codigo" to it.codigoProduto,
                "qtde" to it.quantidade,
                "valor" to it.valorUnit
            )
        }

        // 2) base do body (cliente + entrega MESA)
        val base = mapOf(
            "cliente" to mapOf("nome" to "Mesa $mesaLabel"),
            "tipo_entrega_tipo" to "MESA",
            "tipo_entrega_adicional" to mesaLabel,
            "local_entrega" to mesaLabel
        )

        // 3) variantes de body (algumas instalações aceitam "itens" original)
        val bodies = listOf(
            base + mapOf("produtos" to produtosPayload),
            base + mapOf("itens" to itens) // fallback
        )

        // 4) combinações mais prováveis de módulo/função
        val moduloCandidates = listOf("pedido", "atendimento")
        val funcaoCandidates = listOf("gravar", "cadastrar", "inserir", "gravarPedido", "cadastrarPedido")

        var lastError: String? = null

        for (body in bodies) {
            for (m in moduloCandidates) {
                for (f in funcaoCandidates) {
                    val env = call(modulo = m, funcao = f, body = body)
                    if (env.erro == null) return env
                    lastError = env.erro
                }
            }
        }

        return ApiEnvelope(
            sucesso = null,
            erro = lastError ?: "Nenhuma função localizada para gravar pedido"
        )
    }
}
