/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.vo.template;

import java.util.List;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.DataTypes;
import com.serotonin.m2m2.Common.TimePeriods;
import com.serotonin.m2m2.db.dao.TemplateDao;
import com.serotonin.m2m2.i18n.ProcessMessage;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.view.chart.ImageChartRenderer;
import com.serotonin.m2m2.view.chart.TableChartRenderer;
import com.serotonin.m2m2.view.text.AnalogRenderer;
import com.serotonin.m2m2.view.text.BinaryTextRenderer;
import com.serotonin.m2m2.view.text.PlainRenderer;
import com.serotonin.m2m2.vo.DataPointVO.IntervalLoggingTypes;
import com.serotonin.m2m2.vo.DataPointVO.LoggingTypes;
import com.serotonin.m2m2.vo.DataPointVO.PlotTypes;

/**
 * @author Terry Packer
 *
 */
public class DefaultDataPointPropertiesTemplateFactory {
	
	private static final String ALPHANUMERIC_XID = "Alphanumeric_Default";
	private static final String NUMERIC_XID = "Numeric_Default";
	private static final String BINARY_XID = "Binary_Default";
	private static final String MULTISTATE_XID = "Multistate_Default";
	

    /**
	 * Save the Default Templates
	 */
	public void saveDefaultTemplates() {
		
		DataPointPropertiesTemplateVO defaultAlphanumericPointTemplate = createDefaultAlphanumericTemplate();
		this.saveTemplate(defaultAlphanumericPointTemplate);

		DataPointPropertiesTemplateVO defaultBinaryPointTemplate = createDefaultBinaryTemplate();
		this.saveTemplate(defaultBinaryPointTemplate);
		
		DataPointPropertiesTemplateVO defaultMultistatePointTemplate = createDefaultMultistateTemplate();
		this.saveTemplate(defaultMultistatePointTemplate);

		DataPointPropertiesTemplateVO defaultNumericPointTemplate = createDefaultNumericTemplate();
		this.saveTemplate(defaultNumericPointTemplate);

		
	}
	
	protected void saveTemplate(DataPointPropertiesTemplateVO template){
		ProcessResult response = new ProcessResult();
		template.validate(response);
		if(!response.getHasMessages()){
			TemplateDao.instance.save(template);
		}else{
			String output = new String();
			List<ProcessMessage> messages = response.getMessages();
			for(ProcessMessage message : messages){
				output += message.toString(Common.getTranslations());
				output += "\n";
			}
			throw new ShouldNeverHappenException(output);
		}
	}
	
	protected DataPointPropertiesTemplateVO createDefaultAlphanumericTemplate(){
		DataPointPropertiesTemplateVO defaultAlphanumericPointTemplate = new DataPointPropertiesTemplateVO();
		defaultAlphanumericPointTemplate.setXid(ALPHANUMERIC_XID);
		defaultAlphanumericPointTemplate.setName("Alphanumeric");
		defaultAlphanumericPointTemplate.setDataTypeId(DataTypes.ALPHANUMERIC);
		defaultAlphanumericPointTemplate.setDefaultTemplate(true);
		defaultAlphanumericPointTemplate.setDefinition(new DataPointPropertiesTemplateDefinition());
		defaultAlphanumericPointTemplate.setChartColour("red");
		defaultAlphanumericPointTemplate.setPlotType(PlotTypes.STEP);
		defaultAlphanumericPointTemplate.setLoggingType(LoggingTypes.ON_CHANGE);
		defaultAlphanumericPointTemplate.setDefaultCacheSize(1);
		defaultAlphanumericPointTemplate.setTextRenderer(new PlainRenderer());
		TableChartRenderer alphaChartRenderer = new TableChartRenderer();
		alphaChartRenderer.setLimit(10);
		defaultAlphanumericPointTemplate.setChartRenderer(alphaChartRenderer);
		return defaultAlphanumericPointTemplate;
	}
	
