@file:JvmName("ModuleDataConverter")

package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.externalSystem.JavaModuleData
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.util.io.FileUtil
import com.intellij.pom.java.LanguageLevel
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.MODULE_PROP_BUILD_FILE
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.CompilerData
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectResolver.ModuleContextHolder
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.DependencyAnalyzerData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MavenContentRoot
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MavenGeneratedContentRoot
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
    moduleData.setProperty(MODULE_PROP_BUILD_FILE, MavenUtils.getBuildFilePath(project.file.absolutePath))

    val moduleDataNode = parentDataNode.createChild(ProjectKeys.MODULE, moduleData)

    val rootPaths = listOf(
        getContentRootPath(project.sourceRoots, ExternalSystemSourceType.SOURCE),
        getContentRootPath(project.resourceRoots, ExternalSystemSourceType.RESOURCE),
        getContentRootPath(project.testSourceRoots, ExternalSystemSourceType.TEST),
        getContentRootPath(project.testResourceRoots, ExternalSystemSourceType.TEST_RESOURCE),
        getPluginContentRootPaths(project)
    ).flatten()
    val generatedPaths = getGeneratedSources(project)
    val contentRoot = ContentRoots(rootPaths, generatedPaths, project.buildDirectory)

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

    populateAnnotationProcessorData(project, moduleDataNode, compilerData)
    populateTasks(moduleDataNode, project, context.mavenResult.settings.localRepository?.let { Path.of(it) })

    val perSourceSetModules = setupContentRootAndSourceSetData(moduleDataNode, contentRoot, context, project, compilerData)
    context.moduleDataByArtifactId.put(project.id, ModuleContextHolder(moduleDataNode, perSourceSetModules))

    if (parentDataNode.data is ModuleData) {
        for (childContainer in container.modules) {
            createModuleData(childContainer, moduleDataNode, context)
        }
    }
    return moduleDataNode
}

