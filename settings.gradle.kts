pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.ogplayer.tv") { content { includeGroup("tv.ogplayer") } }
    }
}

rootProject.name = "ogplayer-demos"
include(":app")
