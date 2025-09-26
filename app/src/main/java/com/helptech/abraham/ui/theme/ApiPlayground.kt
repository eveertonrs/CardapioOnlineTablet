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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.helptech.abraham.BuildConfig
import com.helptech.abraham.data.remote.*
import com.helptech.abraham.data.remote.enviarPedido as enviarPedidoRemote
import com.helptech.abraham.network.AdicionaisRepo
import com.helptech.abraham.network.AdicionaisService
import com.helptech.abraham.network.buscarFotoPrincipal
import com.helptech.abraham.settings.AppSettings
import com.helptech.abraham.settings.DeviceRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// >>> usa o extension 'preco' (valor != 0 senão valor_ad)
import com.helptech.abraham.data.remote.preco

/* =========================================================
 *  Cores / Constantes
 * ========================================================= */

private val Orange     = Color(0xFFF57C00)
private val PanelBg    = Color(0xFF1F2226)
private val CardBg     = Color(0xFF24262B)
private val Muted      = Color(0xFFB8BEC6)
private val DividerClr = Color(0x33222222)

private const val ADMIN_PIN = "1234"

/* =========================================================
 *  Telas locais
 * ========================================================= */
private enum class Screen { MENU, CART }

/* =========================================================
 *  MODELOS DE CARRINHO (com adicionais)
 * ========================================================= */

data class CartItem(
    val key: String,
    val produto: ProdutoDto,
    var quantidade: Int,
    val adicionais: List<OpcaoAdicionalDto> // retornadas pelo ProductDetailSheet
)

private fun adicionalUnit(item: CartItem): Double =
    // <<< trocado para usar op.preco (fallback valor/valor_ad)
    item.adicionais.sumOf { it.preco }

private fun unitPrice(item: CartItem): Double =
    (item.produto.valor ?: 0.0) + adicionalUnit(item)

private fun lineTotal(item: CartItem): Double =
    unitPrice(item) * item.quantidade

private fun cartKey(produto: ProdutoDto, adicionais: List<OpcaoAdicionalDto>): String {
    // chave = produto + IDs dos adicionais (ordem estável)
    val addKey = adicionais.map { it.codigo }.sorted().joinToString("-")
    return "${produto.codigo}|$addKey"
}

/* =========================================================
 *  Carrinho
 * ========================================================= */

