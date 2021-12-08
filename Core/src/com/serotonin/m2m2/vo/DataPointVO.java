/*
 * Copyright (C) 2021 Radix IoT LLC. All rights reserved.
 */
package com.serotonin.m2m2.vo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.measure.converter.UnitConverter;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.infiniteautomation.mango.permission.MangoPermission;
import com.infiniteautomation.mango.util.LazyField;
import com.serotonin.ShouldNeverHappenException;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonReader;
import com.serotonin.json.ObjectWriter;
import com.serotonin.json.spi.JsonProperty;
import com.serotonin.json.type.JsonObject;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.db.dao.DataPointTagsDao;
import com.serotonin.m2m2.db.dao.EventDetectorDao;
import com.serotonin.m2m2.i18n.TranslatableJsonException;
import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.util.ExportCodes;
import com.serotonin.m2m2.util.JUnitUtil;
import com.serotonin.m2m2.view.text.AnalogRenderer;
import com.serotonin.m2m2.view.text.ConvertingRenderer;
import com.serotonin.m2m2.view.text.NoneRenderer;
import com.serotonin.m2m2.view.text.PlainRenderer;
import com.serotonin.m2m2.view.text.TextRenderer;
import com.serotonin.m2m2.vo.dataSource.PointLocatorVO;
import com.serotonin.util.SerializationHelper;

public class DataPointVO extends AbstractActionVO implements IDataPoint {
    private static final long serialVersionUID = -1;
    public static final String XID_PREFIX = "DP_";

    public interface LoggingTypes {
        int ON_CHANGE = 1;
        int ALL = 2;
        int NONE = 3;
        int INTERVAL = 4;
        int ON_TS_CHANGE = 5;
        int ON_CHANGE_INTERVAL = 6;
    }

    public static final ExportCodes LOGGING_TYPE_CODES = new ExportCodes();
    static {
        LOGGING_TYPE_CODES.addElement(LoggingTypes.ON_CHANGE, "ON_CHANGE", "pointEdit.logging.type.change");
        LOGGING_TYPE_CODES.addElement(LoggingTypes.ALL, "ALL", "pointEdit.logging.type.all");
        LOGGING_TYPE_CODES.addElement(LoggingTypes.NONE, "NONE", "pointEdit.logging.type.never");
        LOGGING_TYPE_CODES.addElement(LoggingTypes.INTERVAL, "INTERVAL", "pointEdit.logging.type.interval");
        LOGGING_TYPE_CODES.addElement(LoggingTypes.ON_TS_CHANGE, "ON_TS_CHANGE", "pointEdit.logging.type.tsChange");
        LOGGING_TYPE_CODES.addElement(LoggingTypes.ON_CHANGE_INTERVAL, "ON_CHANGE_INTERVAL", "pointEdit.logging.type.changeInterval");
    }

    public interface PurgeTypes {
        int DAYS = Common.TimePeriods.DAYS;
        int WEEKS = Common.TimePeriods.WEEKS;
        int MONTHS = Common.TimePeriods.MONTHS;
        int YEARS = Common.TimePeriods.YEARS;
    }

    public interface IntervalLoggingTypes {
        int INSTANT = 1;
        int MAXIMUM = 2;
        int MINIMUM = 3;
        int AVERAGE = 4;
    }

    public static final ExportCodes INTERVAL_LOGGING_TYPE_CODES = new ExportCodes();
    static {
        INTERVAL_LOGGING_TYPE_CODES.addElement(IntervalLoggingTypes.INSTANT, "INSTANT",
                "pointEdit.logging.valueType.instant");
        INTERVAL_LOGGING_TYPE_CODES.addElement(IntervalLoggingTypes.MAXIMUM, "MAXIMUM",
                "pointEdit.logging.valueType.maximum");
        INTERVAL_LOGGING_TYPE_CODES.addElement(IntervalLoggingTypes.MINIMUM, "MINIMUM",
                "pointEdit.logging.valueType.minimum");
        INTERVAL_LOGGING_TYPE_CODES.addElement(IntervalLoggingTypes.AVERAGE, "AVERAGE",
                "pointEdit.logging.valueType.average");
    }

    public interface PlotTypes {
        int STEP = 1;
        int LINE = 2;
        int SPLINE = 3;
        int BAR = 4;
    }

    public static final ExportCodes PLOT_TYPE_CODES = new ExportCodes();
    static {
        PLOT_TYPE_CODES.addElement(PlotTypes.STEP, "STEP", "pointEdit.plotType.step");
        PLOT_TYPE_CODES.addElement(PlotTypes.LINE, "LINE", "pointEdit.plotType.line");
        PLOT_TYPE_CODES.addElement(PlotTypes.SPLINE, "SPLINE", "pointEdit.plotType.spline");
        PLOT_TYPE_CODES.addElement(PlotTypes.BAR, "BAR", "pointEdit.plotType.bar");
    }

    public interface SimplifyTypes {
        int NONE = 1;
        int TARGET = 2;
        int TOLERANCE = 3;
    }
    public static final ExportCodes SIMPLIFY_TYPE_CODES = new ExportCodes();
    static {
        SIMPLIFY_TYPE_CODES.addElement(SimplifyTypes.NONE, "NONE", "pointEdit.simplify.none");
        SIMPLIFY_TYPE_CODES.addElement(SimplifyTypes.TARGET, "TARGET", "pointEdit.simplify.target");
        SIMPLIFY_TYPE_CODES.addElement(SimplifyTypes.TOLERANCE, "TOLERANCE", "pointEdit.simplify.tolerance");
    }

    public TranslatableMessage getDataTypeMessage() {
        return pointLocator.getDataTypeMessage();
    }

    public TranslatableMessage getConfigurationDescription() {
        return pointLocator.getConfigurationDescription();
    }

    //
    //
    // Properties
    // id,xid,name and enabled are now in superclasses

