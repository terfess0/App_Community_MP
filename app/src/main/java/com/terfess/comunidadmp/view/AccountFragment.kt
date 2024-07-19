package com.terfess.comunidadmp.view

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.integrity.internal.c
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.classes.ImageUtils
import com.terfess.comunidadmp.classes.ImageUtils.compressBitmap
import com.terfess.comunidadmp.classes.Utilities
import com.terfess.comunidadmp.databinding.ActivityMainBinding
import com.terfess.comunidadmp.databinding.FragmentAccountBinding
import com.terfess.comunidadmp.viewmodel.AccountViewModel
import java.io.File

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val db = FirebaseFirestore.getInstance()
    private val user = Firebase.auth.currentUser
    private val nombreArchivoFotoPerfil = "profile_perfil.jpg"

    private val binding get() = _binding!!

    private val viewModelUser: AccountViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentAccountBinding.inflate(inflater, container, false)

        val root: View = binding.root

        val localFile = File(root.context?.filesDir, nombreArchivoFotoPerfil)
        println("Archivo de imagen presente: $localFile")

        viewModelUser.getUserAccount()


        //-------------------OBSERVERS

        viewModelUser.dataUser.observe(viewLifecycleOwner, Observer { user ->
            // Access to data
            binding.UserCorreo.text = user.userEmail

            if (user.userJoinDate.toString().isNotBlank()) {
                binding.FechaCreacion.text = user.userJoinDate.toString()
            }

            if (user.userNumPosts.isNotBlank() && user.userNumPosts.toInt() != 0) {
                binding.NPublicaciones.text = user.userNumPosts
            }


            if (user.userName.isNotBlank()) {
                binding.NameUser.text = user.userName
                binding.edtNameUser.setHint(user.userName)
            }

            //save name shared preferences
            Utilities().saveSharedpref(requireContext(), "userName", user.userName)

            val imgProfile = user.userPhoto
            if (imgProfile.isNotBlank()) {
                Glide.with(root.context)
                    .load(imgProfile)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Almacenamiento en caché tanto en memoria como en disco
                    .skipMemoryCache(false) // No omitir la caché de memoria
                    .into(root.findViewById(R.id.imageViewFotoPerfil))
            }


            val btnSaveName = binding.btnSaveUsername

            btnSaveName.setOnClickListener {
                val newName = binding.edtNameUser.text.toString()

                if (newName.isBlank()) {
                    binding.errorUserNewName.apply {
                        visibility = View.VISIBLE
                        error = "Campo Requerido"
                        text = getString(R.string.must_fill_all_fields)
                    }
                } else {
                    viewModelUser.updateNameUser(user.userEmail, newName)

                    val viewUserName = binding.tvUsername
                    val viewEdtUserName = binding.edtUsername

                    binding.errorUserNewName.visibility = View.GONE
                    viewUserName.visibility = View.VISIBLE
                    viewEdtUserName.visibility = View.GONE
                }
            }

            //save userRole un shared preferences local
            Utilities().saveSharedpref(requireContext(), "userRole", user.userRole)
        })


        viewModelUser.resultChangeName.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                "SUCCESS" -> {
//                    Snackbar.make(binding.root, "Nombre Actualizado", Snackbar.LENGTH_SHORT).show()
                    Log.i("resultado", "Todo ok")
                }

                "ALREADY_IN_USE" -> {
                    binding.errorUserNewName.visibility = View.VISIBLE
                    binding.errorUserNewName.text = getString(R.string.newName_already_in_use)
                    Log.i("resultado", "Ya en uso newName")
                }

                else -> {
//                    Snackbar.make(
//                        binding.root,
//                        "No se pudo actualizar el nombre",
//                        Snackbar.LENGTH_SHORT
//                    ).show()
                }
            }
            viewModelUser.getUserAccount()
        })


        //--------------------------------------------

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.action_navigation_account_to_navigation_home)
                    println("a home from account")
                }
            })


        var refImagen = ""
        val refStorage = FirebaseStorage.getInstance().reference
        //devolucion de llamada cuando el usuario elige imagen local se subira a storage
        val getContent =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri == null) {
                    // La ventana fue cerrada sin seleccionar una imagen
                    Snackbar.make(root, "No se seleccionó ninguna imagen", Snackbar.LENGTH_SHORT)
                        .show()
                    // Habilitar la navegación y ocultar el indicador de carga
                    enableNavigation(true)
                    showLoadingIndicator(false)
                } else {
                    enableNavigation(false)
                    showLoadingIndicator(true)
                    // Handle the returned Uri
                    val imageViewIcon = binding.imageViewFotoPerfil

                    uri.let { img ->
                        imageViewIcon.setImageURI(img) // Muestra la imagen seleccionada en tu ImageView

                        // Cargar imagen desde URI y convertirla en Bitmap
                        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source =
                                ImageDecoder.createSource(requireContext().contentResolver, uri)
                            ImageDecoder.decodeBitmap(source)
                        } else {
                            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                        }

                        // Comprimir el bitmap
                        val compressedBytes = compressBitmap(bitmap, 150000)

                        // Nombre único para el archivo en Firebase Storage
                        val fileName = "${System.currentTimeMillis()}_${File(uri.path!!).name}"
                        val riversRef = refStorage.child("images/profileUser/$fileName")

                        // Subir el archivo comprimido a Firebase Storage
                        val uploadTask = riversRef.putBytes(compressedBytes)

                        uploadTask.addOnFailureListener { exception ->
                            // Handle unsuccessful uploads
                            println("Upload failed: $exception")
                            // Habilitar la navegación y ocultar el indicador de carga
                            enableNavigation(true)
                            showLoadingIndicator(false)


                        }.addOnSuccessListener { _ ->
                            // Obtener la URL de descarga de la imagen
                            riversRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                val imageUrl = downloadUri.toString()
                                refImagen = imageUrl

                                // Actualizar la referencia de la imagen en Firestore
                                Utilities().actualizarFotoUsuario(
                                    root,
                                    user?.email.toString(),
                                    refImagen
                                )


                                // Habilitar la navegación y ocultar el indicador de carga
                                enableNavigation(true)
                                showLoadingIndicator(false)

                            }
                        }
                    }
                    //reload
                    viewModelUser.getUserAccount()
                }
            }


        binding.buttonCerrarSesion.setOnClickListener {
            // Usuario presiona botón cerrar sesión
            Utilities().saveSharedpref(requireContext(), "LoginState", "false")
            Firebase.auth.signOut()
            Snackbar.make(root, "Saliste de tu cuenta.", Snackbar.LENGTH_SHORT).show()

            // Navegar al fragmento de inicio de sesión
            findNavController().navigate(R.id.action_navigation_account_to_navigation_home)
        }

        binding.btnProfileImage.setOnClickListener {
            //el usuario esta logeado?
            if (user != null) {
                //seleccionar imagen local
                getContent.launch("image/*")
            } else {

                // Navegar al fragmento de inicio de sesión
                findNavController().navigate(R.id.action_navigation_account_to_navigation_home)
            }
        }

        val viewUserName = binding.tvUsername
        val viewEdtUserName = binding.edtUsername
        val btnEditName = binding.btnEditUsername
        val btnSaveName = binding.btnSaveUsername

        btnEditName.setOnClickListener {
            viewUserName.visibility = View.GONE
            viewEdtUserName.visibility = View.VISIBLE

            binding.edtNameUser.requestFocus()

        }


        return root
    }

    private fun showLoadingIndicator(show: Boolean) {
        binding.progressBar2.visibility = if (show) View.VISIBLE else View.GONE
    }


    private fun enableNavigation(enabled: Boolean) {
        val bin = ActivityMainBinding.bind(requireActivity().findViewById(R.id.container))
        if (enabled) {
            // Habilitar la navegación
            bin.navView.visibility = View.VISIBLE
            findNavController().let { controller ->
                val navGraph = controller.navInflater.inflate(R.navigation.mobile_navigation)
                controller.graph = navGraph
            }
        } else {
            // Bloquear la navegación
            bin.navView.visibility = View.GONE
        }
    }


    fun getImageProfile(root: View) {
        db.collection("users")
            .whereEqualTo("userEmail", user?.email.toString())
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val imagenPerfil =
                        querySnapshot.documents[0].getString("userPhoto")

                    Glide.with(root.context)
                        .load(imagenPerfil)
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Almacenamiento en caché tanto en memoria como en disco
                        .skipMemoryCache(false) // No omitir la caché de memoria
                        .into(root.findViewById(R.id.imageViewFotoPerfil))


//                    val localFile2 = File(root.context?.filesDir, nombreArchivoFotoPerfil)
//                    // Borrar la imagen anterior localmente (si existe)
//                    if (localFile2.exists()) {
//                        localFile2.delete()
//                        println("Imagen anterior borrada exitosamente.")
//                    }

//                    downloadImageProfile(root, imagenPerfil.toString())
                }
            }
    }

