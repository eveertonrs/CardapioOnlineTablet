package com.helptech.abraham.data.remote

/**
 * Grupo de adicionais de um produto.
 * Mantém compatibilidade com o nome antigo via typealias.
 */
data class GrupoAdicionalDto(
    val codigo: Int,
    val produtos_codigo: Int? = null,
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

/** Compatibilidade com código legado que ainda importa AdicionalGrupoDto. */
typealias AdicionalGrupoDto = GrupoAdicionalDto
