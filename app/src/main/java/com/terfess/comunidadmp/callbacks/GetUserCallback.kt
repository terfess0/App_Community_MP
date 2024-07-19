package com.terfess.comunidadmp.callbacks

import com.terfess.comunidadmp.model.User

interface GetUserCallback {
    fun onGetUser(user: User)
    fun onGetUserError(error: String)
}