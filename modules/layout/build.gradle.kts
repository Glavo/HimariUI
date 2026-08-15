// Layout is a pure-Java constraints-down / sizes-up implementation.

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

val sourceSets = extensions.getByType<SourceSetContainer>()
val layoutConformanceDirectory = rootProject.layout.buildDirectory.dir("conformance/m2-layout")

tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the deterministic M2 layout, input, focus, and semantics profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.layout.LayoutConformance")
    args(layoutConformanceDirectory.get().asFile.absolutePath)
    outputs.file(layoutConformanceDirectory.map { it.file("results.json") })
}
