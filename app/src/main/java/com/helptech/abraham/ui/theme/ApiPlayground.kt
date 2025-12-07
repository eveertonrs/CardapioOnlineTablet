package com.helptech.abraham.ui.theme

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.helptech.abraham.BuildConfig
import com.helptech.abraham.data.remote.*
import com.helptech.abraham.data.remote.enviarPedido as enviarPedidoRemote
import com.helptech.abraham.integracao.IntegracaoService
import com.helptech.abraham.network.AdicionaisRepo
import com.helptech.abraham.network.AdicionaisService
import com.helptech.abraham.network.buscarFotoPrincipal
import com.helptech.abraham.settings.AppSettings
import com.helptech.abraham.settings.DeviceRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.style.TextAlign
import kotlin.inc

/* =========================================================
 *  Cores / Constantes
 * ========================================================= */

private val Orange     = Color(0xFFF57C00)
private val PanelBg    = Color(0xFF1F2226)
private val CardBg     = Color(0xFF24262B)
private val CardAltBg  = Color(0xFF2A2D33)
private val Muted      = Color(0xFFB8BEC6)
private val DividerClr = Color(0x33222222)

private const val ADMIN_PIN = "1234"

/* =========================================================
 *  Telas locais
 * ========================================================= */
private enum class Screen { MENU, CART }

/* =========================================================
 *  Helpers de JSON seguro (evita crashes)
 * ========================================================= */

private fun JsonElement?.asStringOrNull(): String? =
    if (this != null && this.isJsonPrimitive && this.asJsonPrimitive.isString) this.asString else null

private fun JsonElement?.asDoubleOrNull(): Double? =
    if (this != null && this.isJsonPrimitive && this.asJsonPrimitive.isNumber) this.asDouble else null

private fun JsonElement?.asBooleanOrNull(): Boolean? =
    if (this != null && this.isJsonPrimitive && this.asJsonPrimitive.isBoolean) this.asBoolean else null

private fun JsonElement?.asJsonObjectOrNull(): JsonObject? =
    if (this != null && this.isJsonObject) this.asJsonObject else null

private fun JsonElement?.asJsonArrayOrNull(): JsonArray? =
    if (this != null && this.isJsonArray) this.asJsonArray else null

// ---- Extras para desarmar BOM e JSON "dentro de string"
private fun String.stripBom(): String =
    if (isNotEmpty() && this[0] == '\uFEFF') substring(1) else this

private fun parseJson(s: String): JsonElement? =
    runCatching { JsonParser.parseString(s) }.getOrNull()

private fun unwrapToJsonObject(any: Any?): JsonObject? = when (any) {
    is JsonObject -> any
    is JsonElement -> try { any.asJsonObject } catch (_: Exception) {
        if (any.isJsonPrimitive && any.asJsonPrimitive.isString)
            parseJson(any.asString.stripBom())?.asJsonObjectOrNull()
        else null
    }
    is String -> {
        val first = parseJson(any.stripBom())
        when {
            first == null -> null
            first.isJsonObject -> first.asJsonObject
            first.isJsonPrimitive && first.asJsonPrimitive.isString ->
                parseJson(first.asString.stripBom())?.asJsonObjectOrNull()
            else -> null
        }
    }
    else -> null
}

