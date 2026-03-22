package com.agroapp

import android.app.Application
import com.agroapp.network.SessionManager

class AgroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
    }
}
