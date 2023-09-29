package com.akash.leakmasterv2

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.blogspot.atifsoftwares.animatoolib.Animatoo

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Delay the transition to the second activity by 5 seconds (5000 milliseconds)
        Handler().postDelayed({
            startActivity(Intent(this, MainActivity::class.java)) // Move to next Activity
            Animatoo.animateSlideUp(this);
            finish() // Optionally, you can finish the current activity
        }, 5000)
    }
}