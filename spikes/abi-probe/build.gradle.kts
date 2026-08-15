import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

val ffiSchemaGenerator = configurations.create("ffiSchemaGenerator") {
    isCanBeConsumed = false
    isCanBeResolved = true
    description = "Canonical ABI schema generator runtime."
}

dependencies {
    implementation(project(":modules:ffi"))
    implementation(project(":tools:ffi-schema"))
    add(ffiSchemaGenerator.name, project(":tools:ffi-schema"))
}

val schemaFile = layout.projectDirectory.file("src/main/abi/abi-probe-schema-v1.json")
val probeSource = layout.projectDirectory.file("src/main/probe/abi_probe.c")
val protocolSchema = rootProject.layout.projectDirectory.file("schema/abi-probe.schema.json")
val generatedSourceDirectory = layout.buildDirectory.dir("generated/sources/ffi/main")
val sourceSets = extensions.getByType<SourceSetContainer>()

val generateFfmBindings = tasks.register<JavaExec>("generateFfmBindings") {
    group = "build"
    description = "Generates Java layouts for the canonical ABI probe fixture."
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

sourceSets.named("main") {
    java.srcDir(generateFfmBindings)
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(generateFfmBindings)
}

pureJavaGuardConfig {
    ffmBoundary.set(true)
    nativeAccess.set(true)
    nativeLoadTrace.set(layout.buildDirectory.file("conformance/m0-abi-probe-smoke/native-load.log"))
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

fun registerAbiProbeRun(
    name: String,
    descriptionText: String,
    evidenceDirectory: Provider<Directory>,
    logPathFromProject: String,
): TaskProvider<JavaExec> = tasks.register<JavaExec>(name) {
    group = "verification"
    description = descriptionText
    dependsOn(tasks.named("classes"))
    classpath = sourceSets.named("main").get().runtimeClasspath
    mainModule.set("org.glavo.himari.spikes.abi.probe")
    mainClass.set("org.glavo.himari.spikes.abi.probe.AbiProbeConformance")
    systemProperty("himari.workspace", rootProject.projectDir.absolutePath)
    workingDir = project.projectDir
    jvmArgs(
        "--enable-native-access=org.glavo.himari.spikes.abi.probe",
        "-Xlog:library=trace:file=$logPathFromProject:uptime,level,tags:filecount=0",
    )
    inputs.files(schemaFile, probeSource, protocolSchema)
    outputs.files(
        evidenceDirectory.map { it.file("probe.json") },
        evidenceDirectory.map { it.file("comparison.json") },
        evidenceDirectory.map { it.file("native-load.log") },
    )
    doFirst {
        setArgs(listOf(
            probeSource.asFile.absolutePath,
            schemaFile.asFile.absolutePath,
            evidenceDirectory.get().asFile.absolutePath,
        ))
    }
}

val abiProbeSmoke = registerAbiProbeRun(
    name = "abiProbeSmoke",
    descriptionText = "Compiles and compares the short local native ABI probe.",
    evidenceDirectory = layout.buildDirectory.dir("conformance/m0-abi-probe-smoke"),
    logPathFromProject = "build/conformance/m0-abi-probe-smoke/native-load.log",
)

registerAbiProbeRun(
    name = "conformance",
    descriptionText = "Compiles and compares the complete M0 native ABI probe profile.",
    evidenceDirectory = rootProject.layout.buildDirectory.dir("conformance/m0-abi-probe"),
    logPathFromProject = "../../build/conformance/m0-abi-probe/native-load.log",
)

tasks.named("verifyNativeLoadTrace") {
    dependsOn(abiProbeSmoke)
}

tasks.named("check") {
    dependsOn(abiProbeSmoke)
}
