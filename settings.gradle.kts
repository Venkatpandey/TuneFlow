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
    }
}

rootProject.name = "TuneFlow"
include(":app")
include(":core:design")
include(":core:network")
include(":core:player")
include(":feature:auth")
include(":feature:browse")
include(":feature:playback")
