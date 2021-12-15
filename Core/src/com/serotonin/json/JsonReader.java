package com.serotonin.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;

import com.serotonin.json.spi.ClassConverter;
import com.serotonin.json.spi.TypeResolver;
import com.serotonin.json.type.JsonTypeReader;
import com.serotonin.json.type.JsonValue;
import com.serotonin.json.util.TypeUtils;

/**
 * Converts a JSON value graph into objects. Instances should be created, used, and discarded. Reuse is generally
 * unwise.
 * 
 * @author Matthew Lohbihler
 */
public class JsonReader {
    @SuppressWarnings("unchecked")
    public static <T> T toObject(JsonContext context, JsonValue json, Type type) throws JsonException {
        JsonReader reader = new JsonReader(context, (JsonValue) null);
        return (T) reader.read(type, json);
    }

    /**
     * The reader's context.
     */
    private final JsonContext context;

    /**
     * The hint to use.
     */
    private String includeHint;

    /**
     * The underlying type reader that supplies the JSON value graph.
     */
    private final JsonTypeReader reader;

    /**
     * The underlying base JSON value in the absence of a type reader.
     */
    private JsonValue jsonValue;

    /**
     * Constructs a JSON reader using the given JSON formatted string. The data can be one or more JSON documents. A new
     * JSON context is created.
     * 
     * @param data
     *            JSON formatted string
     */
    public JsonReader(String data) {
        this(new JsonContext(), new StringReader(data));
    }

    /**
     * Constructs a JSON reader using the given I/O reader. The data can be one or more JSON documents. A new JSON
     * context is created.
     * 
     * @param reader
     *            an I/O reader supplying JSON formatted data.
     */
    public JsonReader(Reader reader) {
        this(new JsonContext(), reader);
    }

    /**
     * Constructs a JSON reader using the given JSON value. This can be the result from a JSON type reader or an
     * explicitly constructed JSON value object. A new JSON context is created.
     * 
     * @param jsonValue
     *            the JSON value to use.
     */
    public JsonReader(JsonValue jsonValue) {
        this(new JsonContext(), jsonValue);
    }

    /**
     * Constructs a JSON reader using the given JSON formatted string. The data can be one or more JSON documents.
     * 
     * @param context
     *            the context to use
     * @param data
     *            JSON formatted string
     */
    public JsonReader(JsonContext context, String data) {
        this(context, new StringReader(data));
    }

    /**
     * Constructs a JSON reader using the given I/O reader. The data can be one or more JSON documents.
     * 
     * @param context
     *            the context to use
     * @param reader
     *            an I/O reader supplying JSON formatted data.
     */
    public JsonReader(JsonContext context, Reader reader) {
        this.context = context;
        includeHint = context.getDefaultIncludeHint();
        this.reader = new JsonTypeReader(reader, context.getMaximumDocumentLength());
        this.jsonValue = null;
    }

    /**
     * Constructs a JSON reader using the given JSON value. This can be the result from a JSON type reader or an
     * explicitly constructed JSON value object.
     * 
     * @param context
     *            the context to use
     * @param jsonValue
     *            the JSON value to use.
     */
    public JsonReader(JsonContext context, JsonValue jsonValue) {
        this.context = context;
        includeHint = context.getDefaultIncludeHint();
        this.reader = null;
        this.jsonValue = jsonValue;
    }

    public JsonContext getContext() {
        return context;
    }

    public String getIncludeHint() {
        return includeHint;
    }

    public void setIncludeHint(String includeHint) {
        this.includeHint = includeHint;
    }

    public boolean isDone() throws JsonException, IOException {
        if (reader != null)
            return reader.isEos();
        return jsonValue == null;
    }

    private JsonValue next() throws JsonException, IOException {
        JsonValue jsonValue = null;
        if (reader != null) {
            if (!reader.isEos())
                jsonValue = reader.read();
        }
        else {
            jsonValue = this.jsonValue;
            this.jsonValue = null;
        }

        return jsonValue;
    }

    /**
     * Return an object of the given class that represents the next JSON value from the reader.
     * 
     * @param <T>
     *            the generic type of the object to return.
     * @param clazz
     *            the explicit class of object to return
     * @return the populated object
     */
    @SuppressWarnings("unchecked")
    public <T> T read(Class<T> clazz) throws JsonException, IOException {
        return (T) read((Type) clazz);
    }

    /**
     * Return an object of the given generic type that represents the next JSON value from the reader. This method can
     * be used when the type to return is, say, {@code List<MyObject>}. To represent a specific type, use the TypeDefinition
     * object.
     * 
     * @param type
     *            the generic type of object to return
     * @return the populated object
     */
    public Object read(Type type) throws JsonException, IOException {
        JsonValue value = next();
        if (value == null)
            return null;
        return read(type, value);
    }

    /**
     * Return an object of the given class that represents the given JSON value.
     * 
     * @param <T>
     *            the generic type of the object to return.
     * @param clazz
     *            the explicit class of object to return
     * @param jsonValue
     *            the JSON value from which to get object data
     * @return the populated object
     */
    @SuppressWarnings("unchecked")
    public <T> T read(Class<T> clazz, JsonValue jsonValue) throws JsonException {
        return (T) read((Type) clazz, jsonValue);
    }

    /**
     * Return an object of the given generic type that represents the given JSON value. This method can be used when the
     * type to return is, say, {@code List<MyObject>}. To represent a specific type, use the TypeDefinition object.
     * 
     * @param type
     *            the generic type of object to return
     * @param jsonValue
     *            the JSON value from which to get object data
     * @return the populated object
     */
    public Object read(Type type, JsonValue jsonValue) throws JsonException {
        if (jsonValue == null)
            return null;

        Class<?> clazz = TypeUtils.getRawClass(type);

        TypeResolver resolver = context.getResolver(clazz);
        if (resolver != null) {
            type = resolver.resolve(jsonValue);
            clazz = TypeUtils.getRawClass(type);
        }

        ClassConverter converter = context.getConverter(clazz);
        Object value = converter.jsonRead(this, jsonValue, type);
        return value;
    }

    /**
     * Populate the given object with data from the next JSON value from the reader.
     * 
     * @param obj
     *            the object to populate
     */
    public void readInto(Object obj) throws JsonException, IOException {
        readInto(obj, next());
    }

    /**
     * Populate the given object with data from the given JSON value.
     * 
     * @param obj
     *            the object to populate
     * @param jsonValue
     *            the JSON value from which to get object data
     */
    public void readInto(Object obj, JsonValue jsonValue) throws JsonException {
        if (obj == null)
            return;
        readInto(obj.getClass(), obj, jsonValue);
    }

    /**
     * Populate the given object with data from the next JSON value from the reader.
     * 
     * @param type
     *            due to erasure, the generic type of the given object is indeterminate. The type is given to provide
     *            this information.
     * @param obj
     *            the object to populate
     */
    public void readInto(Type type, Object obj) throws JsonException, IOException {
        readInto(type, obj, next());
    }

    /**
     * Populate the given object with data from the given JSON value.
     * 
     * @param type
     *            due to erasure, the generic type of the given object is indeterminate. The type is given to provide
     *            this information.
     * @param obj
     *            the object to populate
     * @param jsonValue
     *            the JSON value from which to get object data
     */
    public void readInto(Type type, Object obj, JsonValue jsonValue) throws JsonException {
        if (obj == null || jsonValue == null)
            return;

        Class<?> clazz = TypeUtils.getRawClass(type);
        ClassConverter converter = context.getConverter(clazz);
        converter.jsonRead(this, jsonValue, obj, type);
    }
}
