plugins {
    id("java")
    id("java-library")
    id("maven-publish")
}

group = "net.thenextlvl"
version = "1.0.2"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
    withJavadocJar()
}

tasks.compileJava {
    options.release.set(21)
}

repositories {
    mavenCentral()
    maven("https://repo.thenextlvl.net/releases")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("net.thenextlvl.core:files:1.0.5")

    compileOnly("net.thenextlvl.core:annotations:2.0.1")
    compileOnly("org.projectlombok:lombok:1.18.34")
    compileOnly("org.jetbrains:annotations:26.0.1")

    api("com.google.guava:guava:33.3.1-jre")
    api("com.google.code.gson:gson:2.11.0")

    api("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT") {
        isTransitive = false
    }

    annotationProcessor("org.projectlombok:lombok:1.18.34")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
    repositories.maven {
        val channel = if ((version as String).contains("-pre")) "snapshots" else "releases"
        url = uri("https://repo.thenextlvl.net/$channel")
        credentials {
            username = System.getenv("REPOSITORY_USER")
            password = System.getenv("REPOSITORY_TOKEN")
        }
    }
}