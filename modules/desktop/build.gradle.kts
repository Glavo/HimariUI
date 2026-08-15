import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test

dependencies {
    implementation(project(":modules:controls"))
    implementation(project(":modules:font"))
    implementation(project(":modules:graphics"))
    implementation(project(":modules:inspector"))
    implementation(project(":modules:layout"))
    implementation(project(":modules:objc"))
    api(project(":modules:platform-api"))
    implementation(project(":modules:platform-headless"))
    implementation(project(":modules:platform-macos"))
    implementation(project(":modules:platform-wayland"))
    implementation(project(":modules:platform-windows"))
    implementation(project(":modules:render-software"))
    implementation(project(":modules:rhi-d3d12"))
    implementation(project(":modules:rhi-metal"))
    implementation(project(":modules:runtime"))
    implementation(project(":modules:state"))
    implementation(project(":modules:text"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val conformanceDirectory = rootProject.layout.buildDirectory.dir("conformance/m11-desktop")
val smokeDirectory = layout.buildDirectory.dir("conformance/m11-desktop-smoke")
val smokeLog = "build/conformance/m11-desktop-smoke/native-load.log"
val launchDirectory = rootProject.layout.buildDirectory.dir("conformance/m11-desktop-launch")

pureJavaGuardConfig {
    ffmBoundary.set(true)
    nativeAccess.set(true)
    nativeLoadTrace.set(layout.buildDirectory.file("conformance/m11-desktop-smoke/native-load.log"))
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

val desktopSmoke = tasks.register<JavaExec>("desktopSmoke") {
    group = "verification"
    description = "Captures the desktop native-load allowlist trace."
    dependsOn("testClasses")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.desktop.DesktopConformance")
    workingDir = project.projectDir
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "-Xlog:library=trace:file=$smokeLog:uptime,level,tags:filecount=0",
    )
    args(smokeDirectory.get().asFile.absolutePath)
    outputs.file(smokeDirectory.map { it.file("native-load.log") })
}

tasks.named("verifyNativeLoadTrace") {
    dependsOn(desktopSmoke)
}

val desktopLaunch = tasks.register<JavaExec>("desktopLaunch") {
    group = "verification"
    description = "Launches the production himari-desktop CounterApp in smoke mode."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.desktop.HimariDesktop")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    args("--smoke", launchDirectory.get().asFile.absolutePath)
    outputs.dir(launchDirectory)
}

val conformance = tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the production himari-desktop profile."
    dependsOn("testClasses", "test", "pureJavaGuard", desktopLaunch)
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.desktop.DesktopConformance")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    args(conformanceDirectory.get().asFile.absolutePath)
    outputs.file(conformanceDirectory.map { it.file("results.json") })
}

tasks.named("check") {
    dependsOn(conformance)
}
