package com.terfess.comunidadmp.view

import android.os.Bundle
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.MobileAds
import com.terfess.comunidadmp.R
import com.terfess.comunidadmp.classes.Utilities
import com.terfess.comunidadmp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        MobileAds.initialize(this) {}

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Configurar AppBarConfiguration con los destinos de nivel superior
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_account, R.id.favoritesFragment
            )
        )

        // Configurar BottomNavigationView con NavController
        navView.setupWithNavController(navController)


        // Escuchar cambios en el destino de navegación
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_home, R.id.favoritesFragment, R.id.navigation_account -> {
                    navView.visibility = View.VISIBLE
                }
                R.id.navigation_login, R.id.navigation_registro -> {
                    navView.visibility = View.GONE
                }
                else -> {
                    navView.visibility = View.GONE
                }
            }
        }
    }
}