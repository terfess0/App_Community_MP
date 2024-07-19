package com.terfess.comunidadmp.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.ads.MobileAds
import com.terfess.comunidadmp.R

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        splashScreen.setKeepOnScreenCondition { true }
        MobileAds.initialize(this) {}

        val transicion = Intent(this, MainActivity::class.java)
        startActivity(transicion)
        finish()
    }
}