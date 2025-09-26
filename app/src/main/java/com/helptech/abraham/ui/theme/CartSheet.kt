//@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
//
//package com.helptech.abraham.ui.theme
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.Close
//import androidx.compose.material.icons.filled.Remove
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.helptech.abraham.data.remote.ProdutoDto
//import java.text.NumberFormat
//import java.util.Locale
//
///* ===== Paleta ===== */
//private val PanelBg    = Color(0xFF1A1B1F)
//private val CardBg     = Color(0xFF24262B)
//private val DividerClr = Color(0xFF2E3036)
//private val Muted      = Color(0xFF9AA0A6)
//private val Accent     = Color(0xFF7C4DFF)
//
///* ===================== MODELOS (públicos) ===================== */
//data class AdicionalEscolhido(
//    val id: Int,
//    val nome: String,
//    val valor: Double = 0.0,
//    val grupoNome: String? = null,
//    val isRemocao: Boolean = false,
//    val isSaborPizza: Boolean = false
//)
//
//data class CartItem(
//    val produto: ProdutoDto,
//    val quantidade: Int = 1,
//    val adicionais: List<AdicionalEscolhido> = emptyList(),
//    val observacao: String? = null
//) {
//    val totalAdicUnit: Double = adicionais.sumOf { it.valor.coerceAtLeast(0.0) }
//    val precoUnit: Double = (produto.valor ?: 0.0) + totalAdicUnit
//    val precoLinha: Double = precoUnit * quantidade
//
//    fun subtitle(): String {
//        val sabores = adicionais.filter { it.isSaborPizza }.map { it.nome }
//        val extras  = adicionais.filter { !it.isSaborPizza && !it.isRemocao }.map { a ->
//            if (a.valor > 0.0) "+ ${a.nome} (+ ${formatMoneyBr(a.valor)})" else "+ ${a.nome}"
//        }
//        val remocoes = adicionais.filter { it.isRemocao }.map { "sem ${it.nome}" }
//        val partes = buildList {
//            if (sabores.isNotEmpty()) add(sabores.joinToString(" / "))
//            addAll(extras); addAll(remocoes)
//            if (!observacao.isNullOrBlank()) add(observacao!!)
//        }
//        return partes.joinToString("  /  ")
//    }
//
//    /** chave única pra não agrupar itens com opcionais diferentes */
//    fun signature(): String {
//        val addKey = adicionais.sortedBy { it.id }
//            .joinToString("|") { "${it.id}:${it.valor}:${it.isRemocao}:${it.isSaborPizza}" }
//        return "${produto.codigo}|$addKey|${observacao.orEmpty()}"
//    }
//}
//
///* ===================== TELA FULL ===================== */
//@Composable
//fun CartScreen(
//    itens: List<CartItem>,
//    onInc: (CartItem) -> Unit,
//    onDec: (CartItem) -> Unit,
//    onBack: () -> Unit,
//    onCheckout: () -> Unit
//) {
//    val total = remember(itens) { itens.sumOf { it.precoLinha } }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(PanelBg)
//            .windowInsetsPadding(WindowInsets.systemBars)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 20.dp, vertical = 12.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text("Seu carrinho", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
//            Text("Voltar", color = Color(0xFFBDBDBD), modifier = Modifier.clickable { onBack() })
//        }
//        HorizontalDivider(color = DividerClr)
//
//        if (itens.isEmpty()) {
//            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
//                Text("Seu carrinho está vazio.", color = Muted)
//            }
//        } else {
//            LazyColumn(
//                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
//                verticalArrangement = Arrangement.spacedBy(10.dp)
//            ) {
//                items(itens, key = { it.signature() }) { item ->
//                    CartRow(item, onInc = { onInc(item) }, onDec = { onDec(item) })
//                }
//            }
//        }
//
//        HorizontalDivider(color = DividerClr)
//        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
//            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
//                Text("Total", color = Muted)
//                Text(formatMoneyBr(total), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
//            }
//            Button(
//                onClick = onCheckout,
//                modifier = Modifier.fillMaxWidth().height(48.dp),
//                colors = ButtonDefaults.buttonColors(containerColor = Accent),
//                shape = MaterialTheme.shapes.large
//            ) { Text("Confirmar pedido", color = Color.White, fontWeight = FontWeight.SemiBold) }
//        }
//    }
//}
//
///* ===================== SHEET (opcional) ===================== */
//@Composable
//fun CartBottomSheet(
//    itens: List<CartItem>,
//    onInc: (CartItem) -> Unit,
//    onDec: (CartItem) -> Unit,
//    onClose: () -> Unit,
//    onCheckout: () -> Unit
//) {
//    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
//    ModalBottomSheet(
//        onDismissRequest = onClose,
//        sheetState = sheetState,
//        dragHandle = { BottomSheetDefaults.DragHandle() },
//        containerColor = PanelBg,
//        tonalElevation = 0.dp
//    ) {
//        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
//            Row(
//                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text("Seu carrinho", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
//                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, null, tint = Color.White) }
//            }
//            HorizontalDivider(color = DividerClr)
//
//            val total = remember(itens) { itens.sumOf { it.precoLinha } }
//
//            if (itens.isEmpty()) {
//                Box(Modifier.fillMaxWidth().heightIn(min = 180.dp).padding(28.dp), contentAlignment = Alignment.Center) {
//                    Text("Seu carrinho está vazio.", color = Muted)
//                }
//            } else {
//                LazyColumn(
//                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(horizontal = 16.dp, vertical = 8.dp),
//                    verticalArrangement = Arrangement.spacedBy(10.dp)
//                ) {
//                    items(itens, key = { it.signature() }) { item ->
//                        CartRow(item, onInc = { onInc(item) }, onDec = { onDec(item) })
//                    }
//                }
//            }
//
//            HorizontalDivider(color = DividerClr)
//            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
//                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
//                    Text("Total", color = Muted)
//                    Text(formatMoneyBr(total), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
//                }
//                Button(
//                    onClick = onCheckout,
//                    modifier = Modifier.fillMaxWidth().height(48.dp),
//                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
//                    shape = MaterialTheme.shapes.large
//                ) { Text("Confirmar pedido", color = Color.White) }
//            }
//        }
//    }
//}
//
///* ===================== Linha + Stepper ===================== */
//@Composable
//private fun CartRow(item: CartItem, onInc: () -> Unit, onDec: () -> Unit) {
//    Row(
//        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(CardBg).padding(horizontal = 14.dp, vertical = 12.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Column(Modifier.weight(1f)) {
//            Text(item.produto.nome, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
//            val sub = item.subtitle()
//            if (sub.isNotBlank()) {
//                Spacer(Modifier.height(2.dp))
//                Text(sub, color = Muted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
//            }
//            Spacer(Modifier.height(4.dp))
//            Text("Unitário: ${formatMoneyBr(item.precoUnit)}", color = Muted, fontSize = 12.sp)
//        }
//        Stepper(quantidade = item.quantidade, onMinus = onDec, onPlus = onInc)
//        Spacer(Modifier.width(12.dp))
//        Text(formatMoneyBr(item.precoLinha), color = Color.White, fontWeight = FontWeight.Bold)
//    }
//}
//
//@Composable
//private fun Stepper(quantidade: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
//    Surface(color = Color(0xFF2A2C31), shape = MaterialTheme.shapes.small) {
//        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
//            Icon(Icons.Filled.Remove, contentDescription = "Diminuir", tint = Color.White,
//                modifier = Modifier.size(28.dp).clickable { onMinus() }.padding(4.dp))
//            Text(quantidade.toString(), color = Color.White, modifier = Modifier.widthIn(min = 28.dp), fontWeight = FontWeight.Medium)
//            Icon(Icons.Filled.Add, contentDescription = "Aumentar", tint = Color.White,
//                modifier = Modifier.size(28.dp).clickable { onPlus() }.padding(4.dp))
//        }
//    }
//}
//
///* ===================== Helpers ===================== */
//private val moneyBr: NumberFormat by lazy {
//    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).apply {
//        minimumFractionDigits = 2; maximumFractionDigits = 2
//    }
//}
//private fun formatMoneyBr(valor: Double?): String = moneyBr.format(valor ?: 0.0)
