package openqwoutt.miniapp.textstyler.ui.navigation

/**
 * Route definitions for the TextStyler mini-app navigation.
 * Using sealed class for type-safe navigation routes.
 */
sealed class NavRoutes(val route: String) {
    data object Main : NavRoutes("main")
    data object History : NavRoutes("history")
    data object Settings : NavRoutes("settings")
}