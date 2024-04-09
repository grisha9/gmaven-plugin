package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.compiler.CompilerConfiguration
import com.intellij.compiler.CompilerConfigurationImpl
import com.intellij.pom.java.LanguageLevel
import junit.framework.TestCase
import ru.rzn.gmyasoedov.gmaven.MavenImportingTestCase

class AspectJMavenPluginTest : MavenImportingTestCase() {

    fun testBaseArgs() {
        val languageLevel = LanguageLevel.HIGHEST.previewLevel ?: return
        val languageString = languageLevel.toJavaVersion().toFeatureString()
        val aspectJVersion = "1.14"
        import(
            """
            <groupId>org.example</groupId>
            <artifactId>project</artifactId>
            <version>1.0-SNAPSHOT</version>
        
            <build>
                <plugins>
                    <plugin>
					<groupId>dev.aspectj</groupId>
					<artifactId>aspectj-maven-plugin</artifactId>
					<version>${aspectJVersion}</version>
					<executions>
						<execution>
							<phase>process-sources</phase>
							<id>compile</id>
							<goals>
								<goal>compile</goal>
							</goals>
						</execution>
					</executions>
					<configuration>
	                    <source>1.8</source>
						<target>1.8</target>
						<complianceLevel>${languageString}</complianceLevel>
						<aspectDirectory>src</aspectDirectory>
						<XnotReweavable>true</XnotReweavable>
						<Xlint>ignore</Xlint>
						<deprecation>true</deprecation>
                        <enablePreview>true</enablePreview>
					</configuration>
				</plugin>
                </plugins>
            </build>
        """
        )

        assertModules("project")
        val compilerConfiguration = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
        assertUnorderedElementsAreEqual(
            compilerConfiguration.getAdditionalOptions(getModule("project")),
            "--enable-preview"
        )
        val mainJavaCompilerData = getMainJavaCompilerData()
        TestCase.assertTrue(mainJavaCompilerData.dependenciesPath.isNotEmpty())
        TestCase.assertTrue(mainJavaCompilerData.dependenciesPath.first().contains("aspectjtools-1.9.7.jar"))
        assertUnorderedElementsAreEqual(
            listOf("-Xlint:ignore", "-XnotReweavable", "-deprecation", "-enablePreview"),
            mainJavaCompilerData.arguments
        )
        TestCase.assertTrue(getLanguageLevel().isPreview)
    }

    fun testLanguageLevelFromSource() {
        val aspectJVersion = "1.14"
        import(
            """
            <groupId>org.example</groupId>
            <artifactId>project</artifactId>
            <version>1.0-SNAPSHOT</version>
        
            <build>
                <plugins>
                    <plugin>
					<groupId>dev.aspectj</groupId>
					<artifactId>aspectj-maven-plugin</artifactId>
					<version>${aspectJVersion}</version>
					<executions>
						<execution>
							<phase>process-sources</phase>
							<id>compile</id>
							<goals>
								<goal>compile</goal>
							</goals>
						</execution>
					</executions>
					<configuration>
	                    <source>1.8</source>
						<target>1.8</target>	
					</configuration>
				</plugin>
                </plugins>
            </build>
        """
        )

        assertModules("project")
        val level = getLanguageLevel()
        TestCase.assertEquals(LanguageLevel.JDK_1_8, level)
    }

    fun testLanguageLevelFromReleasePriority() {
        val aspectJVersion = "1.14"
        import(
            """
            <groupId>org.example</groupId>
            <artifactId>project</artifactId>
            <version>1.0-SNAPSHOT</version>
        
            <build>
                <plugins>
                    <plugin>
					<groupId>dev.aspectj</groupId>
					<artifactId>aspectj-maven-plugin</artifactId>
					<version>${aspectJVersion}</version>
					<executions>
						<execution>
							<phase>process-sources</phase>
							<id>compile</id>
							<goals>
								<goal>compile</goal>
							</goals>
						</execution>
					</executions>
					<configuration>
	                    <source>1.8</source>
						<target>1.8</target>	
                        <release>11</release>
					</configuration>
				</plugin>
                </plugins>
            </build>
        """
        )

        assertModules("project")
        val level = getLanguageLevel()
        TestCase.assertEquals(LanguageLevel.JDK_11, level)
    }

    fun testCustomAspectJar() {
        val aspectJVersion = "1.14"
        import(
            """
            <groupId>org.example</groupId>
            <artifactId>project</artifactId>
            <version>1.0-SNAPSHOT</version>
        
            <build>
                <plugins>
                    <plugin>
					<groupId>dev.aspectj</groupId>
					<artifactId>aspectj-maven-plugin</artifactId>
					<version>${aspectJVersion}</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.aspectj</groupId>
                            <artifactId>aspectjtools</artifactId> 
                            <version>1.9.22</version>
                        </dependency>
				    </dependencies>
					<executions>
						<execution>
							<phase>process-sources</phase>
							<id>compile</id>
							<goals>
								<goal>compile</goal>
							</goals>
						</execution>
					</executions>
					<configuration>	
                        <complianceLevel>17</complianceLevel> 
					</configuration>
				</plugin>
                </plugins>
            </build>
        """
        )

        assertModules("project")
        val level = getLanguageLevel()
        TestCase.assertEquals(LanguageLevel.JDK_17, level)
        val mainJavaCompilerData = getMainJavaCompilerData()
        TestCase.assertTrue(mainJavaCompilerData.dependenciesPath.isNotEmpty())
        TestCase.assertTrue(mainJavaCompilerData.dependenciesPath.first().contains("aspectjtools-1.9.22.jar"))
    }
}