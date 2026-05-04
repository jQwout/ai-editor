package openqwoutt.textprocessor.app

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import openqwoutt.textstyler.data.settings.OpenRouterModelsCache

class TextProcessorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        GlobalScope.launch(Dispatchers.IO) {
            OpenRouterModelsCache.initialize(this@TextProcessorApplication)
        }
    }
}
