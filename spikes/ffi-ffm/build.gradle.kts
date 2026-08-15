import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources

import java.nio.file.Files

plugins {
    id("org.glavo.himari.pure-java-guard")
}

val ffiSchemaGenerator = configurations.create("ffiSchemaGenerator") {
    isCanBeConsumed = false
    isCanBeResolved = true
    description = "Canonical ABI schema generator runtime."
}

dependencies {
    implementation(project(":modules:ffi"))
    add(ffiSchemaGenerator.name, project(":tools:ffi-schema"))
}

val schemaFile = layout.projectDirectory.file("src/main/abi/c-runtime-schema-v1.json")
val generatedSourceDirectory = layout.buildDirectory.dir("generated/sources/ffi/main")
val generatedMetadataDirectory = layout.buildDirectory.dir("generated/resources/native-image/main")
val generatedMetadataFile = generatedMetadataDirectory.map {
    it.file("META-INF/native-image/org.glavo.himari/himari-ffi-ffm/reachability-metadata.json")
}
val sourceSets = extensions.getByType<SourceSetContainer>()

val generateFfmBindings = tasks.register<JavaExec>("generateFfmBindings") {
    group = "build"
    description = "Generates exact Java FFM bindings for the portable C runtime fixture."
    classpath = ffiSchemaGenerator
    mainClass.set("org.glavo.himari.tools.ffi.schema.FfmGeneratorCli")
    inputs.file(schemaFile)
    outputs.dir(generatedSourceDirectory)
    doFirst {
        setArgs(listOf(
            schemaFile.asFile.absolutePath,
            generatedSourceDirectory.get().asFile.absolutePath,
        ))
    }
}

val generateNativeImageMetadata = tasks.register<JavaExec>("generateNativeImageMetadata") {
    group = "build"
    description = "Generates Native Image reachability metadata for the portable C runtime bindings."
    classpath = ffiSchemaGenerator
    mainClass.set("org.glavo.himari.tools.ffi.schema.NativeImageMetadataGeneratorCli")
    inputs.file(schemaFile)
    outputs.file(generatedMetadataFile)
    doFirst {
        setArgs(listOf(
            schemaFile.asFile.absolutePath,
            generatedMetadataFile.get().asFile.absolutePath,
        ))
    }
}

sourceSets.named("main") {
    java.srcDir(generateFfmBindings)
    resources.srcDir(generatedMetadataDirectory)
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(generateFfmBindings)
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(generateNativeImageMetadata)
}

tasks.named("sourcesJar") {
    dependsOn(generateNativeImageMetadata)
}

pureJavaGuardConfig {
    ffmBoundary.set(true)
    nativeAccess.set(true)
    nativeLoadTrace.set(layout.buildDirectory.file("conformance/m0-ffi-ffm-smoke/native-load.log"))
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

fun registerFfmRun(
    name: String,
    descriptionText: String,
    evidenceDirectory: Provider<Directory>,
    logPathFromProject: String,
    repetitions: Int,
    soakSeconds: Int,
): TaskProvider<JavaExec> = tasks.register<JavaExec>(name) {
    group = "verification"
    description = descriptionText
    dependsOn(tasks.named("classes"))
    classpath = sourceSets.named("main").get().runtimeClasspath
    mainModule.set("org.glavo.himari.spikes.ffi.ffm")
    mainClass.set("org.glavo.himari.spikes.ffi.ffm.FfiFfmConformance")
    workingDir = project.projectDir
    jvmArgs(
        "--enable-native-access=org.glavo.himari.spikes.ffi.ffm",
        "-Xlog:library=trace:file=$logPathFromProject:uptime,level,tags:filecount=0",
    )
    outputs.files(
        evidenceDirectory.map { it.file("results.json") },
        evidenceDirectory.map { it.file("native-load.log") },
    )
    doFirst {
        val directory = evidenceDirectory.get().asFile.toPath()
        Files.createDirectories(directory)
        setArgs(listOf(
            directory.resolve("results.json").toString(),
            repetitions.toString(),
            soakSeconds.toString(),
        ))
    }
}

val ffiFfmSmoke = registerFfmRun(
    name = "ffiFfmSmoke",
    descriptionText = "Runs the short local C runtime FFM smoke profile.",
    evidenceDirectory = layout.buildDirectory.dir("conformance/m0-ffi-ffm-smoke"),
    logPathFromProject = "build/conformance/m0-ffi-ffm-smoke/native-load.log",
    repetitions = 10,
    soakSeconds = 0,
)

val conformance = registerFfmRun(
    name = "conformance",
    descriptionText = "Runs the complete M0 fixed-signature FFM conformance profile.",
    evidenceDirectory = rootProject.layout.buildDirectory.dir("conformance/m0-ffi-ffm"),
    logPathFromProject = "../../build/conformance/m0-ffi-ffm/native-load.log",
    repetitions = 1_000,
    soakSeconds = 60,
)

tasks.named("verifyNativeLoadTrace") {
    dependsOn(ffiFfmSmoke)
}

tasks.named("check") {
    dependsOn(if (providers.gradleProperty("fullFfiConformance").isPresent) conformance else ffiFfmSmoke)
}
