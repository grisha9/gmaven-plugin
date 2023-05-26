package ru.rzn.gmyasoedov.gmaven.wizard;

import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle;
import ru.rzn.gmyasoedov.gmaven.settings.MavenProjectSettings;
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils;
import ru.rzn.gmyasoedov.serverapi.model.MavenId;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static icons.OpenapiIcons.RepositoryLibraryLogo;
import static ru.rzn.gmyasoedov.gmaven.GMavenConstants.GMAVEN;

public class GMavenModuleBuilder extends ModuleBuilder implements SourcePathsBuilder {
    private MavenProject myAggregatorProject;
    private MavenProject myParentProject;

    private boolean myInheritGroupId;
    private boolean myInheritVersion;
    private MavenId myProjectId;

    @Override
    protected void setupModule(Module module) throws ConfigurationException {
        super.setupModule(module);
        ExternalSystemModulePropertyManager.getInstance(module).setMavenized(true);
    }

    @Override
    public void setupRootModel(@NotNull ModifiableRootModel rootModel) {
        final Project project = rootModel.getProject();

        final VirtualFile root = createAndGetContentEntry();
        rootModel.addContentEntry(root);

        // todo this should be moved to generic ModuleBuilder
        if (myJdk != null) {
            rootModel.setSdk(myJdk);
        } else {
            rootModel.inheritSdk();
        }

        String modulePathString = root.toNioPath().toAbsolutePath().toString();
        var settings = new MavenProjectSettings();
       /* File mavenHome = MavenUtils.resolveMavenHome(); todo
        if (mavenHome == null) throw new RuntimeException("no maven home");*/
        settings.setExternalProjectPath(modulePathString);
        settings.setProjectDirectory(modulePathString);
        settings.setJdkName(myJdk.getName());

        MavenUtils.runWhenInitialized(project, (DumbAwareRunnable)
                () -> new GMavenModuleBuilderHelper(
                        myProjectId, myAggregatorProject, myParentProject,
                        myInheritGroupId, myInheritVersion, settings,
                        GBundle.message("command.name.create.new.maven.module")
                ).configure(project, root, false)
        );
    }

    //todo IDEA-85478 Maven compiler plugin testSource and testTarget  MY CHANGES!!!
   /* @Override
    public @Nullable Module commitModule(@NotNull Project project, @Nullable ModifiableModuleModel model) {
        setMavenModuleFilePath(project, getName());
        return super.commitModule(project, model);
    }

    private void setMavenModuleFilePath(@NotNull Project project, @NotNull String moduleName) {
        if (myParentProject == null) return;

        String parentModuleName = MavenImportUtil.getModuleName(myParentProject, project);
        if (StringUtil.isNotEmpty(parentModuleName)) {
            String moduleFilePath =
                    project.getBasePath() + File.separator + parentModuleName + "." + moduleName + ModuleFileType.DOT_DEFAULT_EXTENSION;
            setModuleFilePath(moduleFilePath);
        }
    }
*/
    @Override
    public @NonNls String getBuilderId() {
        return getClass().getName();
    }

    @Override
    public String getPresentableName() {
        return GMAVEN;
    }

    @Override
    public String getParentGroup() {
        return JavaModuleType.JAVA_GROUP;
    }

    @Override
    public int getWeight() {
        return JavaModuleBuilder.BUILD_SYSTEM_WEIGHT;
    }

    @Override
    public String getDescription() {
        return GBundle.message("maven.builder.module.builder.description");
    }

    @Override
    public Icon getNodeIcon() {
        return RepositoryLibraryLogo;
    }

    @Override
    public ModuleType getModuleType() {
        return StdModuleTypes.JAVA;
    }

    @Override
    public boolean isSuitableSdkType(SdkTypeId sdk) {
        return sdk == JavaSdk.getInstance();
    }

    private VirtualFile createAndGetContentEntry() {
        String path = FileUtil.toSystemIndependentName(getContentEntryPath());
        new File(path).mkdirs();
        return LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
    }

    @Override
    public List<Pair<String, String>> getSourcePaths() {
        return Collections.emptyList();
    }

    @Override
    public void setSourcePaths(List<Pair<String, String>> sourcePaths) {
    }

    @Override
    public void addSourcePath(Pair<String, String> sourcePathInfo) {
    }

    public void setAggregatorProject(MavenProject project) {
        myAggregatorProject = project;
    }

    public MavenProject getAggregatorProject() {
        return myAggregatorProject;
    }

    public void setParentProject(MavenProject project) {
        myParentProject = project;
    }

    public MavenProject getParentProject() {
        return myParentProject;
    }

    public void setInheritedOptions(boolean groupId, boolean version) {
        myInheritGroupId = groupId;
        myInheritVersion = version;
    }

    public boolean isInheritGroupId() {
        return myInheritGroupId;
    }

    public void setInheritGroupId(boolean inheritGroupId) {
        myInheritGroupId = inheritGroupId;
    }

    public boolean isInheritVersion() {
        return myInheritVersion;
    }

    public void setInheritVersion(boolean inheritVersion) {
        myInheritVersion = inheritVersion;
    }

    public void setProjectId(MavenId id) {
        myProjectId = id;
    }

    public MavenId getProjectId() {
        return myProjectId;
    }

    @Override
    public String getGroupName() {
        return GMAVEN;
    }

    @Nullable
    @Override
    public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
        final ModuleNameLocationSettings nameLocationSettings = settingsStep.getModuleNameLocationSettings();
        if (nameLocationSettings != null && myProjectId != null) {
            nameLocationSettings.setModuleName(StringUtil.sanitizeJavaIdentifier(myProjectId.getArtifactId()));
            if (myAggregatorProject != null) {
                Path path = Path.of(
                        myAggregatorProject.getParentFile().getAbsolutePath(),
                        myProjectId.getArtifactId());
                nameLocationSettings.setModuleContentRoot(path.toString());
            }
        }
        return super.modifySettingsStep(settingsStep);
    }

    @Nullable
    @Override
    public Project createProject(String name, String path) {
        Project project = super.createProject(name, path);
        if (project != null) {
            ExternalProjectsManagerImpl.setupCreatedProject(project);
        }
        return project;
    }
}
