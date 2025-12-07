package com.helptech.abraham.ui.theme

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.helptech.abraham.data.remote.GrupoAdicionalDto
import com.helptech.abraham.data.remote.ProdutoDto
import com.helptech.abraham.integracao.getAndroidId
import com.helptech.abraham.network.AdicionaisRepo
import com.helptech.abraham.network.AdicionaisService
import com.helptech.abraham.network.buscarFotoPrincipal
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/* ===== Paleta ===== */
private val Orange     = Color(0xFFF57C00)
private val MenuBg     = Color(0xFF2B2F33)
private val PanelBg    = Color(0xFF1F2226)
private val RailBg     = Color(0xFF1C1F23)
private val Muted      = Color(0xFFB8BEC6)
private val CardBg     = Color(0xFF24262B)
private val DividerClr = Color(0x33222222)

/** Itens para a lista seccionada */
private sealed interface SectionItem {
    data class Header(val categoria: String) : SectionItem
    data class Product(val produto: ProdutoDto) : SectionItem
}

/* ============================================================
 * Host (abre sheet, carrega adicionais e confirma seleção)
 * ============================================================ */
@Composable
fun MenuScreenHost(
    produtos: List<ProdutoDto>,
    mesaLabel: String,
    cartCount: Int,
    onCartClick: () -> Unit,
    onAddConfirm: (ProdutoEscolhido, ProdutoDto) -> Unit,
    onCallWaiter: (() -> Unit)? = null,
    onMyBill:   (() -> Unit)? = null,
    onOpenMesaDialog: (() -> Unit)? = null
) {
    var produtoSel by remember { mutableStateOf<ProdutoDto?>(null) }
    var gruposSel by remember(produtoSel) { mutableStateOf<List<GrupoAdicionalDto>?>(null) }

    // >>> Só repassa as ações para a tela principal
    RestaurantMenuColumnsScreen(
        produtos = produtos,
        onAddToCart = { p ->
            produtoSel = p
            gruposSel = null
        },
        mesaLabel = mesaLabel,
        cartCount = cartCount,
        onCartClick = onCartClick,
        onCallWaiter = onCallWaiter,
        onMyBill = onMyBill,
        onOpenMesaDialog = onOpenMesaDialog
    )

    // >>> Lógica de abrir o bottom sheet de detalhes continua igual
    produtoSel?.let { p ->
        LaunchedEffect(p.codigo) {
            val locais: List<GrupoAdicionalDto> = AdicionaisRepo.fromProduto(produto = p)
            gruposSel = if (locais.isNotEmpty()) locais
            else AdicionaisService.consultarAdicionais(produtoCodigo = p.codigo)
        }

        val fechar = {
            produtoSel = null
            gruposSel = null
        }

        when (val g = gruposSel) {
            null -> LoadingBottomSheet(onDismiss = fechar)
            else -> ProductDetailSheet(
                produto = p,
                grupos = g,
                onDismiss = fechar,
                onConfirm = { escolhas ->
                    onAddConfirm(escolhas, p)
                    fechar()
                }
            )
        }
    }
}


