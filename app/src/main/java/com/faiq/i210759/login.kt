
package com.faiq.i210759

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class login : AppCompatActivity() {

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge if needed
        setContentView(R.layout.activity_login)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // References to UI elements
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {
            // Retrieve inputs (treating etUsername as email)
            val email = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validate inputs
            if (email.isEmpty()) {
                etUsername.error = "Email required"
                etUsername.requestFocus()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etUsername.error = "Enter a valid email"
                etUsername.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Password required"
                etPassword.requestFocus()
                return@setOnClickListener
            }

            // Attempt Firebase Authentication login
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Login successful: redirect to the Home screen
                        // Assumed that MainActivity hosts your Home Fragment.
                        val intent = Intent(this, bottom_navigation::class.java)
                        // Clear previous activities from the stack.
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Login failed: show an error message.
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Redirect to Registration screen when "Register" is clicked
        tvRegister.setOnClickListener {
            val intent = Intent(this, registration::class.java)
            startActivity(intent)
        }

        // TODO: Handle "Forgot Password" if needed
        tvForgotPassword.setOnClickListener {
            // You can implement a forgot password flow here.
            Toast.makeText(this, "Forgot password functionality is not implemented yet.", Toast.LENGTH_SHORT).show()
        }
    }
}
