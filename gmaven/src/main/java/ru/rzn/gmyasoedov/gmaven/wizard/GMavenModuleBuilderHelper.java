package ru.rzn.gmyasoedov.gmaven.wizard;

import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.GitSilentFileAdder;
import com.intellij.openapi.GitSilentFileAdderProvider;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.xml.XmlElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.GMavenConstants;
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle;
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomModule;
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomProjectModel;
import ru.rzn.gmyasoedov.gmaven.settings.MavenProjectSettings;
import ru.rzn.gmyasoedov.gmaven.utils.MavenDomUtil;
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils;
import ru.rzn.gmyasoedov.serverapi.model.MavenId;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ru.rzn.gmyasoedov.gmaven.GMavenConstants.SYSTEM_ID;

public class GMavenModuleBuilderHelper {
    private final MavenId myProjectId;

    private final MavenProject myAggregatorProject;
    private final MavenProject myParentProject;

    private final boolean myInheritGroupId;
    private final boolean myInheritVersion;

    private final MavenProjectSettings settings;

    @NlsContexts.Command
    private final String myCommandName;

    public GMavenModuleBuilderHelper(@NotNull MavenId projectId,
                                     MavenProject aggregatorProject,
                                     MavenProject parentProject,
                                     boolean inheritGroupId,
                                     boolean inheritVersion,
                                     @NotNull MavenProjectSettings settings,
                                     @NlsContexts.Command String commandName) {
        myProjectId = projectId;
        myAggregatorProject = aggregatorProject;
        myParentProject = parentProject;
        myInheritGroupId = inheritGroupId;
        myInheritVersion = inheritVersion;
        myCommandName = commandName;
        this.settings = settings;
    }

    public void configure(final Project project, final VirtualFile root, final boolean isInteractive) {
        PsiFile[] psiFiles = myAggregatorProject != null
                ? new PsiFile[]{getPsiFile(project, MavenUtils.getVFile(myAggregatorProject.getFile()))}
                : PsiFile.EMPTY_ARRAY;
        final VirtualFile pom = WriteCommandAction.writeCommandAction(project, psiFiles).withName(myCommandName)
                .compute(() -> {
                    GitSilentFileAdder vcsFileAdder = GitSilentFileAdderProvider.create(project);
                    VirtualFile file = null;
                    try {
                        try {
                            file = root.findChild(GMavenConstants.POM_XML);
                            if (file != null) file.delete(this);
                            file = root.createChildData(this, GMavenConstants.POM_XML);
                            vcsFileAdder.markFileForAdding(file);
                            MavenUtils.runOrApplyMavenProjectFileTemplate(project, file, myProjectId, isInteractive);
                        } catch (IOException e) {
                            showError(project, e);
                            return file;
                        }

                        updateProjectPom(project, file);
                    } finally {
                        vcsFileAdder.finish();
                    }

                    if (myAggregatorProject != null) {
                        VirtualFile aggregatorProjectFile = MavenUtils.getVFile(myAggregatorProject.getFile());
                        MavenDomProjectModel model = MavenDomUtil.getMavenDomProjectModel(project, aggregatorProjectFile);
                        if (model != null) {
                            model.getPackaging().setStringValue("pom");
                            MavenDomModule module = model.getModules().addModule();
                            module.setValue(getPsiFile(project, file));
                            unblockAndSaveDocuments(project, aggregatorProjectFile);
                        }
                    }
                    return file;
                });

        if (pom == null) return;

        if (myAggregatorProject == null) {
            //MavenProjectsManager manager = MavenProjectsManager.getInstance(project);
            //manager.addManagedFilesOrUnignore(Collections.singletonList(pom));
        }

        ExternalSystemApiUtil.getSettings(project, SYSTEM_ID).linkProject(settings);
        ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized(() -> {
            String canonicalPath = pom.getParent().getPath();
            ExternalSystemUtil.refreshProject(canonicalPath, new ImportSpecBuilder(project, SYSTEM_ID)
                    .callback(new ExternalProjectRefreshCallback() {
                        @Override
                        public void onSuccess(@NotNull ExternalSystemTaskId externalTaskId,
                                              @Nullable DataNode<ProjectData> externalProject) {
                            if (externalProject != null) {
                                ProjectDataManager.getInstance().importData(externalProject, project, false);
                            }
                        }
                    }));
        });


        // execute when current dialog is closed (e.g. Project Structure)
        MavenUtils.invokeLater(project, ModalityState.NON_MODAL, () -> {
            if (!pom.isValid()) {
                showError(project, new RuntimeException("Project is not valid"));
                return;
            }

            EditorHelper.openInEditor(getPsiFile(project, pom));
        });
    }

    private void updateProjectPom(final Project project, final VirtualFile pom) {
        if (myParentProject == null) return;

        WriteCommandAction.writeCommandAction(project).withName(myCommandName).run(() -> {
            PsiDocumentManager.getInstance(project).commitAllDocuments();

            MavenDomProjectModel model = MavenDomUtil.getMavenDomProjectModel(project, pom);
            if (model == null) return;

            MavenDomUtil.updateMavenParent(model, myParentProject);

            if (myInheritGroupId) {
                XmlElement el = model.getGroupId().getXmlElement();
                if (el != null) el.delete();
            }
            if (myInheritVersion) {
                XmlElement el = model.getVersion().getXmlElement();
                if (el != null) el.delete();
            }

            CodeStyleManager.getInstance(project).reformat(getPsiFile(project, pom));

            List<VirtualFile> pomFiles = new ArrayList<>(2);
            pomFiles.add(pom);

            if (!FileUtil.namesEqual(GMavenConstants.POM_XML, myParentProject.getFile().getName())) {
                pomFiles.add(MavenUtils.getVFile(myParentProject.getFile()));
                /*todo
                MavenProjectsManager.getInstance(project).forceUpdateProjects(Collections.singleton(myParentProject));*/
            }

            unblockAndSaveDocuments(project, pomFiles.toArray(VirtualFile.EMPTY_ARRAY));
        });
    }

    private static void unblockAndSaveDocuments(@NotNull Project project, VirtualFile @NotNull ... files) {
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        for (VirtualFile file : files) {
            Document document = fileDocumentManager.getDocument(file);
            if (document == null) continue;
            psiDocumentManager.doPostponedOperationsAndUnblockDocument(document);
            fileDocumentManager.saveDocument(document);
        }
    }

    private static PsiFile getPsiFile(Project project, VirtualFile pom) {
        return PsiManager.getInstance(project).findFile(pom);
    }

    private static void showError(Project project, Throwable e) {
        MavenUtils.showError(project, GBundle.message("notification.title.failed.to.create.maven.project"), e);
    }

    /* todo show original file for good example run procces
    @VisibleForTesting
    void copyGeneratedFiles(File workingDir, VirtualFile pom, Project project, String artifactId) {

    }*/
}
