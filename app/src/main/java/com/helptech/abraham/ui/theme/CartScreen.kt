package com.helptech.abraham.ui.theme

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.helptech.abraham.data.remote.CartItem
import com.helptech.abraham.data.remote.lineTotal
import com.helptech.abraham.data.remote.unitPrice
import com.helptech.abraham.network.buscarFotoPrincipal

private val CartCardBg    = Color(0xFF24262B)
private val CartCardAltBg = Color(0xFF2A2D33)
private val CartMuted     = Color(0xFFB8BEC6)


@Composable
fun CartScreen(
    cartItems: List<CartItem>,
    onClose: () -> Unit,
    onRemove: (CartItem) -> Unit,
    onAdd: (CartItem) -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Carrinho", style = MaterialTheme.typography.headlineMedium)
        // Aqui virá a lista de itens
        Column(modifier = Modifier.weight(1f)) {
            if (cartItems.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Seu carrinho está vazio")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cartItems, key = { it.key }) { item ->
                        CartItemRow(
                            item = item,
                            onAdd = { onAdd(item) },
                            onRemove = { onRemove(item) }
                        )
                    }
                }
            }
        }

        Button(onClick = onConfirm) {
            Text("Confirmar Pedido")
        }
        Button(onClick = onClose) {
            Text("Voltar ao menu")
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val produto = item.produto

    Surface(
        shape = MaterialTheme.shapes.large,
        color = CartCardBg,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Imagem ---
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(CartCardAltBg),
                contentAlignment = Alignment.Center
            ) {
                // 1) tenta buscar foto principal (URL externa)
                var fotoUrl by remember(produto.codigo) { mutableStateOf<String?>(null) }
                LaunchedEffect(produto.codigo) {
                    fotoUrl = try { buscarFotoPrincipal(produto.codigo) } catch (_: Exception) { null }
                }

                // 2) foto original do produto (URL ou base64)
                val rawPhoto = produto.foto
                val isUrlOrig = !rawPhoto.isNullOrBlank() &&
                        (rawPhoto.startsWith("http", ignoreCase = true) ||
                                rawPhoto.startsWith("https", ignoreCase = true))

                when {
                    // Prioridade 1: URL vinda da API de imagens
                    !fotoUrl.isNullOrBlank() -> {
                        AsyncImage(
                            model = fotoUrl,
                            contentDescription = "Imagem do produto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Prioridade 2: URL que já veio em produto.foto
                    isUrlOrig -> {
                        AsyncImage(
                            model = rawPhoto,
                            contentDescription = "Imagem do produto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Prioridade 3: base64
                    !rawPhoto.isNullOrBlank() -> {
                        val bmp = base64ToImageBitmapOrNull(rawPhoto)
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = "Imagem do produto",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingCart,
                                contentDescription = null,
                                tint = CartMuted.copy(alpha = 0.8f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    // Fallback final
                    else -> {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            tint = CartMuted.copy(alpha = 0.8f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    produto.nome,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.adicionais.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Column {
                        item.adicionais.forEach {
                            Text(
                                "· ${it.nome}",
                                color = Color(0xFFB8BEC6),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "Unitário: ${formatMoneyLocal(unitPrice(item))}",
                    color = Color(0xFFB8BEC6).copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatMoneyLocal(lineTotal(item)),
                    color = Color(0xFFF57C00),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Stepper(
                    value = item.quantidade,
                    onMinus = onRemove,
                    onPlus = onAdd
                )
            }
        }
    }
}

@Composable
private fun Stepper(value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMinus) {
            Icon(Icons.Default.Remove, contentDescription = "Diminuir", tint = Color(0xFFB8BEC6))
        }

        Text(
            text = value.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )

        IconButton(onClick = onPlus) {
            Icon(Icons.Default.Add, contentDescription = "Aumentar", tint = Color(0xFFF57C00))
        }
    }
}

private fun formatMoneyLocal(valor: Double?): String {
    val v = valor ?: 0.0
    return "R$ " + "%,.2f".format(v)
        .replace(',', 'X')
        .replace('.', ',')
        .replace('X', '.')
}

private fun base64ToImageBitmapOrNull(base64: String): ImageBitmap? {
    return runCatching {
        val clean = base64.substringAfter(",", base64)
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        bmp?.asImageBitmap()
    }.getOrNull()
}

