package com.example.finbot

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class Launch : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        // Delay for 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, GetStarted::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}