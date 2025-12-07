package com.helptech.abraham.data.remote

/**
 * Representa um item no carrinho de compras, combinando um produto,
 * a quantidade e os adicionais selecionados.
 */
data class CartItem(
    val key: String,
    val produto: ProdutoDto,
    var quantidade: Int,
    val adicionais: List<OpcaoAdicionalDto>
)

/** Calcula o valor somado de todos os adicionais para um item. */
fun adicionalUnit(item: CartItem): Double =
    item.adicionais.sumOf { it.preco }

/** Calcula o preço unitário final de um item (produto + adicionais). */
fun unitPrice(item: CartItem): Double =
    (item.produto.valor ?: 0.0) + adicionalUnit(item)

/** Calcula o total da linha para um item (preço unitário * quantidade). */
fun lineTotal(item: CartItem): Double =
    unitPrice(item) * item.quantidade

/**
 * Gera uma chave única para um item de carrinho, baseada no código do produto
 * e nos códigos dos adicionais selecionados. Isso garante que o mesmo produto
 * com os mesmos adicionais seja agrupado no carrinho.
 */
fun cartKey(produto: ProdutoDto, adicionais: List<OpcaoAdicionalDto>): String {
    val addKey = adicionais.map { it.codigo }.sorted().joinToString("-")
    return "${produto.codigo}|$addKey"
}
