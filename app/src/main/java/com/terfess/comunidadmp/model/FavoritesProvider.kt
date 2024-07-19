package com.terfess.comunidadmp.model

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.terfess.comunidadmp.callbacks.posts.FavoritesFirestore
import com.terfess.comunidadmp.callbacks.posts.GetPostCallback
import com.terfess.comunidadmp.callbacks.GetUserCallback

class FavoritesProvider(private val callback: GetPostCallback) : ViewModel(), GetUserCallback {
    // get firebase reference
    private val db = FirebaseFirestore.getInstance()

    fun getUserFavorites() {
        UserProvider().getUserAccount(this)
    }

    override fun onGetUser(user: User) {
        // Iterator in list favorites getting posts favorites
        val favorites = user.userFavorites
        val listaPublicaciones = mutableListOf<Post>()

        var count =
            0 // Contador para llevar el seguimiento de cuántas publicaciones se han recuperado

        if (favorites.isNullOrEmpty()) {
            callback.onGetPosts(listaPublicaciones)
            Log.e("resultado", "No hay posts favorites :: $listaPublicaciones")
        } else {

            for (favorito in favorites) {
                db.collection("posts")
                    .document(favorito)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val fechaPost = documentSnapshot.getDate("fechaPost") ?: ""
                            val autorPost = documentSnapshot.getString("autorPost") ?: ""
                            val tituloPost = documentSnapshot.getString("tituloPost") ?: ""
                            val cuerpoPost = documentSnapshot.getString("cuerpoPost") ?: ""
                            val imagenPost = documentSnapshot.get("imagenPost") as? List<String> ?: emptyList()
                            val emailAutorPost = documentSnapshot.getString("emailAutorPost") ?: ""
                            val idPost = documentSnapshot.id

                            val post = Post(
                                fechaPost,
                                autorPost,
                                tituloPost,
                                cuerpoPost,
                                imagenPost,
                                idPost,
                                emailAutorPost
                            )
                            listaPublicaciones.add(post)
                        }

                        // Incrementar el contador y emitir la lista de publicaciones si todas se han recuperado
                        count++
                        if (count == favorites.size) {
                            callback.onGetPosts(listaPublicaciones)
                            Log.i("resultado", "List post favorites is ready")
                        }
                    }
                    .addOnFailureListener { exception ->
                        println("Error obteniendo posts: $exception")

                        // Incrementar el contador en caso de error para asegurar que se emita la lista de publicaciones vacía
                        count++
                        if (count == favorites.size) {
                            callback.onGetPosts(listaPublicaciones)
                            callback.onError("Algo salio mal al recuperar posts guardados")
                        }
                    }
            }
        }
    }

    override fun onGetUserError(error: String) {
        Log.e("error", "Algo salio mal al obtener el usuario para sacar favoritos")
    }

    fun removeFavorite(callback: FavoritesFirestore, userEmail: String, refNewDocFavorite: String) {
        println("BORRANDO")
        val usersCollection = db.collection("users")
        val query = usersCollection.whereEqualTo("userEmail", userEmail)

        query.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val userDocRef = usersCollection.document(document.id)

                    userDocRef.get()
                        .addOnSuccessListener { userDocument ->
                            if (userDocument.exists()) {
                                val favoritosActuales =
                                    userDocument.get("userFavorites") as? List<String>
                                        ?: emptyList()
                                // Verificar si el nuevo favorito ya está en la lista actual
                                if (favoritosActuales.contains(refNewDocFavorite)) {

                                    val nuevaListaFavoritos = favoritosActuales.toMutableList()
                                    nuevaListaFavoritos.remove(refNewDocFavorite)

                                    // Actualiza el documento del usuario con la nueva lista de favoritos
                                    userDocRef.update("userFavorites", nuevaListaFavoritos)
                                        .addOnSuccessListener {
                                            callback.onRemoveFavorite("SUCCESS")
                                            Log.i("Favorites", "Favorite removed successfully")
                                        }
                                        .addOnFailureListener { e ->
                                            callback.onErrorRemove(e.toString())
                                            Log.e("Favorites","Error al eliminar favorito: $e")
                                        }
                                } else {
                                    callback.onRemoveFavorite("NO_EXISTS")
                                    Log.i("Favorites", "No exists the favorite in user")
                                }
                            } else {
                                Log.i("Favorites", "Error on get user document no exists")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("GetUserInFavorites", "Error on get user :: $e")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("GetUserInFavorites", "Error searching user :: $e")
            }
    }
}