/* =========================================================
 *  Carrinho (NOVO)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PanelBg),
        contentAlignment = Alignment.TopCenter
    ) {
        // coluna centralizada com largura máxima — fica ótimo em tablet
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ){
            // --- Cabeçalho ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Seu Carrinho",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .height(44.dp)
                        .widthIn(min = 220.dp),               // garante largura boa
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Orange,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 0.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "ADICIONAR MAIS ITENS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Divider(color = DividerClr)

            if (itens.isEmpty()) {
                // --- Carrinho Vazio ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = "Carrinho Vazio",
                        tint = Muted.copy(alpha = 0.5f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Seu carrinho está vazio",
                        style = MaterialTheme.typography.titleMedium,
                        color = Muted
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Adicione produtos do nosso cardápio!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted.copy(alpha = 0.8f)
                    )
                }
            } else {
                // --- Lista de Itens ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(itens, key = { it.key }) { item ->
                        CartItemRow(
                            item = item,
                            onAdd = { onAdd(item) },
                            onRemove = { onRemove(item) }
                        )
                    }
                }

                // --- Observações e Total ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        "Observações do pedido",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = obsText,
                        onValueChange = onObsChange,
                        placeholder = {
                            Text(
                                "Ex: tirar cebola, cortar ao meio, etc.",
                                color = Muted
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 120.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Orange,
                            unfocusedBorderColor = DividerClr,
                            focusedContainerColor = CardBg,
                            unfocusedContainerColor = CardBg
                        ),
                        shape = MaterialTheme.shapes.large
                    )

                    Spacer(Modifier.height(16.dp))
                    Divider(color = DividerClr)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total",
                            color = Muted,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            formatMoneyLocal(total),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = itens.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Orange,
                            contentColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            "Confirmar Pedido",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
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
        color = CardBg,
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
                    .background(CardAltBg),
                contentAlignment = Alignment.Center
            ) {
                // 1) tenta buscar a foto principal pela API
                var url by remember(produto.codigo) { mutableStateOf<String?>(null) }
                LaunchedEffect(produto.codigo) {
                    url = try { buscarFotoPrincipal(produto.codigo) } catch (_: Exception) { null }
                }

                // 2) foto original do produto (URL ou base64)
                val f = produto.foto
                val isUrlOrig = !f.isNullOrBlank() &&
                        (f.startsWith("http", ignoreCase = true) ||
                                f.startsWith("https", ignoreCase = true))

                when {
                    // Prioridade 1: URL vinda da API de imagens
                    !url.isNullOrBlank() -> {
                        AsyncImage(
                            model = url,
                            contentDescription = "Imagem do produto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Prioridade 2: URL que já veio em produto.foto
                    isUrlOrig -> {
                        AsyncImage(
                            model = f,
                            contentDescription = "Imagem do produto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Prioridade 3: base64 em produto.foto
                    !f.isNullOrBlank() -> {
                        val bmp = base64ToImageBitmapOrNull(f)
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
                                tint = Muted.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    // Fallback final: ícone
                    else -> {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            tint = Muted.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // --- Detalhes (Nome e Adicionais) ---
            Column(
                modifier = Modifier.weight(1f)
            ) {
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
                                color = Muted,
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
                    color = Muted.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.width(12.dp))

            // --- Preço e Stepper ---
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatMoneyLocal(lineTotal(item)),
                    color = Orange,
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
        val buttonSize = 40.dp
        IconButton(
            onClick = onMinus,
            modifier = Modifier.size(buttonSize),
            colors = IconButtonDefaults.iconButtonColors(contentColor = Muted)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Diminuir")
        }

        Text(
            text = value.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.widthIn(min = 32.dp)
        )

        IconButton(
            onClick = onPlus,
            modifier = Modifier.size(buttonSize),
            colors = IconButtonDefaults.iconButtonColors(contentColor = Orange)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aumentar")
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

// Função normal, chamada de dentro do remember
private fun base64ToImageBitmapOrNull(base64: String): androidx.compose.ui.graphics.ImageBitmap? {
    return runCatching {
        // remove "data:image/xxx;base64," se vier
        val clean = base64.substringAfter(",", base64)
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        bmp?.asImageBitmap()
    }.getOrNull()
}


/* =========================================================
 *  Playground
 * ========================================================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiPlayground() {
    val ctx = LocalContext.current

    val role by AppSettings.observeRole(ctx).collectAsState(initial = DeviceRole.MESA)
    val tableNumber by AppSettings.observeTable(ctx).collectAsState(initial = 1)
    val deviceSerial by AppSettings.observeDeviceSerial(ctx).collectAsState(initial = "")
    val mesaLabel = if (role == DeviceRole.BALCAO) "BALCÃO" else "MESA $tableNumber"

    val empresa = BuildConfig.API_EMPRESA.uppercase()
    val headerLabel = "$empresa · $mesaLabel"

    var produtos by remember { mutableStateOf<List<ProdutoDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var screen by remember { mutableStateOf(Screen.MENU) }

    val cart = remember { mutableStateMapOf<String, CartItem>() }
    val cartCount by remember {
        derivedStateOf { cart.values.sumOf { it.quantidade } }
    }

    var cartObs by rememberSaveable { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    var submitting by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var autoCloseSeconds by remember { mutableStateOf<Int?>(null) } // contador p/ garçom

    var reloadKey by remember { mutableStateOf(0) }

    var detalheProduto by remember { mutableStateOf<ProdutoDto?>(null) }
    var detalheGrupos by remember { mutableStateOf<List<GrupoAdicionalDto>>(emptyList()) }
    var carregandoAdicionais by remember { mutableStateOf(false) }

    var showMesaDialog by remember { mutableStateOf(false) }

    var myBillJson by remember { mutableStateOf<JsonObject?>(null) }
    val prettyGson: Gson = remember { GsonBuilder().setPrettyPrinting().create() }

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
                    empresa = null,
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
        produtos = todos
        loading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2B2F33))
    ) {
        when {
            loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            error != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = error ?: "Sem dados",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { reloadKey++ }) {
                    Text("Tentar novamente")
                }
            }

            else -> when (screen) {
                Screen.MENU -> {
                    MenuScreenHost(
                        produtos = produtos,
                        mesaLabel = headerLabel,
                        cartCount = cartCount,
                        onCartClick = { screen = Screen.CART },
                        onAddConfirm = { escolhido, produtoSheet ->
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
                                cart[key] = atual.copy(
                                    quantidade = atual.quantidade + escolhido.quantidade
                                )
                            }
                            screen = Screen.CART
                        },
                        // CHAMAR GARÇOM – mantém a mesma lógica que você já tinha
                        onCallWaiter = {
                            scope.launch {
                                submitting = true
                                val r = chamarGarcom(mesaLabel)
                                r.onSuccess {
                                    statusMsg = "Chamado enviado! Um garçom irá até $headerLabel."
                                    autoCloseSeconds = 10
                                }.onFailure { e ->
                                    statusMsg =
                                        "Não consegui chamar o garçom: ${e.message ?: "verifique conexão/endpoint"}"
                                    autoCloseSeconds = 10
                                }
                                submitting = false
                            }
                        },
                        // MINHA CONTA – aqui você chama sua lógica de “minha conta”
                        onMyBill = {
                            scope.launch {
                                submitting = true

                                // Mesma regra antiga: BALCÃO ou número da mesa
                                val contaStr = if (role == DeviceRole.BALCAO) "BALCÃO" else tableNumber.toString()

                                val result = runCatching {
                                    IntegracaoService.consultarConsumoJson(contaStr)
                                }.fold(
                                    onSuccess = { it },
                                    onFailure = { e ->
                                        submitting = false
                                        statusMsg = "Não consegui consultar a conta: ${e.message ?: "verifique conexão/endpoint"}"
                                        autoCloseSeconds = null // na tela de conta não auto-fecha
                                        return@launch
                                    }
                                )

                                result.onSuccess { obj ->
                                    myBillJson = obj
                                    if (obj == null) {
                                        statusMsg = "Consulta ok, mas não recebi um objeto JSON válido para exibir."
                                        autoCloseSeconds = null
                                    }
                                }.onFailure { e ->
                                    statusMsg = "Não consegui consultar a conta: ${e.message ?: "verifique conexão/endpoint"}"
                                    autoCloseSeconds = null
                                }

                                submitting = false
                            }
                        },
                        // DOUBLE CLICK / LONG PRESS NA MESA → abre o dialog de troca de mesa
                        onOpenMesaDialog = {
                            showMesaDialog = true
                        }
                    )
                }

                Screen.CART -> {
                    val itens: List<CartItem> =
                        remember(cart.toMap()) { cart.values.toList() }
                    CartPage(
                        itens = itens,
                        onAdd = { it ->
                            cart[it.key] =
                                it.copy(quantidade = it.quantidade + 1)
                        },
                        onRemove = { it ->
                            val q = it.quantidade - 1
                            if (q <= 0) cart.remove(it.key)
                            else cart[it.key] = it.copy(quantidade = q)
                        },
                        onBack = { screen = Screen.MENU },
                        onConfirm = {
                            if (itens.isEmpty()) return@CartPage
                            scope.launch {
                                submitting = true

                                val itensReq: List<ItemPedidoReq> = itens.map { ci ->
                                    ItemPedidoReq(
                                        codigoProduto = ci.produto.codigo,
                                        quantidade = ci.quantidade,
                                        valorUnit = ci.produto.valor ?: 0.0,
                                        adicionais = ci.adicionais.map { op ->
                                            AdicionalReq(
                                                codigo = op.codigo,
                                                nome = op.nome,
                                                valor = op.preco
                                            )
                                        },
                                        observacaoItem = ci.adicionais.joinToString(" · ") {
                                            it.nome ?: "Adicional"
                                        }
                                    )
                                }

                                val result = enviarPedidoRemote(
                                    mesaLabel = mesaLabel,
                                    itens = itensReq,
                                    tipoEntregaCodigo = "2",
                                    formaPgtoCodigo = "1",
                                    obsPedido = cartObs
                                )

                                result.onSuccess { el: JsonElement? ->
                                    val pedidoId = when {
                                        el == null -> null
                                        el.isJsonPrimitive -> el.asString
                                        el.isJsonObject && el.asJsonObject.has("codigo") ->
                                            el.asJsonObject.get("codigo").asString
                                        else -> null
                                    }
                                    statusMsg =
                                        if (pedidoId.isNullOrBlank())
                                            "Pedido enviado com sucesso!"
                                        else
                                            "Pedido enviado com sucesso! Nº: $pedidoId"

                                    autoCloseSeconds = null
                                    cart.clear()
                                    cartObs = ""
                                    screen = Screen.MENU
                                }.onFailure { e: Throwable ->
                                    statusMsg =
                                        "Falha ao enviar pedido: ${e.message ?: "erro desconhecido"}"
                                    autoCloseSeconds = null
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
                        ) {
                            CircularProgressIndicator()
                        }
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
                            cart[key] = atual.copy(
                                quantidade = atual.quantidade + escolhido.quantidade
                            )
                        }
                        detalheProduto = null
                        screen = Screen.CART
                    }
                )
            }
        }
    }

    /* ---------- Status dialog (com auto-close opcional para Garçom) ---------- */
    /* ---------- Status dialog (com auto-close opcional para Garçom) ---------- */
    if (statusMsg != null) {
        // contador
        LaunchedEffect(statusMsg, autoCloseSeconds) {
            val start = autoCloseSeconds
            if (start != null && start > 0) {
                var s = start
                while (s > 0 && statusMsg != null) {
                    delay(1000)
                    s -= 1
                    autoCloseSeconds = s
                }
                if (s == 0) {
                    statusMsg = null
                    autoCloseSeconds = null
                }
            }
        }

        StatusDialog(
            message = statusMsg ?: "",
            autoCloseSeconds = autoCloseSeconds,
            onDismiss = {
                statusMsg = null
                autoCloseSeconds = null
            }
        )
    }
    // Diálogo "Minha conta" — versão redesenhada
    myBillJson?.let { obj ->
        MyBillDialog(root = obj, onDismiss = { myBillJson = null })
    }

    // Diálogo de troca/atualização de mesa
    if (showMesaDialog) {
        MesaOptionsDialog(
            currentTable = tableNumber,
            deviceSerial = deviceSerial.orEmpty(),          // <<< NOVO
            onDismiss = { showMesaDialog = false },
            onConfirm = { novaMesa ->
                scope.launch {
                    AppSettings.saveMesa(ctx, novaMesa)
                    showMesaDialog = false
                }
            },
            onRefreshItems = {
                reloadKey++
                showMesaDialog = false
            }
        )
    }
}


