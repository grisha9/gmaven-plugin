package ru.rzn.gmyasoedov.gmaven.execution;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemRunConfigurationProducer;
import org.jetbrains.annotations.NotNull;

public class GMavenRuntimeConfigurationProducer extends AbstractExternalSystemRunConfigurationProducer {
    @NotNull
    @Override
    public ConfigurationFactory getConfigurationFactory() {
        return GMavenExternalTaskConfigurationType.getInstance().getFactory();
    }
}
