package com.terfess.comunidadmp.callbacks.posts

interface OnSetNewPost {
    fun onResult(docReference: String)
    fun onErrorSetPost(error: String)
}