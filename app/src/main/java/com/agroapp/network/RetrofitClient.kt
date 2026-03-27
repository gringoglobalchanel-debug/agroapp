package com.agroapp.network

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL =  "https://agroapp-backend-bffq.onrender.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

object SessionManager {
    private const val PREF_NAME = "agro_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "email"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_USER_ADDRESS = "user_address"
    private const val KEY_USER_PHONE = "phone"
    private const val KEY_USER_TYPE = "user_type"

    // Nuevas keys para ubicación de entrega
    private const val KEY_DELIVERY_LAT = "delivery_lat"
    private const val KEY_DELIVERY_LNG = "delivery_lng"
    private const val KEY_DELIVERY_ADDRESS = "delivery_address"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(
        token: String,
        userId: String,
        name: String,
        email: String,
        role: String,
        address: String?,
        userType: String = "cliente"
    ) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_ROLE, role)
            .putString(KEY_USER_ADDRESS, address ?: "")
            .putString(KEY_USER_TYPE, userType)
            .apply()
    }

    fun updateProfile(name: String, phone: String, address: String) {
        prefs.edit()
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_PHONE, phone)
            .putString(KEY_USER_ADDRESS, address)
            .apply()
    }

    // ==================== MÉTODOS PARA UBICACIÓN DE ENTREGA ====================

    fun saveDeliveryLocation(lat: Double, lng: Double, address: String) {
        prefs.edit()
            .putFloat(KEY_DELIVERY_LAT, lat.toFloat())
            .putFloat(KEY_DELIVERY_LNG, lng.toFloat())
            .putString(KEY_DELIVERY_ADDRESS, address)
            .apply()
    }

    fun getDeliveryLatitude(): Double {
        return prefs.getFloat(KEY_DELIVERY_LAT, 0f).toDouble()
    }

    fun getDeliveryLongitude(): Double {
        return prefs.getFloat(KEY_DELIVERY_LNG, 0f).toDouble()
    }

    fun getDeliveryAddress(): String {
        return prefs.getString(KEY_DELIVERY_ADDRESS, "") ?: ""
    }

    fun clearDeliveryLocation() {
        prefs.edit()
            .remove(KEY_DELIVERY_LAT)
            .remove(KEY_DELIVERY_LNG)
            .remove(KEY_DELIVERY_ADDRESS)
            .apply()
    }

    // ==================== MÉTODOS EXISTENTES ====================

    fun getToken(): String = "Bearer ${prefs.getString(KEY_TOKEN, "") ?: ""}"
    fun getUserId(): String = prefs.getString(KEY_USER_ID, "") ?: ""
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    fun getRole(): String = prefs.getString(KEY_USER_ROLE, "cliente") ?: "cliente"
    fun getAddress(): String = prefs.getString(KEY_USER_ADDRESS, "") ?: ""
    fun getPhone(): String = prefs.getString(KEY_USER_PHONE, "") ?: ""
    fun getUserType(): String = prefs.getString(KEY_USER_TYPE, "cliente") ?: "cliente"
    fun isVendor(): Boolean = getRole() == "vendedor"
    fun isDriver(): Boolean = getUserType() == "driver"
    fun isLoggedIn(): Boolean = prefs.getString(KEY_TOKEN, null) != null

    fun logout() = prefs.edit().clear().apply()
}