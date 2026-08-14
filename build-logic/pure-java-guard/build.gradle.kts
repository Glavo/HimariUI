plugins {
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

gradlePlugin {
    plugins {
        create("pureJavaGuard") {
            id = "org.glavo.himari.pure-java-guard"
            implementationClass = "org.glavo.himari.buildlogic.PureJavaGuardPlugin"
            displayName = "HimariUI pure-Java guard"
            description = "Enforces the staged pure-Java runtime and distribution constraints."
        }
        create("repositoryGovernance") {
            id = "org.glavo.himari.repository-governance"
            implementationClass = "org.glavo.himari.buildlogic.RepositoryGovernancePlugin"
            displayName = "HimariUI repository governance"
            description = "Validates canonical ADRs, references, provenance, and conformance profiles."
        }
    }
}
