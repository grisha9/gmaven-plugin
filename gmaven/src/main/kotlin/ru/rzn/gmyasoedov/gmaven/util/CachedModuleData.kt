package ru.rzn.gmyasoedov.gmaven.util

import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import kotlinx.collections.immutable.toImmutableSet
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object CachedModuleData {
    private val isPerform = AtomicBoolean(false)
    private val lastResult = AtomicReference<Set<String>>(emptySet())

    fun getAllConfigPaths(project: Project): Set<String> {
        if (!isPerform.compareAndSet(false, true)) {
            return lastResult.get()
        }
        try {
            val cachedValue = CachedValuesManager.getManager(project).getCachedValue(project) {
                CachedValueProvider.Result
                    .create(allConfigPaths(project), ExternalProjectsDataStorage.getInstance(project))
            }
            lastResult.set(cachedValue)
            return cachedValue
        } finally {
            isPerform.set(false)
        }
    }

    fun getAllConfigPaths(): Set<String> = lastResult.get()

    private fun allConfigPaths(project: Project): Set<String> {
        return ProjectDataManager.getInstance().getExternalProjectsData(project, SYSTEM_ID)
            .asSequence()
            .mapNotNull { it.externalProjectStructure }
            .flatMap { ExternalSystemApiUtil.findAllRecursively(it, ProjectKeys.MODULE) }
            .map { it.data }
            .mapNotNull { it.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE) }
            .toImmutableSet()
    }
}