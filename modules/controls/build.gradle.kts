import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

dependencies {
    api(project(":modules:layout"))
    api(project(":modules:platform-api"))
    api(project(":modules:runtime"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val conformanceDirectory = rootProject.layout.buildDirectory.dir("conformance/m9-controls")

val conformance = tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the M9 unstyled control interaction profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.controls.ControlsConformance")
    args(conformanceDirectory.get().asFile.absolutePath)
    outputs.file(conformanceDirectory.map { it.file("results.json") })
}

tasks.named("check") {
    dependsOn(conformance)
}
