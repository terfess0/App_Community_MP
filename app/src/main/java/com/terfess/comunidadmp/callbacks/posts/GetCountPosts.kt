package com.terfess.comunidadmp.callbacks.posts

interface GetCountPosts {
    fun AtSucces(count: Int)
    fun errorGetCount(error: String)
}