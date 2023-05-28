package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.externalSystem.JavaModuleData
import com.intellij.externalSystem.JavaProjectData
import com.intellij.openapi.externalSystem.importing.ProjectResolverPolicy
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.model.project.dependencies.ProjectDependenciesImpl
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ProjectJdkNotFoundException
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver
import com.intellij.openapi.module.ModuleTypeManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.util.io.FileUtil
import com.intellij.pom.java.LanguageLevel
import com.intellij.util.containers.ContainerUtil
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.MODULE_PROP_BUILD_FILE
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.DependencyAnalyzerData
import ru.rzn.gmyasoedov.gmaven.project.importing.AnnotationProcessingData
import ru.rzn.gmyasoedov.gmaven.project.importing.DependencyGraphData
import ru.rzn.gmyasoedov.gmaven.server.GServerRequest
import ru.rzn.gmyasoedov.gmaven.server.firstRun
import ru.rzn.gmyasoedov.gmaven.server.getProjectModel
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings
import ru.rzn.gmyasoedov.serverapi.model.MavenArtifact
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.MavenProjectContainer
import ru.rzn.gmyasoedov.serverapi.model.MavenResult
import java.io.File
import java.nio.file.Path
import java.util.*

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
        settings ?: throw ExternalSystemException("settings is empty")
        val sdk = settings.jdkName?.let { ExternalSystemJdkUtil.getJdk(null, it) }
        if (isPreviewMode) {
            return getPreviewProjectDataNode(settings, id, projectPath, sdk, listener)
        }

        sdk ?: throw ProjectJdkNotFoundException() //InvalidJavaHomeException
        val mavenHome = getMavenHome(settings.distributionSettings)
        val request = GServerRequest(id, Path.of(projectPath), mavenHome, sdk, listener = listener, settings = settings)
        val projectModel = getProjectModel(request)
        return getProjectDataNode(projectPath, projectModel, settings)
    }

    private fun getPreviewProjectDataNode(
        settings: MavenExecutionSettings,
        id: ExternalSystemTaskId,
        projectPath: String,
        sdk: Sdk?,
        listener: ExternalSystemTaskNotificationListener
    ): DataNode<ProjectData> {
        val projectDataNode = getPreviewProjectDataNode(projectPath, settings)
        if (sdk != null && settings.distributionSettings.path != null) {
            val request = GServerRequest(
                id, Path.of(projectPath), settings.distributionSettings.path!!, sdk, listener = listener
            )
            firstRun(request)
        }
        return projectDataNode;
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
        val context = ProjectResolverContext(absolutePath, ideProjectPath, mavenResult, languageLevel, settings)

        val moduleDataByArtifactId = TreeMap<String, DataNode<ModuleData>>()
        val moduleNode = createModuleData(container, projectDataNode, context, moduleDataByArtifactId)

        for (childContainer in container.modules) {
            createModuleData(childContainer, moduleNode, context, moduleDataByArtifactId)
        }
        addDependencies(container, projectDataNode, moduleDataByArtifactId)
        //populateRemoteRepository(projectDataNode, mavenResult.settings)
        return projectDataNode
    }

    private fun addDependencies(
        container: MavenProjectContainer,
        projectNode: DataNode<ProjectData>,
        moduleDataByArtifactId: Map<String, DataNode<ModuleData>>
    ) {
        addDependencies(container.project, projectNode, moduleDataByArtifactId)
        for (childContainer in container.modules) {
            addDependencies(childContainer, projectNode, moduleDataByArtifactId)
        }
    }

    private fun addDependencies(
        project: MavenProject, projectNode: DataNode<ProjectData>,
        moduleDataByArtifactId: Map<String, DataNode<ModuleData>>
    ) {
        val moduleByMavenProject = moduleDataByArtifactId[project.id]!!
        var hasLibrary = false;
        for (artifact in project.resolvedArtifacts) {
            val moduleDataNodeByMavenArtifact = moduleDataByArtifactId[artifact.id]
            if (moduleDataNodeByMavenArtifact == null) {
                addLibrary(moduleByMavenProject, projectNode, artifact)
                if (!hasLibrary) hasLibrary = true
            } else {
                addModuleDependency(moduleByMavenProject, moduleDataNodeByMavenArtifact.data)
            }
        }
        if (hasLibrary) {
            moduleByMavenProject.createChild(ProjectKeys.DEPENDENCIES_GRAPH, ProjectDependenciesImpl())
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
        moduleData.publication = ProjectId(project.groupId, project.artifactId, project.version)
        moduleData.isInheritProjectCompileOutputPath = false

        moduleData.setCompileOutputPath(ExternalSystemSourceType.SOURCE, project.outputDirectory)
        moduleData.setCompileOutputPath(ExternalSystemSourceType.TEST, project.testOutputDirectory)
        moduleData.useExternalCompilerOutput(false)

        moduleData.setProperty(MODULE_PROP_BUILD_FILE, project.file.absolutePath)

        val moduleDataNode = parentDataNode.createChild(ProjectKeys.MODULE, moduleData)
        moduleDataByArtifactId.put(project.id, moduleDataNode)
        val contentRootData = ContentRootData(GMavenConstants.SYSTEM_ID, projectPath)
        moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRootData)
        storePath(project.sourceRoots, contentRootData, ExternalSystemSourceType.SOURCE)
        storePath(project.resourceRoots, contentRootData, ExternalSystemSourceType.RESOURCE)
        storePath(project.testSourceRoots, contentRootData, ExternalSystemSourceType.TEST)
        storePath(project.testResourceRoots, contentRootData, ExternalSystemSourceType.TEST_RESOURCE)
        contentRootData.storePath(ExternalSystemSourceType.EXCLUDED, project.buildDirectory)
        val compilerData = getCompilerData(project, context.mavenResult, context.projectLanguageLevel)
        val sourceLanguageLevel: LanguageLevel = compilerData.sourceLevel
        val targetBytecodeLevel: LanguageLevel = compilerData.targetLevel
        moduleDataNode.createChild(ModuleSdkData.KEY, ModuleSdkData(null))

        moduleDataNode.createChild(
            JavaModuleData.KEY, JavaModuleData(
                GMavenConstants.SYSTEM_ID, sourceLanguageLevel,
                targetBytecodeLevel.toJavaVersion().toFeatureString()
            )
        )
        populateAnnotationProcessorData(project, moduleDataNode)
        populateDependenciesTree(project, moduleDataNode)
        populateTasks(moduleDataNode, project, context.mavenResult.settings.localRepository?.let { Path.of(it) })
        if (parentDataNode.data is ModuleData) {
            for (childContainer in container.modules) {
                createModuleData(childContainer, moduleDataNode, context, moduleDataByArtifactId)
            }
        } else {
            populateProfiles(moduleDataNode, context.mavenResult.settings)
        }
        if (container.modules.isEmpty()) {
            moduleDataNode.createChild(
                DependencyAnalyzerData.KEY, DependencyAnalyzerData(GMavenConstants.SYSTEM_ID, project.artifactId)
            )
        }
        return moduleDataNode
    }

    private fun populateAnnotationProcessorData(project: MavenProject, moduleDataNode: DataNode<ModuleData>) {
        val annotationProcessorPaths = project.annotationProcessorPaths ?: return
        val data = AnnotationProcessingData
            .create(annotationProcessorPaths, emptyList(), project.buildDirectory, project.basedir)
        moduleDataNode.createChild(AnnotationProcessingData.KEY, data)

    }

    //todo стоить переделать чтобы не хранить их в модели?
    // а получать из запуска таска? по типу как в GradleDependencyAnalyzerContributor
    private fun populateDependenciesTree(project: MavenProject, moduleDataNode: DataNode<ModuleData>) {
        if (ContainerUtil.isEmpty(project.dependencyTree)) return
        val data = DependencyGraphData
            .create(project.dependencyTree, project.artifactId, project.groupId, project.version)
        moduleDataNode.createChild(DependencyGraphData.KEY, data)
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

    private fun addLibrary(
        parentNode: DataNode<ModuleData>, projectNode: DataNode<ProjectData>,
        artifact: MavenArtifact
    ) {
        val createLibrary = createLibrary(artifact)
        val libraryDependencyData = LibraryDependencyData(parentNode.data, createLibrary, LibraryLevel.PROJECT)
        libraryDependencyData.scope = getScope(artifact)
        libraryDependencyData.order = 2
        parentNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyData)
        if (libraryDependencyData.scope == DependencyScope.RUNTIME) {
            val libraryDependencyDataTest = LibraryDependencyData(parentNode.data, createLibrary, LibraryLevel.PROJECT)
            libraryDependencyDataTest.scope = DependencyScope.TEST
            libraryDependencyDataTest.order = 2
            parentNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyDataTest)
        }
        projectNode.createChild(ProjectKeys.LIBRARY, createLibrary)
    }

    private fun createLibrary(artifact: MavenArtifact): LibraryData {
        val library = LibraryData(GMavenConstants.SYSTEM_ID, artifact.id, !artifact.isResolved)
        library.artifactId = artifact.artifactId
        library.setGroup(artifact.groupId)
        library.version = artifact.version
        library.addPath(getLibraryPathType(artifact), artifact.file.absolutePath)
        return library;
    }

    private fun getLibraryPathType(artifact: MavenArtifact): LibraryPathType {
        return when {
            "javadoc".equals(artifact.classifier, ignoreCase = true) -> LibraryPathType.DOC
            "sources".equals(artifact.classifier, ignoreCase = true) -> LibraryPathType.SOURCE
            else -> LibraryPathType.BINARY
        }
    }

    private fun getScope(artifact: MavenArtifact): DependencyScope {
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

    private class ProjectResolverContext(
        val rootProjectPath: String,
        val ideaProjectPath: String,
        val mavenResult: MavenResult,
        val projectLanguageLevel: LanguageLevel,
        val settings: MavenExecutionSettings
    )
}