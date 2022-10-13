package io.quarkus.hibernate.reactive.rest.data.panache.deployment.subresource;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class Item extends PanacheEntity {
    public String name;

    @ManyToOne(optional = false)
    @JsonbTransient // Avoid infinite loop when serializing
    public Collection collection;
}
