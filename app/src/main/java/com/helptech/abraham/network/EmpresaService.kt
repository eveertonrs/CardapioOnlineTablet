package com.helptech.abraham.network

import com.helptech.abraham.data.remote.ApiEnvelope

/**
 * Ações do módulo "empresa" pedidas no cURL:
 * - chamarAtendimento { "mesa": "32" }
 * - consultarConsumo { "conta": "32" }
 *
 * Reusa o TolonApi.call que já injeta headers e ?empresa= via interceptors do projeto.
 */
object EmpresaService {
    private val api = Http.retrofit.create(TolonApi::class.java)

    suspend fun chamarAtendimento(mesa: String): ApiEnvelope =
        api.call(
            modulo = "empresa",
            funcao = "chamarAtendimento",
            body = mapOf("mesa" to mesa)
        )

    suspend fun consultarConsumo(conta: String): ApiEnvelope =
        api.call(
            modulo = "empresa",
            funcao = "consultarConsumo",
            body = mapOf("conta" to conta)
        )
}
