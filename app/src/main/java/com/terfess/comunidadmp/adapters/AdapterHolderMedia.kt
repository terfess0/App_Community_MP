package com.terfess.comunidadmp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.classes.Roles
import com.terfess.comunidadmp.classes.UserRole.getCurrentUserRole
import com.terfess.comunidadmp.classes.Utilities
import com.terfess.comunidadmp.databinding.FormatMediaRecyclerBinding
import com.terfess.comunidadmp.model.PostMedia
import com.terfess.comunidadmp.model.UserProvider
import com.terfess.comunidadmp.view.MediaFragment
import com.terfess.comunidadmp.viewmodel.MediaViewModel

class AdapterHolderMedia(
    private var data: MutableList<PostMedia>,
    private val viewModel: MediaViewModel,
    private val instanceMediaF: MediaFragment
) :
    RecyclerView.Adapter<AdapterHolderMedia.HolderMedia>() {
    private val db = FirebaseFirestore.getInstance()
    private val user = Firebase.auth.currentUser
    private var numNextPag = 10
    private var mediaOnFocus : ImageButton? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderMedia {
        val itemView =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.format_media_recycler, parent, false)

        return HolderMedia(itemView)
    }

    override fun onBindViewHolder(holder: HolderMedia, position: Int) {
        val currentItem = data[position]
        holder.bind(currentItem)

        if (data.size >= 10) {
            if (position > data.size - 1 && numNextPag > 0) {
                viewModel.getMediaPosts(numNextPag)
            }
        }
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
        private val linkTxt = binding.linkStream


        fun bind(post: PostMedia) {
            // Mostrar publicación
            nombreAutor.text = post.autorPostMedia
            titleTextView.text = post.tituloPostMedia
            cuerpoPost.text = post.cuerpoPostMedia
            datePost.text = Utilities().formatCustomDate(post.fechaPostMedia)
            txtCountLikes.text = post.likesPostMediaCount
            linkTxt.text = post.linkStreamPostMedia

            //visibilidad opcion de save post (solo en home, en favorites no)
            val btnLikePost = binding.btnLike

            btnLikePost.setOnClickListener {
                val currentUser = Firebase.auth.currentUser
                if (currentUser == null || currentUser.isAnonymous || currentUser.email.isNullOrBlank()) {
                    Utilities().showAlert(
                        itemView.context,
                        "Inicia Sesión",
                        "Para dar me gusta debes iniciar sesión",
                        "Iniciar sesión",
                        "Cancelar",
                        false,
                        {
                            binding.root.findNavController().navigate(R.id.action_mediaFragment_to_navigation_login)
                        },
                        {}
                    )
                } else {
                    btnLikePost.setImageResource(R.drawable.ic_like_heart)
                    viewModel.likeMediaPost(post.idPostMedia)
                    btnLikePost.isClickable = false
                    btnLikePost.isEnabled = false
                }
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


            val rol = getCurrentUserRole()

            binding.deletePostMedia.visibility = if (rol == Roles.ADMIN) {
                View.VISIBLE
            } else {
                View.GONE
            }

            //a user with admin role delete post
            binding.deletePostMedia.setOnClickListener {
                viewModel.deleteMediaPost(post.idPostMedia)
                UserProvider().setUserCounterPost(post.emailAutorPostMedia, "DEL")
            }

            binding.btnPlay.setOnClickListener {
                removeMediaFocus()
                mediaOnFocus = binding.btnPlay

                instanceMediaF.starPlayer(
                    post.linkStreamPostMedia,
                    post.tituloPostMedia,
                    mediaOnFocus!!
                )
            }

            binding.btnCopy.setOnClickListener {
                Utilities().copyText(binding.linkStream.text.toString(), binding.root.context)
            }
        }


    }

    fun setData(newItems: List<PostMedia>, numNextPaginRequest: Int) {
        numNextPag = numNextPaginRequest
        data.clear()
        data.addAll(newItems)
        notifyDataSetChanged()
    }

    fun deleteData() {
        data.clear()
        notifyDataSetChanged()
    }

    fun removeMediaFocus(){
        if (mediaOnFocus != null) mediaOnFocus?.setImageResource(R.drawable.ic_play_music)
    }

}

