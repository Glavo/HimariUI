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
    implementation(project(":spikes:win32"))
    add(ffiSchemaGenerator.name, project(":tools:ffi-schema"))
}

val isWindowsX64Host = System.getProperty("os.name", "").startsWith("Windows")
        && System.getProperty("os.arch", "") in setOf("amd64", "x86_64")
val schemaFile = layout.projectDirectory.file("src/main/abi/d3d12-schema-v1.json")
val generatedSourceDirectory = layout.buildDirectory.dir("generated/sources/ffi/main")
val generatedMetadataDirectory = layout.buildDirectory.dir("generated/resources/native-image/main")
val generatedMetadataFile = generatedMetadataDirectory.map {
    it.file("META-INF/native-image/org.glavo.himari/himari-d3d12/reachability-metadata.json")
}
val sourceSets = extensions.getByType<SourceSetContainer>()

val generateFfmBindings = tasks.register<JavaExec>("generateFfmBindings") {
    group = "build"
    description = "Generates exact D3D12, DXGI, and Kernel32 FFM bindings for the Windows GPU spike."
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
    description = "Generates Native Image reachability metadata for the D3D12, DXGI, and Kernel32 bindings."
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
    nativeLoadTrace.set(layout.buildDirectory.file("conformance/m0-d3d12-surface-smoke/native-load.log"))
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

fun registerD3d12Run(
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
    onlyIf("requires a Windows x64 host") { isWindowsX64Host }
    classpath = sourceSets.named("main").get().runtimeClasspath
    mainModule.set("org.glavo.himari.spikes.d3d12")
    mainClass.set("org.glavo.himari.spikes.d3d12.D3d12Conformance")
    workingDir = project.projectDir
    jvmArgs(
        "--enable-native-access=org.glavo.himari.spikes.d3d12,org.glavo.himari.spikes.windows",
        "-Xms64m",
        "-Xmx256m",
        "-Xlog:library=trace:file=$logPathFromProject:uptime,level,tags:filecount=0",
    )
    inputs.file(schemaFile)
    outputs.files(
        evidenceDirectory.map { it.file("capabilities.json") },
        evidenceDirectory.map { it.file("debug-layer.log") },
        evidenceDirectory.map { it.file("presentation.json") },
        evidenceDirectory.map { it.file("native-load.log") },
    )
    doFirst {
        setArgs(listOf(
            evidenceDirectory.get().asFile.absolutePath,
            repetitions.toString(),
            soakSeconds.toString(),
        ))
    }
}

val d3d12Smoke = registerD3d12Run(
    name = "d3d12Smoke",
    descriptionText = "Runs the short D3D12 device, swapchain, clear, and readback smoke profile.",
    evidenceDirectory = layout.buildDirectory.dir("conformance/m0-d3d12-surface-smoke"),
    logPathFromProject = "build/conformance/m0-d3d12-surface-smoke/native-load.log",
    repetitions = 3,
    soakSeconds = 0,
)

val conformance = registerD3d12Run(
    name = "conformance",
    descriptionText = "Runs the complete M0 D3D12 device and swapchain conformance profile.",
    evidenceDirectory = rootProject.layout.buildDirectory.dir("conformance/m0-d3d12-surface"),
    logPathFromProject = "../../build/conformance/m0-d3d12-surface/native-load.log",
    repetitions = 20,
    soakSeconds = 300,
)

tasks.named("verifyNativeLoadTrace") {
    if (isWindowsX64Host) {
        dependsOn(d3d12Smoke)
    } else {
        onlyIf("requires a Windows x64 host") { false }
    }
}

tasks.named("check") {
    if (isWindowsX64Host) {
        dependsOn(if (providers.gradleProperty("fullD3d12Conformance").isPresent) conformance else d3d12Smoke)
    }
}
