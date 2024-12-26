package com.terfess.comunidadmp.view

import android.os.Bundle
import android.renderscript.Script.FieldBase
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.adapters.AdapterHolderHome
import com.terfess.comunidadmp.classes.Roles
import com.terfess.comunidadmp.classes.UserRole
import com.terfess.comunidadmp.classes.Utilities
import com.terfess.comunidadmp.databinding.ActivityMainBinding
import com.terfess.comunidadmp.databinding.FragmentHomeBinding
import com.terfess.comunidadmp.services.FirebaseAuth
import com.terfess.comunidadmp.viewmodel.PostViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adaptador: AdapterHolderHome
    private lateinit var authUtilsInst: FirebaseAuth

    private val postViewModel: PostViewModel by viewModels()
    private var numPosts = 0


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // call to function get num posts Firestore
        postViewModel.getCountPost()

        // call to function for get user info
        postViewModel.getUserAccount()

        authUtilsInst = FirebaseAuth()

        // Obtener el estado de login desde SharedPreferences
        val loginState = Utilities().getRegistSheredPref("LoginState", requireContext())

        // Si no esta logeado entonces logear como anonimo
        if (loginState != "true" && loginState != "partial") {
            authUtilsInst.loginAnonymously()

            // Save state login anonymously
            Utilities().saveSharedpref(requireContext(), "LoginState", "partial")
        }

        val progressBar = binding.progressShimer
        val recicler = binding.contenido


        adaptador = AdapterHolderHome(mutableListOf(), postViewModel)
        recicler.adapter = adaptador
        recicler.layoutManager = LinearLayoutManager(requireContext())

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
            //save userRole un shared preferences local
            Utilities().saveSharedpref(requireContext(), "userRole", it.userRole)
            //verify user role local
            verifyUserRole()
        })

        postViewModel.numTotalPost.observe(viewLifecycleOwner, Observer {
            println("Numero de post: $it")
            numPosts = it
            // call to function get posts
            postViewModel.getPosts(null)
        })

        postViewModel.postViewModel.observe(this.viewLifecycleOwner, Observer { items ->
            // Actualiza tu UI con la lista de items
            print("Valor numPost 1: $numPosts ")
            numPosts -= 10
            println("Valor numPost 2: $numPosts")
            if (numPosts != 0) {
                val numPaginReq = 10

                if (numPosts >= 10) {
                    adaptador.setData(items, numPaginReq)
                } else {
                    adaptador.setData(items, numPosts)
                }
            }

            progressBar.visibility = View.GONE

            // Mostrar o ocultar el texto de "No hay publicaciones" según si la lista está vacía
            if (items == null) {
                binding.noPostsText.visibility = View.VISIBLE
            } else if (items.isEmpty()) {
                binding.noPostsText.visibility = View.VISIBLE
            } else {
                binding.noPostsText.visibility = View.GONE
            }

            // Cancel swipe anim
            binding.swiperefresh.isRefreshing = false
        })

        postViewModel.resultAddFavorite.observe(this@HomeFragment.viewLifecycleOwner, Observer { result ->
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
        //clean last doc in view model
        postViewModel.currentLastDoc.postValue(null)

        adaptador.deleteData()

        postViewModel.currentLastDoc.observe(viewLifecycleOwner, Observer {
            postViewModel.getCountPost()
        })

        //hide swipe refresh at 1 second
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            binding.swiperefresh.isRefreshing = false
        }
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