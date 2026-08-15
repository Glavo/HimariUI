import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.language.jvm.tasks.ProcessResources

val sourceSets = extensions.getByType<SourceSetContainer>()
val conformanceDirectory = rootProject.layout.buildDirectory.dir("conformance/m0-ffi-schema")

tasks.named<ProcessResources>("processResources") {
    from(rootProject.layout.projectDirectory.file("schema/ffi-schema.schema.json")) {
        into("schema")
    }
    from(rootProject.layout.projectDirectory.file("schema/abi-probe.schema.json")) {
        into("schema")
    }
}

val ffiSchemaConformance = tasks.register<JavaExec>("ffiSchemaConformance") {
    group = "verification"
    description = "Validates and emits the canonical FFI schema conformance fixture."
    dependsOn(tasks.named("classes"))
    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("org.glavo.himari.tools.ffi.schema.FfiSchemaConformance")
    args(
        layout.projectDirectory.file("src/test/resources/ffi-minimum-schema-v1.json").asFile.absolutePath,
        conformanceDirectory.get().asFile.absolutePath,
    )
    inputs.file(layout.projectDirectory.file("src/test/resources/ffi-minimum-schema-v1.json"))
    outputs.files(
        conformanceDirectory.map { it.file("schema.json") },
        conformanceDirectory.map { it.file("report.json") },
    )
}

tasks.named("check") {
    dependsOn(ffiSchemaConformance)
}
