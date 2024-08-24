plugins {
    id("java")
}

group = "ru.rzn.gmyasoedov"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations-java5:20.1.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7;
    targetCompatibility = JavaVersion.VERSION_1_8;
}
