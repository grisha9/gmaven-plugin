package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.util.registry.Registry
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.base.externalSystem.findAll
import ru.rzn.gmyasoedov.gmaven.MavenImportingTestCase

class ReadonlyImportTest: MavenImportingTestCase() {

    override fun tearDown() {
        super.tearDown()
        Registry.get("gmaven.import.readonly").resetToDefault()
    }

    fun testOrdinaryImport() {
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
        createProjectSubFile(
            "m2/pom.xml", createPomXml(
                """
            <parent>
                <groupId>org.example</groupId>
                <artifactId>project</artifactId>
                <version>1.0-SNAPSHOT</version>
            </parent>
            <artifactId>m2</artifactId>
                            
    <dependencies>
        <dependency>            
            <groupId>org.example</groupId>
            <artifactId>m1</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>  
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
        </dependency>
    </dependencies>          
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
                <module>m2</module>
            </modules> 
        """
        )
        import(projectFile)

        assertModules("project", "project.m1", "project.m2")
        val m2ModuleNode = getModulesNode().find { it.data.moduleName == "m2" }
        TestCase.assertNotNull(m2ModuleNode)
        val modulesNodes = m2ModuleNode?.findAll(ProjectKeys.MODULE_DEPENDENCY) ?: emptyList()
        TestCase.assertTrue(modulesNodes.isNotEmpty())
        val libraryNodes = m2ModuleNode?.findAll(ProjectKeys.LIBRARY_DEPENDENCY) ?: emptyList()
        TestCase.assertTrue(libraryNodes.isNotEmpty())
    }

    fun testReadonlyImport() {
        Registry.get("gmaven.import.readonly").setValue(true)
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
        createProjectSubFile(
            "m2/pom.xml", createPomXml(
                """
            <parent>
                <groupId>org.example</groupId>
                <artifactId>project</artifactId>
                <version>1.0-SNAPSHOT</version>
            </parent>
            <artifactId>m2</artifactId>
                            
    <dependencies>
        <dependency>            
            <groupId>org.example</groupId>
            <artifactId>m1</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>  
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
        </dependency>
    </dependencies>          
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
                <module>m2</module>
            </modules> 
        """
        )
        import(projectFile)

        assertModules("project", "project.m1", "project.m2")
        val m2ModuleNode = getModulesNode().find { it.data.moduleName == "m2" }
        TestCase.assertNotNull(m2ModuleNode)
        val modulesNodes = m2ModuleNode?.findAll(ProjectKeys.MODULE_DEPENDENCY) ?: emptyList()
        TestCase.assertTrue(modulesNodes.isNotEmpty())
        val libraryNodes = m2ModuleNode?.findAll(ProjectKeys.LIBRARY_DEPENDENCY) ?: emptyList()
        TestCase.assertTrue(libraryNodes.isEmpty())
    }
}