package com.terfess.comunidadmp.model

import android.content.Context
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.getField
import com.google.firebase.storage.FirebaseStorage
import com.terfess.comunidadmp.callbacks.posts.DeletePost
import com.terfess.comunidadmp.callbacks.posts.FavoritesFirestore
import com.terfess.comunidadmp.callbacks.posts.GetPostCallback
import com.terfess.comunidadmp.callbacks.posts.OnAttachImagesToPost
import com.terfess.comunidadmp.callbacks.posts.OnSetNewPost
import com.terfess.comunidadmp.callbacks.posts.OnUploadImages
import com.terfess.comunidadmp.classes.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

class PostRepository : ViewModel() {
    private val db = FirebaseFirestore.getInstance()


    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun loadPosts(callback: GetPostCallback) {
        CoroutineScope(Dispatchers.Default).launch {
            db.collection("posts")
                .orderBy(
                    "fechaPost",
                    Query.Direction.DESCENDING
                ) // Ordenar por fecha de publicación descendente
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        callback.onError("THERE_NO_POSTS")
                    }
                    val listaPublicaciones = mutableListOf<Post>()
                    for (document in documents) {
                        val fechaPost = document.getDate("fechaPost") ?: ""
                        val autorPost = document.getString("autorPost") ?: ""
                        val tituloPost = document.getString("tituloPost") ?: ""
                        val cuerpoPost = document.getString("cuerpoPost") ?: ""
                        val imagenPost = document.get("imagenPost") as? List<String> ?: emptyList()
                        val emailAutorPost = document.getString("emailAutorPost") ?: ""
                        val idPost = document.id

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
                    callback.onGetPosts(listaPublicaciones)
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

    // Get selected post
    fun loadInfoPost(callback: GetPostCallback, idPost: String) {
        CoroutineScope(Dispatchers.Default).launch {
            db.collection("posts")
                .document(idPost)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // El documento existe

                        val fechaPost = document.getDate("fechaPost") ?: ""
                        val autorPost = document.getString("autorPost") ?: ""
                        val tituloPost = document.getString("tituloPost") ?: ""
                        val cuerpoPost = document.getString("cuerpoPost") ?: ""
                        val imagenPost = document.get("imagenPost") as? List<String> ?: emptyList()
                        val emailAutorPost = document.getString("emailAutorPost") ?: ""
                        val id = document.id


                        val post = listOf(
                            Post(
                                fechaPost,
                                autorPost,
                                tituloPost,
                                cuerpoPost,
                                imagenPost,
                                id,
                                emailAutorPost
                            )
                        )
                        callback.onGetPosts(post)

                    } else {
                        callback.onError("NOT_FOUND")
                        Log.e("RepositoryPosts", "Post not found")
                    }
                }
                .addOnFailureListener { exception ->
                    callback.onError(exception.toString())
                    Log.e(
                        "RepositoryPosts",
                        "Error searchinginfo of post: $idPost Error: $exception"
                    )
                }
        }
    }

    fun deletePost(callback: DeletePost, idPost: String) {

        db.collection("posts").document(idPost)
            .delete()
            .addOnSuccessListener { callback.onDeletePostSuccess("SUCCESS") }
            .addOnFailureListener { e ->
                callback.onErrorDeletePostError("ERROR")
            }
    }

    //guardar un post como favorito, esto va de la mano con FavoritesFragment y su viewmodel
    fun addFavorite(callback: FavoritesFirestore, userEmail: String, refNewDocFavorite: String) {
        println("AÑADIENDO")
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
                                    // El nuevo favorito ya existe en la lista actual
                                    callback.onAddFavorite("ALREADY_EXISTS")
                                } else {

                                    // Agrega el nuevo favorito a la lista de favoritos actuales
                                    val nuevaListaFavoritos = favoritosActuales.toMutableList()
                                    nuevaListaFavoritos.add(refNewDocFavorite)

                                    // Actualiza el documento del usuario con la nueva lista de favoritos
                                    userDocRef.update("userFavorites", nuevaListaFavoritos)
                                        .addOnSuccessListener {
                                            callback.onAddFavorite("SUCCESS")
                                        }
                                        .addOnFailureListener { e ->
                                            callback.onErrorAdd(e.toString())
                                        }


                                }
                            } else {
                                // Si el campo de usuario no existe, crea uno nuevo
                                val nuevaListaFavoritos = listOf(refNewDocFavorite)
                                userDocRef.set(
                                    mapOf(
                                        "userEmail" to userEmail,
                                        "userFavorites" to nuevaListaFavoritos
                                    )
                                )
                                    .addOnSuccessListener {
                                        callback.onAddFavorite("SUCCESS")
                                    }
                                    .addOnFailureListener { e ->
                                        callback.onErrorAdd(e.toString())
                                    }
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

    fun uploadImagesGetUrl(
        callback: OnUploadImages,
        context: Context,
        images: MutableList<ImageSelected>
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
        val listReferences = mutableListOf<String>()

        CoroutineScope(Dispatchers.Main).launch {
            val uploadJobs = images.map { image ->
                async {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, image.imageLink)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, image.imageLink)
                    }

                    val compressedBytes = ImageUtils.compressBitmap(bitmap, 150000)
                    val fileName = "${System.currentTimeMillis()}_${File(image.imageLink.path!!).name}"
                    val riversRef = storageRef.child("images/imagePost/$fileName")

                    try {
                        val uploadTask = riversRef.putBytes(compressedBytes).await()
                        val downloadUri = riversRef.downloadUrl.await()
                        listReferences.add(downloadUri.toString())
                        println("**Mas 1 imagen subida")
                    } catch (e: Exception) {
                        println("Upload failed: $e")
                    }
                }
            }

            // Wait for all upload tasks to complete
            uploadJobs.awaitAll()

            // Invoke the callback with the list of image URLs
            callback.onUploadSuccess(listReferences)
            println("references devueltas")
        }
    }


    fun setNewPost(callback: OnSetNewPost, dataPost: HashMap<String, Any?>) {
        // Agregar el nuevo documento a la colección "posts"

        db.collection("posts")
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

    fun attachImagesToPost(
        callback: OnAttachImagesToPost,
        postId: String,
        imagesReferences: List<String>
    ) {
        val usersCollection = db.collection("posts")

        usersCollection
            .document(postId)
            .update("imagenPost", imagesReferences).addOnSuccessListener {
                callback.onAttachImagesSuccess()
            }.addOnFailureListener {
                callback.onAttachImgsError()
            }

    }
}
