package com.terfess.comunidadmp

import android.app.Application
import com.terfess.comunidadmp.notifications.PushNotificationHelper

class AppCommunityApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inicializar OneSignal y push notification handlers
        PushNotificationHelper.init(this)
    }
}
