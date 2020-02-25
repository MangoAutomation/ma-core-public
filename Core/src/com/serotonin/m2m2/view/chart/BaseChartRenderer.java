/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.chart;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonSerializable;
import com.serotonin.json.spi.TypeResolver;
import com.serotonin.json.type.JsonObject;
import com.serotonin.json.type.JsonValue;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.view.ImplDefinition;

abstract public class BaseChartRenderer implements ChartRenderer, JsonSerializable {
    private static ImplDefinition noneDefinition = new ImplDefinition("chartRendererNone", "NONE",
            "chartRenderer.none", new int[] { DataTypes.ALPHANUMERIC, DataTypes.BINARY, DataTypes.MULTISTATE,
                    DataTypes.NUMERIC, DataTypes.IMAGE });

    static List<ImplDefinition> definitions;

    static void ensureDefinitions() {
        if (definitions == null) {
            List<ImplDefinition> d = new ArrayList<ImplDefinition>();
            d.add(noneDefinition);
            d.add(TableChartRenderer.getDefinition());
            d.add(ImageChartRenderer.getDefinition());
            d.add(StatisticsChartRenderer.getDefinition());
            // d.add(ImageFlipbookRenderer.getDefinition());
            definitions = d;
        }
    }

    public static List<ImplDefinition> getImplementations(int dataType) {
        ensureDefinitions();
        List<ImplDefinition> impls = new ArrayList<ImplDefinition>();
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
        in.readInt();
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
            if (jsonValue == null)
                return null;

            JsonObject json = jsonValue.toJsonObject();

            String type = json.getString("type");
            if (type == null)
                throw new TranslatableJsonException("emport.error.chart.missing", "type", getExportTypes());

            ImplDefinition def = null;
            ensureDefinitions();
            for (ImplDefinition id : definitions) {
                if (id.getExportName().equalsIgnoreCase(type)) {
                    def = id;
                    break;
                }
            }

            if (def == null)
                throw new TranslatableJsonException("emport.error.chart.invalid", "type", type, getExportTypes());

            Class<? extends ChartRenderer> clazz = null;
            if (def == TableChartRenderer.getDefinition())
                clazz = TableChartRenderer.class;
            else if (def == ImageChartRenderer.getDefinition())
                clazz = ImageChartRenderer.class;
            else if (def == StatisticsChartRenderer.getDefinition())
                clazz = StatisticsChartRenderer.class;

            return clazz;
        }
    }
    
    public void setTypeName(String typeName){
    	//NoOp
    }
}
