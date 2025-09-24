package com.helptech.abraham.data.remote

import com.google.gson.JsonElement
import com.helptech.abraham.BuildConfig

/**
 * Chamada tolerante para “chamar garçom”.
 * Tenta pares (módulo, função) comuns nos painéis Tolon/Akitem.
 * Corpo mínimo: { "mesa": "<rótulo>" } – ajuste aqui se o cliente exigir campos extras.
 */
suspend fun chamarGarcom(
    mesaLabel: String,
    moduloPreferido: String? = null,
    funcaoPreferida: String? = null
): Result<JsonElement?> {

    val moduloCandidates = listOfNotNull(
        moduloPreferido,
        "garcom", "atendimento", "pedido", "mesa"
    ).distinct()

    val funcaoCandidates = listOfNotNull(
        funcaoPreferida,
        "chamarGarcom", "chamar_garcom", "callGarcom", "chamar", "acionarGarcom"
    ).distinct()

    val body = mapOf("mesa" to mesaLabel)

    for (m in moduloCandidates) {
        for (f in funcaoCandidates) {
            val env = AkitemClient.api.call(
                empresa = BuildConfig.API_EMPRESA,
                modulo = m,
                funcao = f,
                body = body
            )
            // sucesso quando não vier "erro"
            if (env.erro == null) return Result.success(env.sucesso)
        }
    }

    return Result.failure(
        IllegalStateException("Nenhuma função encontrada para chamar garçom (ajuste modulo/funcao)")
    )
}
