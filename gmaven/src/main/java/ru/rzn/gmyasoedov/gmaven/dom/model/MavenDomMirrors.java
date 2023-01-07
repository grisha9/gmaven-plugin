package ru.rzn.gmyasoedov.gmaven.dom.model;

import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.dom.MavenDomElement;

import java.util.List;

/**
 * @author Sergey Evdokimov
 */
public interface MavenDomMirrors extends MavenDomElement {
  @NotNull
  List<MavenDomMirror> getMirrors();
}
