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

rootProject.name = "FrameByNavin"
include(":app")

// Isolated companion review app. Does not link Firebase, creators' data or reminders.
include(":v217review")

// Offline animation viewer, isolated from both installed Backlot and V217 review.
include(":v218review")
