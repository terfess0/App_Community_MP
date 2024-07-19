package com.terfess.comunidadmp.model

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.terfess.comunidadmp.callbacks.LoginWithEmlPsw
import com.terfess.comunidadmp.callbacks.RegisterUserCallback
import java.util.Date


class LoginRepository : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val usuariosRef = db.collection("users")

    //MODULO AUTENTICACION FIREBASE
    private var auth: FirebaseAuth = Firebase.auth

    fun crearCuenta(callback: RegisterUserCallback, data: User, password: String) {
        val email = data.userEmail

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { _ ->
                // El usuario fue creado exitosamente

                // Crear el nuevo usuario en la colección "users"
                val nuevoUsuarioId = usuariosRef.document().id

                val nuevoUsuario = hashMapOf(
                    "userName" to data.userName,
                    "userNumPosts" to "0",
                    "userEmail" to email,
                    "userJoinDate" to Timestamp(Date()),
                    "userRole" to "USER"
                )

                usuariosRef.document(nuevoUsuarioId)
                    .set(nuevoUsuario)
                    .addOnSuccessListener {
                        // new document firebase user create successfully
                        callback.onRegister()
                    }
                    .addOnFailureListener {
                        // fail
                        callback.onErrorRegist("INCOMPLETE")
                    }

            }
            .addOnFailureListener { e ->
                val errorMessage = when (e) {
                    is FirebaseAuthWeakPasswordException -> "WEAK_PASSWORD"
                    is FirebaseAuthInvalidCredentialsException -> "INVALID_EMAIL"
                    is FirebaseAuthUserCollisionException -> "EMAIL_ALREADY_IN_USE"
                    else -> "UNKNOWN_ERROR"
                }
                callback.onErrorRegist(errorMessage)
            }

    }


    fun loginEmailPassword(callback: LoginWithEmlPsw, email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    callback.onLogin()
                    Log.i("Login", "Inicio correctamente")
                } else {
                    //callback fail
                    when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> {
                            // Credenciales de autenticación inválidas, como una contraseña incorrecta
                            callback.onError("ERROR_CREDENTIAL")
                            Log.e("Login", "Error de credenciales")

                        }

                        is FirebaseAuthInvalidUserException -> {
                            // El usuario no existe o fue deshabilitado
                            callback.onError("NO_EXIST_OR_DISABLED")
                            Log.e("Login", "El usuario no existe o esta deshabilitado")
                        }

                        else -> {
                            callback.onError("ERROR_AUTH")
                            Log.e("Login", "Algun error desconocido")
                        }
                    }
                }
            }
    }
}