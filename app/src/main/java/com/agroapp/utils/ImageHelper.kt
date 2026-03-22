package com.agroapp.utils

import android.widget.ImageView
import com.agroapp.R

object ImageHelper {

    // Mapa con los nombres EXACTOS que vienen del backend
    private val imageMap = mapOf(
        "Aguacate" to R.drawable.aguacate,
        "Limón" to R.drawable.limon,
        "Maracuyá" to R.drawable.maracuya,
        "Mamón chino" to R.drawable.mamon_chino,
        "Banano" to R.drawable.banano,
        "Mango" to R.drawable.mango,
        "Piña" to R.drawable.pina,
        "Naranja" to R.drawable.naranja,
        "Papaya" to R.drawable.papaya,
        "Melón" to R.drawable.melon,
        "Ají chombo" to R.drawable.ajichombo,
        "Brócoli" to R.drawable.brocoli,
        "Tomate" to R.drawable.tomate,
        "Cebolla" to R.drawable.cebolla,
        "Lechuga" to R.drawable.lechuga,
        "Zanahoria" to R.drawable.zanahoria,
        "Pepino" to R.drawable.pepino,
        "Repollo" to R.drawable.repollo,
        "Coliflor" to R.drawable.coliflor,
        "Berenjena" to R.drawable.berenjena,
        "Chayote" to R.drawable.chayote,
        "Ají dulce" to R.drawable.aji_dulce,
        "Remolacha" to R.drawable.remolacha,
        "Papa" to R.drawable.papa,
        "Otoe" to R.drawable.otoe,
        "Ñampí" to R.drawable.nampi,
        "Camote" to R.drawable.camote,
        "Yuca" to R.drawable.yuca,
        "Ñame" to R.drawable.name,
        "Maíz" to R.drawable.maiz,
        "Frijol" to R.drawable.frijol,
        "Lentejas" to R.drawable.lentejas,
        "Arroz" to R.drawable.arroz,
        "Culantro" to R.drawable.culantro,
        "Cebollina" to R.drawable.cebollina,
        "Perejil" to R.drawable.perejil,
        "Orégano" to R.drawable.oregano,
        "Ajo" to R.drawable.ajo,
        "Cilantro" to R.drawable.cilantro,
        "Café" to R.drawable.cafe,
        "Cacao" to R.drawable.cacao,
        "Caña de azúcar" to R.drawable.cana_de_azucar,
        "Plátano" to R.drawable.platano
    )

    fun loadImage(productName: String, imageView: ImageView) {
        val resourceId = imageMap[productName]
        if (resourceId != null) {
            imageView.setImageResource(resourceId)
            imageView.setBackgroundColor(0) // Quitar fondo
        } else {
            // Si no encuentra la imagen, poner color de fondo
            imageView.setImageDrawable(null)
            imageView.setBackgroundColor(imageView.context.getColor(R.color.green_light))
        }
    }
}
