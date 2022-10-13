package io.quarkus.rest.data.panache.deployment.utils;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.gizmo.MethodDescriptor;

public class EntityClassHelper {

    private static final DotName JAVAX_PERSISTENCE_ID = DotName.createSimple("javax.persistence.Id");
    private static final String PERSISTENT_FIELD_WRITER_PREFIX = "$$_hibernate_write_";

    private final IndexView index;

    public EntityClassHelper(IndexView index) {
        this.index = index;
    }

    public String getFieldTypeByName(String className, String fieldName) {
        ClassInfo currentClassInfo = index.getClassByName(className);
        while (currentClassInfo != null) {
            for (FieldInfo field : currentClassInfo.fields()) {
                if (field.name().equals(fieldName)) {
                    return getEffectiveType(field.type()).toString();
                }
            }

            if (currentClassInfo.superName() != null) {
                currentClassInfo = index.getClassByName(currentClassInfo.superName());
            } else {
                currentClassInfo = null;
            }
        }

        return null;
    }

    public Type getEffectiveType(Type type) {
        if (type.name().equals(ResteasyReactiveDotNames.SET) ||
                type.name().equals(ResteasyReactiveDotNames.COLLECTION) ||
                type.name().equals(ResteasyReactiveDotNames.LIST)) {
            return type.asParameterizedType().arguments().get(0);
        } else if (type.name().equals(ResteasyReactiveDotNames.MAP)) {
            return type.asParameterizedType().arguments().get(1);
        }

        return type;
    }

    public FieldInfo getIdField(String className) {
        return getIdField(index.getClassByName(DotName.createSimple(className)));
    }

    private FieldInfo getIdField(ClassInfo classInfo) {
        ClassInfo tmpClassInfo = classInfo;
        while (tmpClassInfo != null) {
            for (FieldInfo field : tmpClassInfo.fields()) {
                if (field.hasAnnotation(JAVAX_PERSISTENCE_ID)) {
                    return field;
                }
            }
            if (tmpClassInfo.superName() != null) {
                tmpClassInfo = index.getClassByName(tmpClassInfo.superName());
            } else {
                tmpClassInfo = null;
            }
        }
        throw new IllegalArgumentException("Couldn't find id field of " + classInfo);
    }

    public MethodDescriptor getSetter(String className, FieldInfo field) {
        return getSetter(index.getClassByName(DotName.createSimple(className)), field);
    }

    private MethodDescriptor getSetter(ClassInfo entityClass, FieldInfo field) {
        MethodDescriptor setter = getMethod(entityClass, JavaBeanUtil.getSetterName(field.name()), field.type());
        if (setter != null) {
            return setter;
        }
        return MethodDescriptor.ofMethod(entityClass.toString(),
                PERSISTENT_FIELD_WRITER_PREFIX + field.name(), void.class, field.type().name().toString());
    }

    private MethodDescriptor getMethod(ClassInfo entityClass, String name, Type... parameters) {
        if (entityClass == null) {
            return null;
        }
        MethodInfo methodInfo = entityClass.method(name, parameters);
        if (methodInfo != null) {
            return MethodDescriptor.of(methodInfo);
        } else if (entityClass.superName() != null) {
            return getMethod(index.getClassByName(entityClass.superName()), name, parameters);
        }
        return null;
    }
}
