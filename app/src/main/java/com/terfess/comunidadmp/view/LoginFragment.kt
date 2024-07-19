package com.terfess.comunidadmp.view

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.classes.Utilities
import com.terfess.comunidadmp.model.LoginRepository
import com.terfess.comunidadmp.viewmodel.LoginViewModel

class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_logear, container, false)
        val loginButton = view.findViewById<Button>(R.id.login_btn)
        val registerTextButton = view.findViewById<TextView>(R.id.iraRegistro)

        // Obtener el estado de login desde SharedPreferences
        val loginState = Utilities().getRegistSheredPref("LoginState", requireContext())

        // Navegar a la pantalla principal si el usuario está logueado
        if (loginState == "true") {
            findNavController().navigate(R.id.action_navigation_login_to_navigation_home)
        }

        //edits de usuario y contraseña
        val correoLogin = view.findViewById<EditText>(R.id.editUser)
        val contra = view.findViewById<EditText>(R.id.editPassword)

        val progressLog = view.findViewById<ProgressBar>(R.id.progress_login)

        //anuncio de error en credenciales
        val anuncio = view.findViewById<TextView>(R.id.cred_fail)

        //limpiar edits
        correoLogin.setText("")
        contra.setText("")

        correoLogin.addTextChangedListener {
            anuncio.visibility = View.GONE
        }
        contra.addTextChangedListener {
            anuncio.visibility = View.GONE
        }


        /////////////el usuario presiona "login" ///////////////////////////////////////////////////////
        loginButton.setOnClickListener {

            anuncio.visibility = View.GONE

            //campos incompletos
            if (correoLogin.text.toString().isBlank() || contra.text.toString().isBlank()) {

                anuncio.text = getString(R.string.must_fill_all_fields)
                anuncio.visibility = View.VISIBLE


            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correoLogin.text).matches()) {

                anuncio.text = getString(R.string.correo_invalido)
                anuncio.visibility = View.VISIBLE


            } else if (contra.text.length < 8) { // Verificar si la contraseña tiene al menos 8 caracteres
                anuncio.text = getString(R.string.wrn_password_8_caracteres)
                anuncio.visibility = View.VISIBLE


            } else { // Si el correo electrónico y la contraseña son válidos
                // Ocultar el mensaje de error
                anuncio.visibility = View.GONE


                // Verificar si ambos campos no están en blanco
                if (correoLogin.text.isNotBlank() && contra.text.isNotBlank()) {

                    // Crear la cuenta utilizando el ViewModel
                    viewModel.loginWithEmaPsw(
                        correoLogin.text.toString(),
                        contra.text.toString()
                    )


                    //mostrar progress en lugar de boton login
                    loginButton.visibility = View.GONE
                    progressLog.visibility = View.VISIBLE
                }
            }
        }


        viewModel.resultLoginEmlPsw.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                "SUCCESS" -> {
                    Utilities().saveSharedpref(requireContext(), "LoginState", "true")

                    findNavController().navigate(R.id.action_navigation_login_to_navigation_home)
                }

                "ERROR_CREDENTIAL" -> {
                    anuncio.visibility = View.VISIBLE
                    anuncio.text = getString(R.string.credenciales_incorrectas)

                    progressLog.visibility = View.GONE
                    loginButton.visibility = View.VISIBLE
                }

                "NO_EXIST_OR_DISABLED" -> {
                    anuncio.visibility = View.VISIBLE
                    anuncio.text = getString(R.string.account_not_exist_registry)

                    progressLog.visibility = View.GONE
                    loginButton.visibility = View.VISIBLE
                }

                "ERROR_AUTH" -> {
                    anuncio.visibility = View.VISIBLE
                    anuncio.text = getString(R.string.algo_ha_salido_mal_login)

                    progressLog.visibility = View.GONE
                    loginButton.visibility = View.VISIBLE
                }
            }
        })


        //el usuario presiona "registrate aqui"
        registerTextButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_login_to_navigation_registro)
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish() // Cierra la actividad actual
                }
            })

        return view
    }

}
