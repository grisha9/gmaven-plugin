package ru.rzn.gmyasoedov.gmaven.chooser;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils;

import java.util.stream.Stream;

public class MavenPomFileChooserDescriptor extends FileChooserDescriptor {


  public MavenPomFileChooserDescriptor() {
    super(false, true, false, false, false, false);
  }

  @Override
  public boolean isFileSelectable(@Nullable VirtualFile file) {
    if (!super.isFileSelectable(file)) return false;
    return Stream.of(file.getChildren()).anyMatch(MavenUtils::isPomFile);
  }
}
