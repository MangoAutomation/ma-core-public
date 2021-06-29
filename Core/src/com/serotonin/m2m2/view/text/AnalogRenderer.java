/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.measure.unit.Unit;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.util.JUnitUtil;
import com.serotonin.m2m2.view.ImplDefinition;
import com.serotonin.util.SerializationHelper;

public class AnalogRenderer extends ConvertingRenderer {
    private static ImplDefinition definition = new ImplDefinition("textRendererAnalog", "ANALOG",
            "textRenderer.analog", new int[] { DataTypes.NUMERIC });

    public static ImplDefinition getDefinition() {
        return definition;
    }

    @Override
    public String getTypeName() {
        return definition.getName();
    }

    @Override
    public ImplDefinition getDef() {
        return definition;
    }

    @JsonProperty
    protected String format;
    protected String suffix;

    public AnalogRenderer() {
        super();
        setDefaults();
    }

    /**
     * @param format
     * @param suffix
     */
    public AnalogRenderer(String format, String suffix) {
        super();
        this.format = format;
        this.suffix = suffix;
    }

    /**
     * @param format
     * @param suffix
     * @param useUnitAsSuffix
     */
    public AnalogRenderer(String format, String suffix, boolean useUnitAsSuffix) {
        super();
        this.format = format;
        this.suffix = suffix;
        this.useUnitAsSuffix = useUnitAsSuffix;
    }

    @Override
    protected void setDefaults() {
        super.setDefaults();
        useUnitAsSuffix = false;
        format = "0.00";
        suffix = "";
    }

    @Override
    public String getMetaText() {
        if (useUnitAsSuffix)
            return JUnitUtil.formatLocal(renderedUnit);
        return suffix;
    }

    @Override
    protected String getTextImpl(DataValue value, int hint, Locale locale) {
        if (!(value instanceof NumericValue))
            return null;
        return getText(value.getDoubleValue(), hint, locale);
    }

    @Override
    public String getText(double value, int hint, Locale locale) {
        if ((hint & HINT_NO_CONVERT) == 0)
            value = unit.getConverterTo(renderedUnit).convert(value);
        String suffix = this.suffix;
        if (useUnitAsSuffix)
            suffix = " " + JUnitUtil.formatLocal(renderedUnit);

        String raw;
        if(format != null && (format.startsWith("0x") || (format.startsWith("0X")))){
            String[] parts = format.toUpperCase().split("0X");
            int digits = 0;
            //Count the 0s in the second part
            for(int i=0; i<parts[1].length(); i++)
                if(parts[1].charAt(i) == '0')
                    digits++;
            raw = String.format("0x%0" + digits + "x", (long)value);
        }else {
            DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
            raw = new DecimalFormat(format, symbols).format(value);
        }
        if ((hint & HINT_RAW) != 0 || suffix == null)
            return raw;
        return raw + suffix;
    }

    @Override
    protected String getColourImpl(DataValue value) {
        return null;
    }

    //
    // Parse
    @Override
    public DataValue parseText(String s, int dataType) {
        if(StringUtils.isEmpty(s))
            return new NumericValue(0);
        else if(s.startsWith("0x") || s.startsWith("0X")) {
            return new NumericValue(new BigInteger(s.substring(2), 16).longValue());
        }else
            return DataValue.stringToValue(s, dataType);
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public String getChangeSnippetFilename() {
        return "changeContentText.jsp";
    }

    @Override
    public String getSetPointSnippetFilename() {
        return "setPointContentText.jsp";
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 4;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        SerializationHelper.writeSafeUTF(out, format);
        SerializationHelper.writeSafeUTF(out, suffix);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            format = SerializationHelper.readSafeUTF(in);
            suffix = SerializationHelper.readSafeUTF(in);
        }
        else if (ver == 2) {
            format = SerializationHelper.readSafeUTF(in);
            suffix = SerializationHelper.readSafeUTF(in);
            useUnitAsSuffix = in.readBoolean();
        }
        else if (ver == 3) {
            format = SerializationHelper.readSafeUTF(in);
            suffix = SerializationHelper.readSafeUTF(in);
            useUnitAsSuffix = in.readBoolean();
            unit = (Unit<?>) in.readObject();
            renderedUnit = (Unit<?>) in.readObject();
        }else if (ver == 4){
            format = SerializationHelper.readSafeUTF(in);
            suffix = SerializationHelper.readSafeUTF(in);
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);

        if (!useUnitAsSuffix)
            writer.writeEntry("suffix", suffix);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);

        if (useUnitAsSuffix) {
            suffix = "";
        } else {
            String text = jsonObject.getString("suffix");
            if (text != null) {
                suffix = text;
            }
            else {
                suffix = "";
            }
        }
    }

    @Override
    public void validate(ProcessResult result, int sourceDataTypeId) {
        super.validate(result, sourceDataTypeId);
        if((format == null)||(format.equals("")))
            result.addContextualMessage("textRenderer.format", "validate.required");
        if (format == null) return;
        if(format.startsWith("0x") || format.startsWith("0X")) {
            String[] parts = format.toUpperCase().split("0X");
            if(parts.length != 2) {
                result.addContextualMessage("textRenderer.format", "validate.invalidValue");
                return;
            }
            //Count digits
            int digits = 0;
            //Count the 0s in the second part
            for(int i=0; i<parts[1].length(); i++)
                if(parts[1].charAt(i) == '0')
                    digits++;
                else
                    result.addContextualMessage("textRenderer.format", "validate.invalidValue");
            if(digits < 1)
                result.addContextualMessage("textRenderer.format", "validate.invalidValue");
        }
    }
}