private fun setupContentRootAndSourceSetData(
    moduleDataNode: DataNode<ModuleData>,
    contentRoot: ContentRoots,
    context: MavenProjectResolver.ProjectResolverContext,
    project: MavenProject,
    compilerData: CompilerData,
): MavenProjectResolver.PerSourceSetModules? {
    val perSourceSet = MavenUtils.isPerSourceSet(context.settings, project, compilerData)
    if (!perSourceSet) {
        val contentRootData = createContentRootData(contentRoot, project.basedir)
        contentRootData.forEach { moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, it) }
        return null
    }

    val mainModuleContentRoots = ContentRoots(emptyList(), emptyList(), contentRoot.excludedPath)
    val contentRootData = createContentRootData(mainModuleContentRoots, project.basedir)
    contentRootData.forEach { moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, it) }

    moduleDataNode.data.isInheritProjectCompileOutputPath = true
    val mainSourceSetModule = createSourceSetModule(moduleDataNode, project, compilerData, contentRoot, false)
    val testSourceSetModule = createSourceSetModule(moduleDataNode, project, compilerData, contentRoot, true)
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
    contentRoots: ContentRoots,
    isTest: Boolean
): DataNode<SourceSetData> {
    val mainModuleData = mainModuleNode.data
    val moduleSuffix = if (isTest) TEST else MAIN
    val id = mainModuleData.id + ":" + moduleSuffix
    val externalName = project.artifactId + ":" + moduleSuffix
    val internalName = getInternalModuleName(mainModuleData.internalName, moduleSuffix)
    val data = SourceSetData(id, externalName, internalName, mainModuleData.moduleFileDirectoryPath, project.basedir)
    val moduleNode = mainModuleNode.createChild(SourceSetData.KEY, data)
    val currentContentRoots = splitContentRoots(contentRoots, isTest)

    data.isInheritProjectCompileOutputPath = false
    data.useExternalCompilerOutput(false)
    data.setProperty(MODULE_PROP_BUILD_FILE, MavenUtils.getBuildFilePath(project.file.absolutePath))

    val basePath = Path.of(project.basedir)
    val rootModulePath = basePath.resolve("src").resolve(moduleSuffix).toString()
    val contentRootData = createContentRootData(currentContentRoots, rootModulePath)
    contentRootData.forEach { moduleNode.createChild(ProjectKeys.CONTENT_ROOT, it) }

    if (isTest) {
        data.setCompileOutputPath(ExternalSystemSourceType.TEST, project.testOutputDirectory)
    } else {
        data.setCompileOutputPath(ExternalSystemSourceType.SOURCE, project.outputDirectory)
    }

    val sourceLevel = if (isTest) compilerData.testSourceLevel else compilerData.sourceLevel
    val targetLevel = (if (isTest) compilerData.testTargetLevel else compilerData.targetLevel)
        .toJavaVersion().toFeatureString()
    moduleNode.createChild(JavaModuleData.KEY, JavaModuleData(SYSTEM_ID, sourceLevel, targetLevel))
    moduleNode.createChild(ModuleSdkData.KEY, ModuleSdkData(null))
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

private fun createContentRootData(contentRoots: ContentRoots, rootPath: String): List<ContentRootData> {
    val roots = mutableListOf(ContentRootData(SYSTEM_ID, rootPath))
    if (contentRoots.excludedPath != null) {
        addExcludedContentRoot(roots, contentRoots.excludedPath)
    }
    for (contentRoot in contentRoots.paths) {
        addContentRoot(roots, contentRoot.type, contentRoot.path)
    }
    for (generatedContentRoot in contentRoots.generatedPaths) {
        addContentRoot(roots, generatedContentRoot)
    }
    return roots
}

private fun addContentRoot(roots: MutableList<ContentRootData>, type: ExternalSystemSourceType, path: String) {
    var errorOnStorePath = false
    for (root in roots) {
        try {
            root.storePath(type, path)
            return
        } catch (e: IllegalArgumentException) {
            errorOnStorePath = true
        }
    }
    if (errorOnStorePath) {
        val contentRootData = ContentRootData(SYSTEM_ID, path)
        contentRootData.storePath(type, path)
        roots.add(contentRootData)
    }
}

private fun addContentRoot(roots: MutableList<ContentRootData>, generatedContentRoot: MavenGeneratedContentRoot) {
    var errorOnStorePath = false
    var added = false
    for (root in roots) {
        for (path in generatedContentRoot.paths) {
            try {
                root.storePath(generatedContentRoot.type, path)
                added = true
            } catch (e: IllegalArgumentException) {
                errorOnStorePath = true
            }
        }
        if (added) return
    }
    if (errorOnStorePath) {
        val contentRootData = ContentRootData(SYSTEM_ID, generatedContentRoot.rootPath.toString())
        generatedContentRoot.paths.forEach { contentRootData.storePath(generatedContentRoot.type, it) }
        roots.add(contentRootData)
    }
}

private fun addExcludedContentRoot(roots: MutableList<ContentRootData>, path: String) {
    var errorOnStorePath = false
    for (root in roots) {
        try {
            val sizeBefore = root.getPaths(ExternalSystemSourceType.EXCLUDED).size
            root.storePath(ExternalSystemSourceType.EXCLUDED, path)
            val sizeAfter = root.getPaths(ExternalSystemSourceType.EXCLUDED).size
            if (sizeBefore == sizeAfter) throw IllegalArgumentException()
            return
        } catch (e: IllegalArgumentException) {
            errorOnStorePath = true
        }
    }
    if (errorOnStorePath) {
        val contentRootData = ContentRootData(SYSTEM_ID, path)
        contentRootData.storePath(ExternalSystemSourceType.EXCLUDED, path)
        roots.add(contentRootData)
    }
}


private fun splitContentRoots(contentRoots: ContentRoots, test: Boolean): ContentRoots {
    return ContentRoots(
        contentRoots.paths.filter { it.type.isTest == test },
        contentRoots.generatedPaths.filter { it.type.isTest == test },
        null
    )
}

private fun getGeneratedSources(project: MavenProject): List<MavenGeneratedContentRoot> {
    if (project.packaging.equals("pom", true)) return emptyList()
    val generatedSourcePath = MavenUtils.getGeneratedSourcesDirectory(project.buildDirectory, false)
    val generatedTestSourcePath = MavenUtils.getGeneratedSourcesDirectory(project.buildDirectory, true)
    return listOfNotNull(
        getGeneratedContentRoot(generatedSourcePath, ExternalSystemSourceType.SOURCE_GENERATED),
        getGeneratedContentRoot(generatedTestSourcePath, ExternalSystemSourceType.TEST_GENERATED),
    )
}

private fun getGeneratedContentRoot(
    rootPath: Path,
    type: ExternalSystemSourceType
): MavenGeneratedContentRoot? {
    val listFiles = rootPath.toFile().listFiles() ?: return null
    val paths = listFiles.asSequence().filter { it.isDirectory }.map { it.absolutePath }.toList()
    return MavenGeneratedContentRoot(type, rootPath, paths)
}

private class ContentRoots(
    val paths: List<MavenContentRoot>,
    val generatedPaths: List<MavenGeneratedContentRoot>,
    val excludedPath: String?
)