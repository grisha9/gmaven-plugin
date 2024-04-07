package ru.rzn.gmyasoedov.gmaven.project

import com.intellij.compiler.CompilerConfiguration
import com.intellij.compiler.CompilerConfigurationImpl
import com.intellij.pom.java.LanguageLevel
import ru.rzn.gmyasoedov.gmaven.MavenImportingTestCase

class MavenCompilerPluginTest : MavenImportingTestCase() {

    fun testCompilerArgsAsString() {
        val languageLevel = LanguageLevel.HIGHEST.previewLevel ?: return
        val languageString = languageLevel.toJavaVersion().toFeatureString()
        import(
            """
            <groupId>org.example</groupId>
            <artifactId>project</artifactId>
            <version>1.0-SNAPSHOT</version>
            <properties> 
                <maven.compiler.release>${languageString}</maven.compiler.release>
                <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
            </properties>
        
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <version>3.12.0</version>
                        <configuration>
                            <compilerArgs>--enable-preview</compilerArgs>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        """
        )

        assertModules("project")
        val compilerConfiguration = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
        assertEquals("Javac", compilerConfiguration.defaultCompiler.id)
        assertUnorderedElementsAreEqual(
            compilerConfiguration.getAdditionalOptions(getModule("project")),
            "--enable-preview")
    }

    fun testCompilerArgs() {
        val languageLevel = LanguageLevel.HIGHEST.previewLevel ?: return
        val languageString = languageLevel.toJavaVersion().toFeatureString()
        import(
            """
            <groupId>org.example</groupId>
            <artifactId>project</artifactId>
            <version>1.0-SNAPSHOT</version>
            <properties> 
                <maven.compiler.release>${languageString}</maven.compiler.release>
                <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
            </properties>
        
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <version>3.12.0</version>
                        <configuration>
                            <compilerArgs>
                                <arg>-Xlint:unchecked</arg>
                                <arg1>--enable-preview</arg1>
                            </compilerArgs>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        """
        )

        assertModules("project")
        val compilerConfiguration = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
        assertEquals("Javac", compilerConfiguration.defaultCompiler.id)
        assertUnorderedElementsAreEqual(
            compilerConfiguration.getAdditionalOptions(getModule("project")),
            "--enable-preview", "-Xlint:unchecked")
    }

    fun testCompilerPluginConfigurationCompilerArguments()  {
        import("<groupId>test</groupId>" +
                "<artifactId>project</artifactId>" +
                "<version>1</version>" +
                "<build>" +
                "  <plugins>" +
                "    <plugin>" +
                "      <groupId>org.apache.maven.plugins</groupId>" +
                "      <artifactId>maven-compiler-plugin</artifactId>" +
                "      <configuration>" +
                "        <compilerArguments>" +
                "          <Averbose>true</Averbose>" +
                "          <parameters></parameters>" +
                "          <bootclasspath>rt.jar_path_here</bootclasspath>" +
                "        </compilerArguments>" +
                "      </configuration>" +
                "    </plugin>" +
                "  </plugins>" +
                "</build>")

        assertModules("project")
        val compilerConfiguration = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
        assertEquals("Javac", compilerConfiguration.defaultCompiler.id)
        assertUnorderedElementsAreEqual(compilerConfiguration.getAdditionalOptions(getModule("project")),
            "-Averbose=true", "-parameters", "-bootclasspath", "rt.jar_path_here")
    }

    fun testCompilerPluginConfigurationCompilerArgumentsParameters()  {
        import("<groupId>test</groupId>" +
                "<artifactId>project</artifactId>" +
                "<version>1</version>" +
                "<build>" +
                "  <plugins>" +
                "    <plugin>" +
                "      <groupId>org.apache.maven.plugins</groupId>" +
                "      <artifactId>maven-compiler-plugin</artifactId>" +
                "      <configuration>" +
                "        <parameters>true</parameters>" +
                "      </configuration>" +
                "    </plugin>" +
                "  </plugins>" +
                "</build>")

        assertModules("project")
        val ideCompilerConfiguration = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
        assertEquals("Javac", ideCompilerConfiguration.defaultCompiler.id)
        assertUnorderedElementsAreEqual(ideCompilerConfiguration.getAdditionalOptions(getModule("project")), "-parameters")
    }

