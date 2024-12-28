package com.terfess.comunidadmp.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.terfess.comunidadmp.adapters.AdapterHolderMedia
import com.terfess.comunidadmp.databinding.FragmentMediaBinding
import com.terfess.comunidadmp.model.PostMedia
import com.terfess.comunidadmp.viewmodel.MediaViewModel

class MediaFragment : Fragment() {
    private var _binding: FragmentMediaBinding? = null
    private val binding get() = _binding!!

    private lateinit var adaptador: AdapterHolderMedia
    private val mediaViewModel: MediaViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recicler = binding.contentMedia

        adaptador = AdapterHolderMedia(mutableListOf(), mediaViewModel)
        recicler.adapter = adaptador
        recicler.layoutManager = GridLayoutManager(requireContext(), 2)

        val list = listOf(
            PostMedia(
                "2024-12-27",
                "John Doe",
                "Summer Beach",
                "Enjoy the summer vibes at the beach. Enjoy the summer vibes at the beach.",

                "https://example.com/summer-beach.jpg",
                "https://example.com/summer-beach-thumb.jpg",
                "post001",
                "1"
            )
        )

        adaptador.setData(list, 10)
    }
}