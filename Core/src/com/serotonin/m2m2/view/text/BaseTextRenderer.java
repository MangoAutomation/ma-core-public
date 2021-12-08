/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.spi.TypeResolver;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.rt.dataImage.PointValueTime;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.ImplDefinition;

abstract public class BaseTextRenderer implements TextRenderer, JsonSerializable {
    static List<ImplDefinition> definitions;

    static void ensureDefinitions() {
        if (definitions == null) {
            List<ImplDefinition> d = new ArrayList<ImplDefinition>();
            d.add(AnalogRenderer.getDefinition());
            d.add(BinaryTextRenderer.getDefinition());
            d.add(MultistateRenderer.getDefinition());
            d.add(NoneRenderer.getDefinition());
            d.add(PlainRenderer.getDefinition());
            d.add(RangeRenderer.getDefinition());
            d.add(TimeRenderer.getDefinition());
            definitions = d;
        }
    }

    public static List<ImplDefinition> getImplementation(DataType dataType) {
        ensureDefinitions();
        List<ImplDefinition> impls = new ArrayList<ImplDefinition>(definitions.size());
        for (ImplDefinition def : definitions) {
            if (def.supports(dataType))
                impls.add(def);
        }
        return impls;
    }

    public static List<String> getExportTypes() {
        ensureDefinitions();
        List<String> result = new ArrayList<String>(definitions.size());
        for (ImplDefinition def : definitions)
            result.add(def.getExportName());
        return result;
    }

    @Override
    public String getText(int hint, Locale locale) {
        if (hint == HINT_RAW)
            return "";
        return UNKNOWN_VALUE;
    }

    @Override
    public String getText(PointValueTime valueTime, int hint, Locale locale) {
        if (valueTime == null)
            return getText(hint, locale);
        return getText(valueTime.getValue(), hint, locale);
    }

    @Override
    public String getText(DataValue value, int hint, Locale locale) {
        if (value == null)
            return getText(hint, locale);
        return getTextImpl(value, hint, locale);
    }

    abstract protected String getTextImpl(DataValue value, int hint, Locale locale);

    @Override
    public String getText(double value, int hint, Locale locale) {
        return Double.toString(value);
    }

    @Override
    public String getText(int value, int hint, Locale locale) {
        return Integer.toString(value);
    }

    @Override
    public String getText(boolean value, int hint, Locale locale) {
        return value ? "1" : "0";
    }

    @Override
    public String getText(String value, int hint, Locale locale) {
        return value;
    }

    @Override
    public String getMetaText() {
        return null;
    }

    //
    // Colours
    @Override
    public String getColour() {
        return null;
    }

    @Override
    public String getColour(PointValueTime valueTime) {
        if (valueTime == null)
            return getColour();
        return getColour(valueTime.getValue());
    }

    @Override
    public String getColour(DataValue value) {
        if (value == null)
            return getColour();
        return getColourImpl(value);
    }

    abstract protected String getColourImpl(DataValue value);

    @Override
    public String getColour(double value) {
        return null;
    }

    @Override
    public String getColour(int value) {
        return null;
    }

    @Override
    public String getColour(boolean value) {
        return null;
    }

    @Override
    public String getColour(String value) {
        return null;
    }

    //
    // Parse
    @Override
    public DataValue parseText(String s, DataType dataType) {
        return DataValue.stringToValue(s, dataType);
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        in.readInt(); // Read the version. Value is currently not used.
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        writer.writeEntry("type", getDef().getExportName());
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        // no op. The type value is used by the factory.
    }

    public static class Resolver implements TypeResolver {
        @Override
        public Type resolve(JsonValue jsonValue) throws JsonException {
            JsonObject json = jsonValue.toJsonObject();

            String type = json.getString("type");
            if (type == null)
                throw new TranslatableJsonException("emport.error.text.missing", "type", getExportTypes());

            ImplDefinition def = null;
            ensureDefinitions();
            for (ImplDefinition id : definitions) {
                if (id.getExportName().equalsIgnoreCase(type)) {
                    def = id;
                    break;
                }
            }

            if (def == null)
                throw new TranslatableJsonException("emport.error.text.invalid", "type", type, getExportTypes());

            Class<? extends TextRenderer> clazz = null;
            if (def == AnalogRenderer.getDefinition())
                clazz = AnalogRenderer.class;
            else if (def == BinaryTextRenderer.getDefinition())
                clazz = BinaryTextRenderer.class;
            else if (def == MultistateRenderer.getDefinition())
                clazz = MultistateRenderer.class;
            else if (def == NoneRenderer.getDefinition())
                clazz = NoneRenderer.class;
            else if (def == PlainRenderer.getDefinition())
                clazz = PlainRenderer.class;
            else if (def == RangeRenderer.getDefinition())
                clazz = RangeRenderer.class;
            else if (def == TimeRenderer.getDefinition())
                clazz = TimeRenderer.class;
            else
                throw new ShouldNeverHappenException("What's this?: " + def.getName());

            return clazz;
        }
    }
    
    
}
