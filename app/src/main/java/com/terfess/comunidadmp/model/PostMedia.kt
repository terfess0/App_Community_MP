package com.terfess.comunidadmp.model

import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import java.io.Serializable

data class PostMedia(
    var idPostMedia: String,
    val autorPostMedia: String,
    var fechaPostMedia: Serializable,
    val tituloPostMedia: String,
    val cuerpoPostMedia: String,
    val linkStreamPostMedia: String,
    val emailAutorPostMedia: String,
    val likesPostMediaCount: String = "0"
)
