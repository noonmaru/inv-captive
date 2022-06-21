import java.io.OutputStream

plugins {
 kotlin("jvm")
}

val relocate = (findProperty("relocate") as? String)?.toBoolean() ?: true

println("relocate = $relocate")

tasks.register<Copy>("copyJar") {
    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(16))
    }
    into("$projectDir/output")
    from("$buildDir/libs")
}
tasks.register<Delete>("cleanPath") {
    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(16))
    }
    delete("output")
    delete("$buildDir/libs")
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
}

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


}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.5.10")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1-native-mt")
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot:1.16.5-R0.1-SNAPSHOT")

    implementation("com.github.noonmaru:tap:3.2.7")
    implementation("com.github.noonmaru:kommand:0.6.4")
}
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
