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
package ru.rzn.gmyasoedov.gmaven.dom.references;

import com.intellij.codeInspection.XmlSuppressionProvider;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.dom.MavenPropertyResolver;
import ru.rzn.gmyasoedov.gmaven.dom.model.MavenDomProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class MavenPropertyPsiReferenceProvider extends PsiReferenceProvider {
  public static final boolean SOFT_DEFAULT = false;
  public static final String UNRESOLVED_MAVEN_PROPERTY_QUICKFIX_ID = "UnresolvedMavenProperty";

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    return getReferences(element, SOFT_DEFAULT);
  }

  private static boolean isElementCanContainReference(PsiElement element) {
    /*if (element instanceof XmlTag) {
      for (MavenPluginParamInfo.ParamInfo info : MavenPluginParamInfo.getParamInfoList((XmlTag)element)) {
        if (Boolean.TRUE.equals(info.getParam().disableReferences)) {
          return false;
        }
      }
    }*/

    return true;
  }

  public static PsiReference[] getReferences(PsiElement element, boolean isSoft) {
    TextRange textRange = ElementManipulators.getValueTextRange(element);
    if (textRange.isEmpty()) return PsiReference.EMPTY_ARRAY;

    String text = element.getText();

    if (StringUtil.isEmptyOrSpaces(text)) return PsiReference.EMPTY_ARRAY;

    if (!isElementCanContainReference(element)) return PsiReference.EMPTY_ARRAY;

    if (XmlSuppressionProvider.isSuppressed(element, UNRESOLVED_MAVEN_PROPERTY_QUICKFIX_ID)) return PsiReference.EMPTY_ARRAY;

    XmlTag propertiesTag = null;
    List<PsiReference> result = null;

    Matcher matcher = MavenPropertyResolver.PATTERN.matcher(textRange.substring(text));
    while (matcher.find()) {
      String propertyName = matcher.group(1);
      int from;
      if (propertyName == null) {
        propertyName = matcher.group(2);
        from = matcher.start(2);
      }
      else {
        from = matcher.start(1);
      }

      TextRange range = TextRange.from(textRange.getStartOffset() + from, propertyName.length());

      if (result == null) {
        result = new ArrayList<>();

        propertiesTag = findPropertiesParentTag(element);
        if (propertiesTag == null) {
          return PsiReference.EMPTY_ARRAY;
        }
      }

      result.add(new MavenContextlessPropertyReference(propertiesTag, element, range, true));
    }

    return result == null ? PsiReference.EMPTY_ARRAY : result.toArray(PsiReference.EMPTY_ARRAY);
  }

  private static XmlTag findPropertiesParentTag(@NotNull PsiElement element) {
    DomElement domElement = DomUtil.getDomElement(element);
    return domElement instanceof MavenDomProperties ? domElement.getXmlTag() : null;
  }
}

