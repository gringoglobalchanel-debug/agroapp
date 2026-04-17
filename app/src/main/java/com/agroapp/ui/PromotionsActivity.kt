package com.agroapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.agroapp.R
import com.agroapp.network.AppBanner
import com.agroapp.network.RetrofitClient
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class PromotionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_promotions)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Promociones"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Banners estáticos del carrusel (siempre visibles)
        setupCarouselBanners()

        // Banners dinámicos del backend para la sección "Más promociones"
        loadBackendBanners()
    }

    // ✅ Banners del carrusel ESTÁTICOS (drawables locales)
    private fun setupCarouselBanners() {
        // Configurar imágenes estáticas para los 3 banners del carrusel
        val ivCarousel1 = findViewById<ImageView>(R.id.ivCarousel1)
        val ivCarousel2 = findViewById<ImageView>(R.id.ivCarousel2)
        val ivCarousel3 = findViewById<ImageView>(R.id.ivCarousel3)

        // Asignar drawables locales
        ivCarousel1.setImageResource(R.drawable.banner_envio_gratis)
        ivCarousel2.setImageResource(R.drawable.banner_yappi)
        ivCarousel3.setImageResource(R.drawable.banner_comparte)

        // Configurar clicks
        findViewById<CardView>(R.id.cardCarousel1).setOnClickListener {
            startActivity(Intent(this, ProductsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardCarousel2).setOnClickListener {
            startActivity(Intent(this, ProductsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardCarousel3).setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Descarga Grun y recibe tus productos frescos a domicilio!")
            }
            startActivity(Intent.createChooser(shareIntent, "Compartir Grun"))
        }
    }

    // ✅ Banners del backend para la sección "Más promociones"
    private fun loadBackendBanners() {
        val ivBanner1 = findViewById<ImageView>(R.id.ivBanner1)
        val ivBanner2 = findViewById<ImageView>(R.id.ivBanner2)
        val ivBanner3 = findViewById<ImageView>(R.id.ivBanner3)
        val cardBanner1 = findViewById<CardView>(R.id.cardBanner1)
        val cardBanner2 = findViewById<CardView>(R.id.cardBanner2)
        val cardBanner3 = findViewById<CardView>(R.id.cardBanner3)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getBanners()
                if (response.isSuccessful) {
                    val banners = response.body() ?: return@launch

                    // Mapear banners por slot (1,2,3) a los cards de abajo
                    banners.forEach { banner ->
                        when (banner.slot) {
                            1 -> {
                                if (!banner.imageUrl.isNullOrEmpty()) {
                                    Glide.with(this@PromotionsActivity)
                                        .load(banner.imageUrl)
                                        .centerCrop()
                                        .into(ivBanner1)
                                }
                                cardBanner1.setOnClickListener { handleBannerClick(banner) }
                            }
                            2 -> {
                                if (!banner.imageUrl.isNullOrEmpty()) {
                                    Glide.with(this@PromotionsActivity)
                                        .load(banner.imageUrl)
                                        .centerCrop()
                                        .into(ivBanner2)
                                }
                                cardBanner2.setOnClickListener { handleBannerClick(banner) }
                            }
                            3 -> {
                                if (!banner.imageUrl.isNullOrEmpty()) {
                                    Glide.with(this@PromotionsActivity)
                                        .load(banner.imageUrl)
                                        .centerCrop()
                                        .into(ivBanner3)
                                }
                                cardBanner3.setOnClickListener { handleBannerClick(banner) }
                            }
                        }
                    }
                } else {
                    Log.e("PromotionsActivity", "Error al cargar banners: ${response.code()}")
                    // Mantener imágenes estáticas por defecto si falla la carga
                    setDefaultBackendBanners()
                }
            } catch (e: Exception) {
                Log.e("PromotionsActivity", "Error cargando banners: ${e.message}")
                // Mantener imágenes estáticas por defecto si hay error
                setDefaultBackendBanners()
            }
        }
    }

    // Imágenes por defecto para los banners del backend (en caso de error)
    private fun setDefaultBackendBanners() {
        val ivBanner1 = findViewById<ImageView>(R.id.ivBanner1)
        val ivBanner2 = findViewById<ImageView>(R.id.ivBanner2)
        val ivBanner3 = findViewById<ImageView>(R.id.ivBanner3)

        ivBanner1.setImageResource(R.drawable.banner_envio_gratis)
        ivBanner2.setImageResource(R.drawable.banner_yappi)
        ivBanner3.setImageResource(R.drawable.banner_comparte)

        findViewById<CardView>(R.id.cardBanner1).setOnClickListener {
            startActivity(Intent(this, ProductsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardBanner2).setOnClickListener {
            startActivity(Intent(this, ProductsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardBanner3).setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Descarga Grun y recibe tus productos frescos a domicilio!")
            }
            startActivity(Intent.createChooser(shareIntent, "Compartir Grun"))
        }
    }

    private fun handleBannerClick(banner: AppBanner) {
        when {
            !banner.title.isNullOrEmpty() -> {
                startActivity(Intent(this, ProductsActivity::class.java)
                    .putExtra("SEARCH_QUERY", banner.title))
            }
            !banner.linkUrl.isNullOrEmpty() -> {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(banner.linkUrl)))
                } catch (e: Exception) {
                    Toast.makeText(this, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
                }
            }
            else -> startActivity(Intent(this, ProductsActivity::class.java))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}