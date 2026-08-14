pluginManagement {
    includeBuild("build-logic/pure-java-guard")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
    }
}

rootProject.name = "HimariUI"

include(
    ":modules:platform-api",
    ":modules:runtime",
    ":modules:state",
)

project(":modules:platform-api").projectDir = file("modules/platform/api")
