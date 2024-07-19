package com.terfess.comunidadmp.classes

import android.content.Context
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore

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



}