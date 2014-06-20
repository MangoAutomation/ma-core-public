/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.converter;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.spi.ClassConverter;
import com.serotonin.json.spi.JsonEntity;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.util.SerializableProperty;
import com.serotonin.json.util.Utils;
import com.serotonin.m2m2.vo.DataPointVO;

/**
 * Factory to generate specialized converters 
 * for Generating JSON
 * 
 * Currently Only Supporting DataPointVO.class
 * 
 * 
 * @author Terry Packer
 *
 */
public class ClassConverterFactory {
	
	/**
	 * 
	 * @param clazz
	 * @return
	 */
	public static ClassConverter createConverter(Class<?> clazz){
		try{
			boolean jsonSerializable = JsonSerializable.class.isAssignableFrom(clazz);
	        boolean jsonEntity = clazz.isAnnotationPresent(JsonEntity.class);
	        List<SerializableProperty> properties = new ArrayList<SerializableProperty>();
	
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
	
	            if (!annotationsFound && !currentClazz.isAnnotationPresent(JsonEntity.class) && !jsonSerializable)
	                // Not annotated and no property annotations were found. Consider it a POJO.
	                addPojoProperties(currentClazz, descriptors, properties);
	
	            currentClazz = currentClazz.getSuperclass();
	        }
	
	        if (properties.isEmpty())
	            properties = null;
	
	        // Create a converter?
	        if (jsonSerializable || jsonEntity || properties != null){
	        	
	        	//Add additional Class Converters here
	        	if(clazz == DataPointVO.class)
	        		return  new DataPointClassConverter(jsonSerializable, properties);
	        	else 
	        		throw new ShouldNeverHappenException("Unsupported type for factory.");
	        }else
	        	throw new ShouldNeverHappenException("Can't create converter for this.");
		}catch(Exception e){
			throw new ShouldNeverHappenException("Can't create converter for this.");
		}
	}
	


	private static boolean addAnnotatedProperties(Class<?> clazz, PropertyDescriptor[] descriptors,
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

            // if (anno.typeFactory() != TypeFactory.class)
            // prop.setTypeFactory(anno.typeFactory());

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

    private static void addPojoProperties(Class<?> clazz, PropertyDescriptor[] descriptors,
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

    private static void maybeAddProperty(List<SerializableProperty> properties, SerializableProperty prop) {
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

    private static Map<String, JsonProperty> gatherJsonPropertyNames(Class<?> clazz) throws JsonException {
        // Ignore Object.
        if (clazz == Object.class)
            return null;

        Map<String, JsonProperty> jsonProperties = new HashMap<String, JsonProperty>();

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

}