/** Tela principal (barra + rail + lista) */
@Composable
fun RestaurantMenuColumnsScreen(
    produtos: List<ProdutoDto>,
    onAddToCart: (ProdutoDto) -> Unit,
    mesaLabel: String,
    cartCount: Int,
    onCartClick: () -> Unit,
    onCallWaiter: (() -> Unit)? = null,
    onMyBill:   (() -> Unit)? = null,
    onOpenMesaDialog: (() -> Unit)? = null
) {
    var search by remember { mutableStateOf("") }

    val agrupados: Map<String, List<ProdutoDto>> = remember(produtos) {
        produtos
            .filter { it.tipo.equals("PRODUTO", true) || it.tipo.equals("PIZZA", true) }
            .groupBy { it.categoria_nome?.takeIf { n -> n.isNotBlank() } ?: "Outros" }
            .toSortedMap(compareBy { it.lowercase() })
    }

    val categoriasBase = remember(agrupados) {
        agrupados.keys.filterNot { it.equals("Adicionais", ignoreCase = true) }.toList()
    }

    var categoriaSel by remember(categoriasBase) { mutableStateOf(categoriasBase.firstOrNull() ?: "") }

    val flatList: List<SectionItem> = remember(agrupados, categoriasBase, search) {
        val cats = categoriasBase
        buildList {
            if (search.isBlank()) {
                cats.forEach { cat ->
                    val itens = agrupados[cat].orEmpty()
                    if (itens.isNotEmpty()) {
                        add(SectionItem.Header(cat))
                        itens.forEach { p -> add(SectionItem.Product(p)) }
                    }
                }
            } else {
                val q = search.trim().lowercase()
                cats.forEach { cat ->
                    val itens = agrupados[cat].orEmpty().filter { p ->
                        p.nome.lowercase().contains(q) ||
                                (p.descricao?.lowercase()?.contains(q) == true)
                    }
                    if (itens.isNotEmpty()) {
                        add(SectionItem.Header(cat))
                        itens.forEach { p -> add(SectionItem.Product(p)) }
                    }
                }
            }
        }
    }

    val headerIndexByCategory: Map<String, Int> = remember(flatList) {
        buildMap {
            flatList.forEachIndexed { idx, item ->
                if (item is SectionItem.Header) put(item.categoria, idx)
            }
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { firstIdx ->
                val maxIndex = if (flatList.isNotEmpty()) flatList.lastIndex else 0
                var idx = firstIdx.coerceIn(0, maxIndex).coerceAtLeastZero()
                while (idx >= 0 && flatList.getOrNull(idx) !is SectionItem.Header) idx--
                (flatList.getOrNull(idx) as? SectionItem.Header)?.categoria
            }
            .filter { it != null }
            .distinctUntilChanged()
            .collectLatest { cat -> categoriaSel = cat!! }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MenuBg)
    ) {
        Spacer(Modifier.height(8.dp))

        TopBarOrange(
            mesaLabel = mesaLabel,
            cartCount = cartCount,
            onCartClick = onCartClick,
            search = search,
            onSearchChange = { search = it },
            onMyBill = onMyBill,
            onCallWaiter = onCallWaiter,
            onOpenMesaDialog = onOpenMesaDialog
        )

        HorizontalDivider(color = DividerClr)

        Row(Modifier.fillMaxSize()) {
            CategoryRail(
                categorias = categoriasBase,
                produtosPorCategoria = agrupados,
                selecionada = categoriaSel,
                onSeleciona = { cat ->
                    categoriaSel = cat
                    headerIndexByCategory[cat]?.let { headerIdx ->
                        scope.launch { listState.animateScrollToItem(headerIdx) }
                    }
                }
            )

            SectionedProductList(
                items = flatList,
                listState = listState,
                onProductClick = { p -> onAddToCart(p) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DeviceInfoDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val serial = remember { getAndroidId(ctx) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Informações do Dispositivo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Serial do dispositivo:")
                Text(serial, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

private fun Int.coerceAtLeastZero() = if (this < 0) 0 else this

/* ---------- Top bar ---------- */

@Composable
private fun TopBarOrange(
    mesaLabel: String,
    cartCount: Int,
    onCartClick: () -> Unit,
    search: String,
    onSearchChange: (String) -> Unit,
    onMyBill: (() -> Unit)?,
    onCallWaiter: (() -> Unit)?,
    onOpenMesaDialog: (() -> Unit)? = null
) {
    val focus = LocalFocusManager.current

    Surface(color = Orange) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp) // Aumentado
                .padding(horizontal = 16.dp), // Aumentado
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mesaLabel,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp, // Aumentado
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onOpenMesaDialog?.invoke() },
                        onDoubleTap = { onOpenMesaDialog?.invoke() }
                    )
                }
            )

            Spacer(Modifier.width(16.dp)) // Aumentado

            SearchField(
                value = search,
                onValueChange = onSearchChange,
                onSearch = { focus.clearFocus() },
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(12.dp)) // Aumentado
            TopChip("MINHA CONTA", onMyBill)
            Spacer(Modifier.width(10.dp)) // Aumentado
            TopChip("CHAMAR GARÇOM", onCallWaiter)
            Spacer(Modifier.width(10.dp)) // Aumentado
            TopChip("CARRINHO ($cartCount)") {
                focus.clearFocus()
                onCartClick()
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                "Buscar produto…",
                maxLines = 1,
                color = Color(0xFF5A5A5A),
                fontSize = 18.sp // Aumentado
            )
        },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF5A5A5A)) },
        textStyle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp), // Aumentado
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier
            .heightIn(min = 60.dp) // Aumentado
            .clip(MaterialTheme.shapes.medium),
        colors = TextFieldDefaults.colors(
            focusedContainerColor   = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor  = Color.White,
            focusedIndicatorColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor             = Color.Black,
            focusedTextColor        = Color.Black,
            unfocusedTextColor      = Color.Black,
            focusedPlaceholderColor   = Color(0xFF5A5A5A),
            unfocusedPlaceholderColor = Color(0xFF7C7C7C),
        )
    )
}

