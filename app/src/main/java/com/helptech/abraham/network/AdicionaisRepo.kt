package com.helptech.abraham.network

import com.google.gson.Gson
import com.helptech.abraham.data.remote.GrupoAdicionalDto
import com.helptech.abraham.data.remote.OpcaoAdicionalDto
import com.helptech.abraham.data.remote.ProdutoDto

/**
 * Converte o campo dinâmico `produto.adicionais` (que pode vir em vários formatos)
 * para uma lista estável de [GrupoAdicionalDto] usada pela UI.
 *
 * Formatos suportados:
 *  1) Map { "sucesso": [ { grupo... , "adicionais": [ ... ] } ] }
 *  2) List<GrupoAdicionalDto> já mapeada
 *  3) List<Map<*, *>> bruta (cada item é um grupo)
 */
object AdicionaisRepo {

    private val gson = Gson()

    /** API simples que a UI usa. Hoje só lê do próprio produto. */
    fun carregar(codigoProduto: Int, produto: ProdutoDto): List<GrupoAdicionalDto> =
        fromProduto(produto)

    /** Converte o campo `produto.adicionais` em grupos. */
    fun fromProduto(produto: ProdutoDto): List<GrupoAdicionalDto> {
        val raiz = produto.adicionais ?: return emptyList()

        // 2) Já vem como lista de DTOs
        if (raiz is List<*> && raiz.all { it is GrupoAdicionalDto }) {
            @Suppress("UNCHECKED_CAST")
            return raiz as List<GrupoAdicionalDto>
        }

        // 3) Lista de mapas (bruto)
        if (raiz is List<*>) {
            return raiz.mapNotNull { toGrupo(it) }
        }

        // 1) Envelope { "sucesso": [...] }
        if (raiz is Map<*, *>) {
            val sucesso = raiz["sucesso"]
            if (sucesso is List<*>) {
                return sucesso.mapNotNull { toGrupo(it) }
            }
        }

        return emptyList()
    }

    /* --------- helpers de conversão --------- */

    private fun toGrupo(any: Any?): GrupoAdicionalDto? {
        if (any == null) return null

        // já for DTO
        if (any is GrupoAdicionalDto) return any

        if (any is Map<*, *>) {
            val nome = (any["nome"] ?: any["grupo"]) as? String
            val min  = (any["adicional_qtde_min"] as? Number)?.toInt() ?: 0
            val max  = (any["adicional_qtde_max"] as? Number)?.toInt()
            val obrig = if (min > 0) "S" else (any["obrigatorio"] as? String ?: "N")

            // lista de opções pode vir em "adicionais" ou envelope { sucesso: [...] }
            val opsRaw = when (val a = any["adicionais"]) {
                is List<*> -> a
                is Map<*, *> -> a["sucesso"] as? List<*>
                else -> null
            }.orEmpty()

            val opcoes = opsRaw.mapNotNull { toOpcao(it) }

            return GrupoAdicionalDto(
                grupo = nome ?: "Adicionais",
                obrigatorio = obrig,
                max = max,
                opcoes = opcoes
            )
        }

        // fallback genérico via Gson
        return runCatching {
            gson.fromJson(gson.toJsonTree(any), GrupoAdicionalDto::class.java)
        }.getOrNull()
    }

    private fun toOpcao(any: Any?): OpcaoAdicionalDto? {
        if (any == null) return null

        if (any is OpcaoAdicionalDto) return any

        if (any is Map<*, *>) {
            return runCatching {
                gson.fromJson(gson.toJsonTree(any), OpcaoAdicionalDto::class.java)
            }.getOrNull()
        }

        return runCatching {
            gson.fromJson(gson.toJsonTree(any), OpcaoAdicionalDto::class.java)
        }.getOrNull()
    }
}
