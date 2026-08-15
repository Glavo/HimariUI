import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

dependencies {
    api(project(":modules:font"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val conformanceDirectory = rootProject.layout.buildDirectory.dir("conformance/m8-shape")

val conformance = tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the M8 Arabic and Hebrew shaping profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.text.ShapeConformance")
    args(conformanceDirectory.get().asFile.absolutePath)
    outputs.file(conformanceDirectory.map { it.file("results.json") })
}

tasks.named("check") {
    dependsOn(conformance)
}
