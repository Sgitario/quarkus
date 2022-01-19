package io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.rest.data.panache.deployment.AbstractPostMethodTest;
import io.quarkus.test.QuarkusDevModeTest;

public class PanacheRepositoryResourcePostMethodTest extends AbstractPostMethodTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Collection.class, CollectionsResource.class, CollectionsRepository.class,
                            AbstractEntity.class, AbstractItem.class, Item.class, ItemsResource.class,
                            ItemsRepository.class)
                    .addAsResource("application.properties")
                    .addAsResource("import.sql"));
}
