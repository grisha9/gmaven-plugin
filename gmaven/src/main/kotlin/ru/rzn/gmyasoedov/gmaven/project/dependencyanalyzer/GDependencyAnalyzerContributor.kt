package ru.rzn.gmyasoedov.gmaven.project.dependencyanalyzer

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.dependency.analyzer.*
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
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
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.MavenManager
import ru.rzn.gmyasoedov.gmaven.project.getMavenHome
import ru.rzn.gmyasoedov.gmaven.server.GServerRequest
import ru.rzn.gmyasoedov.gmaven.server.getDependencyTree
import ru.rzn.gmyasoedov.gmaven.settings.MavenProjectSettings
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import ru.rzn.gmyasoedov.serverapi.model.DependencyTreeNode
import ru.rzn.gmyasoedov.serverapi.model.MavenArtifactState
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class GDependencyAnalyzerContributor(private val project: Project) : DependencyAnalyzerContributor {
    @Volatile
    private var moduleMapByProject: Map<String, Pair<DAProject, ModuleData>>? = null
    private val dependencyTreeByProject: MutableMap<String, List<DependencyTreeNode>> = ConcurrentHashMap()

    override fun whenDataChanged(listener: () -> Unit, parentDisposable: Disposable) {
        val progressManager = ExternalSystemProgressNotificationManager.getInstance()
        progressManager.addNotificationListener(object : ExternalSystemTaskNotificationListenerAdapter() {
            override fun onEnd(id: ExternalSystemTaskId) {
                if (id.type != ExternalSystemTaskType.RESOLVE_PROJECT) return
                if (id.projectSystemId != SYSTEM_ID) return
                moduleMapByProject = null
                dependencyTreeByProject.clear()
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
        val artifactGA = externalProject.getUserData(ARTIFACT_ID)!!
        val moduleData = getModuleMap().get(artifactGA)?.second
        val projectPath = moduleData?.linkedExternalProjectPath ?: return emptyList()
        if (moduleData.getProperty(GMavenConstants.MODULE_PROP_HAS_DEPENDENCIES) == null) return emptyList()
        val mavenSettings = MavenSettings.getInstance(project)
        val projectSettings = mavenSettings.getLinkedProjectSettings(projectPath) ?: return emptyList()
        val gServerRequest = toProjectRequest(mavenSettings, projectSettings) ?: return emptyList()
        val dependencyTree = getDependencyTreeNodes(artifactGA, gServerRequest)
        return createDependencyList(dependencyTree, moduleData, externalProject)
    }

    private fun getDependencyTreeNodes(artifactGA: String, gServerRequest: GServerRequest): List<DependencyTreeNode> {
        val dependencyTreeFromMap = dependencyTreeByProject.get(artifactGA)
        if (dependencyTreeFromMap != null) return dependencyTreeFromMap
        val dependencyTreeProjects = getDependencyTree(gServerRequest, artifactGA)
        dependencyTreeProjects.forEach { dependencyTreeByProject[it.groupId + ":" + it.artifactId] = it.dependencyTree }
        return dependencyTreeByProject[artifactGA]!!
    }

    private fun toProjectRequest(mavenSettings: MavenSettings, projectSettings: MavenProjectSettings): GServerRequest? {
        val executionSettings = MavenManager.getExecutionSettings(
            project, projectSettings.externalProjectPath, mavenSettings, projectSettings
        )
        val id = ExternalSystemTaskId.create(SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, project)
        val mavenHome = getMavenHome(executionSettings.distributionSettings)
        val sdk = executionSettings.jdkName?.let { ExternalSystemJdkUtil.getJdk(null, it) } ?: return null
        val buildPath =
            Path.of(executionSettings.executionWorkspace.projectBuildFile ?: projectSettings.externalProjectPath)
        return GServerRequest(id, buildPath, mavenHome, sdk, executionSettings)
    }

    private fun getModuleMap(): Map<String, Pair<DAProject, ModuleData>> {
        if (moduleMapByProject != null) return moduleMapByProject!!
        return fillModulesInfoCache()
    }

    private fun fillModulesInfoCache(): Map<String, Pair<DAProject, ModuleData>> {
        val projectsData = ProjectDataManager.getInstance().getExternalProjectsData(project, SYSTEM_ID)
        val modulesData = getModulesData(projectsData)
        val modulesMap = modulesData.asSequence().map { Pair(it, MavenUtils.findIdeModule(project, it)) }
            .filter { it.second != null }.map { toDependencyAnalyzerProject(it) }.toMap()
        moduleMapByProject = modulesMap
        return modulesMap
    }

    private fun toDependencyAnalyzerProject(it: Pair<ModuleData, Module?>): Pair<String, Pair<DAProject, ModuleData>> {
        val moduleData = it.first
        val daProject = DAProject(it.second!!, moduleData.moduleName)
        val artifactId = MavenUtils.toGAString(moduleData)
        daProject.putUserData(ARTIFACT_ID, artifactId)
        return Pair(artifactId, Pair(daProject, moduleData))
    }

    private fun getModulesData(projectsData: Collection<ExternalProjectInfo>): List<ModuleData> {
        return projectsData.asSequence().map { it.externalProjectStructure }.filterNotNull()
            .flatMap { ExternalSystemApiUtil.findAllRecursively(it, ProjectKeys.MODULE) }.map { it.data }.toList()
    }

    private fun createDependencyList(
        treeNodeList: List<DependencyTreeNode>, moduleData: ModuleData, externalProject: DependencyAnalyzerProject
    ): List<DependencyAnalyzerDependency> {
        val root = DAModule(externalProject.title)
        root.putUserData(MODULE_DATA, moduleData)
        root.putUserData(BUILD_FILE, moduleData.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE))
        val rootDependency = DADependency(root, scope(GMavenConstants.SCOPE_COMPILE), null, emptyList())
        val result = mutableListOf<DependencyAnalyzerDependency>()
        collectDependency(treeNodeList, rootDependency, result)
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
                scope(mavenArtifactNode.originalScope ?: GMavenConstants.SCOPE_COMPILE),
                parentDependency,
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
            val buildFile = moduleInfo.second.getProperty(GMavenConstants.MODULE_PROP_BUILD_FILE)
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

    private fun getStatus(
        mavenArtifactNode: DependencyTreeNode,
        dependencyData: DependencyAnalyzerDependency.Data
    ): List<DependencyAnalyzerDependency.Status> {
        val status = mutableListOf<DependencyAnalyzerDependency.Status>()
        if (mavenArtifactNode.state == MavenArtifactState.CONFLICT) {
            status.add(DAOmitted)
            mavenArtifactNode.relatedArtifact?.version?.also {
                val message =
                    ExternalSystemBundle.message("external.system.dependency.analyzer.warning.version.conflict", it)
                status.add(DAWarning(message))
            }
        } else if (mavenArtifactNode.state == MavenArtifactState.DUPLICATE) {
            status.add(DAOmitted)
        }
        if (dependencyData is DAArtifact && !mavenArtifactNode.artifact.isResolved) {
            status.add(DAWarning(ExternalSystemBundle.message("external.system.dependency.analyzer.warning.unresolved")))
        }
        return status
    }

    companion object {
        fun scope(name: @NlsSafe String) = DAScope(name, StringUtil.toTitleCase(name))
        val MODULE_DATA = Key.create<ModuleData>("GMavenDependencyAnalyzerContributor.ModuleData")
        val ARTIFACT_ID = Key.create<String>("GMavenDependencyAnalyzerContributor.ArtifactId")
        val BUILD_FILE = Key.create<String>("GMavenDependencyAnalyzerContributor.BuildFile")
    }
}