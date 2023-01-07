/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package ru.rzn.gmyasoedov.gmaven.dom.references;

import com.intellij.lang.properties.references.PropertyReferenceBase;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.utils.MavenDomUtil;
import ru.rzn.gmyasoedov.serverapi.model.MavenProject;

import java.util.List;
import java.util.regex.Pattern;

public class MavenFilteredPropertyPsiReferenceProvider extends PsiReferenceProvider {

  private static final Key<Pattern> KEY = Key.create("MavenFilteredPropertyPsiReferenceProvider:delimitersKey");


  @Override
  public boolean acceptsTarget(@NotNull PsiElement target) {
    return PropertyReferenceBase.isPropertyPsi(target) || target instanceof XmlTag;
  }
  
  private static void appendDelimiter(StringBuilder pattern, String prefix, String suffix) {
    if (pattern.length() > 0) {
      pattern.append('|');
    }
    pattern.append(Pattern.quote(prefix)).append("(.+?)").append(Pattern.quote(suffix));
  }

  private static boolean shouldAddReference(@NotNull PsiElement element) {
    if (element.getFirstChild() == element.getLastChild()) {
      return true; // Add to all leaf elements
    }

    if (element instanceof XmlAttribute) {
      return true;
    }

    return false; // Don't add references to all element to avoid performance problem.
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    if (!shouldAddReference(element)) {
      // Add reference to element with one child or leaf element only to avoid performance problem.
      return PsiReference.EMPTY_ARRAY;
    }

    if (!MavenDomUtil.isFilteredResourceFile(element)) return PsiReference.EMPTY_ARRAY;

    String text = element.getText();
    if (StringUtil.isEmptyOrSpaces(text)) return PsiReference.EMPTY_ARRAY;

    MavenProject mavenProject = MavenDomUtil.findContainingProject(element);
    if (mavenProject == null) return PsiReference.EMPTY_ARRAY;

    List<PsiReference> res = null;

    return res == null ? PsiReference.EMPTY_ARRAY : res.toArray(PsiReference.EMPTY_ARRAY);
  }
}
