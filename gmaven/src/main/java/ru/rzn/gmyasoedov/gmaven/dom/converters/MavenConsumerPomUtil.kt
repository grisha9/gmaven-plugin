package org.jetbrains.idea.maven.dom.converters

import com.intellij.openapi.project.Project
import com.intellij.psi.xml.XmlFile
import com.intellij.util.xml.ConvertContext
import com.intellij.util.xml.GenericDomValue
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomParent
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomProjectModel
import ru.rzn.gmyasoedov.gmaven.utils.MavenDomUtil

object MavenConsumerPomUtil {
  @JvmStatic
  fun isConsumerPomResolutionApplicable(project: Project): Boolean {
    //return Registry.`is`("maven.consumer.pom.support")
    return true;
  }

  @JvmStatic
  fun getParentVersionForConsumerPom(context: ConvertContext): String? {
    return getDerivedPropertiesForConsumerPom(context) { it.version }
  }

  @JvmStatic
  fun getParentGroupForConsumerPom(context: ConvertContext): String? {
    return getDerivedPropertiesForConsumerPom(context) { it.groupId }
  }

  @JvmStatic
  fun getDerivedPropertiesForConsumerPom(context: ConvertContext, extractor: (MavenDomProjectModel) -> GenericDomValue<String>): String? {

    val parentElement = getMavenParentElementFromContext(context) ?: return null
    val artifactId = parentElement.artifactId.value
    val groupId = parentElement.groupId.value
    if (artifactId == null || groupId == null) return null

    return getDerivedParentPropertyForConsumerPom(context.file, artifactId, groupId, extractor)
  }

  @JvmStatic
  fun getDerivedParentPropertyForConsumerPom(currentPomFile: XmlFile,
                                             parentElementArtifactId: String,
                                             parentElementGroupId: String,
                                             extractor: (MavenDomProjectModel) -> GenericDomValue<String>): String? {
    val parentPsi = currentPomFile.parent?.parent?.findFile("pom.xml") as? XmlFile ?: return null
    val mavenParentDomPsiModel = MavenDomUtil.getMavenDomModel(parentPsi, MavenDomProjectModel::class.java) ?: return null
    val parentRealGroupId = mavenParentDomPsiModel.groupId.value ?: mavenParentDomPsiModel.mavenParent.groupId.value
    if (mavenParentDomPsiModel.artifactId.value == parentElementArtifactId && parentRealGroupId == parentElementGroupId) {
      return extractor(mavenParentDomPsiModel).value
    }
    return null
  }

  private fun getMavenParentElementFromContext(context: ConvertContext): MavenDomParent? {
    val mavenDomParent = context.invocationElement.parent as? MavenDomParent
    if (mavenDomParent != null) return mavenDomParent
    return (context.invocationElement.parent as? MavenDomProjectModel)?.mavenParent
  }
}