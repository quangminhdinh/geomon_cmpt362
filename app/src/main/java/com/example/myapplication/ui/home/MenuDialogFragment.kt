package com.example.myapplication.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.example.myapplication.ui.chat.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.AuthManager
import com.example.myapplication.data.User
import com.example.myapplication.ui.pokedex.PokedexActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnChangeDisplayName: Button = view.findViewById(R.id.btnChangeDisplayName)
        val btnPokedex: Button = view.findViewById(R.id.btnPokedex)
        val btnMon: Button = view.findViewById(R.id.btnMon)
        val btnItems: Button = view.findViewById(R.id.btnItems)
        val btnClose: Button = view.findViewById(R.id.btnClose)

        btnChangeDisplayName.setOnClickListener {
            dismiss()
            ChangeNameDialogFragment().show(parentFragmentManager, ChangeNameDialogFragment.TAG)
        }

        btnPokedex.setOnClickListener {
            dismiss()
            startActivity(Intent(requireContext(), PokedexActivity::class.java))
        }

        btnMon.setOnClickListener {
            openMonsterChat()
        }
        btnItems.setOnClickListener {
            dismiss()
            startActivity(
                Intent(
                    requireContext(),
                    com.example.myapplication.ui.bag.BagActivity::class.java
                )
            )
        }


        btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun openMonsterChat() {
        val userId = AuthManager.userId
        if (userId == null) {
            Toast.makeText(context, "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val user = User.fetchById(userId)
                val firstMonsterId = user?.firstMonsterId

                if (firstMonsterId == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No active monster found", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    dismiss()
                    val chatDialog = MonsterChatDialogFragment.newInstance(firstMonsterId)
                    chatDialog.show(parentFragmentManager, MonsterChatDialogFragment.TAG)
                }

            } catch (e: Exception) {
                Log.e("MenuDialog", "Failed to open monster chat", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to load monster", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        const val TAG = "MenuDialogFragment"
    }
}
