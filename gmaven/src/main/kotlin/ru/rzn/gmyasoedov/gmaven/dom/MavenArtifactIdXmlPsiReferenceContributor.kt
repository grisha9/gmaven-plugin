package ru.rzn.gmyasoedov.gmaven.dom

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.patterns.XmlPatterns
import com.intellij.psi.*
import com.intellij.psi.PsiReferenceBase.Immediate
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import ru.rzn.gmyasoedov.gmaven.util.CachedModuleData
import ru.rzn.gmyasoedov.gmaven.util.CachedModuleDataService
import ru.rzn.gmyasoedov.gmaven.utils.MavenArtifactUtil
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
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

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val xmlTag = element as? XmlTag ?: return emptyArray()
        if (xmlTag.name != MavenArtifactUtil.ARTIFACT_ID) return emptyArray()
        val value = ElementManipulators.getValueText(element)
        val range = ElementManipulators.getValueTextRange(element)
        val dataHolder = CachedModuleDataService.getDataHolder(element.project)
        val moduleData = dataHolder.modules.find { it.artifactId == value }
        return if (moduleData != null) {
            getModulePsiReferences(moduleData, element, xmlTag, range)
        } else {
            getLocalRepositoryPsiReferences(element, xmlTag, value, range)
        }
    }

    private fun getLocalRepositoryPsiReferences(
        element: XmlTag, xmlTag: XmlTag, value: String, range: TextRange
    ): Array<PsiReference> {
        val path = element.containingFile.parent?.virtualFile?.canonicalPath ?: return emptyArray()
        val localRepositoryPath = MavenSettings.getInstance(element.project)
            .getLinkedProjectSettings(path)?.localRepositoryPath ?: return emptyArray()
        val parentXml = xmlTag.parentTag ?: return emptyArray()
        val groupId = parentXml.getSubTagText(MavenArtifactUtil.GROUP_ID) ?: return emptyArray()
        val version = parentXml.getSubTagText(MavenArtifactUtil.VERSION) ?: return emptyArray()
        return try {
            val artifactNioPathPom = MavenArtifactUtil
                .getArtifactNioPathPom(Path(localRepositoryPath), groupId, value, version)
            val virtualFile = LocalFileSystem.getInstance()
                .findFileByNioFile(artifactNioPathPom) ?: return emptyArray()
            val psiFile = PsiManager.getInstance(element.project).findFile(virtualFile) ?: return emptyArray()
            arrayOf(Immediate(xmlTag, range, true, psiFile))
        } catch (e: Exception) {
            emptyArray()
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
}