/* ----------- Linha auxiliar para o resumo da conta ----------- */

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    strong: Boolean = false,
    muted: Boolean = false,
    textPrimary: Color,
    textSecondary: Color,
    highlight: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (muted) textSecondary.copy(alpha = 0.6f) else textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            value,
            color = if (strong) highlight else textPrimary,
            fontWeight = if (strong) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}

/* ----------- "Minha conta" – diálogo com melhor visual ----------- */

@Composable
private fun MyBillDialog(root: JsonObject, onDismiss: () -> Unit) {
    val Panel         = Color(0xFFF7F8FA)
    val Card          = Color.White
    val CardAlt       = Color(0xFFF1F3F6)
    val TextPrimary   = Color(0xFF111827)
    val TextSecondary = Color(0xFF6B7280)
    val LightDivider  = Color(0xFFE5E7EB)
    val Orange        = Color(0xFFF57C00)

    val conta   = root.get("Conta").asStringOrNull()
    val cliente = root.get("Cliente").asJsonObjectOrNull()
    val nome    = cliente?.get("Nome").asStringOrNull()
    val doc     = cliente?.get("Documento").asStringOrNull()
    val tel     = cliente?.get("Telefone").asStringOrNull()

    val taxaRemovida = cliente?.get("TaxaServicoRemovida").asBooleanOrNull() ?: false
    val taxa      = cliente?.get("TaxaServico").asDoubleOrNull()
    val descontos = cliente?.get("ValorDescontos").asDoubleOrNull()
    val brindes   = cliente?.get("ValorBrindes").asDoubleOrNull()
    val consMin   = cliente?.get("ConsumacaoMinima").asDoubleOrNull()
    val total     = cliente?.get("Total").asDoubleOrNull()

    val itens = root.get("Itens").asJsonArrayOrNull()

    // FULL SCREEN
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Panel,
            shape = RoundedCornerShape(0.dp) // era .none
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Minha conta",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onDismiss) { Text("OK") }
                }

                Spacer(Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Cabeçalho (conta + cliente)
                    Surface(
                        color = Card,
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, LightDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            if (!conta.isNullOrBlank()) {
                                Text(conta, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            val linha2 = listOfNotNull(nome, doc, tel).joinToString(" · ")
                            if (linha2.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    linha2,
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Resumo
                    Surface(
                        color = Card,
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, LightDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "Resumo",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))

                            taxa?.let {
                                SummaryLine(
                                    label = "Taxa de serviço" +
                                            if (taxaRemovida) " (removida)" else "",
                                    value = formatMoneyLocal(it),
                                    muted = taxaRemovida,
                                    textPrimary = TextPrimary,
                                    textSecondary = TextSecondary,
                                    highlight = Orange
                                )
                            }
                            descontos?.takeIf { it != 0.0 }?.let {
                                SummaryLine(
                                    label = "Descontos",
                                    value = "- ${formatMoneyLocal(it)}",
                                    textPrimary = TextPrimary,
                                    textSecondary = TextSecondary,
                                    highlight = Orange
                                )
                            }
                            brindes?.takeIf { it != 0.0 }?.let {
                                SummaryLine(
                                    label = "Brindes",
                                    value = "- ${formatMoneyLocal(it)}",
                                    textPrimary = TextPrimary,
                                    textSecondary = TextSecondary,
                                    highlight = Orange
                                )
                            }
                            consMin?.takeIf { it != 0.0 }?.let {
                                SummaryLine(
                                    label = "Consumação mínima",
                                    value = formatMoneyLocal(it),
                                    textPrimary = TextPrimary,
                                    textSecondary = TextSecondary,
                                    highlight = Orange
                                )
                            }

                            Divider(
                                Modifier.padding(vertical = 8.dp),
                                color = LightDivider
                            )
                            SummaryLine(
                                label = "Total",
                                value = formatMoneyLocal(total),
                                strong = true,
                                textPrimary = TextPrimary,
                                textSecondary = TextSecondary,
                                highlight = Orange
                            )
                        }
                    }

                    // Itens
                    if (itens != null && itens.size() > 0) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Itens",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items((0 until itens.size()).toList()) { idx ->
                                val el = itens.get(idx).asJsonObjectOrNull()
                                val prod =
                                    el?.get("Produto").asStringOrNull() ?: "Item"
                                val desc = el?.get("Descricao").asStringOrNull()
                                val qtde =
                                    el?.get("Quantidade").asDoubleOrNull()
                                        ?.toInt() ?: 0
                                val totalItem =
                                    el?.get("ValorTotal").asDoubleOrNull()
                                val rowBg = if (idx % 2 == 0) Card else CardAlt

                                Surface(
                                    color = rowBg,
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, LightDivider),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                prod,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                totalItem?.let {
                                                    formatMoneyLocal(it)
                                                } ?: "—",
                                                color = Orange,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        desc?.takeIf { it.isNotBlank() }?.let {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                it,
                                                color = TextSecondary,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0x143B82F6),
                                            border = BorderStroke(
                                                1.dp,
                                                Color(0x263B82F6)
                                            )
                                        ) {
                                            Text(
                                                "Qtd: $qtde",
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp,
                                                    vertical = 2.dp
                                                ),
                                                color = Color(0xFF1E40AF),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.height(10.dp))
                        Text("Nenhum item encontrado.", color = TextSecondary)
                    }
                }
            }
        }
    }
}

