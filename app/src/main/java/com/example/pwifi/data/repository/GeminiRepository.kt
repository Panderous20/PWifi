package com.example.pwifi.data.repository

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

class GeminiRepository {
    private val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel("gemini-2.5-flash")

    suspend fun getRespone(prompt: String): String? {
        val request = model.generateContent(prompt)
        return request.text
    }
}