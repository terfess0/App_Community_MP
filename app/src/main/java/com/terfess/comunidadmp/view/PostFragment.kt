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
import com.terfess.comunidadmp.adapters.CommentAdapter
import com.terfess.comunidadmp.classes.Utilities
import com.terfess.comunidadmp.databinding.FragmentPostBinding
import com.terfess.comunidadmp.model.Comment
import com.terfess.comunidadmp.model.Post
import com.terfess.comunidadmp.viewmodel.InfoPostViewModel

class PostFragment : Fragment() {
    private val viewModel: InfoPostViewModel by viewModels()
    private lateinit var binding: FragmentPostBinding
    private val arguments: PostFragmentArgs by navArgs()

    private var currentPost: Post? = null

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

        // Configurar RecyclerView de Comentarios
        binding.recyclerComments.layoutManager = LinearLayoutManager(requireContext())
        viewModel.loadComments(idPost)

        viewModel.commentsList.observe(viewLifecycleOwner) { comments ->
            if (comments.isNullOrEmpty()) {
                binding.tvNoComments.visibility = View.VISIBLE
                binding.recyclerComments.adapter = CommentAdapter(emptyList(), idPost)
            } else {
                binding.tvNoComments.visibility = View.GONE
                binding.recyclerComments.adapter = CommentAdapter(comments, idPost) { pId, cId ->
                    viewModel.deleteComment(pId, cId)
                }
            }
        }

        viewModel.commentAdded.observe(viewLifecycleOwner) {
            binding.editCommentText.setText("")
            viewModel.loadComments(idPost)
            Snackbar.make(requireView(), "Comentario publicado", Snackbar.LENGTH_SHORT).show()
        }

        binding.btnSendComment.setOnClickListener {
            val text = binding.editCommentText.text.toString().trim()
            if (text.isBlank()) return@setOnClickListener

            val currentUser = Firebase.auth.currentUser
            if (currentUser == null || currentUser.isAnonymous || currentUser.email.isNullOrBlank()) {
                Utilities().showAlert(
                    requireContext(),
                    "Inicia Sesión",
                    "Debes iniciar sesión para comentar.",
                    "Aceptar",
                    "Cancelar",
                    false,
                    {
                        findNavController().navigate(R.id.action_navigation_home_to_navigation_login)
                    },
                    {}
                )
                return@setOnClickListener
            }

            db.collection("users")
                .whereEqualTo("userEmail", currentUser.email)
                .get()
                .addOnSuccessListener { query ->
                    if (!query.isEmpty && query.documents[0].getBoolean("isBanned") == true) {
                        val reason = query.documents[0].getString("banReason") ?: ""
                        Utilities().showAlert(
                            requireContext(),
                            "Cuenta Suspendida",
                            "Tu cuenta está suspendida y no puede comentar.\nMotivo: $reason",
                            "Entendido",
                            null,
                            false,
                            {},
                            {}
                        )
                        return@addOnSuccessListener
                    }

                    var userName = Utilities().getRegistSheredPref("userName", requireContext())
                    if (userName.isNullOrBlank()) {
                        userName = query.documents.firstOrNull()?.getString("userName") ?: "unknown"
                    }

                    val newComment = Comment(
                        userName = userName,
                        userEmail = currentUser.email ?: "",
                        commentText = text
                    )
                    val authorEmail = currentPost?.emailAutorPost ?: ""
                    val postTitle = currentPost?.tituloPost ?: ""
                    viewModel.addComment(idPost, newComment, authorEmail, postTitle)
                }
        }

        viewModel.postDetails.observe(viewLifecycleOwner) { info ->
            val post = info[0]
            currentPost = post
            // Actualizar la UI con los datos del post
            binding.viewPostTitle.text = post.tituloPost
            binding.viewPostCategory.text = post.categoriaPost

            val cuerpoTxt = binding.viewPostContent
            com.terfess.comunidadmp.classes.MarkdownUtils.renderMarkdown(cuerpoTxt, post.cuerpoPost)

            if (post.imagenPost.isNotEmpty()) {
                recyclerimage.adapter =
                    AdapterHolderViewPostImage(info[0].imagenPost, requireContext())
            } else {
                binding.tvDeslize.visibility = View.GONE
            }

            // Cargar foto y nombre de perfil del autor
            db.collection("users")
                .whereEqualTo("userEmail", post.emailAutorPost)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        val nameAuthor = document.getString("userName")
                        val photoAuthor = document.getString("userPhoto")

                        if (!photoAuthor.isNullOrBlank()) {
                            com.bumptech.glide.Glide.with(this)
                                .load(photoAuthor)
                                .placeholder(R.drawable.predet_image)
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                                .into(binding.imgAuthorDetail)
                        }

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
