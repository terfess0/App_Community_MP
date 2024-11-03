package com.terfess.comunidadmp.callbacks.posts

import com.google.firebase.firestore.DocumentSnapshot
import com.terfess.comunidadmp.model.Post

interface GetPostCallback {
    fun onGetPosts(posts :List<Post>, lastDoc: DocumentSnapshot?)
    fun onError(error: String)
}