@Composable
private fun CartPage(
    itens: List<CartItem>,
    onAdd: (CartItem) -> Unit,
    onRemove: (CartItem) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    obsText: String,
    onObsChange: (String) -> Unit
) {
    val total = itens.sumOf { lineTotal(it) }

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
                items(itens, key = { it.key }) { item ->
                    CartRowOrange(
                        item = item,
                        onAdd = { onAdd(item) },
                        onRemove = { onRemove(item) }
                    )
                }
            }

            // ===== Observações do pedido =====
            Spacer(Modifier.height(12.dp))
            Text(
                "Observações do pedido",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = obsText,
                onValueChange = onObsChange,
                placeholder = { Text("ex.: tirar cebola, cortar ao meio, etc.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                maxLines = 4,
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Orange,
                    focusedBorderColor = Orange,
                    unfocusedBorderColor = Color(0xFF4B4F57),
                    focusedPlaceholderColor = Muted,
                    unfocusedPlaceholderColor = Muted,
                    // background leve p/ contraste
                    focusedContainerColor = Color(0xFF2D3238),
                    unfocusedContainerColor = Color(0xFF2D3238),
                    disabledContainerColor = Color(0xFF2D3238)
                )
            )

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
    item: CartItem,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val produto = item.produto

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
            Text(
                produto.nome,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            // preço unitário base
            Text(
                "Unitário: ${formatMoneyLocal(produto.valor)}",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )

            // lista de adicionais (nome + preço) — usa op.preco
            if (item.adicionais.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                val adicionaisTexto = item.adicionais.joinToString(separator = " · ") { op ->
                    val nome = op.nome ?: "Opção"
                    val v = op.preco
                    if (v > 0.0) "$nome (+ ${formatMoneyLocal(v)})" else nome
                }
                Text(
                    adicionaisTexto,
                    color = Color(0xFFDFE3EA),
                    style = MaterialTheme.typography.bodySmall
                )

                val acrescimo = adicionalUnit(item)
                if (acrescimo > 0.0) {
                    Text(
                        "Adicionais: + ${formatMoneyLocal(acrescimo)}",
                        color = Orange,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Stepper(value = item.quantidade, onMinus = onRemove, onPlus = onAdd)

        Spacer(Modifier.width(12.dp))

        Text(
            formatMoneyLocal(lineTotal(item)),
            color = Orange, fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun Stepper(value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Surface(shape = CircleShape, color = Color.Transparent, border = BorderStroke(1.dp, Orange)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(34.dp)) {
            IconButton(onClick = onMinus, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "Diminuir", tint = Color.White)
            }
            Text(text = value.toString(), color = Color.White, modifier = Modifier.widthIn(min = 24.dp))
            IconButton(onClick = onPlus, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Aumentar", tint = Color.White)
            }
        }
    }
}

private fun formatMoneyLocal(valor: Double?): String {
    val v = valor ?: 0.0
    return "R$ " + "%,.2f".format(v).replace(',', 'X').replace('.', ',').replace('X', '.')
}

// Stub seguro para caso alguma imagem venha em Base64.
@Composable
private fun base64ToImageBitmapOrNull(@Suppress("UNUSED_PARAMETER") base64: String)
        : androidx.compose.ui.graphics.ImageBitmap? = null

/* =========================================================
 *  Playground (Menu + Carrinho + Sheets/Diálogo)
 * ========================================================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiPlayground() {
    val ctx = LocalContext.current

    val role by AppSettings.observeRole(ctx).collectAsState(initial = DeviceRole.MESA)
    val tableNumber by AppSettings.observeTable(ctx).collectAsState(initial = 1)
    val mesaLabel = if (role == DeviceRole.BALCAO) "BALCÃO" else "MESA $tableNumber"

    val empresa = BuildConfig.API_EMPRESA.uppercase()
    val headerLabel = "$empresa · $mesaLabel"

    var produtos by remember { mutableStateOf<List<ProdutoDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var screen by remember { mutableStateOf(Screen.MENU) }

    // carrinho agora guarda itens com adicionais
    val cart = remember { mutableStateMapOf<String, CartItem>() } // key -> CartItem
    val cartCount by remember { derivedStateOf { cart.values.sumOf { it.quantidade } } }

    // observação do pedido
    var cartObs by rememberSaveable { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    var submitting by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    var reloadKey by remember { mutableStateOf(0) }

    var detalheProduto by remember { mutableStateOf<ProdutoDto?>(null) }
    var detalheGrupos by remember { mutableStateOf<List<GrupoAdicionalDto>>(emptyList()) }
    var carregandoAdicionais by remember { mutableStateOf(false) }

    var showMesaDialog by remember { mutableStateOf(false) }

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
        // NÃO filtramos mais por tipo — a API já decide o que vem
        produtos = todos
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
                            val locais = AdicionaisRepo.fromProduto(p)
                            if (locais.isNotEmpty()) {
                                detalheGrupos = locais
                                carregandoAdicionais = false
                            } else {
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
                        onMyBill = { statusMsg = "Minha conta: em breve listaremos os itens." },
                        onOpenMesaDialog = { showMesaDialog = true }
                    )
                }

                Screen.CART -> {
                    val itens: List<CartItem> = remember(cart.toMap()) {
                        cart.values.toList()
                    }

                    CartPage(
                        itens = itens,
                        onAdd = { it ->
                            cart[it.key] = it.copy(quantidade = it.quantidade + 1)
                        },
                        onRemove = { it ->
                            val q = it.quantidade - 1
                            if (q <= 0) cart.remove(it.key) else cart[it.key] = it.copy(quantidade = q)
                        },
                        onBack = { screen = Screen.MENU },
                        onConfirm = {
                            if (itens.isEmpty()) return@CartPage
                            scope.launch {
                                submitting = true

                                // AGRUPA por produto (mantém comportamento atual do envio)
                                val agrupado: Map<Int, Int> = itens
                                    .groupBy { it.produto.codigo }
                                    .mapValues { (_, list) -> list.sumOf { it.quantidade } }

                                val itensReq: List<ItemPedidoReq> = agrupado.map { (codigo, qtde) ->
                                    val produto = produtos.first { it.codigo == codigo }
                                    ItemPedidoReq(
                                        codigoProduto = codigo,
                                        quantidade   = qtde,
                                        valorUnit    = produto.valor ?: 0.0
                                    )
                                }

                                val result = enviarPedidoRemote(
                                    mesaLabel = headerLabel,
                                    itens = itensReq,
                                    tipoEntregaCodigo = "2",
                                    formaPgtoCodigo = "1",
                                    obsPedido = cartObs // envia observação
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
                                        cartObs = ""  // limpa observação após enviar
                                        screen = Screen.MENU
                                    }
                                    .onFailure { e ->
                                        statusMsg = "Falha ao enviar pedido: ${e.message ?: "erro desconhecido"}"
                                    }

                                submitting = false
                            }
                        },
                        obsText = cartObs,
                        onObsChange = { cartObs = it }
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
                        // achata as escolhas (somente lista de opções selecionadas)
                        val selecionadas = escolhido.escolhas.values.flatten()
                        val key = cartKey(produtoSheet, selecionadas)

                        val atual = cart[key]
                        if (atual == null) {
                            cart[key] = CartItem(
                                key = key,
                                produto = produtoSheet,
                                quantidade = escolhido.quantidade,
                                adicionais = selecionadas
                            )
                        } else {
                            cart[key] = atual.copy(quantidade = atual.quantidade + escolhido.quantidade)
                        }
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

    // ===== Diálogo de troca de mesa =====
    if (showMesaDialog) {
        MesaOptionsDialog(
            currentTable = tableNumber ?: 1,
            onDismiss = { showMesaDialog = false },
            onConfirm = { novaMesa ->
                scope.launch {
                    AppSettings.saveMesa(ctx, novaMesa)
                    showMesaDialog = false
                }
            }
        )
    }
}

/* ----------- Loading sheet ----------- */
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
            Text("Carregando opções…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ----------- Diálogo de opções da mesa (com PIN) ----------- */
@Composable
fun MesaOptionsDialog(
    currentTable: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selected by remember { mutableStateOf(currentTable) }
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
    ) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 2.dp) {
            Column(Modifier.padding(20.dp)) {
                Text("Opções de mesa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mesa:", modifier = Modifier.width(64.dp))
                    OutlinedTextField(
                        value = selected.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { if (it in 1..999) selected = it } },
                        singleLine = true,
                        modifier = Modifier.width(120.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = { if (selected < 999) selected++ }) { Text("+") }
                    Spacer(Modifier.width(4.dp))
                    FilledTonalButton(onClick = { if (selected > 1) selected-- }) { Text("-") }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it; error = null },
                    label = { Text("Senha do admin") },
                    singleLine = true,
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showPin = !showPin }) { Text(if (showPin) "Ocultar" else "Mostrar") }
                    }
                )

                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (pin == ADMIN_PIN) onConfirm(selected)
                        else error = "Senha incorreta."
                    }) { Text("Trocar de mesa") }
                }
            }
        }
    }
}
