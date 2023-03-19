// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.project.importing

import com.intellij.compiler.CompilerConfiguration
import com.intellij.compiler.CompilerConfigurationImpl
import com.intellij.ide.projectView.actions.MarkRootActionBase
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.service.project.manage.SourceFolderManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.java.compiler.ProcessorConfigProfile
import org.jetbrains.jps.model.java.impl.compiler.ProcessorConfigProfileImpl
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import java.io.File
import java.nio.file.Path

@Order(ExternalSystemConstants.UNORDERED)
class AnnotationProcessingDataService : AbstractProjectDataService<AnnotationProcessingData, ProcessorConfigProfile>() {

    override fun getTargetDataKey(): Key<AnnotationProcessingData> {
        return AnnotationProcessingData.KEY
    }

    override fun importData(
        toImport: Collection<DataNode<AnnotationProcessingData>>,
        projectData: ProjectData?,
        project: Project,
        modifiableModelsProvider: IdeModifiableModelsProvider
    ) {
        val importedData = mutableSetOf<AnnotationProcessingData>()
        val config = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
        val sourceFolderManager = SourceFolderManager.getInstance(project)
        for (node in toImport) {
            val moduleData = node.parent?.data as? ModuleData
            if (moduleData == null) {
                LOG.debug("Failed to find parent module data in annotation processor data. Parent: ${node.parent} ")
                continue
            }

            val ideModule = modifiableModelsProvider.findIdeModule(moduleData)
            if (ideModule == null) {
                LOG.debug("Failed to find ide module for module data: ${moduleData}")
                continue
            }

            config.configureAnnotationProcessing(ideModule, node.data, importedData)

            if (projectData != null) {
                clearGeneratedSourceFolders(ideModule, node, modifiableModelsProvider)
                addGeneratedSourceFolders(
                    ideModule,
                    node,
                    modifiableModelsProvider,
                    sourceFolderManager
                )
            }
        }
    }

    private fun clearGeneratedSourceFolders(
        ideModule: Module,
        node: DataNode<AnnotationProcessingData>,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        val moduleOutputs = ExternalSystemApiUtil.findAll(node, AnnotationProcessingData.OUTPUT_KEY)
        val buildDirectory = node.data.buildDirectory
        val pathsToRemove =
            (moduleOutputs
                .map { it.data.outputPath } +
                    listOf(
                        MavenUtils.getGeneratedSourcesDirectory(buildDirectory, false).toString(),
                        MavenUtils.getGeneratedSourcesDirectory(buildDirectory, true).toString(),
                    ))
                .filterNotNull()

        pathsToRemove.forEach { path ->
            val url = VfsUtilCore.pathToUrl(path)
            val modifiableRootModel = modelsProvider.getModifiableRootModel(ideModule)

            val (entry, folder) = findContentEntryOrFolder(modifiableRootModel, url)

            if (entry != null) {
                if (folder != null) {
                    entry.removeSourceFolder(folder)
                }

                if (entry.sourceFolders.isEmpty()) {
                    modifiableRootModel.removeContentEntry(entry)
                }
            }
        }
    }

    private fun addGeneratedSourceFolders(
        ideModule: Module,
        node: DataNode<AnnotationProcessingData>,
        modelsProvider: IdeModifiableModelsProvider,
        sourceFolderManager: SourceFolderManager
    ) {
        val buildDirectory = node.data.buildDirectory
        val outputPath = MavenUtils.getGeneratedSourcesDirectory(buildDirectory, false).toString();
        if (outputPath != null) {
            addGeneratedSourceFolder(ideModule, outputPath, false, modelsProvider, sourceFolderManager)
        }

        val testOutputPath = MavenUtils.getGeneratedSourcesDirectory(buildDirectory, true).toString()
        if (testOutputPath != null) {
            addGeneratedSourceFolder(ideModule, testOutputPath, true, modelsProvider, sourceFolderManager)
        }
    }


    private fun addGeneratedSourceFolder(
        ideModule: Module,
        path: String,
        isTest: Boolean,
        modelsProvider: IdeModifiableModelsProvider,
        sourceFolderManager: SourceFolderManager
    ) {
        val type = if (isTest) JavaSourceRootType.TEST_SOURCE else JavaSourceRootType.SOURCE
        val url = VfsUtilCore.pathToUrl(path)
        val vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(path)
        if (vf == null || !vf.exists()) {
            sourceFolderManager.addSourceFolder(ideModule, url, type)
            sourceFolderManager.setSourceFolderGenerated(url, true)
        } else {
            val modifiableRootModel = modelsProvider.getModifiableRootModel(ideModule)
            val contentEntry = MarkRootActionBase.findContentEntry(modifiableRootModel, vf)
                ?: modifiableRootModel.addContentEntry(url)
            val properties = JpsJavaExtensionService.getInstance().createSourceRootProperties("", true)
            contentEntry.addSourceFolder(url, type, properties)
        }
    }

