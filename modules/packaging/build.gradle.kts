import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

val sourceSets = extensions.getByType<SourceSetContainer>()
val conformanceDirectory = rootProject.layout.buildDirectory.dir("conformance/m11-packaging")

val conformance = tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the M11 BOM, SBOM, NOTICE, and Native Image registry profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.packaging.PackagingConformance")
    args(conformanceDirectory.get().asFile.absolutePath)
    outputs.files(
        conformanceDirectory.map { it.file("results.json") },
        conformanceDirectory.map { it.file("bom.json") },
        conformanceDirectory.map { it.file("sbom.json") },
        conformanceDirectory.map { it.file("notice.txt") },
        conformanceDirectory.map { it.file("native-image-registry.json") },
        conformanceDirectory.map { it.file("diagnostics.json") },
    )
}

tasks.named("check") {
    dependsOn(conformance)
}
