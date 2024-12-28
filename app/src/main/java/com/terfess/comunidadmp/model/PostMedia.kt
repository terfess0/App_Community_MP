package com.terfess.comunidadmp.model

import java.io.Serializable

data class PostMedia(
    val idPostMedia: String,
    val autorPostMedia: String,
    val fechaPostMedia: Serializable,
    val tituloPostMedia: String,
    val cuerpoPostMedia: String,
    val linkStreamPostMedia: String,
    val emailAutorPostMedia: String,
    val likesPostMediaCount: String = "0"
)
