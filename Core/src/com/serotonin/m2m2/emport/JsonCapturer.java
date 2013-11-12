/**
 * Copyright (C) 2013 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.emport;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.serotonin.json.JsonContext;
import com.serotonin.json.JsonException;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonSerializable;
/**
 * @author Terry Packer
 *
 */
public class JsonCapturer implements ObjectWriter{

	private TreeMap<String,String> values;
	private JsonContext context;
	private JsonSerializable vo;
	
	public JsonCapturer(JsonContext context, JsonSerializable vo){
		this.context = context;
		this.values = new TreeMap<String,String>();
		this.vo = vo;
	}
	
	
	public void capture() throws JsonException, IOException{
		
		//Gather the Json Props
		Map<String, JsonProperty> props = this.gatherJsonPropertyNames(vo.getClass());
        BeanInfo info;
        try {
            info = Introspector.getBeanInfo(vo.getClass());
        }
        catch (IntrospectionException e) {
            throw new JsonException(e);
        }

		
		//Using introspection get the values
		for(PropertyDescriptor descriptor : info.getPropertyDescriptors()){
			try {
				JsonProperty anno = props.get(descriptor.getName());
				 if (anno == null || (!anno.read()))
		            continue;
	            Method readMethod = descriptor.getReadMethod();
	            if (!anno.read() || (readMethod != null && readMethod.getDeclaringClass() != vo.getClass()))
	                readMethod = null;
				if(readMethod == null)
					continue;
				
				this.writeEntry(descriptor.getName(),readMethod.invoke(vo));
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//Add in the remaining info from the method
		vo.jsonWrite(this);
	}
	
	
	/* (non-Javadoc)
	 * @see com.serotonin.json.ObjectWriter#writeEntry(java.lang.String, java.lang.Object)
	 */
	@Override
	public void writeEntry(String paramString, Object paramObject)
			throws IOException, JsonException {
		
		
		if(paramObject instanceof JsonSerializable){
			JsonCapturer capturer = new JsonCapturer(this.context,(JsonSerializable) paramObject);
			recursivelyGenerateJson(paramString,capturer,paramObject);
		}else{
			//Not JsonSerializable but could be something more complex
			this.values.put(paramString,this.getObjectString(paramObject));
		}
	}

	
	/**
	 * Recursively generate Object String
	 * @param paramObject
	 * @return
	 */
	public String getObjectString(Object paramObject){
		if(paramObject == null)
			return "null";
		else if(paramObject.getClass().isArray()){
			int length = Array.getLength(paramObject);
			StringWriter writer = new StringWriter();
			writer.write("[");
	        for (int i = 0; i < length; i++) {
	            if (i > 0)
	                writer.append(',');
	            writer.append(this.getObjectString(Array.get(paramObject, i)));
	        }
	        writer.write("]");
	        return writer.toString();
		}else{
			//All others is just an output
			return paramObject.toString();
		}
	}
	
	
	public void recursivelyGenerateJson(String prefix,JsonCapturer capturer,Object jsonableObject){
		
		try {
			capturer.capture();
			((JsonSerializable)jsonableObject).jsonWrite(capturer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Put the objects into this map with the prefix
		for(String key : capturer.getValues().keySet()){
			this.values.put(prefix+"."+key, capturer.getValues().get(key));
		}
		
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.json.ObjectWriter#finish()
	 */
	@Override
	public void finish() throws IOException {
		//No-op
		
	}
	
	
	public TreeMap<String,String> getValues(){
		return this.values;
	}
	
	
    private Map<String, JsonProperty> gatherJsonPropertyNames(Class<?> clazz) throws JsonException {
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
                if (!name.startsWith("get"))
                    throw new JsonException("Non-JavaBean get methods cannot be marked with JsonRemoteProperty: "
                            + clazz.getName() + "." + name);
                name = Character.toLowerCase(name.charAt(3)) + name.substring(4);
            }

            jsonProperties.put(name, anno);
        }

        return jsonProperties;
    }

	

}
