package ru.rzn.gmyasoedov.gmaven.project.importing

import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.serialization.PropertyMapping
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.Interner

class AnnotationProcessingData @PropertyMapping("path", "arguments", "buildDirectory", "baseDirectory") private constructor(
    path: Collection<String>,
    arguments: Collection<String>,
    buildDirectory: String,
    baseDirectory : String,
) {
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
        this.path = ContainerUtil.immutableList(ArrayList(path))
        this.arguments = ContainerUtil.immutableList(ArrayList(arguments))
        this.buildDirectory = buildDirectory
        this.baseDirectory = baseDirectory
    }


    class AnnotationProcessorOutput @PropertyMapping("outputPath", "testSources") constructor(
        val outputPath: String,
        val isTestSources: Boolean
    )

    companion object {
        val KEY = Key.create(
            AnnotationProcessingData::class.java,
            ExternalSystemConstants.UNORDERED
        )
        val OUTPUT_KEY = Key.create(
            AnnotationProcessorOutput::class.java, ExternalSystemConstants.UNORDERED
        )
        private val ourInterner: Interner<AnnotationProcessingData> = Interner.createWeakInterner()
        fun create(
            path: Collection<String>,
            arguments: Collection<String>,
            buildDirectory: String,
            baseDirectory: String
        ): AnnotationProcessingData {
            return ourInterner.intern(AnnotationProcessingData(path, arguments, buildDirectory, baseDirectory))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnnotationProcessingData

        if (path != other.path) return false
        if (arguments != other.arguments) return false
        if (buildDirectory != other.buildDirectory) return false
        if (baseDirectory != other.baseDirectory) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + arguments.hashCode()
        result = 31 * result + buildDirectory.hashCode()
        result = 31 * result + baseDirectory.hashCode()
        return result
    }

}