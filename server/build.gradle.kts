plugins {
    id("java")
}

group = "ru.rzn.gmyasoedov"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://www.jetbrains.com/intellij-repository/snapshots")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
}


dependencies {
    compileOnly("com.jetbrains.intellij.platform", "util-rt", providers.gradleProperty("pluginSinceBuild").get())
    compileOnly("org.jetbrains:annotations-java5:20.1.0")
    compileOnly("org.codehaus.plexus:plexus-classworlds:2.6.0")
    implementation(project(":server-api"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7;
    targetCompatibility = JavaVersion.VERSION_1_7;
}
