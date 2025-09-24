package com.helptech.abraham.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Wrappers para o retorno de produto/consultar?com_adicionais=S
 * Mantém compatibilidade sem redeclarar DTOs já existentes.
 */

data class ProdutoComAdicionaisEnvelope(
    val sucesso: List<ProdutoComAdicionaisItem>? = null,
    val erro: String? = null
)

data class ProdutoComAdicionaisItem(
    val codigo: Int,
    @SerializedName("adicionais")
    val blocoAdicionais: BlocoAdicionais? = null
)

data class BlocoAdicionais(
    val sucesso: List<GrupoAdicionalDto>? = null
)
