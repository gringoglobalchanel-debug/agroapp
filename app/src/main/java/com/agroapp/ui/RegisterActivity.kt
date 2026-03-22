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

class RegisterActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Usar EditText normal (NO TextInputEditText)
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etAddress = findViewById<EditText>(R.id.etAddress)

        // NUEVO: Selector de rol
        val rgUserType = findViewById<RadioGroup>(R.id.rgUserType)
        val rbClient = findViewById<RadioButton>(R.id.rbClient)
        val rbDriver = findViewById<RadioButton>(R.id.rbDriver)

        // Usar Button normal (NO MaterialButton)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val address = etAddress.text.toString().trim()

            // Obtener el tipo de usuario seleccionado
            val userType = if (rgUserType.checkedRadioButtonId == R.id.rbDriver) {
                "driver"
            } else {
                "cliente"
            }

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register(email, password, name, phone, address, userType)
        }

        tvLogin.setOnClickListener { finish() }

        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> progressBar.visibility = View.VISIBLE
                is AuthState.Registered -> {
                    progressBar.visibility = View.GONE
                    val userTypeMsg = if (rgUserType.checkedRadioButtonId == R.id.rbDriver)
                        "¡Repartidor registrado!" else "¡Cuenta creada!"
                    Toast.makeText(this, userTypeMsg, Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                is AuthState.Error -> {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> progressBar.visibility = View.GONE
            }
        }
    }
}