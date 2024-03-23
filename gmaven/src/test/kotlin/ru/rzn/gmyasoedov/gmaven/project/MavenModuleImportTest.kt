package ru.rzn.gmyasoedov.gmaven.project

import ru.rzn.gmyasoedov.gmaven.MavenImportingTestCase

class MavenModuleImportTest : MavenImportingTestCase() {

    fun testSimpleModules() {
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
    }

    fun testDirectPathModules() {
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
            "m2/custom.xml", createPomXml(
                """
            <parent>
                <groupId>org.example</groupId>
                <artifactId>project</artifactId>
                <version>1.0-SNAPSHOT</version>
            </parent>
            <artifactId>m2</artifactId>                
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
                <module>m1/pom.xml</module> 
                <module>m2/custom.xml</module>
            </modules> 
        """
        )
        import(projectFile)

        assertModules("project", "project.m1", "project.m2")
    }

}