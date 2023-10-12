package ru.rzn.gmyasoedov.serverapi.model;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

import static ru.rzn.gmyasoedov.serverapi.GServerUtils.toFilePath;

public class MavenArtifact extends MavenId {

  private static final long serialVersionUID = 6389627095309274357L;


  private final String type;
  private final String classifier;
  private final String scope;
  private final boolean optional;
  private final String filePath;
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
    this.filePath = toFilePath(file);
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

  public String getFilePath() {
    return filePath;
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
    return Objects.equals(filePath, that.filePath);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
    result = 31 * result + (scope != null ? scope.hashCode() : 0);
    result = 31 * result + (optional ? 1 : 0);
    result = 31 * result + (filePath != null ? filePath.hashCode() : 0);
    result = 31 * result + (resolved ? 1 : 0);
    return result;
  }
}
