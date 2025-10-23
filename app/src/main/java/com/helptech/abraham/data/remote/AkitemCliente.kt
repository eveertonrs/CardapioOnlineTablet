package com.helptech.abraham.data.remote

import com.helptech.abraham.network.Http
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AkitemApi {
    @POST("integracao.php")
    suspend fun call(
        @Header("empresa") empresa: String?,
        @Header("modulo")  modulo: String,
        @Header("funcao")  funcao: String,
        @Body body: Any?
    ): ApiEnvelope
}

object AkitemClient {
    val api: AkitemApi by lazy { Http.retrofit.create(AkitemApi::class.java) }
}
