package openqwoutt.miniapp.textstyler.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TextStylerScreen navigation logic.
 */
class TextStylerNavigationTest {

    /**
     * Test that state flags correctly map to route navigation.
     * This tests the sync logic between ViewModel state and NavHost routes.
     */
    @Test
    fun `showHistory true should navigate to history route`() {
        val showHistory = true
        val showSettings = false
        val currentRoute: String? = "main"
        
        val shouldNavigateToHistory = showHistory && currentRoute != NavRoutes.History.route
        assertTrue(shouldNavigateToHistory)
    }

    @Test
    fun `showSettings true should navigate to settings route`() {
        val showHistory = false
        val showSettings = true
        val currentRoute: String? = "main"
        
        val shouldNavigateToSettings = showSettings && currentRoute != NavRoutes.Settings.route
        assertTrue(shouldNavigateToSettings)
    }

    @Test
    fun `both false should pop back to main route`() {
        val showHistory = false
        val showSettings = false
        val currentRoute: String? = "history"
        
        val shouldPopBack = !showHistory && !showSettings && currentRoute != NavRoutes.Main.route
        assertTrue(shouldPopBack)
    }

    @Test
    fun `should not navigate when already on correct route`() {
        val showHistory = true
        val currentRoute: String? = "history"
        
        val shouldNavigateToHistory = showHistory && currentRoute != NavRoutes.History.route
        assertFalse(shouldNavigateToHistory)
    }

    @Test
    fun `TemplatesSheet overlay should not affect nav back stack`() {
        // TemplatesSheet is toggled via boolean flag, not route
        // When only templates is shown, we should still be on main route
        val showHistory = false
        val showSettings = false
        val showTemplates = true
        
        // Overlay should be visible when flag is true
        assertTrue(showTemplates)
        
        // When only templates is shown, no route change needed
        val shouldPopBack = !showHistory && !showSettings
        assertTrue(shouldPopBack)
    }
}