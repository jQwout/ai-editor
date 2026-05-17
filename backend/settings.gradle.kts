// Stand-alone Gradle settings for `:backend`-only workflows (Docker build, tooling) when this directory
// is used as the Gradle project root — not used by the main Android repo at ../settings.gradle.kts.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.2.20"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.2.20"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "SideAiEditorBackend"
