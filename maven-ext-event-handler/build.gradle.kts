plugins {
    id("java")
}

group = "ru.rzn.gmyasoedov"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":server"))
    compileOnly(project(":server-api"))
    compileOnly("org.apache.maven:maven-embedder:3.3.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7;
    targetCompatibility = JavaVersion.VERSION_1_8;
}