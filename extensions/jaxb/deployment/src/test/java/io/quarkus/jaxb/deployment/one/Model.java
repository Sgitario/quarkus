package io.quarkus.jaxb.deployment.one;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(namespace = "one")
public class Model {
    private Enum name1;

    public Enum getName1() {
        return name1;
    }

    public void setName1(Enum name1) {
        this.name1 = name1;
    }
}
