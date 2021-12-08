/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumSet;
import java.util.Locale;

import javax.measure.unit.Unit;

import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.util.JUnitUtil;
import com.serotonin.m2m2.view.ImplDefinition;
import com.serotonin.util.SerializationHelper;

public class PlainRenderer extends ConvertingRenderer {
    private static final ImplDefinition definition = new ImplDefinition("textRendererPlain", "PLAIN", "textRenderer.plain",
            EnumSet.of(DataType.BINARY, DataType.ALPHANUMERIC, DataType.MULTISTATE, DataType.NUMERIC));

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

    private String suffix;
    
    public PlainRenderer() {
        super();
        setDefaults();
    }
    
    /**
     */
    public PlainRenderer(String suffix) {
        super();
        this.suffix = suffix;
    }

    /**
     */
    public PlainRenderer(String suffix, boolean useUnitAsSuffix) {
        super();
        this.suffix = suffix;
        this.useUnitAsSuffix = useUnitAsSuffix;
    }
    
    @Override
    protected void setDefaults() {
    	super.setDefaults();
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
        String raw;
        String suffix = this.suffix;
        
        if (value instanceof BinaryValue) {
            if (value.getBooleanValue())
                raw = "1";
            else
            	raw = "0";
        }
        else if (value instanceof NumericValue) {
            double dblValue = value.getDoubleValue();
            if ((hint & HINT_NO_CONVERT) == 0)
                dblValue = unit.getConverterTo(renderedUnit).convert(dblValue);
            raw = Double.toString(dblValue);
            if (useUnitAsSuffix)
                suffix = " " + JUnitUtil.formatLocal(renderedUnit);
        }
        else {
            raw = value.toString();
        }

        if ((hint & HINT_RAW) != 0 || suffix == null)
            return raw;
        
        return raw + suffix;
    }

    @Override
    public String getText(double value, int hint, Locale locale) {
        if ((hint & HINT_NO_CONVERT) == 0)
            value = unit.getConverterTo(renderedUnit).convert(value);
        
        String suffix = this.suffix;
        
        if (useUnitAsSuffix)
            suffix = " " + JUnitUtil.formatLocal(renderedUnit);
        
        String raw = Double.toString(value);
        if ((hint & HINT_RAW) != 0 || suffix == null)
            return raw;
        
        return raw + suffix;
    }
    
    
    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    @Override
    protected String getColourImpl(DataValue value) {
        return null;
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
    private static final int version = 5;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        SerializationHelper.writeSafeUTF(out, suffix);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            suffix = SerializationHelper.readSafeUTF(in);
            useUnitAsSuffix = false;
        }
        else if (ver == 2) {
            suffix = SerializationHelper.readSafeUTF(in);
            useUnitAsSuffix = in.readBoolean();
        }
        else if (ver == 3) {
            suffix = SerializationHelper.readSafeUTF(in);
            useUnitAsSuffix = in.readBoolean();
            unit = (Unit<?>) in.readObject();
            renderedUnit = (Unit<?>) in.readObject();
        }else if (ver == 4){
        	suffix = SerializationHelper.readSafeUTF(in);
            useUnitAsSuffix = in.readBoolean();
            try{
            	unit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
            	unit = Unit.ONE;
            }
            try{
            	renderedUnit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
            	renderedUnit = Unit.ONE;
            }
        }else if (ver == 5){
        	suffix = SerializationHelper.readSafeUTF(in);
        }
    }
    
    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("suffix", suffix);
    }
    
    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {
        super.jsonRead(reader, jsonObject);

        String text = jsonObject.getString("suffix");
        if (text != null) {
            suffix = text;
        }
        else {
            suffix = "";
        }
        
    }

	@Override
	public void validate(ProcessResult result, DataType sourcePointDataType) {
	    super.validate(result, sourcePointDataType);
	}
}
