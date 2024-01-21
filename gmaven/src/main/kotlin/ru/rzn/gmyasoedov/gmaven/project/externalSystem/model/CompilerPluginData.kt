package ru.rzn.gmyasoedov.gmaven.project.externalSystem.model

import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.serialization.PropertyMapping
import ru.rzn.gmyasoedov.gmaven.GMavenConstants

class CompilerPluginData @PropertyMapping("path", "arguments", "buildDirectory", "baseDirectory") private constructor(
    path: Collection<String>,
    arguments: Collection<String>,
    buildDirectory: String,
    baseDirectory : String,
) : AbstractExternalEntityData(GMavenConstants.SYSTEM_ID) {
    /**
     * Annotation processor path
     *
     * @return immutable collection of path elements
     */
    val path: Collection<String>

    /**
     * Annotation processor arguments
     *
     * @return immutable collection of arguments
     */
    val arguments: Collection<String>

    /**
     * Module build dir
     *
     * @return path to build dir
     */
    val buildDirectory: String

    /**
     * Module base dir
     *
     * @return path to build dir
     */
    val baseDirectory: String

    init {
        this.path = path.toList()
        this.arguments = arguments.toList()
        this.buildDirectory = buildDirectory
        this.baseDirectory = baseDirectory
    }


    companion object {
        val KEY = Key.create(
            CompilerPluginData::class.java,
            ExternalSystemConstants.UNORDERED
        )

        fun create(
            path: Collection<String>,
            arguments: Collection<String>,
            buildDirectory: String,
            baseDirectory: String
        ): CompilerPluginData {
            return CompilerPluginData(path, arguments, buildDirectory, baseDirectory)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompilerPluginData

        if (path != other.path) return false
        if (arguments != other.arguments) return false
        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + arguments.hashCode()
        return result
    }

    override fun toString(): String {
        return "CompilerPluginData(baseDirectory='$baseDirectory', path=$path, arguments=$arguments)"
    }
}