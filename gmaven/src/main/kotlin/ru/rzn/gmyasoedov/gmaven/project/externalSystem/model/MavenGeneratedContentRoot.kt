package ru.rzn.gmyasoedov.gmaven.project.externalSystem.model

import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import java.nio.file.Path

data class MavenGeneratedContentRoot(val type: ExternalSystemSourceType, val rootPath: Path, val paths: List<String>)
