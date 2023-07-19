@file:JvmName("DependencyDataConverter")

package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.model.project.dependencies.ProjectDependenciesImpl
import com.intellij.openapi.roots.DependencyScope
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.SourceSetData
import ru.rzn.gmyasoedov.serverapi.model.MavenArtifact
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.MavenProjectContainer

fun addDependencies(
    container: MavenProjectContainer,
    projectNode: DataNode<ProjectData>,
    moduleDataByArtifactId: Map<String, MavenProjectResolver.ModuleContextHolder>
) {
    addDependencies(container.project, moduleDataByArtifactId)
    for (childContainer in container.modules) {
        addDependencies(childContainer, projectNode, moduleDataByArtifactId)
    }
}

private fun addDependencies(
    project: MavenProject, moduleDataByArtifactId: Map<String, MavenProjectResolver.ModuleContextHolder>
) {
    val moduleContextHolder = moduleDataByArtifactId[project.id]!!
    val sourceSetModules = moduleContextHolder.perSourceSetModules
    if (sourceSetModules == null) {
        addDependencies(project, moduleContextHolder.moduleNode, moduleDataByArtifactId)
    } else {
        addDependencies(project, sourceSetModules.mainNode, sourceSetModules.testNode, moduleDataByArtifactId)
    }
}

private fun addDependencies(
    project: MavenProject,
    moduleByMavenProject: DataNode<ModuleData>,
    moduleDataByArtifactId: Map<String, MavenProjectResolver.ModuleContextHolder>
) {
    var hasLibrary = false;
    for (artifact in project.resolvedArtifacts) {
        val moduleContextHolder = moduleDataByArtifactId[artifact.id]
        val moduleDataNodeByMavenArtifact = moduleContextHolder?.perSourceSetModules?.mainNode
            ?: moduleContextHolder?.moduleNode
        if (moduleDataNodeByMavenArtifact == null) {
            addLibrary(moduleByMavenProject, artifact)
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
    moduleDataByArtifactId: Map<String, MavenProjectResolver.ModuleContextHolder>
) {
    var hasLibrary = false;
    for (artifact in project.resolvedArtifacts) {
        val moduleContextHolder = moduleDataByArtifactId[artifact.id]
        val moduleDataNodeByMavenArtifact = moduleContextHolder?.perSourceSetModules?.mainNode
            ?: moduleContextHolder?.moduleNode
        if (moduleDataNodeByMavenArtifact == null) {
            addLibrary(mainModule, testModule, artifact)
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

private fun addLibrary(parentNode: DataNode<ModuleData>, artifact: MavenArtifact) {
    val createLibrary = createLibrary(artifact)
    val libraryDependencyData = LibraryDependencyData(parentNode.data, createLibrary, LibraryLevel.PROJECT)
    libraryDependencyData.scope = getScope(artifact)
    libraryDependencyData.order = 20 + getScopeOrder(libraryDependencyData.scope)
    parentNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyData)
    if (libraryDependencyData.scope == DependencyScope.RUNTIME) {
        val libraryDependencyDataTest = LibraryDependencyData(parentNode.data, createLibrary, LibraryLevel.PROJECT)
        libraryDependencyDataTest.scope = DependencyScope.TEST
        libraryDependencyDataTest.order = 20 + getScopeOrder(libraryDependencyDataTest.scope)
        parentNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyDataTest)
    }
    //projectNode.createChild(ProjectKeys.LIBRARY, createLibrary) - todo #c.i.o.e.s.p.m.LibraryDataService - Multiple project level libraries found with the same name 'GMaven: org.junit.platform:junit-platform-engine:1.7.1'
}

private fun addLibrary(
    mainNode: DataNode<SourceSetData>, testNode: DataNode<SourceSetData>, artifact: MavenArtifact
) {
    val scope = getScope(artifact)
    val createLibrary = createLibrary(artifact)
    if (scope != DependencyScope.TEST) {
        val libraryDependencyData = LibraryDependencyData(mainNode.data, createLibrary, LibraryLevel.PROJECT)
        libraryDependencyData.scope = scope
        libraryDependencyData.order = 20 + getScopeOrder(libraryDependencyData.scope)
        mainNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyData)

        val libraryDependencyDataTest = LibraryDependencyData(testNode.data, createLibrary, LibraryLevel.PROJECT)
        libraryDependencyDataTest.scope = scope
        libraryDependencyDataTest.order = libraryDependencyData.order
        testNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyDataTest)
    } else {
        val libraryDependencyData = LibraryDependencyData(mainNode.data, createLibrary, LibraryLevel.PROJECT)
        libraryDependencyData.scope = scope
        libraryDependencyData.order = 20 + getScopeOrder(libraryDependencyData.scope)
        mainNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyData)
    }
}

private fun addModuleDependency(parentNode: DataNode<out ModuleData>, targetModule: ModuleData) {
    val ownerModule = parentNode.data
    val data = ModuleDependencyData(ownerModule, targetModule)
    data.order = 1
    parentNode.createChild(ProjectKeys.MODULE_DEPENDENCY, data)
}

private fun createLibrary(artifact: MavenArtifact): LibraryData {
    val library = LibraryData(GMavenConstants.SYSTEM_ID, artifact.id, !artifact.isResolved)
    library.artifactId = artifact.artifactId
    library.setGroup(artifact.groupId)
    library.version = artifact.version
    library.addPath(getLibraryPathType(artifact), artifact.file.absolutePath)
    return library;
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