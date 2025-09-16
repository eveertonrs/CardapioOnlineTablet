@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.helptech.abraham.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helptech.abraham.data.remote.ProdutoDto
import java.text.NumberFormat
import java.util.Locale

/* ===== Paleta consistente com o menu ===== */
private val PanelBg    = Color(0xFF1A1B1F)
private val CardBg     = Color(0xFF24262B)
private val DividerClr = Color(0xFF2E3036)
private val Muted      = Color(0xFF9AA0A6)
private val Accent     = Color(0xFF7C4DFF) // roxo do CTA

/* =============================================================================
   TELA FULL-SCREEN DE CARRINHO
   ========================================================================== */
@Composable
fun CartScreen(
    itens: List<Pair<ProdutoDto, Int>>,
    onAdd: (ProdutoDto) -> Unit,
    onRemove: (ProdutoDto) -> Unit,
    onBack: () -> Unit,
    onCheckout: () -> Unit
) {
    val total = remember(itens) { itens.sumOf { (p, q) -> (p.valor ?: 0.0) * q } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PanelBg)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Seu carrinho",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Voltar",
                color = Color(0xFFBDBDBD),
                modifier = Modifier.clickable { onBack() }
            )
        }
        HorizontalDivider(color = DividerClr)

        // Lista
        if (itens.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Seu carrinho está vazio.", color = Muted)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(itens, key = { it.first.codigo }) { (p, q) ->
                    CartRow(
                        produto = p,
                        quantidade = q,
                        onAdd = { onAdd(p) },
                        onRemove = { onRemove(p) }
                    )
                }
            }
        }

        // Footer com total + CTA
        HorizontalDivider(color = DividerClr)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total", color = Muted)
                Text(
                    text = formatMoneyBr(total),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }

            Button(
                onClick = onCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Confirmar pedido", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/* =============================================================================
   VERSÃO OPCIONAL EM BOTTOM SHEET (mantida para compatibilidade)
   ========================================================================== */
@Composable
fun CartBottomSheet(
    itens: List<Pair<ProdutoDto, Int>>,
    onAdd: (ProdutoDto) -> Unit,
    onRemove: (ProdutoDto) -> Unit,
    onClose: () -> Unit,
    onCheckout: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = PanelBg,
        tonalElevation = 0.dp
    ) {
        // Reaproveita a mesma UI da tela, com header próprio do sheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Seu carrinho",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = Color.White)
                }
            }
            HorizontalDivider(color = DividerClr)

            // mesmo conteúdo da CartScreen, só que dentro do sheet
            val total = remember(itens) { itens.sumOf { (p, q) -> (p.valor ?: 0.0) * q } }

            if (itens.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp)
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Seu carrinho está vazio.", color = Muted) }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(itens, key = { it.first.codigo }) { (p, q) ->
                        CartRow(
                            produto = p,
                            quantidade = q,
                            onAdd = { onAdd(p) },
                            onRemove = { onRemove(p) }
                        )
                    }
                }
            }

            HorizontalDivider(color = DividerClr)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total", color = Muted)
                    Text(
                        text = formatMoneyBr(total),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
                Button(
                    onClick = onCheckout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = MaterialTheme.shapes.large
                ) { Text("Confirmar pedido", color = Color.White) }
            }
        }
    }
}

/* ===================== Itens e Stepper ===================== */

@Composable
private fun CartRow(
    produto: ProdutoDto,
    quantidade: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(CardBg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = produto.nome,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Unitário: ${formatMoneyBr(produto.valor)}",
                color = Muted,
                fontSize = 12.sp
            )
        }

        Stepper(
            quantidade = quantidade,
            onMinus = onRemove,
            onPlus = onAdd
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = formatMoneyBr((produto.valor ?: 0.0) * quantidade),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Stepper(
    quantidade: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Surface(
        color = Color(0xFF2A2C31),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = "Diminuir",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onMinus() }
                    .padding(4.dp)
            )
            Text(
                text = quantidade.toString(),
                color = Color.White,
                modifier = Modifier.widthIn(min = 28.dp),
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Aumentar",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onPlus() }
                    .padding(4.dp)
            )
        }
    }
}

/* ===================== Helpers ===================== */

private val moneyBr: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
}

/** Formata moeda em pt-BR (local ao arquivo para evitar conflitos). */
private fun formatMoneyBr(valor: Double?): String = moneyBr.format(valor ?: 0.0)
