package com.agroapp.ui

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.agroapp.R
import com.agroapp.viewmodel.AuthState
import com.agroapp.viewmodel.AuthViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupViewModel()
    }

    override fun onBackPressed() {
        // En login, sí puede salir de la app
        finishAffinity()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)

        btnLogin.setOnClickListener { performLogin() }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        viewModel.authState.observe(this, Observer { state ->
            when (state) {
                is AuthState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnLogin.isEnabled = false
                    tvError.visibility = View.GONE
                }
                is AuthState.Success -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true

                    when {
                        state.role == "admin" -> {
                            Toast.makeText(this, "Bienvenido Administrador", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, AdminActivity::class.java))
                        }
                        state.role == "vendedor" -> {
                            Toast.makeText(this, "Bienvenido Vendedor", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, VendorActivity::class.java))
                        }
                        state.userType == "driver" -> {
                            Toast.makeText(this, "Bienvenido Repartidor", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, DriverActivity::class.java))
                        }
                        else -> {
                            Toast.makeText(this, "Bienvenido ${state.name}", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, HomeActivity::class.java))
                        }
                    }
                    finish()
                }
                is AuthState.Error -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    tvError.text = state.message
                    tvError.visibility = View.VISIBLE
                }
                else -> {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                }
            }
        })
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (TextUtils.isEmpty(email)) {
            etEmail.error = "Ingresa tu email"
            etEmail.requestFocus()
            return
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.error = "Ingresa tu contraseña"
            etPassword.requestFocus()
            return
        }

        viewModel.login(email, password)
    }

    override fun onResume() {
        super.onResume()
        viewModel.resetState()
    }
}