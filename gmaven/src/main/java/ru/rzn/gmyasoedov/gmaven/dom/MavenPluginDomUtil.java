package ru.rzn.gmyasoedov.gmaven.dom;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlElement;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomConfiguration;
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomPlugin;
import ru.rzn.gmyasoedov.gmaven.dom.plugin.MavenDomPluginModel;
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectsManager;
import ru.rzn.gmyasoedov.gmaven.utils.MavenArtifactUtil;
import ru.rzn.gmyasoedov.gmaven.utils.MavenDomUtil;
import ru.rzn.gmyasoedov.serverapi.model.MavenPlugin;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

public final class MavenPluginDomUtil {

  @Nullable
  public static MavenProject findMavenProject(@NotNull DomElement domElement) {
    XmlElement xmlElement = domElement.getXmlElement();
    if (xmlElement == null) return null;
    PsiFile psiFile = xmlElement.getContainingFile();
    if (psiFile == null) return null;
    VirtualFile file = psiFile.getVirtualFile();
    if (file == null) return null;
    return MavenProjectsManager.getInstance(psiFile.getProject()).findProject(file);
  }

  @Nullable
  public static MavenDomPluginModel getMavenPluginModel(DomElement element) {
    Project project = element.getManager().getProject();

    MavenDomPlugin pluginElement = element.getParentOfType(MavenDomPlugin.class, false);
    if (pluginElement == null) return null;

    String groupId = pluginElement.getGroupId().getStringValue();
    String artifactId = pluginElement.getArtifactId().getStringValue();
    String version = pluginElement.getVersion().getStringValue();
    if (StringUtil.isEmpty(version)) {
      MavenProject mavenProject = findMavenProject(element);
      if (mavenProject != null) {
        for (MavenPlugin plugin : mavenProject.getPlugins()) {
          if (MavenArtifactUtil.isPluginIdEquals(groupId, artifactId, plugin.getGroupId(), plugin.getArtifactId())) {
            version = plugin.getVersion();
            break;
          }
        }
      }
    }
    return getMavenPluginModel(project, groupId, artifactId, version);
  }

  @Nullable
  public static MavenDomPluginModel getMavenPluginModel(Project project, String groupId, String artifactId, String version) {
    VirtualFile pluginXmlFile = getPluginXmlFile(project, groupId, artifactId, version);
    if (pluginXmlFile == null) return null;

    return MavenDomUtil.getMavenDomModel(project, pluginXmlFile, MavenDomPluginModel.class);
  }

  public static boolean isPlugin(@NotNull MavenDomConfiguration configuration, @Nullable String groupId, @NotNull String artifactId) {
    MavenDomPlugin domPlugin = configuration.getParentOfType(MavenDomPlugin.class, true);
    if (domPlugin == null) return false;

    return isPlugin(domPlugin, groupId, artifactId);
  }

  public static boolean isPlugin(@NotNull MavenDomPlugin plugin, @Nullable String groupId, @NotNull String artifactId) {
    if (!artifactId.equals(plugin.getArtifactId().getStringValue())) return false;

    String pluginGroupId = plugin.getGroupId().getStringValue();

    if (groupId == null) {
      return pluginGroupId == null || (pluginGroupId.equals("org.apache.maven.plugins") || pluginGroupId.equals("org.codehaus.mojo"));
    }

    if (pluginGroupId == null && (groupId.equals("org.apache.maven.plugins") || groupId.equals("org.codehaus.mojo"))) {
      return true;
    }

    return groupId.equals(pluginGroupId);
  }

  @Nullable
  private static VirtualFile getPluginXmlFile(Project project, String groupId, String artifactId, String version) {
   /* Path file = MavenArtifactUtil.getArtifactNioPath(MavenProjectsManager.getInstance(project).getLocalRepository(),
                                                     groupId, artifactId, version, "jar");
    VirtualFile pluginFile = LocalFileSystem.getInstance().findFileByNioFile(file);
    if (pluginFile == null) return null;

    VirtualFile pluginJarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(pluginFile);
    if (pluginJarRoot == null) return null;
    return pluginJarRoot.findFileByRelativePath(MavenArtifactUtil.MAVEN_PLUGIN_DESCRIPTOR);*/
    return null;
  }
}
