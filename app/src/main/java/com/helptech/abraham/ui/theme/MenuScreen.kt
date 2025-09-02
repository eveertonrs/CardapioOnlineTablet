package com.helptech.abraham.ui.theme

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.helptech.abraham.data.remote.ProdutoDto
import com.helptech.abraham.network.buscarFotoPrincipal   // <-- IMPORTANTE


// Paleta inspirada no visual do print (escuro + acento vermelho)
private val MenuBg = Color(0xFF121214)
private val PanelBg = Color(0xFF1A1B1F)
private val RailBg = Color(0xFF1F2025)
private val Accent = Color(0xFFD32F2F)     // vermelho do botão
private val AccentText = Color.White
private val Muted = Color(0xFF9AA0A6)
private val CardBg = Color(0xFF24262B)
private val DividerClr = Color(0xFF2E3036)

@Composable
fun RestaurantMenuScreen(
    produtos: List<ProdutoDto>,
    onAddToCart: (ProdutoDto) -> Unit,
    mesaLabel: String = "MESA 1",
    cartCount: Int,
    onCartClick: () -> Unit = {},
    onSearch: (String) -> Unit = {},
) {
    // Agrupa por categoria (null -> "Outros")
    val porCategoria = remember(produtos) {
        produtos.groupBy { it.categoria_nome ?: "Outros" }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }
    var categoriaSelecionada by remember { mutableStateOf(porCategoria.keys.firstOrNull() ?: "Outros") }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MenuBg)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // RAIL LATERAL DE CATEGORIAS
        Column(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(RailBg)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                "CARDÁPIO",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Divider(color = DividerClr)
            Spacer(Modifier.height(8.dp))

            porCategoria.keys.forEach { cat ->
                val selected = cat == categoriaSelecionada
                val bg = if (selected) PanelBg else Color.Transparent
                val fg = if (selected) Color.White else Muted
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg)
                        .clickable { categoriaSelecionada = cat }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(cat, color = fg, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }

        // COLUNA PRINCIPAL
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(PanelBg)
        ) {
            // TOP BAR
            TopBar(mesaLabel = mesaLabel, cartCount = cartCount, onCartClick = onCartClick)

            // LISTA DE PRODUTOS (categoria selecionada)
            val lista = porCategoria[categoriaSelecionada].orEmpty()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lista, key = { it.codigo }) { p ->
                    ProductRowCard(
                        produto = p,
                        onAdd = { onAddToCart(p) }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun TopBar(mesaLabel: String, cartCount: Int, onCartClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelBg)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = mesaLabel,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
            Spacer(Modifier.width(12.dp))
            // espaço para BUSCA / ações no futuro
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color(0xFF2A2C31)),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "Buscar...",
                    color = Muted,
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Pill(text = "Chamar garçom")
            Spacer(Modifier.width(8.dp))
            Pill(text = "Minha conta")
            Spacer(Modifier.width(8.dp))
            Pill(text = "Carrinho ($cartCount)", onClick = onCartClick)
        }
        Spacer(Modifier.height(8.dp))
        Divider(color = DividerClr)
    }
}

@Composable
private fun Pill(text: String, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(MaterialTheme.shapes.small)
            .background(Color(0xFF2A2C31))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun ProductRowCard(
    produto: ProdutoDto,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(CardBg)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ===== IMAGEM DO PRODUTO =====
        // 1) Tenta buscar a imagem principal no backend (rota retornaProdutoAnexo)
        var fotoUrl by remember(produto.codigo) { mutableStateOf<String?>(null) }

        LaunchedEffect(produto.codigo) {
            // só buscamos para itens do tipo "PRODUTO" (combinação com o cliente)
            fotoUrl = if (produto.tipo.equals("PRODUTO", ignoreCase = true)) {
                buscarFotoPrincipal(produto.codigo)
            } else null

            Log.d(
                "Menu",
                "Produto ${produto.codigo} '${produto.nome}' urlAPI=$fotoUrl"
            )
        }

        // 2) Fallback: usa campo existente `produto.foto` (URL direta ou base64)
        val fotoOrig = produto.foto
        val isUrlOrig = !fotoOrig.isNullOrBlank() &&
                (fotoOrig.startsWith("http", true) || fotoOrig.startsWith("https", true))

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Color(0xFF3A3C42)),
            contentAlignment = Alignment.Center
        ) {
            when {
                !fotoUrl.isNullOrBlank() -> {
                    AsyncImage(
                        model = fotoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                isUrlOrig -> {
                    AsyncImage(
                        model = fotoOrig,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                !fotoOrig.isNullOrBlank() -> {
                    val img = remember(fotoOrig) { base64ToImageBitmapOrNull(fotoOrig) }
                    if (img != null) {
                        Image(
                            bitmap = img,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("IMG", color = Muted, fontSize = 12.sp)
                    }
                }
                else -> {
                    Text("IMG", color = Muted, fontSize = 12.sp)
                }
            }
        }
        // ==============================

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                produto.nome,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!produto.descricao.isNullOrBlank()) {
                Text(
                    produto.descricao ?: "",
                    color = Muted,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    produto.categoria_nome ?: "",
                    color = Muted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatMoney(produto.valor),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = AccentText
                )
            ) { Text("ADICIONAR AO CARRINHO") }
        }
    }
}

fun formatMoney(valor: Double?): String {
    val v = valor ?: 0.0
    return "R$ " + "%,.2f".format(v).replace(',', 'X').replace('.', ',').replace('X', '.')
}
