package ru.rzn.gmyasoedov.gmaven.project.dependencyanalyzer

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.dependency.analyzer.*
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtil
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.project.importing.DependencyGraphData
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import ru.rzn.gmyasoedov.serverapi.model.DependencyTreeNode
import ru.rzn.gmyasoedov.serverapi.model.MavenArtifactState
import java.lang.ref.SoftReference

class GDependencyAnalyzerContributor(private val project: Project) : DependencyAnalyzerContributor {
    @Volatile
    private var moduleMapByProject: SoftReference<Map<String, Pair<DAProject, ModuleDependencyGraphData>>> =
        SoftReference(null)

    override fun whenDataChanged(listener: () -> Unit, parentDisposable: Disposable) {
        val progressManager = ExternalSystemProgressNotificationManager.getInstance()
        progressManager.addNotificationListener(object : ExternalSystemTaskNotificationListenerAdapter() {
            override fun onEnd(id: ExternalSystemTaskId) {
                if (id.type != ExternalSystemTaskType.RESOLVE_PROJECT) return
                if (id.projectSystemId != GMavenConstants.SYSTEM_ID) return
                moduleMapByProject = SoftReference(null)
                listener()
            }
        }, parentDisposable)
    }

    override fun getProjects(): List<DependencyAnalyzerProject> {
        val moduleMap = getModuleMap()
        return moduleMap.values.map { it.first }
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
        val artifactId = externalProject.getUserData(ARTIFACT_ID)!!
        val moduleDependencyGraphData = getModuleMap().get(artifactId)?.second
        val dependencyGraphData = moduleDependencyGraphData?.dependencyGraphData ?: return emptyList()
        return createDependencyList(dependencyGraphData, moduleDependencyGraphData.moduleData, externalProject);
    }

    private fun getModuleMap(): Map<String, Pair<DAProject, ModuleDependencyGraphData>> {
        val moduleDataMap = moduleMapByProject.get()
        if (moduleDataMap != null) return moduleDataMap;
        return fillModulesInfoCache()
    }

    private fun fillModulesInfoCache(): Map<String, Pair<DAProject, ModuleDependencyGraphData>> {
        val projectsData = ProjectDataManager.getInstance().getExternalProjectsData(project, GMavenConstants.SYSTEM_ID)
        val modulesData = getModulesData(projectsData)
        val modulesMap = modulesData.asSequence()
            .map { Pair(it, MavenUtils.findIdeModule(project, it.moduleData)) }
            .filter { it.second != null }
            .map { toDependencyAnalyzerProject(it) }
            .toMap()
        moduleMapByProject = SoftReference(modulesMap)
        return modulesMap
    }

    private fun toDependencyAnalyzerProject(it: Pair<ModuleDependencyGraphData, Module?>): Pair<String, Pair<DAProject, ModuleDependencyGraphData>> {
        val moduleData = it.first.moduleData
        val daProject = DAProject(it.second!!, moduleData.moduleName)
        val artifactId = moduleData.group + ":" + moduleData.moduleName
        daProject.putUserData(ARTIFACT_ID, artifactId);
        return Pair(artifactId, Pair(daProject, it.first))
    }

    private fun getModulesData(projectsData: Collection<ExternalProjectInfo>): List<ModuleDependencyGraphData> {
        return projectsData.asSequence()
            .map { it.externalProjectStructure }
            .filterNotNull()
            .flatMap { ExternalSystemApiUtil.findAllRecursively(it, ProjectKeys.MODULE) }
            .map { toModuleDependencyGraphData(it) }
            .toList()
    }

    private fun createDependencyList(
        dependencyGraphData: DependencyGraphData,
        moduleData: ModuleData,
        externalProject: DependencyAnalyzerProject
    ): List<DependencyAnalyzerDependency> {
        val root = DAModule(externalProject.title)
        root.putUserData(MODULE_DATA, moduleData)
        root.putUserData(BUILD_FILE, moduleData.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE))
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
        val moduleInfo = getModuleMap().get(artifact.groupId + ":" + artifact.artifactId)
        if (moduleInfo != null) {
            val buildFile = moduleInfo.second.moduleData.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE)
            val daModule = DAModule(artifact.artifactId)
            daModule.putUserData(BUILD_FILE, buildFile)
            return daModule
        }
        val daArtifact = DAArtifact(artifact.groupId, artifact.artifactId, artifact.version)
        if (artifact.file != null) {
            daArtifact.putUserData(BUILD_FILE, artifact.file.absolutePath.replace(".jar", ".pom"))
        }
        return daArtifact
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
        val MODULE_DATA = Key.create<ModuleData>("GMavenDependencyAnalyzerContributor.ModuleData")
        val ARTIFACT_ID = Key.create<String>("GMavenDependencyAnalyzerContributor.ArtifactId")
        val BUILD_FILE = Key.create<String>("GMavenDependencyAnalyzerContributor.BuildFile")
    }

    private data class ModuleDependencyGraphData(
        val moduleData: ModuleData,
        val dependencyGraphData: DependencyGraphData?
    )
}