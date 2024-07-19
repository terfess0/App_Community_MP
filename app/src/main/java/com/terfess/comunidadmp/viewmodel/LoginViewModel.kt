package com.terfess.comunidadmp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.terfess.comunidadmp.callbacks.LoginWithEmlPsw
import com.terfess.comunidadmp.callbacks.RegisterUserCallback
import com.terfess.comunidadmp.model.LoginRepository
import com.terfess.comunidadmp.model.User

class LoginViewModel : ViewModel(), RegisterUserCallback, LoginWithEmlPsw {
    val resultRegist = MutableLiveData<String>()
    val resultLoginEmlPsw = MutableLiveData<String>()

    fun newAccount(dataUser: User, psw: String) {
        LoginRepository().crearCuenta(callback = this, data = dataUser, password = psw)
    }
    fun loginWithEmaPsw(email:String, psw:String){
        LoginRepository().loginEmailPassword(this, email, psw)
    }

    override fun onRegister() {
        resultRegist.postValue("SUCCESS")
    }

    override fun onErrorRegist(error: String) {
        resultRegist.postValue(error)
    }

    override fun onLogin() {
        resultLoginEmlPsw.postValue("SUCCESS")
    }

    override fun onError(error: String) {
        resultLoginEmlPsw.postValue(error)
    }
}