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
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.name

object XmlPsiUtil {

    fun getParentPath(xmlParentTag: XmlTag, localRepos: List<String>): Path? {
        try {
            val groupId = xmlParentTag.getSubTagText(MavenArtifactUtil.GROUP_ID) ?: return null
            val artifactId = xmlParentTag.getSubTagText(MavenArtifactUtil.ARTIFACT_ID) ?: return null

            val dataHolder = CachedModuleDataService.getDataHolder(xmlParentTag.project)
            val configPath = dataHolder.findModuleData(groupId, artifactId)?.configPath
            if (configPath != null) return Path(configPath)

            return searchParentInLocalRepo(groupId, artifactId, xmlParentTag, localRepos)
        } catch (e: Exception) {
            return null
        }
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

    private fun searchParentInLocalRepo(
        groupId: String, artifactId: String, xmlParentTag: XmlTag, localRepos: List<String>
    ): Path? {
        val version = xmlParentTag.getSubTagText(MavenArtifactUtil.VERSION) ?: return null
        for (localRepoPath in localRepos) {
            val artifactPath = MavenArtifactUtil
                .getArtifactNioPath(Path.of(localRepoPath), groupId, artifactId, version, "pom")
            if (artifactPath.exists()) return artifactPath
        }
        return null
    }
}
