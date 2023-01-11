import java.io.OutputStream

plugins {
    kotlin("jvm") version "1.7.0"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    `maven-publish`
}

val relocate = (findProperty("relocate") as? String)?.toBoolean() ?: true

println("relocate = $relocate")

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
    maven { url = uri("https://jitpack.io") }
    maven {
        name = "Mojang"
        url = uri("https://libraries.minecraft.net/")
    }
    maven {
        name = "Spigot"
        url = uri("https://repo.dmulloy2.net/repository/public/")
    }
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        content {
            group = includeGroup("org.bukkit")
            group = includeGroup("org.spigotmc")
        }
    }
    maven {
        url = uri("https://repo.spring.io/plugins-release/")
    }
    maven {
        url = uri("https://org.bstats/bstats-bukkit")
    }
}


dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.7.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    compileOnly("io.papermc.paper:paper-api:1.19.3-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot:1.19.3-R0.1-SNAPSHOT")

    implementation("com.github.noonmaru:tap:3.2.7")
    implementation("com.github.noonmaru:kommand:0.6.4")
}


java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "16"
    }
    processResources {
        filesMatching("**/*.yml") {
            expand(project.properties)
        }
    }
    create<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)

    }
    shadowJar {
        archiveBaseName.set(project.property("pluginName").toString())
        archiveVersion.set("") // For bukkit plugin update
        archiveClassifier.set("") // Remove 'all'

        if (relocate) {
            relocate("com.github.noonmaru.kommand", "${rootProject.group}.${rootProject.name}.kommand")
            relocate("com.github.noonmaru.tap", "${rootProject.group}.${rootProject.name}.tap")
        }
    }
    create<Copy>("paper") {
        from(shadowJar)
        into("output")
        from("$buildDir/libs")
    }
    create<DefaultTask>("setupWorkspace") {
        doLast {
            val versions = arrayOf(
                "1.12.2"
            )
            val buildtoolsDir = file(".buildtools")
            val buildtools = File(buildtoolsDir, "BuildTools.jar")

            val maven = File(System.getProperty("user.home"), ".m2/repository/org/spigotmc/spigot/")
            val repos = maven.listFiles { file: File -> file.isDirectory } ?: emptyArray()
            val missingVersions = versions.filter { version ->
                repos.find { it.name.startsWith(version) }?.also { println("Skip downloading spigot-$version") } == null
            }.also { if (it.isEmpty()) return@doLast }

            val download by registering(de.undercouch.gradle.tasks.download.Download::class) {
                src("https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar")
                dest(buildtools)
            }
            download.get().download()

            runCatching {
                for (v in missingVersions) {
                    println("Downloading spigot-$v...")

                    javaexec {
                        workingDir(buildtoolsDir)
                        main = "-jar"
                        args = listOf("./${buildtools.name}", "--rev", v)
                        // Silent
                        standardOutput = OutputStream.nullOutputStream()
                        errorOutput = OutputStream.nullOutputStream()
                    }
                }
            }.onFailure {
                it.printStackTrace()
            }
            buildtoolsDir.deleteRecursively()
        }
    }
}

tasks.jar {
    destinationDirectory.set(file("$rootDir/jars"))
    archiveName = rootProject.name + '-' + "1.19" + '-' + version + ".jar"
}