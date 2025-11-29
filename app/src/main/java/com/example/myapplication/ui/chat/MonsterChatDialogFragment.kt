package com.example.myapplication.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
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
    private val conversationHistory = mutableListOf<String>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var tvMonsterName: TextView
    private lateinit var imgMonsterSprite: ImageView
    private lateinit var btnClose: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog_MinWidth)

        // Initialize Gemini
        MonsterAI.initialize(requireContext())
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
        progressBar = view.findViewById(R.id.progressBar)

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
                    tvMonsterName.text = "${monster.name} (Lv.${monster.level})"

                    // Load sprite
                    val spriteName = monster.name.lowercase().replace(" ", "_")
                    val resourceId = resources.getIdentifier(spriteName, "drawable", requireContext().packageName)
                    if (resourceId != 0) {
                        imgMonsterSprite.setImageResource(resourceId)
                    }

                    // Generate welcome message using AI
                    generateWelcomeMessage()
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

    private fun generateWelcomeMessage() {
        lifecycleScope.launch {
            showLoading(true)

            val welcomeMessage = MonsterAI.getChatResponse(
                monsterName = monster.name,
                monsterType = monster.type1,
                monsterLevel = monster.level,
                userMessage = "Hi! I'm your trainer.",
                conversationHistory = emptyList()
            )

            addMonsterMessage(welcomeMessage)
            showLoading(false)
        }
    }

    private fun sendMessage() {
        val userMessage = etMessage.text.toString().trim()
        if (userMessage.isEmpty()) return

        // Add user message
        addUserMessage(userMessage)
        conversationHistory.add("User: $userMessage")
        etMessage.text.clear()

        // Disable input while processing
        setInputEnabled(false)
        showLoading(true)

        // Get AI response
        lifecycleScope.launch {
            try {
                val aiResponse = MonsterAI.getChatResponse(
                    monsterName = monster.name,
                    monsterType = monster.type1,
                    monsterLevel = monster.level,
                    userMessage = userMessage,
                    conversationHistory = conversationHistory
                )

                addMonsterMessage(aiResponse)
                conversationHistory.add("${monster.name}: $aiResponse")

                // Keep conversation history manageable (last 10 messages)
                if (conversationHistory.size > 10) {
                    conversationHistory.removeAt(0)
                    conversationHistory.removeAt(0)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error generating response", e)
                addMonsterMessage("Sorry, I'm having trouble thinking right now...")
            } finally {
                setInputEnabled(true)
                showLoading(false)
            }
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

    private fun setInputEnabled(enabled: Boolean) {
        btnSend.isEnabled = enabled
        etMessage.isEnabled = enabled
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
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