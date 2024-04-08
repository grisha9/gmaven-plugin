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
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.jps.model.java.compiler.CompilerOptions
import org.jetbrains.plugins.groovy.compiler.GreclipseIdeaCompiler
import org.jetbrains.plugins.groovy.compiler.GreclipseIdeaCompilerSettings
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MainJavaCompilerData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MainJavaCompilerData.Companion.ASPECTJ_COMPILER_ID
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MainJavaCompilerData.Companion.ECLIPSE_COMPILER_ID
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MainJavaCompilerData.Companion.GROOVY_ECLIPSE_COMPILER_ID
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
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
        if (Registry.`is`("gmaven.not.change.java.compiler")) return

        val config = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
        val javacCompiler = config.registeredJavaCompilers.firstOrNull { it is JavacCompiler } ?: return
        if (toImport.isEmpty()) {
            setCompilerInSettings(config, javacCompiler)
            return
        }

        val javaCompilerData = getJavaCompilerData(toImport) ?: return
        if (javaCompilerData.compilerId == ECLIPSE_COMPILER_ID) {
            val compiler = config.registeredJavaCompilers.firstOrNull { it is EclipseCompiler } ?: return
            setCompilerInSettings(config, compiler)
        } else if (javaCompilerData.compilerId == GROOVY_ECLIPSE_COMPILER_ID) {
            if (!MavenUtils.groovyPluginEnabled()) {
                setCompilerInSettings(config, javacCompiler)
                return
            }
            val compiler = config.registeredJavaCompilers.firstOrNull { it is GreclipseIdeaCompiler } ?: return
            config.defaultCompiler = compiler
            val pathToBatch = javaCompilerData.dependenciesPath.firstOrNull { it.contains("groovy-eclipse-batch") }
            if (pathToBatch != null) {
                GreclipseIdeaCompilerSettings.setGrCmdParams(project, javaCompilerData.arguments.joinToString(" "))
                GreclipseIdeaCompilerSettings.setGrEclipsePath(project, pathToBatch)
            }
        } else if (javaCompilerData.compilerId == ASPECTJ_COMPILER_ID) {
            val compilerAjc = config.registeredJavaCompilers.firstOrNull { it.id == ASPECTJ_COMPILER_ID } ?: return
            val options = compilerAjc.options
            setAjcPath(options, javaCompilerData)
            setCmdParams(options, javaCompilerData)
            setCompilerInSettings(config, compilerAjc)
        } else {
            setCompilerInSettings(config, javacCompiler)
        }
    }

    private fun getJavaCompilerData(toImport: Collection<DataNode<MainJavaCompilerData>>): MainJavaCompilerData? {
        val ajcCompiler = toImport.find { it.data.compilerId == ASPECTJ_COMPILER_ID }
        return ajcCompiler?.data ?: toImport.firstOrNull()?.data
    }

    private fun setAjcPath(options: CompilerOptions, javaCompilerData: MainJavaCompilerData) {
        val aspectJCompilerJar = javaCompilerData.dependenciesPath.firstOrNull() ?: return
        try {
            val ajCompilerSettingsClass = options.javaClass
            val declaredField = ajCompilerSettingsClass.getDeclaredField("ajcPath") ?: return
            declaredField.setAccessible(true)
            declaredField.set(options, aspectJCompilerJar)
        } catch (e: Exception) {
            MavenLog.LOG.error("error set ajc path", e)
        }
    }

    private fun setCmdParams(options: CompilerOptions, javaCompilerData: MainJavaCompilerData) {
        if (javaCompilerData.arguments.isEmpty()) return
        val params = javaCompilerData.arguments.joinToString(separator = " ")
        try {
            val ajCompilerSettingsClass = options.javaClass
            val declaredFields = ajCompilerSettingsClass.declaredFields
            for (declaredField in declaredFields) {
                println("!!! " + declaredField)
            }
            val declaredField = ajCompilerSettingsClass.getDeclaredField("cmdLineParams") ?: return
            declaredField.setAccessible(true)
            declaredField.set(options, params)
        } catch (e: Exception) {
            MavenLog.LOG.error("error set cmdLineParams $params", e)
        }
    }

    private fun setDelegateToJavac(options: CompilerOptions) {
        try {
            val ajCompilerSettingsClass = options.javaClass
            val declaredField = ajCompilerSettingsClass.getDeclaredField("delegateToJavac") ?: return
            declaredField.setAccessible(true)
            declaredField.setBoolean(options, true)
        } catch (e: Exception) {
            MavenLog.LOG.error("error delegateToJavac", e)
        }
    }

    private fun setCompilerInSettings(
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
