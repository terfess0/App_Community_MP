package com.terfess.comunidadmp.view

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.adapters.AdapterHolderImgPost
import com.terfess.comunidadmp.classes.Utilities
import com.terfess.comunidadmp.databinding.FragmentWritePostBinding
import com.terfess.comunidadmp.model.ImageSelected
import com.terfess.comunidadmp.model.UserProvider
import com.terfess.comunidadmp.viewmodel.PostViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class WritePost : Fragment() {
    private lateinit var binding: FragmentWritePostBinding
    private lateinit var adapterImages: AdapterHolderImgPost
    private val postVM: PostViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentWritePostBinding.inflate(inflater, container, false)
        val root = binding.root

        //recyclerimages
        val imagenes = mutableListOf<ImageSelected>()

        val recycler = binding.selectedImages
        adapterImages = AdapterHolderImgPost(imagenes)
        val numColumnas = 2
        recycler.adapter = adapterImages
        recycler.layoutManager = GridLayoutManager(requireContext(), numColumnas)


        val buttonSelectImage = binding.btnSelectImgPost
        var refImagen = ""
        val btnPublicar = binding.btnSendPublish
        val progresIc = binding.progressPost


        //devolucion de llamada cuando el usuario elige imagen local se subira a storage
        val getContent =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                // Handle the returned Uri

                //mostrar progress ic en lugar de boton
//                btnPublicar.visibility = View.GONE
//                progresIc.visibility = View.VISIBLE

                val selectedImage: Uri? = uri
                selectedImage?.let { img ->

                    val idImageSave = getCurrentTime()
                    val imagen = ImageSelected(idImageSave, img)
                    adapterImages.addImage(imagen)

//                    uri.let { img ->
//
//
//                    }
                }
            }

        buttonSelectImage.setOnClickListener {
            //seleccionar imagen local
            getContent.launch("image/*")
        }

        val edtTitulo = binding.editTextTitle
        val editTextBody = binding.editTextBody

        editTextBody.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                // Ocultar el teclado suavemente
                hideKeyboard(editTextBody)
            }
            false
        }

        val user = Firebase.auth.currentUser


        //publicar
        btnPublicar.setOnClickListener {
            if (user != null) {
                val titulo = edtTitulo.text.toString()
                val cuerpo = editTextBody.text.toString()
                val emailAutor = user.email
                var autor = Utilities().getRegistSheredPref("userName", root.context)

                if (titulo.isBlank() || cuerpo.isBlank()) {
                    Toast.makeText(
                        root.context,
                        "Complete titulo y texto del post",
                        Toast.LENGTH_LONG
                    ).show()
                } else {


                    //mostrar progress ic en lugar de boton
                    btnPublicar.visibility = View.GONE
                    progresIc.visibility = View.VISIBLE



                    if (autor.isNullOrBlank()) autor = "unknown"

                    // Crear un nuevo mapa con los datos a guardar
                    val datosPost: HashMap<String, Any?> = hashMapOf(
                        "tituloPost" to titulo,
                        "cuerpoPost" to cuerpo,
                        "autorPost" to autor,
                        "imagenPost" to refImagen,
                        "fechaPost" to FieldValue.serverTimestamp(),
                        "emailAutorPost" to emailAutor
                    )

                    postVM.setPost(datosPost)
                }

            } else {
                Toast.makeText(root.context, "Debe Iniciar Sesión", Toast.LENGTH_LONG)
                    .show()
                findNavController().navigate(R.id.navigation_login)
                findNavController().popBackStack(R.id.navigation_home, false)
            }
        }

        postVM.resultSetPost.observe(viewLifecycleOwner, Observer {
            // Limpiar los EditText después de agregar los datos
            edtTitulo.setText("")
            editTextBody.setText("")
            btnPublicar.visibility = View.VISIBLE
            progresIc.visibility = View.GONE

            postVM.uploadImagesPost(imagenes, requireActivity())
            //update num post user
            UserProvider().setUserCounterPost(user?.email.toString(), "ADD")

            //exit to home
            Snackbar.make(root, "Post Enviado", Snackbar.LENGTH_SHORT)
                .show()
            findNavController().navigate(R.id.navigation_home)
            findNavController().popBackStack(R.id.navigation_home, false)
        })

        postVM.errorSetPost.observe(viewLifecycleOwner, Observer {
            btnPublicar.visibility = View.VISIBLE
            progresIc.visibility = View.GONE

            Toast.makeText(
                root.context,
                "Algo salio mal, prueba más tarde",
                Toast.LENGTH_SHORT
            ).show()
            println("Algo salio mal al subir post: $it")
            findNavController().navigate(R.id.navigation_home)
            findNavController().popBackStack(R.id.navigation_home, false)
        })

        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_writePost_to_navigation_home)
            println("Se navegó de writepost a HomeFragment")
        }

        return root

    }

    private fun hideKeyboard(editTextBody: EditText) {
        val imm =
            requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editTextBody.windowToken, 0)
    }

    fun getCurrentTime(): String {
        val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
        val fechaActual = Date()
        return formato.format(fechaActual)
    }
}