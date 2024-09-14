package ru.rzn.gmyasoedov.gmaven.utils;

import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;
import ru.rzn.gmyasoedov.gmaven.GMavenConstants;

import static icons.GMavenIcons.MavenProject;

public class MavenFileTemplateGroupFactory implements FileTemplateGroupDescriptorFactory {
  public static final String MAVEN_PROJECT_XML_TEMPLATE = "GMavenProject.xml";

  @Override
  public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
    FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor(GMavenConstants.GMAVEN, MavenProject);
    group.addTemplate(new FileTemplateDescriptor(MAVEN_PROJECT_XML_TEMPLATE, MavenProject));
    return group;
  }
}
