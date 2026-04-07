package com.agroapp.model

data class Banner(
    val imageRes: Int,
    val title: String,
    val description: String,
    val destination: String = "" // "mercado", "compartir", o "" para no hacer nada
)