@Composable
private fun TopChip(text: String, onClick: (() -> Unit)? = null) {
    Surface(
        color = Color(0x26FFFFFF),
        contentColor = Color.White,
        shape = MaterialTheme.shapes.medium, // Aumentado
        modifier = Modifier
            .height(48.dp) // Aumentado
            .defaultMinSize(minWidth = 140.dp) // Aumentado
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp), // Aumentado
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                fontSize = 16.sp, // Aumentado
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/* ---------- Rail / Lista ---------- */

@Composable
private fun CategoryRail(
    categorias: List<String>,
    produtosPorCategoria: Map<String, List<ProdutoDto>>,
    selecionada: String,
    onSeleciona: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(220.dp)
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
        HorizontalDivider(color = DividerClr)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
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
            .height(60.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
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

@Composable
private fun SectionedProductList(
    items: List<SectionItem>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onProductClick: (ProdutoDto) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxHeight()
            .background(MenuBg)
            .padding(horizontal = 20.dp, vertical = 16.dp), // Aumentado
        verticalArrangement = Arrangement.spacedBy(16.dp), // Aumentado
        contentPadding = PaddingValues(bottom = 32.dp) // Aumentado
    ) {
        items(items, key = { it.hashCode() }) { item ->
            when (item) {
                is SectionItem.Header -> SectionHeader(item.categoria)
                is SectionItem.Product -> ProductRow(item.produto, onClick = { onProductClick(item.produto) })
            }
        }
    }
}

@Composable
private fun SectionHeader(titulo: String) {
    Surface(
        color = PanelBg,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, Color(0x22000000)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = titulo,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), // Aumentado
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp // Aumentado
        )
    }
}

@Composable
private fun ProductRow(
    produto: ProdutoDto,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = CardBg,
        tonalElevation = 2.dp,
        modifier = Modifier.clickable { onClick() } // Card inteiro clicável
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp), // Aumentado
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Imagem do Produto ---
            Box(
                modifier = Modifier
                    .size(160.dp) // Aumentado
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color(0xFF343A40)),
                contentAlignment = Alignment.Center
            ) {
                var url by remember(produto.codigo) { mutableStateOf<String?>(null) }
                LaunchedEffect(produto.codigo) {
                    url = try { buscarFotoPrincipal(produto.codigo) } catch (_: Exception) { null }
                }
                val f = produto.foto
                val isUrl = !f.isNullOrBlank() &&
                        (f.startsWith("http", true) || f.startsWith("https|https", true))

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
                                bitmap = bmp, contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(16.dp)) // Aumentado

            // --- Detalhes do Produto ---
            Column(Modifier.weight(1f)) {
                Text(
                    produto.nome,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp, // Aumentado
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!produto.descricao.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        produto.descricao!!,
                        color = Muted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Preço + botão na MESMA LINHA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMoneyUi(produto.valor),
                        color = Orange,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )

                    Button(
                        onClick = { onClick() }, // abre o detalhe igual clicar no card
                        modifier = Modifier
                            .height(40.dp)
                            .defaultMinSize(minWidth = 140.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Orange,
                            contentColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            "ADICIONAR",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}


/* ---------- Utils ---------- */

private fun formatMoneyUi(valor: Double?): String {
    val v = valor ?: 0.0
    return "R$ " + "%,.2f".format(v).replace(',', 'X').replace('.', ',').replace('X', '.')
}

@Composable
private fun base64ToImageBitmapOrNull(base64: String): ImageBitmap? =
    runCatching { decodeBase64ToImageBitmap(base64) }.getOrNull()

private fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
    val clean = base64.substringAfter(",", base64)
    val bytes = Base64.decode(clean, Base64.DEFAULT)
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    return bmp.asImageBitmap()
}
