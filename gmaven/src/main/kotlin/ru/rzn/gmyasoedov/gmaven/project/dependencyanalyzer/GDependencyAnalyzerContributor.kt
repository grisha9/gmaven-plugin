package ru.rzn.gmyasoedov.gmaven.project.dependencyanalyzer

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.dependency.analyzer.*
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtil
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.project.importing.DependencyGraphData
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import ru.rzn.gmyasoedov.serverapi.model.DependencyTreeNode
import ru.rzn.gmyasoedov.serverapi.model.MavenArtifactState
import ru.rzn.gmyasoedov.serverapi.model.MavenId
import java.lang.ref.SoftReference

class GDependencyAnalyzerContributor(private val project: Project) : DependencyAnalyzerContributor {
    @Volatile
    private var moduleMapByProject: SoftReference<Map<DependencyAnalyzerProject, ModuleDependencyGraphData>> =
        SoftReference(null)

    @Volatile
    private var modulesCache: SoftReference<Set<String>> = SoftReference(null)

    override fun whenDataChanged(listener: () -> Unit, parentDisposable: Disposable) {
        modulesCache = SoftReference(null)
        moduleMapByProject = SoftReference(null)
    }

    override fun getProjects(): List<DependencyAnalyzerProject> {
        val moduleMap = getModuleMap()
        return moduleMap.keys.toList()
    }

    override fun getDependencyScopes(externalProject: DependencyAnalyzerProject) = listOf(
        scope(GMavenConstants.SCOPE_COMPILE),
        scope(GMavenConstants.SCOPE_PROVIDED),
        scope(GMavenConstants.SCOPE_RUNTIME),
        scope(GMavenConstants.SCOPE_SYSTEM),
        scope(GMavenConstants.SCOPE_IMPORT),
        scope(GMavenConstants.SCOPE_TEST)
    )

    override fun getDependencies(externalProject: DependencyAnalyzerProject): List<DependencyAnalyzerDependency> {
        val dependencyGraphData = getModuleMap().get(externalProject)?.dependencyGraphData ?: return emptyList()
        return createDependencyList(dependencyGraphData, externalProject);
    }

    private fun getModulesCache(): Set<String> {
        val modulesSet = modulesCache.get()
        if (modulesSet != null) return modulesSet;
        val modulesMap = fillModulesInfoCache()
        return modulesMap.second
    }

    private fun getModuleMap(): Map<DependencyAnalyzerProject, ModuleDependencyGraphData> {
        val moduleDataMap = moduleMapByProject.get()
        if (moduleDataMap != null) return moduleDataMap;
        val modulesMap = fillModulesInfoCache()
        return modulesMap.first
    }

    private fun fillModulesInfoCache():
            Pair<Map<DependencyAnalyzerProject, ModuleDependencyGraphData>, Set<String>> {
        val projectsData = ProjectDataManager.getInstance().getExternalProjectsData(project, GMavenConstants.SYSTEM_ID)
        val modulesData = getModulesData(projectsData)
        val modulesMap = modulesData.asSequence()
            .map { Pair(it, MavenUtils.findIdeModule(project, it.moduleData)) }
            .filter { it.second != null }
            .map { DAProject(it.second!!, it.first.moduleData.moduleName) as DependencyAnalyzerProject to it.first }
            .toMap()
        moduleMapByProject = SoftReference(modulesMap)
        val modulesDataToCache = modulesDataToCache(modulesData)
        modulesCache = SoftReference(modulesDataToCache)
        return modulesMap to modulesDataToCache
    }


    private fun getModulesData(projectsData: Collection<ExternalProjectInfo>): List<ModuleDependencyGraphData> {
        return projectsData.asSequence()
            .map { it.externalProjectStructure }
            .filterNotNull()
            .flatMap { ExternalSystemApiUtil.findAllRecursively(it, ProjectKeys.MODULE) }
            .map { toModuleDependencyGraphData(it) }
            .toList()
    }

