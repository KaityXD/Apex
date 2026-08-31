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
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("com.github.retrooper.packetevents", "ac.apex.libs.packetevents")
        relocate("io.github.retrooper.packetevents", "ac.apex.libs.io.packetevents")
        relocate("org.sqlite", "ac.apex.libs.sqlite")
        exclude("ac/apex/libs/sqlite/native/Linux-Android/**")
        exclude("ac/apex/libs/sqlite/native/Mac/**")
        exclude("ac/apex/libs/sqlite/native/FreeBSD/**")
        exclude("ac/apex/libs/sqlite/native/Linux/arm/**")
        exclude("ac/apex/libs/sqlite/native/Linux/armv6/**")
        exclude("ac/apex/libs/sqlite/native/Linux/armv7/**")
        exclude("ac/apex/libs/sqlite/native/Linux/ppc64/**")
        exclude("ac/apex/libs/sqlite/native/Linux-Musl/**")
        exclude("ac/apex/libs/sqlite/native/Linux/x86/**")
        exclude("ac/apex/libs/sqlite/native/Windows/**")
        exclude("org/sqlite/native/Linux-Android/**")
        exclude("org/sqlite/native/Mac/**")
        exclude("org/sqlite/native/FreeBSD/**")
        exclude("org/sqlite/native/Linux/arm/**")
        exclude("org/sqlite/native/Linux/armv6/**")
        exclude("org/sqlite/native/Linux/armv7/**")
        exclude("org/sqlite/native/Linux/ppc64/**")
        exclude("org/sqlite/native/Linux-Musl/**")
        exclude("org/sqlite/native/Linux/x86/**")
        exclude("org/sqlite/native/Windows/**")
        minimize {
            exclude(dependency("com.github.retrooper:packetevents-spigot:.*"))
            exclude(dependency("com.github.retrooper:packetevents-api:.*"))
            exclude(dependency("org.xerial:sqlite-jdbc:.*"))
        }
        mergeServiceFiles()
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
