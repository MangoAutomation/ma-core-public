/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumSet;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.InvalidArgumentException;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.DataType;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.dataImage.types.BinaryValue;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.util.ColorUtils;
import com.serotonin.m2m2.view.ImplDefinition;
import com.serotonin.util.SerializationHelper;

/**
 * This class is called "binary" so that we can refer to values as 0 and 1, which is the actual representation in most
 * BA systems. However, the render method actually expects a boolean value which (arbitrarily) maps 0 to false and 1 to
 * true.
 * 
 * @author mlohbihler
 */
public class BinaryTextRenderer extends BaseTextRenderer {
    private static final ImplDefinition definition = new ImplDefinition("textRendererBinary", "BINARY",
            "textRenderer.binary", EnumSet.of(DataType.BINARY));

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
    private String zeroLabel;
    @JsonProperty
    private String zeroColour;
    @JsonProperty
    private String oneLabel;
    @JsonProperty
    private String oneColour;

    public BinaryTextRenderer() {
        // no op
    }

    public BinaryTextRenderer(String zeroValue, String zeroColour, String oneValue, String oneColour) {
        zeroLabel = zeroValue;
        this.zeroColour = zeroColour;
        oneLabel = oneValue;
        this.oneColour = oneColour;
    }

    @Override
    protected String getTextImpl(DataValue value, int hint, Locale locale) {
        if (!(value instanceof BinaryValue))
            return null;
        return getText(value.getBooleanValue(), hint, locale);
    }

    @Override
    protected String getColourImpl(DataValue value) {
        if (!(value instanceof BinaryValue))
            return null;
        return getColour(value.getBooleanValue());
    }

    @Override
    public String getColour(boolean value) {
        if (value)
            return oneColour;
        return zeroColour;
    }

    public String getOneLabel() {
        return oneLabel;
    }

    public void setOneLabel(String oneLabel) {
        this.oneLabel = oneLabel;
    }

    public String getOneColour() {
        return oneColour;
    }

    public void setOneColour(String oneColour) {
        this.oneColour = oneColour;
    }

    public String getZeroColour() {
        return zeroColour;
    }

    public void setZeroColour(String zeroColour) {
        this.zeroColour = zeroColour;
    }

    public String getZeroLabel() {
        return zeroLabel;
    }

    public void setZeroLabel(String zeroLabel) {
        this.zeroLabel = zeroLabel;
    }

    @Override
    public String getText(boolean value, int hint, Locale locale) {
        if (hint == TextRenderer.HINT_RAW)
            return value ? "1" : "0";
        if (value)
            return oneLabel;
        return zeroLabel;
    }

    @Override
    public String getChangeSnippetFilename() {
        return "changeContentRadio.jsp";
    }

    @Override
    public String getSetPointSnippetFilename() {
        return "setPointContentRadio.jsp";
    }

    @Override
    public DataValue parseText(String s, DataType dataType) {
        if (oneLabel.equalsIgnoreCase(s))
            return new BinaryValue(true);
        if (zeroLabel.equalsIgnoreCase(s))
            return new BinaryValue(false);
        return super.parseText(s, dataType);
    }

    //
    //
    // Serialization
    //
    private static final long serialVersionUID = -1;
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        SerializationHelper.writeSafeUTF(out, zeroLabel);
        SerializationHelper.writeSafeUTF(out, zeroColour);
        SerializationHelper.writeSafeUTF(out, oneLabel);
        SerializationHelper.writeSafeUTF(out, oneColour);
    }

    private void readObject(ObjectInputStream in) throws IOException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            zeroLabel = SerializationHelper.readSafeUTF(in);
            zeroColour = SerializationHelper.readSafeUTF(in);
            oneLabel = SerializationHelper.readSafeUTF(in);
            oneColour = SerializationHelper.readSafeUTF(in);
        }
    }

	@Override
	public void validate(ProcessResult result, DataType sourcePointDataType) {
	    super.validate(result, sourcePointDataType);
        try {
            if (!StringUtils.isBlank(zeroColour))
                ColorUtils.toColor(zeroColour);
        }
        catch (InvalidArgumentException e) {
            result.addContextualMessage("zeroColour", "systemSettings.validation.invalidColour");
        }
        
        try {
            if (!StringUtils.isBlank(oneColour))
                ColorUtils.toColor(oneColour);
        }
        catch (InvalidArgumentException e) {
            result.addContextualMessage("oneColour", "systemSettings.validation.invalidColour");
        }
        
		if((zeroLabel == null)||(zeroLabel.equals("")))
			result.addContextualMessage("textRenderer.zeroLabel", "validate.required");
		if((oneLabel == null)||(oneLabel.equals("")))
			result.addContextualMessage("textRenderer.oneLabel", "validate.required");
	}
}
