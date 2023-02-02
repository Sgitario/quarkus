
package io.quarkus.kubernetes.deployment;

import io.dekorate.config.ConfigurationSupplier;
import io.dekorate.kubernetes.config.ImageConfiguration;
import io.dekorate.kubernetes.config.ImageConfigurationBuilder;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;

/**
 * Workaround for https://github.com/dekorateio/dekorate/issues/1147: Dekorate only allows providing the image info via
 * suppliers, not configurators.
 */
public class ApplyImageInfoConfigurationSupplier extends ConfigurationSupplier<ImageConfiguration> {

    public ApplyImageInfoConfigurationSupplier(ContainerImageInfoBuildItem image, String defaultRegistry) {
        super(create(image, defaultRegistry));
    }

    private static ImageConfigurationBuilder create(ContainerImageInfoBuildItem image, String defaultRegistry) {
        ImageConfigurationBuilder builder = new ImageConfigurationBuilder();
        builder.withRegistry(image.getRegistry().orElse(defaultRegistry));
        builder.withEnabled(true);
        builder.withGroup(image.getGroup());
        builder.withName(image.getName());
        builder.withVersion(image.getTag());
        return builder;
    }

    @Override
    public boolean isExplicit() {
        return true;
    }
}
