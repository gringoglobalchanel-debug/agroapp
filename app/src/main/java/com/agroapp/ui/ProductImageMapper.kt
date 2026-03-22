package com.agroapp.ui

import com.agroapp.R

object ProductImageMapper {

    fun getImage(productName: String): Int {
        return when {
            productName.contains("sandía", ignoreCase = true) ||
                    productName.contains("sandia", ignoreCase = true) -> R.drawable.sandia
            productName.contains("plátano maduro", ignoreCase = true) ||
                    productName.contains("platano maduro", ignoreCase = true) -> R.drawable.platano_maduro
            productName.contains("guineo verde", ignoreCase = true) -> R.drawable.guineo_verde
            productName.contains("aguacate", ignoreCase = true) -> R.drawable.aguacate
            productName.contains("ají chombo", ignoreCase = true) ||
                    productName.contains("ajichombo", ignoreCase = true) -> R.drawable.ajichombo
            productName.contains("ají dulce", ignoreCase = true) ||
                    productName.contains("aji dulce", ignoreCase = true) -> R.drawable.aji_dulce
            productName.contains("ajo", ignoreCase = true) -> R.drawable.ajo
            productName.contains("arroz", ignoreCase = true) -> R.drawable.arroz
            productName.contains("banano", ignoreCase = true) -> R.drawable.banano
            productName.contains("berenjena", ignoreCase = true) -> R.drawable.berenjena
            productName.contains("brócoli", ignoreCase = true) ||
                    productName.contains("brocoli", ignoreCase = true) -> R.drawable.brocoli
            productName.contains("cacao", ignoreCase = true) -> R.drawable.cacao
            productName.contains("café", ignoreCase = true) ||
                    productName.contains("cafe", ignoreCase = true) -> R.drawable.cafe
            productName.contains("camote", ignoreCase = true) -> R.drawable.camote
            productName.contains("caña", ignoreCase = true) ||
                    productName.contains("cana de azucar", ignoreCase = true) -> R.drawable.cana_de_azucar
            productName.contains("cebollina", ignoreCase = true) -> R.drawable.cebollina
            productName.contains("cebolla", ignoreCase = true) -> R.drawable.cebolla
            productName.contains("chayote", ignoreCase = true) -> R.drawable.chayote
            productName.contains("cilantro", ignoreCase = true) -> R.drawable.cilantro
            productName.contains("coliflor", ignoreCase = true) -> R.drawable.coliflor
            productName.contains("culantro", ignoreCase = true) -> R.drawable.culantro
            productName.contains("frijol", ignoreCase = true) -> R.drawable.frijol
            productName.contains("lechuga", ignoreCase = true) -> R.drawable.lechuga
            productName.contains("lenteja", ignoreCase = true) -> R.drawable.lentejas
            productName.contains("limón", ignoreCase = true) ||
                    productName.contains("limon", ignoreCase = true) -> R.drawable.limon
            productName.contains("maíz", ignoreCase = true) ||
                    productName.contains("maiz", ignoreCase = true) -> R.drawable.maiz
            productName.contains("mamón chino", ignoreCase = true) ||
                    productName.contains("mamon chino", ignoreCase = true) -> R.drawable.mamon_chino
            productName.contains("mango", ignoreCase = true) -> R.drawable.mango
            productName.contains("maracuyá", ignoreCase = true) ||
                    productName.contains("maracuya", ignoreCase = true) -> R.drawable.maracuya
            productName.contains("melón", ignoreCase = true) ||
                    productName.contains("melon", ignoreCase = true) -> R.drawable.melon
            productName.contains("ñame", ignoreCase = true) ||
                    productName.contains("name", ignoreCase = true) -> R.drawable.name
            productName.contains("ñampí", ignoreCase = true) ||
                    productName.contains("nampi", ignoreCase = true) -> R.drawable.nampi
            productName.contains("naranja", ignoreCase = true) -> R.drawable.naranja
            productName.contains("orégano", ignoreCase = true) ||
                    productName.contains("oregano", ignoreCase = true) -> R.drawable.oregano
            productName.contains("otoe", ignoreCase = true) -> R.drawable.otoe
            productName.contains("papaya", ignoreCase = true) -> R.drawable.papaya
            productName.contains("papa", ignoreCase = true) -> R.drawable.papa
            productName.contains("pepino", ignoreCase = true) -> R.drawable.pepino
            productName.contains("perejil", ignoreCase = true) -> R.drawable.perejil
            productName.contains("piña", ignoreCase = true) ||
                    productName.contains("pina", ignoreCase = true) -> R.drawable.pina
            productName.contains("plátano", ignoreCase = true) ||
                    productName.contains("platano", ignoreCase = true) -> R.drawable.platano
            productName.contains("remolacha", ignoreCase = true) -> R.drawable.remolacha
            productName.contains("repollo", ignoreCase = true) -> R.drawable.repollo
            productName.contains("tomate", ignoreCase = true) -> R.drawable.tomate
            productName.contains("yuca", ignoreCase = true) -> R.drawable.yuca
            productName.contains("zanahoria", ignoreCase = true) -> R.drawable.zanahoria
            else -> R.drawable.ic_store
        }
    }
}