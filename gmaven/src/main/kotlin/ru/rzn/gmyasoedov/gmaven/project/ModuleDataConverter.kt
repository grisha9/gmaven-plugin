@file:JvmName("ModuleDataConverter")

package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.externalSystem.JavaModuleData
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.util.io.FileUtil
import com.intellij.pom.java.LanguageLevel
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.CompilerData
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectResolver.ModuleContextHolder
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.DependencyAnalyzerData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.SourceSetData
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import ru.rzn.gmyasoedov.serverapi.model.MavenProject
import ru.rzn.gmyasoedov.serverapi.model.MavenProjectContainer
import java.io.File
import java.nio.file.Path

private const val MAIN = "main"
private const val TEST = "test"

fun createModuleData(
    container: MavenProjectContainer,
    parentDataNode: DataNode<*>,
    context: MavenProjectResolver.ProjectResolverContext
): DataNode<ModuleData> {
    val project = container.project
    val parentExternalName = getModuleName(parentDataNode.data, true)
    val parentInternalName = getModuleName(parentDataNode.data, false)
    val id = if (parentExternalName == null) project.artifactId else ":" + project.artifactId
    val projectPath = project.basedir
    val moduleFileDirectoryPath: String = getIdeaModulePath(context, projectPath)
    val moduleData = ModuleData(
        id, SYSTEM_ID, getDefaultModuleTypeId(), project.artifactId, moduleFileDirectoryPath, projectPath
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
    moduleData.setProperty(GMavenConstants.MODULE_PROP_BUILD_FILE, project.file.absolutePath)

    val moduleDataNode = parentDataNode.createChild(ProjectKeys.MODULE, moduleData)

    val contentRootData = ContentRootData(SYSTEM_ID, projectPath)
    storePath(project.sourceRoots, contentRootData, ExternalSystemSourceType.SOURCE)
    storePath(project.resourceRoots, contentRootData, ExternalSystemSourceType.RESOURCE)
    storePath(project.testSourceRoots, contentRootData, ExternalSystemSourceType.TEST)
    storePath(project.testResourceRoots, contentRootData, ExternalSystemSourceType.TEST_RESOURCE)
    addGeneratedSources(project, contentRootData)
    contentRootData.storePath(ExternalSystemSourceType.EXCLUDED, project.buildDirectory)

    val compilerData = getCompilerData(project, context)
    val sourceLanguageLevel: LanguageLevel = compilerData.sourceLevel
    val targetBytecodeLevel: LanguageLevel = compilerData.targetLevel
    moduleDataNode.createChild(ModuleSdkData.KEY, ModuleSdkData(null))

    val targetBytecodeVersion = targetBytecodeLevel.toJavaVersion().toFeatureString()
    moduleDataNode.createChild(
        JavaModuleData.KEY, JavaModuleData(SYSTEM_ID, sourceLanguageLevel, targetBytecodeVersion)
    )

    if (!MavenUtils.isPomProject(project)) {
        moduleDataNode.createChild(DependencyAnalyzerData.KEY, DependencyAnalyzerData(SYSTEM_ID, project.artifactId))
    }

    applyPlugins(project, moduleData, contentRootData)
    populateAnnotationProcessorData(project, moduleDataNode, compilerData)
    populateTasks(moduleDataNode, project, context.mavenResult.settings.localRepository?.let { Path.of(it) })

    val perSourceSetModules = setupSourceSetData(moduleDataNode, contentRootData, context, project, compilerData)
    context.moduleDataByArtifactId.put(project.id, ModuleContextHolder(moduleDataNode, perSourceSetModules))

    if (parentDataNode.data is ModuleData) {
        for (childContainer in container.modules) {
            createModuleData(childContainer, moduleDataNode, context)
        }
    } else {
        populateProfiles(moduleDataNode, context.mavenResult.settings)
    }
    return moduleDataNode
}

private fun setupSourceSetData(
    moduleDataNode: DataNode<ModuleData>,
    contentRootData: ContentRootData,
    context: MavenProjectResolver.ProjectResolverContext,
    project: MavenProject,
    compilerData: CompilerData,
): MavenProjectResolver.PerSourceSetModules? {
    val perSourceSet = MavenUtils.isPerSourceSet(context.settings, project, compilerData)
    if (!perSourceSet) {
        moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRootData)
        return null
    }

    val mainPaths = ArrayList<Pair<ExternalSystemSourceType, String>>()
    val testPaths = ArrayList<Pair<ExternalSystemSourceType, String>>()
    val excludedPaths = ArrayList<Pair<ExternalSystemSourceType, String>>()
    for (type in ExternalSystemSourceType.values()) {
        for (path in contentRootData.getPaths(type)) {
            when {
                type.isTest -> testPaths.add(type to path.path)
                type.isExcluded -> excludedPaths.add(type to path.path)
                else -> mainPaths.add(type to path.path)
            }
        }
    }
    val mainContentRootData = ContentRootData(SYSTEM_ID, contentRootData.rootPath)
    excludedPaths.forEach { mainContentRootData.storePath(it.first, it.second) }
    moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, mainContentRootData)
    moduleDataNode.data.isInheritProjectCompileOutputPath = true
    val mainSourceSetModule = createSourceSetModule(moduleDataNode, project, compilerData, mainPaths, false)
    val testSourceSetModule = createSourceSetModule(moduleDataNode, project, compilerData, testPaths, true)
    setDependTestOnMain(testSourceSetModule, mainSourceSetModule)
    return MavenProjectResolver.PerSourceSetModules(mainSourceSetModule, testSourceSetModule)
}

