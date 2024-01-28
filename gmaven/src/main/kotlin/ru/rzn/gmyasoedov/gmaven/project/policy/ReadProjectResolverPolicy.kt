package ru.rzn.gmyasoedov.gmaven.project.policy

import com.intellij.openapi.externalSystem.importing.ProjectResolverPolicy

class ReadProjectResolverPolicy : ProjectResolverPolicy {
    override fun isPartialDataResolveAllowed() = false
}