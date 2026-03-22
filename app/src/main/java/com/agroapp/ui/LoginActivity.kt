package com.agroapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.agroapp.R
import com.agroapp.network.SessionManager
import com.agroapp.viewmodel.AuthState
import com.agroapp.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Verificar si ya hay sesión iniciada
        if (SessionManager.isLoggedIn()) {
            goToMain()
            return
        }

        // Inicializar vistas
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        // Configurar botón de login
        btnLogin.setOnClickListener {
            val email = etEmail.text?.toString()?.trim() ?: ""
            val password = etPassword.text?.toString()?.trim() ?: ""

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        // Ir a registro
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Observar estado de autenticación
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnLogin.isEnabled = false
                }
                is AuthState.Success -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    Toast.makeText(this, "¡Bienvenido ${state.name}!", Toast.LENGTH_SHORT).show()
                    goToMain()
                }
                is AuthState.Error -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                }
            }
        }
    }

    private fun goToMain() {
        val role = SessionManager.getRole()
        val userType = SessionManager.getUserType()

        val intent = when {
            userType == "driver" -> {
                Intent(this, DriverActivity::class.java)
            }
            role == "vendedor" -> {
                Intent(this, VendorActivity::class.java)
            }
            else -> {
                Intent(this, HomeActivity::class.java)
            }
        }
        startActivity(intent)
        finish()
    }
}