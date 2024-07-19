package com.terfess.comunidadmp.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.classes.Utilities
import com.terfess.comunidadmp.model.User
import com.terfess.comunidadmp.viewmodel.LoginViewModel

class RegisterFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var thisContext: Context

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_registro, container, false)

        thisContext = requireContext()
        val emailRegist = view.findViewById<EditText>(R.id.editUserR)
        val passwrdRegist = view.findViewById<EditText>(R.id.editPasswordR)
        val anuncioFail = view.findViewById<TextView>(R.id.anuncioRegistroFail)

        // Comportamiento botón y progress
        val botonRegistro = view.findViewById<Button>(R.id.registrarse_btn)
        val progressReg = view.findViewById<ProgressBar>(R.id.progress_regist)

        botonRegistro.setOnClickListener {
            emailRegist.clearFocus()
            passwrdRegist.clearFocus()

            if (emailRegist.text.isNotBlank() && passwrdRegist.text.isNotBlank()) {
                if (!isEmailValid(emailRegist.text.toString())) {
                    anuncioFail.text = getString(R.string.correo_invalido)
                    anuncioFail.visibility = View.VISIBLE
                    emailRegist.requestFocus()
                } else if (!isPasswordValid(passwrdRegist.text.toString())) {
                    anuncioFail.text = getString(R.string.wrn_password_8_caracteres)
                    anuncioFail.visibility = View.VISIBLE
                    passwrdRegist.requestFocus()
                } else {
                    anuncioFail.visibility = View.GONE

                    val user = User(
                        "",
                        "",
                        emailRegist.text.toString(),
                        "",
                        "",
                        "",
                        emptyList()
                    )

                    viewModel.newAccount(user, passwrdRegist.text.toString())
                    botonRegistro.visibility = View.GONE
                    progressReg.visibility = View.VISIBLE
                }
            } else {
                anuncioFail.text = getString(R.string.must_fill_all_fields)
                anuncioFail.visibility = View.VISIBLE
                emailRegist.requestFocus()
            }
        }

        viewModel.resultRegist.observe(viewLifecycleOwner, Observer { result ->
            handleResult(result, anuncioFail, botonRegistro, progressReg)
        })

        val botonIrLogin = view.findViewById<TextView>(R.id.iraLogin)
        botonIrLogin.setOnClickListener {
            findNavController().navigate(R.id.navigation_login)
        }

        return view
    }

    private fun isEmailValid(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 8
    }

    private fun handleResult(
        result: String,
        anuncioFail: TextView,
        botonRegistro: Button,
        progressReg: ProgressBar
    ) {
        when (result) {
            "SUCCESS" -> {
                val intent = Intent(thisContext, MainActivity::class.java)
                thisContext.startActivity(intent)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.register_completed),
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigate(R.id.action_navigation_registro_to_navigation_home)
                Utilities().saveSharedpref(thisContext, "LoginState", "true")
            }

            "WEAK_PASSWORD" -> {
                anuncioFail.text = getString(R.string.wrn_password_8_caracteres)
                anuncioFail.visibility = View.VISIBLE
                botonRegistro.visibility = View.VISIBLE
                progressReg.visibility = View.GONE
            }

            "INVALID_EMAIL" -> {
                anuncioFail.text = getString(R.string.correo_invalido)
                anuncioFail.visibility = View.VISIBLE
                botonRegistro.visibility = View.VISIBLE
                progressReg.visibility = View.GONE
            }

            "EMAIL_ALREADY_IN_USE" -> {
                anuncioFail.text = getString(R.string.correo_ya_en_uso)
                anuncioFail.visibility = View.VISIBLE
//                emailRegist.requestFocus()
                botonRegistro.visibility = View.VISIBLE
                progressReg.visibility = View.GONE
            }

            else -> {
                anuncioFail.text = getString(R.string.regist_not_working)
                anuncioFail.visibility = View.VISIBLE
                botonRegistro.visibility = View.VISIBLE
                progressReg.visibility = View.GONE
            }
        }
    }

}