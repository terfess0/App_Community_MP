package com.terfess.comunidadmp.services

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class FirebaseAuth {
    private var auth: FirebaseAuth = Firebase.auth

    fun loginAnonymously() {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success anonymously
                    Log.d("OnLoginFirebase", "signInAnonymously:success")
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("OnLoginFirebase", "signInAnonymously:failure", task.exception)
                }
            }
    }

    fun sessionIsAnonymously(): Boolean {
        val currentUser = auth.currentUser
        return if (currentUser != null && currentUser.isAnonymous) {
            true
        } else {
            false
        }
    }

    fun isUserLoggedIn(): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser
        return currentUser != null
    }

    fun logOutSessionUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            auth.signOut()
            Log.d("OnLoginFirebase", "closeCurrentSession:success")
        }
    }

}