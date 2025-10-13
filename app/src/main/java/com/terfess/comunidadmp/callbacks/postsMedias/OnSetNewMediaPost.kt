package com.terfess.comunidadmp.callbacks.posts

interface OnSetNewMediaPost {
    fun onResult(docReference: String)
    fun onErrorSetPost(error: String)
}