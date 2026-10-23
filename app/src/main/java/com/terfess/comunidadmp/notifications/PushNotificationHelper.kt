package com.terfess.comunidadmp.notifications

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import com.onesignal.OneSignal
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.view.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

object PushNotificationHelper {

    private const val TAG = "PushNotificationHelper"
    const val CHANNEL_ID = "comunidad_samp_notifications"
    private const val CHANNEL_NAME = "Comunidad SA-MP Notificaciones"

    @Volatile
    var isOneSignalInitialized: Boolean = false
        private set

    /**
     * Valida si el App ID de OneSignal tiene formato UUID válido y no es el placeholder.
     */
    fun isValidOneSignalAppId(appId: String?): Boolean {
        if (appId.isNullOrBlank() || appId == "YOUR_ONESIGNAL_APP_ID") return false
        return try {
            UUID.fromString(appId.trim())
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Inicializa OneSignal y sus listeners de forma segura.
     * Si el App ID no es válido o falla, la app NO crashea y sigue funcionando normalmente.
     */
    fun init(context: Context) {
        val appId = try {
            context.getString(R.string.onesignal_app_id).trim()
        } catch (e: Exception) {
            ""
        }

        if (!isValidOneSignalAppId(appId)) {
            Log.w(TAG, "OneSignal no inicializado: falta configurar un App ID UUID válido en strings.xml (actual: '$appId').")
            isOneSignalInitialized = false
            return
        }

        try {
            OneSignal.initWithContext(context.applicationContext, appId)
            isOneSignalInitialized = true

            // Listener para cuando el usuario hace clic en una notificación de OneSignal
            OneSignal.Notifications.addClickListener(object : com.onesignal.notifications.INotificationClickListener {
                override fun onClick(event: com.onesignal.notifications.INotificationClickEvent) {
                    val data = event.notification.additionalData
                    val postId = data?.optString("postId")
                        ?: data?.optString("postDocumentId")

                    if (!postId.isNullOrBlank()) {
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("postDocumentId", postId.toString())
                        }
                        context.startActivity(intent)
                    }
                }
            })
            Log.d(TAG, "OneSignal inicializado con éxito.")
        } catch (t: Throwable) {
            isOneSignalInitialized = false
            Log.e(TAG, "Excepción capturada de forma segura al inicializar OneSignal: ${t.message}")
        }
    }

    /**
     * Solicita permisos de notificaciones (Android 13+).
     * Si OneSignal está activo lo hace vía SDK; si no, lo hace nativamente con Android.
     */
    fun requestNotificationPermission(context: Context) {
        if (isOneSignalInitialized) {
            try {
                CoroutineScope(Dispatchers.Main).launch {
                    OneSignal.Notifications.requestPermission(true)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error al solicitar permisos con OneSignal: ${t.message}")
            }
        } else {
            // Permiso nativo Android 13+ (POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context is Activity) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        context,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        101
                    )
                }
            }
        }
    }

    /**
     * Sincroniza y asocia el usuario actual autenticado con OneSignal y FCM,
     * persistiendo sus identificadores en su documento de Firestore.
     */
    fun syncUserPushTokens() {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null || currentUser.isAnonymous) {
            Log.d(TAG, "Usuario es invitado o no autenticado, no se sincronizan tokens de push.")
            return
        }

        val uid = currentUser.uid
        val email = currentUser.email ?: return

        // 1. Vincular en OneSignal sólo si fue inicializado con éxito
        if (isOneSignalInitialized) {
            try {
                OneSignal.login(uid)
                Log.d(TAG, "OneSignal logueado con external_id: $uid")
            } catch (t: Throwable) {
                Log.e(TAG, "Error vinculando usuario en OneSignal: ${t.message}")
            }
        }

        // 2. Obtener Token de FCM y guardarlo en Firestore
        try {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { fcmToken ->
                    if (!fcmToken.isNullOrBlank()) {
                        val oneSignalId = if (isOneSignalInitialized) uid else ""
                        updateTokensInFirestore(email, fcmToken, oneSignalId)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error obteniendo FCM Token: ${e.message}")
                }
        } catch (t: Throwable) {
            Log.e(TAG, "Error invocando FirebaseMessaging: ${t.message}")
        }
    }

    /**
     * Guarda el token de FCM y el ID de OneSignal en la colección 'users'.
     */
    fun updateTokensInFirestore(email: String, fcmToken: String, oneSignalId: String = "") {
        val db = Firebase.firestore
        db.collection("users")
            .whereEqualTo("userEmail", email)
            .get()
            .addOnSuccessListener { query ->
                for (doc in query.documents) {
                    val updateMap = mutableMapOf<String, Any>(
                        "fcmToken" to fcmToken,
                        "lastTokenUpdate" to FieldValue.serverTimestamp()
                    )
                    if (oneSignalId.isNotBlank()) {
                        updateMap["oneSignalId"] = oneSignalId
                    }
                    doc.reference.update(updateMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "Push tokens actualizados en Firestore para: $email")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error actualizando push tokens en Firestore: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error buscando usuario para actualizar push tokens: ${e.message}")
            }
    }

    /**
     * Actualiza el FCM token cuando se genera un nuevo token en FirebaseMessagingService.
     */
    fun onNewFcmToken(fcmToken: String) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null && !currentUser.isAnonymous && !currentUser.email.isNullOrBlank()) {
            val oneSignalId = if (isOneSignalInitialized) currentUser.uid else ""
            updateTokensInFirestore(currentUser.email!!, fcmToken, oneSignalId)
        }
    }

    /**
     * Cierra la sesión en OneSignal cuando el usuario hace logout en la app.
     */
    fun onUserLogout() {
        if (isOneSignalInitialized) {
            try {
                OneSignal.logout()
                Log.d(TAG, "OneSignal sesión cerrada.")
            } catch (t: Throwable) {
                Log.e(TAG, "Error al cerrar sesión de OneSignal: ${t.message}")
            }
        }
    }

    /**
     * Muestra una notificación visual en la barra de notificaciones del dispositivo.
     */
    fun showLocalNotification(
        context: Context,
        title: String,
        body: String,
        postId: String? = null
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!postId.isNullOrBlank()) {
                putExtra("postDocumentId", postId)
            }
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            pendingIntentFlag
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "No se tienen permisos POST_NOTIFICATIONS para mostrar la notificación: ${e.message}")
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de publicaciones, comentarios y anuncios de SA-MP"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }
}
