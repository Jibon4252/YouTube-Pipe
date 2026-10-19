package com.example.youtubepipe

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val container = findViewById<LinearLayout>(R.id.splashContainer)
        container.alpha = 0f
        container.animate().alpha(1f).scaleX(1.1f).scaleY(1.1f).setDuration(1000).start()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2000)
    }
}
