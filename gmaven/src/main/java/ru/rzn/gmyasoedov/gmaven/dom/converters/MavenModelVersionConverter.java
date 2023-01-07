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

import com.intellij.util.xml.ConvertContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.bundle.GDomBundle;

import java.util.Collection;
import java.util.List;

public class MavenModelVersionConverter extends MavenConstantListConverter {
  private static final String VERSION = "4.0.0";

  @Override
  protected Collection<String> getValues(@NotNull ConvertContext context) {
    return List.of(VERSION);
  }

  @Override
  public String getErrorMessage(@Nullable String s, ConvertContext context) {
    return GDomBundle.message("inspection.message.unsupported.model.version.only.version.supported", VERSION);
  }
}
