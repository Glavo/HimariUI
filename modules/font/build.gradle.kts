import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

val sourceSets = extensions.getByType<SourceSetContainer>()
val output = rootProject.layout.buildDirectory.dir("conformance/m4-font")

tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the SFNT cmap and grayscale raster conformance profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.font.FontConformance")
    args(output.get().asFile.absolutePath)
    outputs.files(
        output.map { it.file("results.json") },
        output.map { it.file("leftovers.json") },
    )
}
