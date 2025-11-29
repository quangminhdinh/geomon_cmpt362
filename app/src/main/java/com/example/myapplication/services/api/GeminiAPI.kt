package com.example.myapplication.services.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiAPI {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    private const val MODEL = "gemini-2.0-flash"

    private var apiKey: String? = null

    fun setApiKey(key: String) {
        apiKey = key
    }

    /**
     * Generate content using Gemini REST API
     */
    suspend fun generateContent(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey == null) {
                    Log.e("GeminiAPI", "API key not set")
                    return@withContext "Error: API key not configured"
                }

                val url = URL("$BASE_URL/$MODEL:generateContent?key=$apiKey")
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                // Build request body
                val requestBody = buildRequestBody(prompt)

                // Send request
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                // Read response
                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }

                    parseResponse(response)
                } else {
                    val errorStream = connection.errorStream
                    val errorResponse = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    } else {
                        "Unknown error"
                    }

                    Log.e("GeminiAPI", "Error $responseCode: $errorResponse")
                    "Sorry, I'm having trouble thinking right now..."
                }

            } catch (e: Exception) {
                Log.e("GeminiAPI", "Request failed", e)
                "Sorry, I encountered an error: ${e.message}"
            }
        }
    }

    private fun buildRequestBody(prompt: String): JSONObject {
        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.9)
                put("topK", 40)
                put("topP", 0.95)
                put("maxOutputTokens", 200)
            })
            put("safetySettings", JSONArray().apply {
                addSafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_NONE")
                addSafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_NONE")
                addSafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_NONE")
                addSafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_NONE")
            })
        }
    }

    private fun JSONArray.addSafetySetting(category: String, threshold: String) {
        put(JSONObject().apply {
            put("category", category)
            put("threshold", threshold)
        })
    }

    private fun parseResponse(jsonResponse: String): String {
        return try {
            val json = JSONObject(jsonResponse)
            val candidates = json.getJSONArray("candidates")

            if (candidates.length() == 0) {
                Log.e("GeminiAPI", "No candidates in response")
                return "..."
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")

            if (parts.length() == 0) {
                Log.e("GeminiAPI", "No parts in response")
                return "..."
            }

            val text = parts.getJSONObject(0).getString("text")
            text.trim()

        } catch (e: Exception) {
            Log.e("GeminiAPI", "Failed to parse response", e)
            "..."
        }
    }
}