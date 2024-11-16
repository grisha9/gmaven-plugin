package ru.rzn.gmyasoedov.gmaven.execution;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.GMavenConstants;

public final class GMavenExternalTaskConfigurationType extends AbstractExternalSystemTaskConfigurationType {
  public GMavenExternalTaskConfigurationType() {
    super(GMavenConstants.SYSTEM_ID);
  }

  @Override
  public String getHelpTopic() {
    return "reference.dialogs.rundebug.GMavenRunConfiguration";
  }

  public static GMavenExternalTaskConfigurationType getInstance() {
    return (GMavenExternalTaskConfigurationType)ExternalSystemUtil.findConfigurationType(GMavenConstants.SYSTEM_ID);
  }

  @Override
  protected @NotNull String getConfigurationFactoryId() {
    return GMavenConstants.SYSTEM_ID.getReadableName();
  }

  @NotNull
  @Override
  protected GMavenRunConfiguration doCreateConfiguration(@NotNull ProjectSystemId externalSystemId,
                                                         @NotNull Project project,
                                                         @NotNull ConfigurationFactory factory,
                                                         @NotNull String name) {
    return new GMavenRunConfiguration(project, factory, name);
  }

  @Override
  public boolean isDumbAware() {
    return true;
  }

  @Override
  protected boolean isEditableInDumbMode() {
    return true;
  }
}