	protected DataPointPropertiesTemplateVO createDefaultBinaryTemplate(){
		DataPointPropertiesTemplateVO defaultBinaryPointTemplate = new DataPointPropertiesTemplateVO();
		defaultBinaryPointTemplate.setXid(BINARY_XID);
		defaultBinaryPointTemplate.setName("Binary");
		defaultBinaryPointTemplate.setDataTypeId(DataTypes.BINARY);
		defaultBinaryPointTemplate.setDefaultTemplate(true);
		defaultBinaryPointTemplate.setDefinition(new DataPointPropertiesTemplateDefinition());
		defaultBinaryPointTemplate.setChartColour("blue");
		defaultBinaryPointTemplate.setPlotType(PlotTypes.STEP);
		defaultBinaryPointTemplate.setLoggingType(LoggingTypes.ON_CHANGE);
		defaultBinaryPointTemplate.setDefaultCacheSize(1);
		BinaryTextRenderer binaryRenderer = new BinaryTextRenderer();
		binaryRenderer.setOneColour("black");
		binaryRenderer.setOneLabel("one");
		binaryRenderer.setZeroColour("blue");
		binaryRenderer.setZeroLabel("zero");
		defaultBinaryPointTemplate.setTextRenderer(binaryRenderer);
		TableChartRenderer binaryChartRenderer = new TableChartRenderer();
		binaryChartRenderer.setLimit(10);
		defaultBinaryPointTemplate.setChartRenderer(binaryChartRenderer);
		return defaultBinaryPointTemplate;
	}
	
	protected DataPointPropertiesTemplateVO createDefaultMultistateTemplate(){
		DataPointPropertiesTemplateVO defaultMultistatePointTemplate = new DataPointPropertiesTemplateVO();
		defaultMultistatePointTemplate.setXid(MULTISTATE_XID);
		defaultMultistatePointTemplate.setName("Multistate");
		defaultMultistatePointTemplate.setDataTypeId(DataTypes.MULTISTATE);
		defaultMultistatePointTemplate.setDefaultTemplate(true);
		defaultMultistatePointTemplate.setDefinition(new DataPointPropertiesTemplateDefinition());
		defaultMultistatePointTemplate.setChartColour("green");
		defaultMultistatePointTemplate.setPlotType(PlotTypes.STEP);
		defaultMultistatePointTemplate.setLoggingType(LoggingTypes.ON_CHANGE);
		defaultMultistatePointTemplate.setDefaultCacheSize(1);
		defaultMultistatePointTemplate.setTextRenderer(new PlainRenderer());
		TableChartRenderer multistateChartRenderer = new TableChartRenderer();
		multistateChartRenderer.setLimit(10);
		defaultMultistatePointTemplate.setChartRenderer(multistateChartRenderer);
		return defaultMultistatePointTemplate;
	}
	
	protected DataPointPropertiesTemplateVO createDefaultNumericTemplate(){
		DataPointPropertiesTemplateVO defaultNumericPointTemplate = new DataPointPropertiesTemplateVO();
		defaultNumericPointTemplate.setXid(NUMERIC_XID);
		defaultNumericPointTemplate.setName("Numeric");
		defaultNumericPointTemplate.setDefaultTemplate(true);
		defaultNumericPointTemplate.setDataTypeId(DataTypes.NUMERIC);
		defaultNumericPointTemplate.setDefinition(new DataPointPropertiesTemplateDefinition());
		defaultNumericPointTemplate.setChartColour("black");
		defaultNumericPointTemplate.setPlotType(PlotTypes.SPLINE);
		defaultNumericPointTemplate.setLoggingType(LoggingTypes.INTERVAL);
		defaultNumericPointTemplate.setIntervalLoggingPeriod(1);
		defaultNumericPointTemplate.setIntervalLoggingPeriodType(TimePeriods.MINUTES);
		defaultNumericPointTemplate.setIntervalLoggingType(IntervalLoggingTypes.AVERAGE);
		defaultNumericPointTemplate.setDefaultCacheSize(1);
		AnalogRenderer numericTextRenderer = new AnalogRenderer();
		numericTextRenderer.setFormat("0.00");
		defaultNumericPointTemplate.setTextRenderer(numericTextRenderer);
		ImageChartRenderer numericChartRenderer = new ImageChartRenderer();
		numericChartRenderer.setTimePeriod(TimePeriods.DAYS);
		numericChartRenderer.setNumberOfPeriods(1);
		defaultNumericPointTemplate.setChartRenderer(numericChartRenderer);
		return defaultNumericPointTemplate;
	}
}
