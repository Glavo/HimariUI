import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

dependencies {
    implementation(project(":modules:rhi-vulkan"))
}

val sourceSets = extensions.getByType<SourceSetContainer>()
val output = rootProject.layout.buildDirectory.dir("conformance/m0-vulkan-surface")

tasks.register<JavaExec>("conformance") {
    group = "verification"
    description = "Probes the production Vulkan backend."
    dependsOn("pureJavaGuard")
    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("org.glavo.himari.spikes.vulkan.VulkanSpike")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    args(output.get().asFile.absolutePath)
    outputs.dir(output)
}
