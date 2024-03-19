package ru.rzn.gmyasoedov.gmaven.dom

import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.readText
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.util.CachedModuleDataService
import ru.rzn.gmyasoedov.gmaven.utils.MavenArtifactUtil
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.name

object XmlPsiUtil {

    fun getParentPath(
        xmlParentTag: XmlTag, localRepos: List<String>, properties: Map<String, String> = emptyMap()
    ): Path? {
        try {
            val groupIdText = xmlParentTag.getSubTagText(MavenArtifactUtil.GROUP_ID)
            val groupId = getPlaceHolderValue(groupIdText, properties) ?: return null
            val artifactId = xmlParentTag.getSubTagText(MavenArtifactUtil.ARTIFACT_ID) ?: return null

            val dataHolder = CachedModuleDataService.getDataHolder(xmlParentTag.project)
            val configPath = dataHolder.findModuleData(groupId, artifactId)?.configPath
            if (configPath != null) return Path(configPath)
            val versionText = xmlParentTag.getSubTagText(MavenArtifactUtil.VERSION)
            val version = getPlaceHolderValue(versionText, properties) ?: return null
            return searchInLocalRepo(groupId, artifactId, version, localRepos)
        } catch (e: Exception) {
            return null
        }
    }

    fun getPlaceHolderValue(subTagText: String?, properties: Map<String, String>): String? {
        if (subTagText != null && subTagText.startsWith("\${") && subTagText.endsWith("}")) {
            val propertyName = subTagText.subSequence(2, subTagText.length - 1)
            return properties[propertyName]
        }
        return subTagText
    }

    fun getXmlFile(path: Path, project: Project): XmlFile? {
        val filePath = if (path.toFile().isDirectory()) path.resolve(GMavenConstants.POM_XML) else path
        if (!filePath.exists()) return null

        val virtualFile = LocalFileSystem.getInstance().findFileByNioFile(filePath) ?: return null
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return null
        return if (psiFile is XmlFile) psiFile else {
            try {
                PsiFileFactory.getInstance(project)
                    .createFileFromText(filePath.name, XMLLanguage.INSTANCE, virtualFile.readText()) as? XmlFile
            } catch (e: Exception) {
                null
            }
        }
    }

    fun searchInLocalRepo(groupId: String, artifactId: String, version: String, localRepos: List<String>): Path? {
        for (localRepoPath in localRepos) {
            val artifactPath = MavenArtifactUtil
                .getArtifactNioPath(Path.of(localRepoPath), groupId, artifactId, version, "pom")
            if (artifactPath.exists()) return artifactPath
        }
        return null
    }

    fun fillProperties(
        xmlFile: XmlFile, propertiesMap: MutableMap<String, XmlTag> = TreeMap(), localRepos: List<String>,
        deepCount: Int = 0
    ) {
        if (deepCount > 100) return
        val properties = xmlFile.rootTag?.findFirstSubTag(MavenArtifactUtil.PROPERTIES)?.subTags ?: emptyArray()
        for (property in properties) {
            propertiesMap.putIfAbsent(property.name, property)
        }
        val parentTag = xmlFile.rootTag?.findFirstSubTag(MavenArtifactUtil.PARENT) ?: return
        val parentPath = XmlPsiUtil.getParentPath(parentTag, localRepos) ?: return
        val parentXmlFile = XmlPsiUtil.getXmlFile(parentPath, xmlFile.project) ?: return
        if (xmlFile.virtualFile == parentXmlFile.virtualFile) return
        fillProperties(parentXmlFile, propertiesMap, localRepos, deepCount + 1)
    }
}
