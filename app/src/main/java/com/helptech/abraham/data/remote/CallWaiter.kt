package com.helptech.abraham.data.remote

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.helptech.abraham.Env
import com.helptech.abraham.data.remote.AkitemClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


/**
 * Chamada tolerante para “chamar garçom”.
 * Tenta pares (módulo, função) comuns nos painéis Tolon/Akitem.
 * Corpo mínimo: { "mesa": "<rótulo>" }.
 */
suspend fun chamarGarcom(
    mesaLabel: String,
    moduloPreferido: String? = null,
    funcaoPreferida: String? = null
): Result<JsonElement?> {

    // Primeiro tenta o par confirmado pelo cliente
    val moduloCandidates = listOfNotNull(
        moduloPreferido,
        "empresa", "garcom", "atendimento", "pedido", "mesa"
    ).distinct()

    val funcaoCandidates = listOfNotNull(
        funcaoPreferida,
        "chamarAtendimento", "chamarGarcom", "chamar_garcom", "callGarcom", "chamar", "acionarGarcom"
    ).distinct()

    val body = mapOf("mesa" to mesaLabel)

    for (m in moduloCandidates) {
        for (f in funcaoCandidates) {
            val env = AkitemClient.api.call(
                empresa = null, // interceptors injetam (empresa/usuario/token) do runtime
                modulo = m,
                funcao = f,
                body = body
            )
            if (env.erro == null) return Result.success(env.sucesso)
        }
    }

    return Result.failure(
        IllegalStateException("Nenhuma função encontrada para chamar garçom (ajuste modulo/funcao)")
    )
}

/**
 * Consulta o consumo/conta com o *cliente genérico* (envelope).
 * ⚠️ Em alguns backends "sucesso" vem como "true" (String) e não como objeto/array.
 * Para renderizar a conta/itens, prefira usar [consultarConsumoJson] abaixo,
 * que retorna o **JSON bruto completo** da resposta.
 */
suspend fun consultarConsumo(
    contaOuMesa: String,
    moduloPreferido: String? = null,
    funcaoPreferida: String? = null
): Result<JsonElement?> {

    val moduloCandidates = listOfNotNull(
        moduloPreferido,
        "empresa", "mesa", "pedido", "conta", "consumo"
    ).distinct()

    val funcaoCandidates = listOfNotNull(
        funcaoPreferida,
        "consultarConsumo", "consultar_consumo",
        "consultarConta", "consultar_conta",
        "consultaConsumo"
    ).distinct()

    val body = mapOf(
        "conta" to contaOuMesa,
        "mesa" to contaOuMesa
    )

    for (m in moduloCandidates) {
        for (f in funcaoCandidates) {
            val env = AkitemClient.api.call(
                empresa = null, // interceptors injetam (empresa/usuario/token) do runtime
                modulo = m,
                funcao = f,
                body = body
            )
            if (env.erro == null) {
                // Pode ser "true" (String) e não um objeto. Mantenho por compat.,
                // mas para a UI use consultarConsumoJson().
                return Result.success(env.sucesso)
            }
        }
    }

    return Result.failure(
        IllegalStateException("Nenhuma função encontrada para consultar consumo (ajuste modulo/funcao)")
    )
}

/* ============================================================
   VERSÃO RECOMENDADA PARA A UI
   ------------------------------------------------------------
   Faz a chamada HTTP direta e retorna o JSON **completo**:
   { "sucesso": "...", "Conta": "...", "Cliente": {...}, "Itens":[...] }
   Remove o BOM invisível (observado no log: 'ï»¿{').
   Respeita base dinâmica (online x IP local) e credenciais do runtime.
   ============================================================ */

private val rawHttpClient by lazy { OkHttpClient() }

/**
 * Consulta de consumo que captura o **JSON bruto completo** da API Tolon,
 * sem passar pelo "envelope" que pode reduzir a resposta a "sucesso":"true".
 */
/**suspend fun consultarConsumoJson(contaOuMesa: String): Result<JsonObject> =
    withContext(Dispatchers.IO) {
        runCatching {

            val base = (Env.RUNTIME_BASE_URL ?: LegacyConfig.BASE_URL)
                .let { if (it.endsWith("/")) it else "$it/" }

            val emp = Env.RUNTIME_EMPRESA
            val usr = Env.RUNTIME_USUARIO
            val tok = Env.RUNTIME_TOKEN

            if (emp.isBlank() || tok.isBlank()) {
                error("Auth não carregado (empresa/token vazios).")
            }

            val url = base + "integracao.php?empresa=$emp"

            val payload = """{"conta":"$contaOuMesa"}"""
                .toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url(url)
                .post(payload)
                .addHeader("modulo", "empresa")
                .addHeader("funcao", "consultarConsumo")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("token", tok)
                .addHeader("usuario", usr)
                .addHeader("empresa", emp)
                .build()

            rawHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val raw = resp.body?.string() ?: error("Corpo vazio")
                val clean = raw.removePrefix("\uFEFF").trim()
                JsonParser.parseString(clean).asJsonObject
            }
        }
    }
*/
