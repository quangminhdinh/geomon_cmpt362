package com.example.myapplication.ui.chat

import android.content.Context
import android.util.Log
import com.example.myapplication.services.api.GeminiAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MonsterAI {

    fun initialize(context: Context) {
        try {
            val apiKey = context.packageManager
                .getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
                .metaData
                .getString("com.google.ai.client.generativeai.API_KEY")

            if (apiKey.isNullOrEmpty()) {
                Log.e("MonsterAI", "Gemini API key not found in manifest")
                return
            }

            GeminiAPI.setApiKey(apiKey)
            Log.d("MonsterAI", "Gemini API initialized successfully")

        } catch (e: Exception) {
            Log.e("MonsterAI", "Failed to initialize Gemini", e)
        }
    }

    suspend fun getChatResponse(
        monsterName: String,
        monsterType: String?,
        monsterLevel: Int,
        userMessage: String,
        conversationHistory: List<String> = emptyList()
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildChatPrompt(
                    monsterName,
                    monsterType,
                    monsterLevel,
                    userMessage,
                    conversationHistory
                )

                val response = GeminiAPI.generateContent(prompt)
                Log.d("MonsterAI", "Generated response: $response")

                response

            } catch (e: Exception) {
                Log.e("MonsterAI", "Failed to generate chat response", e)
                getFallbackResponse(monsterName, userMessage)
            }
        }
    }

    private fun buildChatPrompt(
        name: String,
        type: String?,
        level: Int,
        userMessage: String,
        history: List<String>
    ): String {
        val personality = getTypePersonality(type)

        val historyText = if (history.isNotEmpty()) {
            "Recent conversation:\n${history.takeLast(6).joinToString("\n")}\n\n"
        } else {
            ""
        }

        return """
You are $name, a $type-type monster at level $level.

Your personality: $personality

${historyText}User just said: "$userMessage"

Respond as $name in 1-3 sentences (max 50 words). Stay in character, be friendly and engaging. Show emotion and personality. Don't break the fourth wall or mention you're an AI.

Your response:
        """.trimIndent()
    }

    private fun getTypePersonality(type: String?): String {
        return when (type?.lowercase()) {
            "fire" -> "You're hot-headed, passionate, and energetic. You love showing off your power and get excited easily."
            "water" -> "You're calm, flowing, and adaptable. You're patient and strategic, preferring to go with the flow."
            "grass" -> "You're patient, nurturing, and peaceful. You value growth and harmony with nature."
            "electric" -> "You're energetic, quick-witted, and unpredictable. You love speed and excitement."
            "psychic" -> "You're intelligent, mysterious, and thoughtful. You're contemplative and enjoy deep conversations."
            "fighting" -> "You're strong, determined, and honorable. You value training and physical strength."
            "dragon" -> "You're proud, powerful, and wise. You're majestic and command respect."
            "dark" -> "You're cunning, mysterious, and playful. You enjoy tricks and mischief."
            "ghost" -> "You're spooky, ethereal, and playful. You enjoy scaring others in a fun way."
            "poison" -> "You're resilient, adaptable, and a bit toxic. You're surprisingly friendly despite your dangerous nature."
            "rock" -> "You're solid, dependable, and tough. You're reliable and protective."
            "ground" -> "You're grounded, steady, and strong. You're practical and down-to-earth."
            "flying" -> "You're free-spirited, adventurous, and carefree. You love freedom and exploration."
            "bug" -> "You're energetic, hardworking, and persistent. You're determined and never give up."
            "steel" -> "You're strong, disciplined, and protective. You value structure and order."
            "ice" -> "You're cool, calm, and composed. You're elegant but can be fierce when needed."
            "fairy" -> "You're magical, playful, and kind. You bring joy and wonder to others."
            "normal" -> "You're versatile, friendly, and reliable. You're adaptable and easy to get along with."
            else -> "You're unique and full of personality. You're friendly and love to chat."
        }
    }

    private fun getFallbackResponse(name: String, userMessage: String): String {
        val responses = listOf(
            "That's interesting! Tell me more.",
            "I see! How does that make you feel?",
            "Hmm, I'm thinking about that...",
            "That's really cool!",
            "I appreciate you sharing that with me.",
            "What an interesting thought!"
        )
        return responses.random()
    }
}
