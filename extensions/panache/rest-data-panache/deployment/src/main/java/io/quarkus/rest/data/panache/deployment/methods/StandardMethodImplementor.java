package io.quarkus.rest.data.panache.deployment.methods;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;

/**
 * A standard JAX-RS method implementor.
 */
public abstract class StandardMethodImplementor extends MethodImplementor {

    protected StandardMethodImplementor(boolean isResteasyClassic, boolean isReactivePanache) {
        super(isResteasyClassic, isReactivePanache);
    }

    /**
     * Implement exposed JAX-RS method.
     */
    public void implement(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        if (resourceProperties.isExposed(getResourceMethodName())) {
            implementInternal(classCreator, resourceMetadata, resourceProperties, resourceField);
        }
    }

    /**
     * Implement the actual JAX-RS method logic.
     */
    protected abstract void implementInternal(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField);

}
