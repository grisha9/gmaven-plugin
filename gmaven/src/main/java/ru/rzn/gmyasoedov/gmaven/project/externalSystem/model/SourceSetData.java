// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.project.externalSystem.model;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.GMavenConstants;
import ru.rzn.gmyasoedov.gmaven.project.ProjectResolverUtils;

public final class SourceSetData extends ModuleData {
  @NotNull
  public static final Key<SourceSetData> KEY = Key.create(SourceSetData.class, ProjectKeys.MODULE.getProcessingWeight() + 1);

  @PropertyMapping({"id", "externalName", "internalName", "moduleFileDirectoryPath", "externalConfigPath"})
  public SourceSetData(@NotNull String id,
                       @NotNull String externalName,
                       @NotNull String internalName,
                       @NotNull String moduleFileDirectoryPath,
                       @NotNull String externalConfigPath) {
    super(id, GMavenConstants.SYSTEM_ID, ProjectResolverUtils.getDefaultModuleTypeId(),
          externalName, internalName,
          moduleFileDirectoryPath, externalConfigPath);
    setModuleName(getSourceSetName());
  }

  @NotNull
  @Override
  public String getIdeGrouping() {
    return super.getIdeGrouping() + ":" + getSourceSetName();
  }

  @Override
  @NotNull
  public String getIdeParentGrouping() {
    return super.getIdeGrouping();
  }

  private String getSourceSetName() {
    return StringUtil.substringAfterLast(getExternalName(), ":");
  }
}
