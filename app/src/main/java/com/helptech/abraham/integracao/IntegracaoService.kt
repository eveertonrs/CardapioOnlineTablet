package com.helptech.abraham.integracao

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.helptech.abraham.Env
import com.helptech.abraham.data.remote.ApiEnvelope
import com.helptech.abraham.network.Http
import com.helptech.abraham.network.TolonApi
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

data class AuthDeviceResp(
    val sucesso: Boolean = false,
    val mensagem: String? = null,
    val token: String? = null,
    @SerializedName("empresa_db") // Mapeia o campo "empresa_db" do JSON para esta propriedade
    val empresa: String? = null, // Este é o empresa_db, ex: yinkitoledo
    val usuario: String? = null
)

//============ APIs ============

private interface TolonAuthApi {
    @POST("integracao.php")
    suspend fun authDevice(
        // Parâmetro de URL obrigatório para o contexto master
        @Query("empresa") empresaQuery: String,

        // Headers do contexto master
        @Header("empresa") empresaHeader: String,
        @Header("usuario") usuarioHeader: String,
        @Header("token") masterToken: String,
        @Header("modulo") modulo: String,
        @Header("funcao") funcao: String,

        // Body com o serial do device
        @Body body: Map<String, String>
    ): AuthDeviceResp
}

private interface TolonRawApi {
    @POST("integracao.php")
    suspend fun callRaw(
        @Header("modulo") modulo: String,
        @Header("funcao") funcao: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): ResponseBody
}

//============ Serviço ============

object IntegracaoService {

    private val gson = GsonBuilder().setLenient().serializeNulls().create()

    // --- Auth Client & API (usa o client SEM interceptors de sessão) ---

    private fun authBaseUrl(): String {
        val base = (Env.RUNTIME_BASE_URL ?: Env.PANEL_BASE_URL)
        return if (base.endsWith("/")) base else "$base/"
    }

    private fun authApi(): TolonAuthApi {
        return Retrofit.Builder()
            .baseUrl(authBaseUrl())
            .client(Http.authClient) // Usa o cliente SEM interceptors de sessão
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(TolonAuthApi::class.java)
    }

    suspend fun authDevice(serialNumber: String): AuthDeviceResp {
        Log.i("IntegracaoService", "authdevice com serial: $serialNumber e contexto master")

        return authApi().authDevice(
            // Query Param
            empresaQuery = Env.MASTER_EMPRESA,

            // Headers
            empresaHeader = Env.MASTER_EMPRESA,
            usuarioHeader = Env.MASTER_USUARIO,
            masterToken = Env.MASTER_TOKEN,
            modulo = "empresa",
            funcao = "authdevice",

            // Body
            body = mapOf("serialnumber" to serialNumber)
        )
    }

    // --- Demais chamadas (usam o client PADRÃO com interceptors) ---

    private val api: TolonApi by lazy { Http.retrofit.create(TolonApi::class.java) }

    suspend fun consultarConsumo(conta: String, empresaQuery: String? = null): ApiEnvelope {
        val body = mapOf<String, Any?>("conta" to conta)
        return api.call(
            modulo = "empresa",
            funcao = "consultarConsumo",
            empresa = empresaQuery,
            body = body
        )
    }

    private val rawApi: TolonRawApi by lazy { Http.retrofit.create(TolonRawApi::class.java) }

    suspend fun consultarConsumoJson(conta: String): Result<JsonObject> = runCatching {
        val body = mapOf<String, Any?>("conta" to conta)
        val respText = rawApi
            .callRaw(modulo = "empresa", funcao = "consultarConsumo", body = body)
            .string()

        val clean = if (respText.isNotEmpty() && respText[0] == '\uFEFF') {
            respText.substring(1)
        } else {
            respText
        }

        JsonParser.parseString(clean).asJsonObject
    }
}
