package io.quarkus.rest.data.panache.deployment.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.rest.data.panache.deployment.utils.ResourceName;

public class ResourcePropertiesProvider {

    public static final DotName RESOURCE_PROPERTIES_ANNOTATION = DotName
            .createSimple(io.quarkus.rest.data.panache.ResourceProperties.class.getName());

    public static final String RESOURCE_PROPERTIES_SUB_RESOURCES = "subResources";

    private static final DotName METHOD_PROPERTIES_ANNOTATION = DotName.createSimple(
            io.quarkus.rest.data.panache.MethodProperties.class.getName());

    private final IndexView index;

    public ResourcePropertiesProvider(IndexView index) {
        this.index = index;
    }

    /**
     * Find resource and method properties annotations used by a given interface
     * and build {@link ResourceProperties} instance.
     */
    public ResourceProperties getForInterface(String resourceInterface) {
        DotName resourceInterfaceName = DotName.createSimple(resourceInterface);
        AnnotationInstance annotation = findResourcePropertiesAnnotation(resourceInterfaceName);
        String resourceName = ResourceName.fromClass(resourceInterface);

        return new ResourceProperties(
                isExposed(annotation),
                getPath(annotation, resourceName),
                isPaged(annotation),
                isHal(annotation),
                getHalCollectionName(annotation, resourceName),
                collectMethodProperties(resourceInterfaceName),
                collectSubResourceProperties(annotation));
    }

    private AnnotationInstance findResourcePropertiesAnnotation(DotName className) {
        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            return null;
        }
        if (classInfo.classAnnotation(RESOURCE_PROPERTIES_ANNOTATION) != null) {
            return classInfo.classAnnotation(RESOURCE_PROPERTIES_ANNOTATION);
        }
        if (classInfo.superName() != null) {
            return findResourcePropertiesAnnotation(classInfo.superName());
        }
        return null;
    }

    private List<SubResourceProperties> collectSubResourceProperties(AnnotationInstance annotation) {
        if (annotation == null) {
            return Collections.emptyList();
        }

        AnnotationValue subResourcesValue = annotation.value(RESOURCE_PROPERTIES_SUB_RESOURCES);
        if (subResourcesValue == null) {
            return Collections.emptyList();
        }

        AnnotationInstance[] subResourcesArray = subResourcesValue.asNestedArray();
        if (subResourcesArray == null || subResourcesArray.length == 0) {
            return Collections.emptyList();
        }

        List<SubResourceProperties> subResources = new ArrayList<>();
        for (AnnotationInstance subResourceAnnotation : subResourcesArray) {
            String subResourceName = subResourceAnnotation.value("of").asString();
            subResources.add(new SubResourceProperties(subResourceName,
                    getPath(subResourceAnnotation, subResourceName),
                    isPaged(subResourceAnnotation),
                    isHal(subResourceAnnotation),
                    getHalCollectionName(subResourceAnnotation, subResourceName)));
        }

        return subResources;
    }

    private Map<String, MethodProperties> collectMethodProperties(DotName className) {
        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            return Collections.emptyMap();
        }

        Map<String, MethodProperties> properties = new HashMap<>();
        for (MethodInfo method : classInfo.methods()) {
            if (!properties.containsKey(method.name()) && method.hasAnnotation(METHOD_PROPERTIES_ANNOTATION)) {
                properties.put(method.name(), getMethodProperties(method.annotation(METHOD_PROPERTIES_ANNOTATION)));
            }
        }
        if (classInfo.superName() != null) {
            properties.putAll(collectMethodProperties(classInfo.superName()));
        }

        return properties;
    }

    private MethodProperties getMethodProperties(AnnotationInstance annotation) {
        return new MethodProperties(isExposed(annotation), getPath(annotation));
    }

    private boolean isHal(AnnotationInstance annotation) {
        return annotation != null
                && annotation.value("hal") != null
                && annotation.value("hal").asBoolean();
    }

    private boolean isPaged(AnnotationInstance annotation) {
        return annotation == null
                || annotation.value("paged") == null
                || annotation.value("paged").asBoolean();
    }

    private boolean isExposed(AnnotationInstance annotation) {
        return annotation == null
                || annotation.value("exposed") == null
                || annotation.value("exposed").asBoolean();
    }

    private String getPath(AnnotationInstance annotation) {
        if (annotation != null && annotation.value("path") != null) {
            return annotation.value("path").asString();
        }
        return "";
    }

    private String getPath(AnnotationInstance annotation, String defaultValue) {
        if (annotation != null && annotation.value("path") != null) {
            return annotation.value("path").asString();
        }

        return defaultValue;
    }

    private String getHalCollectionName(AnnotationInstance annotation, String defaultValue) {
        if (annotation != null && annotation.value("halCollectionName") != null) {
            return annotation.value("halCollectionName").asString();
        }

        return defaultValue;
    }
}
