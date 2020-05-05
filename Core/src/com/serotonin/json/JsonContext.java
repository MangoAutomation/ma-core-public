package com.serotonin.json;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.util.script.ScriptPermissions;
import com.serotonin.json.convert.ArrayConverter;
import com.serotonin.json.convert.BigDecimalConverter;
import com.serotonin.json.convert.BigIntegerConverter;
import com.serotonin.json.convert.BooleanConverter;
import com.serotonin.json.convert.ByteConverter;
import com.serotonin.json.convert.CollectionConverter;
import com.serotonin.json.convert.DateConverter;
import com.serotonin.json.convert.DoubleConverter;
import com.serotonin.json.convert.EnumConverter;
import com.serotonin.json.convert.FloatConverter;
import com.serotonin.json.convert.IntegerConverter;
import com.serotonin.json.convert.JacksonJsonNodeConverter;
import com.serotonin.json.convert.JsonArrayConverter;
import com.serotonin.json.convert.JsonBooleanConverter;
import com.serotonin.json.convert.JsonNumberConverter;
import com.serotonin.json.convert.JsonObjectConverter;
import com.serotonin.json.convert.JsonPropertyConverter;
import com.serotonin.json.convert.JsonStringConverter;
import com.serotonin.json.convert.JsonValueConverter;
import com.serotonin.json.convert.LongConverter;
import com.serotonin.json.convert.MangoPermissionConverter;
import com.serotonin.json.convert.MapConverter;
import com.serotonin.json.convert.ObjectConverter;
import com.serotonin.json.convert.RoleConverter;
import com.serotonin.json.convert.ScriptPermissionConverter;
import com.serotonin.json.convert.SerializerConverter;
import com.serotonin.json.convert.ShortConverter;
import com.serotonin.json.convert.StreamedArrayConverter;
import com.serotonin.json.convert.StringConverter;
import com.serotonin.json.convert.UUIDConverter;
import com.serotonin.json.factory.DefaultConstructorFactory;
import com.serotonin.json.factory.ListFactory;
import com.serotonin.json.factory.MapFactory;
import com.serotonin.json.factory.SetFactory;
import com.serotonin.json.spi.ClassConverter;
import com.serotonin.json.spi.ClassSerializer;
import com.serotonin.json.spi.JsonEntity;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.spi.JsonPropertyOrder;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.spi.ObjectFactory;
import com.serotonin.json.spi.TypeResolver;
import com.serotonin.json.type.JsonArray;
import com.serotonin.json.type.JsonBoolean;
import com.serotonin.json.type.JsonNumber;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonStreamedArray;
import com.serotonin.json.type.JsonString;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.util.MaxCharacterCountExceededException;
import com.serotonin.json.util.SerializableProperty;
import com.serotonin.json.util.Utils;
import com.serotonin.m2m2.vo.role.Role;

/**
 * The JsonContext is the central repository of converters and factories for all JSON conversion. Typically, an
 * application will create and (if necessary) configure one of these and share it. Multiple instances may be created if
 * different types of conversion are required for different purposes. (See the discussion of includeHints in
 * JsonProperty.)
 *
 * For example, a server application may convert value objects for transport to a client application, and include fields
 * such as the primary key. For saving the same objects into a SQL database though (if the objects are sufficiently
 * complex or variable that it is undesirable to create an explicit schema for them), the primary key would be a table
 * column and so not required in the JSON. In this case two different JSON contexts would be created, one with a default
 * include hint of perhaps "client", and the other with one of "database". Annotations or custom code can then be used
 * to determine if an object attribute should be included in the JSON for the given context or not.
 *
 * By default a context populates its registry with converters for all primitives as well as String, BigInteger,
 * BigDecimal, Map, List, Set, Enum, and array objects. Naturally, all native JSON type objects have default converters
 * as well.
 *
 * @author Matthew Lohbihler
 */
public class JsonContext {
    private final Map<Class<?>, ClassConverter> classConverters = new ConcurrentHashMap<>();
    private final Map<Class<?>, TypeResolver> typeResolvers = new ConcurrentHashMap<>();
    private final Map<Class<?>, ObjectFactory> objectFactories = new ConcurrentHashMap<>();
    private DefaultConstructorFactory defaultConstructorFactory = new DefaultConstructorFactory();
    private String defaultIncludeHint;

    /**
     * Determines whether forward slashes ('/') in strings should be escaped (true) or not (false).
     */
    private boolean escapeForwardSlash = true;

    /**
     * The maximum number of characters to read before throwing a {@link MaxCharacterCountExceededException}. Setting
     * this to -1 means that documents can be of any length. This option is useful when reading from untrusted streams.
     */
    private int maximumDocumentLength = -1;

    public JsonContext() {
        this(null);
    }

