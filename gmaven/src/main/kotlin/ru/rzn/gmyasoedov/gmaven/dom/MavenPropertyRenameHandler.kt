package ru.rzn.gmyasoedov.gmaven.dom

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.refactoring.rename.PsiElementRenameHandler


class MavenPropertyRenameHandler : PsiElementRenameHandler() {

    override fun isAvailableOnDataContext(context: DataContext): Boolean {
        return findTarget(context) != null
    }

    private fun findTarget(context: DataContext): PsiElement? {
        return getFindTarget(
            CommonDataKeys.EDITOR.getData(context),
            CommonDataKeys.PSI_FILE.getData(context), true
        )
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext) {
        invoke(project, emptyArray(), dataContext)
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        var element = if (elements.size == 1) elements[0] else null
        if (element == null) {
            element = dataContext?.let {
                findTarget(it)
            }
        }
        super.invoke(project, elements, dataContext)
    }

    private fun getFindTarget(editor: Editor?, file: PsiFile?, rename: Boolean): PsiElement? {
        if (editor == null || file == null) return null
        if (!rename && (file !is XmlFile)) return null
        var target = TargetElementUtil.findTargetElement(editor, TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED)

        if (target == null) {
            target = file.findElementAt(editor.getCaretModel().getOffset())
            if (target == null) return null
        }
        return PsiTreeUtil.getParentOfType(target, XmlTag::class.java, false)
    }
}