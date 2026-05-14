package openqwoutt.miniapp.textstyler.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for navigation routes.
 */
class NavRoutesTest {

    @Test
    fun `Main route should be 'main'`() {
        assertEquals("main", NavRoutes.Main.route)
    }

    @Test
    fun `History route should be 'history'`() {
        assertEquals("history", NavRoutes.History.route)
    }

    @Test
    fun `Settings route should be 'settings'`() {
        assertEquals("settings", NavRoutes.Settings.route)
    }

    @Test
    fun `NavRoutes should have exactly three routes`() {
        val routes = listOf(NavRoutes.Main, NavRoutes.History, NavRoutes.Settings)
        assertEquals(3, routes.size)
    }
}