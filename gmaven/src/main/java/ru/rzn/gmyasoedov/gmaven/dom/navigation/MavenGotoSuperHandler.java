package ru.rzn.gmyasoedov.gmaven.dom.navigation;

import com.intellij.util.PsiNavigateUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomElementNavigationProvider;
import ru.rzn.gmyasoedov.gmaven.dom.MavenDomProjectProcessorUtils;
import ru.rzn.gmyasoedov.gmaven.dom.model.*;

public class MavenGotoSuperHandler extends DomElementNavigationProvider {
  @Override
  public String getProviderName() {
    return "MAVEN_GOTO_SUPER";
  }

  @Override
  public void navigate(DomElement domElement, boolean requestFocus) {
    DomElement target = null;
    if (domElement instanceof MavenDomDependency) {
      target = MavenDomProjectProcessorUtils.searchManagingDependency((MavenDomDependency)domElement);
    }
    else if (domElement instanceof MavenDomPlugin) {
      target = MavenDomProjectProcessorUtils.searchManagingPlugin((MavenDomPlugin)domElement);
    }
    else if (domElement instanceof MavenDomParent) {
      target = MavenDomProjectProcessorUtils.findParent((MavenDomParent)domElement, domElement.getManager().getProject());
    }
    if (target != null) {
      PsiNavigateUtil.navigate(target.getXmlTag(), requestFocus);
    }
  }

  @Override
  public boolean canNavigate(DomElement domElement) {
    return (domElement instanceof MavenDomDependency
            && domElement.getParentOfType(MavenDomDependencyManagement.class, false) == null)
           || (domElement instanceof MavenDomPlugin
               && domElement.getParentOfType(MavenDomPluginManagement.class, false) == null)
           || domElement instanceof MavenDomParent;
  }
}
