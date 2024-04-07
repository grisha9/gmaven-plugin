package ru.rzn.gmyasoedov.gmaven.extensionpoints.plugin

import com.intellij.openapi.util.text.Strings
import com.intellij.util.containers.ContainerUtil
import org.jdom.Element
import ru.rzn.gmyasoedov.serverapi.model.MavenProject

private const val propStartTag = "\${"
private const val propEndTag = "}"

const val PROC_NONE = "-proc:none"
const val PROC_ONLY = "-proc:only"

fun collectCompilerArgs(mavenProject: MavenProject, pluginConfiguration: Element?): List<String> {
    val options = mutableListOf<String>()
    val parameters = pluginConfiguration?.getChild("parameters")
    val propertyCompilerParameters = mavenProject.properties["maven.compiler.parameters"] as? String
    if (parameters?.textTrim?.toBoolean() == true) {
        options += "-parameters"
    } else if (parameters == null && propertyCompilerParameters?.toBoolean() == true) {
        options += "-parameters"
    }

    if (pluginConfiguration == null) return options

    val procElement = pluginConfiguration.getChild("proc")
    procElement?.value?.also {
        when (it) {
            "none" -> options += PROC_NONE
            "only" -> options += PROC_ONLY
        }
    }

    val compilerArguments = pluginConfiguration.getChild("compilerArguments")
    if (compilerArguments != null) {
        val unresolvedArgs = mutableSetOf<String>()
        val effectiveArguments = compilerArguments.children.map {
            val key = it.name.run { if (startsWith("-")) this else "-$this" }
            val value = getResolvedText(it)
            if (value == null && hasUnresolvedProperty(it.textTrim)) {
                unresolvedArgs += key
            }
            key to value
        }.toMap()

        effectiveArguments.forEach { key, value ->
            if (key.startsWith("-A") && value != null) {
                options.add("$key=$value")
            } else if (key !in unresolvedArgs) {
                options.add(key)
                ContainerUtil.addIfNotNull(options, value)
            }
        }
    }

    ContainerUtil.addIfNotNull(
        options,
        getResolvedText(pluginConfiguration.getChildTextTrim("compilerArgument"))
    )

    val compilerArgs = pluginConfiguration.getChild("compilerArgs")
    if (compilerArgs != null) {
        if (compilerArgs.children.isEmpty()) {
            ContainerUtil.addIfNotNull(options, getResolvedText(compilerArgs.value))
        } else {
            for (arg in compilerArgs.children) {
                ContainerUtil.addIfNotNull(options, getResolvedText(arg))
            }
        }
    }
    return options;
}

private fun hasUnresolvedProperty(txt: String): Boolean {
    val i = txt.indexOf(propStartTag)
    return i >= 0 && findClosingBraceOrNextUnresolvedProperty(i + 1, txt) != -1
}

private fun findClosingBraceOrNextUnresolvedProperty(index: Int, s: String): Int {
    if (index == -1) return -1
    val pair = s.findAnyOf(listOf(propEndTag, propStartTag), index) ?: return -1
    if (pair.second == propEndTag) return pair.first
    val nextIndex = if (pair.second == propStartTag) pair.first + 2 else pair.first + 1
    return findClosingBraceOrNextUnresolvedProperty(nextIndex, s)
}

private fun getResolvedText(txt: String?): String? {
    val result = Strings.nullize(txt) ?: return null
    if (hasUnresolvedProperty(result)) return null
    return result
}

private fun getResolvedText(it: Element): String? {
    return getResolvedText(it.textTrim)
}
