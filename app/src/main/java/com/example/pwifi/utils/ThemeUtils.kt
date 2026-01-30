package com.example.pwifi.utils

import android.content.Context
import android.content.SharedPreferences

object ThemeUtils {
    private const val PREF_NAME = "pwifi_prefs"
    private const val KEY_IS_DARK_MODE = "key_is_dark_mode"

    // Hàm lưu trạng thái
    fun saveThemeMode(context: Context, isDarkMode: Boolean) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_DARK_MODE, isDarkMode).apply()
    }

    // Hàm lấy trạng thái (mặc định trả về null để biết là chưa set gì cả)
    fun getThemeMode(context: Context): Boolean? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_IS_DARK_MODE)) {
            prefs.getBoolean(KEY_IS_DARK_MODE, false)
        } else {
            null // dùng theo hệ thống
        }
    }
}