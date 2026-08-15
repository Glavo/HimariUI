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

val gestureConformanceDirectory = rootProject.layout.buildDirectory.dir("conformance/m9-gestures")

val gestureConformance = tasks.register<JavaExec>("gestureConformance") {
    group = "verification"
    description = "Runs the M9 gesture-arena competition profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.layout.input.gesture.GestureConformance")
    args(gestureConformanceDirectory.get().asFile.absolutePath)
    outputs.file(gestureConformanceDirectory.map { it.file("results.json") })
}

tasks.named("check") {
    dependsOn(gestureConformance)
}