    private fun findContentEntryOrFolder(
        modifiableRootModel: ModifiableRootModel,
        url: String
    ): Pair<ContentEntry?, SourceFolder?> {
        var entryVar: ContentEntry? = null
        var folderVar: SourceFolder? = null
        modifiableRootModel.contentEntries.forEach search@{ ce ->
            ce.sourceFolders.forEach { sf ->
                if (sf.url == url) {
                    entryVar = ce
                    folderVar = sf
                    return@search
                }
            }
            if (ce.url == url) {
                entryVar = ce
            }
        }
        return entryVar to folderVar
    }

    override fun computeOrphanData(
        toImport: Collection<DataNode<AnnotationProcessingData>>,
        projectData: ProjectData,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ): Computable<Collection<ProcessorConfigProfile>> =
        Computable {
            val profiles =
                ArrayList((CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl).moduleProcessorProfiles)
            val importedProcessingProfiles = ArrayList(toImport).asSequence()
                .map { it.data }
                .distinct()
                .map { createProcessorConfigProfile(it) }
                .toList()

            val orphans = profiles
                .filter {
                    it.moduleNames.all { configuredModule -> isGMavenModule(configuredModule, modelsProvider) }
                            && importedProcessingProfiles.none { imported -> imported.matches(it) }
                }
                .toMutableList()

            orphans
        }

    private fun isGMavenModule(moduleName: String, modelsProvider: IdeModifiableModelsProvider): Boolean {
        return ExternalSystemApiUtil.isExternalSystemAwareModule(
            SYSTEM_ID,
            modelsProvider.findIdeModule(moduleName)
        )
    }

    override fun removeData(
        toRemoveComputable: Computable<out Collection<ProcessorConfigProfile>>,
        toIgnore: Collection<DataNode<AnnotationProcessingData>>,
        projectData: ProjectData,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        val toRemove = toRemoveComputable.compute()
        val config = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
        val newProfiles = config
            .moduleProcessorProfiles
            .toMutableList()
            .apply { removeAll(toRemove) }
        config.setModuleProcessorProfiles(newProfiles)
    }

    private fun CompilerConfigurationImpl.configureAnnotationProcessing(
        ideModule: Module,
        data: AnnotationProcessingData,
        importedData: MutableSet<AnnotationProcessingData>
    ) {
        val profile = findOrCreateProcessorConfigProfile(data)
        if (importedData.add(data)) {
            profile.clearModuleNames()
        }

        with(profile) {
            isEnabled = true
            isObtainProcessorsFromClasspath = false
            isOutputRelativeToContentRoot = true
            addModuleName(ideModule.name)
            setGeneratedSourcesDirectoryName(getRelativePath(data, false), false)
            setGeneratedSourcesDirectoryName(getRelativePath(data, true), true)
        }
    }

    private fun CompilerConfigurationImpl.findOrCreateProcessorConfigProfile(data: AnnotationProcessingData): ProcessorConfigProfile {
        val newProfile = createProcessorConfigProfile(data)
        return ArrayList(this.moduleProcessorProfiles)
            .find { existing -> existing.matches(newProfile) }
            ?: newProfile.also { addModuleProcessorProfile(it) }
    }

    private fun createProcessorConfigProfile(annotationProcessingData: AnnotationProcessingData): ProcessorConfigProfileImpl {
        val newProfile = ProcessorConfigProfileImpl(IMPORTED_PROFILE_NAME)
        newProfile.setProcessorPath(annotationProcessingData.path.joinToString(separator = File.pathSeparator))
        annotationProcessingData.arguments
            .map { it.removePrefix("-A").split('=', limit = 2) }
            .forEach { newProfile.setOption(it[0], if (it.size > 1) it[1] else "") }
        return newProfile
    }

    private fun ProcessorConfigProfile.matches(other: ProcessorConfigProfile): Boolean {
        return this.name == other.name
                && this.processorPath == other.processorPath
                && this.processorOptions == other.processorOptions
    }

    private fun getRelativePath(data: AnnotationProcessingData, isTest: Boolean): String? {
        val annotationProcessorDirectoryFile = MavenUtils.getGeneratedAnnotationsDirectory(data.buildDirectory, isTest)
        return try {
            Path.of(data.baseDirectory).relativize(annotationProcessorDirectoryFile).toString()
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    companion object {
        private val LOG = Logger.getInstance(AnnotationProcessingDataService::class.java)
        const val IMPORTED_PROFILE_NAME = "GMaven Imported"
    }
}
