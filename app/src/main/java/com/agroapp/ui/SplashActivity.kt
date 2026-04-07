package com.agroapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.agroapp.R
import com.agroapp.network.SessionManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val ivLogo = findViewById<ImageView>(R.id.ivLogo)

        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            fillAfter = true
        }

        val scaleUp = ScaleAnimation(
            0.90f, 1f, 0.90f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            fillAfter = true
        }

        val animSet = AnimationSet(false).apply {
            addAnimation(fadeIn)
            addAnimation(scaleUp)
            fillAfter = true
        }

        ivLogo.alpha = 1f
        ivLogo.startAnimation(animSet)

        ivLogo.postDelayed({
            val fadeOut = AlphaAnimation(1f, 0f).apply {
                duration = 400
                interpolator = DecelerateInterpolator()
                fillAfter = true
            }
            ivLogo.startAnimation(fadeOut)
            ivLogo.postDelayed({ navigateNext() }, 400)
        }, 2000)
    }

    private fun navigateNext() {
        val intent = if (SessionManager.isLoggedIn()) {
            when {
                SessionManager.getRole() == "admin"    -> Intent(this, AdminActivity::class.java)
                SessionManager.getRole() == "vendedor" -> Intent(this, VendorActivity::class.java)
                SessionManager.isDriver()              -> Intent(this, DriverActivity::class.java)
                else                                   -> Intent(this, HomeActivity::class.java)
            }
        } else {
            // ✅ Primera vez → Registro. Desde registro pueden ir al login
            Intent(this, RegisterActivity::class.java)
        }

        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { }
}