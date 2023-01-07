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

// Generated on Mon Mar 17 18:02:09 MSK 2008
// DTD/Schema  :    http://maven.apache.org/POM/4.0.0

package ru.rzn.gmyasoedov.gmaven.dom.model;

import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import ru.rzn.gmyasoedov.gmaven.dom.MavenDomElement;

/**
 * http://maven.apache.org/POM/4.0.0:Extension interface.
 * <pre>
 * <h3>Type http://maven.apache.org/POM/4.0.0:Extension documentation</h3>
 * 4.0.0
 * </pre>
 */
public interface MavenDomExtension extends MavenDomElement, MavenDomArtifactCoordinates {
  @Override
  @Required(value = false, nonEmpty = true)
  GenericDomValue<String> getGroupId();

  @Override
  @Required(value = false, nonEmpty = true)
  GenericDomValue<String> getVersion();
}
