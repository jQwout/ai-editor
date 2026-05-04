package openqwoutt.textstyler.data.settings

data class AppSettings(
    val mode: ApiMode = ApiMode.LOCAL_BACKEND,
    val apiKey: String = "",
    val model: String = "google/gemini-2.0-flash-exp:free",
    val backendUrl: String = "http://10.0.2.2:8080"
)
