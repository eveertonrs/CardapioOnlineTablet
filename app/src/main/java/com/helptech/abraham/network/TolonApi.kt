package com.helptech.abraham.network

import com.google.gson.annotations.SerializedName
import com.helptech.abraham.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*

/** Corpo da chamada de imagem (retornaProdutoAnexo) */
data class RetornaProdutoAnexoBody(
    @SerializedName("produtos_codigo") val produtosCodigo: String,
    @SerializedName("codigo") val codigo: String = "",       // vazio
    @SerializedName("principal") val principal: String = "S",// foto principal
    @SerializedName("tag") val tag: String = "N"             // volta só o path
)

interface TolonApi {
    @POST("integracao.php")
    suspend fun retornaProdutoAnexo(
        @Query("empresa") empresa: String = BuildConfig.API_EMPRESA,
        @Header("token") token: String = BuildConfig.API_TOKEN,
        @Header("empresa") empresaHeader: String = BuildConfig.API_EMPRESA,
        @Header("usuario") usuario: String = BuildConfig.API_USUARIO,
        @Header("funcao") funcao: String = "retornaProdutoAnexo",
        @Header("modulo") modulo: String = "produto",
        @Header("Content-Type") contentType: String = "application/json",
        @Body body: RetornaProdutoAnexoBody
    ): String // pode vir "<img ...>" ou "arquivos/produto/..."
}

/** Singleton do Retrofit */
object TolonApiClient {
    val api: TolonApi by lazy {
        val log = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.BASIC
        }
        val ok = OkHttpClient.Builder()
            .addInterceptor(log)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL) // "https://painel.tolon.com.br/"
            .client(ok)
            .addConverterFactory(ScalarsConverterFactory.create()) // precisa vir antes
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TolonApi::class.java)
    }
}
