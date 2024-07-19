package com.terfess.comunidadmp.callbacks.posts

import com.terfess.comunidadmp.model.Post

interface GetPostCallback {
    fun onGetPosts(posts :List<Post>)
    fun onError(error: String)
}