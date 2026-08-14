import org.gradle.api.artifacts.dsl.LockMode
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    base
    id("org.glavo.himari.pure-java-guard") apply false
    id("org.glavo.himari.repository-governance")
}

group = "org.glavo.himari"
version = "0.1.0-SNAPSHOT"

val junitBom = libs.junit.bom
val junitJupiter = libs.junit.jupiter
val junitLauncher = libs.junit.launcher
val productionModules = listOf(
    project(":modules:platform-api"),
    project(":modules:runtime"),
    project(":modules:state"),
)

configure(productionModules) {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "org.glavo.himari.pure-java-guard")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
        modularity.inferModulePath.set(true)
        withJavadocJar()
        withSourcesJar()
    }

    base {
        archivesName.set("himari-${project.name}")
    }

    dependencies {
        add("testImplementation", platform(junitBom))
        add("testImplementation", junitJupiter)
        add("testRuntimeOnly", junitLauncher)
    }

    dependencyLocking {
        lockMode.set(LockMode.STRICT)
        lockAllConfigurations()
    }

    configurations.configureEach {
        resolutionStrategy.failOnVersionConflict()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(25)
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).apply {
            encoding = "UTF-8"
            charSet = "UTF-8"
            addBooleanOption("Xdoclint:all", true)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        systemProperty("file.encoding", "UTF-8")
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                pom {
                    name.set("HimariUI ${project.name}")
                    description.set("A module of the HimariUI pure-Java GUI framework.")
                    url.set("https://github.com/Glavo/HimariUI")
                    licenses {
                        license {
                            name.set("Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }
                }
            }
        }
    }

    tasks.register("resolveAndLockAll") {
        group = "build setup"
        description = "Resolves every resolvable configuration and writes dependency locks."

        doFirst {
            require(gradle.startParameter.isWriteDependencyLocks) {
                "resolveAndLockAll must be run with --write-locks"
            }
        }

        doLast {
            configurations
                .filter { it.isCanBeResolved }
                .forEach { it.resolve() }
        }
    }
}

tasks.named("check") {
    dependsOn(productionModules.map { "${it.path}:check" })
}

tasks.register("pureJavaGuard") {
    group = "verification"
    description = "Runs the active pure-Java distribution gates for every production module."
    dependsOn(productionModules.map { "${it.path}:pureJavaGuard" })
}

tasks.register("resolveAndLockAll") {
    group = "build setup"
    description = "Writes dependency locks for every module."
    dependsOn(productionModules.map { "${it.path}:resolveAndLockAll" })
}