    fun testCompilerPluginConfigurationCompilerArgumentsParametersFalse()  {
        import("<groupId>test</groupId>" +
                "<artifactId>project</artifactId>" +
                "<version>1</version>" +
                "<build>" +
                "  <plugins>" +
                "    <plugin>" +
                "      <groupId>org.apache.maven.plugins</groupId>" +
                "      <artifactId>maven-compiler-plugin</artifactId>" +
                "      <configuration>" +
                "        <parameters>false</parameters>" +
                "      </configuration>" +
                "    </plugin>" +
                "  </plugins>" +
                "</build>")

        assertModules("project")
        val ideCompilerConfiguration = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
        assertEquals("Javac", ideCompilerConfiguration.defaultCompiler.id)
        assertEmpty(ideCompilerConfiguration.getAdditionalOptions(getModule("project")))
    }

    fun testCompilerPluginConfigurationCompilerArgumentsParametersPropertyOverride()  {
        import("<groupId>test</groupId>" +
                "<artifactId>project</artifactId>" +
                "<version>1</version>" +
                "<properties>" +
                "  <maven.compiler.parameters>true</maven.compiler.parameters>" +
                "</properties>" +
                "<build>" +
                "  <plugins>" +
                "    <plugin>" +
                "      <groupId>org.apache.maven.plugins</groupId>" +
                "      <artifactId>maven-compiler-plugin</artifactId>" +
                "      <configuration>" +
                "        <parameters>false</parameters>" +
                "      </configuration>" +
                "    </plugin>" +
                "  </plugins>" +
                "</build>")

        assertModules("project")
        val ideCompilerConfiguration = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
        assertEquals("Javac", ideCompilerConfiguration.defaultCompiler.id)
        assertEmpty(ideCompilerConfiguration.getAdditionalOptions(getModule("project")))
    }

    fun testCompilerPluginConfigurationCompilerArgumentsParametersPropertyOverride1()  {
        import("<groupId>test</groupId>" +
                "<artifactId>project</artifactId>" +
                "<version>1</version>" +
                "<properties>" +
                "  <maven.compiler.parameters>false</maven.compiler.parameters>" +
                "</properties>" +
                "<build>" +
                "  <plugins>" +
                "    <plugin>" +
                "      <groupId>org.apache.maven.plugins</groupId>" +
                "      <artifactId>maven-compiler-plugin</artifactId>" +
                "      <configuration>" +
                "        <parameters>true</parameters>" +
                "      </configuration>" +
                "    </plugin>" +
                "  </plugins>" +
                "</build>")

        assertModules("project")
        val ideCompilerConfiguration = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
        assertEquals("Javac", ideCompilerConfiguration.defaultCompiler.id)
        assertUnorderedElementsAreEqual(ideCompilerConfiguration.getAdditionalOptions(getModule("project")), "-parameters")
    }

    fun testCompilerPluginConfigurationCompilerArgumentsParametersProperty()  {
        import("<groupId>test</groupId>" +
                "<artifactId>project</artifactId>" +
                "<version>1</version>" +
                "<properties>" +
                "  <maven.compiler.parameters>true</maven.compiler.parameters>" +
                "</properties>" +
                "<build>" +
                "  <plugins>" +
                "    <plugin>" +
                "      <groupId>org.apache.maven.plugins</groupId>" +
                "      <artifactId>maven-compiler-plugin</artifactId>" +
                "    </plugin>" +
                "  </plugins>" +
                "</build>")

        assertModules("project")
        val ideCompilerConfiguration = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
        assertEquals("Javac", ideCompilerConfiguration.defaultCompiler.id)
        assertUnorderedElementsAreEqual(ideCompilerConfiguration.getAdditionalOptions(getModule("project")), "-parameters")
    }

   fun testCompilerPluginConfigurationCompilerArgumentsParametersPropertyFalse()  {
        import("<groupId>test</groupId>" +
                "<artifactId>project</artifactId>" +
                "<version>1</version>" +
                "<properties>" +
                "  <maven.compiler.parameters>false</maven.compiler.parameters>" +
                "</properties>" +
                "<build>" +
                "  <plugins>" +
                "    <plugin>" +
                "      <groupId>org.apache.maven.plugins</groupId>" +
                "      <artifactId>maven-compiler-plugin</artifactId>" +
                "    </plugin>" +
                "  </plugins>" +
                "</build>")

        assertModules("project")
        val ideCompilerConfiguration = CompilerConfiguration.getInstance(project) as CompilerConfigurationImpl
        assertEquals("Javac", ideCompilerConfiguration.defaultCompiler.id)
        assertEmpty(ideCompilerConfiguration.getAdditionalOptions(getModule("project")))
    }
}