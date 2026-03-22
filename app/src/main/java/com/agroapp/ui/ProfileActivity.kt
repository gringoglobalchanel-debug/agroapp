package com.agroapp.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.agroapp.R
import com.agroapp.network.SessionManager
import com.agroapp.viewmodel.ProfileState
import com.agroapp.viewmodel.ProfileViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class ProfileActivity : AppCompatActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        if (!SessionManager.isLoggedIn()) {
            goToLogin()
            return
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val tvEditPhoto = findViewById<TextView>(R.id.tvEditPhoto)
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvPhone = findViewById<TextView>(R.id.tvPhone)
        val tvAddress = findViewById<TextView>(R.id.tvAddress)
        val btnEditProfile = findViewById<MaterialButton>(R.id.btnEditProfile)
        val btnChangePassword = findViewById<MaterialButton>(R.id.btnChangePassword)
        val btnMyOrders = findViewById<MaterialButton>(R.id.btnMyOrders)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)

        toolbar.setNavigationOnClickListener { finish() }

        tvName.text = SessionManager.getUserName()
        tvEmail.text = SessionManager.getUserEmail()
        tvPhone.text = SessionManager.getPhone().ifEmpty { "No especificado" }
        tvAddress.text = SessionManager.getAddress().ifEmpty { "No especificada" }

        viewModel.loadProfile()
        viewModel.profile.observe(this, Observer { profile ->
            profile?.let {
                tvName.text = it.fullName
                tvEmail.text = it.email
                tvPhone.text = it.phone ?: "No especificado"
                tvAddress.text = it.address ?: "No especificada"
            }
        })

        viewModel.updateState.observe(this, Observer { state ->
            when (state) {
                is ProfileState.Success -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.loadProfile()
                }
                is ProfileState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        })

        tvEditPhoto.setOnClickListener {
            Toast.makeText(this, "Función próximamente", Toast.LENGTH_SHORT).show()
        }

        btnEditProfile.setOnClickListener {
            showEditProfileDialog(
                currentName = tvName.text.toString(),
                currentPhone = tvPhone.text.toString(),
                currentAddress = tvAddress.text.toString()
            )
        }

        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        btnMyOrders.setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }

        btnLogout.setOnClickListener {
            SessionManager.logout()
            goToLogin()
            finishAffinity()
        }
    }

    private fun showEditProfileDialog(
        currentName: String,
        currentPhone: String,
        currentAddress: String
    ) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_edit_profile, null)

        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val etAddress = dialogView.findViewById<EditText>(R.id.etAddress)

        etName.setText(currentName)
        etPhone.setText(if (currentPhone == "No especificado") "" else currentPhone)
        etAddress.setText(if (currentAddress == "No especificada") "" else currentAddress)

        AlertDialog.Builder(this)
            .setTitle("Editar perfil")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val address = etAddress.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.updateProfile(name, phone, address)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_change_password, null)

        val etCurrentPassword = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(this)
            .setTitle("Cambiar contraseña")
            .setView(dialogView)
            .setPositiveButton("Cambiar") { _, _ ->
                val current = etCurrentPassword.text.toString().trim()
                val newPass = etNewPassword.text.toString().trim()
                val confirm = etConfirmPassword.text.toString().trim()

                when {
                    current.isEmpty() || newPass.isEmpty() || confirm.isEmpty() ->
                        Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                    newPass.length < 6 ->
                        Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                    newPass != confirm ->
                        Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                    else -> viewModel.changePassword(current, newPass)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}