package ru.rzn.gmyasoedov.gmaven.project

import org.jetbrains.jps.model.java.JavaSourceRootType
import ru.rzn.gmyasoedov.gmaven.MavenImportingTestCase

class GroovyImporterTest : MavenImportingTestCase() {

    fun testGroovyMavenPluginBase() {
        createProjectSubDirs(
            "src/main/java",
            "src/main/groovy",
            "src/test/java",
            "src/test/groovy"
        )

        import(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1</version>
            <build>
              <plugins>
                <plugin>
                  <groupId>org.codehaus.gmaven</groupId>
                  <artifactId>groovy-maven-plugin</artifactId>
                </plugin>
              </plugins>
            </build>
            """.trimIndent()
        )

        assertModules("project")

        assertContentRootsSources(
            "project", JavaSourceRootType.SOURCE,
            "src/main/groovy",
            "src/main/java",
        )

        assertContentRootsSources(
            "project", JavaSourceRootType.TEST_SOURCE,
            "src/test/groovy",
            "src/test/java",
        )
    }

    fun testGMavenPlusPluginBase() {
        createProjectSubDirs(
            "src/main/java",
            "src/main/groovy",
            "src/test/java",
            "src/test/groovy"
        )

        import(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1</version>
            <build>
              <plugins>
                <plugin>
                <groupId>org.codehaus.gmavenplus</groupId>
                <artifactId>gmavenplus-plugin</artifactId>
                </plugin>
              </plugins>
            </build>
            """.trimIndent()
        )

        assertModules("project")

        assertContentRootsSources(
            "project", JavaSourceRootType.SOURCE,
            "src/main/groovy",
            "src/main/java",
        )

        assertContentRootsSources(
            "project", JavaSourceRootType.TEST_SOURCE,
            "src/test/groovy",
            "src/test/java",
        )
    }

    fun testGrooveMavenCompilerPluginBase() {
        createProjectSubDirs(
            "src/main/java",
            "src/main/groovy",
            "src/test/java",
            "src/test/groovy"
        )

        import(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1</version>
             <dependencies>
                <dependency>
                    <groupId>org.apache.groovy</groupId>
                    <artifactId>groovy</artifactId>
                    <version>4.0.13</version>
                </dependency>        
             </dependencies>
            <build>
              <plugins>
               <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version><!-- 3.6.2 is the minimum -->
                <configuration>
                    <compilerId>groovy-eclipse-compiler</compilerId>
                    <compilerArguments>
                        <indy/><!-- optional; supported by batch 2.4.12-04+ -->
                    </compilerArguments>
                    <failOnWarning>true</failOnWarning><!-- optional; supported by batch 2.5.8-02+ -->
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>org.codehaus.groovy</groupId>
                        <artifactId>groovy-eclipse-compiler</artifactId>
                        <version>3.9.0</version>
                    </dependency>
                    <dependency>
                        <groupId>org.codehaus.groovy</groupId>
                        <artifactId>groovy-eclipse-batch</artifactId>
                        <version>4.0.13-03</version>
                    </dependency>
                </dependencies>
               </plugin>
              </plugins>
            </build>
            """.trimIndent()
        )

        assertModules("project")

        assertContentRootsSources(
            "project", JavaSourceRootType.SOURCE,
            "src/main/groovy",
            "src/main/java",
        )

        assertContentRootsSources(
            "project", JavaSourceRootType.TEST_SOURCE,
            "src/test/groovy",
            "src/test/java",
        )
    }

    fun testGMavenPlusPluginCustomSources() {
        createProjectSubDirs(
            "src/main/groovy",
            "src/main/java",
            "src/foo1",
            "src/foo2",
            "src/test/java",
            "src/test/groovy",
            "src/test-foo1",
            "src/test-foo2"
        )

        import(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1</version>
            <build>
              <plugins>
                <plugin>
                <groupId>org.codehaus.gmavenplus</groupId>
                <artifactId>gmavenplus-plugin</artifactId>
                <executions>
                  <execution>
                    <id>one</id>
                    <goals>
                      <goal>compile</goal>
                    </goals>
                    <configuration>
                      <sources>
                        <fileset>
                          <directory>${'$'}{project.basedir}/src/foo1</directory>
                        </fileset>
                        <fileset>
                          <directory>${'$'}{project.basedir}/src/foo2</directory>
                        </fileset>
                      </sources>
                    </configuration>
                  </execution>
                  <execution>
                    <id>two</id>
                    <goals>
                      <goal>testCompile</goal>
                    </goals>
                    <configuration>
                      <sources>
                        <fileset>
                          <directory>${'$'}{project.basedir}/src/test-foo1</directory>
                        </fileset>
                        <fileset>
                          <directory>${'$'}{project.basedir}/src/test-foo2</directory>
                        </fileset>
                      </sources>
                    </configuration>
                  </execution>
                </executions>
                </plugin>
              </plugins>
            </build>
            """.trimIndent()
        )

        assertModules("project")

        assertContentRootsSources(
            "project", JavaSourceRootType.SOURCE,
            "src/main/java",
            "src/foo1",
            "src/foo2",
        )

        assertContentRootsSources(
            "project", JavaSourceRootType.TEST_SOURCE,
            "src/test-foo1",
            "src/test-foo2",
            "src/test/java",
        )
    }

