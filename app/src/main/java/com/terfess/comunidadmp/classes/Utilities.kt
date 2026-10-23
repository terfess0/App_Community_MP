package com.terfess.comunidadmp.classes

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Utilities {
    private val db = FirebaseFirestore.getInstance()
    fun actualizarFotoUsuario(root: View, emailUsuario: String, nuevaFoto: String) {

        val usuarioRef = db.collection("users")

        // Realizar una consulta para obtener el documento del usuario por su nombre
        usuarioRef.whereEqualTo("userEmail", emailUsuario)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Encontrar el primer documento que coincida con el email de usuario
                    val document = querySnapshot.documents[0]
                    val userId = document.id

                    // Actualizar el campo userPhoto del documento del usuario
                    usuarioRef.document(userId)
                        .update("userPhoto", nuevaFoto)
                        .addOnSuccessListener {
                            // Campo userPhoto actualizado exitosamente
                            Snackbar.make(
                                root,
                                "Foto de perfil actualizada.",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener { e ->
                            // Error al actualizar el campo
                            println("Error al actualizar el campo 'userPhoto' Error: $e")
                            Snackbar.make(
                                root,
                                "Algo salió mal, prueba más tarde",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    // No se encontró ningún usuario con el nombre dado
                    println("No se encontró ningún usuario con el email '$emailUsuario'")
                }
            }
            .addOnFailureListener { e ->
                // Error al realizar la consulta
                println("Error al buscar el usuario con email '$emailUsuario': $e")
            }
    }

    fun getUserName(context: Context, userEmail: String) {
        val usuarioRef = db.collection("users")

        usuarioRef
            .whereNotEqualTo("userEmail", userEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userName = documents.documents[0].getString("userName") ?: ""
                    Utilities().saveSharedpref(context, "userName", userName)
                } else {
                    println("Usuario no encontrado o no tiene nombre.")
                }
            }
            .addOnFailureListener { exception ->
                // Manejar errores
                println("Error al obtener usuario de email: $exception")
            }
    }

    fun setUserName(emailUsuario: String, newName: String) {

        val usuarioRef = db.collection("users")

        // Realizar una consulta para obtener el documento del usuario por su nombre
        usuarioRef.whereEqualTo("userEmail", emailUsuario)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Encontrar el primer documento que coincida con el email de usuario
                    val document = querySnapshot.documents[0]
                    val userId = document.id

                    // Actualizar el campo userPhoto del documento del usuario
                    usuarioRef.document(userId)
                        .update("userName", newName)
                        .addOnSuccessListener {
                            // Campo userPhoto actualizado exitosamente
                            println("Campo 'userName' actualizado para el usuario con correo '$emailUsuario'")
                        }
                        .addOnFailureListener { e ->
                            // Error al actualizar el campo
                            println("Error al actualizar el campo 'userName' para el usuario '$emailUsuario': $e")
                        }
                } else {
                    // No se encontró ningún usuario con el nombre dado
                    println("No se encontró ningún usuario con el email '$emailUsuario'")
                }
            }
            .addOnFailureListener { e ->
                // Error al realizar la consulta
                println("Error al buscar el usuario con email '$emailUsuario': $e")
            }
    }


    // Función para guardar la fecha como string en SharedPreferences
    fun saveSharedpref(context: Context, claveDato: String, valor: String) {
        val sharedPreferences =
            context.getSharedPreferences("MiSharedPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(claveDato, valor)
        editor.apply()
        println("Se guardo: $claveDato")
    }

    // Función para obtener la fecha guardada en SharedPreferences
    fun getRegistSheredPref(claveDato: String, context: Context): String? {
        val sharedPreferences =
            context.getSharedPreferences("MiSharedPreferences", Context.MODE_PRIVATE)
        println("Se recupero de: $claveDato")
        return sharedPreferences.getString(claveDato, null)
    }

    fun makeSnackbar(root: View, message: String, colorRes: Int) {
        val snackbar = Snackbar.make(root, message, Snackbar.LENGTH_SHORT)

        // Personalizar la posición (arriba)
        val snackbarView = snackbar.view
        val params = snackbarView.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        snackbarView.layoutParams = params

        // Personalizar el color de fondo
        snackbarView.setBackgroundColor(ContextCompat.getColor(root.context, colorRes))

        snackbar.show()
    }

    fun showAlert(
        context: Context,
        title: String,
        message: String,
        textOpAcept: String,
        textOpCancel: String?,
        addOptCancel: Boolean,
        onAccept: () -> Unit,
        onCancel: () -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(textOpAcept) { dialog, _ ->
                onAccept() // Acción al aceptar
                dialog.dismiss() // Cierra la alerta
            }

        if (addOptCancel) {
            builder.setNegativeButton(textOpCancel) { dialog, _ ->
                onCancel() // Acción al cancelar
                dialog.dismiss() // Cierra la alerta
            }
        }

        builder.create()
        builder.show()
    }

    fun getCurrentTime(): String {
        val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
        val fechaActual = Date()
        return formato.format(fechaActual)
    }

    fun hideKeyboard(context: Context, editTextBody: EditText) {
        val imm =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editTextBody.windowToken, 0)
    }

    fun isValidLink(link: String): Boolean {
        // Patrón estricto para validar enlaces
        val urlPattern = "^(https?://)([\\w-]+\\.)+[\\w-]{2,}(\\/[\\w-./?%&=]*)?$".toRegex()
        return link.trim().isNotEmpty() && urlPattern.matches(link)
    }

    fun copyText(textCopy: String, context: Context) {
        // Obtener el servicio de portapapeles
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Crear un ClipData con el texto a copiar
        val clip = ClipData.newPlainText("Texto copiado", textCopy)

        // Copiar al portapapeles
        clipboard.setPrimaryClip(clip)

        // Mostrar un mensaje al usuario (opcional)
        Toast.makeText(context, "Texto copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val services = activityManager.getRunningServices(Int.MAX_VALUE)

        for (service in services) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    fun formatCustomDate(rawDate: Any?): String {
        if (rawDate == null) return "00/00/0000"
        val targetFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        return try {
            when (rawDate) {
                is com.google.firebase.Timestamp -> targetFormat.format(rawDate.toDate())
                is Date -> targetFormat.format(rawDate)
                is Long -> targetFormat.format(Date(rawDate))
                is String -> {
                    val str = rawDate.trim()
                    if (str.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) return str
                    val formats = listOf(
                        "yyyy-MM-dd",
                        "yyyy-MM-dd HH:mm:ss",
                        "EEE MMM dd HH:mm:ss zzz yyyy",
                        "EEE MMM dd HH:mm:ss yyyy"
                    )
                    for (fmt in formats) {
                        try {
                            val parsed = SimpleDateFormat(fmt, Locale.US).parse(str)
                            if (parsed != null) return targetFormat.format(parsed)
                        } catch (_: Exception) {}
                    }
                    if (str.length >= 10 && str[4] == '-' && str[7] == '-') {
                        val y = str.substring(0, 4)
                        val m = str.substring(5, 7)
                        val d = str.substring(8, 10)
                        return "$d/$m/$y"
                    }
                    str
                }
                else -> rawDate.toString()
            }
        } catch (e: Exception) {
            "00/00/0000"
        }
    }

}