/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

package ru.rzn.gmyasoedov.serverapi.model;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

public class MavenArtifact extends MavenId {

  static final long serialVersionUID = 6389627095309274357L;


  private final String type;
  private final String classifier;
  private final String scope;
  private final boolean optional;
  private final File file;
  private final boolean resolved;

  public MavenArtifact(String groupId,
                       String artifactId,
                       String version,
                       String type,
                       String classifier,
                       String scope,
                       boolean optional,
                       @Nullable File file,
                       boolean resolved) {
    super(groupId, artifactId, version);
    this.type = type;
    this.classifier = classifier;
    this.scope = scope;
    this.optional = optional;
    this.file = file;
    this.resolved = resolved;
  }

  public String getType() {
    return type;
  }

  public String getClassifier() {
    return classifier;
  }

  public String getScope() {
    return scope;
  }

  public boolean isOptional() {
    return optional;
  }

  public boolean isResolved() {
    return resolved;
  }

  public File getFile() {
    return file;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    MavenArtifact that = (MavenArtifact) o;

    if (optional != that.optional) return false;
    if (resolved != that.resolved) return false;
    if (!Objects.equals(type, that.type)) return false;
    if (!Objects.equals(classifier, that.classifier)) return false;
    if (!Objects.equals(scope, that.scope)) return false;
    return Objects.equals(file, that.file);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
    result = 31 * result + (scope != null ? scope.hashCode() : 0);
    result = 31 * result + (optional ? 1 : 0);
    result = 31 * result + (file != null ? file.hashCode() : 0);
    result = 31 * result + (resolved ? 1 : 0);
    return result;
  }
}
