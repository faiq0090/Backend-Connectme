package com.faiq.i210759

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class registration : AppCompatActivity() {
    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Get references to the EditText fields and Register button
        val etName = findViewById<EditText>(R.id.etName)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPhoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val Regbtn: Button = findViewById(R.id.btnRegister)

        Regbtn.setOnClickListener {
            // Get input values
            val name = etName.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val phone = etPhoneNumber.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validate inputs
            if (name.isEmpty()) {
                etName.error = "Name required"
                etName.requestFocus()
                return@setOnClickListener
            }
            if (username.isEmpty()) {
                etUsername.error = "Username required"
                etUsername.requestFocus()
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                etPhoneNumber.error = "Phone number required"
                etPhoneNumber.requestFocus()
                return@setOnClickListener
            }
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Valid email required"
                etEmail.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty() || password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                etPassword.requestFocus()
                return@setOnClickListener
            }

            // Create the user with Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // User registration successful; now store additional details in Firebase Realtime Database
                        val userId = auth.currentUser?.uid
                        // Create a map for additional user details
                        val userMap = hashMapOf<String, Any>(
                            "name" to name,
                            "username" to username,
                            "phone" to phone,
                            "email" to email
                        )
                        if (userId != null) {
                            FirebaseDatabase.getInstance().getReference("Users")
                                .child(userId)
                                .setValue(userMap)
                                .addOnCompleteListener { dbTask ->
                                    if (dbTask.isSuccessful) {
                                        Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                                        // Redirect to the Profile Setup screen
                                        val intent = Intent(this, edit_profile::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Failed to save user details: ${dbTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        }
                    } else {
                        Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