//    private fun downloadImageProfile(root: View, urlImage: String) {
//        println("Descargando archivo $nombreArchivoFotoPerfil")
//
//        try {
//            // La imagen no está descargada, descárgala desde Firebase Storage
//            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(urlImage)
//
//            val localFile = File(root.context?.filesDir, nombreArchivoFotoPerfil)
//
//            storageRef.getFile(localFile)
//                .addOnSuccessListener {
//                    // La imagen ya está descargada localmente, cárgala desde la memoria interna
//                    Glide.with(root.context)
//                        .load(localFile)
//                        .diskCacheStrategy(DiskCacheStrategy.NONE)
//                        .into(root.findViewById(R.id.imageViewFotoPerfil))
//
//                    println("Archivo profile_photo.jpg fue guardado en almacenamiento local.")
//                }
//                .addOnFailureListener { exception ->
//                    println("Error al guardar archivo profile_photo.jpg : $exception")
//                }
//        } catch (e: IllegalArgumentException) {
//            println("Error: La URL del Storage de Firebase no es válida: $urlImage")
//            e.printStackTrace()
//        }
//    }
//
//
//
//    private fun checkImageProfileLocal(root: View) {
//        // Comprueba si la imagen ya está descargada localmente
//        val localFile = File(root.context?.filesDir, nombreArchivoFotoPerfil)
//        if (localFile.exists()) {
//            // La imagen ya está descargada localmente, cárgala desde la memoria interna
//            Glide.with(root.context)
//                .load(localFile)
//                .diskCacheStrategy(DiskCacheStrategy.NONE)
//                .into(root.findViewById(R.id.imageViewFotoPerfil))
//            println("Fue recuperada la foto de perfil: $nombreArchivoFotoPerfil.")
//        } else {
//            println("No hay imagen guardada, se procede a buscar en firebase.")
//            getImageProfile(root)
//
//        }
//    }

}

//// Realiza una consulta para verificar si el nombre de usuario ya está en uso
//        usuariosRef.whereEqualTo("userName", nombre)
//            .get()
//            .addOnSuccessListener { querySnapshot ->
//                if (!querySnapshot.isEmpty) {
//                    // El nombre de usuario ya está en uso
//                    Toast.makeText(
//                        contexto,
//                        "El usuario '$nombre' ya está en uso.",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                } else {
//                    // El nombre de usuario no está en uso, procede a crear la cuenta
//
//                }
//            }
//            .addOnFailureListener { e ->
//                // Error al verificar el nombre de usuario
//                Toast.makeText(
//                    contexto,
//                    "No se pudo verificar el nombre de usuario: $e",
//                    Toast.LENGTH_SHORT
//                ).show()
//                println("Falla verificar nombre de usuario caso : $e")
//            }

//        val textView: TextView = binding.textDashboard
//        dashboardViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }

