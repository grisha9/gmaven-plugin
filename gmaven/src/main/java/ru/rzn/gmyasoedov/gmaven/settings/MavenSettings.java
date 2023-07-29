package ru.rzn.gmyasoedov.gmaven.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.openapi.project.ExternalStorageConfigurationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@State(name = "MavenSettings", storages = @Storage("gmaven.xml"))
public class MavenSettings extends AbstractExternalSystemSettings<MavenSettings, MavenProjectSettings, MavenSettingsListener>
        implements PersistentStateComponent<MavenSettings.MyState> {

    private boolean offlineMode = false;
    private boolean skipTests = false;

    public MavenSettings(@NotNull Project project) {
        super(MavenSettingsListener.TOPIC, project);
    }

    @NotNull
    public static MavenSettings getInstance(@NotNull Project project) {
        return project.getService(MavenSettings.class);
    }


    @Override
    public void subscribe(@NotNull ExternalSystemSettingsListener<MavenProjectSettings> listener,
                          @NotNull Disposable parentDisposable) {
        doSubscribe(new DelegatingMavenSettingsListenerAdapter(listener), parentDisposable);
    }

    @Override
    protected void copyExtraSettingsFrom(@NotNull MavenSettings settings) {
    }

    @Nullable
    @Override
    public MavenSettings.MyState getState() {
        MyState state = new MyState();
        fillState(state);

        state.setOfflineMode(isOfflineMode());
        state.setSkipTests(isSkipTests());
        return state;
    }

    @Override
    public void loadState(@NotNull MyState state) {
        super.loadState(state);
        setOfflineMode(state.isOfflineMode());
        setSkipTests(state.skipTests);
    }

    public boolean isOfflineMode() {
        return offlineMode;
    }

    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
    }

    public boolean isSkipTests() {
        return skipTests;
    }

    public void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

    public boolean getStoreProjectFilesExternally() {
        return ExternalStorageConfigurationManager.getInstance(getProject()).isEnabled();
    }

    public void setStoreProjectFilesExternally(boolean value) {
        ExternalProjectsManagerImpl.getInstance(getProject()).setStoreExternally(value);
    }

    @Override
    protected void checkSettings(@NotNull MavenProjectSettings old, @NotNull MavenProjectSettings current) {
        if (!Objects.equals(old.getDistributionSettings(), current.getDistributionSettings())) {
            /*getPublisher().onMavenHomeChange(
                    old.getDistributionSettings().getPath().toString(), //todo
                    current.getDistributionSettings().getPath().toString(),
                    current.getExternalProjectPath());*/
        }
        /*if (old.getDistributionType() != current.getDistributionType()) {
            getPublisher().onGradleDistributionTypeChange(current.getDistributionType(), current.getExternalProjectPath());
        }*/
        if (old.getResolveModulePerSourceSet() != current.getResolveModulePerSourceSet()) {
            ExternalProjectsManager.getInstance(getProject()).getExternalProjectsWatcher().markDirty(current.getExternalProjectPath());
        }
    }

    @Override
    public @Nullable MavenProjectSettings getLinkedProjectSettings(@NotNull String projectPath) {
        Path projectAbsolutePath = Path.of(projectPath).toAbsolutePath();
        MavenProjectSettings projectSettings = super.getLinkedProjectSettings(projectPath);
        if (projectSettings == null) {
            for (MavenProjectSettings setting : getLinkedProjectsSettings()) {
                Path settingPath = Path.of(setting.getExternalProjectPath()).toAbsolutePath();
                if (FileUtil.isAncestor(settingPath.toFile(), projectAbsolutePath.toFile(), false)) {
                    return setting;
                }
            }
        }
        return projectSettings;
    }

    public static class MyState implements State<MavenProjectSettings> {
        private final Set<MavenProjectSettings> myProjectSettings = new TreeSet<>();
        private boolean isOfflineMode = false;
        private boolean skipTests = false;


        @Override
        @XCollection(elementTypes = MavenProjectSettings.class)
        public Set<MavenProjectSettings> getLinkedExternalProjectsSettings() {
            return myProjectSettings;
        }

        @Override
        public void setLinkedExternalProjectsSettings(Set<MavenProjectSettings> settings) {
            if (settings != null) {
                myProjectSettings.addAll(settings);
            }
        }

        public boolean isOfflineMode() {
            return isOfflineMode;
        }

        public void setOfflineMode(boolean isOfflineMode) {
            this.isOfflineMode = isOfflineMode;
        }

        public boolean isSkipTests() {
            return skipTests;
        }

        public void setSkipTests(boolean skipTests) {
            this.skipTests = skipTests;
        }
    }
}