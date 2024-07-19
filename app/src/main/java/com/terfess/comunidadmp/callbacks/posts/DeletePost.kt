package com.terfess.comunidadmp.callbacks.posts

import java.lang.Error

interface DeletePost {
    fun onDeletePostSuccess(result: String)
    fun onErrorDeletePostError(error: String)
}