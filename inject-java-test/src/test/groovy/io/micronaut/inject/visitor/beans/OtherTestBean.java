package io.micronaut.inject.visitor.beans;

@MarkerAnnotation
public class OtherTestBean {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
