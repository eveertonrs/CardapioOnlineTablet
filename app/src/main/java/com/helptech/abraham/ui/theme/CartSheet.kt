package com.helptech.abraham.ui.theme


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helptech.abraham.data.remote.ProdutoDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(
    itens: List<Pair<ProdutoDto, Int>>, // produto + quantidade
    onAdd: (ProdutoDto) -> Unit,
    onRemove: (ProdutoDto) -> Unit,
    onClose: () -> Unit,
    onCheckout: () -> Unit
) {
    val total = itens.sumOf { (p, q) -> (p.valor ?: 0.0) * q }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Color(0xFF1F2025),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF888888)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Seu carrinho", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(itens, key = { it.first.codigo }) { (p, q) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF24262B), shape = MaterialTheme.shapes.medium)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.nome, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Unitário: ${formatMoney(p.valor)}", color = Color(0xFF9AA0A6))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { onRemove(p) }) { Text("−") }
                            Spacer(Modifier.width(8.dp))
                            Text("$q", color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = { onAdd(p) }) { Text("+") }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(formatMoney((p.valor ?: 0.0) * q), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total", color = Color.White, fontWeight = FontWeight.Bold)
                Text(formatMoney(total), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f)
                ) { Text("Continuar comprando") }
                Button(
                    onClick = onCheckout,
                    modifier = Modifier.weight(1f)
                ) { Text("Fechar pedido") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
