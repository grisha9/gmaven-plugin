@file:JvmName("DependencyDataConverter")

package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.model.project.dependencies.ProjectDependenciesImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.util.registry.Registry
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectResolver.ProjectResolverContext
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.SourceSetData
import ru.rzn.gmyasoedov.serverapi.model.MavenArtifact
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.MavenProjectContainer
import java.nio.file.Path
import kotlin.io.path.exists


fun addDependencies(
    container: MavenProjectContainer,
    projectNode: DataNode<ProjectData>,
    context: ProjectResolverContext
) {
    addDependencies(container.project, context)
    for (childContainer in container.modules) {
        addDependencies(childContainer, projectNode, context)
    }
}

private fun addDependencies(project: MavenProject, context: ProjectResolverContext) {
    val moduleContextHolder = context.moduleDataByArtifactId[project.id]!!
    val sourceSetModules = moduleContextHolder.perSourceSetModules
    if (sourceSetModules == null) {
        addDependencies(project, moduleContextHolder.moduleNode, context)
    } else {
        addDependencies(project, sourceSetModules.mainNode, sourceSetModules.testNode, context)
    }
}

private fun addDependencies(
    project: MavenProject, moduleByMavenProject: DataNode<ModuleData>, context: ProjectResolverContext
) {
    var hasLibrary = false
    for (artifact in project.resolvedArtifacts) {
        val moduleContextHolder = context.moduleDataByArtifactId[artifact.id]
        val moduleDataNodeByMavenArtifact = moduleContextHolder?.perSourceSetModules?.mainNode
            ?: moduleContextHolder?.moduleNode
        if (moduleDataNodeByMavenArtifact == null) {
            addLibrary(moduleByMavenProject, artifact, context)
            if (!hasLibrary) hasLibrary = true
        } else {
            addModuleDependency(moduleByMavenProject, moduleDataNodeByMavenArtifact.data)
        }
    }
    if (hasLibrary) {
        moduleByMavenProject.data.setProperty(GMavenConstants.MODULE_PROP_HAS_DEPENDENCIES, true.toString())
        moduleByMavenProject.createChild(ProjectKeys.DEPENDENCIES_GRAPH, ProjectDependenciesImpl())
    }
}

private fun addDependencies(
    project: MavenProject,
    mainModule: DataNode<SourceSetData>,
    testModule: DataNode<SourceSetData>,
    context: ProjectResolverContext
) {
    var hasLibrary = false
    for (artifact in project.resolvedArtifacts) {
        val moduleContextHolder = context.moduleDataByArtifactId[artifact.id]
        val moduleDataNodeByMavenArtifact = moduleContextHolder?.perSourceSetModules?.mainNode
            ?: moduleContextHolder?.moduleNode
        if (moduleDataNodeByMavenArtifact == null) {
            addLibrary(mainModule, testModule, artifact, context)
            if (!hasLibrary) hasLibrary = true
        } else {
            addModuleDependency(mainModule, moduleDataNodeByMavenArtifact.data)
            addModuleDependency(testModule, moduleDataNodeByMavenArtifact.data)
        }
    }
    if (hasLibrary) {
        mainModule.data.setProperty(GMavenConstants.MODULE_PROP_HAS_DEPENDENCIES, true.toString())
        mainModule.createChild(ProjectKeys.DEPENDENCIES_GRAPH, ProjectDependenciesImpl())

        testModule.data.setProperty(GMavenConstants.MODULE_PROP_HAS_DEPENDENCIES, true.toString())
        testModule.createChild(ProjectKeys.DEPENDENCIES_GRAPH, ProjectDependenciesImpl())
    }
}

private fun addLibrary(
    parentNode: DataNode<ModuleData>,
    artifact: MavenArtifact,
    context: ProjectResolverContext
) {
    val createdLibrary = createLibrary(artifact, context.mavenResult.settings.modulesCount)

    var level = LibraryLevel.PROJECT
    if (!linkProjectLibrary(context, context.projectNode, createdLibrary)) {
        level = LibraryLevel.MODULE
    }

    val libraryDependencyData = LibraryDependencyData(parentNode.data, createdLibrary, level)
    libraryDependencyData.scope = getScope(artifact)
    libraryDependencyData.order = 20 + getScopeOrder(libraryDependencyData.scope)
    parentNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyData)
    if (libraryDependencyData.scope == DependencyScope.RUNTIME) {
        val libraryDependencyDataTest = LibraryDependencyData(parentNode.data, createdLibrary, level)
        libraryDependencyDataTest.scope = DependencyScope.TEST
        libraryDependencyDataTest.order = 20 + getScopeOrder(libraryDependencyDataTest.scope)
        parentNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyDataTest)
    }
}

