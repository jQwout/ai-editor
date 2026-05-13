package openqwoutt.textstyler.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelProvider
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import openqwoutt.miniapp.textstyler.data.local.AppDatabase
import openqwoutt.miniapp.textstyler.data.local.InteractionDao
import openqwoutt.miniapp.textstyler.data.prompts.PromptRepository
import openqwoutt.miniapp.textstyler.data.repository.InteractionRepository
import openqwoutt.miniapp.textstyler.domain.TextProcessorUseCase
import openqwoutt.miniapp.textstyler.presentation.HistoryViewModel
import openqwoutt.miniapp.textstyler.presentation.TextStylerViewModel
import openqwoutt.textprocessor.app.BuildConfig
import openqwoutt.textstyler.data.settings.EncryptedPrefs
import openqwoutt.textstyler.data.settings.OpenRouterModelsRepository
import openqwoutt.textstyler.data.settings.SecureStorage
import openqwoutt.textstyler.data.settings.SettingsRepository
import openqwoutt.textstyler.network.AiApiClient
import openqwoutt.miniapp.textstyler.service.voice.AndroidSpeechRecognitionService
import openqwoutt.miniapp.textstyler.service.voice.SpeechRecognitionService

/**
 * Application-scoped Metro dependency graph.
 */
@SingleIn(AppScope::class)
@DependencyGraph
interface AppGraph {

    @Named("textStyler")
    val textStylerViewModelFactory: ViewModelProvider.Factory

    @Named("history")
    val historyViewModelFactory: ViewModelProvider.Factory

    /** Repositories */
    val settingsRepository: SettingsRepository
    val promptRepository: PromptRepository
    val interactionRepository: InteractionRepository
    val secureStorage: SecureStorage
    val openRouterModelsRepository: OpenRouterModelsRepository
    val aiApiClient: AiApiClient

    /** Use case */
    val textProcessorUseCase: TextProcessorUseCase

    /** Database */
    val appDatabase: AppDatabase
    val interactionDao: InteractionDao

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Application): AppGraph
    }

    /** Providers */
    @Provides
    @SingleIn(AppScope::class)
    fun provideContext(application: Application): Context = application.applicationContext

    @Provides
    @SingleIn(AppScope::class)
    fun provideAppDatabase(context: Context): AppDatabase = AppDatabase.getInstance(context)

    @Provides
    @SingleIn(AppScope::class)
    fun provideInteractionDao(database: AppDatabase): InteractionDao = database.interactionDao()

    @Provides
    @SingleIn(AppScope::class)
    @Named("securePrefs")
    fun provideSecurePrefs(context: Context): SharedPreferences =
        EncryptedPrefs.create(context, SecureStorage.PREFS_NAME)

    @Provides
    @SingleIn(AppScope::class)
    @Named("settingsPrefs")
    fun provideSettingsPrefs(context: Context): SharedPreferences =
        EncryptedPrefs.create(context, SettingsRepository.PREFS_NAME)

    @Provides
    @SingleIn(AppScope::class)
    fun provideAiApiClient(): AiApiClient = AiApiClient()

    @Provides
    @SingleIn(AppScope::class)
    fun providePromptRepository(context: Context): PromptRepository = PromptRepository(context)

    @Provides
    @SingleIn(AppScope::class)
    fun provideTextProcessorUseCase(
        settingsRepository: SettingsRepository,
        aiApiClient: AiApiClient
    ): TextProcessorUseCase =
        TextProcessorUseCase(
            settingsRepository = settingsRepository,
            apiClient = aiApiClient,
            maxChars = 3000,
            backendUrl = BuildConfig.AI_BACKEND_URL
        )

    @Provides
    @SingleIn(AppScope::class)
    fun provideSpeechRecognitionService(context: Context): SpeechRecognitionService =
        AndroidSpeechRecognitionService(context)

    @Provides
    @SingleIn(AppScope::class)
    @Named("textStyler")
    fun provideTextStylerViewModelFactory(
        textProcessorUseCase: TextProcessorUseCase,
        settingsRepository: SettingsRepository,
        promptRepository: PromptRepository,
        interactionRepository: InteractionRepository,
        speechRecognitionService: SpeechRecognitionService
    ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return TextStylerViewModel(
                textProcessorUseCase = textProcessorUseCase,
                settingsRepository = settingsRepository,
                promptRepository = promptRepository,
                interactionRepository = interactionRepository,
                speechService = speechRecognitionService
            ) as T
        }
    }

    @Provides
    @SingleIn(AppScope::class)
    @Named("history")
    fun provideHistoryViewModelFactory(
        interactionRepository: InteractionRepository
    ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(interactionRepository) as T
        }
    }
}

/** Application scope marker */
abstract class AppScope private constructor()
