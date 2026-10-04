plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net")
}

dependencies {
    implementation("com.mojang:brigadier:1.0.18")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "me.lumen.Main"
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    shadowJar{
        archiveClassifier.set("")
    }
}