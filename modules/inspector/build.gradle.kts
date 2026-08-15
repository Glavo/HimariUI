import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

dependencies {
    api(project(":modules:layout"))
    api(project(":modules:runtime"))
    testImplementation(project(":modules:controls"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val conformanceDirectory = rootProject.layout.buildDirectory.dir("conformance/m10-inspector")

val conformance = tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the M10 tree and frame inspector profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.inspector.InspectorConformance")
    args(conformanceDirectory.get().asFile.absolutePath)
    outputs.files(
        conformanceDirectory.map { it.file("results.json") },
        conformanceDirectory.map { it.file("inspector.json") },
    )
}

tasks.named("check") {
    dependsOn(conformance)
}