    private int dataSourceId;
    @JsonProperty
    private String deviceName;

    private int loggingType = LoggingTypes.ON_CHANGE;
    private int intervalLoggingPeriodType = Common.TimePeriods.MINUTES;
    @JsonProperty
    private int intervalLoggingPeriod = 15;
    private int intervalLoggingType = IntervalLoggingTypes.INSTANT;
    @JsonProperty
    private double tolerance = 0;
    @JsonProperty
    private boolean purgeOverride = false;
    private int purgeType = Common.TimePeriods.YEARS;
    @JsonProperty
    private int purgePeriod = 1;
    @JsonProperty
    private TextRenderer textRenderer;
    @JsonProperty
    private int defaultCacheSize = 1;
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
    /**
     * @deprecated Use unit instead
     */
    @Deprecated
    private int engineeringUnits = com.serotonin.m2m2.util.EngineeringUnits.noUnits;
    // Replaces Engineering Units
    Unit<?> unit = defaultUnit();
    // replaces integralEngUnits
    Unit<?> integralUnit = defaultIntegralUnit();
    // unit used for rendering if the renderer supports it
    Unit<?> renderedUnit = defaultUnit();

    boolean useIntegralUnit = false;
    boolean useRenderedUnit = false;

    @JsonProperty
    private String chartColour = "";
    private int rollup = Common.Rollups.NONE;

    private int plotType = PlotTypes.STEP;

    // Properties for Simplify
    private int simplifyType = SimplifyTypes.NONE;
    private double simplifyTolerance = 10.0;
    private int simplifyTarget = 5000;

    private PointLocatorVO<?> pointLocator;

    @JsonProperty
    private boolean overrideIntervalLoggingSamples = false;
    @JsonProperty
    private int intervalLoggingSampleWindowSize;
    @JsonProperty
    private LazyField<MangoPermission> readPermission = new LazyField<>(new MangoPermission());
    @JsonProperty
    private LazyField<MangoPermission> editPermission = new LazyField<>(new MangoPermission());
    @JsonProperty
    private LazyField<MangoPermission> setPermission = new LazyField<>(new MangoPermission());
    @JsonProperty
    private JsonNode data;

    private int seriesId = Common.NEW_ID;

    //
    //
    // Convenience data from data source
    //
    private String dataSourceTypeName;
    private String dataSourceName;

    //
    //
    // Required for importing
    //
    @JsonProperty
    private String dataSourceXid;

    /**
     * Defaults to null to indicate that the relational data has not been loaded
     */
    @JsonProperty
    private LazyField<Map<String, String>> tags = new LazyField<>(new HashMap<>());

    public DataPointVO() { }

    /**
     * Sets the text renderer to the default text renderer for the point locator data type
     */
    public void defaultTextRenderer() {
        if (pointLocator == null)
            setTextRenderer(new PlainRenderer("", false));
        else {
            switch (pointLocator.getDataType()) {
                case IMAGE:
                    setTextRenderer(new NoneRenderer());
                    break;
                case NUMERIC:
                    setTextRenderer(new PlainRenderer("", true));
                    break;
                default:
                    setTextRenderer(new PlainRenderer("", false));
            }
        }
    }

    @Override
    public String getTypeKey() {
        return "event.audit.dataPoint";
    }

