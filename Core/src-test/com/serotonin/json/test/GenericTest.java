package com.serotonin.json.test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import com.serotonin.json.util.TypeDefinition;
import com.serotonin.json.util.TypeUtils;

public class GenericTest {
    public static void main(String[] args) throws Exception {
        Class<?> varClass = GenObject.class;
        Method method = varClass.getDeclaredMethod("getValue");

        ParameterizedType paramType = new TypeDefinition(List.class, String.class);
        ParameterizedType type = new TypeDefinition(varClass, paramType);

        //        System.out.println(TypeUtils.determineTypeArguments(varClass,
        //                (ParameterizedType) varClass.getGenericSuperclass()));

        System.out.println(TypeUtils.resolveTypeVariable(type, method.getGenericReturnType()));
    }
}