    fun testGMavenPlusPluginCustomSources2() {
        createProjectSubDirs(
            "src/main/groovy",
            "src/main/java",
            "src/foo1",
            "src/test/java",
            "src/test/groovy",
            "src/test-foo1",
        )

        import(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1</version>
            <build>
              <plugins>
                <plugin>
                <groupId>org.codehaus.gmavenplus</groupId>
                <artifactId>gmavenplus-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>addSources</goal>
                            <goal>addTestSources</goal>
                            <goal>generateStubs</goal>
                            <goal>compile</goal>
                            <goal>generateTestStubs</goal>
                            <goal>compileTests</goal>
                            <goal>removeStubs</goal>
                            <goal>removeTestStubs</goal>
                        </goals>
						<configuration>
							<sources>
								<source>
                                    <directory>${'$'}{project.basedir}/src/foo1</directory>									
									<includes>
										<include>**/*.groovy</include>
									</includes>
								</source>
							</sources>
							<testSources>
                                <testSource>
                                    <directory>${'$'}{project.basedir}/src/test-foo1</directory>
                                    <includes>
                                        <include>**/*.groovy</include>
                                    </includes>
                                </testSource>
                            </testSources>
						</configuration>
                    </execution>
                </executions>
                </plugin>
              </plugins>
            </build>
            """.trimIndent()
        )

        assertModules("project")

        assertContentRootsSources(
            "project", JavaSourceRootType.SOURCE,
            "src/main/java",
            "src/foo1",
        )

        assertContentRootsSources(
            "project", JavaSourceRootType.TEST_SOURCE,
            "src/test-foo1",
            "src/test/java",
        )
    }

    fun testGMavenPlusPluginGeneratedSources() {
        createProjectSubDirs(
            "src/main/java",
            "src/test/java",
            "src/main/groovy",
            "src/test/groovy",
            "target/generated-sources/xxx/yyy",
            "target/generated-sources/groovy-stubs/main/foo",
            "target/generated-sources/groovy-stubs/test/bar"
        )

        import(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1</version>
            <build>
              <plugins>
                <plugin>
                <groupId>org.codehaus.gmavenplus</groupId>
                <artifactId>gmavenplus-plugin</artifactId>
                <executions>
                  <execution>
                    <goals>
                      <goal>generateStubs</goal>
                      <goal>compile</goal>
                      <goal>generateTestStubs</goal>
                      <goal>testCompile</goal>
                    </goals>
                  </execution>
                </executions>
                </plugin>
              </plugins>
            </build>
            """.trimIndent()
        )

        assertModules("project")

        assertContentRootsSources(
            "project", JavaSourceRootType.SOURCE,
            "src/main/groovy",
            "src/main/java",
            "target/generated-sources/xxx"
        )

        assertContentRootsSources(
            "project", JavaSourceRootType.TEST_SOURCE,
            "src/test/groovy",
            "src/test/java",
        )

        assertExcludes("project", "target")
    }

    fun testGMavenPlusPluginGeneratedSourcesCustom() {
        createProjectSubDirs(
            "src/main/java",
            "src/test/java",
            "src/main/groovy",
            "src/test/groovy",
            "target/generated-sources/xxx/yyy",
            "target/generated-sources/foo/aaa",
            "target/generated-sources/bar/bbb"
        )

        import(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1</version>
            <build>
              <plugins>
                <plugin>
                <groupId>org.codehaus.gmavenplus</groupId>
                <artifactId>gmavenplus-plugin</artifactId>
                <executions>
                  <execution>
                    <id>one</id>
                    <goals>
                      <goal>generateStubs</goal>
                    </goals>
                    <configuration>
                      <outputDirectory>${'$'}{project.build.directory}/generated-sources/foo</outputDirectory>
                    </configuration>
                  </execution>
                  <execution>
                    <id>two</id>
                    <goals>
                      <goal>generateTestStubs</goal>
                    </goals>
                    <configuration>
                      <outputDirectory>${'$'}{project.build.directory}/generated-sources/bar</outputDirectory>
                    </configuration>
                  </execution>
                </executions>
                </plugin>
              </plugins>
            </build>
            """.trimIndent()
        )

        assertModules("project")

        assertContentRootsSources(
            "project", JavaSourceRootType.SOURCE,
            "src/main/groovy",
            "src/main/java",
            "target/generated-sources/xxx"
        )

        assertContentRootsSources(
            "project", JavaSourceRootType.TEST_SOURCE,
            "src/test/groovy",
            "src/test/java",
        )

        assertExcludes("project", "target")
    }

    fun testGMavenPlusPluginGeneratedSourcesCustomRelative() {
        createProjectSubDirs(
            "src/main/java",
            "src/test/java",
            "src/main/groovy",
            "src/test/groovy",
            "target/generated-sources/xxx/yyy",
            "target/generated-sources/foo/aaa",
            "target/generated-sources/bar/bbb"
        )

        import(
            """
            <groupId>test</groupId>
            <artifactId>project</artifactId>
            <version>1</version>
            <build>
              <plugins>
                <plugin>
                <groupId>org.codehaus.gmavenplus</groupId>
                <artifactId>gmavenplus-plugin</artifactId>
                <executions>
                  <execution>
                    <id>one</id>
                    <goals>
                      <goal>generateStubs</goal>
                    </goals>
                    <configuration>
                      <outputDirectory>target/generated-sources/foo</outputDirectory>
                    </configuration>
                  </execution>
                  <execution>
                    <id>two</id>
                    <goals>
                      <goal>generateTestStubs</goal>
                    </goals>
                    <configuration>
                      <outputDirectory>target/generated-sources/bar</outputDirectory>
                    </configuration>
                  </execution>
                </executions>
                </plugin>
              </plugins>
            </build>
            """.trimIndent()
        )

        assertModules("project")

        assertContentRootsSources(
            "project", JavaSourceRootType.SOURCE,
            "src/main/groovy",
            "src/main/java",
            "target/generated-sources/xxx"
        )

        assertContentRootsSources(
            "project", JavaSourceRootType.TEST_SOURCE,
            "src/test/groovy",
            "src/test/java",
        )

        assertExcludes("project", "target")
    }
}
