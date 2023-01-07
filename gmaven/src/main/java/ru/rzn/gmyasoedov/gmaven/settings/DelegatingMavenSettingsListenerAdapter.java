/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.externalSystem.settings.DelegatingExternalSystemSettingsListener;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Denis Zhdanov
 */
public class DelegatingMavenSettingsListenerAdapter
        extends DelegatingExternalSystemSettingsListener<MavenProjectSettings>
        implements MavenSettingsListener {

    public DelegatingMavenSettingsListenerAdapter(@NotNull ExternalSystemSettingsListener<MavenProjectSettings> delegate) {
        super(delegate);
    }


    @Override
    public void onMavenHomeChange(@Nullable String oldPath, @Nullable String newPath, @NotNull String linkedProjectPath) {
    }

    @Override
    public void onServiceDirectoryPathChange(@Nullable String oldPath, @Nullable String newPath) {

    }

    @Override
    public void onVmOptionsChange(@Nullable String oldOptions, @Nullable String newOptions) {

    }
}
