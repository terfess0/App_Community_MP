package com.terfess.comunidadmp.callbacks.posts

import com.terfess.comunidadmp.model.Comment

interface CommentCallback {
    fun onCommentsLoaded(comments: List<Comment>)
    fun onCommentAdded()
    fun onError(error: String)
}