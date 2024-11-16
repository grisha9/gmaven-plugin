package ru.rzn.gmyasoedov.gmaven.execution;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.project.Project;
import ru.rzn.gmyasoedov.gmaven.GMavenConstants;

public class GMavenRunConfiguration extends ExternalSystemRunConfiguration {

    public GMavenRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(GMavenConstants.SYSTEM_ID, project, factory, name);
    }
}
