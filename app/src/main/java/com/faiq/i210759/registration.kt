package com.faiq.i210759

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class registration : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        val Regbtn: Button = findViewById(R.id.btnRegister)

        Regbtn.setOnClickListener{
            val intent= Intent(this, login::class.java)
            startActivity(intent)
        }
    }

}