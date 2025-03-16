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
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("net.thenextlvl.core:files:2.0.1")

    compileOnly("org.projectlombok:lombok:1.18.36")
    compileOnly("org.jspecify:jspecify:0.3.0")

    api("com.google.guava:guava:33.4.0-jre")
    api("com.google.code.gson:gson:2.12.1")

    api("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT") {
        isTransitive = false
    }

    annotationProcessor("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.jspecify:jspecify:0.3.0")
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