private fun setDependTestOnMain(
    testSourceSetModule: DataNode<SourceSetData>,
    mainSourceSetModule: DataNode<SourceSetData>
) {
    val ownerModule = testSourceSetModule.data
    val data = ModuleDependencyData(ownerModule, mainSourceSetModule.data)
    data.order = 1
    testSourceSetModule.createChild(ProjectKeys.MODULE_DEPENDENCY, data)
}

private fun createSourceSetModule(
    mainModuleNode: DataNode<ModuleData>,
    project: MavenProject,
    compilerData: CompilerData,
    paths: ArrayList<Pair<ExternalSystemSourceType, String>>,
    isTest: Boolean
): DataNode<SourceSetData> {
    val mainModuleData = mainModuleNode.data
    val moduleSuffix = if (isTest) TEST else MAIN
    val id = mainModuleData.id + ":" + moduleSuffix;
    val externalName = project.artifactId + ":" + moduleSuffix
    val internalName = getInternalModuleName(mainModuleData.internalName, moduleSuffix)
    val data = SourceSetData(id, externalName, internalName, mainModuleData.moduleFileDirectoryPath, project.basedir)
    val moduleNode = mainModuleNode.createChild(SourceSetData.KEY, data)

    data.isInheritProjectCompileOutputPath = false
    data.useExternalCompilerOutput(false)
    data.setProperty(GMavenConstants.MODULE_PROP_BUILD_FILE, project.file.absolutePath)

    //todo basedir check for gradle CommonGradleProjectResolverExtension#addExternalProjectContentRoots
    val basePath = Path.of(project.basedir)
    val contentRootData = ContentRootData(SYSTEM_ID, basePath.resolve("src").resolve(moduleSuffix).toString())
    paths.forEach { storePath(contentRootData, it.first, it.second) }
    if (isTest) {
        data.setCompileOutputPath(ExternalSystemSourceType.TEST, project.testOutputDirectory)
    } else {
        data.setCompileOutputPath(ExternalSystemSourceType.SOURCE, project.outputDirectory)
    }

    moduleNode.createChild(ProjectKeys.CONTENT_ROOT, contentRootData)
    moduleNode.createChild(ModuleSdkData.KEY, ModuleSdkData(null))


    val sourceLevel = if (isTest) compilerData.testSourceLevel else compilerData.sourceLevel
    val targetLevel = (if (isTest) compilerData.testTargetLevel else compilerData.targetLevel)
        .toJavaVersion().toFeatureString()
    moduleNode.createChild(JavaModuleData.KEY, JavaModuleData(SYSTEM_ID, sourceLevel, targetLevel))
    return moduleNode
}

private fun getModuleName(data: Any, external: Boolean): String? {
    return if (data is ModuleData) {
        if (external) data.externalName else data.internalName
    } else null
}

private fun getInternalModuleName(parentName: String?, moduleName: String): String {
    return if (parentName == null) moduleName else "$parentName.$moduleName"
}

private fun getIdeaModulePath(context: MavenProjectResolver.ProjectResolverContext, projectPath: String): String {
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

private fun addGeneratedSources(
    project: MavenProject,
    contentRootData: ContentRootData,
    isTest: Boolean? = null,
) {
    if (project.packaging.equals("pom", true)) return;
    val generatedSourcePath = MavenUtils.getGeneratedSourcesDirectory(project.buildDirectory, false)
    val generatedTestSourcePath = MavenUtils.getGeneratedSourcesDirectory(project.buildDirectory, true)
    if (isTest == null) {
        storeGeneratedPaths(
            generatedSourcePath.toFile().listFiles(), contentRootData, ExternalSystemSourceType.SOURCE_GENERATED
        )
        storeGeneratedPaths(
            generatedTestSourcePath.toFile().listFiles(), contentRootData, ExternalSystemSourceType.TEST_GENERATED
        )
    } else if (isTest) {
        storeGeneratedPaths(
            generatedTestSourcePath.toFile().listFiles(), contentRootData, ExternalSystemSourceType.TEST_GENERATED
        )
    } else {
        storeGeneratedPaths(
            generatedSourcePath.toFile().listFiles(), contentRootData, ExternalSystemSourceType.SOURCE_GENERATED
        )
    }
}

private fun storeGeneratedPaths(
    listFiles: Array<File>?,
    contentRootData: ContentRootData,
    sourceGenerated: ExternalSystemSourceType
) {
    if (listFiles != null) {
        val paths = listFiles.filter { it.isDirectory }.map { it.absolutePath }
        storePath(paths, contentRootData, sourceGenerated)
    }
}