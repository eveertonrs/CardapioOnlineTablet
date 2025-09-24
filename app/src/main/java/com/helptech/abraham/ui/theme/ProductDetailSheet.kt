package com.helptech.abraham.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.helptech.abraham.data.remote.GrupoAdicionalDto
import com.helptech.abraham.data.remote.OpcaoAdicionalDto
import com.helptech.abraham.data.remote.ProdutoDto
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

    // abre já expandido pra não cortar conteúdo
    LaunchedEffect(Unit) { scope.launch { sheetState.expand() } }

    var quantidade by remember { mutableStateOf(1) }

    fun keyOf(g: GrupoAdicionalDto, idx: Int): String =
        g.grupo?.takeIf { it.isNotBlank() } ?: "Adicionais #$idx"

    // Seleções por grupo
    val escolhas: MutableMap<String, SnapshotStateList<OpcaoAdicionalDto>> =
        remember(grupos) {
            mutableStateMapOf<String, SnapshotStateList<OpcaoAdicionalDto>>().apply {
                grupos.forEachIndexed { i, g -> put(keyOf(g, i), mutableStateListOf()) }
            }
        }

    var fotoUrl by remember(produto.codigo) { mutableStateOf<String?>(null) }
    LaunchedEffect(produto.codigo) {
        fotoUrl = try { buscarFotoPrincipal(produto.codigo) } catch (_: Exception) { null }
    }

    val adicionalUnit by remember(grupos, escolhas) {
        derivedStateOf { escolhas.values.flatten().sumOf { it.valor ?: it.valorAd ?: 0.0 } }
    }
    val precoUnit = (produto.valor ?: 0.0) + adicionalUnit
    val total = precoUnit * quantidade

    val podeConfirmar by remember(grupos, escolhas) {
        derivedStateOf {
            grupos.withIndex().all { (idx, g) ->
                if (g.obrigatorio.equals("S", true))
                    escolhas[keyOf(g, idx)].orEmpty().isNotEmpty()
                else true
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        // >>> Deixa o sheet claro e o scrim mais forte
        containerColor = Color.White,
        contentColor = Color(0xFF111111),
        scrimColor = Color(0x99000000)
    ) {
        // padding para não colar nas bordas e não ficar atrás da barra de navegação
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
                        text = produto.nome ?: "",
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
                // limita a altura pra nunca ficar escondido atrás do botão
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp) // ajuste fino se precisar
                ) {
                    items(grupos, key = { (it.grupo ?: "Adicionais") + "|" + it.hashCode() }) { g ->
                        val idx = grupos.indexOf(g)
                        val gKey = keyOf(g, idx)

                        GrupoAdicionalCard(
                            grupo = g,
                            selecionadas = escolhas[gKey].orEmpty(),
                            onSelect = { opc ->
                                val max = g.max ?: 0
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
                            },
                            obrigatorio = g.obrigatorio.equals("S", true),
                            max = g.max ?: 0
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
                    onConfirm(
                        ProdutoEscolhido(
                            quantidade = quantidade,
                            escolhas = escolhas.mapValues { it.value.toList() }
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
    selecionadas: List<OpcaoAdicionalDto>,
    onSelect: (OpcaoAdicionalDto) -> Unit,
    obrigatorio: Boolean,
    max: Int
) {
    val opcoes = grupo.opcoes ?: emptyList()
    if (opcoes.isEmpty()) return

    val tituloGrupo = grupo.grupo?.takeIf { it.isNotBlank() } ?: "Adicionais"

    // Cartão claro para legibilidade
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
                if (max > 0) {
                    if (isNotEmpty()) append(" • "); append("Até $max")
                }
            }
            Text(
                text = tituloGrupo,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            if (sub.isNotEmpty()) {
                Text(
                    text = sub,
                    color = Color(0xFF5F6368),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))

            opcoes.forEach { op ->
                val checked = selecionadas.any { it.codigo == op.codigo }
                val nomeOpc = op.nome?.ifBlank { "Opção" } ?: "Opção"
                val precoOpc = op.valor ?: op.valorAd
                val titulo = if (precoOpc != null) {
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
