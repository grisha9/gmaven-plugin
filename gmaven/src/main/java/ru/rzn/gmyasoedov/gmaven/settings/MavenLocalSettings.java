// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.GMavenConstants;

import java.util.HashMap;
import java.util.Map;

@State(name = "MavenLocalSettings", storages = @Storage(StoragePathMacros.CACHE_FILE))
public final class MavenLocalSettings extends AbstractExternalSystemLocalSettings<MavenLocalSettings.MyState>
  implements PersistentStateComponent<MavenLocalSettings.MyState> {

  public MavenLocalSettings(@NotNull Project project) {
    super(GMavenConstants.SYSTEM_ID, project, new MyState());
  }

  @NotNull
  public static MavenLocalSettings getInstance(@NotNull Project project) {
    return project.getService(MavenLocalSettings.class);
  }

  @Nullable
  public String getMavenHome(String linkedProjectPath) {
    return ContainerUtil.notNullize(state.myMavenHomes).get(linkedProjectPath);
  }

  public void setMavenHome(@NotNull String linkedProjectPath, @NotNull String gradleHome) {
    if (state.myMavenHomes == null) {
      state.myMavenHomes = new HashMap<>();
    }
    state.myMavenHomes.put(linkedProjectPath, gradleHome);
  }

  @Override
  public void loadState(@NotNull MyState state) {
    super.loadState(state);
  }

  public static class MyState extends State {
    public String myMavenUserHome;
    public Map<String/* project path */, String> myMavenHomes;
  }
}
