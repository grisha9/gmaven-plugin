package ru.rzn.gmyasoedov.gmaven.project.wrapper

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists


object MvnDotProperties {
    private const val DISTRIBUTION_URL_PROPERTY = "distributionUrl"

    @JvmStatic
    fun getDistributionUrl(project: Project, projectPath: String): String {
        val propertiesVFile = getWrapperPropertiesVFile(projectPath) ?: return ""
        return getWrapperProperties(project, propertiesVFile).getProperty(DISTRIBUTION_URL_PROPERTY, "")
    }

    @JvmStatic
    fun getDistributionUrl(projectPath: String): String {
        val propertiesVFile = getWrapperPropertiesVFile(projectPath) ?: return ""
        return getWrapperProperties(propertiesVFile).getProperty(DISTRIBUTION_URL_PROPERTY, "")
    }

    @JvmStatic
    fun isWrapperExist(projectDirectory: VirtualFile): Boolean {
        val nioPath = projectDirectory.toNioPath()
        return nioPath.resolve(".mvn").resolve("wrapper").resolve("maven-wrapper.properties").exists()
                && nioPath.resolve("mvnw").exists()
    }

    private fun getWrapperProperties(project: Project, wrapperProperties: VirtualFile): Properties {
        return CachedValuesManager.getManager(project)
            .getCachedValue(project)
            { CachedValueProvider.Result.create(getWrapperProperties(wrapperProperties), wrapperProperties) }
    }

    private fun getWrapperPropertiesVFile(projectPath: String): VirtualFile? {
        return LocalFileSystem.getInstance().findFileByPath(projectPath)
            ?.findChild(".mvn")?.findChild("wrapper")?.findChild("maven-wrapper.properties")
    }

    private fun getWrapperProperties(wrapperProperties: VirtualFile): Properties {
        val properties = Properties()
        properties.load(ByteArrayInputStream(wrapperProperties.contentsToByteArray(true)))
        return properties
    }

    @JvmStatic
    fun getJvmConfig(projectPath: Path): String {
        val jvmConfigVFile = getJvmConfigVFile(projectPath) ?: return ""
        return String(jvmConfigVFile.contentsToByteArray(true), jvmConfigVFile.charset)
    }

    private fun getJvmConfigVFile(projectPath: Path): VirtualFile? {
        return LocalFileSystem.getInstance().findFileByNioFile(projectPath)
            ?.findChild(".mvn")?.findChild("jvm.config")
    }
}