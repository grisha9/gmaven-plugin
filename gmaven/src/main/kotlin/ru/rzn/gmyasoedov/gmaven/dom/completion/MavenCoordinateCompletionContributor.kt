package ru.rzn.gmyasoedov.gmaven.dom.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlText
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import ru.rzn.gmyasoedov.gmaven.util.MavenCentralArtifactInfo
import ru.rzn.gmyasoedov.gmaven.util.MavenCentralClient.find
import ru.rzn.gmyasoedov.gmaven.utils.MavenArtifactUtil.*
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer


private const val TIMEOUT_PROMISE_MS = 10_000
private const val TIMEOUT_REQUEST_MS = 1_000

class MavenCoordinateCompletionContributor : CompletionContributor() {
    private val supportTagNames = setOf(ARTIFACT_ID, GROUP_ID, VERSION)

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (MavenUtils.pluginEnabled(MavenUtils.INTELLIJ_MAVEN_PLUGIN_ID)) return
        if (parameters.completionType != CompletionType.BASIC) return
        val currentTimeMillis = System.currentTimeMillis()
        val get = Util.timeStamp.get()
        if (currentTimeMillis - get < TIMEOUT_REQUEST_MS) return
        Util.timeStamp.set(currentTimeMillis)

        result.restartCompletionWhenNothingMatches()
        getCompletionConsumer(parameters)?.accept(result)
    }

    private fun getCompletionConsumer(parameters: CompletionParameters): Consumer<CompletionResultSet>? {
        val element: PsiElement = parameters.position
        val xmlText = element.parent as? XmlText ?: return null
        val tagElement = xmlText.parent as? XmlTag ?: return null
        if (!supportTagNames.contains(tagElement.name)) return null
        val parentXmlTag = tagElement.parent as? XmlTag ?: return null

        if (tagElement.name == VERSION) {
            val artifactId = parentXmlTag.getSubTagText(ARTIFACT_ID) ?: return null
            val groupId = parentXmlTag.getSubTagText(GROUP_ID) ?: return null
            return VersionContributor(artifactId, groupId)
        } else {
            return GAVContributor(tagElement)
        }
    }

    object Util {
        val timeStamp = AtomicLong(0)
    }

}

private class VersionContributor(val artifactId: String, val groupId: String) :
    Consumer<CompletionResultSet> {

    override fun accept(result: CompletionResultSet) {
        val promise = AsyncPromise<List<MavenCentralArtifactInfo>>()
        ApplicationManager.getApplication().executeOnPooledThread {
            promise.setResult(find(groupId, artifactId))
        }
        val startMillis = System.currentTimeMillis()
        while (promise.getState() == Promise.State.PENDING && System.currentTimeMillis() - startMillis < TIMEOUT_PROMISE_MS) {
            ProgressManager.checkCanceled()
            Thread.yield()
        }
        if (!promise.isDone()) return

        val artifactInfoList = promise.get() ?: return
        artifactInfoList.filter { it.v != null }.forEach {
            result.addElement(LookupElementBuilder.create(it, it.v!!).withPresentableText(it.id))
        }
    }
}

private class GAVContributor(val artifactOrGroupTag: XmlTag) : Consumer<CompletionResultSet> {
    override fun accept(result: CompletionResultSet) {
        val queryText = result.prefixMatcher.prefix
        val promise = AsyncPromise<List<MavenCentralArtifactInfo>>()
        ApplicationManager.getApplication().executeOnPooledThread {
            promise.setResult(find(queryText))
        }
        val startMillis = System.currentTimeMillis()
        while (promise.getState() == Promise.State.PENDING && System.currentTimeMillis() - startMillis < TIMEOUT_PROMISE_MS) {
            ProgressManager.checkCanceled()
            Thread.yield()
        }
        if (!promise.isDone()) return

        val artifactInfoList = promise.get() ?: return
        artifactInfoList.forEach {
            val lookupString = if (artifactOrGroupTag.name == ARTIFACT_ID) it.a else it.g
            result.addElement(
                LookupElementBuilder.create(it, lookupString)
                    .withPresentableText(it.id)
                    .withInsertHandler(GAVInsertHandler)
            )
        }
    }
}

private object GAVInsertHandler : InsertHandler<LookupElement> {

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val artifactInfo: MavenCentralArtifactInfo = item.`object` as? MavenCentralArtifactInfo ?: return
        val contextFile = context.file as? XmlFile ?: return
        val element = contextFile.findElementAt(context.startOffset)
        val xmlTag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java) ?: return
        val isArtifactTag = xmlTag.name == ARTIFACT_ID
        val targetTag = if (isArtifactTag) xmlTag.parentTag?.findFirstSubTag(GROUP_ID) else
            xmlTag.parentTag?.findFirstSubTag(ARTIFACT_ID)
        val versionTag = xmlTag.parentTag?.findFirstSubTag(VERSION)
        context.commitDocument()
        if (artifactInfo.v != null) {
            versionTag?.value?.text = artifactInfo.v
        }
        targetTag?.value?.text = if (isArtifactTag) artifactInfo.g else artifactInfo.a

        context.setLaterRunnable {
            CodeCompletionHandlerBase(CompletionType.BASIC).invokeCompletion(context.project, context.editor)
        }
    }
}