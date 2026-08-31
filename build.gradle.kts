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
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.slf4j:slf4j-api:2.0.13")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("com.github.retrooper.packetevents", "ac.apex.libs.packetevents")
        relocate("io.github.retrooper.packetevents", "ac.apex.libs.io.packetevents")
        relocate("com.zaxxer.hikari", "ac.apex.libs.hikari")
        relocate("org.sqlite", "ac.apex.libs.sqlite")
        relocate("org.slf4j", "ac.apex.libs.slf4j")
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
