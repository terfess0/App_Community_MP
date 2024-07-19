package com.terfess.comunidadmp.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.terfess.comunidadmp.adapters.AdapterHolderFavorites
import com.terfess.comunidadmp.classes.Utilities
import com.terfess.comunidadmp.databinding.FragmentFavoritesBinding
import com.terfess.comunidadmp.viewmodel.FavoritesViewModel

class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private lateinit var adaptador: AdapterHolderFavorites

    private val favoritesViewModel: FavoritesViewModel by viewModels()

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Inicializar RecyclerView y ProgressBar
        val progressBar = binding.progressShimer
        val reciclerGuardados = binding.contenido
        adaptador = AdapterHolderFavorites(emptyList(), favoritesViewModel)
        reciclerGuardados.adapter = adaptador
        reciclerGuardados.layoutManager = LinearLayoutManager(requireContext())


        favoritesViewModel.getFavorites()


        // Observar cambios en la lista de favoritos
        favoritesViewModel.postsFavorites.observe(viewLifecycleOwner, Observer { favoritos ->
            // Actualizar UI con la lista de favoritos
            adaptador.setData(favoritos)

            progressBar.visibility = View.GONE

            // Mostrar o ocultar el texto de "No hay publicaciones" según si la lista está vacía
            if (favoritos == null) {
                binding.noPostsText.visibility = View.VISIBLE
            } else if (favoritos.isEmpty()) {
                binding.noPostsText.visibility = View.VISIBLE
            } else {
                binding.noPostsText.visibility = View.GONE
            }
        })

        favoritesViewModel.resultRemoveFavorite.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                "SUCCESS" -> {
//                    Snackbar.make(requireView(), "Favorito eliminado", Snackbar.LENGTH_SHORT).show()
                }

                "NO_EXISTS" -> {
//                    Snackbar.make(
//                        requireView(),
//                        "Este favorito ya fué eliminado",
//                        Snackbar.LENGTH_SHORT
//                    ).show()
                }

                else -> {
                    Snackbar.make(
                        requireView(),
                        "Algo salio mal, no se pudo eliminar el favorito",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            favoritesViewModel.getFavorites()
        })

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
