import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    java
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "net.refractored"
version = findProperty("version")!!

fun getGitHash(): String {
    var gitCommitHash = "unknown"
    try {
        val workingDir = File("${project.projectDir}")
        val process =
            ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                .directory(workingDir)
                .start()
        process.waitFor()
        if (process.exitValue() == 0) {
            gitCommitHash =
                process.inputStream
                    .bufferedReader()
                    .readText()
                    .trim()
        }
    } catch (_: Exception) {
    }
    return gitCommitHash
}

fun isGitDirty(): Boolean =
    try {
        val workingDir = File("${project.projectDir}")
        val process =
            ProcessBuilder("git", "diff", "--quiet", "--ignore-submodules=dirty")
                .directory(workingDir)
                .start()
        process.waitFor()
        process.exitValue() != 0
    } catch (_: Exception) {
        false
    }

fun getCurrentGitTag(): String? {
    var gitTag: String? = null
    try {
        val workingDir = File("${project.projectDir}")
        val process =
            ProcessBuilder("git", "describe", "--tags", "--exact-match")
                .directory(workingDir)
                .start()
        process.waitFor()
        if (process.exitValue() == 0) {
            gitTag =
                process.inputStream
                    .bufferedReader()
                    .readText()
                    .trim()
        }
    } catch (_: Exception) {
    }
    return gitTag
}

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://jitpack.io")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.auxilor.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20-R0.1-SNAPSHOT")

    // Config Updater
    implementation("com.tchristofferson:ConfigUpdater:2.1-SNAPSHOT")

    // EssentialsX
    compileOnly("net.essentialsx:EssentialsX:2.20.1")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.22.0")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:2.22.0")

    // Lamp (Commands)
    implementation("com.github.Revxrsal.Lamp:common:3.3.6")
    implementation("com.github.Revxrsal.Lamp:bukkit:3.3.6")

    implementation("org.bstats:bstats-bukkit:3.0.2")

    implementation("io.papermc:paperlib:1.0.7")

    // ORMLite (ORM)
    implementation("com.j256.ormlite:ormlite-core:6.1")
    implementation("com.j256.ormlite:ormlite-jdbc:6.1")

    // Vault (Economy)
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    // SpiGUI (GUI)
    implementation("com.samjakob:SpiGUI:1.3.1")

    compileOnly("com.github.Emibergo02:RedisChat:5.3.2")
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile> {
    // Preserve parameter names in the bytecode
    options.compilerArgs.add("-parameters")
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions {
        javaParameters = true
    }
}

tasks {

    if (getCurrentGitTag()?.removePrefix("v") != version) {
        version = "$version-${getGitHash()}"
    }
    if (isGitDirty()) {
        version = "$version-dirty"
    }

    shadowJar {
        val libModule = "net.refractored.joblistings.libs"
        relocate("org.json", "$libModule.json")
        relocate("revxrsal.commands", "$libModule.lamp")
        relocate("kotlin", "$libModule.kotlin")
        relocate("kotlinx.coroutines", "$libModule.coroutines")
        relocate("com.github.shynixn.mccoroutine.bukkit", "$libModule.mccoroutines")
        relocate("com.samjakob.spigui", "$libModule.spigui")
        relocate("com.tchristofferson.configupdater", "$libModule.configupdater")
        relocate("com.j256.ormlite", "$libModule.ormlite")
        relocate("org.bstats", "$libModule.bstats")
        relocate("io.papermc.lib", "$libModule.paperlib")

        delete(file("$rootDir/bin"))
        destinationDirectory.set(file("$rootDir/bin"))
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("**plugin.yml") {
            expand(props)
        }
    }
}
