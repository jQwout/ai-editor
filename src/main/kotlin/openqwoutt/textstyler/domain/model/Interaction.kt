package openqwoutt.miniapp.textstyler.domain.model

/**
 * Domain model for interaction history.
 */
data class Interaction(
    val id: Long,
    val timestamp: Long,
    val inputText: String,
    val outputText: String?,
    val mode: String,
    val status: InteractionStatus,
    val errorMessage: String?
) {
    val inputPreview: String
        get() = if (inputText.length > 50) inputText.take(50) + "..." else inputText

    val outputPreview: String
        get() = outputText?.let { if (it.length > 50) it.take(50) + "..." else it } ?: ""

    val relativeTime: String
        get() {
            val diff = System.currentTimeMillis() - timestamp
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                seconds < 60 -> "Just now"
                minutes < 60 -> "$minutes min ago"
                hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
                days == 1L -> "Yesterday"
                else -> "$days days ago"
            }
        }
}

/**
 * Status of interaction.
 */
enum class InteractionStatus {
    SUCCESS,
    ERROR
}