plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.3.0"
}

group = "ac.apex"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    implementation("com.github.retrooper:packetevents-spigot:2.13.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("com.github.retrooper.packetevents", "ac.apex.libs.packetevents")
        relocate("io.github.retrooper.packetevents", "ac.apex.libs.io.packetevents")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
