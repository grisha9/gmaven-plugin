package ru.rzn.gmyasoedov.gmaven.project

import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import ru.rzn.gmyasoedov.gmaven.MavenImportingTestCase

class BuildHelperMavenPluginTest : MavenImportingTestCase() {

    fun testRelativePaths() {
        createProjectSubFile(
            "m1/pom.xml", createPomXml(
                """
            <parent>
                <groupId>org.example</groupId>
                <artifactId>project</artifactId>
                <version>1.0-SNAPSHOT</version>
            </parent>
            <artifactId>m1</artifactId>
        
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.codehaus.mojo</groupId>
                        <artifactId>build-helper-maven-plugin</artifactId>
                        <executions>
                            <execution>
                                <id>add-source</id>
                                <phase>process-resources</phase>
                                <goals>
                                    <goal>add-source</goal>
                                </goals>
                                <configuration>
                                    <sources>
                                        <source>src/it/java</source>
                                    </sources>
                                </configuration>
                            </execution>
                            <execution>
                                <id>add-resource</id>
                                <phase>process-resources</phase>
                                <goals>
                                    <goal>add-resource</goal>
                                </goals>
                                <configuration>
                                    <resources>
                                        <resource>src/it/resources</resource>
                                    </resources>
                                </configuration>
                            </execution>
                            <execution>
                                <id>add-test-source</id>
                                <phase>process-resources</phase>
                                <goals>
                                    <goal>add-test-source</goal>
                                </goals>
                                <configuration>
                                    <sources>
                                        <source>src/it/java-test</source>
                                    </sources>
                                </configuration>
                            </execution>
                            <execution>
                                <id>add-test-resource</id>
                                <phase>process-resources</phase>
                                <goals>
                                    <goal>add-test-resource</goal>
                                </goals>
                                <configuration>
                                    <resources>
                                        <resource>src/it/resources-test</resource>
                                    </resources>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
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
        createProjectSubDirs(
            "m1/src/main/java",
            "m1/src/main/resources",
            "m1/src/it/java",
            "m1/src/it/resources",
            "m1/src/it/java-test",
            "m1/src/it/resources-test",
        )

        import(projectFile)

        assertModules("project", "project.m1")
        assertContentRootsSources(
            "project.m1", "m1", JavaSourceRootType.SOURCE,
            "src/main/java",
            "src/it/java",
        )
        assertContentRootsSources(
            "project.m1", "m1", JavaSourceRootType.TEST_SOURCE,
            "src/it/java-test",
        )
        assertContentRootsResources(
            "project.m1", "m1", JavaResourceRootType.RESOURCE,
            "src/main/resources",
            "src/it/resources",
        )
        assertContentRootsResources(
            "project.m1", "m1", JavaResourceRootType.TEST_RESOURCE,
            "src/it/resources-test",
        )
    }

    fun testAbsolutePaths() {
        val basedirVariable = "\${basedir}"
        val projectBasedirVariable = "\${project.basedir}"
        createProjectSubFile(
            "m1/pom.xml", createPomXml(
                """
            <parent>
                <groupId>org.example</groupId>
                <artifactId>project</artifactId>
                <version>1.0-SNAPSHOT</version>
            </parent>
            <artifactId>m1</artifactId>
        
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.codehaus.mojo</groupId>
                        <artifactId>build-helper-maven-plugin</artifactId>
                        <executions>
                            <execution>
                                <id>add-source</id>
                                <phase>process-resources</phase>
                                <goals>
                                    <goal>add-source</goal>
                                </goals>
                                <configuration>
                                    <sources>
                                        <source1>${basedirVariable}/src/it/java1</source1>
                                        <source2>${projectBasedirVariable}/src/it/java2</source2>
                                        <source>${projectBasedirVariable}/../m1_src</source>
                                    </sources>
                                </configuration>
                            </execution>                               
                        </executions>
                    </plugin>
                </plugins>
            </build>
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
        createProjectSubDirs(
            "m1/src/main/java",
            "m1/src/main/resources",
            "m1/src/it/java1",
            "m1/src/it/java2",
            "m1_src",
        )

        import(projectFile)

        assertModules("project", "project.m1")
        assertContentRootsSources(
            "project.m1", "m1", JavaSourceRootType.SOURCE,
            "src/main/java",
            "src/it/java1",
            "src/it/java2",
        )

        assertContentRootsSources("project.m1", "m1_src", JavaSourceRootType.SOURCE, "")
    }

}