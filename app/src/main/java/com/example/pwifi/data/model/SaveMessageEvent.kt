package com.example.pwifi.data.model

sealed class SaveMessageEvent {
    data class Success(val filePath: String) : SaveMessageEvent()
    data object Error : SaveMessageEvent()
}