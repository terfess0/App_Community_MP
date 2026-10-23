package com.terfess.comunidadmp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.model.Post
import android.view.View
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.terfess.comunidadmp.classes.Roles
import com.terfess.comunidadmp.classes.UserRole.getCurrentUserRole
import com.terfess.comunidadmp.classes.Utilities
import com.terfess.comunidadmp.databinding.FormatoNewsRecyclerBinding
import com.terfess.comunidadmp.model.UserProvider
import com.terfess.comunidadmp.view.HomeFragmentDirections
import com.terfess.comunidadmp.viewmodel.PostViewModel

class AdapterHolderHome(private var data: MutableList<Post>, private val viewModel: PostViewModel) :
    RecyclerView.Adapter<AdapterHolderHome.Holder>() {
    private val db = FirebaseFirestore.getInstance()
    private val user = Firebase.auth.currentUser
    private var numNextPag = 10

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.formato_news_recycler, parent, false)

        loadAd(itemView)

        return Holder(itemView)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val currentItem = data[position]

        //show ad each 5 post
        if (position % 8 == 0) {
            holder.bind(currentItem, true)
        } else {
            holder.bind(currentItem, false)
        }

        if (data.size >= 10) {
            if (position > data.size - 3 && numNextPag > 0) {
                viewModel.getPosts(numNextPag)
            }
        }

    }

    override fun getItemCount() = data.size

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var binding = FormatoNewsRecyclerBinding.bind(itemView)
        private val nombreAutor = binding.nickAuthor
        private val titleTextView = binding.tituloP
        private val cuerpoPost = binding.cuerpoP
        private val imagenP = binding.imgP
        private val perfilAutorImg = binding.imgProfileAuthor
        private val datePost = binding.datePost
        private val containImg = binding.containImgP


        fun bind(post: Post, showAd: Boolean) {
            // Mostrar publicación
            nombreAutor.text = post.autorPost
            titleTextView.text = post.tituloPost
            cuerpoPost.text = post.cuerpoPost
            datePost.text = post.fechaPost.toString().substring(0, 10)

            //show ad
            val visibil = if (showAd) View.VISIBLE else View.GONE
            binding.adView.visibility = visibil

            // Visibilidad de la imagen del post
            if (post.imagenPost.isNotEmpty()) {
                containImg.visibility = View.VISIBLE
                // Cargar imagen del post
                Glide.with(itemView)
                    .load(post.imagenPost[0])
                    .placeholder(R.drawable.predet_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imagenP)
            } else {
                containImg.visibility = View.GONE
            }

            //visibilidad opcion de save post (solo en home, en favorites no)
            val btn_save_post = binding.btnSavePost
            btn_save_post.visibility = View.VISIBLE
            btn_save_post.setOnClickListener {
                val currentUser = Firebase.auth.currentUser
                if (currentUser == null || currentUser.isAnonymous || currentUser.email.isNullOrBlank()) {
                    Utilities().showAlert(
                        itemView.context,
                        "Inicia Sesión",
                        "Para guardar publicaciones debes iniciar sesión",
                        "Iniciar sesión",
                        "Cancelar",
                        false,
                        {
                            binding.root.findNavController().navigate(R.id.action_navigation_home_to_navigation_login)
                        },
                        {}
                    )
                } else {
                    btn_save_post.setImageResource(R.drawable.ic_post_saved)
                    viewModel.addFavorite(currentUser.email.toString(), post.idPost)
                    btn_save_post.isClickable = false
                    btn_save_post.isEnabled = false
                }
            }

            perfilAutorImg.setImageResource(R.drawable.predet_image)

            // Cargar foto de perfil del autor
            db.collection("users")
                .whereEqualTo("userEmail", post.emailAutorPost)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        val photoUser = document.getString("userPhoto")
                        val nameAuthor = document.getString("userName")

                        if (!photoUser.isNullOrBlank()) {
                            Glide.with(itemView)
                                .load(photoUser)
                                .placeholder(R.drawable.predet_image)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(perfilAutorImg)
                        }

                        if (nameAuthor?.isNotBlank() == true) {
                            nombreAutor.text = nameAuthor
                        } else {
                            nombreAutor.text = "unknown"
                        }
                    }
                }

            //usuario selecciona clickea publicacion
            itemView.setOnClickListener {
                binding.root.findNavController().navigate(
                    HomeFragmentDirections.actionNavigationHomeToPostFragment(postDocumentId = post.idPost)
                )
            }

            // Reportar publicación (UGC Google Play)
            binding.btnReportPost.setOnClickListener {
                val context = itemView.context
                val reasons = arrayOf(
                    "Spam o publicidad no deseada",
                    "Contenido ofensivo o lenguaje inapropiado",
                    "Aporte malicioso / Stealer / Mod con crash",
                    "Acoso o difamación",
                    "Otro motivo"
                )

                androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Reportar publicación")
                    .setItems(reasons) { dialog, which ->
                        val selectedReason = reasons[which]
                        val currentUser = Firebase.auth.currentUser
                        val reportData = hashMapOf(
                            "idPost" to post.idPost,
                            "postTitle" to post.tituloPost,
                            "reportedAuthorEmail" to post.emailAutorPost,
                            "reporterEmail" to (currentUser?.email ?: "guest"),
                            "reason" to selectedReason,
                            "date" to com.google.firebase.Timestamp.now()
                        )

                        db.collection("reports")
                            .add(reportData)
                            .addOnSuccessListener {
                                com.google.android.material.snackbar.Snackbar.make(
                                    itemView,
                                    "Gracias por tu reporte. Nuestro equipo de moderación lo revisará.",
                                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                                ).show()
                            }
                            .addOnFailureListener {
                                com.google.android.material.snackbar.Snackbar.make(
                                    itemView,
                                    "Error al enviar reporte. Inténtalo más tarde.",
                                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancelar") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }

            val rol = getCurrentUserRole()

            binding.deletePost.visibility = if (rol == Roles.ADMIN) {
                View.VISIBLE
            } else {
                View.GONE
            }

            //a user with admin role delete post
            binding.deletePost.setOnClickListener {
                viewModel.deletePostAdmin(post.idPost)
                UserProvider().setUserCounterPost(post.emailAutorPost, "DEL")
            }
        }


    }

    fun setData(newItems: List<Post>, numNextPaginRequest: Int) {
        numNextPag = numNextPaginRequest
        println("Numero New de posts en lista:: -> ${data.size}")
        data.clear()
        data.addAll(newItems)
        println("Numero New de posts en lista:: -> ${data.size}")
        notifyDataSetChanged()
    }

    fun loadAd(view: View) {
        //ads
        val adView = view.findViewById<AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    fun deleteData() {
        data.clear()
        notifyDataSetChanged()
    }
}

