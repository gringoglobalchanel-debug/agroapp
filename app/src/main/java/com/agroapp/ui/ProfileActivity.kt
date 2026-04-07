package com.agroapp.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.agroapp.R
import com.agroapp.network.SessionManager
import com.agroapp.viewmodel.ProfileState
import com.agroapp.viewmodel.ProfileViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ProfileActivity : AppCompatActivity() {

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var ivAvatar: ShapeableImageView

    // Launcher para galería
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processAndUploadImage(it) }
    }

    // Launcher para cámara
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { uploadBitmap(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        if (!SessionManager.isLoggedIn()) {
            goToLogin()
            return
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val tvEditPhoto = findViewById<TextView>(R.id.tvEditPhoto)
        ivAvatar = findViewById(R.id.ivAvatar)
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvPhone = findViewById<TextView>(R.id.tvPhone)
        val tvAddress = findViewById<TextView>(R.id.tvAddress)
        val btnEditProfile = findViewById<MaterialButton>(R.id.btnEditProfile)
        val btnChangePassword = findViewById<MaterialButton>(R.id.btnChangePassword)
        val btnMyOrders = findViewById<MaterialButton>(R.id.btnMyOrders)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)

        toolbar.setNavigationOnClickListener { finish() }

        // Datos locales inmediatos (sin esperar red)
        tvName.text = SessionManager.getUserName()
        tvEmail.text = SessionManager.getUserEmail()
        tvPhone.text = SessionManager.getPhone().ifEmpty { "No especificado" }
        tvAddress.text = SessionManager.getAddress().ifEmpty { "No especificada" }

        // ✅ Carga avatar persistido INMEDIATAMENTE sin esperar la red
        val cachedAvatar = SessionManager.getAvatarUrl()
        if (cachedAvatar.isNotEmpty()) {
            loadAvatarFromUrl(cachedAvatar)
        }

        // Carga perfil desde servidor (actualiza si hay cambios)
        viewModel.loadProfile()

        // Observa perfil completo
        viewModel.profile.observe(this, Observer { profile ->
            profile?.let {
                tvName.text = it.fullName
                tvEmail.text = it.email
                tvPhone.text = it.phone ?: "No especificado"
                tvAddress.text = it.address ?: "No especificada"
            }
        })

        // ✅ Observa avatar URL separado — se actualiza al subir foto o al cargar perfil
        viewModel.avatarUrl.observe(this, Observer { url ->
            if (!url.isNullOrEmpty()) {
                loadAvatarFromUrl(url)
            }
        })

        // Observa resultados de acciones (editar perfil, cambiar contraseña)
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

        tvEditPhoto.setOnClickListener { showPhotoOptions() }
        ivAvatar.setOnClickListener { showPhotoOptions() }

        btnEditProfile.setOnClickListener {
            showEditProfileDialog(
                currentName = tvName.text.toString(),
                currentPhone = tvPhone.text.toString(),
                currentAddress = tvAddress.text.toString()
            )
        }

        btnChangePassword.setOnClickListener { showChangePasswordDialog() }

        btnMyOrders.setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }

        btnLogout.setOnClickListener {
            SessionManager.logout()
            goToLogin()
            finishAffinity()
        }
    }

    // ==================== HELPERS DE AVATAR ====================

    // ✅ Carga con skipMemoryCache para forzar imagen nueva tras subida
    private fun loadAvatarFromUrl(url: String) {
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.ic_default_avatar)
            .error(R.drawable.ic_default_avatar)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .circleCrop()
            .into(ivAvatar)
    }

    // ==================== FOTO DE PERFIL ====================

    private fun showPhotoOptions() {
        AlertDialog.Builder(this)
            .setTitle("Foto de perfil")
            .setItems(arrayOf("📷 Tomar foto", "🖼️ Elegir de galería")) { _, which ->
                when (which) {
                    0 -> cameraLauncher.launch(null)
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun processAndUploadImage(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                withContext(Dispatchers.Main) {
                    uploadBitmap(bitmap)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Error al leer la imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadBitmap(bitmap: Bitmap) {
        // Preview inmediato mientras sube
        ivAvatar.setImageBitmap(bitmap)
        Toast.makeText(this, "Subiendo foto...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resized = resizeBitmap(bitmap, 400)
                val outputStream = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val imageBytes = outputStream.toByteArray()
                val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                withContext(Dispatchers.Main) {
                    // ✅ Delega al ViewModel — él guarda en SessionManager y actualiza LiveData
                    viewModel.uploadAvatar(base64) { success, avatarUrl ->
                        if (success && !avatarUrl.isNullOrEmpty()) {
                            Toast.makeText(this@ProfileActivity, "✅ Foto actualizada", Toast.LENGTH_SHORT).show()
                            // avatarUrl LiveData ya se actualizó en el ViewModel, Glide recarga solo
                        } else {
                            Toast.makeText(this@ProfileActivity, "Error al guardar la foto", Toast.LENGTH_LONG).show()
                            // Restaura el avatar anterior desde SessionManager
                            val saved = SessionManager.getAvatarUrl()
                            if (saved.isNotEmpty()) loadAvatarFromUrl(saved)
                            else ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    val saved = SessionManager.getAvatarUrl()
                    if (saved.isNotEmpty()) loadAvatarFromUrl(saved)
                    else ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                }
            }
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val ratio = width.toFloat() / height.toFloat()
        return if (width > height) {
            Bitmap.createScaledBitmap(bitmap, maxSize, (maxSize / ratio).toInt(), true)
        } else {
            Bitmap.createScaledBitmap(bitmap, (maxSize * ratio).toInt(), maxSize, true)
        }
    }

    // ==================== DIALOGS ====================

    private fun showEditProfileDialog(currentName: String, currentPhone: String, currentAddress: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)
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
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)
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
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}