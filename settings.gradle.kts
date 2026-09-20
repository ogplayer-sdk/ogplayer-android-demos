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
        // tv/android branch: the SDK comes from the LOCAL build published as
        // 1.3.0-tv (./gradlew publishToMavenLocal -PVERSION_NAME=1.3.0-tv in
        // ogplayer-android); flipped back to Maven Central at the 1.3.0 release.
        google()
        mavenCentral()
    }
}

rootProject.name = "ogplayer-demos"
include(":app")
