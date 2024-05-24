package ru.rzn.gmyasoedov.gmaven

import com.intellij.externalSystem.JavaModuleData
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ContentFolder
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.java.LanguageLevel
import org.intellij.lang.annotations.Language
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.CompilerPluginData
import ru.rzn.gmyasoedov.gmaven.project.externalSystem.model.MainJavaCompilerData
import ru.rzn.gmyasoedov.gmaven.settings.MavenProjectSettings
import ru.rzn.gmyasoedov.gmaven.settings.MavenSettings
import ru.rzn.gmyasoedov.gmaven.wizard.GOpenProjectProvider
import ru.rzn.gmyasoedov.gmaven.wizard.createMavenProjectSettings
import java.util.*

abstract class MavenImportingTestCase : MavenTestCase() {
    protected var mavenSettings: MavenSettings? = null
    protected var mavenProjectSettings: MavenProjectSettings? = null

    protected fun getProjectSettings() = mavenProjectSettings!!

    protected fun getExternalProjectPath() = mavenProjectSettings!!.externalProjectPath

    protected fun getLanguageLevel(): LanguageLevel {
        return ProjectDataManager.getInstance().getExternalProjectData(
            project,
            SYSTEM_ID, getExternalProjectPath()
        )
            ?.externalProjectStructure
            ?.let { ExternalSystemApiUtil.findAllRecursively(it, JavaModuleData.KEY) }
            ?.map { it.data }
            ?.firstOrNull()
            ?.languageLevel!!
    }

    protected fun getMainJavaCompilerData(): MainJavaCompilerData {
        return ProjectDataManager.getInstance().getExternalProjectData(
            project,
            SYSTEM_ID, getExternalProjectPath()
        )
            ?.externalProjectStructure
            ?.let { ExternalSystemApiUtil.findAllRecursively(it, MainJavaCompilerData.KEY) }
            ?.map { it.data }
            ?.first()!!
    }

    protected fun getCompilerData(): List<CompilerPluginData> {
        return ProjectDataManager.getInstance().getExternalProjectData(project, SYSTEM_ID, getExternalProjectPath())
            ?.externalProjectStructure
            ?.let { ExternalSystemApiUtil.findAllRecursively(it, CompilerPluginData.KEY) }
            ?.map { it.data } ?: emptyList()
    }

    protected fun import(projectFile: VirtualFile) {
        mavenSettings = MavenSettings.getInstance(project)
        mavenSettings!!.storeProjectFilesExternally = true
        mavenProjectSettings = createMavenProjectSettings(projectFile, project)

        GOpenProjectProvider().attachProjectAndRefresh(mavenProjectSettings!!, project)
    }

    protected fun reimport() {
        ExternalSystemUtil.refreshProject(
            mavenProjectSettings!!.externalProjectPath,
            ImportSpecBuilder(project, SYSTEM_ID)
                .use(ProgressExecutionMode.MODAL_SYNC)
        )
    }

    protected fun import(
        @Language(value = "XML", prefix = "<project>", suffix = "</project>") xml: String
    ) {
        val createPomFile = createProjectPom(xml)
        import(createPomFile)
    }

    protected fun assertModules(vararg expectedNames: String) {
        val actualNames = ModuleManager.getInstance(project).modules.mapTo(mutableSetOf()) { it.name }
        assertUnorderedElementsAreEqual(actualNames, *expectedNames)
    }

    private fun getContentRoot(moduleName: String, path: String): ContentEntry {
        val roots = getContentRoots(moduleName)
        for (e: ContentEntry in roots) {
            if (e.url == VfsUtilCore.pathToUrl(path)) return e
        }
        throw java.lang.AssertionError(
            "content root not found in module " + moduleName + ":" +
                    "\nExpected root: " + path +
                    "\nExisting roots:" +
                    "\n" + StringUtil.join(
                roots,
                { it: ContentEntry -> " * " + it.getUrl() }, "\n"
            )
        )
    }

    protected fun assertContentRootsSources(
        moduleName: String, type: JavaSourceRootType, vararg expectedSources: String
    ) {
        assertContentRootsSources(moduleName, "", type, *expectedSources)
    }

    protected fun assertContentRootsResources(
        moduleName: String, type: JavaResourceRootType, vararg expectedSources: String
    ) {
        assertContentRootsResources(moduleName, "", type, *expectedSources)
    }

    protected fun assertContentRootsSources(
        moduleName: String, relativeContentRoot: String, type: JavaSourceRootType, vararg expectedSources: String
    ) {
        val contentRoot = rootFromRelativePath(relativeContentRoot)
        val root: ContentEntry = getContentRoot(moduleName, contentRoot)
        doAssertContentFolders(
            root,
            root.getSourceFolders(type),
            *expectedSources
        )
    }

    protected fun assertContentRootsResources(
        moduleName: String, relativeContentRoot: String, type: JavaResourceRootType, vararg expectedSources: String
    ) {
        val contentRoot = rootFromRelativePath(relativeContentRoot)
        val root: ContentEntry = getContentRoot(moduleName, contentRoot)
        doAssertContentFolders(
            root,
            root.getSourceFolders(type),
            *expectedSources
        )
    }

    protected fun assertRelativeContentRoots(moduleName: String, vararg expectedRelativeRoots: String) {
        val expectedRoots = expectedRelativeRoots.map { rootFromRelativePath(it) }.toTypedArray()
        assertContentRootSources(moduleName, *expectedRoots)
    }

    private fun rootFromRelativePath(root: String) = projectPath + (if ("" == root) "" else "/$root")

    protected fun assertContentRootSources(moduleName: String, vararg expectedRoots: String) {
        val actual = getContentRoots(moduleName).map { it.url }
        val expected = expectedRoots.map { VfsUtilCore.pathToUrl(it) }
        assertUnorderedPathsAreEqual(actual, expected)
    }

    fun getContentRoots(moduleName: String): Array<ContentEntry> {
        return getRootManager(moduleName).contentEntries
    }

    private fun getRootManager(module: String): ModuleRootManager {
        return ModuleRootManager.getInstance(getModule(module))
    }

    protected fun getModule(name: String): Module {
        val m = ReadAction.compute<Module?, RuntimeException> {
            ModuleManager.getInstance(project).findModuleByName(name)
        }
        assertNotNull("Module $name not found", m)
        return m
    }

    private fun doAssertContentFolders(
        e: ContentEntry, folders: List<ContentFolder>, vararg expected: String
    ) {
        val actual: MutableList<String> = ArrayList()
        for (f in folders) {
            val rootUrl = e.url
            var folderUrl = f.url

            if (folderUrl.startsWith(rootUrl)) {
                val length = rootUrl.length + 1
                folderUrl = folderUrl.substring(Math.min(length, folderUrl.length))
            }

            actual.add(folderUrl)
        }

        assertSameElements(
            "Unexpected list of folders in content root " + e.url,
            actual, Arrays.asList(*expected)
        )
    }

    protected fun assertExcludes(moduleName: String, vararg expectedExcludes: String) {
        val contentRoot = getContentRoots(moduleName)[0]
        doAssertContentFolders(
            contentRoot,
            listOf(*contentRoot.excludeFolders),
            *expectedExcludes
        )
    }
}