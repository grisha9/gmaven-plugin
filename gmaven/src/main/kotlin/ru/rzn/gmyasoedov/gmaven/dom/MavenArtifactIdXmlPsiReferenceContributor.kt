package ru.rzn.gmyasoedov.gmaven.dom

import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.patterns.XmlPatterns
import com.intellij.psi.*
import com.intellij.psi.PsiReferenceBase.Immediate
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import ru.rzn.gmyasoedov.gmaven.util.CachedModuleData
import ru.rzn.gmyasoedov.gmaven.util.CachedModuleDataService
import ru.rzn.gmyasoedov.gmaven.utils.MavenArtifactUtil
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.Path


class MavenArtifactIdXmlPsiReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        if (MavenUtils.pluginEnabled(MavenUtils.INTELLIJ_MAVEN_PLUGIN_ID)) return

        val pattern = XmlPatterns.xmlTag()
            .withName(MavenArtifactUtil.ARTIFACT_ID)
            .withParent(XmlPatterns.xmlTag().withName(MavenArtifactUtil.DEPENDENCY))
        registrar.registerReferenceProvider(
            pattern, MavenArtifactIdXmlPsiReferenceProvider(), PsiReferenceRegistrar.LOWER_PRIORITY
        )
    }
}

private class MavenArtifactIdXmlPsiReferenceProvider : PsiReferenceProvider() {
    private val currentModuleName = AtomicReference("")
    private val currentDependencies = AtomicReference<List<LibraryData>>(emptyList())

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val xmlTag = element as? XmlTag ?: return emptyArray()
        val file = element.containingFile as? XmlFile ?: return emptyArray()
        val moduleName = file.rootTag?.getSubTagText(MavenArtifactUtil.ARTIFACT_ID) ?: return emptyArray()
        if (xmlTag.name != MavenArtifactUtil.ARTIFACT_ID) return emptyArray()
        val value = ElementManipulators.getValueText(element)
        val range = ElementManipulators.getValueTextRange(element)
        val dataHolder = CachedModuleDataService.getCurrentData()
        val moduleData = dataHolder.modules.find { it.artifactId == value }
        return if (moduleData != null) {
            getModulePsiReferences(moduleData, element, xmlTag, range)
        } else {
            getLocalRepositoryPsiReferences(xmlTag, moduleName, value, range)
        }
    }

    private fun getModulePsiReferences(
        moduleData: CachedModuleData, element: XmlTag, xmlTag: XmlTag, range: TextRange
    ): Array<PsiReference> {
        val virtualFile = LocalFileSystem.getInstance()
            .findFileByNioFile(Path(moduleData.configPath)) ?: return emptyArray()
        val psiFile = PsiManager.getInstance(element.project).findFile(virtualFile) as? XmlFile ?: return emptyArray()
        val artifactIdTag = psiFile.rootTag?.subTags
            ?.find { it.name == MavenArtifactUtil.ARTIFACT_ID } ?: return emptyArray()
        return arrayOf(Immediate(xmlTag, range, true, artifactIdTag))
    }

    private fun getLocalRepositoryPsiReferences(
        xmlTag: XmlTag, moduleName: String, artifactId: String, range: TextRange
    ): Array<PsiReference> {
        val path = xmlTag.containingFile.parent?.virtualFile?.canonicalPath ?: return emptyArray()
        val settings = MavenSettings.getInstance(xmlTag.project).getLinkedProjectSettings(path) ?: return emptyArray()
        val localRepositoryPath = settings.localRepositoryPath ?: return emptyArray()
        val parentXml = xmlTag.parentTag ?: return emptyArray()
        val groupId = parentXml.getSubTagText(MavenArtifactUtil.GROUP_ID) ?: return emptyArray()
        val version = parentXml.getSubTagText(MavenArtifactUtil.VERSION) ?: ""
        var psiFile = getFilePsiReference(localRepositoryPath, groupId, artifactId, version, xmlTag)
        if (psiFile != null) return arrayOf(Immediate(xmlTag, range, true, psiFile))

        val groupAndVersion = getGroupIdAndVersionFromProjectStructure(
            parentXml.project, moduleName, artifactId, settings.externalProjectPath
        ) ?: return emptyArray()
        psiFile =
            getFilePsiReference(localRepositoryPath, groupAndVersion.first, artifactId, groupAndVersion.second, xmlTag)
        return if (psiFile != null) arrayOf(Immediate(xmlTag, range, true, psiFile)) else emptyArray()
    }

    private fun getFilePsiReference(
        localRepositoryPath: String, groupId: String, artifactId: String, version: String, xmlTag: XmlTag
    ): PsiFile? {
        return try {
            val artifactNioPathPom = MavenArtifactUtil
                .getArtifactNioPathPom(Path(localRepositoryPath), groupId, artifactId, version)
            val virtualFile = LocalFileSystem.getInstance()
                .findFileByNioFile(artifactNioPathPom) ?: return null
            PsiManager.getInstance(xmlTag.project).findFile(virtualFile) ?: return null
        } catch (e: Exception) {
            null
        }
    }

    private fun getGroupIdAndVersionFromProjectStructure(
        project: Project, moduleName: String, artifactId: String, projectPath: String
    ): Pair<String, String>? {
        if (currentModuleName.get() != moduleName) {
            val projectStructure = ProjectDataManager.getInstance()
                .getExternalProjectData(project, GMavenConstants.SYSTEM_ID, projectPath)
                ?.externalProjectStructure ?: return null
            val dependencyNodes = ExternalSystemApiUtil.findAll(projectStructure, ProjectKeys.MODULE)
                .find { it.data.moduleName == moduleName }
                ?.let { ExternalSystemApiUtil.findAll(it, ProjectKeys.LIBRARY_DEPENDENCY) }
                ?.map { it.data.target }
                ?: emptyList()
            currentModuleName.set(moduleName)
            currentDependencies.set(dependencyNodes)
        }

        val dependency = currentDependencies.get().find { it.artifactId == artifactId } ?: return null
        return if (dependency.groupId != null && dependency.version != null)
            dependency.groupId!! to dependency.version!! else null
    }
}
