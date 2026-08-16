import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

dependencies {
    implementation(project(":modules:font"))
    implementation(project(":modules:graphics"))
    implementation(project(":modules:layout"))
    implementation(project(":modules:platform-headless"))
    implementation(project(":modules:render-software"))
    implementation(project(":modules:runtime"))
    implementation(project(":modules:text"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val sampleDirectory = rootProject.layout.buildDirectory.dir("conformance/v0-counter")

tasks.register<JavaExec>("v0Sample") {
    group = "verification"
    description = "Runs the V0 Headless CounterApp that writes PNG, extended-linear, and scene replay."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.samples.counter.V0CounterApp")
    args(sampleDirectory.get().asFile.absolutePath)
    outputs.dir(sampleDirectory)
}

tasks.named("check") {
    dependsOn("v0Sample")
}
