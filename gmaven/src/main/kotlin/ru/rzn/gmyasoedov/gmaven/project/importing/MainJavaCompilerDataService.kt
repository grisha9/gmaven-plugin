package ru.rzn.gmyasoedov.gmaven.project.importing

import com.intellij.compiler.CompilerConfiguration
import com.intellij.compiler.CompilerConfigurationImpl
import com.intellij.compiler.impl.javaCompiler.BackendCompiler
import com.intellij.compiler.impl.javaCompiler.eclipse.EclipseCompiler
import com.intellij.compiler.impl.javaCompiler.javac.JavacCompiler
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.groovy.compiler.GreclipseIdeaCompiler
import org.jetbrains.plugins.groovy.compiler.GreclipseIdeaCompilerSettings
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MainJavaCompilerData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MainJavaCompilerData.Companion.ECLIPSE_COMPILER_ID
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MainJavaCompilerData.Companion.GROOVY_ECLIPSE_COMPILER_ID
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils

@Order(ExternalSystemConstants.UNORDERED)
class MainJavaCompilerDataService : AbstractProjectDataService<MainJavaCompilerData, Void>() {

    override fun getTargetDataKey(): Key<MainJavaCompilerData> {
        return MainJavaCompilerData.KEY
    }

    override fun importData(
        toImport: Collection<DataNode<MainJavaCompilerData>>,
        projectData: ProjectData?,
        project: Project,
        modifiableModelsProvider: IdeModifiableModelsProvider
    ) {
        val config = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
        val javacCompiler = config.registeredJavaCompilers.firstOrNull { it is JavacCompiler } ?: return
        if (toImport.isEmpty()) {
            setupJavacOrEclipse(config, javacCompiler)
            return
        }

        val javaCompilerData = toImport.first().data
        if (javaCompilerData.compilerId == ECLIPSE_COMPILER_ID) {
            val compiler = config.registeredJavaCompilers.firstOrNull { it is EclipseCompiler } ?: return
            setupJavacOrEclipse(config, compiler)
        } else if (javaCompilerData.compilerId == GROOVY_ECLIPSE_COMPILER_ID) {
            if (!MavenUtils.groovyPluginEnabled()) {
                setupJavacOrEclipse(config, javacCompiler)
                return
            }
            val compiler = config.registeredJavaCompilers.firstOrNull { it is GreclipseIdeaCompiler } ?: return
            config.defaultCompiler = compiler
            val pathToBatch = javaCompilerData.dependenciesPath.firstOrNull { it.contains("groovy-eclipse-batch") }
            if (pathToBatch != null) {
                GreclipseIdeaCompilerSettings.setGrCmdParams(project, javaCompilerData.arguments.joinToString(" "))
                GreclipseIdeaCompilerSettings.setGrEclipsePath(project, pathToBatch)
            }
        } else {
            setupJavacOrEclipse(config, javacCompiler)
        }
    }

    private fun setupJavacOrEclipse(
        config: CompilerConfigurationImpl,
        backendCompiler: BackendCompiler
    ) {
        if (isSameCompilers(config.defaultCompiler, backendCompiler)) return
        config.defaultCompiler = backendCompiler
    }

    private fun isSameCompilers(currentCompiler: BackendCompiler?, backendCompiler: BackendCompiler): Boolean {
        if (currentCompiler == null) return false
        return currentCompiler.id == backendCompiler.id
    }

}
