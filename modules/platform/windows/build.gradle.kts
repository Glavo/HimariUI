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
    api(project(":modules:platform-api"))
    api(project(":modules:layout"))
    implementation(project(":modules:ffi"))
    add(ffiSchemaGenerator.name, project(":tools:ffi-schema"))
}

val isWindowsHost = System.getProperty("os.name", "").startsWith("Windows")
val schemaFile = layout.projectDirectory.file("src/main/abi/win32-schema-v1.json")
val generatedSourceDirectory = layout.buildDirectory.dir("generated/sources/ffi/main")
val generatedMetadataDirectory = layout.buildDirectory.dir("generated/resources/native-image/main")
val generatedMetadataFile = generatedMetadataDirectory.map {
    it.file("META-INF/native-image/org.glavo.himari/himari-platform-windows/reachability-metadata.json")
}
val sourceSets = extensions.getByType<SourceSetContainer>()
val conformanceDirectory = rootProject.layout.buildDirectory.dir("conformance/m6-windows")

val generateFfmBindings = tasks.register<JavaExec>("generateFfmBindings") {
    group = "build"
    description = "Generates production Win32 and DXGI FFM bindings."
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
    description = "Generates Native Image metadata for Windows platform bindings."
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

val smokeDirectory = layout.buildDirectory.dir("conformance/m6-windows-smoke")
val smokeLog = "build/conformance/m6-windows-smoke/native-load.log"

pureJavaGuardConfig {
    ffmBoundary.set(true)
    nativeAccess.set(true)
    nativeLoadTrace.set(layout.buildDirectory.file("conformance/m6-windows-smoke/native-load.log"))
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

val windowsSmoke = tasks.register<JavaExec>("windowsSmoke") {
    group = "verification"
    description = "Captures the Windows native-load allowlist trace."
    dependsOn("testClasses")
    onlyIf("requires Windows") { isWindowsHost }
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.platform.windows.WindowsPlatformConformance")
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
        dependsOn(windowsSmoke)
    } else {
        onlyIf("requires a Windows host") { false }
    }
}

val conformance = tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Runs the production Windows platform profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    onlyIf("requires Windows") { isWindowsHost }
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.platform.windows.WindowsPlatformConformance")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    args(conformanceDirectory.get().asFile.absolutePath)
    outputs.file(conformanceDirectory.map { it.file("results.json") })
}

val imeA11yDirectory = rootProject.layout.buildDirectory.dir("conformance/m9-windows-ime-a11y")
val imeA11yConformance = tasks.register<JavaExec>("imeA11yConformance") {
    group = "verification"
    description = "Runs the production Windows IME and UIA projection profile."
    dependsOn("testClasses", "test", "pureJavaGuard")
    onlyIf("requires Windows") { isWindowsHost }
    classpath = sourceSets.named("test").get().runtimeClasspath
    mainClass.set("org.glavo.himari.platform.windows.WindowsImeA11yConformance")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    args(imeA11yDirectory.get().asFile.absolutePath)
    outputs.file(imeA11yDirectory.map { it.file("results.json") })
}

tasks.named("check") {
    if (isWindowsHost) {
        dependsOn(conformance, imeA11yConformance)
    }
}
