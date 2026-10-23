package com.terfess.comunidadmp.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.adapters.AdapterHolderHome
import com.terfess.comunidadmp.classes.Roles
import com.terfess.comunidadmp.classes.UserRole
import com.terfess.comunidadmp.classes.Utilities
import com.terfess.comunidadmp.databinding.FragmentHomeBinding
import com.terfess.comunidadmp.model.Post
import com.terfess.comunidadmp.services.FirebaseAuth
import com.terfess.comunidadmp.viewmodel.PostViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adaptador: AdapterHolderHome
    private lateinit var authUtilsInst: FirebaseAuth

    private val postViewModel: PostViewModel by activityViewModels()
    private var numPosts = 0

    private var listPost = mutableListOf<Post>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        authUtilsInst = FirebaseAuth()

        // Obtener el estado de login desde SharedPreferences
        val loginState = Utilities().getRegistSheredPref("LoginState", requireContext())

        // Si no esta logeado entonces logear como anonimo
        if (loginState != "true" && loginState != "partial") {
            authUtilsInst.loginAnonymously()
            Utilities().saveSharedpref(requireContext(), "LoginState", "partial")
        }

        val progressBar = binding.progressShimer
        val recicler = binding.contenido

        adaptador = AdapterHolderHome(mutableListOf(), postViewModel)
        recicler.adapter = adaptador
        recicler.layoutManager = LinearLayoutManager(requireContext())

        // Filtrado por categoría
        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipAll
            filterPostsByCategory(checkedId)
        }

        // Si ya hay posts cargados en el ViewModel, mostrarlos sin re-activar shimmer
        if (postViewModel.isInitialLoaded && postViewModel.allLoadedPosts.isNotEmpty()) {
            progressBar.visibility = View.GONE
            listPost = postViewModel.allLoadedPosts.toMutableList()
            val currentChipId = binding.chipGroupCategories.checkedChipId
            if (currentChipId != View.NO_ID && currentChipId != R.id.chipAll) {
                filterPostsByCategory(currentChipId)
            } else {
                adaptador.setData(listPost, 10)
            }
            binding.noPostsText.visibility = View.GONE
        } else {
            progressBar.visibility = View.VISIBLE
        }

        // Obtener conteo y verificar si hay nuevos posts en segundo plano
        postViewModel.getCountPost()

        // Obtener datos del usuario
        postViewModel.getUserAccount()

        // Banner de nuevos posts disponibles
        postViewModel.hasNewPostsAvailable.observe(viewLifecycleOwner) { hasNew ->
            binding.tvNewPostsAvailable.visibility = if (hasNew) View.VISIBLE else View.GONE
        }

        binding.tvNewPostsAvailable.setOnClickListener {
            binding.tvNewPostsAvailable.visibility = View.GONE
            binding.contenido.smoothScrollToPosition(0)
            binding.swiperefresh.isRefreshing = true
            refreshPosts()
        }

        binding.newPublisBtn.setOnClickListener {
            // Verify auth state
            val isAnony = authUtilsInst.sessionIsAnonymously()
            if (isAnony) {
                Utilities().showAlert(
                    requireContext(),
                    "Inicia Sesión",
                    "Para publicar debes iniciar sesión",
                    "Aceptar",
                    null,
                    false,
                    {
                        findNavController().navigate(R.id.action_navigation_home_to_navigation_login)
                    },
                    {}
                )
            } else {
                findNavController().navigate(R.id.action_navigation_home_to_writePost)
            }
        }

        // Listener at swipe to refresh
        binding.swiperefresh.setOnRefreshListener {
            refreshPosts()
        }

        //-------------------OBSERVERS

        postViewModel.dataUser.observe(viewLifecycleOwner, Observer {
            Utilities().saveSharedpref(requireContext(), "userRole", it.userRole)
            verifyUserRole()
        })

        postViewModel.numTotalPost.observe(viewLifecycleOwner) { total ->
            numPosts = total
            if (!postViewModel.isInitialLoaded) {
                progressBar.visibility = View.VISIBLE
                postViewModel.getPosts(10)
            }
        }

        postViewModel.postViewModel.observe(viewLifecycleOwner) { items ->
            listPost = items.toMutableList()

            val currentChipId = binding.chipGroupCategories.checkedChipId
            if (currentChipId != View.NO_ID && currentChipId != R.id.chipAll) {
                filterPostsByCategory(currentChipId)
            } else {
                val remaining = numPosts - listPost.size
                val nextPag = if (remaining >= 10) 10 else if (remaining > 0) remaining else 0
                adaptador.setData(listPost, nextPag)
            }

            if (items.isEmpty()) {
                binding.noPostsText.visibility = View.VISIBLE
            } else {
                binding.noPostsText.visibility = View.GONE
            }

            binding.swiperefresh.isRefreshing = false
            progressBar.visibility = View.GONE
        }

        postViewModel.resultAddFavorite.observe(
            this@HomeFragment.viewLifecycleOwner,
            Observer { result ->
                when (result) {
                    "SUCCESS" -> {
//                    Utilities().showAlert(
//                        requireContext(),
//                        "Post guardado",
//                        "El post se guardo correctamente",
//                        "Aceptar",
//                        null,
//                        false,
//                        {},
//                        {}
//                    )
                    }

                    "ALREADY_EXISTS" -> {
//                    Utilities().showAlert(
//                        requireContext(),
//                        "Post guardado",
//                        "El post ya estaba guardado",
//                        "Aceptar",
//                        null,
//                        false,
//                        {},
//                        {}
//                    )

                    }

                    else -> {
//                    Snackbar.make(
//                        requireView(),
//                        "Algo salio mal, no se pudo guardar el favorito",
//                        Snackbar.LENGTH_SHORT
//                    ).show()
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

            postViewModel.getPosts(null)
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
                        postViewModel.getPosts(null)
                    }
                }
            }

        })


        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    Utilities().showAlert(
                        requireContext(),
                        "Salir de la app",
                        "¿Quieres salir de la app?",
                        "Aceptar",
                        "Permanecer",
                        true,
                        { requireActivity().finish() },
                        {}
                    )
                }
            })

        //----------------------------------------

    }

    private fun refreshPosts() {
        binding.tvNewPostsAvailable.visibility = View.GONE
        postViewModel.refreshAllPosts()
    }

    private fun filterPostsByCategory(checkedId: Int) {
        val filtered = when (checkedId) {
            R.id.chipGeneral -> listPost.filter { it.categoriaPost.equals("General", ignoreCase = true) }
            R.id.chipFacciones -> listPost.filter { it.categoriaPost.contains("Facciones", ignoreCase = true) }
            R.id.chipRoleplay -> listPost.filter { it.categoriaPost.equals("Roleplay", ignoreCase = true) }
            R.id.chipGuias -> listPost.filter { it.categoriaPost.contains("Guías", ignoreCase = true) || it.categoriaPost.contains("Guias", ignoreCase = true) }
            R.id.chipAnuncios -> listPost.filter { it.categoriaPost.equals("Anuncios", ignoreCase = true) }
            else -> listPost
        }
        adaptador.setData(filtered.toMutableList(), 10)
        adaptador.notifyDataSetChanged()
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