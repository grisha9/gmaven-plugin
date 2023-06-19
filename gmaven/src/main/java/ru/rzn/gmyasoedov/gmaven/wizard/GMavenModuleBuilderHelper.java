package ru.rzn.gmyasoedov.gmaven.wizard;

import com.intellij.openapi.GitSilentFileAdder;
import com.intellij.openapi.GitSilentFileAdderProvider;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.xml.XmlElement;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.GMavenConstants;
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle;
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomModule;
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomProjectModel;
import ru.rzn.gmyasoedov.gmaven.utils.MavenDomUtil;
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils;
import ru.rzn.gmyasoedov.serverapi.model.MavenId;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GMavenModuleBuilderHelper {
    private final MavenId myProjectId;

    private final MavenProject myParentProject;

    private final boolean myInheritGroupId;
    private final boolean myInheritVersion;
    @NotNull
    private final VirtualFile buildScriptFile;

    public GMavenModuleBuilderHelper(@NotNull MavenId projectId,
                                     MavenProject parentProject,
                                     boolean inheritGroupId,
                                     boolean inheritVersion,
                                     @NotNull VirtualFile buildScriptFile) {
        myProjectId = projectId;
        myParentProject = parentProject;
        myInheritGroupId = inheritGroupId;
        myInheritVersion = inheritVersion;
        this.buildScriptFile = buildScriptFile;
    }

    @NotNull
    public void setupBuildScript(final Project project, final boolean isInteractive) {
        PsiFile[] psiFiles = myParentProject != null
                ? new PsiFile[]{getPsiFile(project, MavenUtils.getVFile(myParentProject.getFile()))}
                : PsiFile.EMPTY_ARRAY;
        WriteCommandAction
                .writeCommandAction(project, psiFiles)
                .withName(GBundle.message("command.name.create.new.maven.module"))
                .compute(() -> {
                    setupBuildPomFile(project, buildScriptFile, isInteractive);
                    return null;
                });
    }

    private void setupBuildPomFile(Project project, VirtualFile buildScriptFile, boolean isInteractive) {
        GitSilentFileAdder vcsFileAdder = GitSilentFileAdderProvider.create(project);
        try {
            try {
                vcsFileAdder.markFileForAdding(buildScriptFile);
                MavenUtils.runOrApplyMavenProjectFileTemplate(project, buildScriptFile, myProjectId, isInteractive);
            } catch (IOException e) {
                showError(project, e);
            }

            updateProjectPom(project, buildScriptFile);
        } finally {
            vcsFileAdder.finish();
        }

        if (myParentProject != null) {
            VirtualFile aggregatorProjectFile = MavenUtils.getVFile(myParentProject.getFile());
            MavenDomProjectModel model = MavenDomUtil.getMavenDomProjectModel(project, aggregatorProjectFile);
            if (model != null) {
                model.getPackaging().setStringValue("pom");
                MavenDomModule module = model.getModules().addModule();
                module.setValue(getPsiFile(project, buildScriptFile));
                unblockAndSaveDocuments(project, aggregatorProjectFile);
            }
        }
    }

    private void updateProjectPom(final Project project, final VirtualFile pom) {
        if (myParentProject == null) return;

        WriteCommandAction
                .writeCommandAction(project)
                .withName(GBundle.message("command.name.create.new.maven.module"))
                .run(() -> applySettingsToModulePomFile(project, pom));
    }

    private void applySettingsToModulePomFile(Project project, VirtualFile pom) {
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
        }

        unblockAndSaveDocuments(project, pomFiles.toArray(VirtualFile.EMPTY_ARRAY));
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
