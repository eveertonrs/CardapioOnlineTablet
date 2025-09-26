package com.helptech.abraham.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import com.helptech.abraham.network.AdicionaisRepo
import com.helptech.abraham.network.AdicionaisService
import com.helptech.abraham.network.buscarFotoPrincipal
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

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
 * Host (opcional)
 * ============================================================ */
@Composable
fun MenuScreenHost(
    produtos: List<ProdutoDto>,
    mesaLabel: String,
    cartCount: Int,
    onCartClick: () -> Unit,
    onAddConfirm: (ProdutoEscolhido, ProdutoDto) -> Unit,
    onCallWaiter: (() -> Unit)? = null,
    onMyBill:   (() -> Unit)? = null
) {
    var produtoSel by remember { mutableStateOf<ProdutoDto?>(null) }
    var gruposSel   by remember(produtoSel) { mutableStateOf<List<GrupoAdicionalDto>?>(null) }

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
        onMyBill = onMyBill
    )

    produtoSel?.let { p ->
        LaunchedEffect(p.codigo) {
            val locais = AdicionaisRepo.fromProduto(p)
            gruposSel = if (locais.isNotEmpty()) locais
            else AdicionaisService.consultarAdicionais(p.codigo)
        }

        val fechar = {
            produtoSel = null
            gruposSel = null
        }

        when (val g = gruposSel) {
            null -> ProductDetailLoading(onDismiss = fechar)
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

/** Tela principal */
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
                var idx = firstIdx.coerceIn(0, flatList.lastIndex.coerceAtLeast(0))
                while (idx >= 0 && flatList[idx] !is SectionItem.Header) idx--
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
                onAdd = { p -> onAddToCart(p) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/* ---------- Top bar (mais alta) ---------- */

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
                .height(64.dp)              // topo mais alto
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mesaLabel,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onOpenMesaDialog?.invoke() },
                        onDoubleTap = { onOpenMesaDialog?.invoke() }
                    )
                }
            )

            Spacer(Modifier.width(10.dp))

            SearchField(
                value = search,
                onValueChange = onSearchChange,
                onSearch = { focus.clearFocus() },
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(10.dp))
            TopChip("MINHA CONTA", onMyBill)
            Spacer(Modifier.width(8.dp))
            TopChip("CHAMAR GARÇOM", onCallWaiter)
            Spacer(Modifier.width(8.dp))
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
                fontSize = 16.sp
            )
        },
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF5A5A5A)) },
        textStyle = TextStyle(
            fontSize = 18.sp,         // maior e sem cortar caudas
            lineHeight = 24.sp
        ),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier
            .heightIn(min = 56.dp)    // altura padrão confortável
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

/* Chips do topo maiores (inclui CARRINHO) */
@Composable
private fun TopChip(text: String, onClick: (() -> Unit)? = null) {
    Surface(
        color = Color(0x26FFFFFF),
        contentColor = Color.White,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .height(36.dp)                // ↑ altura do chip
            .defaultMinSize(minWidth = 132.dp) // ↑ mais “corpo”, o de Carrinho fica maior
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp), // ↑ padding
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                fontSize = 14.sp,          // ↑ fonte
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
    onAdd: (ProdutoDto) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxHeight()
            .background(MenuBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        items(items, key = { it.hashCode() }) { item ->
            when (item) {
                is SectionItem.Header -> SectionHeader(item.categoria)
                is SectionItem.Product -> ProductRowCompact(item.produto) { onAdd(item.produto) }
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProductRowCompact(
    produto: ProdutoDto,
    onAdd: () -> Unit
) {
    Surface(color = CardBg, shape = MaterialTheme.shapes.large, tonalElevation = 0.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
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
                                bitmap = bmp, contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    produto.nome,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!produto.descricao.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        produto.descricao!!,
                        color = Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatMoneyUi(produto.valor),
                    color = Orange, fontWeight = FontWeight.Black, fontSize = 18.sp
                )
            }

            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White),
                shape = MaterialTheme.shapes.large
            ) { Text("ADICIONAR AO CARRINHO") }
        }
    }
}

/* ---------- Utils ---------- */

private fun formatMoneyUi(valor: Double?): String {
    val v = valor ?: 0.0
    return "R$ " + "%,.2f".format(v).replace(',', 'X').replace('.', ',').replace('X', '.')
}

@Composable
private fun base64ToImageBitmapOrNull(base64: String) =
    runCatching { decodeBase64ToImageBitmap(base64) }.getOrNull()

private fun decodeBase64ToImageBitmap(@Suppress("UNUSED_PARAMETER") base64: String)
        : androidx.compose.ui.graphics.ImageBitmap? = null
