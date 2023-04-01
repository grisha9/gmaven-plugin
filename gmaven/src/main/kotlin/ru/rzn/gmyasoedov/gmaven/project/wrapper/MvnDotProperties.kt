package ru.rzn.gmyasoedov.gmaven.project.wrapper

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import java.io.ByteArrayInputStream
import java.util.*


class MvnDotProperties {

    companion object {
        private val DISTRIBUTION_URL_PROPERTY = "distributionUrl"

        @JvmStatic
        fun getDistributionUrl(project: Project, projectPath: String): String {
            val propertiesVFile = getWrapperPropertiesVFile(projectPath) ?: return "";
            return getWrapperProperties(project, propertiesVFile).getProperty(DISTRIBUTION_URL_PROPERTY, "");
        }

        private fun getDistributionUrl(wrapperProperties: VirtualFile): String {
            return getWrapperProperties(wrapperProperties).getProperty(DISTRIBUTION_URL_PROPERTY, "");
        }

        private fun getWrapperProperties(project: Project, wrapperProperties: VirtualFile): Properties {
            return CachedValuesManager.getManager(project)
                .getCachedValue(project)
                { -> CachedValueProvider.Result.create(getWrapperProperties(wrapperProperties), wrapperProperties) }
        }

        fun getWrapperPropertiesVFile(projectPath: String): VirtualFile? {
            return LocalFileSystem.getInstance().findFileByPath(projectPath)
                ?.findChild(".mvn")?.findChild("wrapper")?.findChild("maven-wrapper.properties")
        }

        private fun getWrapperProperties(wrapperProperties: VirtualFile): Properties {
            val properties = Properties()
            properties.load(ByteArrayInputStream(wrapperProperties.contentsToByteArray(true)))
            return properties
        }
    }
}