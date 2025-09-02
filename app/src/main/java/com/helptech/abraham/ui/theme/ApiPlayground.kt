package com.helptech.abraham.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.helptech.abraham.BuildConfig
import com.helptech.abraham.data.remote.AkitemClient
import com.helptech.abraham.data.remote.ApiEnvelope
import com.helptech.abraham.data.remote.ProdutoDto
import com.helptech.abraham.settings.AppSettings
import com.helptech.abraham.settings.DeviceRole

@Composable
fun ApiPlayground() {
    val ctx = LocalContext.current

    // Observa o tipo de dispositivo (MESA/BALCÃO) e o número da mesa
    val role by AppSettings.observeRole(ctx).collectAsState(initial = DeviceRole.MESA.name)
    val tableNumber by AppSettings.observeTable(ctx).collectAsState(initial = 1)
    val mesaLabel = if (role == DeviceRole.BALCAO.name) "BALCÃO" else "MESA $tableNumber"

    // Estados de dados
    var produtos by remember { mutableStateOf<List<ProdutoDto>>(emptyList()) }
    var mostrarMenu by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Carrinho em memória
    val cart = remember { mutableStateMapOf<Int, Int>() }
    val cartCount by derivedStateOf { cart.values.sum() }
    var cartOpen by remember { mutableStateOf(false) }

    // Carrega automaticamente na primeira composição
    LaunchedEffect(Unit) {
        val body = mapOf(
            "item_adicional" to "",
            "n_categoria_codigo" to "",
            "codigo" to "",
            "codigo_empresa" to "",
            "ativo" to "",
            "imagem" to "base64"
        )
        runCatching {
            AkitemClient.api.call(
                empresa = BuildConfig.API_EMPRESA,
                modulo = "produto",
                funcao = "consultar",
                body = body
            )
        }.onSuccess { env: ApiEnvelope ->
            if (env.erro != null) {
                error = env.erro
            } else {
                val listType = object : TypeToken<List<ProdutoDto>>() {}.type
                produtos = Gson().fromJson(env.sucesso, listType) ?: emptyList()
                mostrarMenu = true
            }
        }.onFailure { e ->
            error = e.message ?: "Falha desconhecida"
        }
        loading = false
    }

    when {
        mostrarMenu -> {
            RestaurantMenuScreen(
                produtos = produtos,
                onAddToCart = { p -> cart[p.codigo] = (cart[p.codigo] ?: 0) + 1 },
                mesaLabel = mesaLabel,
                cartCount = cartCount,
                onCartClick = { cartOpen = true }
            )

            if (cartOpen) {
                val itens = produtos.filter { cart.containsKey(it.codigo) }
                    .map { it to (cart[it.codigo] ?: 0) }
                    .filter { it.second > 0 }

                CartBottomSheet(
                    itens = itens,
                    onAdd = { p -> cart[p.codigo] = (cart[p.codigo] ?: 0) + 1 },
                    onRemove = { p ->
                        val q = (cart[p.codigo] ?: 0) - 1
                        if (q <= 0) cart.remove(p.codigo) else cart[p.codigo] = q
                    },
                    onClose = { cartOpen = false },
                    onCheckout = {
                        // TODO: montar JSON e enviar pedido
                        cartOpen = false
                    }
                )
            }
        }

        loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        else -> {
            // Estado de erro (quando a chamada falha)
            Column(
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
                Button(onClick = {
                    // força um novo carregamento
                    error = null
                    loading = true
                    mostrarMenu = false
                    // reexecuta o LaunchedEffect alterando a chave
                    // (tática simples: mudar um state "reloadKey")
                }) {
                    Text("Tentar novamente")
                }
            }
        }
    }
}
