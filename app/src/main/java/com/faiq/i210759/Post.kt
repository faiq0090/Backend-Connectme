package com.faiq.i210759

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Post : Fragment() {
    private lateinit var cameraPreview: PreviewView
    private lateinit var imagePreview: ImageView
    private lateinit var captionInput: EditText
    private lateinit var shareButton: Button
    private lateinit var captureButton: CardView
    private lateinit var switchCameraButton: View
    private lateinit var galleryButton: ImageButton
    private lateinit var postText: TextView
    private lateinit var storyText: TextView
    private lateinit var loadingIndicator: ProgressBar

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var currentImageUri: Uri? = null
    private var isPostMode = true
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            currentImageUri = it
            showImagePreview()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        cameraPreview = view.findViewById(R.id.cameraPreview)
        imagePreview = view.findViewById(R.id.imagePreview)
        captionInput = view.findViewById(R.id.captionInput)
        shareButton = view.findViewById(R.id.shareButton)
        captureButton = view.findViewById(R.id.captureButton)
        switchCameraButton = view.findViewById(R.id.switchCameraButton)
        galleryButton = view.findViewById(R.id.galleryButton)
        postText = view.findViewById(R.id.postText)
        storyText = view.findViewById(R.id.storyText)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Set up camera
        setupCamera()

        // Set up listeners
        captureButton.setOnClickListener {
            takePhoto()
        }

        switchCameraButton.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            setupCamera()
        }

        galleryButton.setOnClickListener {
            getContent.launch("image/*")
        }

        // Set up tab selection behavior
        postText.setOnClickListener {
            setSelectedTab(true)
        }

        storyText.setOnClickListener {
            setSelectedTab(false)
        }

        // Share button
        shareButton.setOnClickListener {
            if (currentImageUri != null) {
                uploadImageToFirebase()
            }
        }

        // Start with Post selected by default
        setSelectedTab(true)
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Set up preview use case
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(cameraPreview.surfaceProvider)

                // Set up image capture use case
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // Select camera based on lens facing
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )

            } catch (e: Exception) {
                Log.e(TAG, "Camera setup failed", e)
                Toast.makeText(context, "Failed to set up camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Create temporary file
        val photoFile = File(
            outputDirectory(),
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    currentImageUri = savedUri
                    showImagePreview()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    Toast.makeText(context, "Failed to take photo: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showImagePreview() {
        // Switch to preview mode
        cameraPreview.visibility = View.GONE
        imagePreview.visibility = View.VISIBLE
        imagePreview.setImageURI(currentImageUri)

        // Show share button
        shareButton.visibility = View.VISIBLE

        // Show caption field only in post mode
        if (isPostMode) {
            captionInput.visibility = View.VISIBLE
        } else {
            captionInput.visibility = View.GONE
        }

        // Hide camera controls
        captureButton.visibility = View.GONE
        switchCameraButton.visibility = View.GONE
    }

    private fun resetCameraView() {
        // Switch back to camera mode
        cameraPreview.visibility = View.VISIBLE
        imagePreview.visibility = View.GONE

        // Hide sharing controls
        captionInput.visibility = View.GONE
        shareButton.visibility = View.GONE

        // Show camera controls
        captureButton.visibility = View.VISIBLE
        switchCameraButton.visibility = View.VISIBLE

        // Clear current image
        currentImageUri = null
    }

    private fun uploadImageToFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(context, "You need to be logged in to post", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading indicator
        loadingIndicator.visibility = View.VISIBLE
        shareButton.isEnabled = false

        // Get user ID
        val userId = currentUser.uid

        // Generate unique ID for post/story
        val itemId = if (isPostMode) "Post-${UUID.randomUUID()}" else "Story-${UUID.randomUUID()}"

        // Reference to storage location
        val storageRef = FirebaseStorage.getInstance().reference
            .child("user_content")
            .child(userId)
            .child(if (isPostMode) "posts" else "stories")
            .child("$itemId.jpg")

        // Upload image
        storageRef.putFile(currentImageUri!!)
            .addOnSuccessListener { taskSnapshot ->
                // Get download URL
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Save metadata to database
                    saveToDatabase(userId, itemId, downloadUri.toString())
                }
            }
            .addOnFailureListener { e ->
                loadingIndicator.visibility = View.GONE
                shareButton.isEnabled = true
                Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Upload failed", e)
            }
    }

    private fun saveToDatabase(userId: String, itemId: String, imageUrl: String) {
        val database = FirebaseDatabase.getInstance().reference
        val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        if (isPostMode) {
            // Create post data
            val postData = hashMapOf(
                "postId" to itemId,
                "caption" to captionInput.text.toString(),
                "image" to imageUrl,
                "createdAt" to currentTime
            )

            // Save post data
            database.child("Posts").child(userId).child(itemId)
                .setValue(postData)
                .addOnSuccessListener {
                    handleUploadSuccess()
                }
                .addOnFailureListener { e ->
                    handleUploadFailure(e)
                }
        } else {
            // Create story data - stories expire after 24 hours
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.HOUR, 24)
            val expiresAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(calendar.time)

            val storyData = hashMapOf(
                "storyId" to itemId,
                "image" to imageUrl,
                "createdAt" to currentTime,
                "expiresAt" to expiresAt
            )

            // Save story data
            database.child("Stories").child(userId).child(itemId)
                .setValue(storyData)
                .addOnSuccessListener {
                    handleUploadSuccess()
                }
                .addOnFailureListener { e ->
                    handleUploadFailure(e)
                }
        }
    }

    private fun handleUploadSuccess() {
        loadingIndicator.visibility = View.GONE
        shareButton.isEnabled = true
        Toast.makeText(
            context,
            if (isPostMode) "Post shared successfully!" else "Story shared successfully!",
            Toast.LENGTH_SHORT
        ).show()

        // Reset UI
        resetCameraView()

        // Optionally navigate back to feed
        // For example: findNavController().navigate(R.id.action_post_to_feed)
    }

    private fun handleUploadFailure(e: Exception) {
        loadingIndicator.visibility = View.GONE
        shareButton.isEnabled = true
        Toast.makeText(
            context,
            "Failed to save to database: ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
        Log.e(TAG, "Database save failed", e)
    }

    private fun setSelectedTab(isPost: Boolean) {
        isPostMode = isPost

        context?.let { ctx ->
            if (isPost) {
                postText.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
                storyText.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
            } else {
                postText.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                storyText.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
            }
        }

        // If in preview mode, update UI based on selected tab
        if (currentImageUri != null) {
            captionInput.visibility = if (isPost) View.VISIBLE else View.GONE
        }
    }

    private fun outputDirectory(): File {
        val mediaDir = context?.externalMediaDirs?.firstOrNull()?.let {
            File(it, "camera_photos").apply { mkdirs() }
        }
        return mediaDir ?: requireContext().filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "PostFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        @JvmStatic
        fun newInstance() = Post()
    }
}