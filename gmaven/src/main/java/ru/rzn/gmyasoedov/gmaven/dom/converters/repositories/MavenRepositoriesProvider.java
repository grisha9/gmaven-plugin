package ru.rzn.gmyasoedov.gmaven.dom.converters.repositories;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.util.xmlb.XmlSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.rzn.gmyasoedov.gmaven.dom.converters.repositories.beans.RepositoriesBean;
import ru.rzn.gmyasoedov.gmaven.dom.converters.repositories.beans.RepositoryBeanInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service(Service.Level.APP)
public final class MavenRepositoriesProvider {
  public static MavenRepositoriesProvider getInstance() {
    return ApplicationManager.getApplication().getService(MavenRepositoriesProvider.class);
  }

  final Map<String, RepositoryBeanInfo> myRepositoriesMap = new HashMap<>();

  public MavenRepositoriesProvider() {
    final RepositoriesBean repositoriesBean =
      XmlSerializer.deserialize(MavenRepositoriesProvider.class.getResource("repositories.xml"), RepositoriesBean.class);

    RepositoryBeanInfo[] repositories = repositoriesBean.getRepositories();
    assert repositories != null;

    for (RepositoryBeanInfo repository : repositories) {
      registerRepository(repository.getId(), repository);
    }
  }

  public void registerRepository(@NotNull String id, RepositoryBeanInfo info) {
    myRepositoriesMap.put(id, info);
  }

  @NotNull
  public Set<String> getRepositoryIds() {
    return myRepositoriesMap.keySet();
  }

  @Nullable
  public String getRepositoryName(@Nullable String id) {
    RepositoryBeanInfo pair = myRepositoriesMap.get(id);
    return pair != null ? pair.getName() : null;
  }

  @Nullable
  public String getRepositoryUrl(@Nullable String id) {
    RepositoryBeanInfo pair = myRepositoriesMap.get(id);
    return pair != null ? pair.getUrl() : null;
  }
}
