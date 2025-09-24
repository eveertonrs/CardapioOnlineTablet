package com.helptech.abraham.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Opção de adicional (item dentro de um grupo).
 * Importante: o backend manda "valor_ad" (snake_case) — mapeado para "valorAd".
 */
data class OpcaoAdicionalDto(
    val codigo: Int,
    val categoria_codigo: Int? = null,
    val tipo: String? = null,
    val nome: String? = null,
    val descricao: String? = null,
    val sub_nome: String? = null,
    val cor_sub_nome: String? = null,

    // preço exibido “valor” (normalmente 0 para adicionais)
    @SerializedName("valor")
    val valor: Double? = null,

    // preço do adicional vindo do JSON
    @SerializedName("valor_ad")
    val valorAd: Double? = null,

    val estoque: Int? = null,
    val limitar_estoque: String? = null,
    val fracao: String? = null,
    val item_adicional_obrigar: String? = null,
    val adicional_juncao: String? = null,
    val item_adicional_multi: String? = null,
    val adicional_qtde_min: Int? = null,
    val adicional_qtde_max: Int? = null,
    val codigo_empresa: String? = null,
    val codigo_barras: String? = null,
    val codigo_barras_padrao: String? = null,
    val usu_alt: Any? = null,
    val dta_alteracao: String? = null,
    val ativo: String? = null,
    val qtde_min_pedido: Int? = null,
    val incremento_qtde: Int? = null,
    val ordem: Int? = null,
    val limite_adicao: Int? = null,
    val pizza_qtde_sabor: Int? = null,
    val editavel: String? = null,
    val vis_online: String? = null,
    val pdv_obs: String? = null,
    val valor_custo: String? = null,
    val categoria_nome: String? = null
)
