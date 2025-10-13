package com.terfess.comunidadmp.callbacks.postsMedias

import com.google.firebase.firestore.DocumentSnapshot
import com.terfess.comunidadmp.model.PostMedia

interface GetMediaPostCallback {
    fun onGetPosts(posts :MutableList<PostMedia>, lastDoc: DocumentSnapshot?)
    fun onError(error: String)
}