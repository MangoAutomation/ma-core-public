/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.serotonin.json.JsonException;
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
 * to allow collecting annotated properties from an Object
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
	 * @param from
	 * @param to
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws JsonException
	 * @throws IOException
	 */
	public Map<String,Object> findChanges(Object from, Object to) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonException, IOException{
		Map<String,Object> allChanges = new HashMap<String,Object>();
		
		//First check the annotated properties
		List<SerializableProperty> properties = findProperties(from.getClass());
		for(SerializableProperty property : properties){
			//Compare the property and if it has members, compare them.
			if(different(property.getReadMethod().invoke(from), property.getReadMethod().invoke(to))){
				allChanges.put(property.getName(), property.getReadMethod().invoke(to));
			}
			
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
			
		//Compare the 2 maps and if different add the toValues
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
	
	protected boolean different(Object fromValue, Object toValue) throws JsonException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		
		//Either null?
		if(((fromValue == null)&&(toValue != null))||((fromValue != null)&&(toValue == null)))
			return true;
		
		//Both null
		if((fromValue == null)&&(toValue == null))
			return false;
		
		//Different classes
		if(!fromValue.getClass().equals(toValue.getClass()))
			return true;
		
		//Same class, check if it has properties
		return differentRecursive(fromValue, toValue);
	}
	
	protected boolean differentRecursive(Object from, Object to) throws JsonException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		List<SerializableProperty> properties = findProperties(from.getClass());
		if(properties.size() == 0)
			return !ObjectUtils.equals(from, to);
		else{
			for(SerializableProperty property : properties)
				if(different(property.getReadMethod().invoke(from), property.getReadMethod().invoke(to)))
					return true;
		}
		return false;
	}
	
	public List<SerializableProperty> findProperties(Class<?> clazz) throws JsonException{
        
		//
        // Introspect the class.
        List<SerializableProperty> properties = new ArrayList<>();

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
            addAnnotatedProperties(currentClazz, descriptors, properties);
            //Serotonin JSON searches for POJO properties here, we don't want to.
            currentClazz = currentClazz.getSuperclass();
        }

        return properties;
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
	 * @param map
	 * @return
	 * @throws JsonException 
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
