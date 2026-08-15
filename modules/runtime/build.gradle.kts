import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

dependencies {
    api(project(":modules:state"))
    api(project(":modules:platform-api"))
    testImplementation(project(":modules:platform-headless"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val schedulerConformanceDirectory = rootProject.layout.buildDirectory.dir(
    "conformance/m1-scheduler",
)
val animationConformanceDirectory = rootProject.layout.buildDirectory.dir(
    "conformance/m1-animation",
)
val structureConformanceDirectory = rootProject.layout.buildDirectory.dir(
    "conformance/m1-structure",
)

tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the deterministic M1 application and per-window scheduling profile."
    dependsOn(
        ":modules:platform-headless:pureJavaGuard",
        ":modules:platform-headless:test",
        ":modules:state:pureJavaGuard",
        ":modules:state:test",
        "testClasses",
        "test",
        "pureJavaGuard",
    )
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.runtime.RuntimeSchedulerConformance")
    args(schedulerConformanceDirectory.get().asFile.absolutePath)
    outputs.file(schedulerConformanceDirectory.map { it.file("results.json") })
}

tasks.register<JavaExec>("animationConformance") {
    group = "verification"
    description = "Runs the deterministic M1 animation transaction and sampling profile."
    dependsOn(
        ":modules:platform-headless:pureJavaGuard",
        ":modules:platform-headless:test",
        ":modules:state:pureJavaGuard",
        ":modules:state:test",
        "testClasses",
        "test",
        "pureJavaGuard",
    )
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.runtime.animation.RuntimeAnimationConformance")
    args(animationConformanceDirectory.get().asFile.absolutePath)
    outputs.file(animationConformanceDirectory.map { it.file("results.json") })
}

tasks.register<JavaExec>("structureConformance") {
    group = "verification"
    description = "Runs the deterministic M1 grouped-structure and recovery profile."
    dependsOn(
        ":modules:state:pureJavaGuard",
        ":modules:state:test",
        "testClasses",
        "test",
        "pureJavaGuard",
    )
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.runtime.structure.RuntimeStructureConformance")
    args(structureConformanceDirectory.get().asFile.absolutePath)
    outputs.file(structureConformanceDirectory.map { it.file("results.json") })
}
