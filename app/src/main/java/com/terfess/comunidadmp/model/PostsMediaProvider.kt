package com.terfess.comunidadmp.model

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.terfess.comunidadmp.callbacks.IncrementFieldCallback
import com.terfess.comunidadmp.callbacks.posts.DeletePost
import com.terfess.comunidadmp.callbacks.posts.OnSetNewMediaPost
import com.terfess.comunidadmp.callbacks.postsMedias.GetMediaPostCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostsMediaProvider {
    private val db = FirebaseFirestore.getInstance()

    fun loadPosts(
        callback: GetMediaPostCallback,
        lastPostIdPagin: DocumentSnapshot?,
        limitPosts: Int
    ) {
        CoroutineScope(Dispatchers.Default).launch {

            var preQuery: Query = db.collection("mediaPosts")
                .orderBy(
                    "fechaPostMedia",
                    Query.Direction.DESCENDING
                ) // Ordenar por fecha de publicación descendente

            // Si hay un último documento, usa startAfter
            if (lastPostIdPagin != null) {
                preQuery = preQuery.startAfter(lastPostIdPagin)
                println("-----Hay ultimo post, añadido inicio paginacion (afterstart)")
            } else {
                println("-----No hay ultimo documento")
            }

            preQuery.limit(limitPosts.toLong())
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        callback.onError("THERE_NO_POSTS")
                        return@addOnSuccessListener
                    }

                    val listMediaPosts = mutableListOf<PostMedia>()
                    for (document in documents) {
                        try {
                            val idPost = document.id
                            val fechaPost = document.getDate("fechaPostMedia") ?: ""
                            val autorPost = document.getString("autorPostMedia") ?: ""
                            val tituloPost = document.getString("tituloPostMedia") ?: ""
                            val cuerpoPost = document.getString("cuerpoPostMedia") ?: ""
                            val linkPost = document.getString("linkStreamPostMedia") ?: ""
                            val emailAutorPost = document.getString("emailAutorPostMedia") ?: ""
                            val likes = document.getString("likesPostMediaCount") ?: "0"

                            val mediaPost = PostMedia(
                                idPost,
                                autorPost,
                                fechaPost,
                                tituloPost,
                                cuerpoPost,
                                linkPost,
                                emailAutorPost,
                                likes
                            )

                            listMediaPosts.add(mediaPost)
                        } catch (e: Exception) {
                            println("El error fue: ${e.message}")
                        }
                    }

                    callback.onGetPosts(listMediaPosts, documents.last())
                }
                .addOnFailureListener { exception ->
                    val errorMessage = if (exception is FirebaseFirestoreException) {
                        if (exception.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            "PERMISSION_DENIED"
                        } else {
                            exception.message ?: "UNKNOWN_ERROR"
                        }
                    } else {
                        exception.message ?: "UNKNOWN_ERROR"
                    }
                    callback.onError(errorMessage)
                }
        }


    }

    fun setPost(callback: OnSetNewMediaPost, dataPost: HashMap<String, Any?>) {
        // Agregar el nuevo documento a la colección "posts"

        db.collection("mediaPosts")
            .add(dataPost)
            .addOnSuccessListener { documentReference ->
                println("Documento agregado con ID: ${documentReference.id}")
                val refdoc = documentReference.id
                callback.onResult(refdoc)
            }
            .addOnFailureListener { e ->
                callback.onErrorSetPost(e.message.toString())
            }
    }

    fun setCounterLikesMediaPost(callback: IncrementFieldCallback, idPost: String, action: String) {
        val collection = db.collection("mediaPosts")

        collection.document(idPost)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Obtener el valor actual del campo likesPostMediaCount
                    val count = documentSnapshot.getString("likesPostMediaCount")?.toInt() ?: 0

                    // Calcular el nuevo valor del contador
                    val newCount = if (action == "ADD") {
                        count + 1
                    } else {
                        count - 1
                    }

                    // Actualizar el campo likesPostMediaCount
                    collection.document(idPost).update("likesPostMediaCount", newCount.toString())
                        .addOnSuccessListener {
                            callback.onSuccesIncrement()
                            Log.i("MediaPost", "Likes count updated successfully.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("MediaPost", "Error updating likes count: $e")
                        }
                } else {
                    Log.e("MediaPost", "Document not found: $idPost")
                }
            }
            .addOnFailureListener { e ->
                Log.e("MediaPost", "Error fetching document: $e")
            }
    }

    fun deletePost(callback: DeletePost, idPost: String) {

        db.collection("mediaPosts").document(idPost)
            .delete()
            .addOnSuccessListener { callback.onDeletePostSuccess("SUCCESS") }
            .addOnFailureListener { e ->
                callback.onErrorDeletePostError("ERROR")
            }
    }
}