package com.example.myapplication.ui.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.AuthManager
import com.example.myapplication.data.User
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bumptech.glide.Glide

class ChangeAvatarDialogFragment : DialogFragment() {

    private lateinit var imgAvatarPreview: ImageView
    private lateinit var btnSelectImage: Button
    private lateinit var btnUpload: Button
    private lateinit var btnCancel: Button

    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                selectedImageUri = uri
                imgAvatarPreview.setImageURI(uri)
                btnUpload.isEnabled = true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_change_avatar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgAvatarPreview = view.findViewById(R.id.imgAvatarPreview)
        btnSelectImage = view.findViewById(R.id.btnSelectImage)
        btnUpload = view.findViewById(R.id.btnUpload)
        btnCancel = view.findViewById(R.id.btnCancel)

        // Load current avatar
        loadCurrentAvatar()

        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            imagePickerLauncher.launch(intent)
        }

        btnUpload.setOnClickListener {
            uploadAvatar()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun loadCurrentAvatar() {
        val userId = AuthManager.userId ?: return

        lifecycleScope.launch {
            val user = User.fetchById(userId)
            if (user != null && user.avatarUrl.isNotBlank()) {
                Glide.with(this@ChangeAvatarDialogFragment)
                    .load(user.avatarUrl)
                    .circleCrop()
                    .into(imgAvatarPreview)
            }
        }
    }

    private fun uploadAvatar() {
        val uri = selectedImageUri ?: return
        val userId = AuthManager.userId ?: return

        lifecycleScope.launch {
            try {
                btnUpload.isEnabled = false
                btnSelectImage.isEnabled = false
                Toast.makeText(requireContext(), "Uploading avatar...", Toast.LENGTH_SHORT).show()

                // Upload to Firebase Storage
                val storageRef = FirebaseStorage.getInstance().reference
                val avatarRef = storageRef.child("avatars/$userId.jpg")

                avatarRef.putFile(uri).await()
                val downloadUrl = avatarRef.downloadUrl.await()

                // Update user's avatar URL in database
                User.updateAvatarUrl(userId, downloadUrl.toString())

                Toast.makeText(requireContext(), "Avatar updated successfully!", Toast.LENGTH_SHORT).show()

                // Notify parent fragment to refresh (PlayerStatsDialog)
                (parentFragment as? OnAvatarUpdatedListener)?.onAvatarUpdated(downloadUrl.toString())

                // Notify activity to refresh (MainActivity bottom panel)
                (activity as? OnAvatarUpdatedListener)?.onAvatarUpdated(downloadUrl.toString())

                dismiss()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to upload avatar: ${e.message}", Toast.LENGTH_SHORT).show()
                btnUpload.isEnabled = true
                btnSelectImage.isEnabled = true
            }
        }
    }

    interface OnAvatarUpdatedListener {
        fun onAvatarUpdated(avatarUrl: String)
    }

    companion object {
        const val TAG = "ChangeAvatarDialog"
    }
}
