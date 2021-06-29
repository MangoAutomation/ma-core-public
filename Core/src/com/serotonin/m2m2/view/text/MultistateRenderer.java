/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.view.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.view.ImplDefinition;

public class MultistateRenderer extends BaseTextRenderer {
    private static ImplDefinition definition = new ImplDefinition("textRendererMultistate", "MULTISTATE",
            "textRenderer.multistate", new int[] { DataTypes.MULTISTATE });

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
    private List<MultistateValue> multistateValues = new ArrayList<MultistateValue>();

    public void addMultistateValue(int key, String text, String colour) {
        multistateValues.add(new MultistateValue(key, text, colour));
    }

    public List<MultistateValue> getMultistateValues() {
        return multistateValues;
    }

    public void setMultistateValues(List<MultistateValue> multistateValues) {
        this.multistateValues = multistateValues;
    }

    @Override
    protected String getTextImpl(DataValue value, int hint, Locale locale) {
        if (!(value instanceof com.serotonin.m2m2.rt.dataImage.types.MultistateValue))
            return null;
        return getText(value.getIntegerValue(), hint, locale);
    }

    @Override
    public String getText(int value, int hint, Locale locale) {
        if (hint == HINT_RAW)
            return Integer.toString(value);

        MultistateValue mv = getMultistateValue(value);
        if (mv == null)
            return Integer.toString(value);
        return mv.getText();
    }

    @Override
    protected String getColourImpl(DataValue value) {
        if (!(value instanceof com.serotonin.m2m2.rt.dataImage.types.MultistateValue))
            return null;
        return getColour(value.getIntegerValue());
    }

    @Override
    public String getColour(int value) {
        MultistateValue mv = getMultistateValue(value);
        if (mv == null)
            return null;
        return mv.getColour();
    }

    private MultistateValue getMultistateValue(int value) {
        for (MultistateValue mv : multistateValues) {
            if (mv.getKey() == value)
                return mv;
        }
        return null;
    }

    @Override
    public String getChangeSnippetFilename() {
        return "changeContentSelect.jsp";
    }

    @Override
    public String getSetPointSnippetFilename() {
        return "setPointContentSelect.jsp";
    }

    @Override
    public DataValue parseText(String s, int dataType) {
        for (MultistateValue mv : multistateValues) {
            if (mv.getText().equalsIgnoreCase(s))
                return new com.serotonin.m2m2.rt.dataImage.types.MultistateValue(mv.getKey());
        }

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
        out.writeObject(multistateValues);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            multistateValues = (List<MultistateValue>) in.readObject();
        }
    }

	@Override
	public void validate(ProcessResult result, int sourcePointDataTypeId) {
	    super.validate(result, sourcePointDataTypeId);
		if(multistateValues == null || multistateValues.size() == 0)
			result.addContextualMessage("textRenderer.multistateValues", "validate.atLeast1");
		else {
		    int i=0;
		    for(MultistateValue value : multistateValues) {
                value.validate("multistateValues[" + i + "]", result);
                i++;
		    }
		}
	}
    
}
