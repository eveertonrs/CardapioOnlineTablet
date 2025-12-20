package com.helptech.abraham.data.remote

import com.google.gson.JsonElement
import java.math.BigDecimal
import java.math.RoundingMode
import com.helptech.abraham.Env

/**
 * Item mínimo para envio (código, quantidade, valor unit).
 */
data class AdicionalReq(
    val codigo: Int,
    val nome: String? = null,
    val valor: Double? = null
)

data class ItemPedidoReq(
    val codigoProduto: Int,
    val quantidade: Int,
    val valorUnit: Double,
    val adicionais: List<AdicionalReq> = emptyList(),
    val observacaoItem: String? = null
)


/**
 * Utilidades para valores monetários.
 */
private fun bd2(v: Double): BigDecimal =
    BigDecimal(v).setScale(2, RoundingMode.HALF_UP)

/**
 * Monta o mapa do cliente sem enviar campos vazios que quebram no back-end.
 * - NÃO envia "codigo" quando não há um código existente.
 * - NÃO envia "fone" quando vazio (o banco deles espera inteiro; string vazia dá erro 1366).
 */
private fun clienteMesa(mesaLabel: String): Map<String, Any?> {
    val m = linkedMapOf<String, Any?>(
        "nome" to "Mesa $mesaLabel",
        "cpf" to null,
        "dta_nasc" to null
    )
    // Se um dia tiver telefone, envie somente dígitos:
    // val phoneDigits = telefone?.filter(Char::isDigit)
    // if (!phoneDigits.isNullOrEmpty()) m["fone"] = phoneDigits
    return m
}

/**
 * Envia o pedido usando:
 *   módulo = "pedido"
 *   função = "gravarPedido"
 *
 * Observações:
 * - tipo_entrega_codigo: nos exemplos do cliente "2" = BALCÃO.
 * - forma_pgto_codigo: nos exemplos variou; por padrão "1" (Dinheiro).
 * - "produtos": objeto com chaves sequenciais "1","2",... contendo os campos do item.
 */
suspend fun enviarPedido(
    mesaLabel: String,
    itens: List<ItemPedidoReq>,
    tipoEntregaCodigo: String = "2",
    formaPgtoCodigo: String = "1",
    obsPedido: String = ""
): Result<JsonElement?> {
    require(itens.isNotEmpty()) { "Lista de itens vazia" }

    // Sequenciador exigido pela API
    var seq = 1

    // Objeto "produtos" indexado por "1","2",...
    val produtosAsObject: MutableMap<String, Any> = linkedMapOf()

    // Soma total do pedido (produto + adicionais)
    var totalPedido = BigDecimal.ZERO

    for (it in itens) {
        val base = bd2(it.valorUnit)
        val linhaPrincipalTotal = (base * BigDecimal(it.quantidade)).setScale(2, RoundingMode.HALF_UP)

        // 1) Linha do produto (adicional vazio)
        val linhaProduto = linkedMapOf<String, Any?>(
            "sequencial"               to seq,
            "adicional"                to "",                 // IMPORTANTE: vazio/NULL para item principal
            "grupo_nome_adicional"     to "",
            "grupo_nome_adicional_dsc" to "",
            "codigo"                   to it.codigoProduto.toString(),
            "nome"                     to "",
            "qtde"                     to it.quantidade,
            "vlr_unit"                 to base,
            "vlr_total"                to linhaPrincipalTotal,
            "prod_obs"                 to (it.observacaoItem ?: "")
        )
        produtosAsObject[(seq++).toString()] = linhaProduto
        totalPedido = (totalPedido + linhaPrincipalTotal).setScale(2, RoundingMode.HALF_UP)

        // 2) Linhas dos adicionais como itens separados (adicional = "1")
        //    - agrupados por código para somar quantidades
        val agrupados = it.adicionais.groupBy { ad -> ad.codigo }
        for ((codigoAd, lista) in agrupados) {
            val adExemplo = lista.first()
            val precoAd = bd2(adExemplo.valor ?: 0.0)

            // qtd por item * quantidade do item
            val qtdTotal = lista.size * it.quantidade
            val totalAd = (precoAd * BigDecimal(qtdTotal)).setScale(2, RoundingMode.HALF_UP)

            val linhaAdicional = linkedMapOf<String, Any?>(
                "sequencial"               to seq,
                "adicional"                to "1",                         // FLAG de adicional
                "grupo_nome_adicional"     to "",                          // preencha se tiver
                "grupo_nome_adicional_dsc" to "",                          // preencha se tiver
                "codigo"                   to codigoAd.toString(),
                "nome"                     to (adExemplo.nome ?: ""),
                "qtde"                     to qtdTotal,
                "vlr_unit"                 to precoAd,
                "vlr_total"                to totalAd,
                "prod_obs"                 to ""                            // opcional
            )
            produtosAsObject[(seq++).toString()] = linhaAdicional
            totalPedido = (totalPedido + totalAd).setScale(2, RoundingMode.HALF_UP)
        }
    }

    val body: Map<String, Any?> = linkedMapOf(
        "cliente"                 to clienteMesa(mesaLabel),
        "cli_endereco"            to null,
        "notificacao"             to "",
        "tipo_entrega_codigo"     to tipoEntregaCodigo,
        "entrega_agendada"        to "N",
        "entrega_agendada_data"   to null,
        "entrega_agendada_hora"   to null,
        "forma_pgto_codigo"       to formaPgtoCodigo,
        "forma_pgto_adicional"    to "",
        "tipo_entrega_adicional"  to mesaLabel,
        "local_entrega"           to mesaLabel,
        "situacao_pgto"           to "PENDENTE",
        "pedido_situacao_codigo"  to 1,
        "vlr_desc"                to BigDecimal.ZERO,
        "dsc_desc"                to "",
        "vlr_taxa"                to BigDecimal.ZERO,
        "dsc_taxa"                to "",
        "vlr_acrescimo"           to BigDecimal.ZERO,
        "vlr_produtos"            to totalPedido,  // agora inclui adicionais
        "vlr_total"               to totalPedido,  // idem
        "obs"                     to obsPedido,
        "produtos"                to produtosAsObject,
        "pin"                     to "",
        "complementos"            to emptyList<Map<String, String>>(),
        "previsao_entrega"        to null
    )

    val env = AkitemClient.api.call(
        empresa = Env.RUNTIME_EMPRESA,
        modulo  = "pedido",
        funcao  = "gravarPedido",
        body    = body
    )

    return if (env.erro == null) Result.success(env.sucesso)
    else Result.failure(IllegalStateException(env.erro))
}



/* Operador auxiliar para BigDecimal */
private operator fun BigDecimal.times(other: BigDecimal): BigDecimal = this.multiply(other)
