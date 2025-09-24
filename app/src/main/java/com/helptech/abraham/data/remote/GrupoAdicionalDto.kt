package com.helptech.abraham.data.remote

import com.google.gson.annotations.SerializedName

/** Grupo de adicionais de um produto (ex.: "Escolha seus adicionais"). */
data class GrupoAdicionalDto(
    @SerializedName("nome")
    val grupo: String? = null,                         // nome do grupo
    @SerializedName("adicionais")
    val opcoes: List<OpcaoAdicionalDto>? = emptyList(),// opções dentro do grupo
    @SerializedName("adicional_qtde_max")
    val max: Int? = null,                              // 0 = ilimitado
    @SerializedName("adicional_qtde_min")
    val min: Int? = null,
    // Alguns ambientes não enviam flag explícita; mantemos compatível com a UI
    val obrigatorio: String? = null                    // "S" / "N" (se houver)
)
