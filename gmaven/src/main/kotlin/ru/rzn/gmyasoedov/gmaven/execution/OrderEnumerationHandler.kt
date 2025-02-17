package ru.rzn.gmyasoedov.gmaven.execution

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderEnumerationHandler
import ru.rzn.gmyasoedov.gmaven.GMavenConstants

class MavenOrderEnumerationHandler : OrderEnumerationHandler() {

    class MavenOrderEnumerationFactory : Factory() {
        override fun isApplicable(module: Module): Boolean {
            return ExternalSystemApiUtil.isExternalSystemAwareModule(GMavenConstants.SYSTEM_ID, module)
        }

        override fun createHandler(p0: Module) = MavenOrderEnumerationHandler()
    }

    override fun shouldAddRuntimeDependenciesToTestCompilationClasspath() = true

    override fun shouldIncludeTestsFromDependentModulesToTestClasspath() = false

    override fun shouldProcessDependenciesRecursively() = false

    override fun areResourceFilesFromSourceRootsCopiedToOutput() = false
}