package ru.rzn.gmyasoedov.gmaven.project.externalSystem.model

import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType

data class MavenContentRoot(val type: ExternalSystemSourceType, val path: String)
