package com.helptech.abraham.data.remote

import com.google.gson.JsonElement
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Item mínimo para envio (código, quantidade, valor unit).
 */
data class ItemPedidoReq(
    val codigoProduto: Int,
    val quantidade: Int,
    val valorUnit: Double
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
    mesaLabel: String,                       // ex.: "MESA 10" ou "BALCÃO"
    itens: List<ItemPedidoReq>,
    tipoEntregaCodigo: String = "2",         // 2 = BALCÃO
    formaPgtoCodigo: String = "1",           // 1 = Dinheiro
    obsPedido: String = ""
): Result<JsonElement?> {
    require(itens.isNotEmpty()) { "Lista de itens vazia" }

    // Total dos produtos
    val total = itens.fold(BigDecimal.ZERO) { acc, it ->
        acc + bd2(it.valorUnit) * BigDecimal(it.quantidade)
    }.setScale(2, RoundingMode.HALF_UP)

    // "produtos" precisa ser um OBJETO indexado por "1","2",...
    val produtosAsObject: MutableMap<String, Any> = linkedMapOf()
    var seq = 1
    for (it in itens) {
        val linha = linkedMapOf<String, Any?>(
            "sequencial" to seq,
            "adicional" to "",
            "grupo_nome_adicional" to "",
            "grupo_nome_adicional_dsc" to "",
            "codigo" to it.codigoProduto.toString(),
            // Nome é opcional; mantemos vazio para não depender do catálogo local
            "nome" to "",
            "qtde" to it.quantidade,
            "vlr_unit" to bd2(it.valorUnit),
            "vlr_total" to (bd2(it.valorUnit) * BigDecimal(it.quantidade)).setScale(2, RoundingMode.HALF_UP),
            "prod_obs" to ""
        )
        produtosAsObject[(seq++).toString()] = linha
    }

    // Corpo conforme especificado pelo cliente
    val body: Map<String, Any?> = linkedMapOf(
        "cliente" to clienteMesa(mesaLabel),
        "cli_endereco" to null,                  // não necessário para balcão/mesa
        "notificacao" to "",
        "tipo_entrega_codigo" to tipoEntregaCodigo,
        "entrega_agendada" to "N",
        "entrega_agendada_data" to null,
        "entrega_agendada_hora" to null,
        "forma_pgto_codigo" to formaPgtoCodigo,
        "forma_pgto_adicional" to "",
        "tipo_entrega_adicional" to mesaLabel,   // aparece no painel
        "local_entrega" to mesaLabel,            // idem
        "situacao_pgto" to "PENDENTE",
        "pedido_situacao_codigo" to 1,           // 1 = aguardando
        "vlr_desc" to BigDecimal.ZERO,
        "dsc_desc" to "",
        "vlr_taxa" to BigDecimal.ZERO,
        "dsc_taxa" to "",
        "vlr_acrescimo" to BigDecimal.ZERO,
        "vlr_produtos" to total,
        "vlr_total" to total,
        "obs" to obsPedido,
        "produtos" to produtosAsObject,
        "pin" to "",
        "complementos" to emptyList<Map<String, String>>(),  // ex.: [{"campo":"troco_para","valor":"100"}]
        "previsao_entrega" to null
    )

    val env = AkitemClient.api.call(
        empresa = null,   // runtime via interceptor
        modulo  = "pedido",
        funcao  = "gravarPedido",
        body    = body
    )

    return if (env.erro == null) Result.success(env.sucesso)
    else Result.failure(IllegalStateException(env.erro))
}

/* Operador auxiliar para BigDecimal */
private operator fun BigDecimal.times(other: BigDecimal): BigDecimal = this.multiply(other)
