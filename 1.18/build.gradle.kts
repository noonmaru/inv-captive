
plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    `maven-publish`
}

val version = "1.0.0.1"


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


}




dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.7.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1-native-mt")
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot:1.18.2-R0.1-SNAPSHOT")

    implementation("com.github.noonmaru:tap:3.2.7")
    implementation("com.github.noonmaru:kommand:0.6.4")
}
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.register<Copy>("copyJar") {
    into("$rootDir/output")
    from("$buildDir/libs")
}
tasks.register<Delete>("cleanPath") {
    delete("output")
    delete("$buildDir")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "16"
    }

}

tasks.jar {
    destinationDirectory.set(file("$rootDir/jars"))
    archiveName = rootProject.name + '-' + "1.18" + '-' + version + ".jar"
}