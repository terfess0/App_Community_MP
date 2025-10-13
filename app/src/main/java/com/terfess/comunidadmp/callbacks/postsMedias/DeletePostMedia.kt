package com.terfess.comunidadmp.callbacks.postsMedias

interface DeletePostMedia {
    fun onDeletePostSuccess(result: String)
    fun onErrorDeletePostError(error: String)
}