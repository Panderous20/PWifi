package com.example.pwifi.data.repository

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor() {

    val model = Firebase.ai(backend= GenerativeBackend.googleAI())
        .generativeModel("gemini-2.5-flash")

    suspend fun getRespone(prompt: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val response = model.generateContent(prompt)
                print(response.text)
                response.text
            } catch (e: Exception) {
                e.printStackTrace()
                "${e.message}"
            }
        }
    }
}