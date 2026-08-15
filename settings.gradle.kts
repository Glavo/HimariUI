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
    ":modules:font",
    ":modules:graphics",
    ":modules:layout",
    ":modules:platform-api",
    ":modules:platform-headless",
    ":modules:platform-windows",
    ":modules:platform-wayland",
    ":modules:platform-macos",
    ":modules:controls",
    ":modules:inspector",
    ":modules:packaging",
    ":modules:rhi-d3d12",
    ":modules:rhi-vulkan",
    ":modules:rhi-metal",
    ":modules:objc",
    ":modules:render-software",
    ":modules:runtime",
    ":modules:state",
    ":modules:text",
    ":modules:samples:counter",
    ":spikes:abi-probe",
    ":spikes:d3d12",
    ":spikes:ffi-ffm",
    ":spikes:macos",
    ":spikes:metal",
    ":spikes:native-image-ffm",
    ":spikes:objc-block",
    ":spikes:runtime-grouped",
    ":spikes:runtime-hybrid",
    ":spikes:runtime-decision",
    ":spikes:runtime-oneshot",
    ":spikes:runtime-sample",
    ":spikes:vulkan",
    ":spikes:wayland",
    ":spikes:win32",
    ":tools:ffi-schema",
)

project(":modules:platform-api").projectDir = file("modules/platform/api")
project(":modules:platform-headless").projectDir = file("modules/platform/headless")
project(":modules:platform-windows").projectDir = file("modules/platform/windows")
project(":modules:platform-wayland").projectDir = file("modules/platform/wayland")
project(":modules:platform-macos").projectDir = file("modules/platform/macos")
project(":modules:rhi-d3d12").projectDir = file("modules/rhi/d3d12")
project(":modules:rhi-vulkan").projectDir = file("modules/rhi/vulkan")
project(":modules:rhi-metal").projectDir = file("modules/rhi/metal")
project(":modules:samples:counter").projectDir = file("modules/samples/counter")
