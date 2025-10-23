package com.helptech.abraham.network

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.helptech.abraham.data.remote.AkitemClient
import com.helptech.abraham.data.remote.ApiEnvelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private const val IMG_HOST = "https://tolon.com.br/"

/**
 * Cache em memória. String não-nula:
 * - URL válida = string com valor
 * - "miss" (sem imagem) = string vazia ""
 */
private val imagemCache = ConcurrentHashMap<Int, String>()

/** Converte path/URL parcial em URL absoluta. */
private fun toAbsoluteUrl(pathOrUrl: String): String {
    val p = pathOrUrl.trim()
    return if (p.startsWith("http", ignoreCase = true)) p
    else IMG_HOST + p.removePrefix("/")
}

/** Procura um campo de URL de imagem em um objeto arbitrário. */
private fun extractUrlFromImageObj(obj: JsonObject): String? {
    val candidates = listOf("urlFoto", "url", "foto", "image", "path")
    for (k in candidates) {
        if (obj.has(k)) {
            val v = obj.get(k)
            if (v != null && !v.isJsonNull) {
                val s = v.asString.trim()
                if (s.isNotEmpty()) return s
            }
        }
    }
    return null
}

/** Lê o JsonElement retornado em "sucesso" e tenta achar a URL principal. */
private fun findPrincipalUrlFromSuccess(sucesso: JsonElement): String? {
    val productObj: JsonObject = when {
        sucesso.isJsonArray && sucesso.asJsonArray.size() > 0 ->
            sucesso.asJsonArray[0].asJsonObject
        sucesso.isJsonObject -> sucesso.asJsonObject
        else -> return null
    }

    if (productObj.has("imagens") && productObj.get("imagens").isJsonArray) {
        val arr = productObj.getAsJsonArray("imagens")

        // 1) principal = "S"
        for (el in arr) {
            if (!el.isJsonObject) continue
            val obj = el.asJsonObject
            val principal = obj.get("principal")?.asString?.equals("S", true) == true
            if (principal) return extractUrlFromImageObj(obj)
        }

        // 2) primeira imagem
        if (arr.size() > 0 && arr[0].isJsonObject) {
            extractUrlFromImageObj(arr[0].asJsonObject)?.let { return it }
        }
    }

    // Fallbacks: alguns retornos colocam url direto no produto
    extractUrlFromImageObj(productObj)?.let { return it }

    return null
}

/**
 * Busca a foto principal de um produto.
 * Estratégia: cache ("" = miss) + consulta produto/consultar por código.
 */
suspend fun buscarFotoPrincipal(codigoProduto: Int): String? = withContext(Dispatchers.IO) {
    imagemCache[codigoProduto]?.let { cached ->
        return@withContext cached.ifBlank { null }
    }

    val body = mapOf(
        "item_adicional" to "",
        "n_categoria_codigo" to "",
        "codigo" to codigoProduto.toString(),
        "codigo_empresa" to "",
        "ativo" to "",
        "imagem" to ""
    )

    val env: ApiEnvelope = runCatching {
        AkitemClient.api.call(
            empresa = null, // empresa vem do interceptor
            modulo = "produto",
            funcao = "consultar",
            body = body
        )
    }.getOrElse {
        imagemCache[codigoProduto] = ""
        return@withContext null
    }

    if (env.erro != null) {
        imagemCache[codigoProduto] = ""
        return@withContext null
    }

    val urlPath = env.sucesso?.let { findPrincipalUrlFromSuccess(it) }
    val finalUrl = urlPath?.let { toAbsoluteUrl(it) } ?: ""

    imagemCache[codigoProduto] = finalUrl
    return@withContext finalUrl.ifBlank { null }
}
