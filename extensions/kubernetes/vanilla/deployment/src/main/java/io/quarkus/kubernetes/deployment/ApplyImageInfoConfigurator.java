
package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.config.Configurator;
import io.dekorate.kubernetes.config.ImageConfigurationFluent;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;

public class ApplyImageInfoConfigurator extends Configurator<ImageConfigurationFluent> {

    private final ContainerImageInfoBuildItem image;
    private final String defaultRegistry;

    public ApplyImageInfoConfigurator(ContainerImageInfoBuildItem image, String defaultRegistry) {
        this.image = image;
        this.defaultRegistry = defaultRegistry;
    }

    @Override
    public void visit(ImageConfigurationFluent builder) {
        System.out.println("AAAAAA!!");
        builder.withRegistry(image.getRegistry().orElse(defaultRegistry));
        builder.withGroup(image.getGroup());
        builder.withName(image.getName());
        builder.withVersion(image.getTag());
    }
}