    public JsonContext(String defaultIncludeHint) {
        this.defaultIncludeHint = defaultIncludeHint;

        // Primitive (or so) converters
        addConverter(new BooleanConverter(), Boolean.class, Boolean.TYPE);
        addConverter(new ByteConverter(), Byte.class, Byte.TYPE);
        addConverter(new ShortConverter(), Short.class, Short.TYPE);
        addConverter(new IntegerConverter(), Integer.class, Integer.TYPE);
        addConverter(new LongConverter(), Long.class, Long.TYPE);
        addConverter(new FloatConverter(), Float.class, Float.TYPE);
        addConverter(new DoubleConverter(), Double.class, Double.TYPE);
        addConverter(new BigIntegerConverter(), BigInteger.class);
        addConverter(new BigDecimalConverter(), BigDecimal.class);
        addConverter(new StringConverter(), String.class, Character.class, Character.TYPE);
        addConverter(new ObjectConverter(), Object.class);

        // Native JSON types
        addConverter(new JsonArrayConverter(), JsonArray.class);
        addConverter(new JsonBooleanConverter(), JsonBoolean.class);
        addConverter(new JsonNumberConverter(), JsonNumber.class);
        addConverter(new JsonObjectConverter(), JsonObject.class);
        addConverter(new JsonStringConverter(), JsonString.class);
        addConverter(new JsonValueConverter(), JsonValue.class);

        // Interface and array converters
        addConverter(new ArrayConverter(), Array.class);
        addConverter(new CollectionConverter(), Collection.class);
        addConverter(new EnumConverter(), Enum.class);
        addConverter(new MapConverter(), Map.class);
        addConverter(new StreamedArrayConverter(), JsonStreamedArray.class);

        // Other converters
        addConverter(new UUIDConverter(), UUID.class);
        addConverter(new JacksonJsonNodeConverter(), JsonNode.class);
        addConverter(new DateConverter(), Date.class);
        addConverter(new RoleConverter(), Role.class);
        addConverter(new MangoPermissionConverter(), MangoPermission.class);
        addConverter(new ScriptPermissionConverter(), ScriptPermissions.class);

        // Object factories
        addFactory(new ListFactory(), List.class);
        addFactory(new MapFactory(), Map.class);
        addFactory(new SetFactory(), Set.class);
    }

    public String getDefaultIncludeHint() {
        return defaultIncludeHint;
    }

    public void setDefaultIncludeHint(String defaultIncludeHint) {
        this.defaultIncludeHint = defaultIncludeHint;
    }

    /**
     * Register a ClassSerializer against the given class. This method will wrap the serializer in a SerializerConverter
     * and then register the converter with the class.
     *
     * @param <T>
     *            the generic type of the class
     * @param serializer
     *            the serializer to register
     * @param clazz
     *            the class to which to bind the serializer
     */
    public <T> void addSerializer(ClassSerializer<T> serializer, Class<?> clazz) {
        classConverters.put(clazz, new SerializerConverter<>(serializer));
    }

    /**
     * Register a ClassConverter against the given list of classes.
     *
     * @param converter
     *            the converter instance
     *
     * @param classes
     *            the classes to which the converter should be bound.
     */
    public void addConverter(ClassConverter converter, Class<?>... classes) {
        for (Class<?> clazz : classes)
            classConverters.put(clazz, converter);
    }

