package com.helptech.abraham.ui.theme

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.helptech.abraham.data.remote.ProdutoDto
import com.helptech.abraham.network.buscarFotoPrincipal
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image


/* ===== Paleta “laranja” (frames 01/02/03) ===== */
private val Orange = Color(0xFFF57C00)
private val MenuBg = Color(0xFF2B2F33)
private val PanelBg = Color(0xFF1F2226)
private val RailBg = Color(0xFF1C1F23)
private val Muted = Color(0xFFB8BEC6)
private val CardBg = Color(0xFF24262B)
private val DividerClr = Color(0x33222222)

/**
 * OBS: o carrinho é aberto por navegação externa (onCartClick()).
 * Os parâmetros de carrinho no final foram mantidos só por compatibilidade
 * e NÃO são usados aqui.
 */
@Composable
fun RestaurantMenuColumnsScreen(
    produtos: List<ProdutoDto>,
    onAddToCart: (ProdutoDto) -> Unit,
    mesaLabel: String,
    cartCount: Int,
    onCartClick: () -> Unit
) {
    // categorias distintas, mantendo ordem de aparição
    val categorias = remember(produtos) {
        produtos.mapNotNull { it.categoria_nome?.takeIf { n -> n.isNotBlank() } }
            .distinct()
    }
    var categoriaSel by remember(categorias) { mutableStateOf(categorias.firstOrNull()) }

    val filtrados = remember(produtos, categoriaSel) {
        produtos.filter { p ->
            (categoriaSel == null || p.categoria_nome == categoriaSel) &&
                    (p.tipo.equals("PRODUTO", true) || p.tipo.equals("PIZZA", true))
        }
    }

    Column(Modifier.fillMaxSize().background(MenuBg)) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mesaLabel,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onCartClick,
                colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White),
                shape = CircleShape
            ) {
                Text("Carrinho ($cartCount)")
            }
        }

        Divider(color = DividerClr)

        Row(Modifier.fillMaxSize()) {
            // Esquerda: categorias
            LazyColumn(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .background(PanelBg)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categorias) { cat ->
                    val selected = cat == categoriaSel
                    Surface(
                        onClick = { categoriaSel = cat },
                        color = if (selected) Orange.copy(alpha = 0.18f) else Color.Transparent,
                        shape = MaterialTheme.shapes.medium,
                        border = if (selected) BorderStroke(1.dp, Orange) else null,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = cat,
                            color = if (selected) Color.White else Muted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                        )
                    }
                }
            }

            // Direita: lista de produtos
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MenuBg)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtrados, key = { it.codigo }) { p ->
                    ProductBigCard(
                        produto = p,
                        onAdd = { onAddToCart(p) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductBigCard(
    produto: ProdutoDto,
    onAdd: () -> Unit
) {
    Surface(
        color = CardBg,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // foto grande
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color(0xFF343A40)),
                contentAlignment = Alignment.Center
            ) {
                var url by remember(produto.codigo) { mutableStateOf<String?>(null) }
                LaunchedEffect(produto.codigo) {
                    url = try { buscarFotoPrincipal(produto.codigo) } catch (_: Exception) { null }
                }
                val f = produto.foto
                val isUrl = !f.isNullOrBlank() && (f.startsWith("http", true) || f.startsWith("https", true))

                when {
                    !url.isNullOrBlank() -> AsyncImage(
                        model = url, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                    isUrl -> AsyncImage(
                        model = f, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                    !f.isNullOrBlank() -> {
                        val bmp = base64ToImageBitmapOrNull(f)
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(produto.nome, color = Color.White, fontWeight = FontWeight.Bold)
                if (!produto.descricao.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(produto.descricao!!, color = Muted, maxLines = 2)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "R$ " + "%,.2f".format(produto.valor ?: 0.0)
                        .replace(',', 'X').replace('.', ',').replace('X', '.'),
                    color = Orange, fontWeight = FontWeight.Black
                )
            }

            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White),
                shape = MaterialTheme.shapes.large
            ) { Text("Adicionar") }
        }
    }
}


/* ---------- Top bar ---------- */

@Composable
private fun TopBarOrange(
    mesaLabel: String,
    cartCount: Int,
    onCartClick: () -> Unit,
    search: String,
    onSearchChange: (String) -> Unit
) {
    val focus = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Orange)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = mesaLabel,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Spacer(Modifier.width(12.dp))

            TextField(
                value = search,
                onValueChange = onSearchChange,
                placeholder = { Text("Buscar…", color = Color(0xFFFFF3E0)) },
                singleLine = true,
                modifier = Modifier
                    .height(44.dp)
                    .weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0x33FFFFFF),
                    unfocusedContainerColor = Color(0x33FFFFFF),
                    disabledContainerColor = Color(0x33FFFFFF),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(Modifier.width(12.dp))
            TopChip("COMANDAS") {}
            Spacer(Modifier.width(8.dp))
            TopChip("MESA") {}
            Spacer(Modifier.width(8.dp))
            TopChip("CARRINHO ($cartCount)") {
                focus.clearFocus()
                onCartClick()
            }
        }
    }
}

