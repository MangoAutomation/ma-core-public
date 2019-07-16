/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.template;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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
import com.serotonin.m2m2.view.chart.ChartRenderer;
import com.serotonin.m2m2.view.text.ConvertingRenderer;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.util.ColorUtils;
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
    private String chartColour;
    private int rollup = Common.Rollups.NONE;

    private int plotType = DataPointVO.PlotTypes.STEP;
    private int simplifyType = DataPointVO.SimplifyTypes.NONE;
    private double simplifyTolerance = 10.0;
    private int simplifyTarget = 5000;
	
    /* Logging Properties */
    private int loggingType = DataPointVO.LoggingTypes.ON_CHANGE;
    @JsonProperty
    private double tolerance = 0;
    @JsonProperty
    private boolean discardExtremeValues = false;
    @JsonProperty
    private double discardLowLimit = -Double.MAX_VALUE;
    @JsonProperty
    private double discardHighLimit = Double.MAX_VALUE;
    @JsonProperty
    private boolean preventSetExtremeValues = false;
    @JsonProperty
    private double setExtremeLowLimit = -Double.MAX_VALUE;
    @JsonProperty
    private double setExtremeHighLimit = Double.MAX_VALUE;
    
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
    
	private static final long serialVersionUID = 1L;

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

	public String getChartColour() {
		return chartColour;
	}

	public void setChartColour(String chartColour) {
		this.chartColour = chartColour;
	}
	
	public int getRollup() {
		return rollup;
	}
	
	public void setRollup(int rollup) {
		this.rollup = rollup;
	}

	public int getPlotType() {
		return plotType;
	}

	public void setPlotType(int plotType) {
		this.plotType = plotType;
	}
	
	public int getSimplifyType() {
	    return simplifyType;
	}
	
	public void setSimplifyType(int simplifyType) {
	    this.simplifyType = simplifyType;
	}
	
	public double getSimplifyTolerance() {
	    return simplifyTolerance;
	}
	
	public void setSimplifyTolerance(double simplifyTolerance) {
	    this.simplifyTolerance = simplifyTolerance;
	}
	
	public int getSimplifyTarget() {
	    return simplifyTarget;
	}
	
	public void setSimplifyTarget(int simplifyTarget) {
	    this.simplifyTarget = simplifyTarget;
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
	
	public boolean isPreventSetExtremeValues() {
    	return preventSetExtremeValues;
    }
    
    public void setPreventSetExtremeValues(boolean preventSetExtremeValues) {
    	this.preventSetExtremeValues = preventSetExtremeValues;
    }
    
    public double getSetExtremeHighLimit() {
    	return setExtremeHighLimit;
    }
    
    public void setSetExtremeHighLimit(double setExtremeHighLimit) {
    	this.setExtremeHighLimit = setExtremeHighLimit;
    }
    
    public double getSetExtremeLowLimit() {
    	return setExtremeLowLimit;
    }
    
    public void setSetExtremeLowLimit(double setExtremeLowLimit) {
    	this.setExtremeLowLimit = setExtremeLowLimit;
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

        if (textRenderer == null){
            response.addContextualMessage("textRenderer", "validate.required");
        }else {
            textRenderer.validate(response, dataTypeId);
        }
        
        
        if (defaultCacheSize < 0)
            response.addContextualMessage("defaultCacheSize", "validate.cannotBeNegative");

        if (discardExtremeValues && discardHighLimit <= discardLowLimit)
            response.addContextualMessage("discardHighLimit", "validate.greaterThanDiscardLow");
        
        if(dataTypeId != DataTypes.NUMERIC && dataTypeId != DataTypes.MULTISTATE)
        	preventSetExtremeValues = false;
        
        if(preventSetExtremeValues && setExtremeHighLimit <= setExtremeLowLimit)
        	response.addContextualMessage("setExtremeHighLimit", "validate.greaterThanSetExtremeLow");

        if (!StringUtils.isBlank(chartColour)) {
            try {
                ColorUtils.toColor(chartColour);
            }
            catch (InvalidArgumentException e) {
                response.addContextualMessage("chartColour", "validate.invalidValue");
            }
        }
        
        if(!Common.ROLLUP_CODES.isValidId(rollup))
        	response.addContextualMessage("rollup", "validate.invalidValue");

        // Check text renderer type
        if (textRenderer != null) {
            textRenderer.validate(response, dataTypeId);
        }

        // Check chart renderer type
        if (chartRenderer != null && !chartRenderer.getDef().supports(dataTypeId))
            response.addGenericMessage("validate.chart.incompatible");

        // Check the plot type
        if (!DataPointVO.PLOT_TYPE_CODES.isValidId(plotType))
            response.addContextualMessage("plotType", "validate.invalidValue");
        if (plotType != DataPointVO.PlotTypes.STEP && dataTypeId != DataTypes.NUMERIC)
            response.addContextualMessage("plotType", "validate.invalidValue");

        if (overrideIntervalLoggingSamples) {
            if (intervalLoggingSampleWindowSize <= 0) {
                response.addContextualMessage("intervalLoggingSampleWindowSize", "validate.greaterThanZero");
            }
        }
        
        if (simplifyType == DataPointVO.SimplifyTypes.TARGET) {
            if (simplifyTarget < 10)
                response.addContextualMessage("simplifyTarget", "validate.greaterThan", 3);
        } else if(!DataPointVO.SIMPLIFY_TYPE_CODES.isValidId(simplifyType)) {
            response.addContextualMessage("simplifyType", "validate.invalidValue");
        } else if(simplifyType != DataPointVO.SimplifyTypes.NONE && (dataTypeId == DataTypes.ALPHANUMERIC || dataTypeId == DataTypes.IMAGE))
            response.addContextualMessage("simplifyType", "validate.cannotSimplifyType", DataTypes.getDataTypeMessage(dataTypeId));
    }
    

    //
    //
    // Serialization
    //
    private static final int version = 6;

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(version);
        out.writeBoolean(defaultTemplate);
        out.writeInt(dataTypeId);
        /* Point Properties */
        SerializationHelper.writeSafeUTF(out, chartColour);
        out.writeInt(rollup);
        out.writeInt(plotType);
        out.writeInt(simplifyType);
        out.writeDouble(simplifyTolerance);
        out.writeInt(simplifyTarget);
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
        
        out.writeBoolean(preventSetExtremeValues);
        out.writeDouble(setExtremeLowLimit);
        out.writeDouble(setExtremeHighLimit);
    }

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be
        // elegantly handled.
        if (ver == 1) {
        	/* Point Properties */
        	defaultTemplate = in.readBoolean();
        	dataTypeId = in.readInt();
        	SerializationHelper.readSafeUTF(in); //unit
        	in.readBoolean(); //useIntegralUnit
        	SerializationHelper.readSafeUTF(in); //integralUnit
        	in.readBoolean(); //useRenderedUnit
        	SerializationHelper.readSafeUTF(in); //renderedUnit
        	chartColour = SerializationHelper.readSafeUTF(in);
        	rollup = Common.Rollups.NONE;
        	plotType = in.readInt();
        	simplifyType = DataPointVO.SimplifyTypes.NONE;
        	simplifyTolerance = 10.0;
        	simplifyTarget = 5000;
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
            //The units are not used in the template so the renderer shan't care what they are
            if(textRenderer instanceof ConvertingRenderer) {
                ConvertingRenderer cr = (ConvertingRenderer) textRenderer;
                cr.setUnit(Unit.ONE);
                cr.setRenderedUnit(Unit.ONE);
            }
            chartRenderer = (ChartRenderer) in.readObject();
            preventSetExtremeValues = false;
            setExtremeLowLimit = -Double.MAX_VALUE;
            setExtremeHighLimit = Double.MAX_VALUE;
        }else if (ver == 2) {
        	/* Point Properties */
        	defaultTemplate = in.readBoolean();
        	dataTypeId = in.readInt();
        	chartColour = SerializationHelper.readSafeUTF(in);
        	rollup = Common.Rollups.NONE;
        	plotType = in.readInt();
        	simplifyType = DataPointVO.SimplifyTypes.NONE;
            simplifyTolerance = 10.0;
            simplifyTarget = 5000;
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
            //The units are not used in the template so the renderer shan't care what they are
            if(textRenderer instanceof ConvertingRenderer) {
                ConvertingRenderer cr = (ConvertingRenderer) textRenderer;
                cr.setUnit(Unit.ONE);
                cr.setRenderedUnit(Unit.ONE);
            }
            chartRenderer = (ChartRenderer) in.readObject();
            preventSetExtremeValues = false;
            setExtremeLowLimit = -Double.MAX_VALUE;
            setExtremeHighLimit = Double.MAX_VALUE;
        }else if (ver == 3) {
        	/* Point Properties */
        	defaultTemplate = in.readBoolean();
        	dataTypeId = in.readInt();
        	chartColour = SerializationHelper.readSafeUTF(in);
        	rollup = Common.Rollups.NONE;
        	plotType = in.readInt();
        	simplifyType = DataPointVO.SimplifyTypes.NONE;
            simplifyTolerance = 10.0;
            simplifyTarget = 5000;
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
            //The units are not used in the template so the renderer shan't care what they are
            //The units are not used in the template so the renderer shan't care what they are
            if(textRenderer instanceof ConvertingRenderer) {
                ConvertingRenderer cr = (ConvertingRenderer) textRenderer;
                cr.setUnit(Unit.ONE);
                cr.setRenderedUnit(Unit.ONE);
            }
            chartRenderer = (ChartRenderer) in.readObject();
            preventSetExtremeValues = in.readBoolean();
            setExtremeLowLimit = in.readDouble();
            setExtremeHighLimit = in.readDouble();
        }else if (ver == 4) {
        	/* Point Properties */
        	defaultTemplate = in.readBoolean();
        	dataTypeId = in.readInt();
        	chartColour = SerializationHelper.readSafeUTF(in);
        	rollup = in.readInt();
        	plotType = in.readInt();
        	simplifyType = DataPointVO.SimplifyTypes.NONE;
            simplifyTolerance = 10.0;
            simplifyTarget = 5000;
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
            //The units are not used in the template so the renderer shan't care what they are
            //The units are not used in the template so the renderer shan't care what they are
            if(textRenderer instanceof ConvertingRenderer) {
                ConvertingRenderer cr = (ConvertingRenderer) textRenderer;
                cr.setUnit(Unit.ONE);
                cr.setRenderedUnit(Unit.ONE);
            }
            chartRenderer = (ChartRenderer) in.readObject();
            preventSetExtremeValues = in.readBoolean();
            setExtremeLowLimit = in.readDouble();
            setExtremeHighLimit = in.readDouble();
        }else if (ver == 5) {
            /* Point Properties */
            defaultTemplate = in.readBoolean();
            dataTypeId = in.readInt();
            chartColour = SerializationHelper.readSafeUTF(in);
            rollup = in.readInt();
            plotType = in.readInt();
            simplifyType = in.readInt();
            simplifyTolerance = in.readDouble();
            simplifyTarget = 5000;
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
            //The units are not used in the template so the renderer shan't care what they are
            if(textRenderer instanceof ConvertingRenderer) {
                ConvertingRenderer cr = (ConvertingRenderer) textRenderer;
                cr.setUnit(Unit.ONE);
                cr.setRenderedUnit(Unit.ONE);
            }
            chartRenderer = (ChartRenderer) in.readObject();
            preventSetExtremeValues = in.readBoolean();
            setExtremeLowLimit = in.readDouble();
            setExtremeHighLimit = in.readDouble();
        }else if (ver == 6) {
            /* Point Properties */
            defaultTemplate = in.readBoolean();
            dataTypeId = in.readInt();
            chartColour = SerializationHelper.readSafeUTF(in);
            rollup = in.readInt();
            plotType = in.readInt();
            simplifyType = in.readInt();
            simplifyTolerance = in.readDouble();
            simplifyTarget = in.readInt();
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
            //The units are not used in the template so the renderer shan't care what they are
            if(textRenderer instanceof ConvertingRenderer) {
                ConvertingRenderer cr = (ConvertingRenderer) textRenderer;
                cr.setUnit(Unit.ONE);
                cr.setRenderedUnit(Unit.ONE);
            }
            chartRenderer = (ChartRenderer) in.readObject();
            preventSetExtremeValues = in.readBoolean();
            setExtremeLowLimit = in.readDouble();
            setExtremeHighLimit = in.readDouble();
        }
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("dataType", DataTypes.CODES.getCode(dataTypeId));
        writer.writeEntry("rollup", Common.ROLLUP_CODES.getCode(rollup));
        writer.writeEntry("loggingType", DataPointVO.LOGGING_TYPE_CODES.getCode(loggingType));
        writer.writeEntry("intervalLoggingPeriodType", Common.TIME_PERIOD_CODES.getCode(intervalLoggingPeriodType));
        writer.writeEntry("intervalLoggingType", DataPointVO.INTERVAL_LOGGING_TYPE_CODES.getCode(intervalLoggingType));
        writer.writeEntry("purgeType", Common.TIME_PERIOD_CODES.getCode(purgeType));
        writer.writeEntry("plotType", DataPointVO.PLOT_TYPE_CODES.getCode(plotType));
        writer.writeEntry("simplifyType", DataPointVO.SIMPLIFY_TYPE_CODES.getCode(simplifyType));
        if(simplifyType == DataPointVO.SimplifyTypes.TARGET)
            writer.writeEntry("simplifyTarget", simplifyTarget);
        else if(simplifyType == DataPointVO.SimplifyTypes.TOLERANCE)
            writer.writeEntry("simplifyTolerance", simplifyTolerance);
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
        
        //Rollup
        text = jsonObject.getString("rollup");
        if (text != null){
	        rollup = Common.ROLLUP_CODES.getId(text);
	        if (rollup == -1)
	            throw new TranslatableJsonException("emport.error.chart.invalid", "rollup", text,
	                    Common.ROLLUP_CODES.getCodeList());
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

        text = jsonObject.getString("plotType");
        if (text != null) {
            plotType = DataPointVO.PLOT_TYPE_CODES.getId(text);
            if (plotType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "plotType", text,
                		DataPointVO.PLOT_TYPE_CODES.getCodeList());
        }
        
        //Simplify
        text = jsonObject.getString("simplifyType");
        if (text != null){
            simplifyType = DataPointVO.SIMPLIFY_TYPE_CODES.getId(text);
            if(simplifyType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "simplifyType", text,
                        DataPointVO.SIMPLIFY_TYPE_CODES.getCodeList());
        }
        
        int simplifyTarget = jsonObject.getInt("simplifyTarget", Integer.MIN_VALUE);
        if (simplifyTarget != Integer.MIN_VALUE)
            this.simplifyTarget = simplifyTarget;
        
        double simplifyTolerance = jsonObject.getDouble("simplifyTolerance", Double.NaN);
        if (simplifyTolerance != Double.NaN)
            this.simplifyTolerance = simplifyTolerance;
    }
    
    /**
     * Update a Data Point VO to the template values
     * @param vo
     */
	public void updateDataPointVO(DataPointVO vo){
        vo.setTemplateId(this.id);
        vo.setTemplateName(this.name);
        vo.setTemplateXid(this.xid);

		/* Point Properties */
		vo.setChartColour(getChartColour());
		vo.setPlotType(getPlotType());
		vo.setRollup(getRollup());
		vo.setSimplifyType(getSimplifyType());
		vo.setSimplifyTolerance(getSimplifyTolerance());
		vo.setSimplifyTarget(getSimplifyTarget());
		
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
		
		vo.setPreventSetExtremeValues(isPreventSetExtremeValues());
		vo.setSetExtremeLowLimit(getSetExtremeLowLimit());
		vo.setSetExtremeHighLimit(getSetExtremeHighLimit());
	}
	
	/**
	 * Update this template using the Data Point's values
	 * @param vo
	 */
	public void updateTemplate(DataPointVO vo){
		
		this.setDataTypeId(vo.getPointLocator().getDataTypeId());
		
		/* Point Properties */
		this.setChartColour(vo.getChartColour());
		this.setPlotType(vo.getPlotType());
		this.setRollup(vo.getRollup());
		this.setSimplifyType(vo.getSimplifyType());
		this.setSimplifyTolerance(vo.getSimplifyTolerance());
		this.setSimplifyTarget(vo.getSimplifyTarget());
		
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
		this.setPreventSetExtremeValues(vo.isPreventSetExtremeValues());
		this.setSetExtremeLowLimit(vo.getSetExtremeLowLimit());
		this.setSetExtremeHighLimit(vo.getSetExtremeHighLimit());
	}
}
