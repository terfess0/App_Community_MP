package com.terfess.comunidadmp.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.terfess.comunidadmp.callbacks.ChangeUserNameCallback
import com.terfess.comunidadmp.callbacks.GetUserCallback
import com.terfess.comunidadmp.classes.Utilities
import kotlinx.coroutines.newSingleThreadContext

class UserProvider : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text
    private val db = Firebase.firestore
    private val user = Firebase.auth.currentUser

    fun getUserAccount(callback: GetUserCallback) {
        var userData = User("", "", "", "", "", "", emptyList())

        db.collection("users")
            .whereEqualTo("userEmail", user?.email.toString())
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val data = querySnapshot.documents[0]

                    val name = data.getString("userName") ?: "unknown"
                    val numPosts = data.getString("userNumPosts") ?: ""
                    val email = data.getString("userEmail") ?: ""
                    val joinDate = data.getDate("userJoinDate") ?: ""
                    val photo = data.getString("userPhoto") ?: ""
                    val role = data.getString("userRole") ?: ""
                    val favorites = data.get("userFavorites") as? List<String> ?: emptyList()

                    //collect data
                    userData = User(name, numPosts, email, joinDate, photo, role, favorites)

                    // Logging user data
                    Log.i(
                        "UserData",
                        "Name: $name, NumPosts: $numPosts, Email: $email, JoinDate: $joinDate, Photo: $photo, Role: $role"
                    )
                    for (favorite in favorites) {
                        Log.i("UserFavorites", "Favorite: $favorite")
                    }

                    // Callback to pass the user data
                    callback.onGetUser(userData)
                }
                callback.onGetUser(userData)
            }.addOnFailureListener { e ->
                println("an error has ocurred on get imagen profile := $e")
            }
    }

    fun changeNameUser(
        callback: ChangeUserNameCallback,
        userEmail: String,
        newName: String
    ) {
        val usersCollection = db.collection("users")

        usersCollection.whereEqualTo("userName", newName)
            .get()
            .addOnSuccessListener { document ->
                if (document.isEmpty) {
                    usersCollection.whereEqualTo("userEmail", userEmail)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (querySnapshot.documents.isNotEmpty()) {
                                val userDocRef =
                                    usersCollection.document(querySnapshot.documents[0].id)
                                userDocRef.update("userName", newName).addOnSuccessListener {
                                    callback.onResult("SUCCESS")
                                }.addOnFailureListener {
                                    callback.onResult("ERROR")
                                }

                            } else {
                                callback.onResult("ERROR")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("error", "Hubo un error al ChangeName ::= $e")
                        }
                } else {
                    callback.onResult("ALREADY_IN_USE")
                }
            }
            .addOnFailureListener { e ->
                Log.e("error", "Hubo un error al comprobar uso de newName ::= $e")
            }


    }

    fun setUserCounterPost(emailUser: String, action:String) {
        val usersCollection = db.collection("users")

        usersCollection.whereEqualTo("userEmail", emailUser)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.documents.isNotEmpty()) {

                    val userCount = querySnapshot.documents[0].getString("userNumPosts").toString().toInt()
                    val newCount = if (action == "ADD"){
                        userCount + 1
                    }else{
                        userCount - 1
                    }
                    val userDocRef =
                        usersCollection.document(querySnapshot.documents[0].id)
                    userDocRef.update("userNumPosts", newCount.toString()).addOnSuccessListener {
                        Log.i("User", "The number of user post has been updated")
                    }.addOnFailureListener {
                        Log.e("User", "Error at updating usernumposts")
                    }

                } else {
                    Log.e("User", "Error at updating usernumposts - no entontrado")
                }
            }
            .addOnFailureListener { e ->
                Log.e("User", "Hubo un error al cambiar numpostsuser ::= $e")
            }
    }
}