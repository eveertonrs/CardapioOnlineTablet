package com.helptech.abraham.network

import com.helptech.abraham.data.remote.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Endpoint genérico: POST integracao.php
 * - Headers obrigatórios: modulo, funcao
 * - Query opcional: empresa (também pode vir pelo interceptor)
 * - Body: JSON livre
 */
interface TolonApi {

    @POST("integracao.php")
    suspend fun call(
        @Header("modulo") modulo: String,
        @Header("funcao") funcao: String,
        @Query("empresa") empresa: String? = null,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): ApiEnvelope
}
