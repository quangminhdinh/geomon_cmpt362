package com.example.myapplication.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.AuthManager
import com.example.myapplication.data.FirebaseManager
import com.example.myapplication.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangeNameDialogFragment : DialogFragment() {

    private lateinit var etDisplayName: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_change_name, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etDisplayName = view.findViewById(R.id.etDisplayName)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)

        loadCurrentDisplayName()

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnSave.setOnClickListener {
            saveDisplayName()
        }
    }

    private fun loadCurrentDisplayName() {
        val userId = AuthManager.userId ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val user = User.fetchById(userId)
            withContext(Dispatchers.Main) {
                etDisplayName.setText(user?.displayName ?: "Player")
            }
        }
    }

    private fun saveDisplayName() {
        val userId = AuthManager.userId
        if (userId == null) {
            Toast.makeText(context, "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }

        val newName = etDisplayName.text.toString().trim()
        if (newName.isEmpty()) {
            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseManager.usersRef.child(userId).child("displayName").setValue(newName)
            .addOnSuccessListener {
                Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()

                (activity as? OnNameUpdatedListener)?.onNameUpdated(newName)

                dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update name", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    interface OnNameUpdatedListener {
        fun onNameUpdated(newName: String)
    }

    companion object {
        const val TAG = "ChangeNameDialogFragment"
    }
}
