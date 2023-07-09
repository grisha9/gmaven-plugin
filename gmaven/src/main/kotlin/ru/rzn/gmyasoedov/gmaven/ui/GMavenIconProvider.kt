// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.ui

import com.intellij.openapi.externalSystem.ui.ExternalSystemIconProvider
import icons.GMavenIcons
import javax.swing.Icon

class GMavenIconProvider : ExternalSystemIconProvider {

  override val reloadIcon: Icon = GMavenIcons.MavenLoadChanges

  override val projectIcon: Icon = GMavenIcons.MavenProject
}