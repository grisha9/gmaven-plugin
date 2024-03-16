package ru.rzn.gmyasoedov.gmaven.dom

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.XmlPatterns
import com.intellij.psi.*
import com.intellij.psi.PsiReferenceRegistrar.LOWER_PRIORITY
import com.intellij.psi.xml.XmlTag
import com.intellij.util.ProcessingContext
import ru.rzn.gmyasoedov.gmaven.dom.reference.MavenPropertyPsiReference
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils

class MavenPropertiesXmlPsiReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        if (MavenUtils.pluginEnabled(MavenUtils.INTELLIJ_MAVEN_PLUGIN_ID)) return

        registrar.registerReferenceProvider(XmlPatterns.xmlTag(), MavenPropertyPsiReferenceProvider(), LOWER_PRIORITY)
    }
}

class MavenPropertyPsiReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val xmlTag = element as? XmlTag ?: return emptyArray()
        val text = xmlTag.value.text
        if (text.isEmpty()) return emptyArray()

        if (text.startsWith("\${") && text.endsWith("}")) {
            try {
                val range = ElementManipulators.getValueTextRange(element)
                val textRange = TextRange(range.startOffset + 2, range.endOffset - 1)
                val propertyName = text.substring(2, text.length - 1)
                return arrayOf(MavenPropertyPsiReference(propertyName, xmlTag, textRange))
            } catch (e: Exception) {
                return emptyArray<PsiReference>()
            }
        }

        return emptyArray<PsiReference>()
    }

}