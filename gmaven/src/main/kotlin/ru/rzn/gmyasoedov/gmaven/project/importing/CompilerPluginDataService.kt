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
import ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.PROC_NONE
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.CompilerPluginData
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import java.io.File
import java.nio.file.Path

@Order(ExternalSystemConstants.UNORDERED)
class CompilerPluginDataService : AbstractProjectDataService<CompilerPluginData, ProcessorConfigProfile>() {

    override fun getTargetDataKey(): Key<CompilerPluginData> {
        return CompilerPluginData.KEY
    }

    override fun importData(
        toImport: Collection<DataNode<CompilerPluginData>>,
        projectData: ProjectData?,
        project: Project,
        modifiableModelsProvider: IdeModifiableModelsProvider
    ) {
        val importedData = mutableSetOf<CompilerPluginData>()
        val config = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
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
            config.setAdditionalOptions(ideModule, ArrayList(node.data.arguments))
        }
    }

    override fun computeOrphanData(
        toImport: Collection<DataNode<CompilerPluginData>>,
        projectData: ProjectData,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ): Computable<Collection<ProcessorConfigProfile>> =
        Computable {
            val compilerConfiguration = CompilerConfiguration.getInstance(project)
            val gProfiles =
                ArrayList(( compilerConfiguration as CompilerConfigurationImpl).moduleProcessorProfiles)
                    .filter { it.name.equals(IMPORTED_PROFILE_NAME) }
            val importedProcessingProfiles = ArrayList(toImport).asSequence()
                .map { it.data }
                .distinct()
                .map { createProcessorConfigProfile(it) }
                .toList()

            val orphans = gProfiles
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
        toIgnore: Collection<DataNode<CompilerPluginData>>,
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
        data: CompilerPluginData,
        importedData: MutableSet<CompilerPluginData>
    ) {
        if (data.path.isEmpty()) return
        val profile = findOrCreateProcessorConfigProfile(data, ideModule) ?: return
        if (importedData.add(data)) {
            profile.clearModuleNames()
        }
        val profileEnabled = !data.arguments.contains(PROC_NONE)
        with(profile) {
            isEnabled = profileEnabled
            isObtainProcessorsFromClasspath = false
            isOutputRelativeToContentRoot = true
            addModuleName(ideModule.name)
            setGeneratedSourcesDirectoryName(getRelativePath(data, false), false)
            setGeneratedSourcesDirectoryName(getRelativePath(data, true), true)
        }
    }

    private fun CompilerConfigurationImpl.findOrCreateProcessorConfigProfile(
        data: CompilerPluginData,
        ideModule: Module
    ): ProcessorConfigProfile? {
        val moduleExistInUserProfile = this.moduleProcessorProfiles.asSequence()
            .filter { !it.name.equals(IMPORTED_PROFILE_NAME) }
            .flatMap { it.moduleNames }
            .find { it.equals(ideModule.name) }
        if (moduleExistInUserProfile != null) return null

        val newProfile = createProcessorConfigProfile(data)
        val find = this.moduleProcessorProfiles
            .find { it.matches(newProfile) }
        return find ?: newProfile.also { addModuleProcessorProfile(it) }
    }

    private fun createProcessorConfigProfile(compilerPluginData: CompilerPluginData): ProcessorConfigProfileImpl {
        val newProfile = ProcessorConfigProfileImpl(IMPORTED_PROFILE_NAME)
        newProfile.setProcessorPath(compilerPluginData.path.joinToString(separator = File.pathSeparator))
        return newProfile
    }

    private fun ProcessorConfigProfile.matches(other: ProcessorConfigProfile): Boolean {
        return this.name == other.name
                && this.processorPath == other.processorPath
                && this.processorOptions == other.processorOptions
    }

    private fun getRelativePath(data: CompilerPluginData, isTest: Boolean): String? {
        val annotationProcessorDirectoryFile = MavenUtils.getGeneratedAnnotationsDirectory(data.buildDirectory, isTest)
        return try {
            Path.of(data.baseDirectory).relativize(annotationProcessorDirectoryFile).toString()
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    companion object {
        private val LOG = Logger.getInstance(CompilerPluginDataService::class.java)
        const val IMPORTED_PROFILE_NAME = "GMaven Imported"
    }
}
