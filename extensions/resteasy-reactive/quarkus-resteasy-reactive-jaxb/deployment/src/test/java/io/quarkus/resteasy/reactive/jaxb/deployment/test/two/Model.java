package io.quarkus.resteasy.reactive.jaxb.deployment.test.two;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Model {
    private String name2;

    public String getName2() {
        return name2;
    }

    public void setName2(String name2) {
        this.name2 = name2;
    }
}