    /**
     * Returns the class converter for the given class.
     *
     * @param clazz
     *            the class to look up
     * @return the converter that is bound to the class. May be null if no converter was registered.
     * @throws JsonException
     */
    public ClassConverter getConverter(Class<?> clazz) throws JsonException {
        // First check if the class is already in the map.
        ClassConverter converter = classConverters.get(clazz);
        if (converter != null)
            return converter;

        // Check for inheritance
        Class<?> sc = clazz.getSuperclass();
        while (sc != null && sc != Object.class) {
            converter = classConverters.get(sc);
            if (converter != null) {
                classConverters.put(clazz, converter);
                return converter;
            }
            sc = sc.getSuperclass();
        }

        // Check for enum
        if (Enum.class.isAssignableFrom(clazz)) {
            converter = classConverters.get(Enum.class);
            if (converter != null) {
                classConverters.put(clazz, converter);
                return converter;
            }
        }

        // Check for map
        if (Map.class.isAssignableFrom(clazz)) {
            converter = classConverters.get(Map.class);
            if (converter != null) {
                classConverters.put(clazz, converter);
                return converter;
            }
        }

        // Check for collection
        if (Collection.class.isAssignableFrom(clazz)) {
            converter = classConverters.get(Collection.class);
            if (converter != null) {
                classConverters.put(clazz, converter);
                return converter;
            }
        }

        // Check for array
        if (clazz.isArray()) {
            converter = classConverters.get(Array.class);
            if (converter != null) {
                classConverters.put(clazz, converter);
                return converter;
            }
        }

        if(JsonStreamedArray.class.isAssignableFrom(clazz)) {
            converter = classConverters.get(JsonStreamedArray.class);
            if (converter != null) {
                classConverters.put(clazz, converter);
                return converter;
            }
        }

        //
        // Introspect the class.
        boolean jsonSerializable = JsonSerializable.class.isAssignableFrom(clazz);
        boolean jsonEntity = clazz.isAnnotationPresent(JsonEntity.class);
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
        List<String> propertyOrder = new ArrayList<>();
        while (currentClazz != Object.class) {
            boolean annotationsFound = addAnnotatedProperties(currentClazz, descriptors, properties);

            if (!annotationsFound && !currentClazz.isAnnotationPresent(JsonEntity.class) && !jsonSerializable)
                // Not annotated and no property annotations were found. Consider it a POJO.
                addPojoProperties(currentClazz, descriptors, properties);

            JsonPropertyOrder order = clazz.getAnnotation(JsonPropertyOrder.class);
            if(order != null) {
                for(String field : order.value()) {
                    propertyOrder.add(field);
                }
            }

            currentClazz = currentClazz.getSuperclass();
        }

        if (properties.isEmpty()) {
            properties = null;
        }else {
            List<SerializableProperty> ordered = new ArrayList<>();
            for(String field : propertyOrder) {
                Iterator<SerializableProperty> it = properties.listIterator();
                while(it.hasNext()) {
                    SerializableProperty property = it.next();
                    if(StringUtils.equals(field, property.getNameToUse())) {
                        ordered.add(property);
                        it.remove();
                        break;
                    }
                }
            }
            //Add in the remaining properties
            for(SerializableProperty property : properties) {
                ordered.add(property);
            }
            properties.clear();
            properties.addAll(ordered);
        }

        // Create a converter?
        if (jsonSerializable || jsonEntity || properties != null) {
            converter = new JsonPropertyConverter(jsonSerializable, properties);
            classConverters.put(clazz, converter);
            return converter;
        }

        // Give up
        throw new JsonException("No converter for class " + clazz);
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
            prop.setReadAliases(anno.readAliases());

            maybeAddProperty(properties, prop);
        }

        return !jsonProperties.isEmpty();
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
     * Register an {@link TypeResolver} against the given list of classes.
     *
     * @param resolver
     *            the type resolver to register
     *
     * @param classes
     *            the classes to which the resolver should be bound.
     */
    public void addResolver(TypeResolver resolver, Class<?>... classes) {
        for (Class<?> clazz : classes)
            typeResolvers.put(clazz, resolver);
    }

    /**
     * Returns the {@link TypeResolver} bound to the given class.
     *
     * @param clazz
     *            the class to look up
     * @return the type resolver, or null if not found.
     */
    public TypeResolver getResolver(Class<?> clazz) {
        return typeResolvers.get(clazz);
    }

    /**
     * Register an ObjectFactory against the given list of classes.
     *
     * @param factory
     *            the object factory to register
     *
     * @param classes
     *            the classes to which the factory should be bound.
     */
    public void addFactory(ObjectFactory factory, Class<?>... classes) {
        for (Class<?> clazz : classes)
            objectFactories.put(clazz, factory);
    }

    /**
     * Create a new instance of the given class using the given jsonValue if necessary. If no object factory is found
     * for the given class, the default constructor factory is used.
     *
     * @param clazz
     *            the clazz of object to be created
     * @param jsonValue
     *            the jsonValue to use for hints. The given class may be abstract or a base class, so the jsonValue can
     *            provider information as to the specific subclass of object to instantiate.
     * @return the new object instance
     * @throws JsonException
     */
    public Object getNewInstance(Class<?> clazz, JsonValue jsonValue) throws JsonException {
        ObjectFactory factory = objectFactories.get(clazz);
        if (factory != null) {
            return factory.create(jsonValue);
        }
        return defaultConstructorFactory.create(clazz);
    }

    public DefaultConstructorFactory getDefaultConstructorFactory() {
        return defaultConstructorFactory;
    }

    public void setDefaultConstructorFactory(DefaultConstructorFactory defaultConstructorFactory) {
        this.defaultConstructorFactory = defaultConstructorFactory;
    }

    public boolean isEscapeForwardSlash() {
        return escapeForwardSlash;
    }

    public void setEscapeForwardSlash(boolean escapeForwardSlash) {
        this.escapeForwardSlash = escapeForwardSlash;
    }

    public int getMaximumDocumentLength() {
        return maximumDocumentLength;
    }

    public void setMaximumDocumentLength(int maximumDocumentLength) {
        this.maximumDocumentLength = maximumDocumentLength;
    }
}
