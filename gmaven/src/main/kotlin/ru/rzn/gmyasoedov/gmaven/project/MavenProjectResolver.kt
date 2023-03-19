package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.externalSystem.JavaModuleData
import com.intellij.externalSystem.JavaProjectData
import com.intellij.openapi.externalSystem.importing.ProjectResolverPolicy
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ProjectJdkNotFoundException
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver
import com.intellij.openapi.module.ModuleTypeManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ui.configuration.SdkLookupBuilder
import com.intellij.openapi.roots.ui.configuration.SdkLookupDecision
import com.intellij.openapi.roots.ui.configuration.UnknownSdkDownloadableSdkFix
import com.intellij.openapi.roots.ui.configuration.lookupSdk
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.pom.java.LanguageLevel
import org.apache.commons.lang.StringUtils
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.project.importing.AnnotationProcessingData
import ru.rzn.gmyasoedov.gmaven.server.GServerRequest
import ru.rzn.gmyasoedov.gmaven.server.getProjectModel
import ru.rzn.gmyasoedov.gmaven.server.getProjectModelFirstRun
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import ru.rzn.gmyasoedov.serverapi.model.MavenArtifact
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.MavenProjectContainer
import java.io.File
import java.nio.file.Path.of

class MavenProjectResolver : ExternalSystemProjectResolver<MavenExecutionSettings> {
    override fun cancelTask(taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener) = false

    override fun resolveProjectInfo(
        id: ExternalSystemTaskId,
        projectPath: String,
        isPreviewMode: Boolean,
        settings: MavenExecutionSettings?,
        resolverPolicy: ProjectResolverPolicy?,
        listener: ExternalSystemTaskNotificationListener
    ): DataNode<ProjectData> {
        val sdk = (settings ?: throw RuntimeException("settings is empty"))
            .jdkName?.let { ExternalSystemJdkUtil.getJdk(null, it) } ?: throw ProjectJdkNotFoundException()

        if (isPreviewMode) {
            return getPreviewProjectDataNode(settings, id, projectPath, sdk)
        }
        val request = GServerRequest(id, of(projectPath), of(settings.mavenHome!!), sdk) //todo mavenHome
        val projectModel = getProjectModel(request)
        return getProjectDataNode(projectPath, projectModel, settings)
    }

    private fun getPreviewProjectDataNode(
        settings: MavenExecutionSettings,
        id: ExternalSystemTaskId,
        projectPath: String,
        sdk: Sdk
    ): DataNode<ProjectData> {
        if (settings.mavenHome != null) {
            val request = GServerRequest(id, of(projectPath), of(settings.mavenHome), sdk)
            val projectModel = getProjectModelFirstRun(request)
            return getProjectDataNode(projectPath, projectModel, settings)
        } else {
            return getPreviewProjectDataNode(projectPath, settings)
        }
    }

    private fun getPreviewProjectDataNode(
        projectPath: String,
        settings: MavenExecutionSettings
    ): DataNode<ProjectData> {
        val projectName = File(projectPath).name
        val projectData = ProjectData(GMavenConstants.SYSTEM_ID, projectName, projectPath, projectPath)
        val projectDataNode = DataNode(ProjectKeys.PROJECT, projectData, null)
        val ideProjectPath = settings.ideProjectPath
        val mainModuleFileDirectoryPath = ideProjectPath ?: projectPath
        projectDataNode
            .createChild(
                ProjectKeys.MODULE, ModuleData(
                    projectName, GMavenConstants.SYSTEM_ID, getDefaultModuleTypeId(),
                    projectName, mainModuleFileDirectoryPath, projectPath
                )
            )
            .createChild(ProjectKeys.CONTENT_ROOT, ContentRootData(GMavenConstants.SYSTEM_ID, projectPath))
        return projectDataNode
    }

    private fun getProjectDataNode(
        projectPath: String, container: MavenProjectContainer, settings: MavenExecutionSettings
    ): DataNode<ProjectData> {
        val project = container.project
        val projectName = project.displayName
        val absolutePath = project.file.parent
        val projectData = ProjectData(GMavenConstants.SYSTEM_ID, projectName, absolutePath, absolutePath)
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
        val context = ProjectResolverContext(absolutePath, ideProjectPath)

        val moduleDataByArtifactId = HashMap<String, DataNode<ModuleData>>()
        val moduleNode = createModuleData(container, projectDataNode, context, moduleDataByArtifactId)

        for (childContainer in container.modules) {
            createModuleData(childContainer, moduleNode!!, context, moduleDataByArtifactId)
        }
        addDependencies(container, moduleDataByArtifactId)
        return projectDataNode
    }

