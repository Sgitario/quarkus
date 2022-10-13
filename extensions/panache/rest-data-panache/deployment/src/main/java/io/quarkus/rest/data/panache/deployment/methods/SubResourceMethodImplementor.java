package io.quarkus.rest.data.panache.deployment.methods;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.SubResourceProperties;
import io.quarkus.rest.data.panache.deployment.utils.EntityClassHelper;

/**
 * A standard JAX-RS method implementor.
 */
public abstract class SubResourceMethodImplementor extends MethodImplementor {

    private final EntityClassHelper entityClassHelper;

    protected SubResourceMethodImplementor(EntityClassHelper entityClassHelper, boolean isResteasyClassic,
            boolean isReactivePanache) {
        super(isResteasyClassic, isReactivePanache);

        this.entityClassHelper = entityClassHelper;
    }

    public void implement(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            SubResourceProperties subResourceProperties, FieldDescriptor resourceField) {
        implementInternal(classCreator, resourceMetadata, subResourceProperties, resourceField);
    }

    protected abstract void implementInternal(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            SubResourceProperties subResourceProperties, FieldDescriptor resourceField);

    protected String getIdTypeOfSubResource(ResourceMetadata resourceMetadata, String subResourceName) {
        String fieldInResource = entityClassHelper.getFieldTypeByName(resourceMetadata.getEntityType(), subResourceName);
        return entityClassHelper.getIdField(fieldInResource).type().toString();
    }

}
