plugins {
    id("java")
}

group = "ru.rzn.gmyasoedov"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    /*compileOnly(fileTree("libs") { include("*.jar") })*/
    compileOnly("org.jetbrains:annotations-java5:20.1.0")

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7;
    targetCompatibility = JavaVersion.VERSION_1_7;
}
