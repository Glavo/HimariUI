import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources

val ffiSchemaGenerator = configurations.create("ffiSchemaGenerator") {
    isCanBeConsumed = false
    isCanBeResolved = true
    description = "Canonical ABI schema generator runtime."
}

dependencies {
    implementation(project(":modules:ffi"))
    add(ffiSchemaGenerator.name, project(":tools:ffi-schema"))
}

val schemaFile = layout.projectDirectory.file("src/main/abi/metal-schema-v1.json")
val generatedSourceDirectory = layout.buildDirectory.dir("generated/sources/ffi/main")
val generatedMetadataDirectory = layout.buildDirectory.dir("generated/resources/native-image/main")
val generatedMetadataFile = generatedMetadataDirectory.map {
    it.file("META-INF/native-image/org.glavo.himari/himari-rhi-metal/reachability-metadata.json")
}
val sourceSets = extensions.getByType<SourceSetContainer>()
val conformanceDirectory = rootProject.layout.buildDirectory.dir("conformance/m7-metal")
val smokeDirectory = layout.buildDirectory.dir("conformance/m7-metal-smoke")
val smokeLog = "build/conformance/m7-metal-smoke/native-load.log"

val generateFfmBindings = tasks.register<JavaExec>("generateFfmBindings") {
    group = "build"
    description = "Generates production Metal FFM bindings."
    classpath = ffiSchemaGenerator
    mainClass.set("org.glavo.himari.tools.ffi.schema.FfmGeneratorCli")
    inputs.file(schemaFile)
    outputs.dir(generatedSourceDirectory)
    doFirst {
        setArgs(listOf(schemaFile.asFile.absolutePath, generatedSourceDirectory.get().asFile.absolutePath))
    }
}

val generateNativeImageMetadata = tasks.register<JavaExec>("generateNativeImageMetadata") {
    group = "build"
    description = "Generates Native Image metadata for Metal bindings."
    classpath = ffiSchemaGenerator
    mainClass.set("org.glavo.himari.tools.ffi.schema.NativeImageMetadataGeneratorCli")
    inputs.file(schemaFile)
    outputs.file(generatedMetadataFile)
    doFirst {
        setArgs(listOf(schemaFile.asFile.absolutePath, generatedMetadataFile.get().asFile.absolutePath))
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
    nativeLoadTrace.set(layout.buildDirectory.file("conformance/m7-metal-smoke/native-load.log"))
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

val metalSmoke = tasks.register<JavaExec>("metalSmoke") {
    group = "verification"
    description = "Captures the Metal native-load allowlist trace or an environment block."
    dependsOn("testClasses")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.rhi.metal.MetalConformance")
    workingDir = project.projectDir
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "-Xlog:library=trace:file=$smokeLog:uptime,level,tags:filecount=0",
    )
    args(smokeDirectory.get().asFile.absolutePath)
    outputs.file(smokeDirectory.map { it.file("native-load.log") })
}

tasks.named("verifyNativeLoadTrace") {
    dependsOn(metalSmoke)
}

val conformance = tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the production Metal device profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.rhi.metal.MetalConformance")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    args(conformanceDirectory.get().asFile.absolutePath)
    outputs.file(conformanceDirectory.map { it.file("results.json") })
}

tasks.named("check") {
    dependsOn(conformance)
}
