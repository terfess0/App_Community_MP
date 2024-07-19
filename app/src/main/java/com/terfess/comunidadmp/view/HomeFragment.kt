package com.terfess.comunidadmp.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.adapters.AdapterHolderHome
import com.terfess.comunidadmp.classes.Roles
import com.terfess.comunidadmp.classes.UserRole
import com.terfess.comunidadmp.classes.Utilities
import com.terfess.comunidadmp.databinding.FragmentHomeBinding
import com.terfess.comunidadmp.viewmodel.PostViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adaptador: AdapterHolderHome

    private val postViewModel: PostViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // call to function get posts Firestore
        postViewModel.getPosts()
        // call to function for get user info
        postViewModel.getUserAccount()


        // Obtener el estado de login desde SharedPreferences
        val loginState = Utilities().getRegistSheredPref("LoginState", requireContext())

        // Navegar a login si no esta logeado
        if (loginState != "true") {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_login)
        }

        val progressBar = binding.progressShimer
        val recicler = binding.contenido


        adaptador = AdapterHolderHome(emptyList(), postViewModel)
        recicler.adapter = adaptador
        recicler.layoutManager = LinearLayoutManager(requireContext())

        binding.newPublisBtn.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_writePost)
        }

        // Escucha el evento de deslizar para recargar
        recicler.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) PostViewModel().getPosts()

            false
        }

        //-------------------OBSERVERS

        postViewModel.dataUser.observe(viewLifecycleOwner, Observer {

            //save userRole un shared preferences local
            Utilities().saveSharedpref(requireContext(), "userRole", it.userRole)
            //verify user role local
            verifyUserRole()
        })

        postViewModel.postViewModel.observe(viewLifecycleOwner, Observer { items ->
            // Actualiza tu UI con la lista de items
            adaptador.setData(items)

            progressBar.visibility = View.GONE

            // Mostrar o ocultar el texto de "No hay publicaciones" según si la lista está vacía
            if (items == null) {
                binding.noPostsText.visibility = View.VISIBLE
            } else if (items.isEmpty()) {
                binding.noPostsText.visibility = View.VISIBLE
            } else {
                binding.noPostsText.visibility = View.GONE
            }
        })

        postViewModel.resultAddFavorite.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                "SUCCESS" -> {
//                    Snackbar.make(requireView(), "Post guardado", Snackbar.LENGTH_SHORT).show()
                }

                "ALREADY_EXISTS" -> {
//                    Snackbar.make(requireView(), "Ya guardaste este post", Snackbar.LENGTH_SHORT).show()
                }

                else -> {
                    Snackbar.make(
                        requireView(),
                        "Algo salio mal, no se pudo guardar el favorito",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        })

        postViewModel.resultDelPost.observe(viewLifecycleOwner, Observer { resultado ->
            if (resultado == "SUCCESS") {
//                Snackbar.make(requireView(), "Post eliminado", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(
                    requireView(),
                    "Algo salio mal, no se pudo eliminar el Post",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            postViewModel.getPosts()
        })


        postViewModel.errorGetPost.observe(viewLifecycleOwner, Observer { error ->
            when (error) {
                "PERMISSION_DENIED" -> {
                    progressBar.visibility = View.GONE
                    Snackbar.make(requireView(), "Debe iniciar sesión", Snackbar.LENGTH_SHORT)
                        .show()
                    Utilities().saveSharedpref(requireContext(), "LoginState", "false")

                    findNavController().navigate(R.id.action_navigation_home_to_navigation_login)
                }

                "UNKNOWN_ERROR" -> {
                    binding.errorReload.visibility = View.VISIBLE
                    binding.reloadPosts.setOnClickListener {
                        binding.errorReload.visibility = View.GONE
                        postViewModel.getPosts()
                    }
                }
            }

        })


        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val alert = AlertDialog.Builder(requireContext())
                    alert.setMessage("¿Quieres salir de la app?")
                    alert.setPositiveButton("Salir") { _, _ ->
                        requireActivity().finish() // Cierra la actividad actual
                    }
                    alert.setNegativeButton("Cancelar") { dialog, _ ->
                        dialog.dismiss() // Cerrar el diálogo sin realizar ninguna acción adicional
                    }
                    alert.show()

                }
            })

        //----------------------------------------

    }

    private fun verifyUserRole() {
        val role = Utilities().getRegistSheredPref("userRole", requireContext())
        when (role) {
            "USER" -> {
                UserRole.setUserRole(Roles.USER)
            }

            "ADMIN" -> {
                UserRole.setUserRole(Roles.ADMIN)
            }

            else -> {
                UserRole.setUserRole(Roles.USER)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}