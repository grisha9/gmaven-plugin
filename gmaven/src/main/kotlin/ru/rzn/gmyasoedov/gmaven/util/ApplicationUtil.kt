@file:JvmName("GUtil")

package gutil

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.IdeFocusManager

fun findCurrentProject(): Project? {
    val project = IdeFocusManager.getGlobalInstance().lastFocusedFrame?.project
    return project?.takeIf { isValidProject(it) }
        ?: ProjectManager.getInstance().openProjects.firstOrNull { isValidProject(it) }
}

private fun isValidProject(project: Project?): Boolean {
    return project != null && !project.isDisposed && !project.isDefault
}