package ru.rzn.gmyasoedov.gmaven.project.dependencyanalyzer

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerDependency
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerGoToAction
import com.intellij.openapi.externalSystem.dependency.analyzer.DependencyAnalyzerView
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.pom.Navigatable
import com.intellij.pom.NavigatableAdapter
import ru.rzn.gmyasoedov.gmaven.GMavenConstants

class MavenDependencyAnalyzerGoToAction : DependencyAnalyzerGoToAction(GMavenConstants.SYSTEM_ID) {

    override fun getNavigatable(e: AnActionEvent): Navigatable? {
        val project = e.project ?: return null
        val dependency = e.getData(DependencyAnalyzerView.DEPENDENCY) ?: return null
        val parentData = dependency.parent?.data ?: return null
        val artifactId = getArtifactId(dependency.data) ?: return null
        val buildFile = parentData.getUserData(GDependencyAnalyzerContributor.BUILD_FILE) ?: return null
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(buildFile)
        val bytes = virtualFile?.contentsToByteArray() ?: return null
        var indexOf = String(bytes).indexOf("<artifactId>$artifactId</artifactId>")
        if (indexOf < 0) {
            indexOf = String(bytes).indexOf(artifactId)
        }
        return object : NavigatableAdapter() {
            override fun navigate(requestFocus: Boolean) {
                navigate(project, virtualFile, indexOf, requestFocus)
            }
        }
    }

    internal fun getArtifactId(dependencyData: DependencyAnalyzerDependency.Data?): String? {
        return when (dependencyData) {
            is DependencyAnalyzerDependency.Data.Artifact -> dependencyData.artifactId
            is DependencyAnalyzerDependency.Data.Module -> dependencyData.name
            else -> null
        }
    }
}