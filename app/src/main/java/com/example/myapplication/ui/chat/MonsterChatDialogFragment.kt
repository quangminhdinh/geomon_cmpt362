package com.example.myapplication.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.battle.Monster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MonsterChatDialogFragment : DialogFragment() {

    private lateinit var monster: Monster
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var tvMonsterName: TextView
    private lateinit var imgMonsterSprite: ImageView
    private lateinit var btnClose: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog_MinWidth)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_monster_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerChat)
        etMessage = view.findViewById(R.id.etMessage)
        btnSend = view.findViewById(R.id.btnSend)
        tvMonsterName = view.findViewById(R.id.tvMonsterName)
        imgMonsterSprite = view.findViewById(R.id.imgMonsterSprite)
        btnClose = view.findViewById(R.id.btnClose)

        // Setup RecyclerView
        chatAdapter = ChatAdapter(chatMessages)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = chatAdapter

        // Load monster data
        val monsterId = arguments?.getString(ARG_MONSTER_ID)
        if (monsterId == null) {
            Toast.makeText(context, "Monster ID not found", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        loadMonsterData(monsterId)

        // Setup button listeners
        btnSend.setOnClickListener {
            sendMessage()
        }

        btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun loadMonsterData(monsterId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fetchedMonster = Monster.fetchById(monsterId)

                if (fetchedMonster == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load monster", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                    return@launch
                }

                monster = fetchedMonster

                withContext(Dispatchers.Main) {
                    tvMonsterName.text = monster.name

                    // Load sprite
                    val spriteName = monster.name.lowercase().replace(" ", "_")
                    val resourceId = resources.getIdentifier(spriteName, "drawable", requireContext().packageName)
                    if (resourceId != 0) {
                        imgMonsterSprite.setImageResource(resourceId)
                    }

                    // Add welcome message
                    addMonsterMessage("Hi! I'm ${monster.name}. Let's chat!")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading monster", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }

    private fun sendMessage() {
        val userMessage = etMessage.text.toString().trim()
        if (userMessage.isEmpty()) return

        // Add user message
        addUserMessage(userMessage)
        etMessage.text.clear()

        // Disable send button while processing
        btnSend.isEnabled = false

        // TODO: Get AI response here
        // For now, just echo back
        lifecycleScope.launch {
            kotlinx.coroutines.delay(1000) // Simulate thinking
            addMonsterMessage("You said: $userMessage")
            btnSend.isEnabled = true
        }
    }

    private fun addUserMessage(message: String) {
        chatMessages.add(ChatMessage(message, isUser = true))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        recyclerView.scrollToPosition(chatMessages.size - 1)
    }

    private fun addMonsterMessage(message: String) {
        chatMessages.add(ChatMessage(message, isUser = false))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        recyclerView.scrollToPosition(chatMessages.size - 1)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )
    }

    companion object {
        const val TAG = "MonsterChatDialog"
        private const val ARG_MONSTER_ID = "monster_id"

        fun newInstance(monsterId: String): MonsterChatDialogFragment {
            val fragment = MonsterChatDialogFragment()
            val args = Bundle()
            args.putString(ARG_MONSTER_ID, monsterId)
            fragment.arguments = args
            return fragment
        }
    }
}