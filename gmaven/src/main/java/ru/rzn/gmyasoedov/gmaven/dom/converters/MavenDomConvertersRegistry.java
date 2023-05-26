package ru.rzn.gmyasoedov.gmaven.dom.converters;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.converters.values.GenericDomValueConvertersRegistry;
import ru.rzn.gmyasoedov.gmaven.dom.references.MavenPathReferenceConverter;

import java.io.File;
import java.util.Set;

@Service(Service.Level.APP)
public final class MavenDomConvertersRegistry {
  private GenericDomValueConvertersRegistry myConvertersRegistry;

  private final Set<String> mySoftConverterTypes = ContainerUtil.immutableSet(File.class.getCanonicalName());

  public static MavenDomConvertersRegistry getInstance() {
    return ApplicationManager.getApplication().getService(MavenDomConvertersRegistry.class);
  }

  public MavenDomConvertersRegistry() {
    myConvertersRegistry = new GenericDomValueConvertersRegistry();

    initConverters();
  }

  private void initConverters() {
    myConvertersRegistry.registerDefaultConverters();

    myConvertersRegistry.registerConverter(new MavenPathReferenceConverter(), File.class);
  }

  public GenericDomValueConvertersRegistry getConvertersRegistry() {
    return myConvertersRegistry;
  }

  public boolean isSoft(String type) {
    return mySoftConverterTypes.contains(type);
  }
}
