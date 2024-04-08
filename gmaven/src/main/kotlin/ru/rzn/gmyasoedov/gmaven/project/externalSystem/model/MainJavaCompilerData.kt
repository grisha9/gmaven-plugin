package ru.rzn.gmyasoedov.gmaven.project.externalSystem.model

import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.serialization.PropertyMapping
import ru.rzn.gmyasoedov.gmaven.GMavenConstants

class MainJavaCompilerData @PropertyMapping("compilerId", "dependenciesPath", "arguments") private constructor(
    compilerId: String,
    dependenciesPath: Collection<String>,
    arguments: Collection<String>,
) : AbstractExternalEntityData(GMavenConstants.SYSTEM_ID) {
    val compilerId: String
    val dependenciesPath: Collection<String>
    val arguments: Collection<String>

    init {
        this.compilerId = compilerId
        this.dependenciesPath = dependenciesPath
        this.arguments = arguments
    }


    companion object {
        const val GROOVY_ECLIPSE_COMPILER_ID = "groovy-eclipse-compiler"
        const val ECLIPSE_COMPILER_ID = "eclipse"
        const val ASPECTJ_COMPILER_ID = "ajc"
        private const val JAVAC_COMPILER_ID = "javac"

        val KEY = Key.create(
            MainJavaCompilerData::class.java,
            ExternalSystemConstants.UNORDERED
        )

        fun createDefault() = MainJavaCompilerData(JAVAC_COMPILER_ID, emptyList(), emptyList())

        fun create(
            compilerId: String,
            dependenciesPath: Collection<String>,
            arguments: Collection<String>
        ): MainJavaCompilerData {
            return MainJavaCompilerData(compilerId, dependenciesPath, arguments)
        }
    }

}