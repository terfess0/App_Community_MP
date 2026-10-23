package com.terfess.comunidadmp.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.terfess.comunidadmp.notifications.PushNotificationHelper

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo FCM Token recibido: $token")
        PushNotificationHelper.onNewFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Mensaje push FCM recibido de: ${remoteMessage.from}")

        val data = remoteMessage.data
        val postId = data["postId"] ?: data["postDocumentId"]

        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: "Comunidad SA-MP"

        val body = remoteMessage.notification?.body
            ?: data["body"]
            ?: "Tienes una nueva actualización en la comunidad."

        PushNotificationHelper.showLocalNotification(
            context = applicationContext,
            title = title,
            body = body,
            postId = postId
        )
    }

    companion object {
        private const val TAG = "AppFirebaseMsgService"
    }
}
