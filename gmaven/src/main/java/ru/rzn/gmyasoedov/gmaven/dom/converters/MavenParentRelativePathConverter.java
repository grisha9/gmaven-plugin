/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package ru.rzn.gmyasoedov.gmaven.dom.converters;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.util.ArrayUtil;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.GMavenConstants;
import ru.rzn.gmyasoedov.gmaven.bundle.GDomBundle;
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomProjectModel;
import ru.rzn.gmyasoedov.gmaven.dom.references.MavenPathReferenceConverter;
import ru.rzn.gmyasoedov.gmaven.project.MavenProjectsManager;
import ru.rzn.gmyasoedov.gmaven.utils.MavenDomUtil;
import ru.rzn.gmyasoedov.gmaven.utils.MavenUtils;
import ru.rzn.gmyasoedov.serverapi.model.MavenId;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.rzn.gmyasoedov.gmaven.utils.MavenDomUtil.collectProjectModels;

public class MavenParentRelativePathConverter extends ResolvingConverter<PsiFile> implements CustomReferenceConverter {
  @Override
  public PsiFile fromString(@Nullable @NonNls String s, ConvertContext context) {
    if (StringUtil.isEmptyOrSpaces(s)) return null;

    VirtualFile contextFile = context.getFile().getVirtualFile();
    if (contextFile == null) return null;

    VirtualFile parent = contextFile.getParent();
    if (parent == null) {
      return null;
    }
    VirtualFile f = parent.findFileByRelativePath(s);
    if (f == null) return null;

    if (f.isDirectory()) f = f.findChild(GMavenConstants.POM_XML);
    if (f == null) return null;

    return context.getPsiManager().findFile(f);
  }

  @Override
  public String toString(@Nullable PsiFile f, ConvertContext context) {
    if (f == null) return null;
    VirtualFile currentFile = context.getFile().getOriginalFile().getVirtualFile();
    if (currentFile == null) return null;

    return MavenDomUtil.calcRelativePath(currentFile.getParent(), f.getVirtualFile());
  }

  @NotNull
  @Override
  public Collection<PsiFile> getVariants(ConvertContext context) {
    List<PsiFile> result = new ArrayList<>();
    PsiFile currentFile = context.getFile().getOriginalFile();
    for (DomFileElement<MavenDomProjectModel> each : collectProjectModels(context.getFile().getProject())) {
      PsiFile file = each.getOriginalFile();
      if (file == currentFile) continue;
      result.add(file);
    }
    return result;
  }

  @Override
  public LocalQuickFix[] getQuickFixes(ConvertContext context) {
    return ArrayUtil.append(super.getQuickFixes(context), new RelativePathFix(context));
  }

  private static class RelativePathFix implements LocalQuickFix {
    private final ConvertContext myContext;

    RelativePathFix(ConvertContext context) {
      myContext = context;
    }

    @Override
    @NotNull
    public String getName() {
      return GDomBundle.message("fix.parent.path");
    }

    @Override
    @NotNull
    public String getFamilyName() {
      return GDomBundle.message("inspection.group");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      GenericDomValue el = (GenericDomValue)myContext.getInvocationElement();
      MavenId id = MavenArtifactCoordinatesHelper.getId(myContext);

      MavenProjectsManager manager = MavenProjectsManager.getInstance(project);
      MavenProject parentFile = manager.findProject(id);
      if (parentFile != null) {
        VirtualFile currentFile = myContext.getFile().getVirtualFile();
        el.setStringValue(MavenDomUtil.calcRelativePath(currentFile.getParent(),
                MavenUtils.getVFile(parentFile.getFile())));
      }
    }
  }

  @Override
  public PsiReference @NotNull [] createReferences(final GenericDomValue genericDomValue, final PsiElement element, final ConvertContext context) {
    Project project = element.getProject();
    Condition<PsiFileSystemItem> condition = item ->
      item.isDirectory() || MavenUtils.isPomFile(project, item.getVirtualFile());
    return new MavenPathReferenceConverter(condition).createReferences(genericDomValue, element, context);
  }
}