import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

dependencies {
    implementation(project(":modules:objc"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val output = rootProject.layout.buildDirectory.dir("conformance/m0-objc-block")

tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Probes the production Objective-C block ABI."
    dependsOn("pureJavaGuard")
    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("org.glavo.himari.spikes.objc.ObjcBlockSpike")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    args(output.get().asFile.absolutePath)
    outputs.dir(output)
}
