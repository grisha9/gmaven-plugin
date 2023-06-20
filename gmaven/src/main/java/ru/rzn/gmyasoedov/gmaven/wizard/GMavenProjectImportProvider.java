package ru.rzn.gmyasoedov.gmaven.wizard;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectImportBuilder;
import com.intellij.projectImport.ProjectImportProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.bundle.GBundle;
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

public final class GMavenProjectImportProvider extends ProjectImportProvider {
    @Override
    protected ProjectImportBuilder<MavenProject> doGetBuilder() {
        return null;
    }

    @Override
    public boolean canImport(@NotNull VirtualFile fileOrDirectory, @Nullable Project project) {
        if (super.canImport(fileOrDirectory, project)) return true;

        if (!fileOrDirectory.isDirectory()) {
            return MavenUtils.isPomFileIgnoringName(project, fileOrDirectory);
        }

        return false;
    }

    @Override
    protected boolean canImportFromFile(VirtualFile file) {
        return MavenUtils.isPomFileName(file.getName());
    }

    @NotNull
    @Override
    public String getFileSample() {
        return GBundle.message("maven.project.file.pom.xml");
    }
}