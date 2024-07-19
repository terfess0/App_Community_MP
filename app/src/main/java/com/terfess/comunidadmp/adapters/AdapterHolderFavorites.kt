package com.terfess.comunidadmp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.databinding.FormatoNewsRecyclerBinding
import com.terfess.comunidadmp.model.Post
import com.terfess.comunidadmp.view.FavoritesFragmentDirections
import com.terfess.comunidadmp.viewmodel.FavoritesViewModel

class AdapterHolderFavorites(
    private var data: List<Post>,
    private val viewModel: FavoritesViewModel
) :
    RecyclerView.Adapter<AdapterHolderFavorites.Holder>() {
    private val db = FirebaseFirestore.getInstance()
    private val user = Firebase.auth.currentUser

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.formato_news_recycler, parent, false)
        return Holder(itemView)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val currentItem = data[position]
        holder.bind(currentItem)
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

        fun bind(post: Post) {
            // Mostrar publicación
            nombreAutor.text = post.autorPost
            titleTextView.text = post.tituloPost
            cuerpoPost.text = post.cuerpoPost
            datePost.text = post.fechaPost.toString().substring(0, 10)

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


            val btnUnSavePost = binding.btnSavePost
            btnUnSavePost.visibility = View.VISIBLE
            btnUnSavePost.setImageResource(R.drawable.ic_post_saved)

            btnUnSavePost.setOnClickListener {
                //al tocar se guarda el post y el boton desaparece
                btnUnSavePost.visibility = View.GONE
                viewModel.removeFavorite(user?.email.toString(), post.idPost)
                btnUnSavePost.isClickable = false
                btnUnSavePost.isEnabled = false
            }

            //usuario selecciona clickea publicacion
            itemView.setOnClickListener {
                binding.root.findNavController().navigate(
                    FavoritesFragmentDirections.actionFavoritesFragmentToPostFragment(postDocumentId = post.idPost)
                )
                println("Se navegó de guardados en recicler hacia PostFragment con args")
            }
        }
    }

    fun setData(newItems: List<Post>) {
        data = newItems
        notifyDataSetChanged()
    }
}
