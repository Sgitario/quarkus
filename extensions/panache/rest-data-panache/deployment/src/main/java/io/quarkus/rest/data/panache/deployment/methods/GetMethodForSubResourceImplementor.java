package io.quarkus.rest.data.panache.deployment.methods;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator.ofType;

import java.util.List;

import javax.ws.rs.core.Response;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.properties.SubResourceProperties;
import io.quarkus.rest.data.panache.deployment.utils.EntityClassHelper;
import io.quarkus.rest.data.panache.deployment.utils.SignatureMethodCreator;
import io.quarkus.rest.data.panache.deployment.utils.UniImplementor;
import io.smallrye.mutiny.Uni;

public final class GetMethodForSubResourceImplementor extends SubResourceMethodImplementor {

    private static final String RESOURCE_METHOD_NAME = "get";

    private static final String EXCEPTION_MESSAGE = "Failed to get an entity";

    public GetMethodForSubResourceImplementor(EntityClassHelper entityClassHelper, boolean isResteasyClassic,
            boolean isReactivePanache) {
        super(entityClassHelper, isResteasyClassic, isReactivePanache);
    }

    /**
     * Generate JAX-RS GET method.
     *
     * The RESTEasy Classic version looks more or less like this:
     *
     * <pre>
     * {@code
     * &#64;GET
     * &#64;Produces({ "application/json" })
     * &#64;Path("{id}/subentity/{sid}")
     * &#64;LinkResource(rel = "subentity", entityClassName = "com.example.Entity")
     * public Response getsubentity(@PathParam("id") ID id, @PathParam("sid") ID subResourceId) {
     *     try {
     *         Object entity = restDataResource.getsubentity(id, subResourceId);
     *         if (entity != null) {
     *             return entity;
     *         } else {
     *             return Response.status(404).build();
     *         }
     *     } catch (Throwable t) {
     *         throw new RestDataPanacheException(t);
     *     }
     * }
     * }
     * </pre>
     *
     * The RESTEasy Reactive version looks more or less like this:
     *
     * <pre>
     * {@code
     * &#64;GET
     * &#64;Produces({ "application/json" })
     * &#64;Path("{id}")
     * &#64;LinkResource(rel = "subentity", entityClassName = "com.example.Entity")
     * public Uni<Response> getsubentity(@PathParam("id") ID id, @PathParam("sid") ID subResourceId) {
     *     try {
     *         return restDataResource.getsubentity(id, subResourceId)
     *                 .map(entity -> entity == null ? Response.status(404).build() : Response.ok(entity).build());
     *     } catch (Throwable t) {
     *         throw new RestDataPanacheException(t);
     *     }
     * }
     * }
     * </pre>
     */
    @Override
    protected void implementInternal(ClassCreator classCreator, ResourceMetadata resourceMetadata,
            SubResourceProperties subResourceProperties, FieldDescriptor resourceField) {
        String methodName = RESOURCE_METHOD_NAME + subResourceProperties.getSubResourceName();
        String resourceMethodName = "list" + subResourceProperties.getSubResourceName();
        MethodCreator methodCreator = SignatureMethodCreator.getMethodCreator(methodName, classCreator,
                isNotReactivePanache() ? ofType(Response.class) : ofType(Uni.class, resourceMetadata.getEntityType()),
                resourceMetadata.getIdType(),
                getIdTypeOfSubResource(resourceMetadata, subResourceProperties.getSubResourceName()));

        // Add method annotations
        addPathAnnotation(methodCreator, appendToPath("{id}", subResourceProperties.getSubResourceName(), "{sid}"));
        addGetAnnotation(methodCreator);
        addProducesJsonAnnotation(methodCreator, subResourceProperties.isHal());

        addPathParamAnnotation(methodCreator.getParameterAnnotations(0), "id");
        addPathParamAnnotation(methodCreator.getParameterAnnotations(1), "sid");
        addLinksAnnotation(methodCreator, resourceMetadata.getEntityType(), subResourceProperties.getSubResourceName());

        ResultHandle resource = methodCreator.readInstanceField(resourceField, methodCreator.getThis());
        ResultHandle id = methodCreator.getMethodParam(0);
        ResultHandle subResourceId = methodCreator.getMethodParam(1);
        if (isNotReactivePanache()) {
            TryBlock tryBlock = implementTryBlock(methodCreator, EXCEPTION_MESSAGE);
            ResultHandle listOfEntities = tryBlock.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), resourceMethodName, List.class, Object.class, Object.class),
                    resource, id, subResourceId);
            BranchResult isEmptyBranch = tryBlock
                    .ifTrue(tryBlock.invokeInterfaceMethod(ofMethod(List.class, "isEmpty", boolean.class),
                            listOfEntities));
            isEmptyBranch.trueBranch().returnValue(responseImplementor.notFound(isEmptyBranch.trueBranch()));
            isEmptyBranch.falseBranch().returnValue(responseImplementor.ok(isEmptyBranch.falseBranch(),
                    isEmptyBranch.falseBranch().invokeInterfaceMethod(ofMethod(List.class, "get", Object.class, int.class),
                            listOfEntities, isEmptyBranch.falseBranch().load(0))));

            tryBlock.close();
        } else {
            ResultHandle uniList = methodCreator.invokeVirtualMethod(
                    ofMethod(resourceMetadata.getResourceClass(), resourceMethodName, Uni.class, Object.class, Object.class),
                    resource, id, subResourceId);

            methodCreator.returnValue(UniImplementor.map(methodCreator, uniList, EXCEPTION_MESSAGE,
                    (body, list) -> {
                        BranchResult isEmptyBranch = body.ifTrue(body.invokeInterfaceMethod(
                                ofMethod(List.class, "isEmpty", boolean.class), list));
                        isEmptyBranch.trueBranch().returnValue(responseImplementor.notFound(isEmptyBranch.trueBranch()));
                        isEmptyBranch.falseBranch().returnValue(responseImplementor.ok(isEmptyBranch.falseBranch(),
                                isEmptyBranch.falseBranch().invokeInterfaceMethod(
                                        ofMethod(List.class, "get", Object.class, int.class),
                                        list, isEmptyBranch.falseBranch().load(0))));
                    }));
        }

        methodCreator.close();
    }

    @Override
    protected String getResourceMethodName() {
        return RESOURCE_METHOD_NAME;
    }
}
