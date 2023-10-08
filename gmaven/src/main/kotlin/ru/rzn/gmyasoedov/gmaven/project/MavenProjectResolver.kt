package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.externalSystem.JavaProjectData
import com.intellij.openapi.externalSystem.importing.ProjectResolverPolicy
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ContentRootData
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.project.ProjectSdkData
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ProjectJdkNotFoundException
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.registry.Registry
import com.intellij.pom.java.LanguageLevel
import com.intellij.util.io.isDirectory
import org.jdom.Element
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.MavenFullImportPlugin
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.SourceSetData
import ru.rzn.gmyasoedov.gmaven.server.GServerRemoteProcessSupport
import ru.rzn.gmyasoedov.gmaven.server.GServerRequest
import ru.rzn.gmyasoedov.gmaven.server.firstRun
import ru.rzn.gmyasoedov.gmaven.server.getProjectModel
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings
import ru.rzn.gmyasoedov.serverapi.model.MavenResult
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.absolutePathString

class MavenProjectResolver : ExternalSystemProjectResolver<MavenExecutionSettings> {

    private val cancellationMap = ConcurrentHashMap<ExternalSystemTaskId, GServerRemoteProcessSupport>()

    override fun cancelTask(id: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
        cancellationMap[id]?.stopAll()
        return true
    }

    override fun resolveProjectInfo(
        id: ExternalSystemTaskId,
        projectPath: String,
        isPreviewMode: Boolean,
        settings: MavenExecutionSettings?,
        resolverPolicy: ProjectResolverPolicy?,
        listener: ExternalSystemTaskNotificationListener
    ): DataNode<ProjectData> {
        settings ?: throw ExternalSystemException("settings is empty")
        val sdk = settings.jdkName?.let { ExternalSystemJdkUtil.getJdk(null, it) }
        if (isPreviewMode) {
            return getPreviewProjectDataNode(settings, id, projectPath, sdk, listener)
        }

        sdk ?: throw ProjectJdkNotFoundException() //InvalidJavaHomeException
        val mavenHome = getMavenHome(settings.distributionSettings)
        val buildPath = Path.of(settings.executionWorkspace.projectBuildFile ?: projectPath)
        val request = GServerRequest(id, buildPath, mavenHome, sdk, listener = listener, settings = settings)
        try {
            val projectModel = getProjectModel(request) { cancellationMap[id] = it }
            return getProjectDataNode(projectPath, projectModel, settings)
        } finally {
            cancellationMap.remove(id)
        }
    }

    private fun getPreviewProjectDataNode(
        settings: MavenExecutionSettings,
        id: ExternalSystemTaskId,
        projectPath: String,
        sdk: Sdk?,
        listener: ExternalSystemTaskNotificationListener
    ): DataNode<ProjectData> {
        val projectDataNode = getPreviewProjectDataNode(projectPath, settings)
        val distributionPath = settings.distributionSettings.path
        if (sdk != null && distributionPath != null) {
            val buildPath = Path.of(settings.executionWorkspace.projectBuildFile ?: projectPath)
            firstRun(GServerRequest(id, buildPath, distributionPath, sdk, settings, listener = listener))
        }
        return projectDataNode
    }

    private fun getPreviewProjectDataNode(
        projectPath: String,
        settings: MavenExecutionSettings
    ): DataNode<ProjectData> {
        val projectDirectory = getProjectDirectory(projectPath).absolutePathString()
        val projectName = File(projectDirectory).name
        val projectData = ProjectData(GMavenConstants.SYSTEM_ID, projectName, projectDirectory, projectDirectory)
        val projectDataNode = DataNode(ProjectKeys.PROJECT, projectData, null)
        val ideProjectPath = settings.ideProjectPath
        val mainModuleFileDirectoryPath = ideProjectPath ?: projectDirectory
        projectDataNode
            .createChild(
                ProjectKeys.MODULE, ModuleData(
                    projectName, GMavenConstants.SYSTEM_ID, getDefaultModuleTypeId(),
                    projectName, mainModuleFileDirectoryPath, projectDirectory
                )
            )
            .createChild(ProjectKeys.CONTENT_ROOT, ContentRootData(GMavenConstants.SYSTEM_ID, projectDirectory))
        return projectDataNode
    }

    private fun getProjectDataNode(
        projectPath: String, mavenResult: MavenResult, settings: MavenExecutionSettings
    ): DataNode<ProjectData> {
        val container = mavenResult.projectContainer
        val project = container.project
        val projectName = project.displayName
        val absolutePath = project.file.parent
        val projectData = ProjectData(GMavenConstants.SYSTEM_ID, projectName, absolutePath, absolutePath)
        projectData.version = project.version
        projectData.group = project.groupId

        val projectDataNode = DataNode(ProjectKeys.PROJECT, projectData, null)

        val sdkName: String = settings.jdkName!! //todo
        val projectSdkData = ProjectSdkData(sdkName)
        projectDataNode.createChild(ProjectSdkData.KEY, projectSdkData)
        val languageLevel = LanguageLevel.parse(sdkName)
        val javaProjectData = JavaProjectData(
            GMavenConstants.SYSTEM_ID, project.outputDirectory, languageLevel,
            languageLevel!!.toJavaVersion().toFeatureString()
        )
        projectDataNode.createChild(JavaProjectData.KEY, javaProjectData)

        var ideProjectPath = settings.ideProjectPath
        ideProjectPath = ideProjectPath ?: projectPath
        val context = ProjectResolverContext(settings, absolutePath, ideProjectPath, mavenResult, languageLevel)

        val moduleNode = createModuleData(container, projectDataNode, context)

        for (childContainer in container.modules) {
            createModuleData(childContainer, moduleNode, context)
        }
        addDependencies(container, projectDataNode, context)
        populateProfiles(projectDataNode, context.mavenResult.settings)
        moduleNode.data.setProperty(GMavenConstants.MODULE_PROP_LOCAL_REPO, mavenResult.settings.localRepository)
        return projectDataNode
    }

    private fun getProjectDirectory(projectPath: String): Path {
        val projectNioPath = Path.of(projectPath)
        return if (projectNioPath.isDirectory()) projectNioPath else projectNioPath.parent
    }

    class ProjectResolverContext(
        val settings: MavenExecutionSettings,
        val rootProjectPath: String,
        val ideaProjectPath: String,
        val mavenResult: MavenResult,
        val projectLanguageLevel: LanguageLevel,
        val contextElementMap: MutableMap<String, Element> = HashMap(),
        val moduleDataByArtifactId: MutableMap<String, ModuleContextHolder> = TreeMap(),
        val pluginExtensionMap: Map<String, MavenFullImportPlugin> = MavenFullImportPlugin.EP_NAME.extensions
            .associateBy { it.key }
    ) {
        val lifecycles = Registry.stringValue("gmaven.lifecycles").split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    class ModuleContextHolder(
        val moduleNode: DataNode<ModuleData>,
        val perSourceSetModules: PerSourceSetModules?,
    )

    class PerSourceSetModules(
        val mainNode: DataNode<SourceSetData>,
        val testNode: DataNode<SourceSetData>,
    )
}