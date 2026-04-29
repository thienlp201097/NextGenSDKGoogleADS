pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { setUrl("https://jitpack.io") }
        maven { url = uri("https://maven-android.solar-engine.com/repository/se_sdk_for_android/") }
        maven { url = uri("https://dl-maven-android.mintegral.com/repository/se_sdk_for_android/") }

    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { url = uri("https://maven-android.solar-engine.com/repository/se_sdk_for_android/") }
        maven { url = uri("https://dl-maven-android.mintegral.com/repository/se_sdk_for_android/") }

    }
}

rootProject.name = "NextGenDetechAds"
include(":app")
include(":nextgenlibrary")
