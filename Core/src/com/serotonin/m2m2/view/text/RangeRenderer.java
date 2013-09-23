/*
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.view.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.serotonin.json.spi.JsonProperty;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.rt.dataImage.types.DataValue;
import com.serotonin.m2m2.rt.dataImage.types.NumericValue;
import com.serotonin.m2m2.view.ImplDefinition;
import com.serotonin.util.SerializationHelper;

public class RangeRenderer extends BaseTextRenderer {
    private static ImplDefinition definition = new ImplDefinition("textRendererRange", "RANGE", "textRenderer.range",
            new int[] { DataTypes.NUMERIC });

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
    private String format;
    @JsonProperty
    private List<RangeValue> rangeValues = new ArrayList<RangeValue>();

    public RangeRenderer() {
        // no op
    }

    public RangeRenderer(String format) {
        this.format = format;
    }

    public void addRangeValues(double from, double to, String text, String colour) {
        rangeValues.add(new RangeValue(from, to, text, colour));
    }

    public List<RangeValue> getRangeValues() {
        return rangeValues;
    }

    public void setRangeValues(List<RangeValue> rangeValues) {
        this.rangeValues = rangeValues;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    protected String getTextImpl(DataValue value, int hint) {
        if (!(value instanceof NumericValue))
            return null;
        return getText(value.getDoubleValue(), hint);
    }

    @Override
    public String getText(double value, int hint) {
        if (hint == HINT_RAW || hint == HINT_SPECIFIC)
            return new DecimalFormat(format).format(value);

        RangeValue range = getRangeValue(value);
        if (range == null)
            return new DecimalFormat(format).format(value);
        return range.getText();
    }

    @Override
    protected String getColourImpl(DataValue value) {
        if (!(value instanceof NumericValue))
            return null;
        return getColour(value.getDoubleValue());
    }

    @Override
    public String getColour(double value) {
        RangeValue range = getRangeValue(value);
        if (range == null)
            return null;
        return range.getColour();
    }

    private RangeValue getRangeValue(double value) {
        for (RangeValue range : rangeValues) {
            if (range.contains(value)) {
                return range;
            }
        }
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
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        SerializationHelper.writeSafeUTF(out, format);
        out.writeObject(rangeValues);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be elegantly handled.
        if (ver == 1) {
            format = SerializationHelper.readSafeUTF(in);
            rangeValues = (List<RangeValue>) in.readObject();
        }
    }

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.view.text.TextRenderer#validate(com.serotonin.m2m2.i18n.ProcessResult)
	 */
	@Override
	public void validate(ProcessResult result) {
		if((format == null)||(format.equals("")))
			result.addContextualMessage("format", "validate.required");
		
		if((rangeValues == null)||(rangeValues.size() == 0))
			result.addContextualMessage("rangeValues", "validate.atLeast1");
		
		//TODO Validate the range values too
	}
    
    
    
    
    
}
