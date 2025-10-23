package com.helptech.abraham.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.helptech.abraham.data.remote.AdicionalItemDto
import com.helptech.abraham.data.remote.GrupoAdicionalDto
import com.helptech.abraham.data.remote.OpcaoAdicionalDto
import com.helptech.abraham.data.remote.ProdutoDto
import com.helptech.abraham.data.remote.preco
import com.helptech.abraham.network.buscarFotoPrincipal
import kotlinx.coroutines.launch

/** Retorno do sheet: quantidade e as opções escolhidas por grupo. */
data class ProdutoEscolhido(
    val quantidade: Int,
    val escolhas: Map<String, List<OpcaoAdicionalDto>>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailSheet(
    produto: ProdutoDto,
    grupos: List<GrupoAdicionalDto>,
    onDismiss: () -> Unit,
    onConfirm: (ProdutoEscolhido) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { scope.launch { sheetState.expand() } }

    var quantidade by remember { mutableStateOf(1) }

    fun keyOf(g: GrupoAdicionalDto, idx: Int): String =
        g.nome.takeIf { it.isNotBlank() } ?: "Adicionais #$idx"

    // Guardamos seleções por grupo (AdicionalItemDto); no confirmar mapeamos pra OpcaoAdicionalDto
    val escolhas: MutableMap<String, SnapshotStateList<AdicionalItemDto>> =
        remember(grupos) {
            mutableStateMapOf<String, SnapshotStateList<AdicionalItemDto>>().apply {
                grupos.forEachIndexed { i, g -> put(keyOf(g, i), mutableStateListOf()) }
            }
        }

    var fotoUrl by remember(produto.codigo) { mutableStateOf<String?>(null) }
    LaunchedEffect(produto.codigo) {
        fotoUrl = try { buscarFotoPrincipal(produto.codigo) } catch (_: Exception) { null }
    }

    // soma dos adicionais selecionados (usando extension preco do AdicionalItemDto)
    val adicionalUnit by remember(grupos, escolhas) {
        derivedStateOf { escolhas.values.flatten().sumOf { it.preco } }
    }
    val precoUnit = (produto.valor ?: 0.0) + adicionalUnit
    val total = precoUnit * quantidade

    val podeConfirmar by remember(grupos, escolhas) {
        derivedStateOf {
            grupos.withIndex().all { (idx, g) ->
                val min = g.adicional_qtde_min ?: 0
                if (min > 0) escolhas[keyOf(g, idx)].orEmpty().size >= min else true
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color.White,
        contentColor = Color(0xFF111111),
        scrimColor = Color(0x99000000)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = fotoUrl ?: produto.foto,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = produto.nome,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF111111)
                    )
                    if (!produto.descricao.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = produto.descricao!!,
                            color = Color(0xFF5F6368)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = formatMoneyUi(precoUnit),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                }
                QuantityStepper(
                    valor = quantidade,
                    onMinus = { if (quantidade > 1) quantidade-- },
                    onPlus = { quantidade++ }
                )
            }

            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            if (grupos.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                ) {
                    items(grupos, key = { (it.nome.ifBlank { "Adicionais" }) + "|" + it.hashCode() }) { g ->
                        val idx = grupos.indexOf(g)
                        val gKey = keyOf(g, idx)

                        GrupoAdicionalCard(
                            grupo = g,
                            selecionadas = escolhas[gKey].orEmpty(),
                            onSelect = { opc ->
                                val max = g.adicional_qtde_max ?: 0
                                val lista = escolhas.getOrPut(gKey) { mutableStateListOf() }
                                if (max <= 1) {
                                    lista.clear(); lista.add(opc)
                                } else {
                                    val exists = lista.any { it.codigo == opc.codigo }
                                    if (exists) {
                                        lista.removeAll { it.codigo == opc.codigo }
                                    } else if (max == 0 || lista.size < max) {
                                        lista.add(opc)
                                    }
                                }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
            } else {
                Text("Sem adicionais para este produto.", color = Color(0xFF5F6368))
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    // Converte selecionados (AdicionalItemDto) -> OpcaoAdicionalDto pro carrinho
                    val escolhasMap: Map<String, List<OpcaoAdicionalDto>> =
                        escolhas.mapValues { (_, lista) ->
                            lista.map { it.asOpcao() }
                        }

                    onConfirm(
                        ProdutoEscolhido(
                            quantidade = quantidade,
                            escolhas = escolhasMap
                        )
                    )
                },
                enabled = podeConfirmar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Adicionar ao carrinho — ${formatMoneyUi(total)}")
            }
        }
    }
}

@Composable
private fun GrupoAdicionalCard(
    grupo: GrupoAdicionalDto,
    selecionadas: List<AdicionalItemDto>,
    onSelect: (AdicionalItemDto) -> Unit
) {
    val opcoes: List<AdicionalItemDto> = grupo.adicionais
    if (opcoes.isEmpty()) return

    val tituloGrupo = grupo.nome.ifBlank { "Adicionais" }
    val obrigatorio = (grupo.adicional_qtde_min ?: 0) > 0
    val max = grupo.adicional_qtde_max ?: 0

    Surface(
        color = Color(0xFFF5F6F8),
        contentColor = Color(0xFF111111),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, Color(0x22000000)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            val sub = buildString {
                if (obrigatorio) append("Obrigatório")
                if (max > 0) { if (isNotEmpty()) append(" • "); append("Até $max") }
            }
            Text(
                text = tituloGrupo,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            if (sub.isNotEmpty()) {
                Text(text = sub, color = Color(0xFF5F6368), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))

            opcoes.forEach { op ->
                val checked = selecionadas.any { it.codigo == op.codigo }
                val nomeOpc = op.nome.ifBlank { "Opção" }

                val precoOpc = op.preco
                val titulo = if (precoOpc > 0.0) {
                    "$nomeOpc  (+ ${formatMoneyUi(precoOpc)})"
                } else nomeOpc

                if (max <= 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = checked,
                            onClick = { onSelect(op) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(text = titulo, modifier = Modifier.padding(start = 8.dp))
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onSelect(op) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(text = titulo, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    valor: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMinus) { Icon(Icons.Filled.Remove, contentDescription = null, tint = Color(0xFF111111)) }
        Text("$valor", modifier = Modifier.widthIn(min = 24.dp), color = Color(0xFF111111))
        IconButton(onClick = onPlus) { Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF111111)) }
    }
}

private fun formatMoneyUi(v: Double?): String {
    val x = v ?: 0.0
    return "R$ " + "%,.2f".format(x).replace(',', 'X').replace('.', ',').replace('X', '.')
}

/** Criei aqui pra resolver o erro de referência ausente. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailLoading(onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = Color.White,
        contentColor = Color(0xFF111111),
        scrimColor = Color(0x99000000)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Carregando adicionais…")
        }
    }
}

/* ------------------------- Mapeamento para carrinho ------------------------- */

private fun AdicionalItemDto.asOpcao(): OpcaoAdicionalDto =
    OpcaoAdicionalDto(
        codigo = this.codigo,
        categoria_codigo = this.categoria_codigo,
        tipo = this.tipo,
        nome = this.nome,
        descricao = this.descricao,
        sub_nome = null,
        cor_sub_nome = null,
        valor = this.valor,
        valorAd = this.valorAd,
        estoque = null,
        limitar_estoque = null,
        fracao = null,
        item_adicional_obrigar = null,
        adicional_juncao = null,
        item_adicional_multi = null,
        adicional_qtde_min = null,
        adicional_qtde_max = null,
        codigo_empresa = null,
        codigo_barras = null,
        codigo_barras_padrao = null,
        usu_alt = null,
        dta_alteracao = null,
        ativo = null,
        qtde_min_pedido = null,
        incremento_qtde = null,
        ordem = null,
        limite_adicao = null,
        pizza_qtde_sabor = null,
        editavel = null,
        vis_online = null,
        pdv_obs = null,
        valor_custo = null,
        categoria_nome = null
    )
