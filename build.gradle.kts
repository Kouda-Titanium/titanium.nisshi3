import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
            version = "1.0.0"
        }
    }
    repositories {
        maven {
            url = project.rootDir.resolve("maven").toURI()
        }
    }
}
