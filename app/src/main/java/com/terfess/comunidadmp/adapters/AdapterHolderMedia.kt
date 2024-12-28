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
import com.terfess.comunidadmp.classes.Roles
import com.terfess.comunidadmp.classes.UserRole.getCurrentUserRole
import com.terfess.comunidadmp.databinding.FormatMediaRecyclerBinding
import com.terfess.comunidadmp.model.PostMedia
import com.terfess.comunidadmp.model.UserProvider
import com.terfess.comunidadmp.view.HomeFragmentDirections
import com.terfess.comunidadmp.viewmodel.MediaViewModel

class AdapterHolderMedia(
    private var data: MutableList<PostMedia>,
    private val viewModel: MediaViewModel
) :
    RecyclerView.Adapter<AdapterHolderMedia.HolderMedia>() {
    private val db = FirebaseFirestore.getInstance()
    private val user = Firebase.auth.currentUser
    private var numNextPag = 10

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderMedia {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.format_media_recycler, parent, false)

        return HolderMedia(itemView)
    }

    override fun onBindViewHolder(holder: HolderMedia, position: Int) {
        val currentItem = data[position]
        holder.bind(currentItem)
//        if (data.size >= 10){
//            if (position > data.size - 3 && numNextPag > 0){
//                viewModel.getPosts(numNextPag)
//            }
//        }
    }

    override fun getItemCount() = data.size

    inner class HolderMedia(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var binding = FormatMediaRecyclerBinding.bind(itemView)
        private val nombreAutor = binding.nickAuthorMedia
        private val titleTextView = binding.tituloPMedia
        private val cuerpoPost = binding.cuerpoPMedia
        private val perfilAutorImg = binding.imgProfileAuthorMedia
        private val datePost = binding.datePostMedia
        private val txtCountLikes = binding.txtCountLikesPMedia


        fun bind(post: PostMedia) {
            // Mostrar publicación
            nombreAutor.text = post.autorPostMedia
            titleTextView.text = post.tituloPostMedia
            cuerpoPost.text = post.cuerpoPostMedia
            datePost.text = post.fechaPostMedia.toString().substring(0, 10)
            txtCountLikes.text = post.likesPostMediaCount

            binding.linkStream.text = post.tituloPostMedia

            //visibilidad opcion de save post (solo en home, en favorites no)
            val btn_save_post = binding.btnPlay
            btn_save_post.visibility = View.VISIBLE
            btn_save_post.setOnClickListener {
                //al tocar se guarda el post y el boton desaparece
                btn_save_post.setImageResource(R.drawable.ic_post_saved)
//                viewModel.addFavorite(user?.email.toString(), post.idPostMedia)
                btn_save_post.isClickable = false
                btn_save_post.isEnabled = false
            }

            perfilAutorImg.setImageResource(R.drawable.predet_image)

            // Cargar foto de perfil del autor
            db.collection("users")
                .whereEqualTo("userEmail", post.emailAutorPostMedia)
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
                    HomeFragmentDirections.actionNavigationHomeToPostFragment(postDocumentId = post.idPostMedia)
                )
            }

            val rol = getCurrentUserRole()

            binding.deletePostMedia.visibility = if (rol == Roles.ADMIN) {
                View.VISIBLE
            } else {
                View.GONE
            }

            //a user with admin role delete post
            binding.deletePostMedia.setOnClickListener {
//                viewModel.deletePostMediaAdmin(post.idPostMedia)
                UserProvider().setUserCounterPost(post.emailAutorPostMedia, "DEL")
            }
        }


    }

    fun setData(newItems: List<PostMedia>, numNextPaginRequest: Int) {
        numNextPag = numNextPaginRequest
        val start = data.size
        data.addAll(newItems)
        notifyItemRangeInserted(start, newItems.size)
    }

    fun deleteData() {
        data.clear()
        notifyDataSetChanged()
    }
}

