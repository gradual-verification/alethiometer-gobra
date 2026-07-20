plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.12.0"
}

group = "edu.cmu"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.2.8")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("223")
        untilBuild.set("261.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

// Increase the JVM stack size because the Silicon verifier is written using
// continuation-passing style
tasks.withType<JavaExec>().configureEach {
    jvmArgs( "-Xss16m", "--enable-preview")
}

configurations.implementation {
    exclude(group="org.slf4j", module="slf4j-log4j12")
}

dependencies {
    implementation(files("../gobra/target/scala-2.13/gobra.jar"))
}