    private fun addDependencies(
        container: MavenProjectContainer,
        moduleDataByArtifactId: Map<String, DataNode<ModuleData>>
    ) {
        addDependencies(container.project, moduleDataByArtifactId)
        for (childContainer in container.modules) {
            addDependencies(childContainer, moduleDataByArtifactId)
        }
    }

    private fun addDependencies(project: MavenProject, moduleDataByArtifactId: Map<String, DataNode<ModuleData>>) {
        val moduleByMavenProject = moduleDataByArtifactId[project.id]!!
        for (artifact in project.resolvedArtifacts) {
            val moduleDataNodeByMavenArtifact = moduleDataByArtifactId[artifact.id]
            if (moduleDataNodeByMavenArtifact == null) {
                addLibrary(moduleByMavenProject, artifact)
            } else {
                addModuleDependency(moduleByMavenProject, moduleDataNodeByMavenArtifact.data)
            }
        }
    }

    private fun createModuleData(
        container: MavenProjectContainer,
        parentDataNode: DataNode<*>,
        context: ProjectResolverContext,
        moduleDataByArtifactId: MutableMap<String, DataNode<ModuleData>>
    ): DataNode<ModuleData> {
        val project = container.project
        val parentExternalName = getModuleName(parentDataNode.data, true)
        val parentInternalName = getModuleName(parentDataNode.data, false)
        val id = if (parentExternalName == null) project.artifactId else ":" + project.artifactId
        val projectPath = project.basedir
        val mainModuleFileDirectoryPath: String = getIdeaModulePath(context, projectPath)
        val moduleData = ModuleData(
            id, GMavenConstants.SYSTEM_ID, getDefaultModuleTypeId(), project.artifactId,
            mainModuleFileDirectoryPath,
            projectPath
        )
        moduleData.internalName = getInternalModuleName(parentInternalName, project.artifactId)
        moduleData.group = project.groupId
        moduleData.version = project.version
        moduleData.moduleName = project.artifactId
        moduleData.useExternalCompilerOutput(true)
        moduleData.isInheritProjectCompileOutputPath = false
        moduleData.setCompileOutputPath(ExternalSystemSourceType.SOURCE, project.outputDirectory)
        moduleData.setExternalCompilerOutputPath(ExternalSystemSourceType.SOURCE, project.outputDirectory)
        val moduleDataNode = parentDataNode.createChild(ProjectKeys.MODULE, moduleData)
        moduleDataByArtifactId.put(project.id, moduleDataNode)
        val contentRootData = ContentRootData(GMavenConstants.SYSTEM_ID, projectPath)
        moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRootData)
        storePath(project.sourceRoots, contentRootData, ExternalSystemSourceType.SOURCE)
        storePath(project.resourceRoots, contentRootData, ExternalSystemSourceType.RESOURCE)
        storePath(project.testSourceRoots, contentRootData, ExternalSystemSourceType.TEST)
        storePath(project.testResourceRoots, contentRootData, ExternalSystemSourceType.TEST_RESOURCE)
        contentRootData.storePath(ExternalSystemSourceType.EXCLUDED, project.buildDirectory)
        val compilerPluginVersion: String = getCompilerPluginVersion(project)
        val sourceLanguageLevel: LanguageLevel =
            getMavenLanguageLevel(project, compilerPluginVersion, "maven.compiler.source")
        val targetBytecodeLevel: LanguageLevel =
            getMavenLanguageLevel(project, compilerPluginVersion, "maven.compiler.target")
        moduleDataNode.createChild(ModuleSdkData.KEY, ModuleSdkData(null))

