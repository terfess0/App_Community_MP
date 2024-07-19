package com.terfess.comunidadmp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.model.Post
import android.view.View
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.terfess.comunidadmp.classes.Roles
import com.terfess.comunidadmp.classes.UserRole.getCurrentUserRole
import com.terfess.comunidadmp.databinding.FormatoNewsRecyclerBinding
import com.terfess.comunidadmp.model.UserProvider
import com.terfess.comunidadmp.view.HomeFragmentDirections
import com.terfess.comunidadmp.viewmodel.PostViewModel

class AdapterHolderHome(private var data: List<Post>, private val viewModel: PostViewModel) :
    RecyclerView.Adapter<AdapterHolderHome.Holder>() {
    private val db = FirebaseFirestore.getInstance()
    private val user = Firebase.auth.currentUser

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
        if (position % 5 == 0) {
            holder.bind(currentItem, true)
        } else {
            holder.bind(currentItem, false)
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
            if (showAd) binding.adView.visibility = View.VISIBLE

            // Visibilidad de la imagen del post
            if (post.imagenPost.isNotEmpty()) {
                containImg.visibility = View.VISIBLE
                // Cargar imagen del post
                Glide.with(itemView)
                    .load(post.imagenPost[0])
                    .placeholder(R.drawable.predet_image)
                    .into(imagenP)
            } else {
                containImg.visibility = View.GONE
            }

            //visibilidad opcion de save post (solo en home, en favorites no)
            val btn_save_post = binding.btnSavePost
            btn_save_post.visibility = View.VISIBLE
            btn_save_post.setOnClickListener {
                //al tocar se guarda el post y el boton desaparece
                btn_save_post.setImageResource(R.drawable.ic_post_saved)
                viewModel.addFavorite(user?.email.toString(), post.idPost)
                btn_save_post.isClickable = false
                btn_save_post.isEnabled = false
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

    fun setData(newItems: List<Post>) {
        data = newItems
        notifyDataSetChanged()
    }

    fun loadAd(view: View) {
        //ads
        val adView = view.findViewById<AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }
}

