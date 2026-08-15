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
    ":modules:ffi",
    ":modules:platform-api",
    ":modules:platform-headless",
    ":modules:runtime",
    ":modules:state",
    ":spikes:abi-probe",
    ":spikes:d3d12",
    ":spikes:ffi-ffm",
    ":spikes:native-image-ffm",
    ":spikes:runtime-grouped",
    ":spikes:runtime-hybrid",
    ":spikes:runtime-decision",
    ":spikes:runtime-oneshot",
    ":spikes:runtime-sample",
    ":spikes:win32",
    ":tools:ffi-schema",
)

project(":modules:platform-api").projectDir = file("modules/platform/api")
project(":modules:platform-headless").projectDir = file("modules/platform/headless")
