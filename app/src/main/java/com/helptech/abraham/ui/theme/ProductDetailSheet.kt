package com.helptech.abraham.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.helptech.abraham.data.remote.AdicionalItemDto
import com.helptech.abraham.data.remote.GrupoAdicionalDto
import com.helptech.abraham.data.remote.OpcaoAdicionalDto
import com.helptech.abraham.data.remote.ProdutoDto
import com.helptech.abraham.data.remote.preco
import com.helptech.abraham.network.buscarFotoPrincipal
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput



/** Retorno do sheet: quantidade e as opções escolhidas por grupo. */
data class ProdutoEscolhido(
    val quantidade: Int,
    val escolhas: Map<String, List<OpcaoAdicionalDto>>
)

/* Paleta local */
private val Orange = Color(0xFFF57C00)
private val SheetBg = Color(0xFFFFFFFF)
private val CardBg  = Color(0xFFF5F6F8)
private val Muted   = Color(0xFF5F6368)
private val DividerClr = Color(0x22000000)

// Função de formatação de dinheiro (para ser acessível aqui)
private fun formatMoneyUi(valor: Double?): String {
    val v = valor ?: 0.0
    return "R$ " + "%,.2f".format(v).replace(',', 'X').replace('.', ',').replace('X', '.')
}

// Converte AdicionalItemDto para OpcaoAdicionalDto
fun AdicionalItemDto.asOpcao(): OpcaoAdicionalDto {
    return OpcaoAdicionalDto(
        codigo = this.codigo,
        nome = this.nome,
        valor = this.valor,
        valorAd = this.valorAd,
        limite_adicao = this.limite_adicao
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailSheet(
    produto: ProdutoDto,
    grupos: List<GrupoAdicionalDto>,
    onDismiss: () -> Unit,
    onConfirm: (ProdutoEscolhido) -> Unit
) {
    var quantidade by remember { mutableStateOf(1) }
    var observacao by remember { mutableStateOf("") }
    var fotoUrl by remember(produto.codigo) { mutableStateOf<String?>(null) }
    LaunchedEffect(produto.codigo) { fotoUrl = try { buscarFotoPrincipal(produto.codigo) } catch (_: Exception) { null } }

    fun gKey(g: GrupoAdicionalDto, idx: Int) = g.nome.takeIf { it.isNotBlank() } ?: "Grupo ${idx + 1}"

    val selecoes: MutableMap<String, SnapshotStateList<AdicionalItemDto>> = remember(grupos) {
        mutableStateMapOf<String, SnapshotStateList<AdicionalItemDto>>().apply {
            grupos.forEachIndexed { i, g -> put(gKey(g, i), mutableStateListOf()) }
        }
    }

    val stepsLabels: List<String> = remember(grupos) { grupos.mapIndexed { i, g -> gKey(g, i) } + "Quantidade" }
    var currentStep by remember { mutableStateOf(0) }

    var showValidationError by remember { mutableStateOf(false) }
    var validationErrorMessage by remember { mutableStateOf("") }

    val attemptNavigationTo: (Int) -> Unit = { targetStep ->
        if (currentStep < grupos.size) {
            val g = grupos[currentStep]
            val min = g.adicional_qtde_min ?: 0
            val selectedCount = selecoes[gKey(g, currentStep)].orEmpty().size

            if (min > 0 && selectedCount < min) {
                validationErrorMessage = "É obrigatório escolher no mínimo $min ${if (min == 1) "item" else "itens"}."
                showValidationError = true
            } else {
                currentStep = targetStep
            }
        } else {
            currentStep = targetStep
        }
    }

    val allRequiredStepsCompleted by remember(selecoes) {
        derivedStateOf {
            grupos.indices.all { index ->
                val grupo = grupos[index]
                val min = grupo.adicional_qtde_min ?: 0
                if (min > 0) {
                    val selectedCount = selecoes[gKey(grupo, index)].orEmpty().size
                    selectedCount >= min
                } else {
                    true
                }
            }
        }
    }

    val adicionalUnit by remember(selecoes) {
        derivedStateOf { selecoes.values.flatten().sumOf { it.preco } }
    }
    val precoUnit = (produto.valor ?: 0.0) + adicionalUnit
    val total = precoUnit * quantidade

    if (showValidationError) {
        AlertDialog(
            onDismissRequest = { showValidationError = false },
            title = { Text("Atenção") },
            text = { Text(validationErrorMessage) },
            confirmButton = {
                Button(onClick = { showValidationError = false }) {
                    Text("OK")
                }
            }
        )
    }

    val focusManager = LocalFocusManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            color = SheetBg,
            contentColor = Color(0xFF111111),
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(horizontal = 4.dp)) {
                    AsyncImage(
                        model = fotoUrl ?: produto.foto,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(produto.nome, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (!produto.descricao.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(produto.descricao!!, color = Muted, style = MaterialTheme.typography.bodyLarge)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(formatMoneyUi(precoUnit), color = Orange, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, "Fechar", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxSize().weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.width(260.dp).fillMaxHeight().padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stepsLabels.size) { idx ->
                            StepTile(index = idx + 1, label = stepsLabels[idx], selected = idx == currentStep) {
                                attemptNavigationTo(idx)
                            }
                        }
                    }

                    Surface(
                        color = CardBg,
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, DividerClr),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        if (currentStep < grupos.size) {
                            val g = grupos[currentStep]
                            val key = gKey(g, currentStep)
                            GrupoEtapa(
                                grupo = g,
                                selecionadas = selecoes[key].orEmpty(),
                                onChange = { opc: AdicionalItemDto, novoValor: Int ->
                                    val lista = selecoes.getOrPut(key) { mutableStateListOf() }
                                    val grupoMax = g.adicional_qtde_max ?: 0
                                    val totalAtual = lista.size
                                    val countAtual = lista.count { it.codigo == opc.codigo }
                                    val limiteOpc = (opc.limite_adicao ?: 1).coerceAtLeast(1)
                                    val novoCount = novoValor.coerceIn(0, limiteOpc)
                                    val espacoGrupo = if (grupoMax > 0) (grupoMax - (totalAtual - countAtual)).coerceAtLeast(0) else Int.MAX_VALUE
                                    val countAplicado = novoCount.coerceAtMost(espacoGrupo)
                                    lista.removeAll { it.codigo == opc.codigo }
                                    repeat(countAplicado) { lista.add(opc) }
                                }
                            )
                        } else {
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

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Subtotal  " + formatMoneyUi(total),
                        color = Color(0xFF111111),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )

                    if (currentStep < grupos.size) {
                        val g = grupos[currentStep]
                        val min = g.adicional_qtde_min ?: 0
                        val isOptional = min == 0

                        Button(
                            onClick = { attemptNavigationTo(currentStep + 1) },
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text(if (isOptional) "PULAR" else "AVANÇAR", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                val mapa: Map<String, List<OpcaoAdicionalDto>> = selecoes.mapValues { entry ->
                                    entry.value.map { it.asOpcao() }
                                }
                                onConfirm(ProdutoEscolhido(quantidade = quantidade, escolhas = mapa))
                            },
                            enabled = allRequiredStepsCompleted,
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("ADICIONAR AO CARRINHO", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepTile(index: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Orange.copy(alpha = .18f) else Color(0xFFF0F1F5)
    val fg = if (selected) Color(0xFF111111) else Muted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(bg, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$index.", color = if (selected) Orange else fg, fontWeight = if (selected) FontWeight.Black else FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(label, color = fg, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun GrupoEtapa(
    grupo: GrupoAdicionalDto,
    selecionadas: List<AdicionalItemDto>,
    onChange: (AdicionalItemDto, Int) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(grupo.nome, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        grupo.adicional_qtde_min?.takeIf { it > 0 }?.let {
            val max = grupo.adicional_qtde_max?.takeIf { it > 0 }
            val rule = when {
                max != null && max > it -> "Escolha de $it a $max itens."
                max != null && max == it -> "Escolha $it itens."
                else -> "Escolha no mínimo $it ${if (it == 1) "item" else "itens"}."
            }
            Spacer(Modifier.height(4.dp))
            Text(rule, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(grupo.adicionais) { item ->
                val count = selecionadas.count { it.codigo == item.codigo }
                AdicionalRow(
                    item = item,
                    count = count,
                    onCountChange = { novo -> onChange(item, novo) }
                )
            }
        }
    }
}

@Composable
fun AdicionalRow(
    item: AdicionalItemDto,
    count: Int,
    onCountChange: (Int) -> Unit
) {
    val max = (item.limite_adicao ?: 1).coerceAtLeast(1)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (count > 0) Orange.copy(alpha = 0.08f) else Color.White,
        border = BorderStroke(1.dp, if (count > 0) Orange.copy(alpha = 0.5f) else DividerClr)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.nome, fontWeight = FontWeight.SemiBold)
                if (item.preco > 0) {
                    Text(formatMoneyUi(item.preco), color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onCountChange(count - 1) }, enabled = count > 0) {
                    Icon(Icons.Default.Remove, "Remover", tint = if (count > 0) Color.Black else Color.Gray)
                }
                Text("$count", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                IconButton(onClick = { onCountChange(count + 1) }, enabled = count < max) {
                    Icon(Icons.Default.Add, "Adicionar", tint = if (count < max) Color.Black else Color.Gray)
                }
            }
        }
    }
}

@Composable
fun QuantidadeEtapa(
    quantidade: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    observacao: String,
    onObservacao: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Quantidade", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onMinus, modifier = Modifier.size(64.dp), shape = CircleShape) {
                Text("-", fontSize = 24.sp)
            }
            Text(
                "$quantidade",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            OutlinedButton(onClick = onPlus, modifier = Modifier.size(64.dp), shape = CircleShape) {
                Text("+", fontSize = 24.sp)
            }
        }
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = observacao,
            onValueChange = onObservacao,
            label = { Text("Observações do item") },
            placeholder = { Text("Ex: Sem cebola, ponto da carne, etc.") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            maxLines = 4,
            trailingIcon = {
                if (observacao.isNotEmpty()) { // Mostra o X apenas se houver texto
                    IconButton(onClick = {
                        onObservacao("") // Limpa o texto
                        focusManager.clearFocus() // E fecha o teclado
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpar e fechar teclado"
                        )
                    }
                }
            }
        )
    }
}

