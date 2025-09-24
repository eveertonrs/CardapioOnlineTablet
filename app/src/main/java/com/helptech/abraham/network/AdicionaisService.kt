package com.helptech.abraham.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.helptech.abraham.BuildConfig
import com.helptech.abraham.data.remote.AkitemClient
import com.helptech.abraham.data.remote.ApiEnvelope
import com.helptech.abraham.data.remote.GrupoAdicionalDto
import com.helptech.abraham.data.remote.ProdutoComAdicionaisItem

/**
 * Consulta adicionais tentando 3 variações conhecidas.
 * Fallback para quando o Produto não traz "adicionais".
 */
object AdicionaisService {
    private const val TAG = "AdicionaisService"

    suspend fun consultarAdicionais(produtoCodigo: Int): List<GrupoAdicionalDto> {
        // 1) produto/consultarAdicional
        consultar(
            modulo = "produto",
            funcao = "consultarAdicional",
            body = mapOf("produto_codigo" to produtoCodigo)
        )?.let { return it }

        // 2) adicional/consultar
        consultar(
            modulo = "adicional",
            funcao = "consultar",
            body = mapOf("produto_codigo" to produtoCodigo)
        )?.let { return it }

        // 3) produto/consultar + com_adicionais=S  (seu ambiente respondeu aqui)
        consultar(
            modulo = "produto",
            funcao = "consultar",
            body = mapOf("codigo" to produtoCodigo, "com_adicionais" to "S")
        )?.let { return it }

        return emptyList()
    }

    private suspend fun consultar(
        modulo: String,
        funcao: String,
        body: Map<String, Any?>
    ): List<GrupoAdicionalDto>? {
        return try {
            val env: ApiEnvelope = AkitemClient.api.call(
                empresa = BuildConfig.API_EMPRESA,
                modulo = modulo,
                funcao = funcao,
                body = body
            )
            if (env.erro != null) {
                Log.w(TAG, "API $modulo/$funcao: ${env.erro}")
                return null
            }

            // produto/consultar => lista de produtos; extrair adicionais do item
            if (modulo == "produto" && funcao == "consultar") {
                parseProdutoComAdicionais(env.sucesso, (body["codigo"] as? Int) ?: -1)
            } else {
                parseGruposDireto(env.sucesso)
            }
        } catch (e: Exception) {
            Log.w(TAG, "API $modulo/$funcao falhou: ${e.message}")
            null
        }
    }

    /** Caso já seja lista de grupos, ou { "grupos": [...] } / { "adicionais": [...] } */
    private fun parseGruposDireto(el: JsonElement?): List<GrupoAdicionalDto> {
        if (el == null || el.isJsonNull) return emptyList()
        val gson = Gson()
        val listType = object : TypeToken<List<GrupoAdicionalDto>>() {}.type
        return try {
            when {
                el.isJsonArray -> gson.fromJson(el, listType) ?: emptyList()
                el.isJsonObject -> {
                    val obj = el.asJsonObject
                    when {
                        obj.has("grupos")     -> gson.fromJson(obj.get("grupos"), listType) ?: emptyList()
                        obj.has("adicionais") -> gson.fromJson(obj.get("adicionais"), listType) ?: emptyList()
                        else -> emptyList()
                    }
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** produto/consultar?com_adicionais=S -> sucesso é lista de produtos; pegar adicionais do código pedido */
    private fun parseProdutoComAdicionais(el: JsonElement?, codigo: Int): List<GrupoAdicionalDto> {
        if (el == null || el.isJsonNull) return emptyList()
        val gson = Gson()
        val listType = object : TypeToken<List<ProdutoComAdicionaisItem>>() {}.type
        return try {
            val itens: List<ProdutoComAdicionaisItem> = when {
                el.isJsonArray -> gson.fromJson(el, listType) ?: emptyList()
                el.isJsonObject && el.asJsonObject.has("sucesso") ->
                    gson.fromJson(el.asJsonObject.get("sucesso"), listType) ?: emptyList()
                else -> emptyList()
            }
            val doProduto = itens.firstOrNull { it.codigo == codigo }
            doProduto?.blocoAdicionais?.sucesso ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
