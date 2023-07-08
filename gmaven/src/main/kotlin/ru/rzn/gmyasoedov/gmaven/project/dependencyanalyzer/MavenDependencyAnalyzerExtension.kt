package ru.rzn.gmyasoedov.gmaven.project.dependencyanalyzer

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerContributor
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerExtension
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project
import ru.rzn.gmyasoedov.gmaven.GMavenConstants

class MavenDependencyAnalyzerExtension : DependencyAnalyzerExtension {

    override fun createContributor(project: Project, parentDisposable: Disposable): DependencyAnalyzerContributor {
        return GDependencyAnalyzerContributor(project)
    }

    override fun isApplicable(systemId: ProjectSystemId): Boolean {
        return systemId == GMavenConstants.SYSTEM_ID
    }
}