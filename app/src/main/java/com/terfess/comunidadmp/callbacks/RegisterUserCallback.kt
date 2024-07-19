package com.terfess.comunidadmp.callbacks

interface RegisterUserCallback {
    fun onRegister()
    fun onErrorRegist(error:String)
}