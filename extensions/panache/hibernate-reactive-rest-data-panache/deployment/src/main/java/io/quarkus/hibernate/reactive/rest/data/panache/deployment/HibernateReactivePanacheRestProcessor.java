package io.quarkus.hibernate.reactive.rest.data.panache.deployment;

import static io.quarkus.deployment.Feature.HIBERNATE_REACTIVE_REST_DATA_PANACHE;
import static io.quarkus.rest.data.panache.deployment.properties.ResourcePropertiesProvider.RESOURCE_PROPERTIES_ANNOTATION;
import static io.quarkus.rest.data.panache.deployment.properties.ResourcePropertiesProvider.RESOURCE_PROPERTIES_SUB_RESOURCES;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.hibernate.reactive.rest.data.panache.PanacheEntityResource;
import io.quarkus.hibernate.reactive.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.hibernate.reactive.rest.data.panache.runtime.RestDataPanacheExceptionMapper;
import io.quarkus.rest.data.panache.deployment.ResourceMetadata;
import io.quarkus.rest.data.panache.deployment.RestDataResourceBuildItem;
import io.quarkus.rest.data.panache.deployment.utils.EntityClassHelper;
import io.quarkus.resteasy.reactive.spi.CustomExceptionMapperBuildItem;

class HibernateReactivePanacheRestProcessor {

    private static final DotName PANACHE_ENTITY_RESOURCE_INTERFACE = DotName
            .createSimple(PanacheEntityResource.class.getName());

    private static final DotName PANACHE_REPOSITORY_RESOURCE_INTERFACE = DotName
            .createSimple(PanacheRepositoryResource.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(HIBERNATE_REACTIVE_REST_DATA_PANACHE);
    }

    @BuildStep
    void registerRestDataPanacheExceptionMapper(BuildProducer<CustomExceptionMapperBuildItem> customExceptionMappers) {
        customExceptionMappers
                .produce(new CustomExceptionMapperBuildItem(RestDataPanacheExceptionMapper.class.getName()));
    }

    /**
     * Find Panache entity resources and generate their implementations.
     */
    @BuildStep
    void findEntityResources(CombinedIndexBuildItem index,
            BuildProducer<GeneratedBeanBuildItem> implementationsProducer,
            BuildProducer<RestDataResourceBuildItem> restDataResourceProducer) {
        ResourceImplementor resourceImplementor = new ResourceImplementor(new EntityClassHelper(index.getIndex()));
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(implementationsProducer);

        for (ClassInfo classInfo : index.getIndex().getKnownDirectImplementors(PANACHE_ENTITY_RESOURCE_INTERFACE)) {
            validateResource(index.getIndex(), classInfo);

            List<Type> generics = getGenericTypes(classInfo);
            String resourceInterface = classInfo.name().toString();
            String entityType = generics.get(0).name().toString();
            String idType = generics.get(1).name().toString();
            List<FieldInfo> subResources = getSubResources(index.getIndex(),
                    classInfo.annotation(RESOURCE_PROPERTIES_ANNOTATION), entityType);

            DataAccessImplementor dataAccessImplementor = new EntityDataAccessImplementor(entityType);
            String resourceClass = resourceImplementor.implement(
                    classOutput, dataAccessImplementor, resourceInterface, entityType, subResources);

            restDataResourceProducer.produce(new RestDataResourceBuildItem(
                    new ResourceMetadata(resourceClass, resourceInterface, entityType, idType)));
        }
    }

    /**
     * Find Panache repository resources and generate their implementations.
     */
    @BuildStep
    void findRepositoryResources(CombinedIndexBuildItem index,
            BuildProducer<GeneratedBeanBuildItem> implementationsProducer,
            BuildProducer<RestDataResourceBuildItem> restDataResourceProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeansProducer) {
        ResourceImplementor resourceImplementor = new ResourceImplementor(new EntityClassHelper(index.getIndex()));
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(implementationsProducer);

        for (ClassInfo classInfo : index.getIndex().getKnownDirectImplementors(PANACHE_REPOSITORY_RESOURCE_INTERFACE)) {
            validateResource(index.getIndex(), classInfo);

            List<Type> generics = getGenericTypes(classInfo);
            String resourceInterface = classInfo.name().toString();
            String repositoryClassName = generics.get(0).name().toString();
            String entityType = generics.get(1).name().toString();
            String idType = generics.get(2).name().toString();
            List<FieldInfo> subResources = getSubResources(index.getIndex(),
                    classInfo.annotation(RESOURCE_PROPERTIES_ANNOTATION), entityType);

            DataAccessImplementor dataAccessImplementor = new RepositoryDataAccessImplementor(repositoryClassName);
            String resourceClass = resourceImplementor.implement(
                    classOutput, dataAccessImplementor, resourceInterface, entityType, subResources);
            // Make sure that repository bean is not removed and will be injected to the generated resource
            unremovableBeansProducer.produce(new UnremovableBeanBuildItem(
                    new UnremovableBeanBuildItem.BeanClassNameExclusion(repositoryClassName)));

            restDataResourceProducer.produce(new RestDataResourceBuildItem(
                    new ResourceMetadata(resourceClass, resourceInterface, entityType, idType)));
        }
    }

    private void validateResource(IndexView index, ClassInfo classInfo) {
        if (!Modifier.isInterface(classInfo.flags())) {
            throw new RuntimeException(classInfo.name() + " has to be an interface");
        }

        if (classInfo.interfaceNames().size() > 1) {
            throw new RuntimeException(classInfo.name() + " should only extend REST Data Panache interface");
        }

        if (!index.getKnownDirectImplementors(classInfo.name()).isEmpty()) {
            throw new RuntimeException(classInfo.name() + " should not be extended or implemented");
        }
    }

    private List<FieldInfo> getSubResources(IndexView index, AnnotationInstance resourceProperties, String entityType) {
        if (resourceProperties == null) {
            return Collections.emptyList();
        }

        AnnotationValue subResourcesValue = resourceProperties.value(RESOURCE_PROPERTIES_SUB_RESOURCES);
        if (subResourcesValue == null) {
            return Collections.emptyList();
        }

        AnnotationInstance[] subResourcesArray = subResourcesValue.asNestedArray();
        if (subResourcesArray == null || subResourcesArray.length == 0) {
            return Collections.emptyList();
        }

        Map<String, FieldInfo> allFields = new HashMap<>();
        ClassInfo currentClassInfo = index.getClassByName(entityType);
        while (currentClassInfo != null) {
            for (FieldInfo field : currentClassInfo.fields()) {
                allFields.putIfAbsent(field.name(), field);
            }

            if (currentClassInfo.superName() != null) {
                currentClassInfo = index.getClassByName(currentClassInfo.superName());
            } else {
                currentClassInfo = null;
            }
        }

        List<FieldInfo> subResources = new ArrayList<>();
        for (AnnotationInstance subResourceAnnotation : subResourcesArray) {
            String subResourceName = subResourceAnnotation.value("of").asString();
            FieldInfo field = allFields.get(subResourceName);
            if (field == null) {
                throw new IllegalStateException("Could not find field '" + subResourceName + "' in '" + entityType + "' to "
                        + "expose this sub resource");
            }

            subResources.add(field);
        }

        return subResources;
    }

    private List<Type> getGenericTypes(ClassInfo classInfo) {
        return classInfo.interfaceTypes()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException(classInfo.toString() + " does not have generic types"))
                .asParameterizedType()
                .arguments();
    }
}
