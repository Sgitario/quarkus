package io.quarkus.hibernate.orm.rest.data.panache.deployment.subresource;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.rest.data.panache.SubResourceProperties;

@ResourceProperties(hal = true, paged = false, halCollectionName = "item-collections", subResources = {
        @SubResourceProperties(of = "items") })
public interface CollectionsResource extends PanacheEntityResource<Collection, String> {
}
