/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.template;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import javax.measure.unit.Unit;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.InvalidArgumentException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.type.AuditEventType;
import com.serotonin.m2m2.util.UnitUtil;
import com.serotonin.m2m2.view.chart.ChartRenderer;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.DataPointVO.LoggingTypes;
import com.serotonin.util.ColorUtils;
import com.serotonin.util.SerializationHelper;

/**
 * @author Terry Packer
 *
 */
public class DataPointPropertiesTemplateVO extends BaseTemplateVO<DataPointPropertiesTemplateVO>{

	
    
	/* Point Locator Properties */
    @JsonProperty
    private boolean defaultTemplate = false; //Each data type gets a default template to be assigned to new points 
    private int dataTypeId  = DataTypes.NUMERIC;
    
	/* Point Properties */
    @JsonProperty
    private String unit = new String();
	
    private boolean useIntegralUnit = false;
    private String integralUnit = new String();
    
    private boolean useRenderedUnit = false;
	private String renderedUnit = new String();
	
    @JsonProperty
    private String chartColour;

    private int plotType = DataPointVO.PlotTypes.STEP;
	
    /* Logging Properties */
    private int loggingType = LoggingTypes.ON_CHANGE;
    @JsonProperty
    private double tolerance = 0;
    @JsonProperty
    private boolean discardExtremeValues = false;
    @JsonProperty
    private double discardLowLimit = -Double.MAX_VALUE;
    @JsonProperty
    private double discardHighLimit = Double.MAX_VALUE;
    
    private int intervalLoggingType = DataPointVO.IntervalLoggingTypes.INSTANT;    
    private int intervalLoggingPeriodType = Common.TimePeriods.MINUTES;
    @JsonProperty
    private int intervalLoggingPeriod = 15;
    @JsonProperty
    private boolean overrideIntervalLoggingSamples = false;
    @JsonProperty
    private int intervalLoggingSampleWindowSize;
    @JsonProperty
    private int defaultCacheSize = 1;


    /* Purge Override Settings */
    @JsonProperty
    private boolean purgeOverride = false;
    private int purgeType = Common.TimePeriods.YEARS;
    @JsonProperty
    private int purgePeriod = 1;
    
    /* Text Renderer properties */
    @JsonProperty
    private TextRenderer textRenderer;
    
    /* Chart Renderer properties */
    @JsonProperty
    private ChartRenderer chartRenderer;
    
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see com.serotonin.m2m2.util.ChangeComparable#getTypeKey()
	 */
	@Override
	public String getTypeKey() {
		return "event.audit.dataPointPropertiesTemplate";
	}
	
	public boolean isDefaultTemplate(){
		return defaultTemplate;
	}
	
	public void setDefaultTemplate(boolean defaultTemplate){
		this.defaultTemplate = defaultTemplate;
	}
	
	public int getDataTypeId() {
		return dataTypeId;
	}

