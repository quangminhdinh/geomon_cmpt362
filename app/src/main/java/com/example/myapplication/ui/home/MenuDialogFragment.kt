package com.example.myapplication.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.myapplication.R
import com.example.myapplication.ui.pokedex.PokedexActivity

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

        btnItems.setOnClickListener {
            Toast.makeText(context, "Items - Coming soon", Toast.LENGTH_SHORT).show()
        }

        btnClose.setOnClickListener {
            dismiss()
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
