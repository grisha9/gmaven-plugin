package ru.rzn.gmyasoedov.gmaven.server

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.projectRoots.Sdk
import java.nio.file.Path

data class GServerRequest(
    val taskId: ExternalSystemTaskId,
    val projectPath: Path,
    val mavenPath: Path,
    val sdk: Sdk,
    val vmOptions: String = "",
    val nonRecursion: Boolean = false,
    val installGMavenPlugin: Boolean = false
)

