package io.quarkus.rest.data.panache.deployment.properties;

public class SubResourceProperties {

    private final String subResourceName;

    private final String path;

    private final boolean paged;

    private final boolean hal;

    private final String halCollectionName;

    public SubResourceProperties(String subResourceName, String path, boolean paged, boolean hal, String halCollectionName) {
        this.subResourceName = subResourceName;
        this.path = path;
        this.paged = paged;
        this.hal = hal;
        this.halCollectionName = halCollectionName;
    }

    public String getSubResourceName() {
        return subResourceName;
    }

    public String getPath() {
        return path;
    }

    public boolean isPaged() {
        return paged;
    }

    public boolean isHal() {
        return hal;
    }

    public String getHalCollectionName() {
        return halCollectionName;
    }
}
