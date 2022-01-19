package io.quarkus.rest.data.panache.deployment.methods.hal;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;

import java.lang.annotation.Annotation;
import java.net.URI;

import javax.validation.Valid;
import javax.ws.rs.core.Response;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.rest.data.panache.deployment.utils.ResponseImplementor;
import io.quarkus.rest.data.panache.deployment.utils.UniImplementor;
import io.quarkus.rest.data.panache.runtime.resource.ResourceLinksProvider;
import io.smallrye.mutiny.Uni;

public final class AddHalMethodImplementor extends HalMethodImplementor {

    private static final String METHOD_NAME = "addHal";

    private static final String RESOURCE_METHOD_NAME = "add";

    private static final String EXCEPTION_MESSAGE = "Failed to add an entity";

    private final boolean withValidation;

    public AddHalMethodImplementor(boolean withValidation, boolean isResteasyClassic, boolean hasLinksEnabled) {
        super(isResteasyClassic, hasLinksEnabled);
        this.withValidation = withValidation;
    }

    /**
     * Generate HAL JAX-RS POST method.
     *
     * The RESTEasy Classic version exposes {@link RestDataResource#add(Object)} via HAL JAX-RS method.
     * Generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;POST
     *     &#64;Path("")
     *     &#64;Consumes({"application/json"})
     *     &#64;Produces({"application/hal+json"})
     *     public Response addHal(Entity entityToSave) {
     *         try {
     *             Entity entity = resource.add(entityToSave);
     *             HalEntityWrapper wrapper = new HalEntityWrapper(entity);
     *             String location = new ResourceLinksProvider().getSelfLink(entity);
     *             if (location != null) {
     *                 ResponseBuilder responseBuilder = Response.status(201);
     *                 responseBuilder.entity(wrapper);
     *                 responseBuilder.location(URI.create(location));
     *                 return responseBuilder.build();
     *             } else {
     *                 throw new RuntimeException("Could not extract a new entity URL");
     *             }
     *         } catch (Throwable t) {
     *             throw new RestDataPanacheException(t);
     *         }
     *     }
     * }
     * </pre>
     *
     * The RESTEasy Reactive version exposes {@link io.quarkus.rest.data.panache.ReactiveRestDataResource#add(Object)}
     * and the generated code looks more or less like this:
     *
     * <pre>
     * {@code
     *     &#64;POST
     *     &#64;Path("")
     *     &#64;Consumes({"application/json"})
     *     &#64;Produces({"application/hal+json"})
     *     public Uni<Response> addHal(Entity entityToSave) {
     *
     *         return resource.add(entityToSave).map(entity -> {
     *             HalEntityWrapper wrapper = new HalEntityWrapper(entity);
     *             String location = new ResourceLinksProvider().getSelfLink(entity);
     *             if (location != null) {
     *                 ResponseBuilder responseBuilder = Response.status(201);
     *                 responseBuilder.entity(wrapper);
     *                 responseBuilder.location(URI.create(location));
     *                 return responseBuilder.build();
     *             } else {
     *                 throw new RuntimeException("Could not extract a new entity URL");
     *             }
     *         }).onFailure().invoke(t -> throw new RestDataPanacheException(t));
     *     }
     * }
     * </pre>
     *
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            ResourceProperties resourceProperties, FieldDescriptor resourceField) {
        MethodCreator methodCreator = classCreator.getMethodCreator(METHOD_NAME,
                isResteasyClassic() ? Response.class : Uni.class,
                resourceMetadata.getEntityType());

        // Add method annotations
        addPathAnnotation(methodCreator, resourceProperties.getPath(RESOURCE_METHOD_NAME));
        addPostAnnotation(methodCreator);
        addConsumesAnnotation(methodCreator, APPLICATION_JSON);
        addProducesAnnotation(methodCreator, APPLICATION_HAL_JSON);
        // Add parameter annotations
        if (withValidation) {
            methodCreator.getParameterAnnotations(0).addAnnotation(Valid.class);
        }

        ResultHandle resource = methodCreator.readInstanceField(resourceField, methodCreator.getThis());
        ResultHandle entityToSave = methodCreator.getMethodParam(0);

        if (isResteasyClassic()) {
            TryBlock tryBlock = implementTryBlock(methodCreator, EXCEPTION_MESSAGE);
            ResultHandle entity = tryBlock.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, Object.class, Object.class),
                    resource, entityToSave);

            // Wrap and return response
            tryBlock.returnValue(ResponseImplementor.created(tryBlock, wrapHalEntity(tryBlock, entity),
                    ResponseImplementor.getEntityUrl(tryBlock, entity)));

            tryBlock.close();
        } else {
            ResultHandle arcContainer = methodCreator
                    .invokeStaticMethod(MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class));
            ResultHandle instance = methodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(ArcContainer.class, "instance", InstanceHandle.class, Class.class,
                            Annotation[].class),
                    arcContainer, methodCreator.loadClass(ResourceLinksProvider.class), methodCreator.loadNull());
            ResultHandle linksProvider = methodCreator.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class),
                    instance);

            methodCreator.invokeInterfaceMethod(
                    ofMethod(ResourceLinksProvider.class, "init", void.class), linksProvider);

            ResultHandle uniEntity = methodCreator.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), RESOURCE_METHOD_NAME, Uni.class, Object.class),
                    resource, entityToSave);

            methodCreator.returnValue(UniImplementor.map(methodCreator, uniEntity, EXCEPTION_MESSAGE,
                    (body, item) -> {
                        ResultHandle link = body.invokeInterfaceMethod(
                                ofMethod(ResourceLinksProvider.class, "getSelfLink", String.class, Object.class), linksProvider,
                                item);
                        body.ifNull(link).trueBranch().throwException(RuntimeException.class,
                                "Could not extract a new entity URL");
                        ResultHandle linkUri = body.invokeStaticMethod(ofMethod(URI.class, "create", URI.class, String.class),
                                link);
                        body.returnValue(ResponseImplementor.created(body, wrapHalEntity(body, item), linkUri));
                    }));
        }

        methodCreator.close();
    }

    @Override
    protected String getResourceMethodName() {
        return RESOURCE_METHOD_NAME;
    }
}
