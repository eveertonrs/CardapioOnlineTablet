package com.helptech.abraham.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.helptech.abraham.data.remote.AdicionalItemDto
import com.helptech.abraham.data.remote.GrupoAdicionalDto
import com.helptech.abraham.data.remote.ProdutoDto

/**
 * Lê os "adicionais" que já podem vir junto do Produto (em vários formatos)
 * e normaliza para List<GrupoAdicionalDto>.
 *
 * Formatos aceitos (vistos em ambientes diferentes):
 *  - produto.adicionais é List<GrupoAdicionalDto>
 *  - produto.adicionais é List<Map<String, *>> com chaves legadas: "grupo", "obrigatorio", "max", "opcoes"
 *  - produto.adicionais é Map com "grupos" ou "adicionais" contendo uma lista
 *  - produto.adicionais é JsonElement ou String com os formatos acima
 */
object AdicionaisRepo {

    private const val TAG = "AdicionaisRepo"
    private val gson by lazy { Gson() }

    // Cache em memória por **código int** de produto – usado para fallback offline
    private val cacheByProdutoCodigo = mutableMapOf<Int, List<GrupoAdicionalDto>>()

    /**
     * Converte o campo produto.adicionais para List<GrupoAdicionalDto>
     * e, se conseguir, guarda em cache usando produto.codigo (Int).
     */
    fun fromProduto(produto: ProdutoDto): List<GrupoAdicionalDto> {
        val raw = produto.adicionais ?: return emptyList()

        val grupos: List<GrupoAdicionalDto> = try {
            when (raw) {
                is List<*>     -> parseListAny(raw)
                is Map<*, *>   -> parseMapAny(raw)
                is JsonElement -> parseJsonElement(raw)
                is String      -> parseString(raw)
                else -> {
                    Log.d(TAG, "Formato de adicionais não reconhecido: ${raw::class.java.simpleName}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao converter adicionais do produto ${produto.codigo}: ${e.message}")
            emptyList()
        }

        // Se deu certo, guarda no cache para uso offline
        val codigo: Int? = produto.codigo
        if (codigo != null && grupos.isNotEmpty()) {
            cacheByProdutoCodigo[codigo] = grupos
        }

        return grupos
    }

    /**
     * Retorna a lista de grupos de adicionais em cache para o código do produto (Int).
     * Usar como fallback quando não tiver internet.
     */
    fun getCachedByProdutoCodigo(codigoProduto: Int?): List<GrupoAdicionalDto>? {
        val key = codigoProduto ?: return null
        return cacheByProdutoCodigo[key]
    }

    /* ----------------------------- Parsers ------------------------------ */

    private fun parseString(s: String): List<GrupoAdicionalDto> {
        val trimmed = s.trim()
        if (trimmed.isEmpty()) return emptyList()

        return try {
            val je = JsonParser.parseString(trimmed)
            parseJsonElement(je)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseJsonElement(el: JsonElement): List<GrupoAdicionalDto> {
        if (el.isJsonNull) return emptyList()

        // 1) tenta direto como lista de GrupoAdicionalDto
        val directList: List<GrupoAdicionalDto>? = runCatching {
            val listType = object : TypeToken<List<GrupoAdicionalDto>>() {}.type
            gson.fromJson<List<GrupoAdicionalDto>>(el, listType)
        }.getOrNull()

        if (directList != null) return directList

        // 2) objeto: pode ter "grupos" ou "adicionais" ou vir num formato legado
        if (el.isJsonObject) {
            val obj = el.asJsonObject

            if (obj.has("grupos")) {
                return parseJsonElement(obj.get("grupos"))
            }
            if (obj.has("adicionais")) {
                return parseJsonElement(obj.get("adicionais"))
            }

            // formato legado: alguma chave com array de grupos
            return parseLegacyGroupsFromJsonObject(obj)
        }

        // 3) array genérico
        if (el.isJsonArray) {
            val arr = el.asJsonArray
            val anyList: List<Any> = gson.fromJson(
                arr,
                object : TypeToken<List<Any>>() {}.type
            )
            return parseListAny(anyList)
        }

        return emptyList()
    }

    private fun parseLegacyGroupsFromJsonObject(obj: JsonObject): List<GrupoAdicionalDto> {
        // Alguns ambientes aninham os grupos numa chave qualquer;
        // tenta achar a primeira entrada que seja array.
        val entryWithArray = obj.entrySet().firstOrNull { it.value.isJsonArray }
        val arr = entryWithArray?.value ?: return emptyList()
        return parseJsonElement(arr)
    }

    private fun parseListAny(list: List<*>): List<GrupoAdicionalDto> {
        val first = list.firstOrNull() ?: return emptyList()

        // Caso já seja a lista do tipo certo
        if (first is GrupoAdicionalDto) {
            @Suppress("UNCHECKED_CAST")
            return list as List<GrupoAdicionalDto>
        }

        // Lista de JsonObject / Map / String (formatos variados)
        val out = ArrayList<GrupoAdicionalDto>()

        list.forEachIndexed { index, item ->
            when (item) {
                is JsonElement -> out += parseOneFromJsonElement(item, index)
                is Map<*, *>   -> parseOneFromMap(item, index)?.let { out += it }
                is String      -> {
                    val el = runCatching { JsonParser.parseString(item) }.getOrNull()
                    if (el != null) {
                        out += parseOneFromJsonElement(el, index)
                    }
                }
                // null ou outro tipo é ignorado
            }
        }

        return out
    }

    private fun parseMapAny(map: Map<*, *>): List<GrupoAdicionalDto> {
        // Map pode ter "grupos" ou "adicionais" dentro
        val inner: Any? = when {
            map.containsKey("grupos")     -> map["grupos"]
            map.containsKey("adicionais") -> map["adicionais"]
            else -> null
        }

        return when (inner) {
            is List<*>       -> parseListAny(inner)
            is JsonElement   -> parseJsonElement(inner)
            is String        -> parseString(inner)
            is Map<*, *>     -> parseMapAny(inner)
            else             -> emptyList()
        }
    }

    private fun parseOneFromJsonElement(el: JsonElement, index: Int): GrupoAdicionalDto {
        // Tenta direto como GrupoAdicionalDto
        val direct: GrupoAdicionalDto? = runCatching {
            gson.fromJson(el, GrupoAdicionalDto::class.java)
        }.getOrNull()

        if (direct != null) return direct

        // Se for objeto, tenta como legado
        if (el.isJsonObject) {
            val obj = el.asJsonObject

            val nome = obj.getAsStringOrNull("nome")
                ?: obj.getAsStringOrNull("grupo")
                ?: "Grupo ${index + 1}"

            val obrigatorio = obj.getAsStringOrNull("obrigatorio")?.equals("S", true)
                ?: obj.getAsBooleanOrNull("obrigatorio")
                ?: false

            val max = obj.getAsIntOrNull("max")
                ?: obj.getAsIntOrNull("adicional_qtde_max")

            val min = obj.getAsIntOrNull("adicional_qtde_min")
                ?: (if (obrigatorio) 1 else 0)

            val ordem = obj.getAsIntOrNull("ordem") ?: (index + 1)

            val opcoesEl: JsonElement? = when {
                obj.has("opcoes")     -> obj.get("opcoes")
                obj.has("adicionais") -> obj.get("adicionais")
                else -> null
            }

            val opcoes: List<AdicionalItemDto> = when {
                opcoesEl == null || opcoesEl.isJsonNull -> emptyList()
                opcoesEl.isJsonArray -> {
                    val listType = object : TypeToken<List<AdicionalItemDto>>() {}.type
                    runCatching {
                        gson.fromJson<List<AdicionalItemDto>>(opcoesEl, listType)
                    }.getOrDefault(emptyList())
                }
                else -> emptyList()
            }

            val codigo = obj.getAsIntOrNull("codigo") ?: -(index + 1)

            return GrupoAdicionalDto(
                codigo = codigo,
                produtos_codigo = obj.getAsIntOrNull("produtos_codigo"),
                nome = nome,
                adicional_qtde_min = min,
                adicional_qtde_max = max,
                adicional_juncao = obj.getAsStringOrNull("adicional_juncao"),
                sabor_pizza = obj.getAsStringOrNull("sabor_pizza"),
                ordem = ordem,
                descricao = obj.getAsStringOrNull("descricao"),
                adicionais = opcoes
            )
        }

        // Fallback vazio
        return GrupoAdicionalDto(
            codigo = -(index + 1),
            produtos_codigo = null,
            nome = "Grupo ${index + 1}",
            adicionais = emptyList()
        )
    }

    private fun parseOneFromMap(map: Map<*, *>, index: Int): GrupoAdicionalDto? {
        // Transforma num JsonObject para reutilizar a lógica acima
        val json = gson.toJsonTree(map)
        return parseOneFromJsonElement(json, index)
    }
}

/* --------------------------- Helpers JsonObject --------------------------- */

private fun JsonObject.getAsStringOrNull(key: String): String? =
    runCatching {
        this.get(key)
            ?.takeIf { !it.isJsonNull }
            ?.asString
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }.getOrNull()

private fun JsonObject.getAsIntOrNull(key: String): Int? =
    runCatching {
        val el = this.get(key) ?: return null
        when {
            el.isJsonNull -> null
            el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> el.asInt
            el.isJsonPrimitive && el.asJsonPrimitive.isString -> el.asString.trim().toIntOrNull()
            else -> null
        }
    }.getOrNull()

private fun JsonObject.getAsBooleanOrNull(key: String): Boolean? =
    runCatching {
        val el = this.get(key) ?: return null
        when {
            el.isJsonNull -> null
            el.isJsonPrimitive && el.asJsonPrimitive.isBoolean -> el.asBoolean
            el.isJsonPrimitive && el.asJsonPrimitive.isString -> {
                val s = el.asString.trim().lowercase()
                when (s) {
                    "s", "sim", "true", "1" -> true
                    "n", "nao", "não", "false", "0" -> false
                    else -> null
                }
            }
            else -> null
        }
    }.getOrNull()
