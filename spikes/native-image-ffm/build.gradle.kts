import org.gradle.api.GradleException
import org.gradle.api.tasks.SourceSetContainer

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.TreeMap

dependencies {
    implementation(project(":modules:ffi"))
    implementation(project(":spikes:d3d12"))
    implementation(project(":spikes:ffi-ffm"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val isWindowsX64Host = System.getProperty("os.name", "").startsWith("Windows")
        && System.getProperty("os.arch", "") in setOf("amd64", "x86_64")
val nativeImageHome = providers.gradleProperty("nativeImageHome")
    .orElse(providers.environmentVariable("GRAALVM_HOME"))
val nativeImageExecutable = nativeImageHome.map { home ->
    val root = file(home)
    val candidates = if (isWindowsX64Host) {
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
val nativeImageDirectory = layout.buildDirectory.dir("native-image")
val nativeImageBase = nativeImageDirectory.map { it.file("himari-native-image-ffm") }
val nativeImageBinary = nativeImageDirectory.map {
    it.file(if (isWindowsX64Host) "himari-native-image-ffm.exe" else "himari-native-image-ffm")
}
val nativeImageArgumentFile = nativeImageDirectory.map { it.file("native-image.args") }
val formalEvidenceDirectory = rootProject.layout.buildDirectory.dir("conformance/m0-native-image-ffm")
val buildEvidence = formalEvidenceDirectory.map { it.file("build.json") }
val metadataFiles = listOf(
    project(":spikes:ffi-ffm").layout.buildDirectory.file(
        "generated/resources/native-image/main/META-INF/native-image/" +
                "org.glavo.himari/himari-ffi-ffm/reachability-metadata.json"
    ),
    project(":spikes:win32").layout.buildDirectory.file(
        "generated/resources/native-image/main/META-INF/native-image/" +
                "org.glavo.himari/himari-win32/reachability-metadata.json"
    ),
    project(":spikes:d3d12").layout.buildDirectory.file(
        "generated/resources/native-image/main/META-INF/native-image/" +
                "org.glavo.himari/himari-d3d12/reachability-metadata.json"
    ),
)

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

fun jsonString(value: String): String = buildString(value.length + 2) {
    append('"')
    value.forEach { character ->
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }
    append('"')
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

val nativeImage = tasks.register<Exec>("nativeImage") {
    group = "build"
    description = "Builds the shared-FFM Windows surface conformance executable with GraalVM Native Image."
    dependsOn(tasks.named("classes"))
    dependsOn(
        ":spikes:ffi-ffm:jar",
        ":spikes:win32:jar",
        ":spikes:d3d12:jar",
    )
    onlyIf("requires a Windows x64 host") { isWindowsX64Host }
    inputs.files(runtimeClasspath)
    inputs.files(metadataFiles)
    inputs.property("nativeImageHome", nativeImageHome)
    outputs.file(nativeImageBinary)
    doFirst {
        if (!nativeImageHome.isPresent) {
            throw GradleException("Set GRAALVM_HOME or -PnativeImageHome to a GraalVM 25 installation")
        }
        Files.createDirectories(nativeImageDirectory.get().asFile.toPath())
        val nativeArguments = listOf(
            "--no-fallback",
            "--exact-reachability-metadata",
            "--enable-native-access=ALL-UNNAMED",
            "-H:+ReportExceptionStackTraces",
            "-march=compatibility",
            "-cp",
            runtimeClasspath.get().asPath,
            "-o",
            nativeImageBase.get().asFile.absolutePath,
            "org.glavo.himari.spikes.nativeimage.ffm.NativeImageFfmConformance",
        )
        val argumentFile = nativeImageArgumentFile.get().asFile
        Files.writeString(
            argumentFile.toPath(),
            nativeArguments.joinToString("\n", postfix = "\n"),
            StandardCharsets.UTF_8,
        )
        if (isWindowsX64Host) {
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

val nativeImageBuildEvidence = tasks.register("nativeImageBuildEvidence") {
    group = "verification"
    description = "Records the Native Image builder, generated metadata, and produced executable digest."
    dependsOn(nativeImage)
    onlyIf("requires a Windows x64 host") { isWindowsX64Host }
    inputs.file(nativeImageBinary)
    inputs.files(metadataFiles)
    outputs.file(buildEvidence)
    doLast {
        val executable = nativeImageExecutable.get()
        val versionProcess = ProcessBuilder(executable.absolutePath, "--version")
            .redirectErrorStream(true)
            .start()
        val version = versionProcess.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }.trim()
        if (versionProcess.waitFor() != 0) {
            throw GradleException("Cannot query Native Image version: $version")
        }
        val toolchainBuilder = ProcessBuilder(
            "cmd.exe",
            "/d",
            "/v:on",
            "/s",
            "/c",
            "call \"${windowsVcvars().absolutePath}\" >nul " +
                    "&& echo VSCMD_VER=!VSCMD_VER! " +
                    "&& echo VCToolsVersion=!VCToolsVersion! " +
                    "&& echo WindowsSDKVersion=!WindowsSDKVersion!",
        ).redirectErrorStream(true)
        toolchainBuilder.environment().clear()
        toolchainBuilder.environment().putAll(cleanWindowsEnvironment())
        val toolchainProcess = toolchainBuilder.start()
        val toolchain = toolchainProcess.inputStream.bufferedReader().use { it.readText() }.trim()
        if (toolchainProcess.waitFor() != 0) {
            throw GradleException("Cannot query the Visual Studio toolchain: $toolchain")
        }
        val binary = nativeImageBinary.get().asFile.toPath()
        val metadata = metadataFiles.map { it.get().asFile.toPath() }
        val output = buildEvidence.get().asFile.toPath()
        Files.createDirectories(output.parent)
        Files.writeString(
            output,
            """
            {
              "profileId": "m0-native-image-ffm",
              "profileVersion": 1,
              "target": {"operatingSystem": "windows", "architecture": "x86_64", "runtime": "native-image"},
              "builder": {
                "nativeImageVersionOutput": ${jsonString(version)},
                "gradleVersion": ${jsonString(gradle.gradleVersion)},
                "buildJvm": ${jsonString(System.getProperty("java.runtime.version"))},
                "windowsToolchain": ${jsonString(toolchain)}
              },
              "host": {
                "osName": ${jsonString(System.getProperty("os.name"))},
                "osVersion": ${jsonString(System.getProperty("os.version"))},
                "osArchitecture": ${jsonString(System.getProperty("os.arch"))}
              },
              "command": [
                "native-image",
                "--no-fallback",
                "--exact-reachability-metadata",
                "--enable-native-access=ALL-UNNAMED",
                "-H:+ReportExceptionStackTraces",
                "-march=compatibility",
                "-cp",
                "<Gradle runtimeClasspath>",
                "-o",
                "<module-build>/native-image/himari-native-image-ffm",
                "org.glavo.himari.spikes.nativeimage.ffm.NativeImageFfmConformance"
              ],
              "metadata": [
                {"artifact": "himari-ffi-ffm", "sha256": "${sha256(metadata[0])}"},
                {"artifact": "himari-win32", "sha256": "${sha256(metadata[1])}"},
                {"artifact": "himari-d3d12", "sha256": "${sha256(metadata[2])}"}
              ],
              "binary": {
                "path": "spikes/native-image-ffm/build/native-image/${binary.fileName}",
                "sha256": "${sha256(binary)}",
                "sizeBytes": ${Files.size(binary)}
              },
              "sameGeneratedFfmSourcesAsJvm": true,
              "svmSpecificSystemCallBackendPresent": false
            }
            """.trimIndent() + "\n",
            StandardCharsets.UTF_8,
        )
    }
}

tasks.register<Exec>("conformance") {
    group = "verification"
    description = "Runs the complete M0 Native Image shared-FFM and Windows platform-clear profile."
    dependsOn(nativeImageBuildEvidence)
    onlyIf("requires a Windows x64 host") { isWindowsX64Host }
    workingDir = rootProject.projectDir
    inputs.file(rootProject.layout.projectDirectory.file("gradle/native-load-system-allowlist.txt"))
    outputs.files(
        formalEvidenceDirectory.map { it.file("results.json") },
        formalEvidenceDirectory.map { it.file("native-load.log") },
        buildEvidence,
    )
    doFirst {
        executable(nativeImageBinary.get().asFile)
        setArgs(listOf(
            formalEvidenceDirectory.get().asFile.absolutePath,
            "5",
            "120",
            rootProject.layout.projectDirectory.file("gradle/native-load-system-allowlist.txt").asFile.absolutePath,
        ))
    }
}
