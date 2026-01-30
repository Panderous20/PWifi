package com.example.pwifi.utils

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

data class LanguageUtils(
    val code: String,
    val name: String,
    val icon: String
)

val supportedLanguages = listOf(
    LanguageUtils("en", "English", "🇺🇸"),
    LanguageUtils("vi", "Tiếng Việt", "🇻🇳")
)

fun changeAppLanguage(context: Context, langagueCode: String)
{
    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU)
    {
        val localManager = context.getSystemService(LocaleManager::class.java)
        val appLocal = LocaleList.forLanguageTags(langagueCode)

        localManager.applicationLocales = appLocal
    }
}

fun getCurrentLanguageCode(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        val currentAppLocales = localeManager.applicationLocales
        return if (!currentAppLocales.isEmpty) {
            currentAppLocales.get(0).language
        } else {
            context.resources.configuration.locales.get(0).language
        }
    }
    return "en"
}