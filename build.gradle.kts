import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

plugins {
    application
    kotlin("jvm") version "1.9.20"
    `maven-publish`
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation("com.github.pgreze:kotlin-process:1.4.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}

publishing {
    publications {
        register<MavenPublication>("main") {
            from(components["java"])
            groupId = "titanium"
            artifactId = "titanium.nisshi3"
            version = "5.1.0"
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
        doFirst {
            fun fetch(fileName: String) {
                val file = project.rootDir.resolve("src/main/java").resolve(fileName)
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
            fetch("mirrg/kotlin/gson/hydrogen/Gson.kt")
            fetch("mirrg/kotlin/gson/hydrogen/JsonWrapper.kt")
        }
    }
}
