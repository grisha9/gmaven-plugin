package ru.rzn.gmyasoedov.gmaven.project

import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import ru.rzn.gmyasoedov.gmaven.MavenImportingTestCase

class KotlinMavenPluginImportTest : MavenImportingTestCase() {

    fun testOnlyKotlinSources() {
        val projectFile = createProjectPom(
            """
            <groupId>org.example</groupId>
            <artifactId>project</artifactId> 
            <version>1.0-SNAPSHOT</version>  

            <properties>
                <maven.compiler.source>11</maven.compiler.source>
                <maven.compiler.target>11</maven.compiler.target>
                <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                <kotlin.code.style>official</kotlin.code.style>
                <kotlin.compiler.jvmTarget>1.8</kotlin.compiler.jvmTarget> 
                <kotlin.compiler.incremental>true</kotlin.compiler.incremental>
            </properties>

            <build>
                <sourceDirectory>src/main/kotlin</sourceDirectory>
                <testSourceDirectory>src/test/kotlin</testSourceDirectory>

                <plugins>
                    <plugin>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-maven-plugin</artifactId>
                        <version>1.7.0</version>
                        <extensions>true</extensions> 
                        <executions>
                            <execution>
                                <id>compile</id>
                                <goals>
                                    <goal>compile</goal>
                                </goals>
                            </execution>
                            <execution>
                                <id>test-compile</id>
                                <goals>
                                    <goal>test-compile</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin> 
                </plugins>
            </build>
        """
        )
        createProjectSubDirs(
            "src/main/kotlin",
            "src/test/kotlin",
        )

        import(projectFile)

        assertModules("project")
        assertContentRootsSources(
            "project", JavaSourceRootType.SOURCE,
            "src/main/kotlin",
        )

        assertContentRootsSources(
            "project",JavaSourceRootType.TEST_SOURCE,
            "src/test/kotlin"
        )
    }

    fun testJavaAndKotlinSources() {
        val projectFile = createProjectPom(
            """
            <groupId>org.example</groupId>
            <artifactId>project</artifactId> 
            <version>1.0-SNAPSHOT</version>  

            <properties>
                <maven.compiler.source>11</maven.compiler.source>
                <maven.compiler.target>11</maven.compiler.target>
                <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                <kotlin.code.style>official</kotlin.code.style>
                <kotlin.compiler.jvmTarget>1.8</kotlin.compiler.jvmTarget> 
                <kotlin.compiler.incremental>true</kotlin.compiler.incremental>
            </properties>

            <build> 
                <plugins>
                    <plugin>
                        <groupId>org.jetbrains.kotlin</groupId>
                        <artifactId>kotlin-maven-plugin</artifactId>
                        <version>1.7.0</version>
                        <extensions>true</extensions> 
                        <executions>
                            <execution>
                                <id>compile</id>
                                <goals>
                                    <goal>compile</goal>
                                </goals>
                                <configuration>
                                    <sourceDirs>
                                        <source1>src/main/kotlin</source1>
                                        <source2>src/main/java</source2>
                                        <sourceDir>src/main/sourceDir</sourceDir>
                                    </sourceDirs>
                                </configuration>
                            </execution>
                            <execution>
                                <id>test-compile</id>
                                <goals>
                                    <!-- You can skip the <goals> element if you enable extensions for the plugin -->
                                    <goal>test-compile</goal>
                                </goals>
                                <configuration>
                                    <sourceDirs>
                                        <sourceDir>src/test/kotlin</sourceDir>
                                        <sourceDir>src/test/java</sourceDir>
                                    </sourceDirs>
                                </configuration>
                            </execution> 
                        </executions>
                    </plugin>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <version>3.5.1</version>
                        <executions>
                            <!-- Replacing default-compile as it is treated specially by Maven -->
                            <execution>
                                <id>default-compile</id>
                                <phase>none</phase>
                            </execution>
                            <!-- Replacing default-testCompile as it is treated specially by Maven -->
                            <execution>
                                <id>default-testCompile</id>
                                <phase>none</phase>
                            </execution>
                            <execution>
                                <id>java-compile</id>
                                <phase>compile</phase>
                                <goals>
                                    <goal>compile</goal>
                                </goals>
                            </execution>
                            <execution>
                                <id>java-test-compile</id>
                                <phase>test-compile</phase>
                                <goals>
                                    <goal>testCompile</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin> 
                </plugins>
            </build>
        """
        )
        createProjectSubDirs(
            "src/main/kotlin",
            "src/main/java",
            "src/main/sourceDir",
            "src/main/resources",

            "src/test/kotlin",
            "src/test/java",
            "src/test/resources",
        )

        import(projectFile)

        assertModules("project")
        assertContentRootsSources(
            "project", JavaSourceRootType.SOURCE,
            "src/main/java",
            "src/main/kotlin",
            "src/main/sourceDir",
        )

        assertContentRootsSources(
            "project", JavaSourceRootType.TEST_SOURCE,
            "src/test/java",
            "src/test/kotlin"
        )

        assertContentRootsResources(
            "project", JavaResourceRootType.RESOURCE,
            "src/main/resources"
        )

        assertContentRootsResources(
            "project", JavaResourceRootType.TEST_RESOURCE,
            "src/test/resources"
        )
    }

}