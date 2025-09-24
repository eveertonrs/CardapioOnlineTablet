package com.helptech.abraham.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.helptech.abraham.BuildConfig
import com.helptech.abraham.data.remote.*
import com.helptech.abraham.data.remote.enviarPedido as enviarPedidoRemote
import com.helptech.abraham.network.AdicionaisService
import com.helptech.abraham.network.buscarFotoPrincipal
import com.helptech.abraham.settings.AppSettings
import com.helptech.abraham.settings.DeviceRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.helptech.abraham.network.AdicionaisRepo


/* =========================================================
 *  BLOCO DO CARRINHO
 * ========================================================= */

private val Orange     = Color(0xFFF57C00)
private val PanelBg    = Color(0xFF1F2226)
private val CardBg     = Color(0xFF24262B)
private val Muted      = Color(0xFFB8BEC6)
private val DividerClr = Color(0x33222222)

@Composable
private fun CartPage(
    itens: List<Pair<ProdutoDto, Int>>,
    onAdd: (ProdutoDto) -> Unit,
    onRemove: (ProdutoDto) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val total = itens.sumOf { (p, q) -> (p.valor ?: 0.0) * q }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PanelBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Seu carrinho",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(contentColor = Orange)
            ) { Text("Voltar") }
        }
        Divider(color = DividerClr)
        Spacer(Modifier.height(12.dp))

        if (itens.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Seu carrinho está vazio.", color = Muted)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(itens, key = { it.first.codigo }) { (p, q) ->
                    CartRowOrange(
                        produto = p,
                        quantidade = q,
                        onAdd = { onAdd(p) },
                        onRemove = { onRemove(p) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = DividerClr)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total", color = Muted, fontWeight = FontWeight.SemiBold)
                Text(formatMoneyLocal(total), color = Color.White, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = itens.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color.White),
                shape = MaterialTheme.shapes.large
            ) { Text("Confirmar pedido", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun CartRowOrange(
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
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)
                .background(Color(0xFF343A40)),
            contentAlignment = Alignment.Center
        ) {
            var fotoUrl by remember(produto.codigo) { mutableStateOf<String?>(null) }
            LaunchedEffect(produto.codigo) {
                fotoUrl = try { buscarFotoPrincipal(produto.codigo) } catch (_: Exception) { null }
            }

            val fotoOrig = produto.foto
            val isUrlOrig = !fotoOrig.isNullOrBlank() &&
                    (fotoOrig.startsWith("http", true) || fotoOrig.startsWith("https", true))

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
                        Image(
                            bitmap = img, contentDescription = null,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(produto.nome, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                "Unitário: ${formatMoneyLocal(produto.valor)}",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Stepper(
            value = quantidade,
            onMinus = onRemove,
            onPlus  = onAdd
        )

        Spacer(Modifier.width(12.dp))

        Text(
            formatMoneyLocal((produto.valor ?: 0.0) * quantidade),
            color = Orange,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun Stepper(
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Orange)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(34.dp)
        ) {
            IconButton(onClick = onMinus, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "Diminuir", tint = Color.White)
            }
            Text(
                text = value.toString(),
                color = Color.White,
                modifier = Modifier.widthIn(min = 24.dp)
            )
            IconButton(onClick = onPlus, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Aumentar", tint = Color.White)
            }
        }
    }
}

private fun formatMoneyLocal(valor: Double?): String {
    val v = valor ?: 0.0
    return "R$ " + "%,.2f".format(v)
        .replace(',', 'X').replace('.', ',').replace('X', '.')
}

// Stub seguro para caso alguma imagem venha em Base64.
@Composable
private fun base64ToImageBitmapOrNull(base64: String)
        : androidx.compose.ui.graphics.ImageBitmap? = null

/* =========================================================
 *  TELA DE PLAYGROUND (MENU + CARRINHO + SHEET DE DETALHE)
 * ========================================================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiPlayground() {
    val ctx = LocalContext.current

    val role by AppSettings.observeRole(ctx).collectAsState(initial = DeviceRole.MESA.name)
    val tableNumber by AppSettings.observeTable(ctx).collectAsState(initial = 1)
    val mesaLabel = if (role == DeviceRole.BALCAO.name) "BALCÃO" else "MESA $tableNumber"

    val empresa = BuildConfig.API_EMPRESA.uppercase()
    val headerLabel = "$empresa · $mesaLabel"

    var produtos by remember { mutableStateOf<List<ProdutoDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var screen by remember { mutableStateOf(Screen.MENU) }

    val cart = remember { mutableStateMapOf<Int, Int>() } // codigo -> qtde
    val cartCount by remember { derivedStateOf { cart.values.sum() } }

    val scope = rememberCoroutineScope()
    var submitting by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    var reloadKey by remember { mutableStateOf(0) }

    var detalheProduto by remember { mutableStateOf<ProdutoDto?>(null) }
    var detalheGrupos by remember { mutableStateOf<List<GrupoAdicionalDto>>(emptyList()) }
    var carregandoAdicionais by remember { mutableStateOf(false) }

    // Carrega produtos
    LaunchedEffect(reloadKey) {
        val body = mapOf(
            "item_adicional" to "",
            "n_categoria_codigo" to "",
            "codigo" to "",
            "codigo_empresa" to "",
            "ativo" to "S"
        )
        loading = true
        error = null

        val env: ApiEnvelope = runCatching {
            withContext(Dispatchers.IO) {
                AkitemClient.api.call(
                    empresa = BuildConfig.API_EMPRESA,
                    modulo = "produto",
                    funcao = "consultar",
                    body = body
                )
            }
        }.onFailure { e ->
            error = e.message ?: "Falha desconhecida"
            loading = false
        }.getOrElse { return@LaunchedEffect }

        env.erro?.let {
            error = it
            loading = false
            return@LaunchedEffect
        }

        val listType = object : TypeToken<List<ProdutoDto>>() {}.type
        val todos: List<ProdutoDto> = withContext(Dispatchers.Default) {
            Gson().fromJson(env.sucesso, listType) ?: emptyList()
        }
        val apenasProdutos = withContext(Dispatchers.Default) {
            todos.filter { it.tipo.equals("PRODUTO", true) || it.tipo.equals("PIZZA", true) }
        }

        produtos = apenasProdutos
        loading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2B2F33))
    ) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            error != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = error ?: "Sem dados", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { reloadKey++ }) { Text("Tentar novamente") }
            }

            else -> when (screen) {
                Screen.MENU -> {
                    RestaurantMenuColumnsScreen(
                        produtos = produtos,
                        onAddToCart = { p: ProdutoDto ->
                            detalheProduto = p
                            // 1) tenta extrair dos próprios dados do produto
                            val locais: List<GrupoAdicionalDto> = AdicionaisRepo.fromProduto(p)
                            if (locais.isNotEmpty()) {
                                detalheGrupos = locais
                                carregandoAdicionais = false
                            } else {
                                // 2) fallback: chama a API
                                carregandoAdicionais = true
                                scope.launch {
                                    detalheGrupos = AdicionaisService.consultarAdicionais(p.codigo)
                                    carregandoAdicionais = false
                                }
                            }
                        },
                        mesaLabel = headerLabel,
                        cartCount = cartCount,
                        onCartClick = { screen = Screen.CART },
                        onCallWaiter = {
                            scope.launch {
                                submitting = true
                                val r = chamarGarcom(headerLabel)
                                r.onSuccess {
                                    statusMsg = "Chamado enviado! Um garçom irá até $headerLabel."
                                }.onFailure { e ->
                                    statusMsg = "Não consegui chamar o garçom: ${e.message ?: "verifique conexão/endpoint"}"
                                }
                                submitting = false
                            }
                        },
                        onMyBill = { statusMsg = "Minha conta: em breve listaremos os itens." }
                    )
                }

                Screen.CART -> {
                    val itens: List<Pair<ProdutoDto, Int>> = remember(produtos, cart.toMap()) {
                        produtos
                            .filter { cart.containsKey(it.codigo) }
                            .map { it to (cart[it.codigo] ?: 0) }
                            .filter { it.second > 0 }
                    }

                    CartPage(
                        itens = itens,
                        onAdd = { p: ProdutoDto ->
                            cart[p.codigo] = (cart[p.codigo] ?: 0) + 1
                        },
                        onRemove = { p: ProdutoDto ->
                            val q = (cart[p.codigo] ?: 0) - 1
                            if (q <= 0) cart.remove(p.codigo) else cart[p.codigo] = q
                        },
                        onBack = { screen = Screen.MENU },
                        onConfirm = {
                            if (itens.isEmpty()) return@CartPage
                            scope.launch {
                                submitting = true
                                val itensReq: List<ItemPedidoReq> = itens.map { (p, q) ->
                                    ItemPedidoReq(
                                        codigoProduto = p.codigo,
                                        quantidade   = q,
                                        valorUnit    = p.valor ?: 0.0
                                    )
                                }
                                val result = enviarPedidoRemote(
                                    mesaLabel = headerLabel,
                                    itens = itensReq,
                                    tipoEntregaCodigo = "2",
                                    formaPgtoCodigo = "1",
                                    obsPedido = ""
                                )

                                result
                                    .onSuccess { el: JsonElement? ->
                                        val pedidoId = when {
                                            el == null -> null
                                            el.isJsonPrimitive -> el.asString
                                            el.isJsonObject && el.asJsonObject.has("codigo") ->
                                                el.asJsonObject.get("codigo").asString
                                            else -> null
                                        }
                                        statusMsg = if (pedidoId.isNullOrBlank())
                                            "Pedido enviado com sucesso!"
                                        else
                                            "Pedido enviado com sucesso! Nº: $pedidoId"

                                        cart.clear()
                                        screen = Screen.MENU
                                    }
                                    .onFailure { e ->
                                        statusMsg = "Falha ao enviar pedido: ${e.message ?: "erro desconhecido"}"
                                    }

                                submitting = false
                            }
                        }
                    )

                    if (submitting) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color(0x66000000)),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                }
            }
        }

        // Sheet de detalhe
        val produtoSheet = detalheProduto
        if (produtoSheet != null) {
            if (carregandoAdicionais) {
                LoadingBottomSheet(onDismiss = { detalheProduto = null })
            } else {
                ProductDetailSheet(
                    produto = produtoSheet,
                    grupos = detalheGrupos,
                    onDismiss = { detalheProduto = null },
                    onConfirm = { escolhido ->
                        cart[produtoSheet.codigo] =
                            (cart[produtoSheet.codigo] ?: 0) + escolhido.quantidade
                        detalheProduto = null
                    }
                )
            }
        }
    }

    if (statusMsg != null) {
        AlertDialog(
            onDismissRequest = { statusMsg = null },
            confirmButton = { TextButton(onClick = { statusMsg = null }) { Text("OK") } },
            title = { Text("Status") },
            text = { Text(statusMsg ?: "") }
        )
    }
}

/* ----------- Loading sheet ÚNICO (renomeado) ----------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                "Carregando opções…",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

private enum class Screen { MENU, CART }
