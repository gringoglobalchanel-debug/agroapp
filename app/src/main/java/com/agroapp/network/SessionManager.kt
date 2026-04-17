package com.agroapp.network

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

object SessionManager {

    private const val PREF_NAME = "agro_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_TYPE = "user_type"
    private const val KEY_ROLE = "role"
    private const val KEY_ADDRESS = "address"
    private const val KEY_AVATAR_URL = "avatar_url"
    private const val KEY_DELIVERY_LAT = "delivery_lat"
    private const val KEY_DELIVERY_LNG = "delivery_lng"
    private const val KEY_DELIVERY_ADDRESS = "delivery_address"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // ✅ Auth
    fun saveSession(token: String, userId: Int, name: String, email: String, userType: String, role: String = "") {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_TYPE, userType)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getString(KEY_TOKEN, null)?.isNotEmpty() == true

    fun getToken(): String = "Bearer ${prefs.getString(KEY_TOKEN, "") ?: ""}"

    fun getRawToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""

    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""

    fun getUserType(): String = prefs.getString(KEY_USER_TYPE, "") ?: ""

    fun getRole(): String = prefs.getString(KEY_ROLE, "") ?: ""

    fun isDriver(): Boolean = getUserType() == "driver"

    fun logout() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_TYPE)
            .remove(KEY_ROLE)
            .apply()
        clearDeliveryLocation()
    }

    // ✅ Perfil
    fun getAddress(): String = prefs.getString(KEY_ADDRESS, "") ?: ""

    fun saveAddress(address: String) {
        prefs.edit().putString(KEY_ADDRESS, address).apply()
    }

    fun getAvatarUrl(): String = prefs.getString(KEY_AVATAR_URL, "") ?: ""

    fun saveAvatarUrl(url: String) {
        prefs.edit().putString(KEY_AVATAR_URL, url).apply()
    }

    // ✅ Ubicacion de entrega (mapa)
    fun saveDeliveryLocation(lat: Double, lng: Double, address: String) {
        prefs.edit()
            .putFloat(KEY_DELIVERY_LAT, lat.toFloat())
            .putFloat(KEY_DELIVERY_LNG, lng.toFloat())
            .putString(KEY_DELIVERY_ADDRESS, address)
            .apply()
    }

    fun getDeliveryLatitude(): Double = prefs.getFloat(KEY_DELIVERY_LAT, 0f).toDouble()

    fun getDeliveryLongitude(): Double = prefs.getFloat(KEY_DELIVERY_LNG, 0f).toDouble()

    fun getDeliveryAddress(): String = prefs.getString(KEY_DELIVERY_ADDRESS, "") ?: ""

    fun clearDeliveryLocation() {
        prefs.edit()
            .remove(KEY_DELIVERY_LAT)
            .remove(KEY_DELIVERY_LNG)
            .remove(KEY_DELIVERY_ADDRESS)
            .apply()
    }
}
