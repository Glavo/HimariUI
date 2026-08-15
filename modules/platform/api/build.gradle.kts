import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

val sourceSets = extensions.getByType<SourceSetContainer>()
val conformanceDirectory = rootProject.layout.buildDirectory.dir("conformance/m9-linux-a11y")

val conformance = tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the portable D-Bus / AT-SPI2 probe profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.platform.api.AtSpiConformance")
    args(conformanceDirectory.get().asFile.absolutePath)
    outputs.file(conformanceDirectory.map { it.file("results.json") })
}

tasks.named("check") {
    dependsOn(conformance)
}