    @Override
    public int getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(int dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    @Override
    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    @SuppressWarnings("unchecked")
    public <T extends PointLocatorVO<?>> T getPointLocator() {
        return (T) pointLocator;
    }

    public void setPointLocator(PointLocatorVO<?> pointLocator) {
        this.pointLocator = pointLocator;
        if (this.textRenderer == null) {
            this.defaultTextRenderer();
        }
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
        if (deviceName == null)
            deviceName = dataSourceName;
    }

    public String getDataSourceXid() {
        return dataSourceXid;
    }

    public void setDataSourceXid(String dataSourceXid) {
        this.dataSourceXid = dataSourceXid;
    }

    public String getDataSourceTypeName() {
        return dataSourceTypeName;
    }

    public void setDataSourceTypeName(String dataSourceTypeName) {
        this.dataSourceTypeName = dataSourceTypeName;
    }

    public int getLoggingType() {
        return loggingType;
    }

    public void setLoggingType(int loggingType) {
        this.loggingType = loggingType;
    }

    public boolean isPurgeOverride() {
        return purgeOverride;
    }

    public void setPurgeOverride(boolean purgeOverride) {
        this.purgeOverride = purgeOverride;
    }

    public int getPurgePeriod() {
        return purgePeriod;
    }

    public void setPurgePeriod(int purgePeriod) {
        this.purgePeriod = purgePeriod;
    }

    public int getPurgeType() {
        return purgeType;
    }

    public void setPurgeType(int purgeType) {
        this.purgeType = purgeType;
    }

    public double getTolerance() {
        return tolerance;
    }

    public void setTolerance(double tolerance) {
        this.tolerance = tolerance;
    }

    public TextRenderer getTextRenderer() {
        return textRenderer;
    }

    public void setTextRenderer(TextRenderer textRenderer) {
        this.textRenderer = textRenderer;
        setUnitsOnTextRenderer();
    }

    public TextRenderer createIntegralRenderer() {
        AnalogRenderer renderer = new AnalogRenderer();

        renderer.setUnit(defaultIntegralUnit());
        if (useIntegralUnit)
            renderer.setRenderedUnit(integralUnit);
        else
            renderer.setRenderedUnit(defaultIntegralUnit());

        renderer.setUseUnitAsSuffix(true);
        renderer.setFormat("0.0");
        return renderer;
    }

    public int getDefaultCacheSize() {
        return defaultCacheSize;
    }

    public void setDefaultCacheSize(int defaultCacheSize) {
        this.defaultCacheSize = defaultCacheSize;
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

    public int getIntervalLoggingType() {
        return intervalLoggingType;
    }

    public void setIntervalLoggingType(int intervalLoggingType) {
        this.intervalLoggingType = intervalLoggingType;
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

    /**
     * @deprecated
     *             Use getUnit() instead
     */
    @Deprecated
    public int getEngineeringUnits() {
        return engineeringUnits;
    }

    /**
     * @deprecated
     *             Use setUnit() instead
     */
    @Deprecated
    public void setEngineeringUnits(int engineeringUnits) {
        this.engineeringUnits = engineeringUnits;
    }

    public Unit<?> getIntegralUnit() {
        return integralUnit;
    }

    public void setIntegralUnit(Unit<?> integralUnit) {
        this.integralUnit = integralUnit;
    }

    public Unit<?> getUnit() {
        return unit;
    }

    public void setUnit(Unit<?> unit) {
        this.unit = unit;
    }

    public Unit<?> getRenderedUnit() {
        return renderedUnit;
    }

    public void setRenderedUnit(Unit<?> renderedUnit) {
        this.renderedUnit = renderedUnit;
    }

    public boolean isUseIntegralUnit() {
        return useIntegralUnit;
    }

    public void setUseIntegralUnit(boolean useIntegralUnit) {
        this.useIntegralUnit = useIntegralUnit;
    }

    public boolean isUseRenderedUnit() {
        return useRenderedUnit;
    }

    public void setUseRenderedUnit(boolean useRenderedUnit) {
        this.useRenderedUnit = useRenderedUnit;
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

    public boolean isSimplifyDataSets() {
        return simplifyType != SimplifyTypes.NONE;
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

    public boolean isOverrideIntervalLoggingSamples() {
        return overrideIntervalLoggingSamples;
    }

    public void setOverrideIntervalLoggingSamples(boolean overrideIntervalLoggingSamples) {
        this.overrideIntervalLoggingSamples = overrideIntervalLoggingSamples;
    }

    public int getIntervalLoggingSampleWindowSize() {
        return intervalLoggingSampleWindowSize;
    }

    public void setIntervalLoggingSampleWindowSize(int intervalLoggingSampleWindowSize) {
        this.intervalLoggingSampleWindowSize = intervalLoggingSampleWindowSize;
    }

    @Override
    public MangoPermission getReadPermission() {
        return readPermission.get();
    }

    public void setReadPermission(MangoPermission readPermission) {
        this.readPermission.set(readPermission);
    }

    public void supplyReadPermission(Supplier<MangoPermission> readPermission) {
        this.readPermission = new LazyField<MangoPermission>(readPermission);
    }

    @Override
    public MangoPermission getEditPermission() {
        return editPermission.get();
    }

    public void setEditPermission(MangoPermission editPermission) {
        this.editPermission.set(editPermission);
    }

    public void supplyEditPermission(Supplier<MangoPermission> editPermission) {
        this.editPermission = new LazyField<MangoPermission>(editPermission);
    }

    @Override
    public MangoPermission getSetPermission() {
        return setPermission.get();
    }

    public void setSetPermission(MangoPermission setPermission) {
        this.setPermission.set(setPermission);
    }

    public void supplySetPermission(Supplier<MangoPermission> setPermission) {
        this.setPermission = new LazyField<MangoPermission>(setPermission);
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }

    /**
     * Get the current series Id for our data.  Common.NEW_ID means
     *  generate new series ID on insert of this point
     */
    public int getSeriesId() {
        return seriesId;
    }

    /**
     * Get the series id for where we store our data
     */
    public void setSeriesId(int seriesId) {
        this.seriesId = seriesId;
    }

    /* ############################## */
    @Override
    public DataPointVO copy() {
        try {
            DataPointVO copy = (DataPointVO) super.clone();

            // is all of this necessary after we made a clone?
            copy.setChartColour(chartColour);
            copy.setRollup(rollup);
            copy.setDataSourceId(dataSourceId);
            copy.setDataSourceName(dataSourceName);
            copy.setDataSourceTypeName(dataSourceTypeName);
            copy.setDataSourceXid(dataSourceXid);
            copy.setDefaultCacheSize(defaultCacheSize);
            copy.setDeviceName(deviceName);
            copy.setDiscardExtremeValues(discardExtremeValues);
            copy.setDiscardHighLimit(discardHighLimit);
            copy.setDiscardLowLimit(discardLowLimit);
            copy.setEnabled(enabled);
            copy.setIntegralUnit(integralUnit);
            copy.setIntervalLoggingPeriod(intervalLoggingPeriod);
            copy.setIntervalLoggingPeriodType(intervalLoggingPeriodType);
            copy.setIntervalLoggingType(intervalLoggingType);
            copy.setLoggingType(loggingType);
            copy.setName(name);
            copy.setPlotType(plotType);
            copy.setSimplifyType(simplifyType);
            copy.setSimplifyTolerance(simplifyTolerance);
            copy.setSimplifyTarget(simplifyTarget);
            copy.setTextRenderer(textRenderer);
            copy.setPointLocator(pointLocator);
            copy.setPurgeOverride(purgeOverride);
            copy.setPurgePeriod(purgePeriod);
            copy.setPurgeType(purgeType);
            copy.setRenderedUnit(renderedUnit);
            copy.setTolerance(tolerance);
            copy.setUnit(unit);
            copy.setUseIntegralUnit(useIntegralUnit);
            copy.setUseRenderedUnit(useRenderedUnit);
            copy.setXid(xid);
            copy.setOverrideIntervalLoggingSamples(overrideIntervalLoggingSamples);
            copy.setIntervalLoggingSampleWindowSize(intervalLoggingSampleWindowSize);
            copy.setReadPermission(readPermission.get());
            copy.setEditPermission(editPermission.get());
            copy.setSetPermission(setPermission.get());
            copy.setPreventSetExtremeValues(preventSetExtremeValues);
            copy.setSetExtremeHighLimit(setExtremeHighLimit);
            copy.setSetExtremeLowLimit(setExtremeLowLimit);
            copy.setTags(this.tags.get());

            return copy;
        }
        catch (CloneNotSupportedException e) {
            throw new ShouldNeverHappenException(e);
        }
    }
    /**
     * Validate the Integral Unit
     * Setting a default if its not enabled
     *
     */
    public boolean validateIntegralUnit() {
        if (!useIntegralUnit) {
            integralUnit = defaultIntegralUnit();
            return true;
        }

        // integral unit should have same dimensions as the default integrated unit
        if (integralUnit == null)
            return false;

        return integralUnit.isCompatible(defaultIntegralUnit());
    }

    public boolean validateRenderedUnit() {
        if (!useRenderedUnit) {
            renderedUnit = unit;
            return true;
        }

        // integral unit should have same dimensions as the default integrated unit
        if (renderedUnit == null)
            return false;

        return renderedUnit.isCompatible(unit);
    }

    // default unit is ONE ie no units
    private Unit<?> defaultUnit() {
        return Unit.ONE;
    }

    // default integrated unit is the base unit times seconds
    // as we are integrating over time
    private Unit<?> defaultIntegralUnit() {
        return unit.times(SI.SECOND);
    }

    public UnitConverter getIntegralConverter() {
        return defaultIntegralUnit().getConverterTo(integralUnit);
    }

    @Override
    public String toString() {
        return "DataPointVO [id=" + id + ", xid=" + xid + ", name=" + name + ", dataSourceId=" + dataSourceId
                + ", deviceName=" + deviceName + ", enabled=" + enabled
                + ", loggingType=" + loggingType + ", intervalLoggingPeriodType=" + intervalLoggingPeriodType
                + ", intervalLoggingPeriod=" + intervalLoggingPeriod + ", intervalLoggingType=" + intervalLoggingType
                + ", tolerance=" + tolerance + ", purgeOverride=" + purgeOverride + ", purgeType=" + purgeType
                + ", purgePeriod=" + purgePeriod + ", textRenderer=" + textRenderer
                + ", tags=" + tags
                + ", defaultCacheSize=" + defaultCacheSize + ", discardExtremeValues=" + discardExtremeValues
                + ", discardLowLimit=" + discardLowLimit + ", discardHighLimit=" + discardHighLimit + ", unit=" + unit
                + ", integralUnit=" + integralUnit + ", renderedUnit=" + renderedUnit + ", useIntegralUnit="
                + useIntegralUnit + ", useRenderedUnit=" + useRenderedUnit + chartColour + ", rollup=" + Common.ROLLUP_CODES.getCode(rollup)
                + ", plotType=" + plotType + ", pointLocator=" + pointLocator + ", dataSourceTypeName=" + dataSourceTypeName
                + ", dataSourceName=" + dataSourceName + ", dataSourceXid=" + dataSourceXid
                + ", overrideIntervalLoggingSamples=" + overrideIntervalLoggingSamples
                + ", intervalLoggingSampleWindowSize=" + intervalLoggingSampleWindowSize + ", readPermission="
                + readPermission + ", setPermission=" + setPermission
                + ", preventSetExtremeValues=" + preventSetExtremeValues + ", setExtremeLowLimit=" + setExtremeLowLimit
                + ", setExtremeHighLimit=" + setExtremeHighLimit + "]";
    }

    //
    //
    // Serialization
    //
    private static final int version = 14; //Skipped 7,8

    private void writeObject(ObjectOutputStream out) throws IOException {
        ensureUnitsCorrect();
        out.writeInt(version);
        out.writeObject(textRenderer);
        out.writeObject(pointLocator);
        out.writeDouble(discardLowLimit);
        out.writeDouble(discardHighLimit);
        SerializationHelper.writeSafeUTF(out, chartColour);
        out.writeInt(plotType);
        SerializationHelper.writeSafeUTF(out, JUnitUtil.formatDefault(unit));
        SerializationHelper.writeSafeUTF(out, JUnitUtil.formatDefault(integralUnit));
        SerializationHelper.writeSafeUTF(out, JUnitUtil.formatDefault(renderedUnit));
        out.writeBoolean(useIntegralUnit);
        out.writeBoolean(useRenderedUnit);
        out.writeBoolean(overrideIntervalLoggingSamples);
        out.writeInt(intervalLoggingSampleWindowSize);
        out.writeBoolean(preventSetExtremeValues);
        out.writeDouble(setExtremeLowLimit);
        out.writeDouble(setExtremeHighLimit);
        out.writeInt(simplifyType);
        out.writeDouble(simplifyTolerance);
        out.writeInt(simplifyTarget);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        int ver = in.readInt();

        // Switch on the version of the class so that version changes can be
        // elegantly handled.
        if (ver == 1) {
            name = SerializationHelper.readSafeUTF(in);
            deviceName = null;
            enabled = in.readBoolean();
            loggingType = in.readInt();
            intervalLoggingPeriodType = in.readInt();
            intervalLoggingPeriod = in.readInt();
            intervalLoggingType = in.readInt();
            tolerance = in.readDouble();
            purgeOverride = true;
            purgeType = in.readInt();
            purgePeriod = in.readInt();
            textRenderer = (TextRenderer) in.readObject();
            in.readObject(); //Legacy chart renderer
            pointLocator = (PointLocatorVO<?>) in.readObject();
            defaultCacheSize = in.readInt();
            discardExtremeValues = in.readBoolean();
            discardLowLimit = in.readDouble();
            discardHighLimit = in.readDouble();
            engineeringUnits = in.readInt();
            chartColour = "";
            plotType = PlotTypes.STEP;
            unit = defaultUnit();
            integralUnit = defaultUnit();
            renderedUnit = defaultIntegralUnit();
            useIntegralUnit = false;
            useRenderedUnit = false;
            overrideIntervalLoggingSamples = false;
            intervalLoggingSampleWindowSize = 10;
            preventSetExtremeValues = false;
            setExtremeLowLimit = -Double.MAX_VALUE;
            setExtremeHighLimit = Double.MAX_VALUE;
            simplifyType = SimplifyTypes.NONE;
            simplifyTolerance = 10.0;
            simplifyTarget = 5000;
        }
        else if (ver == 2) {
            name = SerializationHelper.readSafeUTF(in);
            deviceName = null;
            enabled = in.readBoolean();
            loggingType = in.readInt();
            intervalLoggingPeriodType = in.readInt();
            intervalLoggingPeriod = in.readInt();
            intervalLoggingType = in.readInt();
            tolerance = in.readDouble();
            purgeOverride = true;
            purgeType = in.readInt();
            purgePeriod = in.readInt();
            textRenderer = (TextRenderer) in.readObject();
            in.readObject(); //Legacy chart renderer
            pointLocator = (PointLocatorVO<?>) in.readObject();
            defaultCacheSize = in.readInt();
            discardExtremeValues = in.readBoolean();
            discardLowLimit = in.readDouble();
            discardHighLimit = in.readDouble();
            engineeringUnits = in.readInt();
            chartColour = SerializationHelper.readSafeUTF(in);
            plotType = PlotTypes.STEP;
            unit = defaultUnit();
            integralUnit = defaultUnit();
            renderedUnit = defaultIntegralUnit();
            useIntegralUnit = false;
            useRenderedUnit = false;
            overrideIntervalLoggingSamples = false;
            intervalLoggingSampleWindowSize = 10;
            preventSetExtremeValues = false;
            setExtremeLowLimit = -Double.MAX_VALUE;
            setExtremeHighLimit = Double.MAX_VALUE;
            simplifyType = SimplifyTypes.NONE;
            simplifyTolerance = 10.0;
            simplifyTarget = 5000;
        }
        else if (ver == 3) {
            name = SerializationHelper.readSafeUTF(in);
            deviceName = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            loggingType = in.readInt();
            intervalLoggingPeriodType = in.readInt();
            intervalLoggingPeriod = in.readInt();
            intervalLoggingType = in.readInt();
            tolerance = in.readDouble();
            purgeOverride = true;
            purgeType = in.readInt();
            purgePeriod = in.readInt();
            textRenderer = (TextRenderer) in.readObject();
            in.readObject();  //Legacy chart renderer
            pointLocator = (PointLocatorVO<?>) in.readObject();
            defaultCacheSize = in.readInt();
            discardExtremeValues = in.readBoolean();
            discardLowLimit = in.readDouble();
            discardHighLimit = in.readDouble();
            engineeringUnits = in.readInt();
            chartColour = SerializationHelper.readSafeUTF(in);
            plotType = PlotTypes.STEP;
            unit = defaultUnit();
            integralUnit = defaultUnit();
            renderedUnit = defaultIntegralUnit();
            useIntegralUnit = false;
            useRenderedUnit = false;
            overrideIntervalLoggingSamples = false;
            intervalLoggingSampleWindowSize = 10;
            preventSetExtremeValues = false;
            setExtremeLowLimit = -Double.MAX_VALUE;
            setExtremeHighLimit = Double.MAX_VALUE;
            simplifyType = SimplifyTypes.NONE;
            simplifyTolerance = 10.0;
            simplifyTarget = 5000;
        }
        else if (ver == 4) {
            name = SerializationHelper.readSafeUTF(in);
            deviceName = SerializationHelper.readSafeUTF(in);
            enabled = in.readBoolean();
            loggingType = in.readInt();
            intervalLoggingPeriodType = in.readInt();
            intervalLoggingPeriod = in.readInt();
            intervalLoggingType = in.readInt();
            tolerance = in.readDouble();
            purgeOverride = true;
            purgeType = in.readInt();
            purgePeriod = in.readInt();
            textRenderer = (TextRenderer) in.readObject();
            in.readObject(); //Legacy chart renderer
            pointLocator = (PointLocatorVO<?>) in.readObject();
            defaultCacheSize = in.readInt();
            discardExtremeValues = in.readBoolean();
            discardLowLimit = in.readDouble();
            discardHighLimit = in.readDouble();
            engineeringUnits = in.readInt();
            chartColour = SerializationHelper.readSafeUTF(in);
            plotType = in.readInt();
            useIntegralUnit = false;
            useRenderedUnit = false;
            overrideIntervalLoggingSamples = false;
            intervalLoggingSampleWindowSize = 10;
            preventSetExtremeValues = false;
            setExtremeLowLimit = -Double.MAX_VALUE;
            setExtremeHighLimit = Double.MAX_VALUE;
            simplifyType = SimplifyTypes.NONE;
            simplifyTolerance = 10.0;
            simplifyTarget = 5000;
        }
        else if (ver == 5) {
            textRenderer = (TextRenderer) in.readObject();
            in.readObject(); //Legacy chart renderer
            pointLocator = (PointLocatorVO<?>) in.readObject();
            discardLowLimit = in.readDouble();
            discardHighLimit = in.readDouble();
            chartColour = SerializationHelper.readSafeUTF(in);
            plotType = in.readInt();
            unit = defaultUnit();
            integralUnit = defaultUnit();
            renderedUnit = defaultIntegralUnit();
            useIntegralUnit = false;
            useRenderedUnit = false;
            overrideIntervalLoggingSamples = false;
            intervalLoggingSampleWindowSize = 10;
            preventSetExtremeValues = false;
            setExtremeLowLimit = -Double.MAX_VALUE;
            setExtremeHighLimit = Double.MAX_VALUE;
            simplifyType = SimplifyTypes.NONE;
            simplifyTolerance = 10.0;
            simplifyTarget = 5000;
        }
        else if (ver == 6) {
            textRenderer = (TextRenderer) in.readObject();
            in.readObject(); //Legacy chart renderer
            pointLocator = (PointLocatorVO<?>) in.readObject();
            discardLowLimit = in.readDouble();
            discardHighLimit = in.readDouble();
            chartColour = SerializationHelper.readSafeUTF(in);
            plotType = in.readInt();
            unit = (Unit<?>) in.readObject();
            integralUnit = (Unit<?>) in.readObject();
            renderedUnit = (Unit<?>) in.readObject();
            useIntegralUnit = in.readBoolean();
            useRenderedUnit = in.readBoolean();
            overrideIntervalLoggingSamples = false;
            intervalLoggingSampleWindowSize = 10;
            preventSetExtremeValues = false;
            setExtremeLowLimit = -Double.MAX_VALUE;
            setExtremeHighLimit = Double.MAX_VALUE;
            simplifyType = SimplifyTypes.NONE;
            simplifyTolerance = 10.0;
            simplifyTarget = 5000;
        }
        else if (ver == 7) {
            textRenderer = (TextRenderer) in.readObject();
            in.readObject(); //Legacy chart renderer
            pointLocator = (PointLocatorVO<?>) in.readObject();
            discardLowLimit = in.readDouble();
            discardHighLimit = in.readDouble();
            chartColour = SerializationHelper.readSafeUTF(in);
            plotType = in.readInt();
            unit = (Unit<?>) in.readObject();
            integralUnit = (Unit<?>) in.readObject();
            renderedUnit = (Unit<?>) in.readObject();
            useIntegralUnit = in.readBoolean();
            useRenderedUnit = in.readBoolean();
            preventSetExtremeValues = false;
            setExtremeLowLimit = -Double.MAX_VALUE;
            setExtremeHighLimit = Double.MAX_VALUE;
            simplifyType = SimplifyTypes.NONE;
            simplifyTolerance = 10.0;
            simplifyTarget = 5000;
        }
        else if (ver == 8) {
            textRenderer = (TextRenderer) in.readObject();
            in.readObject(); //Legacy chart renderer
            pointLocator = (PointLocatorVO<?>) in.readObject();
            discardLowLimit = in.readDouble();
            discardHighLimit = in.readDouble();
            rollup = Common.Rollups.NONE;
            chartColour = SerializationHelper.readSafeUTF(in);
            plotType = in.readInt();
            unit = (Unit<?>) in.readObject();
            integralUnit = (Unit<?>) in.readObject();
            renderedUnit = (Unit<?>) in.readObject();
            useIntegralUnit = in.readBoolean();
            useRenderedUnit = in.readBoolean();
            in.readDouble(); // error
            in.readBoolean(); // error is a percentage
            preventSetExtremeValues = false;
            setExtremeLowLimit = -Double.MAX_VALUE;
            setExtremeHighLimit = Double.MAX_VALUE;
            simplifyType = SimplifyTypes.NONE;
            simplifyTolerance = 10.0;
            simplifyTarget = 5000;
        }
        else if (ver == 9) {
            textRenderer = (TextRenderer) in.readObject();
            in.readObject(); //Legacy chart renderer
            pointLocator = (PointLocatorVO<?>) in.readObject();
            discardLowLimit = in.readDouble();
            discardHighLimit = in.readDouble();
            chartColour = SerializationHelper.readSafeUTF(in);
            plotType = in.readInt();

            try{
                unit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                unit = defaultUnit();
            }
            try{
                integralUnit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                integralUnit = defaultUnit();
            }

            try{
                renderedUnit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                renderedUnit = defaultUnit();
            }

            useIntegralUnit = in.readBoolean();
            useRenderedUnit = in.readBoolean();
            overrideIntervalLoggingSamples = false;
            intervalLoggingSampleWindowSize = 10;
            preventSetExtremeValues = false;
            setExtremeLowLimit = -Double.MAX_VALUE;
            setExtremeHighLimit = Double.MAX_VALUE;
            simplifyType = SimplifyTypes.NONE;
            simplifyTolerance = 10.0;
            simplifyTarget = 5000;
        }
        else if (ver == 10) {
            textRenderer = (TextRenderer) in.readObject();
            in.readObject(); //Legacy chart renderer
            pointLocator = (PointLocatorVO<?>) in.readObject();
            discardLowLimit = in.readDouble();
            discardHighLimit = in.readDouble();
            chartColour = SerializationHelper.readSafeUTF(in);
            plotType = in.readInt();

            try{
                unit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                unit = defaultUnit();
            }
            try{
                integralUnit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                integralUnit = defaultUnit();
            }

            try{
                renderedUnit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                renderedUnit = defaultUnit();
            }
            useIntegralUnit = in.readBoolean();
            useRenderedUnit = in.readBoolean();
            overrideIntervalLoggingSamples = in.readBoolean();
            intervalLoggingSampleWindowSize = in.readInt();
            preventSetExtremeValues = false;
            setExtremeLowLimit = -Double.MAX_VALUE;
            setExtremeHighLimit = Double.MAX_VALUE;
            simplifyType = SimplifyTypes.NONE;
            simplifyTolerance = 10.0;
            simplifyTarget = 5000;
        }
        else if (ver == 11) {
            textRenderer = (TextRenderer) in.readObject();
            in.readObject(); //Legacy chart renderer
            pointLocator = (PointLocatorVO<?>) in.readObject();
            discardLowLimit = in.readDouble();
            discardHighLimit = in.readDouble();
            chartColour = SerializationHelper.readSafeUTF(in);
            plotType = in.readInt();

            try{
                unit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                unit = defaultUnit();
            }
            try{
                integralUnit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                integralUnit = defaultUnit();
            }

            try{
                renderedUnit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                renderedUnit = defaultUnit();
            }
            useIntegralUnit = in.readBoolean();
            useRenderedUnit = in.readBoolean();
            overrideIntervalLoggingSamples = in.readBoolean();
            intervalLoggingSampleWindowSize = in.readInt();
            preventSetExtremeValues = in.readBoolean();
            setExtremeLowLimit = in.readDouble();
            setExtremeHighLimit = in.readDouble();
            simplifyType = SimplifyTypes.NONE;
            simplifyTolerance = 10.0;
            simplifyTarget = 5000;
        }
        else if (ver == 12) {
            textRenderer = (TextRenderer) in.readObject();
            in.readObject(); //Legacy chart renderer
            pointLocator = (PointLocatorVO<?>) in.readObject();
            discardLowLimit = in.readDouble();
            discardHighLimit = in.readDouble();
            chartColour = SerializationHelper.readSafeUTF(in);
            plotType = in.readInt();

            try{
                unit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                unit = defaultUnit();
            }
            try{
                integralUnit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                integralUnit = defaultUnit();
            }

            try{
                renderedUnit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                renderedUnit = defaultUnit();
            }
            useIntegralUnit = in.readBoolean();
            useRenderedUnit = in.readBoolean();
            overrideIntervalLoggingSamples = in.readBoolean();
            intervalLoggingSampleWindowSize = in.readInt();
            preventSetExtremeValues = in.readBoolean();
            setExtremeLowLimit = in.readDouble();
            setExtremeHighLimit = in.readDouble();
            simplifyType = in.readInt();
            simplifyTolerance = in.readDouble();
            simplifyTarget = 5000;
        }
        else if (ver == 13) {
            textRenderer = (TextRenderer) in.readObject();
            in.readObject(); //Legacy chart renderer
            pointLocator = (PointLocatorVO<?>) in.readObject();
            discardLowLimit = in.readDouble();
            discardHighLimit = in.readDouble();
            chartColour = SerializationHelper.readSafeUTF(in);
            plotType = in.readInt();

            try{
                unit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                unit = defaultUnit();
            }
            try{
                integralUnit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                integralUnit = defaultUnit();
            }

            try{
                renderedUnit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                renderedUnit = defaultUnit();
            }
            useIntegralUnit = in.readBoolean();
            useRenderedUnit = in.readBoolean();
            overrideIntervalLoggingSamples = in.readBoolean();
            intervalLoggingSampleWindowSize = in.readInt();
            preventSetExtremeValues = in.readBoolean();
            setExtremeLowLimit = in.readDouble();
            setExtremeHighLimit = in.readDouble();
            simplifyType = in.readInt();
            simplifyTolerance = in.readDouble();
            simplifyTarget = in.readInt();
        }
        else if (ver == 14) {
            textRenderer = (TextRenderer) in.readObject();
            pointLocator = (PointLocatorVO<?>) in.readObject();
            discardLowLimit = in.readDouble();
            discardHighLimit = in.readDouble();
            chartColour = SerializationHelper.readSafeUTF(in);
            plotType = in.readInt();

            try{
                unit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                unit = defaultUnit();
            }
            try{
                integralUnit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                integralUnit = defaultUnit();
            }

            try{
                renderedUnit = JUnitUtil.parseDefault(SerializationHelper.readSafeUTF(in));
            }catch(Exception e){
                renderedUnit = defaultUnit();
            }
            useIntegralUnit = in.readBoolean();
            useRenderedUnit = in.readBoolean();
            overrideIntervalLoggingSamples = in.readBoolean();
            intervalLoggingSampleWindowSize = in.readInt();
            preventSetExtremeValues = in.readBoolean();
            setExtremeLowLimit = in.readDouble();
            setExtremeHighLimit = in.readDouble();
            simplifyType = in.readInt();
            simplifyTolerance = in.readDouble();
            simplifyTarget = in.readInt();
        }
        //Units no longer stored with text renderer
        setUnitsOnTextRenderer();

        // Check the purge type. Weird how this could have been set to 0.
        if (purgeType == 0)
            purgeType = Common.TimePeriods.YEARS;
        // Ditto for purge period
        if (purgePeriod == 0)
            purgePeriod = 1;

    }

    private void setUnitsOnTextRenderer() {
        if (textRenderer instanceof ConvertingRenderer) {
            ConvertingRenderer cr = (ConvertingRenderer) textRenderer;
            cr.setUnit(unit);
            if (useRenderedUnit) {
                cr.setRenderedUnit(renderedUnit);
            }
            else {
                cr.setRenderedUnit(unit);
            }
        }
    }

    public void ensureUnitsCorrect() {
        if (unit == null) {
            unit = JUnitUtil.convertToUnit(engineeringUnits);
        }

        if (integralUnit == null || !validateIntegralUnit()) {
            integralUnit = defaultIntegralUnit();
        }

        if (renderedUnit == null || !validateRenderedUnit()) {
            renderedUnit = defaultUnit();
        }

        setUnitsOnTextRenderer();
    }

    @Override
    public void jsonWrite(ObjectWriter writer) throws IOException, JsonException {
        super.jsonWrite(writer);
        writer.writeEntry("loggingType", LOGGING_TYPE_CODES.getCode(loggingType));
        writer.writeEntry("intervalLoggingPeriodType", Common.TIME_PERIOD_CODES.getCode(intervalLoggingPeriodType));
        writer.writeEntry("intervalLoggingType", INTERVAL_LOGGING_TYPE_CODES.getCode(intervalLoggingType));
        writer.writeEntry("purgeType", Common.TIME_PERIOD_CODES.getCode(purgeType));
        writer.writeEntry("pointLocator", pointLocator);
        writer.writeEntry("eventDetectors", EventDetectorDao.getInstance().getWithSource(id, this));
        writer.writeEntry("plotType", PLOT_TYPE_CODES.getCode(plotType));
        writer.writeEntry("rollup", Common.ROLLUP_CODES.getCode(rollup));
        writer.writeEntry("unit", JUnitUtil.formatDefault(unit));

        if (useIntegralUnit)
            writer.writeEntry("integralUnit", JUnitUtil.formatDefault(integralUnit));
        if (useRenderedUnit)
            writer.writeEntry("renderedUnit", JUnitUtil.formatDefault(renderedUnit));

        writer.writeEntry("simplifyType", SIMPLIFY_TYPE_CODES.getCode(simplifyType));
        if(simplifyType == SimplifyTypes.TARGET)
            writer.writeEntry("simplifyTarget", simplifyTarget);
        else if(simplifyType == SimplifyTypes.TOLERANCE)
            writer.writeEntry("simplifyTolerance", simplifyTolerance);
    }

    @Override
    public void jsonRead(JsonReader reader, JsonObject jsonObject) throws JsonException {

        //Not reading XID so can't do this: super.jsonRead(reader, jsonObject);
        if(jsonObject.containsKey("name"))
            name = getString(jsonObject, "name");
        if(jsonObject.containsKey("enabled"))
            enabled = getBoolean(jsonObject, "enabled");

        String text = jsonObject.getString("loggingType");
        if (text != null) {
            loggingType = LOGGING_TYPE_CODES.getId(text);
            if (loggingType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "loggingType", text,
                        LOGGING_TYPE_CODES.getCodeList());
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
            intervalLoggingType = INTERVAL_LOGGING_TYPE_CODES.getId(text);
            if (intervalLoggingType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "intervalLoggingType", text,
                        INTERVAL_LOGGING_TYPE_CODES.getCodeList());
        }

        text = jsonObject.getString("purgeType");
        if (text != null) {
            purgeType = Common.TIME_PERIOD_CODES.getId(text);
            if (purgeType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "purgeType", text,
                        Common.TIME_PERIOD_CODES.getCodeList(TimePeriods.MILLISECONDS, TimePeriods.SECONDS, TimePeriods.MINUTES, TimePeriods.HOURS));
        }

        JsonObject locatorJson = jsonObject.getJsonObject("pointLocator");
        if (locatorJson != null)
            reader.readInto(pointLocator, locatorJson);

        text = jsonObject.getString("unit");
        if (text != null) {
            unit = parseUnitString(text, "unit");
        }

        text = jsonObject.getString("integralUnit");
        if (text != null) {
            useIntegralUnit = true;
            integralUnit = parseUnitString(text, "integralUnit");
        }

        text = jsonObject.getString("renderedUnit");
        if (text != null) {
            useRenderedUnit = true;
            renderedUnit = parseUnitString(text, "renderedUnit");
        }

        text = jsonObject.getString("plotType");
        if (text != null) {
            plotType = PLOT_TYPE_CODES.getId(text);
            if (plotType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "plotType", text,
                        PLOT_TYPE_CODES.getCodeList());
        }

        //Rollup
        text = jsonObject.getString("rollup");
        if (text != null){
            rollup = Common.ROLLUP_CODES.getId(text);
            if (rollup == -1)
                throw new TranslatableJsonException("emport.error.chart.invalid", "rollup", text,
                        Common.ROLLUP_CODES.getCodeList());
        }

        //Simplify
        text = jsonObject.getString("simplifyType");
        if (text != null){
            simplifyType = SIMPLIFY_TYPE_CODES.getId(text);
            if(simplifyType == -1)
                throw new TranslatableJsonException("emport.error.invalid", "simplifyType", text,
                        SIMPLIFY_TYPE_CODES.getCodeList());
        }

        int simplifyTarget = jsonObject.getInt("simplifyTarget", Integer.MIN_VALUE);
        if (simplifyTarget != Integer.MIN_VALUE)
            this.simplifyTarget = simplifyTarget;

        double simplifyTolerance = jsonObject.getDouble("simplifyTolerance", Double.NaN);
        if (simplifyTolerance != Double.NaN)
            this.simplifyTolerance = simplifyTolerance;
    }

    private Unit<?> parseUnitString(String string, String item) throws TranslatableJsonException {
        try {
            return JUnitUtil.parseDefault(string);
        }
        catch (Exception e) {
            throw new TranslatableJsonException("emport.error.parseError", item);
        }
    }

    /**
     * Returns a map of the tag keys and values. Will not contain "name" or "device" keys.
     * @return unmodifiable map of tags
     */
    @Override
    public Map<String, String> getTags() {
        Map<String, String> tags = this.tags.get();
        if (tags == null) {
            return null;
        }
        return Collections.unmodifiableMap(tags);
    }

    /**
     * Note "name" and "device" keys are removed when setting the tags.  The original map
     *  is not modified and may still contain "name" and/or "device"
     *
     */
    public void setTags(Map<String, String> tags) {
        if (tags == null) {
            this.tags.set(null);
            return;
        }

        Map<String, String> newTags = new HashMap<>();
        newTags.putAll(tags);
        newTags.remove(DataPointTagsDao.NAME_TAG_KEY);
        newTags.remove(DataPointTagsDao.DEVICE_TAG_KEY);
        this.tags.set(newTags);
    }

    /**
     * Note "name" and "device" keys are removed when setting the tags.  The original map
     *  is not modified and may still contain "name" and/or "device"
     *
     */
    public void supplyTags(Supplier<Map<String, String>> tags) {
        this.tags = new LazyField<>(() -> {
            Map<String, String> newTags = new HashMap<>();
            Map<String, String> t = tags.get();
            if (t == null) {
                return null;
            }
            newTags.putAll(t);
            newTags.remove(DataPointTagsDao.NAME_TAG_KEY);
            newTags.remove(DataPointTagsDao.DEVICE_TAG_KEY);
            return newTags;
        });
    }
}
