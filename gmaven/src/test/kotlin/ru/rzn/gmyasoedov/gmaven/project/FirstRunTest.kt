package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.openapi.util.io.FileUtil
import org.junit.Assert
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.PLUGIN_ARTIFACT_ID
import ru.rzn.gmyasoedov.gmaven.GMavenConstants.PLUGIN_GROUP_ID
import ru.rzn.gmyasoedov.gmaven.MavenImportingTestCase
import ru.rzn.gmyasoedov.gmaven.util.MavenPathUtil
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.pathString

class FirstRunTest : MavenImportingTestCase() {

    override fun tearDown() {
        super.tearDown()
        MavenPathUtil.testEventSpyJarPath = ""
    }

    fun testFirstRunWithEmpty2M() {
        createProjectSubFile(
            "m1/pom.xml", createPomXml(
                """
            <parent>
                <groupId>org.example</groupId>
                <artifactId>project</artifactId>
                <version>1.0-SNAPSHOT</version>
            </parent>
            <artifactId>m1</artifactId>                
        """.trimIndent()
            )
        )

        val projectFile = createProjectPom(
            """
            <groupId>org.example</groupId>
            <artifactId>project</artifactId>
            <version>1.0-SNAPSHOT</version>
            <packaging>pom</packaging>
            <properties>
                <maven.compiler.source>17</maven.compiler.source>
                <maven.compiler.target>17</maven.compiler.target>
                <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
            </properties>
            <modules>
                <module>m1</module>
            </modules> 
        """
        )
        import(projectFile)
        assertModules("project", "project.m1")

        val localRepositoryPath = getLocalRepositoryMavenPluginPath()
        FileUtil.delete(localRepositoryPath)
        Assert.assertFalse(localRepositoryPath.exists())

        reimport()
        assertModules("project", "project.m1")
        Assert.assertTrue(localRepositoryPath.exists())
    }

    fun testFirstRunFolderWithSpacesAndEmpty2() {
        createProjectSubFile(
            "m1/pom.xml", createPomXml(
                """
            <parent>
                <groupId>org.example</groupId>
                <artifactId>project</artifactId>
                <version>1.0-SNAPSHOT</version>
            </parent>
            <artifactId>m1</artifactId>                
        """.trimIndent()
            )
        )

        val projectFile = createProjectPom(
            """
            <groupId>org.example</groupId>
            <artifactId>project</artifactId>
            <version>1.0-SNAPSHOT</version>
            <packaging>pom</packaging>
            <properties>
                <maven.compiler.source>17</maven.compiler.source>
                <maven.compiler.target>17</maven.compiler.target>
                <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
            </properties>
            <modules>
                <module>m1</module>
            </modules> 
        """
        )
        import(projectFile)
        assertModules("project", "project.m1")

        val localRepositoryPath = getLocalRepositoryMavenPluginPath()

        val externalProjectPath = Path(getExternalProjectPath()).resolve("dir with space")
        FileUtil.createDirectory(externalProjectPath.toFile())
        Assert.assertTrue(externalProjectPath.exists())

        val eventSpyJarPath = Path(MavenPathUtil.getEventSpyJarPathForTest())
        val eventSpyJarCopyPath = externalProjectPath.resolve(eventSpyJarPath.name)
        FileUtil.copy(eventSpyJarPath.toFile(), eventSpyJarCopyPath.toFile())
        Assert.assertTrue(eventSpyJarCopyPath.exists())
        MavenPathUtil.testEventSpyJarPath = eventSpyJarCopyPath.pathString

        FileUtil.delete(localRepositoryPath)
        Assert.assertFalse(localRepositoryPath.exists())
        reimport()
        assertModules("project", "project.m1")
        Assert.assertTrue(localRepositoryPath.exists())
    }

    private fun getLocalRepositoryMavenPluginPath(): Path {
        var localRepositoryPath = Path(getProjectSettings().localRepositoryPath!!)
        val groupIds = PLUGIN_GROUP_ID.split(".")
        for (id in groupIds) {
            localRepositoryPath = localRepositoryPath.resolve(id)
        }
        localRepositoryPath = localRepositoryPath.resolve(PLUGIN_ARTIFACT_ID)
        return localRepositoryPath
    }
}