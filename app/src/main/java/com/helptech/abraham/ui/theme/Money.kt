package com.helptech.abraham.ui.theme

import java.text.NumberFormat
import java.util.Locale

// Formatação única e reutilizável para o app inteiro (pt-BR)
private val moneyBr: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
}

/** Usa em qualquer lugar: ApiPlayground, Menu, etc. */
fun formatMoney(valor: Double?): String = moneyBr.format(valor ?: 0.0)
