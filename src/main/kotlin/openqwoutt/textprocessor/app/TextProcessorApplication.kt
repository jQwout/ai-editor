package openqwoutt.textprocessor.app

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import openqwoutt.textstyler.data.settings.OpenRouterModelsCache
import openqwoutt.textstyler.di.AppGraph
import dev.zacsweers.metro.createGraphFactory

class TextProcessorApplication : Application() {

    lateinit var appGraph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        appGraph = createGraphFactory<AppGraph.Factory>().create(this)

        GlobalScope.launch(Dispatchers.IO) {
            OpenRouterModelsCache.initialize(
                repository = appGraph.openRouterModelsRepository,
                settingsRepository = appGraph.settingsRepository
            )
        }
    }
}
