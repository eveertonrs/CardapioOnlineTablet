package com.helptech.abraham.data.remote

import com.helptech.abraham.BuildConfig
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

interface AkitemApi {
    @POST("integracao.php")
    suspend fun call(
        @Query("empresa") empresa: String,
        @Header("modulo") modulo: String,
        @Header("funcao") funcao: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): ApiEnvelope
}

object AkitemClient {
    private val authHeaders = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .addHeader("empresa", BuildConfig.API_EMPRESA)
            .addHeader("usuario", BuildConfig.API_USUARIO)
            .addHeader("token", BuildConfig.API_TOKEN)
            .addHeader("cache-control", "no-cache")
            .addHeader("Content-Type", "application/json")
            .build()
        chain.proceed(req)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(authHeaders)
        .addInterceptor(logging)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttp)
        .build()

    val api: AkitemApi by lazy { retrofit.create(AkitemApi::class.java) }
}
