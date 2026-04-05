pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Slate"

include(":app")

// Core modules
include(":core:core-common")
include(":core:core-model")
include(":core:core-data")
include(":core:core-database")
include(":core:core-datastore")
include(":core:core-network")
include(":core:core-ui")

// Feature modules
include(":feature:feature-chat")
include(":feature:feature-models")
include(":feature:feature-settings")
include(":feature:feature-onboarding")

// Infrastructure modules
include(":inference:inference-llamacpp")
include(":download:download-engine")
