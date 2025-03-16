package com.faiq.i210759

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class edit_profile : AppCompatActivity() {

    // Firebase references
    private lateinit var auth: FirebaseAuth
    private lateinit var profileRef: DatabaseReference
    private lateinit var sequenceRef: DatabaseReference

    // Launcher for image picker (from gallery)
    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>

    // Flag to track if we're processing an image
    private var isProcessingImage = false
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(com.faiq.i210759.R.layout.activity_edit_profile)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val userId = currentUser.uid

        // Initialize Firebase Database references
        val database = FirebaseDatabase.getInstance()
        profileRef = database.getReference("Profiles").child(userId)
        sequenceRef = database.getReference("ProfileSequence/lastProfileId")

        // Get references to UI elements
        val btnDone = findViewById<TextView>(com.faiq.i210759.R.id.btn_done)
        val nameInput = findViewById<EditText>(com.faiq.i210759.R.id.name_input)
        val usernameInput = findViewById<EditText>(com.faiq.i210759.R.id.username_input)
        val contactInput = findViewById<EditText>(com.faiq.i210759.R.id.contact_input)
        val bioInput = findViewById<EditText>(com.faiq.i210759.R.id.bio_input)
        val profileImageView = findViewById<CircleImageView>(com.faiq.i210759.R.id.profile_image)
        val cameraIcon = findViewById<ImageView>(com.faiq.i210759.R.id.camera_icon)

        // Setup image picker to pick an image from gallery
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                // Display the selected image immediately
                profileImageView.setImageURI(uri)
            }
        }

        // When camera icon is clicked, launch image picker
        cameraIcon.setOnClickListener {
            if (!isProcessingImage) {
                imagePickerLauncher.launch("image/*")
            } else {
                Toast.makeText(this, "Please wait, image processing is in progress", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch existing profile data (if any) and populate fields
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    nameInput.setText(snapshot.child("name").getValue(String::class.java) ?: "")
                    usernameInput.setText(snapshot.child("username").getValue(String::class.java) ?: "")
                    contactInput.setText(snapshot.child("contact").getValue(String::class.java) ?: "")
                    bioInput.setText(snapshot.child("bio").getValue(String::class.java) ?: "")
                    val encodedImage = snapshot.child("profilePicture").getValue(String::class.java) ?: ""
                    if (encodedImage.isNotEmpty()) {
                        loadImageFromBase64(encodedImage, profileImageView)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@edit_profile, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // When "Done" is clicked, save or update the profile (no redirection)
        btnDone.setOnClickListener {
            if (isProcessingImage) {
                Toast.makeText(this, "Please wait until image processing completes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nameText = nameInput.text.toString().trim()
            val usernameText = usernameInput.text.toString().trim()
            val contactText = contactInput.text.toString().trim()
            val bioText = bioInput.text.toString().trim()

            if (nameText.isEmpty() || usernameText.isEmpty()) {
                Toast.makeText(this, "Name and username are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val currentTimestamp = dateFormat.format(Date())

            if (selectedImageUri != null) {
                // Convert the selected image to Base64 string
                encodeImageToBase64(selectedImageUri!!) { encodedString ->
                    saveProfileData(nameText, usernameText, contactText, bioText, encodedString, currentTimestamp)
                }
            } else {
                saveProfileData(nameText, usernameText, contactText, bioText, null, currentTimestamp)
            }
        }
    }

    // Helper function to encode an image (from a Uri) to a Base64 string
    private fun encodeImageToBase64(uri: Uri, onComplete: (String?) -> Unit) {
        isProcessingImage = true
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)
            isProcessingImage = false
            onComplete(encodedImage)
        } catch (e: Exception) {
            isProcessingImage = false
            e.printStackTrace()
            Toast.makeText(this, "Error encoding image: ${e.message}", Toast.LENGTH_SHORT).show()
            onComplete(null)
        }
    }

    // Save or update profile data in Realtime Database
    private fun saveProfileData(
        name: String,
        username: String,
        contact: String,
        bio: String,
        encodedImage: String?,
        timestamp: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Update existing profile
                    val updates = HashMap<String, Any>()
                    updates["name"] = name
                    updates["username"] = username
                    updates["contact"] = contact
                    updates["bio"] = bio
                    updates["updatedAt"] = timestamp
                    if (encodedImage != null) {
                        updates["profilePicture"] = encodedImage
                    }
                    profileRef.updateChildren(updates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@edit_profile, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@edit_profile, "Update failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Create new profile with a sequential profileId
                    sequenceRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(mutableData: MutableData): Transaction.Result {
                            val currentValue = mutableData.getValue(Int::class.java) ?: 0
                            mutableData.value = currentValue + 1
                            return Transaction.success(mutableData)
                        }

                        override fun onComplete(error: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                            if (error != null) {
                                Toast.makeText(this@edit_profile, "Error generating profile ID: ${error.message}", Toast.LENGTH_SHORT).show()
                                return
                            }
                            val newIdNumber = dataSnapshot?.getValue(Int::class.java) ?: 1
                            val newProfileId = "P-" + String.format("%02d", newIdNumber)

                            val profileData = HashMap<String, Any>()
                            profileData["profileId"] = newProfileId
                            profileData["userId"] = userId
                            profileData["name"] = name
                            profileData["username"] = username
                            profileData["contact"] = contact
                            profileData["bio"] = bio
                            profileData["profilePicture"] = encodedImage ?: ""
                            profileData["createdAt"] = timestamp
                            profileData["updatedAt"] = timestamp

                            profileRef.setValue(profileData).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this@edit_profile, "Profile created successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@edit_profile, "Profile creation failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@edit_profile, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Helper function to load an image from a Base64-encoded string and display it in a CircleImageView
    private fun loadImageFromBase64(encodedString: String, imageView: CircleImageView) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val imageBytes = Base64.decode(encodedString, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@edit_profile, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
