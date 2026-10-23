package com.terfess.comunidadmp.model

import java.io.Serializable

data class Comment(
    val idComment: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val commentText: String = "",
    val fechaComment: Serializable = ""
)