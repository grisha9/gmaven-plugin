// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.project.dependencyanalyzer

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerContributor
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerExtension
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project
import ru.rzn.gmyasoedov.gmaven.GMavenConstants

class MavenDependencyAnalyzerExtension : DependencyAnalyzerExtension {

    override fun createContributor(
        project: Project,
        systemId: ProjectSystemId,
        parentDisposable: Disposable
    ): DependencyAnalyzerContributor? {
        if (systemId != GMavenConstants.SYSTEM_ID) return null
        return GDependencyAnalyzerContributor(project)
    }
}