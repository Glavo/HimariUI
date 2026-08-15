import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

dependencies {
    implementation(project(":modules:platform-macos"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val output = rootProject.layout.buildDirectory.dir("conformance/m0-macos-window")

tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Probes the production macOS backend."
    dependsOn("pureJavaGuard")
    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("org.glavo.himari.spikes.macos.MacOSSpike")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    args(output.get().asFile.absolutePath)
    outputs.dir(output)
}

tasks.named("check") {
    dependsOn("conformance")
}
