package com.helptech.abraham.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.helptech.abraham.data.remote.AdicionalItemDto
import com.helptech.abraham.data.remote.GrupoAdicionalDto
import com.helptech.abraham.data.remote.OpcaoAdicionalDto
import com.helptech.abraham.data.remote.ProdutoDto
import com.helptech.abraham.network.buscarFotoPrincipal

/** Retorno do sheet: quantidade e as opções escolhidas por grupo. */
data class ProdutoEscolhido(
    val quantidade: Int,
    val escolhas: Map<String, List<OpcaoAdicionalDto>>
)

/* Paleta local (compatível com os prints do Abrahão) */
private val Orange = Color(0xFFF57C00)
private val PanelBg = Color(0xFF1F2226)
private val CardBg  = Color(0xFFF5F6F8)
private val Muted   = Color(0xFF5F6368)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailSheet(
    produto: ProdutoDto,
    grupos: List<GrupoAdicionalDto>,
    onDismiss: () -> Unit,
    onConfirm: (ProdutoEscolhido) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Dentro de LaunchedEffect já é uma corrotina; não precisa de scope.launch
    LaunchedEffect(Unit) { sheetState.expand() }

    // ====== Estado ======
    var quantidade by remember { mutableStateOf(1) }
    var observacao by remember { mutableStateOf("") }
    var fotoUrl by remember(produto.codigo) { mutableStateOf<String?>(null) }
    LaunchedEffect(produto.codigo) {
        fotoUrl = try { buscarFotoPrincipal(produto.codigo) } catch (_: Exception) { null }
    }

    // chave dos grupos (nome ou "Grupo #")
    fun gKey(g: GrupoAdicionalDto, idx: Int) = g.nome.takeIf { it.isNotBlank() } ?: "Grupo ${idx + 1}"

    // Seleções por grupo (AdicionalItemDto); convertidas no confirmar
    val selecoes: MutableMap<String, SnapshotStateList<AdicionalItemDto>> =
        remember(grupos) {
            mutableStateMapOf<String, SnapshotStateList<AdicionalItemDto>>().apply {
                grupos.forEachIndexed { i, g -> put(gKey(g, i), mutableStateListOf()) }
            }
        }

    // Wizard: steps = [grupos...] + [Quantidade]
    val stepsLabels: List<String> = remember(grupos) { grupos.mapIndexed { i, g -> gKey(g, i) } + "Quantidade" }
    var currentStep by remember { mutableStateOf(0) }

    // Preço dinâmico (somando valor/valorAd das opções)
    val adicionalUnit by remember(selecoes) {
        derivedStateOf {
            selecoes.values.flatten().sumOf { (((it.valorAd ?: it.valor) ?: 0.0)) }
        }
    }
    val precoUnit: Double = (produto.valor ?: 0.0) + adicionalUnit
    val total: Double = precoUnit * quantidade.toDouble()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        scrimColor = Color(0x99000000),
        containerColor = Color.White,
        contentColor = Color(0xFF111111)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(12.dp)
        ) {
            // Cabeçalho com foto + título
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                AsyncImage(model = fotoUrl ?: produto.foto, contentDescription = null, modifier = Modifier.size(80.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(produto.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (!produto.descricao.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp)); Text(produto.descricao!!, color = Muted)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "R$ %,.2f".format(precoUnit).replace(',', 'X').replace('.', ',').replace('X', '.'),
                        color = Orange, fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            // Corpo em 2 colunas: esquerda = etapas; direita = conteúdo do passo
            Row(Modifier.fillMaxWidth()) {
                // --- Lista de passos (esquerda) ---
                LazyColumn(
                    modifier = Modifier
                        .width(260.dp)
                        .heightIn(min = 0.dp, max = 460.dp)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stepsLabels.size) { idx ->
                        val selected = idx == currentStep
                        val label = stepsLabels[idx]
                        StepTile(
                            index = idx + 1,
                            label = label,
                            selected = selected,
                            onClick = { currentStep = idx }
                        )
                    }
                }

                // --- Conteúdo do passo (direita) ---
                Surface(
                    color = CardBg,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, Color(0x22000000)),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 200.dp, max = 460.dp)
                ) {
                    when {
                        currentStep < grupos.size -> {
                            val g = grupos[currentStep]
                            val key = gKey(g, currentStep)
                            GrupoEtapa(
                                grupo = g,
                                selecionadas = selecoes[key].orEmpty(),
                                onSelect = { opc ->
                                    val max = g.adicional_qtde_max ?: 0
                                    val lista = selecoes.getOrPut(key) { mutableStateListOf() }
                                    if (max <= 1) {
                                        lista.clear(); lista.add(opc)
                                    } else {
                                        val exists = lista.any { it.codigo == opc.codigo }
                                        if (exists) lista.removeAll { it.codigo == opc.codigo }
                                        else if (max == 0 || lista.size < max) lista.add(opc)
                                    }
                                }
                            )
                        }
                        else -> {
                            QuantidadeEtapa(
                                quantidade = quantidade,
                                onMinus = { if (quantidade > 1) quantidade-- },
                                onPlus = { quantidade++ },
                                observacao = observacao,
                                onObservacao = { observacao = it }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Subtotal  " + "R$ %,.2f".format(total).replace(',', 'X').replace('.', ',').replace('X', '.'),
                    color = Color(0xFF111111), fontWeight = FontWeight.SemiBold
                )

                if (currentStep < grupos.size) {
                    val g = grupos[currentStep]
                    val min = g.adicional_qtde_min ?: 0
                    val selectedCount = selecoes[gKey(g, currentStep)].orEmpty().size
                    val isOptional = min == 0

                    Button(
                        onClick = { currentStep++ },
                        enabled = isOptional || selectedCount >= min,
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White)
                    ) {
                        Text(if (isOptional) "PULAR" else "AVANÇAR", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = {
                            // Converte seleções para OpcaoAdicionalDto
                            val mapa: Map<String, List<OpcaoAdicionalDto>> = selecoes.mapValues { (_, lista) ->
                                lista.map { it.asOpcao() }
                            }
                            onConfirm(ProdutoEscolhido(quantidade = quantidade, escolhas = mapa))
                        },
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White)
                    ) {
                        Text("ADICIONAR AO CARRINHO", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/* ===================== Peças da UI ===================== */

@Composable
private fun StepTile(index: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Orange.copy(alpha = .18f) else Color(0xFFF0F1F5)
    val fg = if (selected) Color(0xFF111111) else Muted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .clickable { onClick() }   // precisa do import de clickable
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Surface(shape = CircleShape, color = if (selected) Orange else Color(0xFFD8DCE3)) {
            Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                Text("$index", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(label, color = fg, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun GrupoEtapa(
    grupo: GrupoAdicionalDto,
    selecionadas: List<AdicionalItemDto>,
    onSelect: (AdicionalItemDto) -> Unit
) {
    val titulo = grupo.nome.ifBlank { "Adicionais" }
    val min = grupo.adicional_qtde_min ?: 0
    val max = grupo.adicional_qtde_max ?: 0

    Column(
        Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(titulo, fontWeight = FontWeight.Bold)
        val regra = buildString {
            if (min > 0 || max > 0) {
                append("Você pode escolher ")
                if (min > 0 && max > 0) append("de $min a $max itens")
                else if (min > 0) append("no mínimo $min item(s)")
                else append("até $max itens")
            }
        }
        if (regra.isNotBlank()) Text(regra, color = Muted, style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(6.dp))
        grupo.adicionais.forEach { opc ->
            val checked = selecionadas.any { it.codigo == opc.codigo }
            val nome = opc.nome.ifBlank { "Opção" }
            val preco = ((opc.valorAd ?: opc.valor) ?: 0.0)
            val tituloOpc = if (preco > 0.0) "$nome    (+ ${formatMoneyUi(preco)})" else nome

            if (max <= 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = checked,
                        onClick = { onSelect(opc) },
                        colors = RadioButtonDefaults.colors(selectedColor = Orange)
                    )
                    Text(tituloOpc, modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onSelect(opc) },
                        colors = CheckboxDefaults.colors(checkedColor = Orange)
                    )
                    Text(tituloOpc, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun QuantidadeEtapa(
    quantidade: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    observacao: String,
    onObservacao: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Text("Quantidade", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuantityStepper(valor = quantidade, onMinus = onMinus, onPlus = onPlus)
            Spacer(Modifier.width(16.dp))
            Text("Você pode escrever uma observação", color = Muted)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = observacao,
            onValueChange = onObservacao,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ex.: sem gelo, ponto da carne, etc.") }
        )
    }
}

@Composable
private fun QuantityStepper(valor: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Surface(shape = CircleShape, color = Color(0xFFE9ECF2), border = BorderStroke(1.dp, Color(0xFFD6DADF))) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(40.dp)) {
            IconButton(onClick = onMinus, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = null, tint = Color(0xFF111111))
            }
            Text("$valor", modifier = Modifier.widthIn(min = 28.dp))
            IconButton(onClick = onPlus, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF111111))
            }
        }
    }
}

private fun formatMoneyUi(v: Double?): String {
    val x = v ?: 0.0
    return "R$ " + "%,.2f".format(x).replace(',', 'X').replace('.', ',').replace('X', '.')
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
