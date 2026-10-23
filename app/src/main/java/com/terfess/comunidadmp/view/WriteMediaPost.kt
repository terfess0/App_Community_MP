package com.terfess.comunidadmp.view

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.classes.Utilities
import com.terfess.comunidadmp.databinding.FragmentWriteMediaPostBinding
import com.terfess.comunidadmp.model.ImageSelected
import com.terfess.comunidadmp.model.UserProvider
import com.terfess.comunidadmp.viewmodel.MediaViewModel

class WriteMediaPost : Fragment() {
    private var _binding: FragmentWriteMediaPostBinding? = null
    private val binding get() = _binding!!

    private val mediaVM: MediaViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentWriteMediaPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnPublicar = binding.btnSendPublishNewMedia
        val progresIc = binding.progressPostNewMedia

        val edtTitulo = binding.editTextTitleNewMedia
        val editTextBody = binding.editTextBodyNewMedia
        val editLinkText = binding.editTextLinkNewMedia

        editTextBody.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                // Ocultar el teclado suavemente
                Utilities().hideKeyboard(requireContext(), editTextBody)
            }
            false
        }

        val user = Firebase.auth.currentUser


        //publicar
        btnPublicar.setOnClickListener {
            val titulo = edtTitulo.text.toString()
            val cuerpo = editTextBody.text.toString()
            val link = editLinkText.text.toString()

            val emailAutor = user?.email
            var autor = Utilities().getRegistSheredPref("userName", binding.root.context)

            if (titulo.isBlank() || cuerpo.isBlank() || link.isBlank()) {
                Toast.makeText(
                    binding.root.context,
                    "Complete titulo, texto y link stream del post",
                    Toast.LENGTH_LONG
                ).show()
            } else if (!Utilities().isValidLink(link)) {
                Toast.makeText(
                    binding.root.context,
                    "Link stream no valido",
                    Toast.LENGTH_LONG
                ).show()
                editLinkText.error = "Link not Valid"
            } else {

                //mostrar progress ic en lugar de boton
                btnPublicar.visibility = View.GONE
                progresIc.visibility = View.VISIBLE

                if (autor.isNullOrBlank()) autor = "unknown"

                val db = FirebaseFirestore.getInstance()
                db.collection("users")
                    .whereEqualTo("userEmail", emailAutor)
                    .get()
                    .addOnSuccessListener { query ->
                        if (!query.isEmpty && query.documents[0].getBoolean("isBanned") == true) {
                            btnPublicar.visibility = View.VISIBLE
                            progresIc.visibility = View.GONE
                            val reason = query.documents[0].getString("banReason") ?: ""
                            val msg = if (reason.isNotBlank()) {
                                "Tu cuenta está suspendida.\nMotivo: $reason"
                            } else {
                                "Tu cuenta está suspendida por infracción a las normas."
                            }
                            Utilities().showAlert(
                                binding.root.context,
                                "Cuenta Suspendida",
                                msg,
                                "Entendido",
                                null,
                                false,
                                {},
                                {}
                            )
                            return@addOnSuccessListener
                        }

                        // Crear un nuevo mapa con los datos a guardar
                        val datosPost: HashMap<String, Any?> = hashMapOf(
                            "autorPostMedia" to autor,
                            "cuerpoPostMedia" to cuerpo,
                            "tituloPostMedia" to titulo,
                            "linkStreamPostMedia" to link,
                            "fechaPostMedia" to FieldValue.serverTimestamp(),
                            "emailAutorPostMedia" to emailAutor,
                            "likesPostMediaCount" to "0"
                        )

                        mediaVM.setMediaPost(datosPost)
                    }
                    .addOnFailureListener {
                        btnPublicar.visibility = View.VISIBLE
                        progresIc.visibility = View.GONE
                        Toast.makeText(binding.root.context, "Error al verificar cuenta", Toast.LENGTH_SHORT).show()
                    }
            }

        }

        mediaVM.resultAddMediaPost.observe(viewLifecycleOwner) {
            // Limpiar los EditText después de agregar los datos
            edtTitulo.setText("")
            editTextBody.setText("")
            editLinkText.setText("")

            btnPublicar.visibility = View.VISIBLE
            progresIc.visibility = View.GONE

            //update num post user
            UserProvider().setUserCounterPost(user?.email.toString(), "ADD")

            //exit to home
            Snackbar.make(binding.root, "Post Enviado", Snackbar.LENGTH_SHORT)
                .show()
            findNavController().navigate(R.id.action_writeMediaPost_to_mediaFragment)
        }

//        mediaVM.errorSetPost.observe(viewLifecycleOwner)  {
//            btnPublicar.visibility = View.VISIBLE
//            progresIc.visibility = View.GONE
//
//            Toast.makeText(
//                root.context,
//                "Algo salio mal, prueba más tarde",
//                Toast.LENGTH_SHORT
//            ).show()
//            println("Algo salio mal al subir post: $it")
//            findNavController().navigate(R.id.navigation_home)
//            findNavController().popBackStack(R.id.navigation_home, false)
//        }

        binding.btnBackMedia.setOnClickListener {
            findNavController().navigate(R.id.action_writeMediaPost_to_mediaFragment)
            println("Se navegó de writepost a HomeFragment")
        }

    }

}