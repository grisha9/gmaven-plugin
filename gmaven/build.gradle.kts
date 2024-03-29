plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.8.22"
    id("org.jetbrains.intellij") version "1.17.1"
    id("org.jetbrains.changelog") version "2.1.0"
}

group = "ru.rzn.gmyasoedov"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
}

dependencies {
    implementation(project(":server-api"))
    runtimeOnly(project(":server"))
    runtimeOnly(project(":maven-ext-event-handler"))
    runtimeOnly(fileTree("libs") { include("*.jar") })
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set(providers.gradleProperty("platformVersion").get())
    type.set("IC") // Target IDE Platform
    plugins.set(listOf(
        "java",
        "properties",
        "org.intellij.groovy",
        "org.jetbrains.kotlin",
        "org.jetbrains.plugins.terminal",
        "com.jetbrains.sh",
    ))
}

changelog {
    headerParserRegex.set("""(\d+\.\d+(.\d+)?)""".toRegex())
    path.set(file("../CHANGELOG.md").canonicalPath)
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set(providers.gradleProperty("pluginSinceBuild").get())
        untilBuild.set(providers.gradleProperty("pluginUntilBuild").get())

        val changelog = project.changelog
        changeNotes.set(providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    org.jetbrains.changelog.Changelog.OutputType.HTML,
                )
            }
        })
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

