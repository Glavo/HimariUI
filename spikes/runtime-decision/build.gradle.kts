import org.gradle.api.GradleException
import org.gradle.api.tasks.SourceSetContainer

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.TreeMap

dependencies {
    implementation(project(":spikes:runtime-grouped"))
    implementation(project(":spikes:runtime-hybrid"))
    implementation(project(":spikes:runtime-oneshot"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val isWindowsHost = System.getProperty("os.name", "").startsWith("Windows")
val nativeImageHome = providers.gradleProperty("nativeImageHome")
    .orElse(providers.environmentVariable("GRAALVM_HOME"))
val nativeImageExecutable = nativeImageHome.map { home ->
    val root = file(home)
    val candidates = if (isWindowsHost) {
        listOf(
            root.resolve("lib/svm/bin/native-image.exe"),
            root.resolve("bin/native-image.cmd"),
        )
    } else {
        listOf(
            root.resolve("lib/svm/bin/native-image"),
            root.resolve("bin/native-image"),
        )
    }
    candidates.firstOrNull(File::isFile)
        ?: throw GradleException("Cannot find Native Image under ${root.absolutePath}")
}
val nativeImageCommand = nativeImageHome.map { home -> file(home).resolve("bin/native-image.cmd") }
val runtimeClasspath = sourceSets.named("main").map { it.runtimeClasspath }
val decisionDirectory = rootProject.layout.buildDirectory.dir("conformance/m1-runtime-decision")
val ceremonyDirectory = decisionDirectory.map { it.dir("ceremony") }
val ceremonyManifest = ceremonyDirectory.map { it.file("manifest.properties") }
val checkedCeremonyReview = rootProject.layout.projectDirectory.file(
    "evidence/m1-runtime-decision/ceremony-review.properties"
)
val checkedReviewedDecision = rootProject.layout.projectDirectory.file(
    "evidence/m1-runtime-decision/reviewed-decision.properties"
)
val nativeBuildDirectory = layout.buildDirectory.dir("native-image")
val nativeImageBase = nativeBuildDirectory.map { it.file("himari-runtime-decision") }
val nativeImageBinary = nativeBuildDirectory.map {
    it.file(if (isWindowsHost) "himari-runtime-decision.exe" else "himari-runtime-decision")
}
val nativeImageArgumentFile = nativeBuildDirectory.map { it.file("native-image.args") }
val nativeEvidenceDirectory = decisionDirectory.map { it.dir("native-image") }
val nativeResults = nativeEvidenceDirectory.map { it.file("results.properties") }
val nativeBuildEvidence = nativeEvidenceDirectory.map { it.file("build.properties") }

fun sha256(path: Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) {
                break
            }
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun escapeProperty(value: String): String = buildString(value.length) {
    value.forEachIndexed { index, character ->
        when (character) {
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '=' -> append("\\=")
            ' ' -> {
                if (index == 0) {
                    append('\\')
                }
                append(' ')
            }
            else -> append(character)
        }
    }
}

fun writeProperties(path: Path, values: Map<String, String>) {
    Files.createDirectories(path.parent)
    Files.writeString(
        path,
        values.toSortedMap().entries.joinToString(
            separator = "\n",
            postfix = "\n",
        ) { (key, value) -> "${escapeProperty(key)}=${escapeProperty(value)}" },
        StandardCharsets.UTF_8,
    )
}

fun windowsVcvars(): File {
    val environmentInstallation = System.getenv("VSINSTALLDIR")?.let(::file)
    val vswhere = file("${System.getenv("ProgramFiles(x86)")}\\Microsoft Visual Studio\\Installer\\vswhere.exe")
    val installation = environmentInstallation?.takeIf(File::isDirectory) ?: run {
        if (!vswhere.isFile) {
            throw GradleException("Cannot find vswhere.exe or VSINSTALLDIR for the Native Image C toolchain")
        }
        val process = ProcessBuilder(
            vswhere.absolutePath,
            "-latest",
            "-products",
            "*",
            "-requires",
            "Microsoft.VisualStudio.Component.VC.Tools.x86.x64",
            "-property",
            "installationPath",
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        if (process.waitFor() != 0 || output.isBlank()) {
            throw GradleException("Cannot locate a Visual Studio x64 C toolchain: $output")
        }
        file(output.lineSequence().first())
    }
    val vcvars = installation.resolve("VC/Auxiliary/Build/vcvars64.bat")
    if (!vcvars.isFile) {
        throw GradleException("Cannot find ${vcvars.absolutePath}")
    }
    return vcvars
}

fun cleanWindowsEnvironment(): Map<String, String> {
    val result = TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
    System.getenv().entries
        .filterNot { (name, _) -> name.equals("Path", ignoreCase = true) }
        .forEach { (name, value) -> result[name] = value }
    result["Path"] = System.getenv().entries.asSequence()
        .filter { (name, _) -> name.equals("Path", ignoreCase = true) }
        .map { (_, value) -> value }
        .maxByOrNull(String::length)
        ?: throw GradleException("The host environment has no PATH")
    return result
}

val ceremonyPackets = tasks.register<JavaExec>("ceremonyPackets") {
    group = "verification"
    description = "Regenerates the name-free complete-micro-source ceremony review packets."
    dependsOn("classes")
    classpath = runtimeClasspath.get()
    mainClass.set("org.glavo.himari.spikes.runtime.decision.CeremonyPacketGenerator")
    args(rootProject.layout.projectDirectory.asFile.absolutePath, ceremonyDirectory.get().asFile.absolutePath)
    inputs.files(
        project(":spikes:runtime-grouped").fileTree("src/main/java"),
        project(":spikes:runtime-oneshot").fileTree("src/main/java"),
        project(":spikes:runtime-hybrid").fileTree("src/main/java"),
    )
    outputs.files(
        ceremonyDirectory.map { it.file("candidate-a.txt") },
        ceremonyDirectory.map { it.file("candidate-b.txt") },
        ceremonyDirectory.map { it.file("candidate-c.txt") },
        ceremonyManifest,
    )
}

val nativeImage = tasks.register<Exec>("nativeImage") {
    group = "build"
    description = "Builds the three-candidate M1 decision executable with GraalVM Native Image."
    dependsOn("classes")
    inputs.files(runtimeClasspath)
    inputs.property("nativeImageHome", nativeImageHome)
    outputs.file(nativeImageBinary)
    doFirst {
        if (!nativeImageHome.isPresent) {
            throw GradleException("Set GRAALVM_HOME or -PnativeImageHome to a GraalVM 25 installation")
        }
        Files.createDirectories(nativeBuildDirectory.get().asFile.toPath())
        val nativeArguments = listOf(
            "--no-fallback",
            "--exact-reachability-metadata",
            "-H:+ReportExceptionStackTraces",
            "-march=compatibility",
            "-cp",
            runtimeClasspath.get().asPath,
            "-o",
            nativeImageBase.get().asFile.absolutePath,
            "org.glavo.himari.spikes.runtime.decision.NativeImageDecisionConformance",
        )
        val argumentFile = nativeImageArgumentFile.get().asFile
        Files.writeString(
            argumentFile.toPath(),
            nativeArguments.joinToString("\n", postfix = "\n"),
            StandardCharsets.UTF_8,
        )
        if (isWindowsHost) {
            val command = nativeImageCommand.get()
            if (!command.isFile) {
                throw GradleException("Cannot find ${command.absolutePath}")
            }
            executable("cmd.exe")
            setEnvironment(cleanWindowsEnvironment())
            setArgs(listOf(
                "/d",
                "/s",
                "/c",
                "call \"${windowsVcvars().absolutePath}\" >nul " +
                    "&& call \"${command.absolutePath}\" @\"${argumentFile.absolutePath}\"",
            ))
        } else {
            executable(nativeImageExecutable.get())
            setArgs(listOf("@${argumentFile.absolutePath}"))
        }
    }
}

val recordNativeImageBuild = tasks.register("recordNativeImageBuild") {
    group = "verification"
    description = "Records the Native Image builder and executable digest for the M1 decision."
    dependsOn(nativeImage)
    inputs.file(nativeImageBinary)
    outputs.file(nativeBuildEvidence)
    doLast {
        val executable = nativeImageExecutable.get()
        val process = ProcessBuilder(executable.absolutePath, "--version")
            .redirectErrorStream(true)
            .start()
        val version = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }.trim()
        if (process.waitFor() != 0) {
            throw GradleException("Cannot query Native Image version: $version")
        }
        val binary = nativeImageBinary.get().asFile.toPath()
        writeProperties(
            nativeBuildEvidence.get().asFile.toPath(),
            mapOf(
                "schemaVersion" to "1",
                "profileId" to "m1-runtime-decision",
                "nativeImageVersionOutput" to version,
                "gradleVersion" to gradle.gradleVersion,
                "buildJvm" to System.getProperty("java.runtime.version"),
                "osName" to System.getProperty("os.name"),
                "osArchitecture" to System.getProperty("os.arch"),
                "binaryPath" to rootProject.projectDir.toPath().relativize(binary).toString().replace('\\', '/'),
                "binarySha256" to sha256(binary),
                "binarySizeBytes" to Files.size(binary).toString(),
            ),
        )
    }
}

val nativeImageConformance = tasks.register<Exec>("nativeImageConformance") {
    group = "verification"
    description = "Runs all three real M1 candidates inside the Native Image decision executable."
    dependsOn(recordNativeImageBuild)
    workingDir = rootProject.projectDir
    outputs.files(
        nativeResults,
        nativeEvidenceDirectory.map { it.file("grouped-report.json") },
        nativeEvidenceDirectory.map { it.file("oneshot-report.json") },
        nativeEvidenceDirectory.map { it.file("hybrid-report.json") },
    )
    doFirst {
        executable(nativeImageBinary.get().asFile)
        setArgs(listOf(
            rootProject.layout.projectDirectory.asFile.absolutePath,
            nativeEvidenceDirectory.get().asFile.absolutePath,
        ))
    }
}

fun registerCandidateReport(
    taskName: String,
    candidateKey: String,
    configureTask: JavaExec.() -> Unit = {},
) = tasks.register<JavaExec>(taskName) {
    group = "verification"
    description = "Produces the evidence-backed $candidateKey JVM decision report in an isolated process."
    dependsOn("classes", ceremonyPackets, nativeImageConformance)
    classpath = runtimeClasspath.get()
    mainClass.set("org.glavo.himari.spikes.runtime.decision.DecisionCandidateConformance")
    val output = decisionDirectory.map { it.dir("jvm-$candidateKey") }
    args(
        candidateKey,
        rootProject.layout.projectDirectory.asFile.absolutePath,
        output.get().asFile.absolutePath,
        checkedCeremonyReview.asFile.absolutePath,
        ceremonyManifest.get().asFile.absolutePath,
        nativeResults.get().asFile.absolutePath,
    )
    inputs.files(checkedCeremonyReview, ceremonyManifest, nativeResults)
    outputs.files(
        output.map { it.file("report.json") },
        output.map { it.file("measurements.properties") },
    )
    configureTask()
}

val groupedDecisionReport = registerCandidateReport("groupedDecisionReport", "grouped")
val oneShotDecisionReport = registerCandidateReport("oneShotDecisionReport", "oneshot") {
    mustRunAfter(groupedDecisionReport)
}
val hybridDecisionReport = registerCandidateReport("hybridDecisionReport", "hybrid") {
    mustRunAfter(oneShotDecisionReport)
}

tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Reproduces and verifies the reviewed RUNTIME-ADR-001 selection."
    dependsOn(groupedDecisionReport, oneShotDecisionReport, hybridDecisionReport)
    classpath = runtimeClasspath.get()
    mainClass.set("org.glavo.himari.spikes.runtime.decision.RuntimeDecisionConformance")
    val groupedMeasurements = decisionDirectory.map { it.file("jvm-grouped/measurements.properties") }
    val oneShotMeasurements = decisionDirectory.map { it.file("jvm-oneshot/measurements.properties") }
    val hybridMeasurements = decisionDirectory.map { it.file("jvm-hybrid/measurements.properties") }
    args(
        decisionDirectory.get().asFile.absolutePath,
        checkedReviewedDecision.asFile.absolutePath,
        groupedMeasurements.get().asFile.absolutePath,
        oneShotMeasurements.get().asFile.absolutePath,
        hybridMeasurements.get().asFile.absolutePath,
    )
    inputs.files(
        checkedReviewedDecision,
        groupedMeasurements,
        oneShotMeasurements,
        hybridMeasurements,
    )
    outputs.files(
        decisionDirectory.map { it.file("decision.json") },
        decisionDirectory.map { it.file("decision.md") },
        decisionDirectory.map { it.file("selection.properties") },
    )
}

tasks.test {
    systemProperty("himari.repository.root", rootProject.layout.projectDirectory.asFile.absolutePath)
}
