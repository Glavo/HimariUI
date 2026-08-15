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

val isWindowsHost = System.getProperty("os.name", "").startsWith("Windows")
val schemaFile = layout.projectDirectory.file("src/main/abi/win32-schema-v1.json")
val generatedSourceDirectory = layout.buildDirectory.dir("generated/sources/ffi/main")
val generatedMetadataDirectory = layout.buildDirectory.dir("generated/resources/native-image/main")
val generatedMetadataFile = generatedMetadataDirectory.map {
    it.file("META-INF/native-image/org.glavo.himari/himari-win32/reachability-metadata.json")
}
val sourceSets = extensions.getByType<SourceSetContainer>()

val generateFfmBindings = tasks.register<JavaExec>("generateFfmBindings") {
    group = "build"
    description = "Generates exact Win32 and DXGI FFM bindings for the Windows feasibility spike."
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
    description = "Generates Native Image reachability metadata for the Win32 and DXGI bindings."
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
    nativeLoadTrace.set(layout.buildDirectory.file("conformance/m0-win32-window-smoke/native-load.log"))
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

fun registerWin32Run(
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
    onlyIf("requires a Windows host") { isWindowsHost }
    classpath = sourceSets.named("main").get().runtimeClasspath
    mainModule.set("org.glavo.himari.spikes.windows")
    mainClass.set("org.glavo.himari.spikes.win32.Win32Conformance")
    workingDir = project.projectDir
    jvmArgs(
        "--enable-native-access=org.glavo.himari.spikes.windows",
        "-Xlog:library=trace:file=$logPathFromProject:uptime,level,tags:filecount=0",
    )
    inputs.file(schemaFile)
    outputs.files(
        evidenceDirectory.map { it.file("events.json") },
        evidenceDirectory.map { it.file("capabilities.json") },
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

val win32Smoke = registerWin32Run(
    name = "win32Smoke",
    descriptionText = "Runs the short Win32 window and Advanced Color smoke profile.",
    evidenceDirectory = layout.buildDirectory.dir("conformance/m0-win32-window-smoke"),
    logPathFromProject = "build/conformance/m0-win32-window-smoke/native-load.log",
    repetitions = 3,
    soakSeconds = 0,
)

val conformance = registerWin32Run(
    name = "conformance",
    descriptionText = "Runs the complete M0 Win32 window and Advanced Color conformance profile.",
    evidenceDirectory = rootProject.layout.buildDirectory.dir("conformance/m0-win32-window"),
    logPathFromProject = "../../build/conformance/m0-win32-window/native-load.log",
    repetitions = 25,
    soakSeconds = 120,
)

tasks.named("verifyNativeLoadTrace") {
    if (isWindowsHost) {
        dependsOn(win32Smoke)
    } else {
        onlyIf("requires a Windows host") { false }
    }
}

tasks.named("check") {
    if (isWindowsHost) {
        dependsOn(if (providers.gradleProperty("fullWin32Conformance").isPresent) conformance else win32Smoke)
    }
}
