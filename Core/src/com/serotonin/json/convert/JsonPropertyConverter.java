package com.serotonin.json.convert;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import com.serotonin.json.JsonContext;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonWriter;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonTypeWriter;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.type.ObjectTypeWriter;
import com.serotonin.json.util.SerializableProperty;
import com.serotonin.json.util.TypeUtils;

/**
 * This class is the general purpose converter for objects for which no other converter has been explicitly provided.
 * Instance of this class are typically generated automatically by the JSON context, but can also be provided by client
 * code.
 *
 * @author Matthew Lohbihler
 */
public class JsonPropertyConverter extends AbstractClassConverter {
    private final boolean jsonSerializable;
    private final List<SerializableProperty> properties;

    /**
     * Constructor.
     *
     * @param jsonSerializable
     *            does the class implement JsonSerializable?
     * @param properties
     *            the list of serializable properties of the class.
     */
    public JsonPropertyConverter(boolean jsonSerializable, List<SerializableProperty> properties) {
        this.jsonSerializable = jsonSerializable;
        this.properties = properties;
    }

    @Override
    public JsonValue jsonWrite(JsonTypeWriter writer, Object value) throws JsonException {
        ObjectTypeWriter otw = new ObjectTypeWriter(writer);
        try {
            jsonWrite(writer.getIncludeHint(), value, otw);
            return otw.getJsonObject();
        }
        catch (IOException e) {
            // Should never happen
            throw new RuntimeException(e);
        }
    }

    @Override
    public void jsonWrite(JsonWriter writer, Object value) throws IOException, JsonException {
        jsonWrite(writer.getIncludeHint(), value, new ObjectJsonWriter(writer));
    }

    public void jsonWrite(String includeHint, Object value, ObjectWriter objectWriter) throws IOException,
    JsonException {
        if (jsonSerializable)
            ((JsonSerializable) value).jsonWrite(objectWriter);

        if (properties != null) {
            for (SerializableProperty prop : properties) {
                // Check whether the property should be included
                if (!prop.include(includeHint))
                    continue;

                Method readMethod = prop.getReadMethod();
                if (readMethod == null)
                    continue;

                String name = prop.getNameToUse();

                Object propertyValue;
                try {
                    propertyValue = readMethod.invoke(value);
                }
                catch (Exception e) {
                    throw new JsonException("Error reading '" + prop.getName() + "' from value " + value + " of class "
                            + value.getClass(), e);
                }

                // Check if the value should be ignored.
                boolean ignore = false;
                if (prop.isSuppressDefaultValue()) {
                    if (propertyValue == null)
                        ignore = true;
                    else {
                        Class<?> propertyClass = readMethod.getReturnType();
                        // Class<?> clazz = propertyValue.getClass();

                        // Check if this value is the properties default value.
                        if (propertyClass == Boolean.TYPE)
                            ignore = ((Boolean) propertyValue) == false;
                        else if (propertyClass == Double.TYPE)
                            ignore = (Double) propertyValue == 0;
                        else if (propertyClass == Long.TYPE)
                            ignore = (Long) propertyValue == 0;
                        else if (propertyClass == Float.TYPE)
                            ignore = (Float) propertyValue == 0;
                        else if (propertyClass == Integer.TYPE)
                            ignore = (Integer) propertyValue == 0;
                        else if (propertyClass == Short.TYPE)
                            ignore = (Short) propertyValue == 0;
                        else if (propertyClass == Byte.TYPE)
                            ignore = (Byte) propertyValue == 0;
                        else if (propertyClass == Character.TYPE)
                            ignore = (Character) propertyValue == 0;
                    }
                }

                if (!ignore)
                    objectWriter.writeEntry(name, propertyValue);
            }
        }

        objectWriter.finish();
    }

    @Override
    protected Object newInstance(JsonContext context, JsonValue jsonValue, Type type) throws JsonException {
        return context.getNewInstance(TypeUtils.getRawClass(type), jsonValue);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonValue jsonValue, Object obj, Type type) throws JsonException {
        JsonObject jsonObject = (JsonObject) jsonValue;

        if (jsonSerializable)
            ((JsonSerializable) obj).jsonRead(reader, jsonObject);

        if (properties != null) {
            for (SerializableProperty prop : properties) {
                // Check whether the property should be included
                if (!prop.include(reader.getIncludeHint()))
                    continue;

                Method writeMethod = prop.getWriteMethod();
                if (writeMethod == null)
                    continue;

                String name = prop.getNameToUse();

                JsonValue propJsonValue = null;
                boolean foundName = false;
                if (jsonObject.containsKey(name)) {
                    propJsonValue = jsonObject.get(name);
                    foundName = true;
                }else {
                    //Try the aliases
                    if (prop.getReadAliases() != null && prop.getReadAliases().length > 0) {
                        for (String readAlias : prop.getReadAliases()) {
                            if(jsonObject.containsKey(readAlias)) {
                                propJsonValue = jsonObject.get(readAlias);
                                foundName = true;
                                break;
                            }
                        }
                    }
                }

                //The property is not in the JSON
                if (!foundName) {
                    continue;
                }

                Type propType = writeMethod.getGenericParameterTypes()[0];
                propType = TypeUtils.resolveTypeVariable(type, propType);
                Class<?> propClass = TypeUtils.getRawClass(propType);

                try {
                    Object propValue = reader.read(propType, propJsonValue);

                    if (propClass.isPrimitive() && propValue == null) {
                        if (propClass == Boolean.TYPE)
                            propValue = false;
                        else
                            propValue = 0;
                    }

                    prop.getWriteMethod().invoke(obj, propValue);
                }
                catch (Exception e) {
                    throw new JsonException("JsonException reading property '" + prop.getName() + "' of class "
                            + propClass.getName(), e);
                }
            }
        }
    }
}
