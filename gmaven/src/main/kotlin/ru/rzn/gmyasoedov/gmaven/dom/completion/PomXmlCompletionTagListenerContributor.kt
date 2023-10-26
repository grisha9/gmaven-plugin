package ru.rzn.gmyasoedov.gmaven.dom.completion

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType.BASIC
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import ru.rzn.gmyasoedov.gmaven.util.CachedModuleData
import ru.rzn.gmyasoedov.gmaven.utils.MavenArtifactUtil
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils.INTELLIJ_MAVEN_PLUGIN_ID


class PomXmlCompletionTagListenerContributor : CompletionContributor() {
    private val handledTags = setOf(MavenArtifactUtil.DEPENDENCY, MavenArtifactUtil.EXCLUSION)

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (MavenUtils.pluginEnabled(INTELLIJ_MAVEN_PLUGIN_ID)) return
        if (TemplateManager.getInstance(parameters.originalFile.project).getActiveTemplate(parameters.editor) != null) {
            return // Don't brake the template.
        }
        val psiFile = parameters.originalFile as? XmlFile ?: return
        if (!CachedModuleData.getAllConfigPaths(psiFile.project).contains(psiFile.virtualFile.path)) return

        result.runRemainingContributors(parameters) { r ->
            val lookupElement = r.lookupElement
            val lookupString = lookupElement.lookupString
            if (!handledTags.contains(lookupString)) {
                result.passResult(r)
                return@runRemainingContributors
            }

            val decorator: LookupElement = LookupElementDecorator.withInsertHandler(lookupElement) { context, _ ->
                lookupElement.handleInsert(context)
                val lookupObject = lookupElement.getObject()
                if (lookupObject is XmlTag && "maven-4.0.0.xsd" == lookupObject.containingFile.name) {
                    context.commitDocument()
                    val caretModel = context.editor.caretModel
                    val psiElement =
                        context.file.findElementAt(caretModel.offset)
                    val xmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag::class.java) ?: return@withInsertHandler

                    var s = "\n<groupId></groupId>\n<artifactId></artifactId>\n"
                    if (lookupString == MavenArtifactUtil.DEPENDENCY) {
                        s += "<version></version>\n";
                    }

                    context.document.insertString(caretModel.offset, s)
                    caretModel.moveToOffset(caretModel.offset + s.indexOf("</artifactId>"))
                    context.commitDocument()

                    ReformatCodeProcessor(context.project, context.file, xmlTag.textRange, false).run()
                    context.setLaterRunnable {
                        CodeCompletionHandlerBase(BASIC).invokeCompletion(context.project, context.editor)
                    }
                }
            }
            result.passResult(r.withLookupElement(decorator))
        }
    }
}