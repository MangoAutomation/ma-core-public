/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.spi.JsonEntity;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.ObjectTypeWriter;
import com.serotonin.json.util.SerializableProperty;
import com.serotonin.json.util.Utils;
import com.serotonin.m2m2.Common;

/**
 * Code Lifted from Serotonin JSON JsonContext
 *
 * Utility to allow collecting differences in annotated properties from an Object
 *
 * @author Terry Packer
 *
 */
public class JsonSerializableUtility {

    public Map<String, Object> findValues(Object o) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException{
        Map<String,Object> allProperties = new HashMap<String,Object>();

        //First check the annotated properties
        List<SerializableProperty> properties = findProperties(o.getClass());
        for(SerializableProperty property : properties){
            allProperties.put(property.getName(), property.getReadMethod().invoke(o));
        }

        //Second Check the JsonSerialization
        JsonMapEntryWriter writer = new JsonMapEntryWriter();
        if(o instanceof JsonSerializable){
            ((JsonSerializable)o).jsonWrite(writer);
        }

        //Compare the 2 maps and if different add the toValues
        Iterator<String> it = writer.keySet().iterator();
        while(it.hasNext()){
            String name = it.next();
            allProperties.put(name, writer.get(name));
        }

        return allProperties;
    }

    /**
     * Get all changes between to Objects based on JsonSerialization properties
     */
    public Map<String,Object> findChanges(Object from, Object to) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException{
        Map<String,Object> allChanges = new HashMap<String,Object>();

        //First check the annotated properties
        List<SerializableProperty> properties = findProperties(from.getClass());
        for(SerializableProperty property : properties)
            //Compare the property and if it has members, compare them.
            if(different(property.getReadMethod().invoke(from), property.getReadMethod().invoke(to))){
                allChanges.put(property.getName(), property.getReadMethod().invoke(to));
            }

        //Second if we are JsonSerializable check the values returned from that
        JsonMapEntryWriter fromWriter = new JsonMapEntryWriter();
        if(from instanceof JsonSerializable){
            ((JsonSerializable)from).jsonWrite(fromWriter);
        }
        JsonMapEntryWriter toWriter = new JsonMapEntryWriter();
        if(to instanceof JsonSerializable){
            ((JsonSerializable)to).jsonWrite(toWriter);
        }

        //Compare the 2 maps and if different add the toValues (use toWriter to pick up new properties/fields)
        Iterator<String> it = toWriter.keySet().iterator();
        while(it.hasNext()){
            String name = it.next();
            Object fromValue = fromWriter.get(name);
            Object toValue = toWriter.get(name);
            if(different(fromValue, toValue))
                allChanges.put(name, toValue);
        }

        return allChanges;
    }

    protected boolean different(Object fromValue, Object toValue) throws JsonException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException{

        //Either null?
        if(((fromValue == null)&&(toValue != null))||((fromValue != null)&&(toValue == null))) {
            //Test Map and Collections, we assume null and empty are a no-change scenario
            // one of fromValue or toValue must be null but not both
            if(toValue == null) {
                if((fromValue instanceof Collection<?>)&&(((Collection<?>)fromValue).size() == 0))
                    return false;
                if((fromValue instanceof Map<?,?>)&&(((Map<?,?>)fromValue).size() == 0))
                    return false;
            }else {
                if ((toValue instanceof Collection<?>) && (((Collection<?>) toValue).size() == 0))
                    return false;
                if ((toValue instanceof Map<?, ?>) && (((Map<?, ?>) toValue).size() == 0))
                    return false;
            }

            return true;
        }

        //Both null
        if((fromValue == null)&&(toValue == null))
            return false;

        //Different classes
        if(!fromValue.getClass().equals(toValue.getClass()))
            return true;

        //List or map
        if(fromValue instanceof Collection<?>) {
            if(((Collection<?>)fromValue).size() != ((Collection<?>)toValue).size())
                return true;
            return digestsDiffer(fromValue, toValue);
        } else if(fromValue instanceof Map<?,?>) {
            Map<?,?> toMap = (Map<?,?>)toValue;
            Map<?,?> fromMap = (Map<?,?>)fromValue;
            if(fromMap.size() != toMap.size())
                return true;
            for(Entry<?,?> entry : fromMap.entrySet()) {
                if(!toMap.containsKey(entry.getKey()))
                    return true;
                if(entry.getValue() instanceof Map<?,?>) //Recurse
                    if(different(entry.getValue(), toMap.get(entry.getKey())))
                        return true;
                if(digestsDiffer(entry.getValue(), toMap.get(entry.getKey())))
                    return true;
            }
            return false;
        } else if(fromValue instanceof Enum) {
            return !((Enum<?>)fromValue).name().equals(((Enum<?>)toValue).name());
        }else if(fromValue.getClass().isArray()) {
            if(Array.getLength(fromValue) != Array.getLength(toValue))
                return true;
            else
                return digestsDiffer(fromValue, toValue);
        }
        //Same class, check if it has properties
        return differentRecursive(fromValue, toValue);
    }

