package ru.rzn.gmyasoedov.serverapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.Serializable;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MavenArtifactNode implements Serializable {

  static final long serialVersionUID = 6389627095309274357L;

  private  String groupId;
  private  String artifactId;
  private  String version;
  private  String type;
  private  String classifier;
  private  String scope;
  private  boolean optional;
  private  File file;
  private  boolean resolved;
}
