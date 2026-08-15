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
val mountConformanceDirectory = rootProject.layout.buildDirectory.dir(
    "conformance/m1-mount",
)
val effectConformanceDirectory = rootProject.layout.buildDirectory.dir(
    "conformance/m1-effect",
)
val mbtConformanceDirectory = rootProject.layout.buildDirectory.dir(
    "conformance/m1-runtime-mbt",
)
val traceConformanceDirectory = rootProject.layout.buildDirectory.dir(
    "conformance/m1-trace",
)
val sampleConformanceDirectory = rootProject.layout.buildDirectory.dir(
    "conformance/m1-sample",
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

tasks.register<JavaExec>("mountConformance") {
    group = "verification"
    description = "Runs the deterministic M1 mounted-property binding profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.runtime.mount.RuntimeMountConformance")
    args(mountConformanceDirectory.get().asFile.absolutePath)
    outputs.file(mountConformanceDirectory.map { it.file("results.json") })
}

tasks.register<JavaExec>("effectConformance") {
    group = "verification"
    description = "Runs the deterministic M1 keyed-effect lifecycle profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.runtime.effect.RuntimeEffectConformance")
    args(effectConformanceDirectory.get().asFile.absolutePath)
    outputs.file(effectConformanceDirectory.map { it.file("results.json") })
}

tasks.register<JavaExec>("mbtConformance") {
    group = "verification"
    description = "Runs the model-based structural and mount differential harness."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.runtime.mbt.RuntimeModelBasedConformance")
    args(mbtConformanceDirectory.get().asFile.absolutePath)
    outputs.file(mbtConformanceDirectory.map { it.file("results.json") })
}

tasks.register<JavaExec>("traceConformance") {
    group = "verification"
    description = "Runs the deterministic M1 runtime-trace profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.runtime.trace.RuntimeTraceConformance")
    args(traceConformanceDirectory.get().asFile.absolutePath)
    outputs.file(traceConformanceDirectory.map { it.file("results.json") })
}

tasks.register<JavaExec>("sampleConformance") {
    group = "verification"
    description = "Runs the deterministic Headless CounterApp sample."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.runtime.sample.CounterApp")
    args(sampleConformanceDirectory.get().asFile.absolutePath)
    outputs.file(sampleConformanceDirectory.map { it.file("counter.json") })
}

tasks.named("check") {
    dependsOn("animationConformance")
}
