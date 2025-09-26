package com.helptech.abraham.data.remote

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

/**
 * Envelope padrão do Tolon/Akitem:
 * - "sucesso" pode ser objeto, array, string ou null -> manter como JsonElement
 * - "erro" vem como string quando a API falha
 */
data class ApiEnvelope(
    @SerializedName("sucesso") val sucesso: JsonElement? = null,
    @SerializedName("erro")    val erro: String? = null
)

/* -------------------------------------------------------------------------- */
/*                                   PRODUTO                                  */
/* -------------------------------------------------------------------------- */

/**
 * Modelo de produto retornado pela API.
 * Observações:
 * - Campos numéricos podem vir como string ("12,50") -> usamos adapters tolerantes.
 * - Mantidos nomes snake_case para compatibilidade com o restante do app.
 */
data class ProdutoDto(
    val codigo: Int,
    val categoria_codigo: Int?,
    val tipo: String?,
    val nome: String,
    val descricao: String?,
    val sub_nome: String?,
    val cor_sub_nome: String?,
    @JsonAdapter(DoubleOrNullAdapter::class)
    val valor: Double?,
    @JsonAdapter(DoubleOrNullAdapter::class)
    val valor_original: Double?,
    val estoque: Int?,
    val limitar_estoque: String?,
    val fracao: String?,
    val item_adicional_obrigar: String?,
    val adicional_juncao: String?,
    val item_adicional_multi: String?,
    val adicional_qtde_min: Int?,
    val adicional_qtde_max: Int?,
    val codigo_empresa: String?,
    val codigo_barras: String?,
    val codigo_barras_padrao: String?,
    val usu_alt: String?,
    val dta_alteracao: String?,
    val ativo: String?,
    val qtde_min_pedido: Int?,
    val incremento_qtde: Int?,
    val ordem: Int?,
    val limite_adicao: Int?,
    val pizza_qtde_sabor: Int?,
    val editavel: String?,
    val vis_online: String?,
    val pdv_obs: String?,
    val valor_custo: String?,
    val categoria_nome: String?,
    val foto: String? = null,

    // a API pode devolver um bloco "adicionais" junto do produto
    val adicionais: Any? = null
)

/* -------------------------------------------------------------------------- */
/*                                 ADICIONAIS                                 */
/* -------------------------------------------------------------------------- */

/** Item de adicional (opção dentro de um grupo). */
data class AdicionalItemDto(
    val codigo: Int,
    val categoria_codigo: Int?,
    val tipo: String?,                 // "ADICIONAL" etc
    val nome: String,
    val descricao: String?,

    @JsonAdapter(DoubleOrNullAdapter::class)
    val valor: Double?,                // muitas respostas usam "valor"

    // Em pizza/outros pode vir o preço nesta tag:
    @SerializedName("valor_ad")
    @JsonAdapter(DoubleOrNullAdapter::class)
    val valorAd: Double? = null,

    val ativo: String?,
    val categoria_nome: String? = null
)

/** Grupo de adicionais de um produto. */
data class AdicionalGrupoDto(
    val codigo: Int,
    val produtos_codigo: Int?,
    val nome: String,

    // Regras:
    val adicional_qtde_min: Int? = null,
    val adicional_qtde_max: Int? = null,
    val adicional_juncao: String? = null,      // "SOMA", "MEDIA", ...

    // Pizzas:
    val sabor_pizza: String? = null,           // "S" | "N"

    // Extras/ordenação:
    val ordem: Int? = null,
    val descricao: String? = null,

    // Lista de opções:
    val adicionais: List<AdicionalItemDto> = emptyList()
)

/* -------------------------------------------------------------------------- */
/*                                   PEDIDO                                   */
/* -------------------------------------------------------------------------- */

data class PedidoCriadoResp(
    val codigo: String? = null
)

/* -------------------------------------------------------------------------- */
/*                                  ADAPTERS                                  */
/* -------------------------------------------------------------------------- */

/**
 * Aceita número (12, 12.5) ou string ("12", "12,50") e devolve Double?.
 * Retorna null em branco, "null" ou formatos inválidos.
 */
class DoubleOrNullAdapter : JsonDeserializer<Double?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Double? {
        if (json == null || json.isJsonNull) return null
        val prim = json.asJsonPrimitive
        return try {
            when {
                prim.isNumber -> prim.asNumber.toDouble()
                prim.isString -> {
                    val s = prim.asString.trim()
                    if (s.isEmpty() || s.equals("null", true)) null
                    else s.replace(',', '.').toDoubleOrNull()
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                  HELPERS                                   */
/* -------------------------------------------------------------------------- */

/** Produto ativo quando "ativo" == "S". */
val ProdutoDto.isAtivo: Boolean
    get() = ativo.equals("S", ignoreCase = true)

/** Preço do produto com fallback para 0.0. */
val ProdutoDto.preco: Double
    get() = valor ?: 0.0

/**
 * Preço do adicional com fallback:
 * 1) Se `valor` for != 0, usa `valor`
 * 2) Senão se existir `valor_ad`, usa `valor_ad`
 * 3) Senão 0.0
 */
val AdicionalItemDto.preco: Double
    get() = when {
        (valor ?: 0.0) != 0.0 -> valor ?: 0.0
        valorAd != null       -> valorAd
        else                  -> 0.0
    }

/**
 * Se o projeto também usa OpcaoAdicionalDto (em ProductDetailSheet, Carrinho, etc),
 * mantemos a mesma regra de fallback. A classe fica em outro arquivo no mesmo pacote.
 */
val OpcaoAdicionalDto.preco: Double
    get() = when {
        (valor ?: 0.0) != 0.0 -> valor ?: 0.0
        valorAd != null       -> valorAd!!
        else                  -> 0.0
    }
