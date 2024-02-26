package ru.rzn.gmyasoedov.gmaven.dom

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider
import com.intellij.openapi.vfs.VirtualFile

class PomFileTypeOverrider : FileTypeOverrider {

    override fun getOverriddenFileType(file: VirtualFile): FileType? {
        return if (file.name.endsWith(".pom")) XmlFileType.INSTANCE else null
    }
}