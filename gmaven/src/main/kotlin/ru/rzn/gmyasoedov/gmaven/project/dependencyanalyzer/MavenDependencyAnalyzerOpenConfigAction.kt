// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package ru.rzn.gmyasoedov.gmaven.project.dependencyanalyzer

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerOpenConfigAction
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerView
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import ru.rzn.gmyasoedov.gmaven.GMavenConstants
import ru.rzn.gmyasoedov.gmaven.project.dependencyanalyzer.GDependencyAnalyzerContributor.Companion.BUILD_FILE

class MavenDependencyAnalyzerOpenConfigAction : DependencyAnalyzerOpenConfigAction(GMavenConstants.SYSTEM_ID) {

  override fun getConfigFile(e: AnActionEvent): VirtualFile? {
    val dependency = e.getData(DependencyAnalyzerView.DEPENDENCY) ?: return null
    val data = dependency.data
    val buildFile = data.getUserData(BUILD_FILE) ?: return null
    return LocalFileSystem.getInstance().findFileByPath(buildFile)
  }
}