package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin.kotlin

import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.serialization.PropertyMapping

class KotlinMavenPluginData @PropertyMapping(
    "jvmTarget", "jdkHome", "kotlinVersion",
    "languageVersion", "apiVersion", "noWarn",
    "arguments", "compilerPlugins", "pluginOptions"
) constructor(
    jvmTarget: String,
    jdkHome: String?,
    kotlinVersion: String,
    languageVersion: String?,
    apiVersion: String?,
    noWarn: Boolean,
    arguments: List<String>,
    compilerPlugins: List<String>,
    pluginOptions: List<String>,
) {
    val jvmTarget: String
    val jdkHome: String?
    val kotlinVersion: String
    val languageVersion: String?
    val apiVersion: String?
    val noWarn: Boolean
    val arguments: List<String>
    val compilerPlugins: List<String>
    val pluginOptions: List<String>

    init {
        this.jvmTarget = jvmTarget
        this.jdkHome = jdkHome
        this.kotlinVersion = kotlinVersion
        this.languageVersion = languageVersion
        this.apiVersion = apiVersion
        this.noWarn = noWarn
        this.arguments = arguments
        this.compilerPlugins = compilerPlugins
        this.pluginOptions = pluginOptions
    }

    companion object {
        val KEY = Key.create(KotlinMavenPluginData::class.java, ProjectKeys.MODULE.processingWeight + 1)
    }
}