package com.terfess.comunidadmp.model

import java.io.Serializable


data class Post(
    val fechaPost: Serializable,
    val autorPost: String,
    val tituloPost: String,
    val cuerpoPost: String,
    val imagenPost: List<String>,
    val idPost: String,
    val emailAutorPost: String
)
