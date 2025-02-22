import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.2.1"
    id("org.jetbrains.intellij.platform.migration") version "2.2.1"
    id("org.jetbrains.changelog") version "2.1.0"
}

group = "ru.rzn.gmyasoedov"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion"))
        instrumentationTools()

        bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.properties")
        bundledPlugin("org.intellij.groovy")
        bundledPlugin("org.jetbrains.kotlin")

        testImplementation("junit:junit:4.12")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)

        zipSigner()
    }

    implementation(project(":server-api"))
    runtimeOnly(project(":maven-ext-event-handler"))
    runtimeOnly("io.github.grisha9:maven-model-reader-plugin:0.4") {
        exclude("com.google.code.gson", "gson")
    }
}

intellijPlatform {

    pluginConfiguration {
        id = "ru.rzn.gmyasoedov.gmaven"
        name = "Easy Maven"
        version = providers.gradleProperty("pluginVersion").get()

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }

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

    signing {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    changelog {
        headerParserRegex.set("""(\d+\.\d+(.\d+)?)""".toRegex())
        path.set(file("../CHANGELOG.md").canonicalPath)
    }
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
}

