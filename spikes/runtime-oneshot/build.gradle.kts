import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

dependencies {
    api(project(":spikes:runtime-sample"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val conformanceDirectory = rootProject.layout.buildDirectory.dir("conformance/m1-runtime-oneshot")

tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the one-shot owner candidate against the frozen M1 suite."
    dependsOn("classes", "test", "pureJavaGuard")
    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("org.glavo.himari.spikes.runtime.oneshot.OneShotRuntimeConformance")
    args(rootProject.layout.projectDirectory.asFile.absolutePath, conformanceDirectory.get().asFile.absolutePath)
    outputs.file(conformanceDirectory.map { it.file("report.json") })
}

tasks.test {
    systemProperty("himari.repository.root", rootProject.layout.projectDirectory.asFile.absolutePath)
}
