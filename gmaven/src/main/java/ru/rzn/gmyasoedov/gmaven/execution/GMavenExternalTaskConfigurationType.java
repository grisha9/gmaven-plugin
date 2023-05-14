// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.execution;

import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
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

  @Override
  public boolean isDumbAware() {
    return true;
  }

  @Override
  protected boolean isEditableInDumbMode() {
    return true;
  }
}


