package com.terfess.comunidadmp.view

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.util.Linkify
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.adapters.AdapterHolderViewPostImage
import com.terfess.comunidadmp.databinding.FragmentPostBinding
import com.terfess.comunidadmp.viewmodel.InfoPostViewModel

class PostFragment : Fragment() {
    private val viewModel: InfoPostViewModel by viewModels()
    private lateinit var binding: FragmentPostBinding
    private val arguments: PostFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val db = FirebaseFirestore.getInstance()
        binding = FragmentPostBinding.inflate(inflater, container, false)

        val idPost = arguments.postDocumentId
        viewModel.getPostDetails(idPost)


        val adapter = AdapterHolderViewPostImage(emptyList(), requireContext())
        val recyclerimage = binding.viewPostImages
        recyclerimage.adapter = adapter
        recyclerimage.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)



        viewModel.postDetails.observe(viewLifecycleOwner) { info ->
            val post = info[0]
            // Actualizar la UI con los datos del post
            binding.viewPostTitle.text = post.tituloPost


            val cuerpoTxt = binding.viewPostContent
            cuerpoTxt.text = post.cuerpoPost

            Linkify.addLinks(cuerpoTxt, Linkify.WEB_URLS)
            cuerpoTxt.movementMethod = android.text.method.LinkMovementMethod.getInstance()

            if (post.imagenPost.isNotEmpty()) {
                recyclerimage.adapter =
                    AdapterHolderViewPostImage(info[0].imagenPost, requireContext())
            }else{
              binding.tvDeslize.visibility = View.GONE
            }


            // Cargar foto de perfil del autor
            db.collection("users")
                .whereEqualTo("userEmail", post.emailAutorPost)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        val nameAuthor = document.getString("userName")
                        if (nameAuthor?.isNotBlank() == true) {
                            binding.viewPostAutor.text = "@$nameAuthor"
                        } else {
                            binding.viewPostAutor.text = "@unknown"
                        }
                    }
                }


            binding.viewPostDate.text = post.fechaPost.toString().substring(0, 10)

            binding.btnReportPostDetail.setOnClickListener {
                val reasons = arrayOf(
                    "Spam o publicidad no deseada",
                    "Contenido ofensivo o lenguaje inapropiado",
                    "Aporte malicioso / Stealer / Mod con crash",
                    "Acoso o difamación",
                    "Otro motivo"
                )

                AlertDialog.Builder(requireContext())
                    .setTitle("Reportar publicación")
                    .setItems(reasons) { dialog, which ->
                        val selectedReason = reasons[which]
                        val currentUser = Firebase.auth.currentUser
                        val reportData = hashMapOf(
                            "idPost" to idPost,
                            "postTitle" to post.tituloPost,
                            "reportedAuthorEmail" to post.emailAutorPost,
                            "reporterEmail" to (currentUser?.email ?: "guest"),
                            "reason" to selectedReason,
                            "date" to com.google.firebase.Timestamp.now()
                        )

                        db.collection("reports")
                            .add(reportData)
                            .addOnSuccessListener {
                                Snackbar.make(
                                    requireView(),
                                    "Gracias por tu reporte. Nuestro equipo de moderación lo revisará.",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                            .addOnFailureListener {
                                Snackbar.make(
                                    requireView(),
                                    "Error al enviar reporte. Inténtalo más tarde.",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancelar") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        viewModel.errorGetPost.observe(viewLifecycleOwner, Observer {
            if (it == "NOT_FOUND") {
                Snackbar.make(requireView(), "Post no encontrado", Snackbar.LENGTH_LONG).show()
                binding.viewPostTitle.text = "Post no encontrado!"
            } else {
                Snackbar.make(requireView(), "Algo ha salido mal...", Snackbar.LENGTH_LONG).show()
                binding.viewPostTitle.text = "Algo salio mal, intentalo más tarde"
            }
        })
        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_postFragment_to_navigation_home)
        }

        return binding.root
    }
}