    private boolean digestsDiffer(Object fromValue, Object toValue) throws IOException, JsonException {
        try (
                DigestOutputStream dos = new DigestOutputStream(new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        // no-op, just digesting
                    }
                }, MessageDigest.getInstance("MD5"));
                OutputStreamWriter toStreamWriter = new OutputStreamWriter(dos);
                OutputStreamWriter fromStreamWriter = new OutputStreamWriter(dos);) {

            JsonWriter fromWriter = new JsonWriter(Common.JSON_CONTEXT, fromStreamWriter);
            //We need fresh writers to avoid miscellaneous commas or whatnot

            JsonWriter toWriter = new JsonWriter(Common.JSON_CONTEXT, toStreamWriter);

            fromWriter.writeObject(fromValue);
            fromWriter.flush();
            byte[] fromDigest = dos.getMessageDigest().digest();
            fromDigest = Arrays.copyOf(fromDigest, fromDigest.length);

            toWriter.writeObject(toValue);
            toWriter.flush();
            byte[] toDigest = dos.getMessageDigest().digest();

            return !Arrays.equals(fromDigest, toDigest);
        } catch(NoSuchAlgorithmException e) {
            //Required to implement MD5, really shouldn't happen
            throw new ShouldNeverHappenException(e);
        }
    }

    protected boolean differentRecursive(Object from, Object to) throws JsonException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException{
        if (from == null && to != null || to == null && from != null)
            return true;
        if (from == null && to == null)
            return false;

        if(!from.getClass().equals(to.getClass()))
            return true;

        List<SerializableProperty> properties = findProperties(from.getClass());

        //Check the serialized annotations
        for(SerializableProperty property : properties)
            if(different(property.getReadMethod().invoke(from), property.getReadMethod().invoke(to)))
                return true;

        //Second if we are JsonSerializable check the values returned from that
        JsonMapEntryWriter fromWriter = new JsonMapEntryWriter();
        if(from instanceof JsonSerializable){
            ((JsonSerializable)from).jsonWrite(fromWriter);
        }
        JsonMapEntryWriter toWriter = new JsonMapEntryWriter();
        if(to instanceof JsonSerializable){
            ((JsonSerializable)to).jsonWrite(toWriter);
        }

        //Compare the 2 maps and if different add the toValues
        Iterator<String> it = toWriter.keySet().iterator();
        if(it.hasNext()){
            while(it.hasNext()){
                String name = it.next();
                Object fromValue = fromWriter.get(name);
                Object toValue = toWriter.get(name);
                if(different(fromValue, toValue))
                    return true;
            }
        }else if(properties.size() == 0){
            //No Sero Json Properties at all, hopefully something that implements .equals()
            return !Objects.equals(from, to);
        }

        return false;
    }

    public List<SerializableProperty> findProperties(Class<?> clazz) throws JsonException{

        //
        // Introspect the class.
        List<SerializableProperty> properties = new ArrayList<>();
        boolean jsonSerializable = JsonSerializable.class.isAssignableFrom(clazz);
        if(!jsonSerializable) {
            //Is it a semi-primative
            if(clazz.isAssignableFrom(Boolean.class) ||
                    clazz.isAssignableFrom(Byte.class) ||
                    clazz.isAssignableFrom(Short.class) ||
                    clazz.isAssignableFrom(Integer.class) ||
                    clazz.isAssignableFrom(Long.class) ||
                    clazz.isAssignableFrom(Float.class) ||
                    clazz.isAssignableFrom(Double.class) ||
                    clazz.isAssignableFrom(BigInteger.class) ||
                    clazz.isAssignableFrom(BigDecimal.class) ||
                    clazz.isAssignableFrom(String.class) ||
                    clazz.isAssignableFrom(Object.class)
                    )
                return properties;
        }
        BeanInfo info;
        try {
            info = Introspector.getBeanInfo(clazz);
        }
        catch (IntrospectionException e) {
            throw new JsonException(e);
        }

        PropertyDescriptor[] descriptors = info.getPropertyDescriptors();

        // Annotations or beans
        Class<?> currentClazz = clazz;
        while (currentClazz != Object.class) {
            boolean annotationsFound = addAnnotatedProperties(currentClazz, descriptors, properties);
            //Serotonin JSON searches for POJO properties here, we don't want to.
            if (!annotationsFound && !currentClazz.isAnnotationPresent(JsonEntity.class) && !jsonSerializable)
                addPojoProperties(currentClazz, descriptors, properties);
            currentClazz = currentClazz.getSuperclass();
        }

        return properties;
    }

    private void addPojoProperties(Class<?> clazz, PropertyDescriptor[] descriptors,
            List<SerializableProperty> properties) {
        for (PropertyDescriptor descriptor : descriptors) {
            String name = descriptor.getName();

            // Don't implicitly marshall getClass()
            if (name.equals("class"))
                continue;

            // Ignore hibernate stuff too
            if (name.equals("hibernateLazyInitializer"))
                continue;

            Method readMethod = descriptor.getReadMethod();
            if (readMethod != null && readMethod.getDeclaringClass() != clazz)
                readMethod = null;

            Method writeMethod = descriptor.getWriteMethod();
            if (writeMethod != null && writeMethod.getDeclaringClass() != clazz)
                writeMethod = null;

            SerializableProperty prop = new SerializableProperty();
            prop.setName(name);
            prop.setReadMethod(readMethod);
            prop.setWriteMethod(writeMethod);

            maybeAddProperty(properties, prop);
        }
    }

    private boolean addAnnotatedProperties(Class<?> clazz, PropertyDescriptor[] descriptors,
            List<SerializableProperty> properties) throws JsonException {
        Map<String, JsonProperty> jsonProperties = gatherJsonPropertyNames(clazz);

        for (PropertyDescriptor descriptor : descriptors) {
            String name = descriptor.getName();

            // Don't implicitly marshall getClass()
            if (name.equals("class"))
                continue;

            // Ignore hibernate stuff too
            if (name.equals("hibernateLazyInitializer"))
                continue;

            JsonProperty anno = jsonProperties.get(name);

            if (anno == null || (!anno.read() && !anno.write()))
                continue;

            Method readMethod = descriptor.getReadMethod();
            if (!anno.write() || (readMethod != null && readMethod.getDeclaringClass() != clazz))
                readMethod = null;

            Method writeMethod = descriptor.getWriteMethod();
            if (!anno.read() || (writeMethod != null && writeMethod.getDeclaringClass() != clazz))
                writeMethod = null;

            if (readMethod == null && writeMethod == null)
                continue;

            SerializableProperty prop = new SerializableProperty();
            prop.setName(name);

            prop.setReadMethod(readMethod);
            prop.setWriteMethod(writeMethod);
            if (!Utils.isEmpty(anno.alias()))
                prop.setAlias(anno.alias());
            prop.setSuppressDefaultValue(anno.suppressDefaultValue());
            prop.setIncludeHints(anno.includeHints());

            maybeAddProperty(properties, prop);
        }

        return !jsonProperties.isEmpty();
    }

    private void maybeAddProperty(List<SerializableProperty> properties, SerializableProperty prop) {
        if (prop.getReadMethod() == null && prop.getWriteMethod() == null)
            return;

        for (SerializableProperty p : properties) {
            if (Utils.equals(p.getNameToUse(), prop.getNameToUse())) {
                if (p.getReadMethod() == null && prop.getReadMethod() != null)
                    p.setReadMethod(prop.getReadMethod());

                if (p.getWriteMethod() == null && prop.getWriteMethod() != null)
                    p.setWriteMethod(prop.getWriteMethod());

                return;
            }
        }
        properties.add(prop);
    }

    private Map<String, JsonProperty> gatherJsonPropertyNames(Class<?> clazz) throws JsonException {
        // Ignore Object.
        if (clazz == Object.class)
            return null;

        Map<String, JsonProperty> jsonProperties = new HashMap<>();

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            JsonProperty anno = field.getAnnotation(JsonProperty.class);
            if (anno != null)
                jsonProperties.put(field.getName(), anno);
        }

        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            JsonProperty anno = method.getAnnotation(JsonProperty.class);
            if (anno == null)
                continue;
            
            // Convert the method name to a property name using the JavaBean rules.
            String name = method.getName();
            if (method.getReturnType() == Boolean.TYPE) {
                if (!name.startsWith("is"))
                    throw new JsonException("Non-JavaBean get methods cannot be marked with JsonRemoteProperty: "
                            + clazz.getName() + "." + name);
                name = Character.toLowerCase(name.charAt(2)) + name.substring(3);
            }
            else {
                if (!name.startsWith("get") && !name.startsWith("set"))
                    throw new JsonException("Non-JavaBean get methods cannot be marked with JsonRemoteProperty: "
                            + clazz.getName() + "." + name);
                name = Character.toLowerCase(name.charAt(3)) + name.substring(4);
            }

            jsonProperties.put(name, anno);
        }

        return jsonProperties;
    }

    /**
     *
     */
    public static JsonObject convertMapToJsonObject(Map<String, Object> map) throws JsonException{
        JsonTypeWriter typeWriter = new JsonTypeWriter(Common.JSON_CONTEXT);
        ObjectTypeWriter writer = new ObjectTypeWriter(typeWriter);

        Iterator<String> it = map.keySet().iterator();
        while(it.hasNext()){
            String name = it.next();
            writer.writeEntry(name, map.get(name));
        }
        return writer.getJsonObject();
    }

}
