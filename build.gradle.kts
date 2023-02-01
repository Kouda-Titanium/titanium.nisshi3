import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

plugins {
    application
    kotlin("jvm") version "1.5.20"
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8", "1.5.20"))
    testImplementation("junit:junit:4.12")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

publishing {
    publications {
        register<MavenPublication>("main") {
            from(components["java"])
            groupId = "titanium"
            artifactId = "titanium.nisshi3"
            version = "5.0.0"
        }
    }
    repositories {
        maven {
            url = project.rootDir.resolve("maven").toURI()
        }
    }
}

tasks {
    register("fetchMirrgKotlin") {
        fun fetch(fileName: String) {
            val file = File("src/main/java").resolve(fileName)
            when {
                file.parentFile.isDirectory -> Unit
                file.parentFile.exists() -> throw RuntimeException("Already exists: ${file.parentFile}")
                !file.parentFile.mkdirs() -> throw RuntimeException("Could not create the directory: ${file.parentFile}")
            }
            file.writeBytes(URL("https://raw.githubusercontent.com/MirrgieRiana/mirrg.kotlin/main/src/main/java/$fileName").readBytes())
        }
        fetch("mirrg/kotlin/hydrogen/Collection.kt")
        fetch("mirrg/kotlin/hydrogen/Lang.kt")
        fetch("mirrg/kotlin/hydrogen/Number.kt")
        fetch("mirrg/kotlin/hydrogen/String.kt")
    }
}
