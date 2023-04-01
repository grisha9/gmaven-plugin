package ru.rzn.gmyasoedov.gmaven.settings

import java.io.Serializable
import java.nio.file.Path

data class DistributionSettings(val type: DistributionType, val path: Path? = null, val url: String? = null) : Serializable