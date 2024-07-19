package com.terfess.comunidadmp.callbacks

interface LoginWithEmlPsw {
    fun onLogin()
    fun onError(error:String)
}