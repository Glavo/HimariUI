import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

dependencies {
    api(project(":modules:graphics"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val output = rootProject.layout.buildDirectory.dir("conformance/m3-software")
val replayDirectory = rootProject.layout.buildDirectory.dir("conformance/m10-replay")

tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the software-renderer PNG conformance profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.render.software.SoftwareConformance")
    args(output.get().asFile.absolutePath)
    outputs.file(output.map { it.file("results.json") })
}

val replayConformance = tasks.register<JavaExec>("replayConformance") {
    group = "verification"
    description = "Runs the M10 offline scene-replay profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.render.software.SceneReplayConformance")
    args(replayDirectory.get().asFile.absolutePath)
    outputs.files(
        replayDirectory.map { it.file("results.json") },
        replayDirectory.map { it.file("scene.json") },
    )
}

tasks.named("check") {
    dependsOn(replayConformance)
}
