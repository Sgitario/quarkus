package io.quarkus.jaxrs.client.reactive.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item configures all the REST Clients to remove the trailing slash from the paths.
 */
public final class RestClientRemoveTrailingSlashBuildItem extends MultiBuildItem {
}
