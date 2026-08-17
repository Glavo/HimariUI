import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

dependencies {
    api(project(":modules:graphics"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val output = rootProject.layout.buildDirectory.dir("conformance/m10-vector")

tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the optional vector-fill conformance profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.render.vector.VectorConformance")
    args(output.get().asFile.absolutePath)
    outputs.file(output.map { it.file("results.json") })
}

tasks.named("check") {
    dependsOn("conformance")
}