/* ----------- Loading sheet ----------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingBottomSheet(onDismiss: () -> Unit) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = {
            androidx.compose.material3.BottomSheetDefaults.DragHandle()
        }
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

/* ----------- Diálogo de opções da mesa (com PIN + Atualizar Itens) ----------- */
@Composable
fun MesaOptionsDialog(
    currentTable: Int,
    deviceSerial: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onRefreshItems: () -> Unit
) {
    var selected by remember { mutableStateOf(currentTable) }
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 2.dp
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Opções de mesa",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mesa:", modifier = Modifier.width(64.dp))
                    OutlinedTextField(
                        value = selected.toString(),
                        onValueChange = { v ->
                            v.toIntOrNull()?.let {
                                if (it in 1..999) selected = it
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.width(120.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { if (selected < 999) selected++ }
                    ) { Text("+") }
                    Spacer(Modifier.width(4.dp))
                    FilledTonalButton(
                        onClick = { if (selected > 1) selected-- }
                    ) { Text("-") }
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it; error = null },
                    label = { Text("Senha do admin") },
                    singleLine = true,
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showPin = !showPin }) {
                            Text(if (showPin) "Ocultar" else "Mostrar")
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = deviceSerial.ifBlank { "—" },
                    onValueChange = { },
                    enabled = false,
                    label = { Text("Serial do dispositivo") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = onRefreshItems) {
                        Text("Atualizar Itens")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (pin == ADMIN_PIN) onConfirm(selected)
                            else error = "Senha incorreta."
                        }
                    ) { Text("Trocar de mesa") }
                }
            }
        }
    }
}
@Composable
private fun StatusDialog(
    message: String,
    autoCloseSeconds: Int?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000)),   // fundo bem escuro
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.6f)           // ocupa boa parte da tela em tablet
                    .padding(24.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF111111)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF111111)
                    )

                    if (autoCloseSeconds != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Esta mensagem será fechada automaticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Orange,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (autoCloseSeconds != null)
                                "OK (${autoCloseSeconds}s)"
                            else
                                "OK",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
