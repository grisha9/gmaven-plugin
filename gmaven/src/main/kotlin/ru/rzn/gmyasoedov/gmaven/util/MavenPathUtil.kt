package ru.rzn.gmyasoedov.gmaven.util

import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.PathUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.DEPENDENCY_TREE_EVENT_SPY_CLASS
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.GMAVEN_PLUGIN_CLASS
import ru.rzn.gmyasoedov.gmaven.settings.MavenExecutionSettings

object MavenPathUtil {

    fun getExtClassesJarPathString(): String {
        if (ApplicationManager.getApplication().isUnitTestMode && testEventSpyJarPath.isNotEmpty()) {
            return testEventSpyJarPath
        }

        return PathUtil.getJarPathForClass(Class.forName(DEPENDENCY_TREE_EVENT_SPY_CLASS))

    }

    fun getLocalMavenPluginPath(): String {
        if (ApplicationManager.getApplication().isUnitTestMode && testMavenPluginPath.isNotEmpty()) {
            return testMavenPluginPath
        }
        return getLocalMavenPluginPathForTest()
    }

    fun getWsl(settings: MavenExecutionSettings): WSLDistribution? {
        return if (SystemInfo.isWindows && Registry.`is`("gmaven.wsl.support"))
            WslPath.getDistributionByWindowsUncPath(settings.executionWorkspace.externalProjectPath) else null
    }

    @VisibleForTesting
    @TestOnly
    fun getLocalMavenPluginPathForTest(): String {
        val clazz = Class.forName(GMAVEN_PLUGIN_CLASS)
        return PathUtil.getJarPathForClass(clazz)
    }

    @TestOnly
    fun getEventSpyJarPathForTest(): String {
        return PathUtil.getJarPathForClass(Class.forName(DEPENDENCY_TREE_EVENT_SPY_CLASS))
    }

    @TestOnly
    var testMavenPluginPath: String = ""

    @TestOnly
    var testEventSpyJarPath: String = ""
}