        moduleDataNode.createChild(
            JavaModuleData.KEY, JavaModuleData(
                GMavenConstants.SYSTEM_ID, sourceLanguageLevel,
                targetBytecodeLevel.toJavaVersion().toFeatureString()
            )
        )
        populateAnnotationProcessorData(project, moduleDataNode)
        if (parentDataNode.data is ModuleData) {
            for (childContainer in container.modules) {
                createModuleData(childContainer, moduleDataNode, context, moduleDataByArtifactId)
            }
        }
        return moduleDataNode
    }

    private fun populateAnnotationProcessorData(project: MavenProject, moduleDataNode: DataNode<ModuleData>) {
        val annotationProcessorPaths = project.annotationProcessorPaths ?: return
        val data = AnnotationProcessingData
            .create(annotationProcessorPaths, emptyList(), project.buildDirectory, project.basedir)
        moduleDataNode.createChild(AnnotationProcessingData.KEY, data)

    }

    private fun getDefaultModuleTypeId(): String {
        return ModuleTypeManager.getInstance().defaultModuleType.id
    }

    private fun getIdeaModulePath(context: ProjectResolverContext, projectPath: String): String {
        val relativePath: String?
        val isUnderProjectRoot = FileUtil.isAncestor(context.rootProjectPath, projectPath, false)
        relativePath = if (isUnderProjectRoot) {
            FileUtil.getRelativePath(context.rootProjectPath, projectPath, File.separatorChar)
        } else {
            FileUtil.pathHashCode(projectPath).toString()
        }
        val subPath = if (relativePath == null || relativePath == ".") "" else relativePath
        return context.ideaProjectPath + File.separatorChar + subPath
    }

    private fun getModuleName(data: Any, external: Boolean): String? {
        return if (data is ModuleData) {
            if (external) data.externalName else data.internalName
        } else null
    }

    private fun storePath(paths: List<String>, contentRootData: ContentRootData, type: ExternalSystemSourceType) {
        for (path in paths) {
            contentRootData.storePath(type, path)
        }
    }

    private fun resolveJdkName(jdkNameOrVersion: String): String? {
        val sdk = lookupSdk { builder: SdkLookupBuilder ->
            builder
                .withSdkName(jdkNameOrVersion)
                .withSdkType(ExternalSystemJdkUtil.getJavaSdkType())
                .onDownloadableSdkSuggested { _: UnknownSdkDownloadableSdkFix? -> SdkLookupDecision.STOP }
        }
        return sdk?.name
    }

    private fun getMavenLanguageLevel(
        mavenProject: MavenProject,
        compilerPluginVersion: String,
        property: String
    ): LanguageLevel {
        val isReleaseEnabled = StringUtil.compareVersionNumbers(compilerPluginVersion, "3.6") >= 0
        val mavenProjectReleaseLevel =
            if (isReleaseEnabled) mavenProject.properties["maven.compiler.release"] as String? else null
        var level = LanguageLevel.parse(mavenProjectReleaseLevel)
        if (level == null) {
            val mavenProjectLanguageLevel = mavenProject.properties[property] as String?
            level = LanguageLevel.parse(mavenProjectLanguageLevel)
        }
        return level ?: LanguageLevel.HIGHEST
    }

    private fun getCompilerPluginVersion(mavenProject: MavenProject): String {
        val plugin = MavenUtils.findPlugin(mavenProject, "org.apache.maven.plugins", "maven-compiler-plugin")
        return plugin?.version ?: StringUtils.EMPTY
    }

    private fun addLibrary(parentNode: DataNode<ModuleData>, artifact: MavenArtifact) {
        val library = LibraryData(GMavenConstants.SYSTEM_ID, artifact.id)
        library.setArtifactId(artifact.artifactId)
        library.setGroup(artifact.groupId)
        library.setVersion(artifact.version)
        library.addPath(getLibraryPathType(artifact), artifact.file.absolutePath)
        val libraryDependencyData = LibraryDependencyData(parentNode.data, library, LibraryLevel.MODULE)
        libraryDependencyData.setScope(getScope(artifact))
        libraryDependencyData.order = 2
        parentNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyData)
    }

    private fun getLibraryPathType(artifact: MavenArtifact): LibraryPathType {
        return when {
            "javadoc".equals(artifact.classifier, ignoreCase = true) -> LibraryPathType.DOC
            "sources".equals(artifact.classifier, ignoreCase = true) -> LibraryPathType.SOURCE
            else -> LibraryPathType.BINARY
        }
    }

    fun getScope(artifact: MavenArtifact): DependencyScope {
        return when {
            isTestScope(artifact) -> DependencyScope.TEST
            GMavenConstants.SCOPE_RUNTIME == artifact.scope -> DependencyScope.RUNTIME
            GMavenConstants.SCOPE_PROVIDED == artifact.scope -> DependencyScope.PROVIDED
            else -> DependencyScope.COMPILE
        }
    }

    private fun isTestScope(artifact: MavenArtifact) =
        (GMavenConstants.SCOPE_TEST == artifact.scope || "tests".equals(artifact.classifier, ignoreCase = true)
                || "test-jar".equals(artifact.type, ignoreCase = true))

    private fun addModuleDependency(parentNode: DataNode<ModuleData>, targetModule: ModuleData) {
        val ownerModule = parentNode.data
        val data = ModuleDependencyData(ownerModule, targetModule)
        data.order = 1
        parentNode.createChild(ProjectKeys.MODULE_DEPENDENCY, data)
    }

    private fun getInternalModuleName(parentName: String?, moduleName: String): String {
        return if (parentName == null) moduleName else "$parentName.$moduleName"
    }

    private class ProjectResolverContext(val rootProjectPath: String, val ideaProjectPath: String)
}