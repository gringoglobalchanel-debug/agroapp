package com.agroapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.agroapp.R
import com.agroapp.viewmodel.AuthState
import com.agroapp.viewmodel.AuthViewModel
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName      = findViewById<TextInputEditText>(R.id.etName)
        val etEmail     = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword  = findViewById<TextInputEditText>(R.id.etPassword)
        val etPhone     = findViewById<TextInputEditText>(R.id.etPhone)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvLogin     = findViewById<TextView>(R.id.tvLogin)

        btnRegister.setOnClickListener {
            val name     = etName.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val phone    = etPhone.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa nombre, email y contrase\u00f1a", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "La contrase\u00f1a debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register(email, password, name, phone, "", "cliente")
        }

        // ✅ Ir al login sin cerrar el stack
        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnRegister.isEnabled = false
                }
                is AuthState.Registered -> {
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true
                    Toast.makeText(this, "\u00a1Cuenta creada! Inicia sesi\u00f3n", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                is AuthState.Error -> {
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true
                }
            }
        }
    }
}