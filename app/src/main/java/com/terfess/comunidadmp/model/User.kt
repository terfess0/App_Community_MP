package com.terfess.comunidadmp.model

import java.io.Serializable

data class User(
    val userName: String,
    val userNumPosts: String,
    val userEmail: String,
    val userJoinDate: Serializable,
    val userPhoto: String,
    val userRole: String,
    val userFavorites: List<String>?
)