@Composable
private fun TopChip(text: String, onClick: (() -> Unit)? = null) {
    Surface(
        color = Color(0x26FFFFFF),
        contentColor = Color.White,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .height(36.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
    }
}

/* ---------- Rail de categorias ---------- */

@Composable
private fun CategoryRail(
    categorias: List<String>,
    produtosPorCategoria: Map<String, List<ProdutoDto>>,
    selecionada: String,
    onSeleciona: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(RailBg)
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        Text(
            "CATEGORIAS",
            color = Color.White,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Divider(color = DividerClr)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            items(categorias) { cat ->
                val selected = cat == selecionada
                val produtos = produtosPorCategoria[cat].orEmpty()
                val thumbProduto = produtos.firstOrNull()

                CategoryTile(
                    titulo = cat,
                    produtoThumb = thumbProduto,
                    selected = selected,
                    onClick = { onSeleciona(cat) }
                )
            }
        }
    }
}

@Composable
private fun CategoryTile(
    titulo: String,
    produtoThumb: ProdutoDto?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Orange.copy(alpha = 0.22f) else PanelBg
    val fg = if (selected) Color.White else Muted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // miniatura
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Color(0xFF343A40)),
            contentAlignment = Alignment.Center
        ) {
            var foto by remember(produtoThumb?.codigo) { mutableStateOf<String?>(null) }
            LaunchedEffect(produtoThumb?.codigo) {
                foto = produtoThumb?.codigo?.let { codigo ->
                    try { buscarFotoPrincipal(codigo) } catch (_: Exception) { null }
                }
            }

            val fotoOrig = produtoThumb?.foto
            val isUrlOrig = !fotoOrig.isNullOrBlank() &&
                    (fotoOrig.startsWith("http", true) || fotoOrig.startsWith("https", true))

            when {
                !foto.isNullOrBlank() -> AsyncImage(
                    model = foto, contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
                isUrlOrig -> AsyncImage(
                    model = fotoOrig, contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
                !fotoOrig.isNullOrBlank() -> {
                    val img = base64ToImageBitmapOrNull(fotoOrig)
                    if (img != null) {
                        Image(bitmap = img, contentDescription = null,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text("IMG", color = Muted, fontSize = 11.sp)
                    }
                }
                else -> Text("IMG", color = Muted, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.width(10.dp))
        Text(
            text = titulo,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* ---------- Grid de produtos ---------- */

@Composable
private fun ProductGrid(
    produtos: List<ProdutoDto>,
    onAdd: (ProdutoDto) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 260.dp),
        modifier = modifier
            .fillMaxHeight()
            .background(PanelBg)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(produtos, key = { it.codigo }) { p ->
            ProductCardOrange(
                produto = p,
                onAdd = { onAdd(p) }
            )
        }
    }
}

@Composable
private fun ProductCardOrange(
    produto: ProdutoDto,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(CardBg)
    ) {
        // imagem
        var fotoUrl by remember(produto.codigo) { mutableStateOf<String?>(null) }
        LaunchedEffect(produto.codigo) {
            fotoUrl = try {
                buscarFotoPrincipal(produto.codigo)
            } catch (e: Exception) {
                Log.w("Menu", "foto erro: ${e.message}")
                null
            }
        }

        val fotoOrig = produto.foto
        val isUrlOrig = !fotoOrig.isNullOrBlank() &&
                (fotoOrig.startsWith("http", true) || fotoOrig.startsWith("https", true))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFF343A40))
        ) {
            when {
                !fotoUrl.isNullOrBlank() -> AsyncImage(
                    model = fotoUrl, contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
                isUrlOrig -> AsyncImage(
                    model = fotoOrig, contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
                !fotoOrig.isNullOrBlank() -> {
                    val img = base64ToImageBitmapOrNull(fotoOrig)
                    if (img != null) {
                        Image(bitmap = img, contentDescription = null,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
            }
        }

        // título + preço + botão
        Column(Modifier.padding(12.dp)) {
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
                    produto.descricao!!,
                    color = Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatMoneyUi(produto.valor),
                    color = Orange,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Orange,
                        contentColor = Color.White
                    )
                ) { Text("ADICIONAR AO CARRINHO") }
            }
        }
    }
}

/* ---------- Utilitário ---------- */

/** Versão local para evitar conflito com outras funções de formatação no mesmo package. */
private fun formatMoneyUi(valor: Double?): String {
    val v = valor ?: 0.0
    return "R$ " + "%,.2f".format(v).replace(',', 'X').replace('.', ',').replace('X', '.')
}
