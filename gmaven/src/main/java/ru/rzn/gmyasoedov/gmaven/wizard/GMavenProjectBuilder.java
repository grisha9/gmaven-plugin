// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package ru.rzn.gmyasoedov.gmaven.wizard;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ExternalStorageConfigurationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.projectImport.ProjectImportBuilder;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.GMavenConstants;
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static icons.OpenapiIcons.RepositoryLibraryLogo;

public final class GMavenProjectBuilder extends ProjectImportBuilder<MavenProject> {
    private static final Logger LOG = Logger.getInstance(GMavenProjectBuilder.class);

    private static class Parameters {
        private Project myProjectToUpdate;


        private Path myImportRootDirectory;
        private Path myImportProjectFile;
        private List<VirtualFile> myFiles;

        private List<MavenProject> mySelectedProjects;

        private boolean myOpenModulesConfigurator;
    }

    private Parameters myParameters;

    @Override
    @NotNull
    public String getName() {
        return GMavenConstants.GMAVEN;
    }

    @Override
    public Icon getIcon() {
        return RepositoryLibraryLogo;
    }

    @Override
    public boolean isMarked(MavenProject element) {
        return false;
    }

    @Override
    public void setOpenProjectSettingsAfter(boolean on) {

    }

    @Override
    public void cleanup() {
        myParameters = null;
        super.cleanup();
    }

    @Override
    public boolean isSuitableSdkType(SdkTypeId sdk) {
        return sdk == JavaSdk.getInstance();
    }

    private Parameters getParameters() {
        if (myParameters == null) {
            myParameters = new Parameters();
        }
        return myParameters;
    }

    @Override
    public List<Module> commit(Project project,
                               ModifiableModuleModel model,
                               ModulesProvider modulesProvider,
                               ModifiableArtifactModel artifactModel) {
        boolean isVeryNewProject = project.getUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT) == Boolean.TRUE;
        if (isVeryNewProject) {
            ExternalStorageConfigurationManager.getInstance(project).setEnabled(true);
        }

        if (ApplicationManager.getApplication().isDispatchThread()) {
            FileDocumentManager.getInstance().saveAllDocuments();
        }

        MavenUtils.setupProjectSdk(project);

        /*if (!setupProjectImport(project)) {
            LOG.debug(String.format("Cannot import project for %s", project.toString()));
            return Collections.emptyList();
        }


        MavenProjectsManager manager = MavenProjectsManager.getInstance(project);*/

        return Collections.emptyList();
    }


}
