package com.helptech.abraham.data.remote

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class ApiEnvelope(
    val sucesso: JsonElement? = null,
    val erro: String? = null
)

data class ProdutoDto(
    val codigo: Int,
    val categoria_codigo: Int?,
    val tipo: String?,
    val nome: String,
    val descricao: String?,
    val valor: Double?,
    val estoque: Double?,
    val ativo: String?,
    val categoria_nome: String?,
    @SerializedName(value = "foto", alternate = ["imagem", "image", "img", "foto_base64"])
    val foto: String? = null
)
