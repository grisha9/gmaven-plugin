package ru.rzn.gmyasoedov.gmaven.dom.model;

import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;
import ru.rzn.gmyasoedov.gmaven.dom.MavenDomElement;
import ru.rzn.gmyasoedov.gmaven.dom.converters.repositories.MavenRepositoryConverter;

/**
 * @author Sergey Evdokimov
 */
public interface MavenDomMirror extends MavenDomElement {
  @NotNull
  @Convert(MavenRepositoryConverter.Url.class)
  GenericDomValue<String> getUrl();

  @NotNull
  @Required(value = false, nonEmpty = true)
  GenericDomValue<String> getId();
}
