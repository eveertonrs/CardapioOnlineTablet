package com.helptech.abraham.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.helptech.abraham.data.remote.AdicionalItemDto
import com.helptech.abraham.data.remote.GrupoAdicionalDto
import com.helptech.abraham.data.remote.OpcaoAdicionalDto
import com.helptech.abraham.data.remote.ProdutoDto
import com.helptech.abraham.data.remote.preco
import com.helptech.abraham.network.buscarFotoPrincipal
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp



/** Retorno do sheet: quantidade e as opções escolhidas por grupo. */
data class ProdutoEscolhido(
    val quantidade: Int,
    val escolhas: Map<String, List<OpcaoAdicionalDto>>
)

/* Paleta local */
private val Orange = Color(0xFFF57C00)
private val CardBg  = Color(0xFFF5F6F8)
private val Muted   = Color(0xFF5F6368)

@Composable
fun ProductDetailSheet(
    produto: ProdutoDto,
    grupos: List<GrupoAdicionalDto>,
    onDismiss: () -> Unit,
    onConfirm: (ProdutoEscolhido) -> Unit
) {
    // ====== Estado ======
    var quantidade by remember { mutableStateOf(1) }
    var observacao by remember { mutableStateOf("") }
    var fotoUrl by remember(produto.codigo) { mutableStateOf<String?>(null) }
    LaunchedEffect(produto.codigo) { fotoUrl = try { buscarFotoPrincipal(produto.codigo) } catch (_: Exception) { null } }

    // chave dos grupos
    fun gKey(g: GrupoAdicionalDto, idx: Int) = g.nome.takeIf { it.isNotBlank() } ?: "Grupo ${idx + 1}"

    // Seleções por grupo (lista de AdicionalItemDto; duplicado = quantidade)
    val selecoes: MutableMap<String, SnapshotStateList<AdicionalItemDto>> =
        remember(grupos) {
            mutableStateMapOf<String, SnapshotStateList<AdicionalItemDto>>().apply {
                grupos.forEachIndexed { i, g -> put(gKey(g, i), mutableStateListOf()) }
            }
        }

    // Wizard: steps = [grupos...] + [Quantidade]
    val stepsLabels: List<String> = remember(grupos) { grupos.mapIndexed { i, g -> gKey(g, i) } + "Quantidade" }
    var currentStep by remember { mutableStateOf(0) }

    // Preço dinâmico
    val adicionalUnit by remember(selecoes) {
        derivedStateOf { selecoes.values.flatten().sumOf { it.preco } }
    }
    val precoUnit = (produto.valor ?: 0.0) + adicionalUnit
    val total = precoUnit * quantidade

    // ====== FULL-SCREEN DIALOG ======
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
            color = Color.White,
            contentColor = Color(0xFF111111),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
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
                    TextButton(onClick = onDismiss) { Text("Fechar") }
                }

                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                // Corpo em 2 colunas: esquerda = etapas; direita = conteúdo do passo
                Row(Modifier.fillMaxSize().weight(1f)) {
                    // --- Lista de passos (esquerda) ---
                    LazyColumn(
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
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
                            .fillMaxHeight()
                    ) {
                        when {
                            currentStep < grupos.size -> {
                                val g = grupos[currentStep]
                                val key = gKey(g, currentStep)
                                GrupoEtapa(
                                    grupo = g,
                                    selecionadas = selecoes[key].orEmpty(),
                                    onChange = { opc, novoValor ->
                                        val lista = selecoes.getOrPut(key) { mutableStateListOf() }
                                        // total permitido no grupo (0 = sem limite)
                                        val grupoMax = g.adicional_qtde_max ?: 0
                                        val totalAtual = lista.size
                                        val countAtual = lista.count { it.codigo == opc.codigo }

                                        val limiteOpc = (opc.limite_adicao ?: 1).coerceAtLeast(1)
                                        val novoCount = novoValor.coerceIn(0, limiteOpc)

                                        // quanto cabe ainda pelo limite do grupo
                                        val espacoGrupo = if (grupoMax > 0) (grupoMax - (totalAtual - countAtual)).coerceAtLeast(0) else Int.MAX_VALUE
                                        val countAplicado = novoCount.coerceAtMost(espacoGrupo)

                                        // aplica: remove todos daquela opção e reinsere countAplicado vezes
                                        lista.removeAll { it.codigo == opc.codigo }
                                        repeat(countAplicado) { lista.add(opc) }
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
                                // Converte seleções para OpcaoAdicionalDto (duplicadas = quantidade da opção)
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
            .clickable { onClick() }
            .background(bg)
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
    onChange: (AdicionalItemDto, Int) -> Unit
) {
    val titulo = grupo.nome.ifBlank { "Adicionais" }
    val min = grupo.adicional_qtde_min ?: 0
    val maxGrupo = grupo.adicional_qtde_max ?: 0

    // helpers
    fun countOf(opc: AdicionalItemDto) = selecionadas.count { it.codigo == opc.codigo }
    fun totalGrupo() = selecionadas.size
    fun podeAdicionar(opc: AdicionalItemDto): Boolean {
        val limiteOpc = (opc.limite_adicao ?: 1).coerceAtLeast(1)
        val atualOpc = countOf(opc)
        val roomOpc = atualOpc < limiteOpc
        val roomGrupo = if (maxGrupo > 0) totalGrupo() < maxGrupo else true
        return roomOpc && roomGrupo
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Cabeçalho
        Text(titulo, fontWeight = FontWeight.Bold)
        val regra = buildString {
            val sItem = { q: Int -> if (q == 1) "item" else "itens" }
            when {
                min > 0 && maxGrupo > 0 -> append("Você pode escolher de $min a $maxGrupo ${sItem(maxGrupo)}")
                min > 0                  -> append("Você deve escolher no mínimo $min ${sItem(min)}")
                maxGrupo > 0             -> append("Você pode escolher até $maxGrupo ${sItem(maxGrupo)}")
            }
        }
        if (regra.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(regra, color = Muted, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))

        // LISTA COM SCROLL (soluciona overflow)
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 6.dp)
        ) {
            items(items = grupo.adicionais, key = { it.codigo }) { opc ->
                val nome = opc.nome.ifBlank { "Opção" }
                val preco = opc.preco
                val precoTxt = if (preco > 0.0) "  (+ ${formatMoneyUi(preco)})" else ""
                val limiteOpc = (opc.limite_adicao ?: 1).coerceAtLeast(1)
                val atual = countOf(opc)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (limiteOpc <= 1) {
                        // 0/1 → Checkbox
                        Checkbox(
                            checked = atual > 0,
                            onCheckedChange = { checked ->
                                onChange(opc, if (checked) 1 else 0)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Orange)
                        )
                    } else {
                        // >1 → Stepper com +/-
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE9ECF2),
                            border = BorderStroke(1.dp, Color(0xFFD6DADF))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(32.dp)) {
                                IconButton(
                                    onClick = { if (atual > 0) onChange(opc, atual - 1) },
                                    modifier = Modifier.size(32.dp),
                                    enabled = atual > 0
                                ) { Icon(Icons.Filled.Remove, contentDescription = null, tint = Color(0xFF111111)) }
                                Text("$atual", modifier = Modifier.widthIn(min = 24.dp))
                                IconButton(
                                    onClick = { if (podeAdicionar(opc)) onChange(opc, atual + 1) },
                                    modifier = Modifier.size(32.dp),
                                    enabled = podeAdicionar(opc)
                                ) { Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF111111)) }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(nome + precoTxt)
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