    private fun modulesDataToCache(modulesData: List<ModuleDependencyGraphData>) =
        modulesData.map { it.moduleData.group + ":" + it.moduleData.moduleName }.toSet()

    private fun createDependencyList(
        dependencyGraphData: DependencyGraphData,
        externalProject: DependencyAnalyzerProject
    ): List<DependencyAnalyzerDependency> {
        val root = DAModule(externalProject.title)
        root.putUserData(
            MAVEN_ARTIFACT_ID,
            MavenId(dependencyGraphData.group, dependencyGraphData.artifactId, dependencyGraphData.version)
        )
        val rootDependency = DADependency(root, scope(GMavenConstants.SCOPE_COMPILE), null, emptyList())
        val result = mutableListOf<DependencyAnalyzerDependency>()
        collectDependency(dependencyGraphData.treeNodes, rootDependency, result)
        return result
    }

    private fun collectDependency(
        nodes: List<DependencyTreeNode>,
        parentDependency: DependencyAnalyzerDependency,
        result: MutableList<DependencyAnalyzerDependency>
    ) {
        for (mavenArtifactNode in nodes) {
            val dependencyData = getDependencyData(mavenArtifactNode)
            val dependency = DADependency(
                dependencyData,
                scope(mavenArtifactNode.originalScope ?: GMavenConstants.SCOPE_COMPILE), parentDependency,
                getStatus(mavenArtifactNode, dependencyData)
            )
            result.add(dependency)
            if (mavenArtifactNode.dependencies != null) {
                collectDependency(mavenArtifactNode.dependencies, dependency, result)
            }
        }
    }

    private fun getDependencyData(mavenArtifactNode: DependencyTreeNode): DependencyAnalyzerDependency.Data {
        val artifact = mavenArtifactNode.artifact
        val isMavenProject = getModulesCache().contains(artifact.groupId + ":" + artifact.artifactId)
        if (isMavenProject) {
            val daModule = DAModule(artifact.artifactId)
            daModule.putUserData(
                MAVEN_ARTIFACT_ID,
                MavenId(artifact.groupId, artifact.artifactId, artifact.version)
            )
            return daModule
        }
        return DAArtifact(artifact.groupId, artifact.artifactId, artifact.version)
    }

    private fun getStatus(mavenArtifactNode: DependencyTreeNode, dependencyData: DependencyAnalyzerDependency.Data):
            List<DependencyAnalyzerDependency.Status> {
        val status = mutableListOf<DependencyAnalyzerDependency.Status>()
        if (mavenArtifactNode.getState() == MavenArtifactState.CONFLICT) {
            status.add(DAOmitted)
            mavenArtifactNode.relatedArtifact?.version?.also {
                val message =
                    ExternalSystemBundle.message("external.system.dependency.analyzer.warning.version.conflict", it)
                status.add(DAWarning(message))
            }
        } else if (mavenArtifactNode.getState() == MavenArtifactState.DUPLICATE) {
            status.add(DAOmitted)
        }
        if (dependencyData is DAArtifact && !mavenArtifactNode.artifact.isResolved) {
            status.add(DAWarning(ExternalSystemBundle.message("external.system.dependency.analyzer.warning.unresolved")))
        }
        return status
    }

    private fun toModuleDependencyGraphData(dataNode: DataNode<ModuleData>): ModuleDependencyGraphData {
        val graphData = ExternalSystemApiUtil.findAll(dataNode, DependencyGraphData.KEY)
        return ModuleDependencyGraphData(dataNode.data, graphData.firstOrNull()?.data)
    }

    companion object {
        fun scope(name: @NlsSafe String) = DAScope(name, StringUtil.toTitleCase(name))
        val MAVEN_ARTIFACT_ID = Key.create<MavenId>("GMavenDependencyAnalyzerContributor.MavenId")
    }

    private data class ModuleDependencyGraphData(
        val moduleData: ModuleData,
        val dependencyGraphData: DependencyGraphData?
    )
}