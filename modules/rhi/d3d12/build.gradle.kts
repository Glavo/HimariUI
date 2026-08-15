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
    testImplementation(project(":modules:graphics"))
    testImplementation(project(":modules:platform-windows"))
    testImplementation(project(":modules:render-software"))
    add(ffiSchemaGenerator.name, project(":tools:ffi-schema"))
}

val isWindowsHost = System.getProperty("os.name", "").startsWith("Windows")
val schemaFile = layout.projectDirectory.file("src/main/abi/d3d12-schema-v1.json")
val generatedSourceDirectory = layout.buildDirectory.dir("generated/sources/ffi/main")
val generatedMetadataDirectory = layout.buildDirectory.dir("generated/resources/native-image/main")
val generatedMetadataFile = generatedMetadataDirectory.map {
    it.file("META-INF/native-image/org.glavo.himari/himari-rhi-d3d12/reachability-metadata.json")
}
val sourceSets = extensions.getByType<SourceSetContainer>()
val conformanceDirectory = rootProject.layout.buildDirectory.dir("conformance/m6-d3d12")

val generateFfmBindings = tasks.register<JavaExec>("generateFfmBindings") {
    group = "build"
    description = "Generates production D3D12 and DXGI FFM bindings."
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
    description = "Generates Native Image metadata for D3D12 bindings."
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

val smokeDirectory = layout.buildDirectory.dir("conformance/m6-d3d12-smoke")
val smokeLog = "build/conformance/m6-d3d12-smoke/native-load.log"

pureJavaGuardConfig {
    ffmBoundary.set(true)
    nativeAccess.set(true)
    nativeLoadTrace.set(layout.buildDirectory.file("conformance/m6-d3d12-smoke/native-load.log"))
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

val d3d12Smoke = tasks.register<JavaExec>("d3d12Smoke") {
    group = "verification"
    description = "Captures the D3D12 native-load allowlist trace."
    dependsOn("testClasses")
    onlyIf("requires Windows") { isWindowsHost }
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.rhi.d3d12.D3d12Conformance")
    workingDir = project.projectDir
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "-Xlog:library=trace:file=$smokeLog:uptime,level,tags:filecount=0",
    )
    args(smokeDirectory.get().asFile.absolutePath)
    outputs.file(smokeDirectory.map { it.file("native-load.log") })
}

tasks.named("verifyNativeLoadTrace") {
    if (isWindowsHost) {
        dependsOn(d3d12Smoke)
    } else {
        onlyIf("requires a Windows host") { false }
    }
}

val conformance = tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the production D3D12 device and SDR present profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    onlyIf("requires Windows") { isWindowsHost }
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.rhi.d3d12.D3d12Conformance")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    args(conformanceDirectory.get().asFile.absolutePath)
    outputs.files(
        conformanceDirectory.map { it.file("results.json") },
        conformanceDirectory.map { it.file("capabilities.json") },
        conformanceDirectory.map { it.file("presentation.json") },
        conformanceDirectory.map { it.file("gpu-diff.json") },
    )
}

tasks.named("check") {
    if (isWindowsHost) {
        dependsOn(conformance)
    }
}
