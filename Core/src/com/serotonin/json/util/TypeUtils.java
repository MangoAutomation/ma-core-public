package com.serotonin.json.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * Utilities for determining generic type information.
 * 
 * @author Matthew Lohbihler
 */
public class TypeUtils {
    public static Class<?> getRawClass(Type type) {
        if (type instanceof Class)
            return (Class<?>) type;
        if (type instanceof ParameterizedType)
            return getRawClass(((ParameterizedType) type).getRawType());
        if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawClass(componentType), 0).getClass();
        }
        throw new RuntimeException("Unknown type: " + type);
    }

    public static Type getActualTypeArgument(Type type, int index) {
        if (type instanceof Class)
            return null;
        if (type instanceof ParameterizedType)
            return ((ParameterizedType) type).getActualTypeArguments()[index];
        throw new RuntimeException("Unknown type: " + type);
    }

    public static Type resolveTypeVariable(Type type, Type propertyType) {
        if (propertyType instanceof TypeVariable) {
            TypeVariable<?> typeVar = (TypeVariable<?>) propertyType;

            Class<?> typeClass = getRawClass(type);
            TypeVariable<?>[] typeVars = typeClass.getTypeParameters();

            int index = Utils.indexOf(typeVars, typeVar);

            return ((ParameterizedType) type).getActualTypeArguments()[index];
        }

        return propertyType;
    }
}
