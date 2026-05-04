package openqwoutt.textstyler.data.settings

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val backendUrl: String = "http://10.0.2.2:8080",
    val defaultMode: String = "style",
    val autoPaste: Boolean = true,
    val autoCopyResult: Boolean = true,
    val soundEffects: Boolean = false,
    val hapticFeedback: Boolean = true
)