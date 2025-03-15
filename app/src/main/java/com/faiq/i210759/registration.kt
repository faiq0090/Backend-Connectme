package com.faiq.i210759

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class registration : AppCompatActivity() {
    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        // Get references to UI elements
        val etName = findViewById<EditText>(R.id.etName)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPhoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val Regbtn: Button = findViewById(R.id.btnRegister)

        Regbtn.setOnClickListener {
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

            // Generate User ID properly
            getLastUserId { newUserId ->
                if (newUserId != null) {
                    // Register the user with Firebase Authentication
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Store user details under the generated User ID
                                val userMap = hashMapOf(
                                    "userId" to newUserId,
                                    "name" to name,
                                    "username" to username,
                                    "phone" to phone,
                                    "email" to email
                                )

                                database.child(newUserId).setValue(userMap)
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
                            } else {
                                Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Failed to generate User ID", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Function to get the last User ID and generate a new one
    private fun getLastUserId(callback: (String?) -> Unit) {
        database.orderByKey().limitToLast(1).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val lastUserId = snapshot.children.first().key // Get last key (e.g., "User-05")
                val lastNumber = lastUserId?.substringAfter("User-")?.toIntOrNull() ?: 0
                val newUserId = "User-${String.format("%02d", lastNumber + 1)}"
                callback(newUserId)
            } else {
                callback("User-01") // Start from User-01 if no users exist
            }
        }.addOnFailureListener {
            callback("User-01") // Handle failure case properly
        }
    }
}
