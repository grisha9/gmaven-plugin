package ru.rzn.gmyasoedov.gmaven.project.importing

import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.serialization.PropertyMapping
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.Interner
import ru.rzn.gmyasoedov.serverapi.model.DependencyTreeNode

class DependencyGraphData @PropertyMapping("treeNodes", "artifactId", "group", "version") private constructor(
    treeNodes: List<DependencyTreeNode>,
    artifactId: String,
    group: String,
    version: String,
) {
    val treeNodes: List<DependencyTreeNode>
    val artifactId: String
    val group: String
    val version: String

    init {
        this.treeNodes = ContainerUtil.immutableList(treeNodes)
        this.artifactId = artifactId;
        this.group = group
        this.version = version
    }

    companion object {
        val KEY = Key.create(
            DependencyGraphData::class.java,
            ExternalSystemConstants.UNORDERED
        )

        private val ourInterner: Interner<DependencyGraphData> = Interner.createWeakInterner()
        fun create(
            treeNodes: List<DependencyTreeNode>,
            artifactId: String,
            group: String,
            version: String,
        ): DependencyGraphData {
            return ourInterner.intern(DependencyGraphData(treeNodes, artifactId, group, version))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DependencyGraphData

        if (treeNodes != other.treeNodes) return false
        return true
    }

    override fun hashCode(): Int {
        return treeNodes.hashCode()
    }

}