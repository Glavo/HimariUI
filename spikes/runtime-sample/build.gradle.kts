import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

dependencies {
    api(project(":modules:platform-headless"))
    api(project(":modules:state"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val conformanceDirectory = rootProject.layout.buildDirectory.dir("conformance/m1-runtime-sample")

tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Validates the frozen M1 runtime comparison suite, rubric, instrumentation, and report format."
    dependsOn("classes", "test", "pureJavaGuard")
    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("org.glavo.himari.spikes.runtime.sample.RuntimeSampleConformance")
    args(rootProject.layout.projectDirectory.asFile.absolutePath, conformanceDirectory.get().asFile.absolutePath)
    outputs.files(
        conformanceDirectory.map { it.file("suite.json") },
        conformanceDirectory.map { it.file("rubric.json") },
        conformanceDirectory.map { it.file("self-test-report.json") },
    )
}

tasks.test {
    systemProperty("himari.repository.root", rootProject.layout.projectDirectory.asFile.absolutePath)
}
