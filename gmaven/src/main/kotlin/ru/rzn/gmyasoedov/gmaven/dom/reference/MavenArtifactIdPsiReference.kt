package ru.rzn.gmyasoedov.gmaven.dom.reference

import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.dom.XmlPsiUtil
import ru.rzn.gmyasoedov.gmaven.settings.MavenProjectSettings
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import java.nio.file.Path
import java.util.*

class MavenArtifactIdPsiReference(
    private val artifactId: String,
    private val xmlTag: XmlTag,
    private val textRange: TextRange,
    private val moduleName: String
) : PsiReference {

    override fun getElement() = xmlTag

    override fun getRangeInElement() = textRange

    override fun resolve() = doResolve()

    override fun getCanonicalText() = artifactId

    override fun handleElementRename(newElementName: String) = xmlTag

    override fun bindToElement(element: PsiElement): PsiElement? = null

    override fun isReferenceTo(element: PsiElement) = getElement().manager.areElementsEquivalent(element, resolve())

    override fun isSoft() = true

    private fun doResolve(): PsiElement? {
        val xmlFile = xmlTag.containingFile as? XmlFile ?: return null
        val dependencyTag = xmlTag.parentTag ?: return null
        val settings = MavenSettings.getInstance(xmlTag.project).linkedProjectsSettings
        val repos = settings.mapNotNull { it.localRepositoryPath }

        val propertiesMap = TreeMap<String, XmlTag>()
        XmlPsiUtil.fillProperties(xmlFile, propertiesMap, repos)
        val properties = propertiesMap.asSequence().map { it.key to it.value.value.text }.toMap()

        val psiFileReference = XmlPsiUtil.getParentPath(dependencyTag, repos, properties)
            ?.let { getFilePsiReference(it) }
        if (psiFileReference != null) return psiFileReference
        val groupAndVersion = getGroupIdAndVersionFromProjectStructure(
            xmlTag.project, moduleName, artifactId, settings
        ) ?: return null
        return XmlPsiUtil.searchInLocalRepo(groupAndVersion.first, artifactId, groupAndVersion.second, repos)
            ?.let { getFilePsiReference(it) }
    }

    private fun getGroupIdAndVersionFromProjectStructure(
        project: Project, moduleName: String, artifactId: String, settings: Collection<MavenProjectSettings>
    ): Pair<String, String>? {
        for (each in settings) {
            val result = getGroupIdAndVersionFromProjectStructure(
                project, moduleName, artifactId, each.externalProjectPath
            )
            if (result != null) return result
        }
        return null
    }

    private fun getGroupIdAndVersionFromProjectStructure(
        project: Project, moduleName: String, artifactId: String, projectPath: String
    ): Pair<String, String>? {
        val libraryData = getDependencyNodes(project, moduleName, projectPath)
        val dependency = libraryData.find { it.artifactId == artifactId } ?: return null
        return if (dependency.groupId != null && dependency.version != null)
            dependency.groupId!! to dependency.version!! else null
    }

    private fun getDependencyNodes(
        project: Project, moduleName: String, projectPath: String
    ): List<LibraryData> {
        val projectStructure = ProjectDataManager.getInstance()
            .getExternalProjectData(project, GMavenConstants.SYSTEM_ID, projectPath)
            ?.externalProjectStructure ?: return emptyList()
        return ExternalSystemApiUtil.findAll(projectStructure, ProjectKeys.MODULE)
            .find { it.data.moduleName == moduleName }
            ?.let { ExternalSystemApiUtil.findAll(it, ProjectKeys.LIBRARY_DEPENDENCY) }
            ?.mapNotNull { it.data.target }
            ?: emptyList()
    }

    private fun getFilePsiReference(artifactNioPathPom: Path): PsiFile? {
        return try {
            val virtualFile = LocalFileSystem.getInstance()
                .findFileByNioFile(artifactNioPathPom) ?: return null
            PsiManager.getInstance(xmlTag.project).findFile(virtualFile) ?: return null
        } catch (e: Exception) {
            null
        }
    }
}