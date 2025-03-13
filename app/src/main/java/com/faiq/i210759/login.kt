package com.faiq.i210759

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class login : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val loginbtn: Button = findViewById(R.id.btnLogin)

        loginbtn.setOnClickListener{
            val intent= Intent(this, bottom_navigation::class.java)
            startActivity(intent)
        }

    }

}
