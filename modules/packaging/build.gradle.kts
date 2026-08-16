import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import java.io.File

val sourceSets = extensions.getByType<SourceSetContainer>()
val conformanceDirectory = rootProject.layout.buildDirectory.dir("conformance/m11-packaging")
val imageDirectory = conformanceDirectory.map { it.dir("image") }
val jlinkModules = listOf(
    project(":modules:ffi"),
    project(":modules:font"),
    project(":modules:graphics"),
    project(":modules:layout"),
    project(":modules:platform-api"),
    project(":modules:platform-headless"),
    project(":modules:platform-windows"),
    project(":modules:platform-wayland"),
    project(":modules:platform-macos"),
    project(":modules:controls"),
    project(":modules:inspector"),
    project(":modules:packaging"),
    project(":modules:desktop"),
    project(":modules:rhi-d3d12"),
    project(":modules:rhi-vulkan"),
    project(":modules:rhi-metal"),
    project(":modules:objc"),
    project(":modules:render-software"),
    project(":modules:runtime"),
    project(":modules:state"),
    project(":modules:text"),
)

val counterModules = listOf(
    project(":modules:samples:counter"),
    project(":modules:font"),
    project(":modules:graphics"),
    project(":modules:layout"),
    project(":modules:platform-api"),
    project(":modules:platform-headless"),
    project(":modules:render-software"),
    project(":modules:runtime"),
    project(":modules:state"),
    project(":modules:text"),
)

val conformance = tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the M11 BOM, SBOM, NOTICE, Native Image registry, and jlink profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    jlinkModules.forEach { module ->
        dependsOn(module.tasks.named("jar"))
    }
    counterModules.forEach { module ->
        dependsOn(module.tasks.named("jar"))
    }
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.packaging.PackagingConformance")
    outputs.files(
        conformanceDirectory.map { it.file("results.json") },
        conformanceDirectory.map { it.file("bom.json") },
        conformanceDirectory.map { it.file("sbom.json") },
        conformanceDirectory.map { it.file("notice.txt") },
        conformanceDirectory.map { it.file("native-image-registry.json") },
        conformanceDirectory.map { it.file("native-image-probe.json") },
        conformanceDirectory.map { it.file("jlink-recipe.json") },
        conformanceDirectory.map { it.file("jlink-image.json") },
        conformanceDirectory.map { it.file("diagnostics.json") },
    )
    doFirst {
        val modulePath = ArrayList<String>()
        modulePath.add(System.getProperty("java.home") + File.separator + "jmods")
        jlinkModules.forEach { module ->
            modulePath.add(module.tasks.named<Jar>("jar").get().archiveFile.get().asFile.absolutePath)
        }
        val counterClassPath = ArrayList<String>()
        counterModules.forEach { module ->
            counterClassPath.add(module.tasks.named<Jar>("jar").get().archiveFile.get().asFile.absolutePath)
        }
        setArgs(listOf(
            conformanceDirectory.get().asFile.absolutePath,
            modulePath.joinToString(File.pathSeparator),
            imageDirectory.get().asFile.absolutePath,
            counterClassPath.joinToString(File.pathSeparator),
        ))
    }
}

tasks.named("check") {
    dependsOn(conformance)
}
