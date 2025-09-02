package com.helptech.abraham.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private const val IMG_HOST = "https://tolon.com.br/"

// cache simples em memória (evita chamar toda hora ao rolar a lista)
private val imagemCache = ConcurrentHashMap<Int, String?>()

/** Normaliza a resposta do backend para um path/URL utilizável */
private fun extractImgPath(raw: String): String? {
    val trimmed = raw.trim().removePrefix("\uFEFF") // remove BOM se vier
    val unescaped = trimmed.replace("\\/", "/")
    // Caso venha como <img src="...">
    val src = Regex("""src\s*=\s*["']([^"']+)["']""").find(unescaped)?.groupValues?.get(1)
    val path = src ?: unescaped.trim('"')
    return path.takeIf { it.contains("arquivos/") || it.startsWith("http", true) }
}

/** Monta URL absoluta se só vier o path */
private fun toAbsoluteUrl(pathOrUrl: String): String {
    return if (pathOrUrl.startsWith("http", true)) pathOrUrl
    else IMG_HOST + pathOrUrl.removePrefix("/")
}

/** Chama o endpoint e devolve a URL da foto principal do produto */
suspend fun buscarFotoPrincipal(produtoCodigo: Int): String? = withContext(Dispatchers.IO) {
    // usa cache primeiro
    imagemCache[produtoCodigo]?.let { return@withContext it }

    val api = TolonApiClient.api
    val body = RetornaProdutoAnexoBody(produtosCodigo = produtoCodigo.toString())
    val raw = api.retornaProdutoAnexo(body = body)
    val path = extractImgPath(raw)
    val url = path?.let(::toAbsoluteUrl)

    imagemCache[produtoCodigo] = url
    return@withContext url
}
