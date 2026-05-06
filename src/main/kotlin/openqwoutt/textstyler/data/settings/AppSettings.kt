package openqwoutt.textstyler.data.settings

data class AppSettings(
    val backendUrl: String = "http://10.0.2.2:8080",
    val defaultMode: String = "style",
    val autoPaste: Boolean = true,
    val autoCopyResult: Boolean = true,
    val soundEffects: Boolean = false,
    val hapticFeedback: Boolean = true,
    val saveHistory: Boolean = true,
    val mode: ApiMode = ApiMode.LOCAL_BACKEND,
    val apiKey: String = "",
    val model: String = "google/gemini-2.0-flash-exp:free"
)
