package ru.rzn.gmyasoedov.gmaven.ui

import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import icons.OpenapiIcons
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle

object MavenFileType : LanguageFileType(XMLLanguage.INSTANCE, true) {
    override fun getIcon() = OpenapiIcons.RepositoryLibraryLogo
    override fun getName(): String = "GMaven"
    override fun getDescription(): String = GBundle.message("gmaven.name")
    override fun getDisplayName(): String = GBundle.message("gmaven.name")
    override fun getDefaultExtension(): String = "xml"

}