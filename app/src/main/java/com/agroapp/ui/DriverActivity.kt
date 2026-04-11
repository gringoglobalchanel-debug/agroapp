package com.agroapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.agroapp.R
import com.agroapp.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DriverRegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_register)

        val etInviteCode = findViewById<TextInputEditText>(R.id.etInviteCode)
        val etFullName = findViewById<TextInputEditText>(R.id.etFullName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPhone = findViewById<TextInputEditText>(R.id.etPhone)
        val etAddress = findViewById<TextInputEditText>(R.id.etAddress)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegisterDriver)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        tvGoToLogin.setOnClickListener { finish() }

        btnRegister.setOnClickListener {
            val inviteCode = etInviteCode.text.toString().trim()
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            when {
                inviteCode.isEmpty() -> { Toast.makeText(this, "Ingresa el codigo de invitacion", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                fullName.isEmpty() -> { Toast.makeText(this, "Ingresa tu nombre completo", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                email.isEmpty() -> { Toast.makeText(this, "Ingresa tu correo", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                address.isEmpty() -> { Toast.makeText(this, "Ingresa tu direccion", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                password.length < 6 -> { Toast.makeText(this, "La contrasena debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                password != confirmPassword -> { Toast.makeText(this, "Las contrasenas no coinciden", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            }

            progressBar.visibility = View.VISIBLE
            btnRegister.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val body = mapOf(
                        "full_name" to fullName,
                        "email" to email,
                        "password" to password,
                        "phone" to phone,
                        "address" to address,
                        "invite_code" to inviteCode
                    )
                    val response = RetrofitClient.instance.registerDriver(body)
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnRegister.isEnabled = true
                        if (response.isSuccessful) {
                            Toast.makeText(this@DriverRegisterActivity, "Cuenta creada exitosamente. Ahora inicia sesion.", Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            val error = response.errorBody()?.string() ?: "Error desconocido"
                            val msg = try {
                                org.json.JSONObject(error).getString("error")
                            } catch (e: Exception) { error }
                            Toast.makeText(this@DriverRegisterActivity, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnRegister.isEnabled = true
                        Toast.makeText(this@DriverRegisterActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}