private fun addLibrary(
    mainNode: DataNode<SourceSetData>, testNode: DataNode<SourceSetData>, artifact: MavenArtifact,
    context: ProjectResolverContext
) {
    val scope = getScope(artifact)
    val createdLibrary = createLibrary(artifact, context.mavenResult.settings.modulesCount)
    var level = LibraryLevel.PROJECT
    if (!linkProjectLibrary(context, context.projectNode, createdLibrary)) {
        level = LibraryLevel.MODULE
    }
    if (scope != DependencyScope.TEST) {
        val libraryDependencyData = LibraryDependencyData(mainNode.data, createdLibrary, level)
        libraryDependencyData.scope = scope
        libraryDependencyData.order = 20 + getScopeOrder(libraryDependencyData.scope)
        mainNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyData)

        val libraryDependencyDataTest = LibraryDependencyData(testNode.data, createdLibrary, level)
        libraryDependencyDataTest.scope = scope
        libraryDependencyDataTest.order = libraryDependencyData.order
        testNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyDataTest)
    } else {
        val libraryDependencyData = LibraryDependencyData(mainNode.data, createdLibrary, level)
        libraryDependencyData.scope = scope
        libraryDependencyData.order = 20 + getScopeOrder(libraryDependencyData.scope)
        mainNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyData)
    }
}

private fun linkProjectLibrary(
    context: ProjectResolverContext, ideProject: DataNode<ProjectData>, library: LibraryData
): Boolean {
    if (!Registry.`is`("gmaven.library.map.for.performance")) return true

    val cache = context.libraryDataMap
    val libraryName = library.externalName

    val libraryData = cache.computeIfAbsent(libraryName) {
        var newValueToCache = ExternalSystemApiUtil
            .findChild(ideProject, ProjectKeys.LIBRARY) { it.data.externalName == libraryName }
        if (newValueToCache == null) {
            newValueToCache = ideProject.createChild(ProjectKeys.LIBRARY, library)
        }
        newValueToCache
    }
    return libraryData.data == library
}

private fun addModuleDependency(parentNode: DataNode<out ModuleData>, targetModule: ModuleData) {
    val ownerModule = parentNode.data
    val data = ModuleDependencyData(ownerModule, targetModule)
    data.order = 1
    parentNode.createChild(ProjectKeys.MODULE_DEPENDENCY, data)
}

private fun createLibrary(artifact: MavenArtifact, modulesCount: Int): LibraryData {
    val library = LibraryData(GMavenConstants.SYSTEM_ID, artifact.id, !artifact.isResolved)
    library.artifactId = artifact.artifactId
    library.setGroup(artifact.groupId)
    library.version = artifact.version
    if (artifact.file == null) return library
    val artifactAbsolutePath = artifact.file.absolutePath
    library.addPath(getLibraryPathType(artifact), artifactAbsolutePath)
    if (modulesCount <= 10 || Registry.`is`("gmaven.import.library.sync.sources")) { //todo registry to settings
        val sourceAbsolutePath = artifactAbsolutePath.replace(".jar", "-sources.jar")
        if (sourceAbsolutePath != artifactAbsolutePath && Path.of(sourceAbsolutePath).exists()) {
            library.addPath(LibraryPathType.SOURCE, sourceAbsolutePath)
        }
    }

    return library
}

private fun getScope(artifact: MavenArtifact): DependencyScope {
    return when {
        isTestScope(artifact) -> DependencyScope.TEST
        GMavenConstants.SCOPE_RUNTIME == artifact.scope -> DependencyScope.RUNTIME
        GMavenConstants.SCOPE_PROVIDED == artifact.scope -> DependencyScope.PROVIDED
        else -> DependencyScope.COMPILE
    }
}

private fun getScopeOrder(scope: DependencyScope): Int {
    return when (scope) {
        DependencyScope.TEST -> 3
        DependencyScope.RUNTIME -> 2
        DependencyScope.PROVIDED -> 1
        else -> 0
    }
}

private fun getLibraryPathType(artifact: MavenArtifact): LibraryPathType {
    return when {
        "javadoc".equals(artifact.classifier, ignoreCase = true) -> LibraryPathType.DOC
        "sources".equals(artifact.classifier, ignoreCase = true) -> LibraryPathType.SOURCE
        else -> LibraryPathType.BINARY
    }
}

private fun isTestScope(artifact: MavenArtifact) =
    (GMavenConstants.SCOPE_TEST == artifact.scope || "tests".equals(artifact.classifier, ignoreCase = true)
            || "test-jar".equals(artifact.type, ignoreCase = true))