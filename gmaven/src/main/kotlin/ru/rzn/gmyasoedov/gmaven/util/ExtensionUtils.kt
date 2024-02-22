package ru.rzn.gmyasoedov.gmaven.util

import com.intellij.pom.java.LanguageLevel

fun LanguageLevel.toFeatureString(): String {
    return languageLevelMap.getOrDefault(this, "8")
}

fun LanguageLevel.toFeatureString2(): String {
    return this.toJavaVersion().toFeatureString()
}

private val languageLevelMap = LanguageLevel.values().associateWith { languageLevelToString(it) }

private fun languageLevelToString(languageLevel: LanguageLevel): String {
    val method = languageLevel::class.java.getMethod("toJavaVersion")
    val invoke = method.invoke(LanguageLevel.JDK_10)
    val method1 = invoke::class.java.getMethod("toFeatureString")
    val res = method1.invoke(invoke)
    println(res)
    return res as String
}