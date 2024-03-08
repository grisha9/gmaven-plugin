package ru.rzn.gmyasoedov.gmaven.util

import com.intellij.pom.java.LanguageLevel


fun LanguageLevel.toFeatureString(): String {
    return this.toJavaVersion().toFeatureString()
}