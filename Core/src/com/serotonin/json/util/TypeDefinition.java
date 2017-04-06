package com.serotonin.json.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A convenience class for specifying generic type information to a JsonReader.
 * 
 * @author Matthew Lohbihler
 */
public class TypeDefinition implements ParameterizedType {
    private final Type rawType;
    private final Type[] actualTypeArguments;

    public TypeDefinition(Type rawType, Type... actualTypeArguments) {
        this.rawType = rawType;
        this.actualTypeArguments = actualTypeArguments;
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return actualTypeArguments;
    }

    @Override
    public Type getOwnerType() {
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(rawType);
        if (actualTypeArguments != null && actualTypeArguments.length > 0) {
            sb.append('<');

            for (int i = 0; i < actualTypeArguments.length; i++) {
                if (i > 0)
                    sb.append(',');
                sb.append(actualTypeArguments[i]);
            }

            sb.append('>');
        }

        return sb.toString();
    }
}
