package ru.rzn.gmyasoedov.gmaven.dom.reference

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.impl.CachingReference
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import ru.rzn.gmyasoedov.gmaven.dom.XmlPsiUtil
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import ru.rzn.gmyasoedov.gmaven.utils.MavenArtifactUtil
import java.nio.file.Path
import java.util.*

class MavenPropertyPsiReference(
    private val propertyName: String, private val xmlTag: XmlTag, textRange: TextRange
) : PsiReference {
    private val TIMESTAMP_PROP: String = "maven.build.timestamp"
    private val MULTIPROJECT_DIR_PROP: String = "maven.multiModuleProjectDirectory"

    private var valueTextRange = textRange

    override fun getElement() = xmlTag

    override fun getRangeInElement() = valueTextRange

    override fun resolve() = doResolve()

    override fun getCanonicalText() = propertyName

    override fun handleElementRename(newElementName: String): PsiElement? {
        val manipulator = CachingReference.getManipulator<PsiElement>(element)
        val rangeInElement = rangeInElement
        val element = manipulator.handleContentChange(element, rangeInElement, newElementName)
        valueTextRange = TextRange(rangeInElement.startOffset, rangeInElement.startOffset + newElementName.length)
        return element
    }

    override fun bindToElement(element: PsiElement): PsiElement? = null

    override fun isReferenceTo(element: PsiElement) = getElement().manager.areElementsEquivalent(element, resolve())

    override fun isSoft() = true

    private fun doResolve(): PsiElement? {
        var hasPrefix = false
        var unprefixed: String = propertyName
        val xmlFile = xmlTag.containingFile as? XmlFile ?: return null

        if (propertyName.startsWith("pom.")) {
            unprefixed = propertyName.substring("pom.".length)
            hasPrefix = true
        } else if (propertyName.startsWith("project.")) {
            unprefixed = propertyName.substring("project.".length)
            hasPrefix = true
        }

        val projectSettings = MavenSettings.getInstance(xmlTag.project).linkedProjectsSettings
        val repos = projectSettings.mapNotNull { it.localRepositoryPath }

        val parentPathDirectory = xmlFile.parent
        var parentPathPom: Path? = null
        while (unprefixed.startsWith("parent.")) {
            if (unprefixed == "parent.groupId" || unprefixed == "parent.artifactId"
                || unprefixed == "parent.version" || unprefixed == "parent.relativePath"
            ) {
                break
            }
            val parentXmlTag = xmlFile.rootTag?.findFirstSubTag(MavenArtifactUtil.PARENT) ?: return null
            parentPathPom = XmlPsiUtil.getParentPath(parentXmlTag, repos) ?: return null

            unprefixed = unprefixed.substring("parent.".length)
        }

        if (unprefixed == "basedir" || (hasPrefix && unprefixed == "baseUri")) {
            return getBaseDir(parentPathDirectory, parentPathPom)
        }

        if (propertyName == TIMESTAMP_PROP) {
            return element
        }

        if (propertyName == MULTIPROJECT_DIR_PROP) {
            val dirPath = xmlFile.parent?.virtualFile?.toNioPathOrNull()?.toString() ?: return null
            val setting = MavenSettings.getInstance(element.project)
                .getLinkedProjectSettings(dirPath) ?: return null
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(setting.externalProjectPath) ?: return null
            return virtualFile.let { PsiManager.getInstance(element.project).findFile(it) }
        }

       /* val mavenConfig = projectSettings
            .firstNotNullOfOrNull { MvnDotProperties.getMavenConfig(Path(it.externalProjectPath)) }
        if (mavenConfig?.contains(propertyName) == true) {
            return projectSettings
                .firstNotNullOfOrNull { MvnDotProperties.getMavenConfigVFile(Path(it.externalProjectPath)) }
                ?.let { PsiManager.getInstance(element.project).findFile(it) }
        }

        val jvmConfig = projectSettings
            .firstNotNullOfOrNull { MvnDotProperties.getJvmConfig(Path(it.externalProjectPath)) }
        if (jvmConfig?.contains(propertyName) == true) {
            return projectSettings
                .firstNotNullOfOrNull { MvnDotProperties.getJvmConfigVFile(Path(it.externalProjectPath)) }
                ?.let { PsiManager.getInstance(element.project).findFile(it) }
        }
*/

        val propertiesMap = TreeMap<String, XmlTag>()
        fillProperties(xmlFile, propertiesMap, repos)

        val propertyXmlTag = propertiesMap.get(propertyName)
        if (propertyXmlTag != null) return propertyXmlTag

        /*if (myText.startsWith("settings.")) { //todo add settings from maven to PS model
            return resolveSettingsModelProperty()
        }*/

        return null
    }

    override fun getVariants(): Array<Any> {
        val xmlFile = xmlTag.containingFile as? XmlFile ?: return emptyArray()
        val projectSettings = MavenSettings.getInstance(xmlTag.project).linkedProjectsSettings
        val repos = projectSettings.mapNotNull { it.localRepositoryPath }
        val propertiesMap = TreeMap<String, XmlTag>()
        fillProperties(xmlFile, propertiesMap, repos)
        return propertiesMap.keys.map { LookupElementBuilder.create(it) }.toTypedArray()
    }

    private fun getBaseDir(parentPathDirectory: PsiDirectory?, parentPathPom: Path?): PsiElement? {
        return if (parentPathPom != null) {
            XmlPsiUtil.getXmlFile(parentPathPom, element.project)?.containingDirectory
        } else parentPathDirectory
    }

    private fun fillProperties(
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