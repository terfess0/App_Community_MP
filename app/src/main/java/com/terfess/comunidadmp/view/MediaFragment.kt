package com.terfess.comunidadmp.view

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.adapters.AdapterHolderMedia
import com.terfess.comunidadmp.classes.Utilities
import com.terfess.comunidadmp.databinding.FragmentMediaBinding
import com.terfess.comunidadmp.model.PostMedia
import com.terfess.comunidadmp.services.AudioService
import com.terfess.comunidadmp.services.FirebaseAuth
import com.terfess.comunidadmp.viewmodel.MediaViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MediaFragment : Fragment() {
    private var _binding: FragmentMediaBinding? = null
    private val binding get() = _binding!!

    private lateinit var adaptador: AdapterHolderMedia
    private val mediaViewModel: MediaViewModel by viewModels()

    private var numPosts = 0
    private lateinit var authUtilsInst: FirebaseAuth

    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private val utilitiesInstance = Utilities()

    object SharedData {
        val valueClose = MutableLiveData<Boolean>()
    }

    val listMediaPost = mutableListOf<PostMedia>()


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

        authUtilsInst = FirebaseAuth()

        mediaViewModel.getCountMediaPosts()

        val recicler = binding.contentMedia

        adaptador = AdapterHolderMedia(mutableListOf(), mediaViewModel, this)
        recicler.adapter = adaptador
        recicler.layoutManager = GridLayoutManager(requireContext(), 2)

        binding.newPostMediaBtn.setOnClickListener {
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
                        findNavController().navigate(R.id.action_mediaFragment_to_navigation_login)
                    },
                    {}
                )
            } else {
                findNavController().navigate(R.id.action_mediaFragment_to_writeMediaPost)
            }

        }

        setObservers()

        SharedData.valueClose.observe(viewLifecycleOwner) {
            if (it) closeService()
        }

        // Listener at swipe to refresh
        binding.swiperefreshMedia.setOnRefreshListener {
            refreshMediaPosts()
        }

        binding.generalBtnPlayLink.setOnClickListener {
            val d = binding.generalInputLink
            if (!d.text.isNullOrEmpty()) {
                if (!utilitiesInstance.isValidLink(d.toString())) {
                    val f = utilitiesInstance.isServiceRunning(
                        requireContext(),
                        AudioService::class.java,
                    )
                    if (f) requireContext().stopService(Intent(context, AudioService::class.java))


                    starPlayer(
                        d.text.toString(),
                        "Link de audio stream",
                        binding.generalBtnPlayLink
                    )
                } else {
                    d.error = "Link no valido"
                    Toast.makeText(requireContext(), "Ingresa un link valido", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                d.error = "Ingresa un link stream"
                Toast.makeText(requireContext(), "Ingresa un link stream", Toast.LENGTH_SHORT)
                    .show()

            }
        }
    }

    private fun setObservers() {

        mediaViewModel.postsMediasList.observe(viewLifecycleOwner) { items ->
            val semaforo =
                listMediaPost.map { it.idPostMedia }.toMutableSet() // Usa un Set para eficiencia
            val nuevosItems =
                items.filter { it.idPostMedia !in semaforo } // Filtra solo nuevos elementos

            val actualizaciones = items.filter { item ->
                val existente = listMediaPost.find { it.idPostMedia == item.idPostMedia }
                existente != null && existente.likesPostMediaCount != item.likesPostMediaCount
            }

            if (actualizaciones.isNotEmpty()) {
                actualizaciones.forEach { item ->
                    val index = listMediaPost.indexOfFirst { it.idPostMedia == item.idPostMedia }
                    if (index != -1) {
                        // Solo actualiza el campo likesPostMediaCount
                        listMediaPost[index] =
                            listMediaPost[index].copy(likesPostMediaCount = item.likesPostMediaCount)
                    }
                }
            }


            if (nuevosItems.isNotEmpty()) {
                listMediaPost.addAll(nuevosItems) // Agrega nuevos elementos a la lista
                Log.i("MediaPostsLog", "Recibidos ${nuevosItems.size} nuevos posts.")
            } else {
                Log.i("MediaPostsLog", ">>>>>> Todos los posts ya están en la lista.")
            }


            adaptador.setData(listMediaPost, numPosts) // Actualiza el adaptador solo una vez
        }



        mediaViewModel.postsGetError.observe(viewLifecycleOwner) {
            if (it == "THERE_NO_POSTS") {
                binding.noMediaText.visibility = View.VISIBLE
                binding.contentMedia.visibility = View.GONE
            }
        }

        mediaViewModel.succesLike.observe(viewLifecycleOwner) {
            mediaViewModel.getCountMediaPosts()
            println("Like dato, recibir")
        }

        mediaViewModel.countMediaPosts.observe(viewLifecycleOwner) {
            numPosts = it
            // call to function get posts
            mediaViewModel.getMediaPosts(null)
        }

        mediaViewModel.resultDelPostMedia.observe(viewLifecycleOwner) {
            listMediaPost.clear()
            mediaViewModel.getCountMediaPosts()
            println("mediapost eliminada")
        }
    }

    private fun refreshMediaPosts() {
        //clean last doc in view model
        mediaViewModel.currentLastMediaDoc.postValue(null)

        adaptador.deleteData()

        mediaViewModel.currentLastMediaDoc.observe(viewLifecycleOwner) {
            mediaViewModel.getCountMediaPosts()
        }

        //hide swipe refresh at 1 second
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            binding.swiperefreshMedia.isRefreshing = false
        }
    }

    fun starPlayer(link: String, audio: String, btnPlay: ImageButton) {
        val context = binding.root.context
        //set playlist radios on the player
        val mediaItem = generateMediaObj(link, audio)

        // Start session token on Service
        val sessionToken =
            SessionToken(context, ComponentName(context, AudioService::class.java))

        controllerFuture =
            MediaController.Builder(context, sessionToken).buildAsync()


        controllerFuture.addListener({

            val mediaController = controllerFuture.get()

            mediaController.setMediaItem(mediaItem)

            mediaController.prepare()
            mediaController.play()

//            btnPlay.setOnClickListener {
//                //hide errorAlert
//                binding.errorAlert.visibility = View.GONE

            if (mediaController.isPlaying) {
                mediaController.pause()
            } else {
                mediaController.prepare()
                mediaController.play()

//                    loadProgress.visibility = View.VISIBLE
//                    btnPlay.visibility = View.GONE

//                    if (mediaController.isPlaying) {
//                        loadProgress.visibility = View.GONE
//                        btnPlay.visibility = View.VISIBLE
//                    }
            }
//            }

//            lyReproductor.visibility = View.VISIBLE

            mediaController.addListener(object : Player.Listener {


                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (
                        mediaController.isPlaying
                        || mediaController.playWhenReady
                        || mediaController.isLoading
                    ) {
//                        btnPower.visibility = View.VISIBLE
                    }


                    if (isPlaying) {
                        // Active playback.
                        btnPlay.setImageResource(R.drawable.ic_pause_music)

                        //hide errorAlert
//                        binding.errorAlert.visibility = View.GONE
//
//                        if (!mediaController.isLoading) loadProgress.visibility = View.GONE
//
//                        val mediaData = mediaController.currentMediaItem
//                        textoDireccional(buildString {
//                            append(getString(R.string.estas_escuchando))
//                            append(mediaData?.mediaMetadata?.title)
//                        })

                        println("Es live: ${mediaController.isCurrentMediaItemLive}")

//                        lyReproductor.visibility = View.VISIBLE
                    } else {
                        btnPlay.setImageResource(R.drawable.ic_play_music)

                        mediaController.stop() //stop each time for at reanudar research the station
                        mediaController.prepare()

//                        if (mediaController.playWhenReady) {
//                            loadProgress.visibility = View.VISIBLE
//                            btnPlay.visibility = View.GONE
//                            textoDireccional(getString(R.string.buscando))
//                        }
                    }
                }


                override fun onPlaybackStateChanged(state: Int) {
                    // Actualiza la visibilidad del estado de carga cuando cambia el estado de reproducción del reproductor
                    if (state == Player.STATE_READY && mediaController.isPlaying) {
                        // El reproductor está listo para reproducir pero aún no ha comenzado
                        btnPlay.visibility = View.VISIBLE
//                        binding.cargando.visibility = View.GONE
                    }

                    if (state == Player.STATE_ENDED) {
                        // Verifica si es contenido en vivo y maneja la reproducción
                        val currentItem = mediaController.currentMediaItem
                        if (currentItem?.liveConfiguration != null) {
                            mediaController.seekToDefaultPosition()
                            mediaController.playWhenReady = true
                        }
                        println("Estate ended evitar next")
                    }

                }

                //on player error signal
                override fun onPlayerError(error: PlaybackException) {
                    // Imprime el error para depuración
                    println("AudioService :: Error playing radio stream: ${error.message}")

                    when (error.errorCode) {
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
//                            val errorMessage = getString(R.string.error_time_over)
//                            radioError(errorMessage)
                            mediaController.pause()
//
//                            val text = getString(R.string.baseTextDireccional)
//                            textoDireccional(text)
                        }

                        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> {
                            val media = mediaController.currentMediaItem
                            val nameRadio = media?.mediaMetadata?.title
//                            val errorMessage = getString(R.string.error_source_station, nameRadio)
//                            radioError(errorMessage)
                            mediaController.pause()

//                            val text = getString(R.string.baseTextDireccional)
//                            textoDireccional(text)
                        }

                        else -> {
//                            val errorMessage = getString(R.string.error_base)
//                            radioError(errorMessage)
                            mediaController.playWhenReady = true
                            mediaController.play()
                        }
                    }
                }

            })

            // MediaController is available here with controllerFuture.get()
        }, MoreExecutors.directExecutor())
    }

    @OptIn(UnstableApi::class)
    private fun generateMediaObj(uriLink: String, titleMedia: String): MediaItem {
        val station = MediaItem.Builder()
            .setUri(uriLink)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(titleMedia)
                    .setDurationMs(C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE.toLong())
                    .build()
            )
            .setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(C.TIME_UNSET)  // Usa el valor predeterminado de ExoPlayer
                    .setMinPlaybackSpeed(0.95f)      // Configura la velocidad mínima de reproducción
                    .setMaxPlaybackSpeed(1.05f)      // Configura la velocidad máxima de reproducción
                    .build()
            )
            .build()
        return station
    }

    private fun closeService() {

        controllerFuture.get().release()
//        stopService(Intent(this, AudioService::class.java))
    }
}