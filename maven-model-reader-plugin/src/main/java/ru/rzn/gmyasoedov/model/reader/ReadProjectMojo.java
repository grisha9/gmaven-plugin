package ru.rzn.gmyasoedov.model.reader;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "read", defaultPhase = LifecyclePhase.NONE, aggregator = true)
public class ReadProjectMojo extends AbstractMojo {

    @Override
    public void execute() {
        getLog().info("!!!-----------------ReadProjectMojo-----------------------!!!");
    }
}
