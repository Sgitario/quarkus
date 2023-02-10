package io.quarkus.jaxb.deployment.two;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType
public class Model {
    private Enum name2;

    public Enum getName2() {
        return name2;
    }

    public void setName2(Enum name2) {
        this.name2 = name2;
    }
}
