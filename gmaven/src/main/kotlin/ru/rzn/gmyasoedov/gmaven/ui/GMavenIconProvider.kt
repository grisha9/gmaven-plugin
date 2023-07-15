package ru.rzn.gmyasoedov.gmaven.ui

import com.intellij.openapi.externalSystem.ui.ExternalSystemIconProvider
import icons.GMavenIcons
import javax.swing.Icon

class GMavenIconProvider : ExternalSystemIconProvider {

  override val reloadIcon: Icon = GMavenIcons.MavenLoadChanges

  override val projectIcon: Icon = GMavenIcons.MavenProject
}