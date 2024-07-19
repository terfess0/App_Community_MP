package com.terfess.comunidadmp.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.terfess.comunidadmp.callbacks.ChangeUserNameCallback
import com.terfess.comunidadmp.callbacks.GetUserCallback
import com.terfess.comunidadmp.model.UserProvider
import com.terfess.comunidadmp.model.User

class AccountViewModel : ViewModel(), GetUserCallback, ChangeUserNameCallback {
    val dataUser = MutableLiveData<User>()
    val resultChangeName = MutableLiveData<String>()

    fun updateNameUser(userEmail: String, userNewName: String) {
        UserProvider().changeNameUser(this, userEmail, userNewName)
    }


    fun getUserAccount() {
        UserProvider().getUserAccount(this)
    }

    override fun onGetUser(user: User) {
        dataUser.postValue(user)
    }

    override fun onGetUserError(error: String) {
        Log.e("error", "Algo salio mal al recibir info cuenta de usuario :: $error")
    }

    override fun onResult(result: String) {
        resultChangeName.postValue(result)
    }
}