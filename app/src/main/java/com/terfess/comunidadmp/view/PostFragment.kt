package com.terfess.comunidadmp.view

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
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



        var adapter = AdapterHolderViewPostImage(emptyList(), requireContext())
        val recyclerimage = binding.viewPostImages
        recyclerimage.adapter = adapter
        recyclerimage.layoutManager = LinearLayoutManager(requireContext())



        viewModel.postDetails.observe(viewLifecycleOwner) { info ->
            val post = info[0]
            // Actualizar la UI con los datos del post
            binding.viewPostTitle.text = post.tituloPost
            binding.viewPostContent.text = post.cuerpoPost

            if (post.imagenPost.isNotEmpty()) {
                recyclerimage.adapter = AdapterHolderViewPostImage(info[0].imagenPost, requireContext())
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
            println("Se navegó de postFragment a HomeFragment")
        }

        return binding.root
    }
}
