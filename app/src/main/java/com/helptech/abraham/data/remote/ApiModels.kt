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

    @JsonAdapter(DoubleOrNullAdapter::class)
    val valor: Double?,

    @JsonAdapter(DoubleOrNullAdapter::class)
    val estoque: Double?,

    val ativo: String?,
    val categoria_nome: String?,

    // A API pode usar várias chaves para foto; mapeamos todas.
    @SerializedName(value = "foto", alternate = ["imagem", "image", "img", "foto_base64"])
    val foto: String? = null,
)

/* ----------------------------- MODELOS DE PEDIDO ------------------------------ */

/** Item usado para enviar o pedido (usa nomes que você já referenciou no código). */
/*data class ItemPedidoReq(
    val codigoProduto: Int,
    val quantidade: Int,
    val valorUnit: Double
)*/

/** Algumas respostas retornam um objeto com "codigo" do pedido criado. */
data class PedidoCriadoResp(
    val codigo: String? = null
)

/* ----------------------------- ADAPTERS ------------------------------ */

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
                    if (s.isEmpty() || s.equals("null", ignoreCase = true)) return null
                    // Troca vírgula por ponto caso venha no formato brasileiro
                    s.replace(',', '.').toDoubleOrNull()
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}

/* ----------------------------- HELPERS ------------------------------- */

/** Conveniência: produto ativo quando campo "ativo" == "S" (case-insensitive). */
val ProdutoDto.isAtivo: Boolean
    get() = ativo.equals("S", ignoreCase = true)

/** Preço com fallback para 0.0 quando nulo (útil em cálculos). */
val ProdutoDto.preco: Double
    get() = valor ?: 0.0