	public void setDataTypeId(int dataTypeId) {
		this.dataTypeId = dataTypeId;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public boolean isUseIntegralUnit() {
		return useIntegralUnit;
	}

	public void setUseIntegralUnit(boolean useIntegralUnit) {
		this.useIntegralUnit = useIntegralUnit;
	}

	public String getIntegralUnit() {
		return integralUnit;
	}

	public void setIntegralUnit(String integralUnit) {
		this.integralUnit = integralUnit;
	}

	public boolean isUseRenderedUnit() {
		return useRenderedUnit;
	}

	public void setUseRenderedUnit(boolean useRenderedUnit) {
		this.useRenderedUnit = useRenderedUnit;
	}

	public String getRenderedUnit() {
		return renderedUnit;
	}

	public void setRenderedUnit(String renderedUnit) {
		this.renderedUnit = renderedUnit;
	}

	public String getChartColour() {
		return chartColour;
	}

	public void setChartColour(String chartColour) {
		this.chartColour = chartColour;
	}

	public int getPlotType() {
		return plotType;
	}

	public void setPlotType(int plotType) {
		this.plotType = plotType;
	}

	public int getLoggingType() {
		return loggingType;
	}

	public void setLoggingType(int loggingType) {
		this.loggingType = loggingType;
	}

	public double getTolerance() {
		return tolerance;
	}

	public void setTolerance(double tolerance) {
		this.tolerance = tolerance;
	}

	public boolean isDiscardExtremeValues() {
		return discardExtremeValues;
	}

	public void setDiscardExtremeValues(boolean discardExtremeValues) {
		this.discardExtremeValues = discardExtremeValues;
	}

	public double getDiscardLowLimit() {
		return discardLowLimit;
	}

	public void setDiscardLowLimit(double discardLowLimit) {
		this.discardLowLimit = discardLowLimit;
	}

	public double getDiscardHighLimit() {
		return discardHighLimit;
	}

	public void setDiscardHighLimit(double discardHighLimit) {
		this.discardHighLimit = discardHighLimit;
	}

	public int getIntervalLoggingType() {
		return intervalLoggingType;
	}

	public void setIntervalLoggingType(int intervalLoggingType) {
		this.intervalLoggingType = intervalLoggingType;
	}

	public int getIntervalLoggingPeriodType() {
		return intervalLoggingPeriodType;
	}

	public void setIntervalLoggingPeriodType(int intervalLoggingPeriodType) {
		this.intervalLoggingPeriodType = intervalLoggingPeriodType;
	}

	public int getIntervalLoggingPeriod() {
		return intervalLoggingPeriod;
	}

	public void setIntervalLoggingPeriod(int intervalLoggingPeriod) {
		this.intervalLoggingPeriod = intervalLoggingPeriod;
	}

	public boolean isOverrideIntervalLoggingSamples() {
		return overrideIntervalLoggingSamples;
	}

	public void setOverrideIntervalLoggingSamples(
			boolean overrideIntervalLoggingSamples) {
		this.overrideIntervalLoggingSamples = overrideIntervalLoggingSamples;
	}

	public int getIntervalLoggingSampleWindowSize() {
		return intervalLoggingSampleWindowSize;
	}

	public void setIntervalLoggingSampleWindowSize(
			int intervalLoggingSampleWindowSize) {
		this.intervalLoggingSampleWindowSize = intervalLoggingSampleWindowSize;
	}

	public int getDefaultCacheSize() {
		return defaultCacheSize;
	}

	public void setDefaultCacheSize(int defaultCacheSize) {
		this.defaultCacheSize = defaultCacheSize;
	}

	public boolean isPurgeOverride() {
		return purgeOverride;
	}

	public void setPurgeOverride(boolean purgeOverride) {
		this.purgeOverride = purgeOverride;
	}

	public int getPurgeType() {
		return purgeType;
	}

	public void setPurgeType(int purgeType) {
		this.purgeType = purgeType;
	}

	public int getPurgePeriod() {
		return purgePeriod;
	}

	public void setPurgePeriod(int purgePeriod) {
		this.purgePeriod = purgePeriod;
	}

	public TextRenderer getTextRenderer() {
		return textRenderer;
	}

	public void setTextRenderer(TextRenderer textRenderer) {
		this.textRenderer = textRenderer;
	}
	
	public ChartRenderer getChartRenderer() {
		return chartRenderer;
	}

	public void setChartRenderer(ChartRenderer chartRenderer) {
		this.chartRenderer = chartRenderer;
	}

	@Override
	public void addProperties(List<TranslatableMessage> list) {
        super.addProperties(list);
        //xid, name and enabled handled in superclass
        
        /* Point Locator Properties */
        AuditEventType.addPropertyMessage(list, "pointEdit.template.defaultTemplate", defaultTemplate);
        AuditEventType.addExportCodeMessage(list, "pointEdit.template.dataType", DataTypes.CODES, dataTypeId);

        
        /* Point Properties */
        AuditEventType.addPropertyMessage(list, "pointEdit.props.unit", unit);
        AuditEventType.addPropertyMessage(list, "pointEdit.props.useIntegralUnit", useIntegralUnit);
        AuditEventType.addPropertyMessage(list, "pointEdit.props.integralUnit", integralUnit);
        AuditEventType.addPropertyMessage(list, "pointEdit.props.useRenderedUnit", useRenderedUnit);
        AuditEventType.addPropertyMessage(list, "pointEdit.props.renderedUnit", renderedUnit);
        AuditEventType.addPropertyMessage(list, "pointEdit.props.chartColour", chartColour);
        AuditEventType.addExportCodeMessage(list, "pointEdit.plotType", DataPointVO.PLOT_TYPE_CODES, plotType);

        /* Logging Properties */
        AuditEventType.addExportCodeMessage(list, "pointEdit.logging.type", DataPointVO.LOGGING_TYPE_CODES, loggingType);
        AuditEventType.addPropertyMessage(list, "pointEdit.logging.tolerance", tolerance);
        AuditEventType.addPropertyMessage(list, "pointEdit.logging.discard", discardExtremeValues);
        AuditEventType.addPropertyMessage(list, "pointEdit.logging.discardLow", discardLowLimit);
        AuditEventType.addPropertyMessage(list, "pointEdit.logging.discardHigh", discardHighLimit);
        AuditEventType.addPeriodMessage(list, "pointEdit.logging.period", intervalLoggingPeriodType,
                intervalLoggingPeriod);
        AuditEventType.addExportCodeMessage(list, "pointEdit.logging.valueType", DataPointVO.INTERVAL_LOGGING_TYPE_CODES,
                intervalLoggingType);
        AuditEventType.addPropertyMessage(list, "pointEdit.props.overrideIntervalLoggingSamples",
                overrideIntervalLoggingSamples);
        AuditEventType.addPropertyMessage(list, "pointEdit.props.intervalLoggingSampleWindowSize",
                intervalLoggingSampleWindowSize);	        
        AuditEventType.addPropertyMessage(list, "pointEdit.logging.defaultCache", defaultCacheSize);
        
        /* Purge Override Settings */
        AuditEventType.addPropertyMessage(list, "pointEdit.logging.purgeOverride", purgeOverride);
        AuditEventType.addPeriodMessage(list, "pointEdit.logging.purge", purgeType, purgePeriod);

        // These were not in the Data Point VO method...
        /* Text Renderer properties */
        /* Chart Renderer properties */
        /* Event Detectors */

    }

    @Override
    public void addPropertyChanges(List<TranslatableMessage> list, BaseTemplateVO<?> fromVo) {
        super.addPropertyChanges(list, fromVo);
        //template type, xid, name and enabled handled in superclasses
        DataPointPropertiesTemplateVO from = (DataPointPropertiesTemplateVO)fromVo;
        
        /* Point Locator Properties */
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.template.defaultTemplate", from.defaultTemplate, defaultTemplate);
        AuditEventType.maybeAddExportCodeChangeMessage(list, "pointEdit.plotType", DataTypes.CODES, from.dataTypeId, dataTypeId);

        
        /* Point Properties */
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.props.unit", from.unit,
                unit);
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.props.useIntegralUnit", from.useIntegralUnit, useIntegralUnit);
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.props.integralUnit", from.integralUnit, integralUnit);
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.props.useRenderedUnit", from.useRenderedUnit, useRenderedUnit);
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.props.renderedUnit", from.renderedUnit, renderedUnit);
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.props.chartColour", from.chartColour, chartColour);
        AuditEventType.maybeAddExportCodeChangeMessage(list, "pointEdit.plotType", DataPointVO.PLOT_TYPE_CODES, from.plotType,plotType);

        /* Logging Properties */
        AuditEventType.maybeAddExportCodeChangeMessage(list, "pointEdit.logging.type", DataPointVO.LOGGING_TYPE_CODES,
                from.loggingType, loggingType);
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.logging.tolerance", from.tolerance, tolerance);
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.logging.discard", from.discardExtremeValues,
                discardExtremeValues);
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.logging.discardLow", from.discardLowLimit,
                discardLowLimit);
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.logging.discardHigh", from.discardHighLimit,
                discardHighLimit);
        AuditEventType.maybeAddPeriodChangeMessage(list, "pointEdit.logging.period", from.intervalLoggingPeriodType,
                from.intervalLoggingPeriod, intervalLoggingPeriodType, intervalLoggingPeriod);
        AuditEventType.maybeAddExportCodeChangeMessage(list, "pointEdit.logging.valueType",
        		DataPointVO.INTERVAL_LOGGING_TYPE_CODES, from.intervalLoggingType, intervalLoggingType);
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.props.overrideIntervalLoggingSamples",
                from.overrideIntervalLoggingSamples, overrideIntervalLoggingSamples);

        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.props.intervalLoggingSampleWindowSize",
                from.intervalLoggingSampleWindowSize, intervalLoggingSampleWindowSize);
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.logging.defaultCache", from.defaultCacheSize,
                defaultCacheSize);

        /* Purge Override Settings */
        AuditEventType.maybeAddPropertyChangeMessage(list, "pointEdit.logging.purgeOverride", from.purgeOverride,
                purgeOverride);
        AuditEventType.maybeAddPeriodChangeMessage(list, "pointEdit.logging.purge", from.purgeType, from.purgePeriod,
                purgeType, purgePeriod);
        
        // These were not in the Data Point VO method...
        /* Text Renderer properties */
        /* Chart Renderer properties */
        /* Event Detectors */

    }
    
    @Override
    public void validate(ProcessResult response) {
        super.validate(response);
        //template type, xid,name in superclass
        if(defaultTemplate){
            //TODO Could check to see if there is already a default template for this data type?
        }
        
        if (!DataTypes.CODES.isValidId(dataTypeId))
            response.addContextualMessage("dataTypeId", "validate.invalidValue");
        

        if (!DataPointVO.LOGGING_TYPE_CODES.isValidId(loggingType))
            response.addContextualMessage("loggingType", "validate.invalidValue");
        if (loggingType == DataPointVO.LoggingTypes.ON_CHANGE && dataTypeId == DataTypes.NUMERIC) {
            if (tolerance < 0)
                response.addContextualMessage("tolerance", "validate.cannotBeNegative");
        }

        if (!Common.TIME_PERIOD_CODES.isValidId(intervalLoggingPeriodType))
            response.addContextualMessage("intervalLoggingPeriodType", "validate.invalidValue");
        if (intervalLoggingPeriod <= 0)
            response.addContextualMessage("intervalLoggingPeriod", "validate.greaterThanZero");
        if (!DataPointVO.INTERVAL_LOGGING_TYPE_CODES.isValidId(intervalLoggingType))
            response.addContextualMessage("intervalLoggingType", "validate.invalidValue");

        if (purgeOverride) {
            if (!Common.TIME_PERIOD_CODES.isValidId(purgeType))
                response.addContextualMessage("purgeType", "validate.invalidValue");
            if (purgePeriod <= 0)
                response.addContextualMessage("purgePeriod", "validate.greaterThanZero");
        }

        if (textRenderer == null)
            response.addContextualMessage("textRenderer", "validate.required");

        if (defaultCacheSize < 0)
            response.addContextualMessage("defaultCacheSize", "validate.cannotBeNegative");

        if (discardExtremeValues && discardHighLimit <= discardLowLimit)
            response.addContextualMessage("discardHighLimit", "validate.greaterThanDiscardLow");

        if (!StringUtils.isBlank(chartColour)) {
            try {
                ColorUtils.toColor(chartColour);
            }
            catch (InvalidArgumentException e) {
                response.addContextualMessage("chartColour", "validate.invalidValue");
            }
        }

        // Check text renderer type
        if (textRenderer != null && !textRenderer.getDef().supports(dataTypeId))
            response.addGenericMessage("validate.text.incompatible");

        // Check chart renderer type
        if (chartRenderer != null && !chartRenderer.getDef().supports(dataTypeId))
            response.addGenericMessage("validate.chart.incompatible");

        // Check the plot type
        if (!DataPointVO.PLOT_TYPE_CODES.isValidId(plotType))
            response.addContextualMessage("plotType", "validate.invalidValue");
        if (plotType != DataPointVO.PlotTypes.STEP && dataTypeId != DataTypes.NUMERIC)
            response.addContextualMessage("plotType", "validate.invalidValue");

        //Validate the unit
        try {
            UnitUtil.parseLocal(this.unit);
        }
        catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                response.addContextualMessage("unit", "validate.unitInvalid", ((IllegalArgumentException) e).getCause()
                        .getMessage());
            }
            else {
                response.addContextualMessage("unit", "validate.unitInvalid", e.getMessage());
            }

        }

        try {
            if (!validateIntegralUnit()) {
                response.addContextualMessage("integralUnit", "validate.unitNotCompatible");
            }
        }
        catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                response.addContextualMessage("integralUnit", "validate.unitInvalid", ((IllegalArgumentException) e)
                        .getCause().getMessage());
            }
            else {
                response.addContextualMessage("integralUnit", "validate.unitInvalid", e.getMessage());
            }
        }

        try {
            if (!validateRenderedUnit()) {
                response.addContextualMessage("renderedUnit", "validate.unitNotCompatible");
            }
        }
        catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                response.addContextualMessage("renderedUnit", "validate.unitInvalid", ((IllegalArgumentException) e)
                        .getCause().getMessage());
            }
            else {
                response.addContextualMessage("renderedUnit", "validate.unitInvalid", e.getMessage());
            }
        }

        if (overrideIntervalLoggingSamples) {
            if (intervalLoggingSampleWindowSize <= 0) {
                response.addContextualMessage("intervalLoggingSampleWindowSize", "validate.greaterThanZero");
            }
        }
    }
    
    /**
     * Validate the Integral Unit
     * Setting a default if its not enabled
     * 
     * @return
     */
    public boolean validateIntegralUnit() {
        if (!useIntegralUnit) {
            return true;
        }

        // integral unit should have same dimensions as the default integrated unit
        if (integralUnit == null)
            return false;
        
        Unit<?> iUnit = UnitUtil.parseLocal(integralUnit);
        return iUnit.isCompatible(UnitUtil.parseLocal(unit));
    }

    /**
     * Validate the Rendered Unit against the unit
     * @return
     */
    public boolean validateRenderedUnit() {
        if (!useRenderedUnit) {
            return true;
        }

        if (renderedUnit == null)
            return false;

        Unit<?> rUnit = UnitUtil.parseLocal(renderedUnit);
        return rUnit.isCompatible(UnitUtil.parseLocal(unit));
    }
    //
    //
    // Serialization
    //
    private static final int version = 1;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeBoolean(defaultTemplate);
        out.writeInt(dataTypeId);
        /* Point Properties */
        SerializationHelper.writeSafeUTF(out, unit);
        out.writeBoolean(useIntegralUnit);
        SerializationHelper.writeSafeUTF(out, integralUnit);
        out.writeBoolean(useRenderedUnit);
        SerializationHelper.writeSafeUTF(out, renderedUnit);
        SerializationHelper.writeSafeUTF(out, chartColour);
        out.writeInt(plotType);
        /* Logging Properties */
        out.writeInt(loggingType);
        out.writeDouble(tolerance);
        out.writeBoolean(discardExtremeValues);
        out.writeDouble(discardLowLimit);
        out.writeDouble(discardHighLimit);
        out.writeInt(intervalLoggingType);
        out.writeInt(intervalLoggingPeriodType);
        out.writeInt(intervalLoggingPeriod);
        out.writeBoolean(overrideIntervalLoggingSamples);
        out.writeInt(intervalLoggingSampleWindowSize);
        out.writeInt(defaultCacheSize);
        /* Purge Override Settings */
        out.writeBoolean(purgeOverride);
        out.writeInt(purgeType);
        out.writeInt(purgePeriod);
        
        out.writeObject(textRenderer);
        out.writeObject(chartRenderer);
    }

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be
        // elegantly handled.
        if (ver == 1) {
        	/* Point Properties */
        	defaultTemplate = in.readBoolean();
        	dataTypeId = in.readInt();
        	unit = SerializationHelper.readSafeUTF(in);
        	useIntegralUnit = in.readBoolean();
        	integralUnit = SerializationHelper.readSafeUTF(in);
        	useRenderedUnit = in.readBoolean();
        	renderedUnit = SerializationHelper.readSafeUTF(in);
        	chartColour = SerializationHelper.readSafeUTF(in);
        	plotType = in.readInt();
        	/* Logging Properties */
        	loggingType = in.readInt();
        	tolerance = in.readDouble();
        	discardExtremeValues = in.readBoolean();
        	discardLowLimit = in.readDouble();
        	discardHighLimit = in.readDouble();
        	intervalLoggingType = in.readInt();
        	intervalLoggingPeriodType = in.readInt();
        	intervalLoggingPeriod = in.readInt();
        	overrideIntervalLoggingSamples = in.readBoolean();
        	intervalLoggingSampleWindowSize = in.readInt();
        	defaultCacheSize = in.readInt();
        	/* Purge Override Settings */
        	purgeOverride = in.readBoolean();
        	purgeType = in.readInt();
        	purgePeriod = in.readInt();
        	
            textRenderer = (TextRenderer) in.readObject();
            chartRenderer = (ChartRenderer) in.readObject();
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("dataType", DataTypes.CODES.getCode(dataTypeId));
        
        writer.writeEntry("loggingType", DataPointVO.LOGGING_TYPE_CODES.getCode(loggingType));
        writer.writeEntry("intervalLoggingPeriodType", Common.TIME_PERIOD_CODES.getCode(intervalLoggingPeriodType));
        writer.writeEntry("intervalLoggingType", DataPointVO.INTERVAL_LOGGING_TYPE_CODES.getCode(intervalLoggingType));
        writer.writeEntry("purgeType", Common.TIME_PERIOD_CODES.getCode(purgeType));
        writer.writeEntry("plotType", DataPointVO.PLOT_TYPE_CODES.getCode(plotType));

        writer.writeEntry("unit", unit);

        if (useIntegralUnit)
            writer.writeEntry("integralUnit", integralUnit);
        if (useRenderedUnit)
            writer.writeEntry("renderedUnit", renderedUnit);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {

        //Not reading XID so can't do this: super.jsonRead(reader, jsonObject);
        name = jsonObject.getString("name");

        //Not reading XID so can't do this: super.jsonRead(reader, jsonObject);
        String text = jsonObject.getString("templateType");
        if (text != null) {
            dataTypeId = TEMPLATE_TYPE_CODES.getId(text);
            if (dataTypeId == -1)
                throw new TranslatableJsonException("emport.error.invalid", "templateType", text,
                        TEMPLATE_TYPE_CODES.getCodeList());
        }
        
        text = jsonObject.getString("dataType");
        if (text != null) {
            dataTypeId = DataTypes.CODES.getId(text);
            if (dataTypeId == -1)
                throw new TranslatableJsonException("emport.error.invalid", "dataType", text,
                        DataTypes.CODES.getCodeList());
        }

        text = jsonObject.getString("loggingType");
        if (text != null) {
            loggingType = DataPointVO.LOGGING_TYPE_CODES.getId(text);
            if (loggingType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "loggingType", text,
                		DataPointVO.LOGGING_TYPE_CODES.getCodeList());
        }

        text = jsonObject.getString("intervalLoggingPeriodType");
        if (text != null) {
            intervalLoggingPeriodType = Common.TIME_PERIOD_CODES.getId(text);
            if (intervalLoggingPeriodType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "intervalLoggingPeriodType", text,
                        Common.TIME_PERIOD_CODES.getCodeList());
        }

        text = jsonObject.getString("intervalLoggingType");
        if (text != null) {
            intervalLoggingType = DataPointVO.INTERVAL_LOGGING_TYPE_CODES.getId(text);
            if (intervalLoggingType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "intervalLoggingType", text,
                		DataPointVO.INTERVAL_LOGGING_TYPE_CODES.getCodeList());
        }

        text = jsonObject.getString("purgeType");
        if (text != null) {
            purgeType = Common.TIME_PERIOD_CODES.getId(text);
            if (purgeType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "purgeType", text,
                        Common.TIME_PERIOD_CODES.getCodeList());
        }

        unit = jsonObject.getString("unit");

        integralUnit = jsonObject.getString("integralUnit");
        if(integralUnit != null)
        	useIntegralUnit = true;
        else
        	useIntegralUnit = false;

        renderedUnit = jsonObject.getString("renderedUnit");
        if (renderedUnit != null) 
            useRenderedUnit = true;
        else
        	useRenderedUnit = false;

        text = jsonObject.getString("plotType");
        if (text != null) {
            plotType = DataPointVO.PLOT_TYPE_CODES.getId(text);
            if (plotType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "plotType", text,
                		DataPointVO.PLOT_TYPE_CODES.getCodeList());
        }
    }
    
    /**
     * Update a Data Point VO to the template values
     * @param vo
     */
	public void updateDataPointVO(DataPointVO vo){
		
		/* Point Properties */
		if(unit == null)
			vo.setUnit(UnitUtil.parseLocal(new String()));
		else
			vo.setUnit(UnitUtil.parseLocal(getUnit()));
			
		vo.setUseIntegralUnit(isUseIntegralUnit());
		if(integralUnit == null)
			vo.setIntegralUnit(UnitUtil.parseLocal(new String()));
		else
			vo.setIntegralUnit(UnitUtil.parseLocal(getIntegralUnit()));
			
		vo.setUseRenderedUnit(isUseRenderedUnit());
		if(renderedUnit == null)
			vo.setRenderedUnit(UnitUtil.parseLocal(new String()));
		else
			vo.setRenderedUnit(UnitUtil.parseLocal(getRenderedUnit()));
			
		vo.setChartColour(getChartColour());
		vo.setPlotType(getPlotType());
		
		/* Logging Properties */
		vo.setLoggingType(getLoggingType());
		vo.setTolerance(getTolerance());
		vo.setDiscardExtremeValues(isDiscardExtremeValues());
		vo.setDiscardLowLimit(getDiscardLowLimit());
		vo.setDiscardHighLimit(getDiscardHighLimit());
		vo.setIntervalLoggingType(getIntervalLoggingType());
		vo.setIntervalLoggingPeriodType(getIntervalLoggingPeriodType());
		vo.setIntervalLoggingPeriod(getIntervalLoggingPeriod());
		vo.setIntervalLoggingSampleWindowSize(getIntervalLoggingSampleWindowSize());
		vo.setDefaultCacheSize(getDefaultCacheSize());
		
		/* Purge Override Settings */
		vo.setPurgeOverride(isPurgeOverride());
		vo.setPurgeType(getPurgeType());
		vo.setPurgePeriod(getPurgePeriod());
		
		/* Text Renderer */
		vo.setTextRenderer(getTextRenderer());
		
		/* Chart Renderer */
		vo.setChartRenderer(getChartRenderer());
		
		vo.setTemplateId(getId());
		
	}
	
	/**
	 * Update this template using the Data Point's values
	 * @param vo
	 */
	public void updateTemplate(DataPointVO vo){
		
		this.setDataTypeId(vo.getPointLocator().getDataTypeId());
		
		/* Point Properties */
		this.setUnit(UnitUtil.formatLocal(vo.getUnit()));
		this.setUseIntegralUnit(vo.isUseIntegralUnit());
		this.setIntegralUnit(UnitUtil.formatLocal(vo.getIntegralUnit()));
		this.setUseRenderedUnit(vo.isUseRenderedUnit());
		this.setRenderedUnit(UnitUtil.formatLocal(vo.getRenderedUnit()));
		this.setChartColour(vo.getChartColour());
		this.setPlotType(vo.getPlotType());
		
		/* Logging Properties */
		this.setLoggingType(vo.getLoggingType());
		this.setTolerance(vo.getTolerance());
		this.setDiscardExtremeValues(vo.isDiscardExtremeValues());
		this.setDiscardLowLimit(vo.getDiscardLowLimit());
		this.setDiscardHighLimit(vo.getDiscardHighLimit());
		this.setIntervalLoggingType(vo.getIntervalLoggingType());
		this.setIntervalLoggingPeriodType(vo.getIntervalLoggingPeriodType());
		this.setIntervalLoggingPeriod(vo.getIntervalLoggingPeriod());
		this.setIntervalLoggingSampleWindowSize(vo.getIntervalLoggingSampleWindowSize());
		this.setDefaultCacheSize(vo.getDefaultCacheSize());
		
		/* Purge Override Settings */
		this.setPurgeOverride(vo.isPurgeOverride());
		this.setPurgeType(vo.getPurgeType());
		this.setPurgePeriod(vo.getPurgePeriod());
		
		/* Text Renderer */
		this.setTextRenderer(vo.getTextRenderer());
		
		/* Chart Renderer */
		this.setChartRenderer(vo.getChartRenderer());
	}
}
