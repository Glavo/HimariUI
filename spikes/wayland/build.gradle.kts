import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

dependencies {
    implementation(project(":modules:platform-wayland"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val output = rootProject.layout.buildDirectory.dir("conformance/m0-wayland-window")

tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Probes libwayland-client through the production Wayland backend."
    dependsOn("pureJavaGuard")
    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("org.glavo.himari.spikes.wayland.WaylandSpike")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    args(output.get().asFile.absolutePath)
    outputs.dir(output)
}
