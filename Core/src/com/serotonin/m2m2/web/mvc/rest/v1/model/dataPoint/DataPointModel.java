/**
 * Copyright (C) 2014 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.TemplateDao;
import com.serotonin.m2m2.util.UnitUtil;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.vo.dataSource.PointLocatorVO;
import com.serotonin.m2m2.vo.template.BaseTemplateVO;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumn;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnGetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVColumnSetter;
import com.serotonin.m2m2.web.mvc.rest.v1.csv.CSVEntity;
import com.serotonin.m2m2.web.mvc.rest.v1.mapping.SuperclassModelDeserializer;
import com.serotonin.m2m2.web.mvc.rest.v1.model.AbstractActionVoModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.chartRenderer.BaseChartRendererModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.chartRenderer.ChartRendererFactory;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.textRenderer.BaseTextRendererModel;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.textRenderer.TextRendererFactory;


/**
 * Rest Data Model
 * 
 * 
 * @author Terry Packer
 *
 */
@CSVEntity()
public class DataPointModel extends AbstractActionVoModel<DataPointVO>{
	
	@CSVColumn(header="templateXid", order = 8)
	private String templateXid; //Used for Model Creation/Saving of Data Points
	
	@JsonProperty
	private LoggingPropertiesModel loggingProperties;
	
	@JsonProperty 
	@JsonDeserialize(using = SuperclassModelDeserializer.class)
	private BaseTextRendererModel<?> textRenderer;
	
	@JsonProperty 
	@JsonDeserialize(using = SuperclassModelDeserializer.class)
	private BaseChartRendererModel<?> chartRenderer;
	
	public DataPointModel(){
		this(new DataPointVO());
		this.loggingProperties = LoggingPropertiesModelFactory.createModel(this.data);
	}
	/**
	 * @param vo
	 */
	public DataPointModel(DataPointVO vo) {
		super(vo);
		//We must set the local properties to ensure they are in the model since
		// this constructor is used in the Mango Rest Controller Code
		if(vo.getTemplateId() != null){
			BaseTemplateVO<?> template = TemplateDao.instance.get(vo.getTemplateId());
			if(template != null)
				this.templateXid = template.getXid();
			
		}
		this.loggingProperties = LoggingPropertiesModelFactory.createModel(vo);
		this.textRenderer = TextRendererFactory.createModel(vo);
		this.chartRenderer = ChartRendererFactory.createModel(vo);
	}
	
	@CSVColumnGetter(order=4, header="deviceName")
	@JsonGetter("deviceName")
	public String getDeviceName(){
		return this.data.getDeviceName();
	}
	
	@CSVColumnSetter(order=4, header="deviceName")
	@JsonSetter("deviceName")
	public void setDeviceName(String deviceName){
		this.data.setDeviceName(deviceName);
	}
	
	@CSVColumnGetter(order=5, header="dataSourceXid")
	@JsonGetter("dataSourceXid")
	public String getDataSourceXid(){
		return this.data.getDataSourceXid();
	}
	
	@CSVColumnSetter(order=5, header="dataSourceXid")
	@JsonSetter("dataSourceXid")
	public void setDataSourceXid(String xid){
		this.data.setDataSourceXid(xid);
	}
	
	@JsonGetter("dataSourceId")
	public int getDataSourceId(){
		return this.data.getDataSourceId();
	}
	@JsonSetter("dataSourceId")
	public void setDataSourceId(int id){ } // No Op
	
	
	@CSVColumnGetter(order=6, header="readPermission")
	@JsonGetter("readPermission")
	public String getReadPermission(){
		return this.data.getReadPermission();
	}
	
	@CSVColumnSetter(order=6, header="readPermission")
	@JsonSetter("readPermission")
	public void setReadPermission(String readPermission){
		this.data.setReadPermission(readPermission);
	}
			
	@CSVColumnGetter(order=7, header="setPermission")
	@JsonGetter("setPermission")
	public String getSetPermission(){
		return this.data.getSetPermission();
	}
	
	@CSVColumnSetter(order=7, header="readPermission")
	@JsonSetter("setPermission")
	public void setSetPermission(String setPermission){
		this.data.setSetPermission(setPermission);
	}
	
	@JsonGetter("pointFolderId")
	public int getPointFolder(){
		return this.data.getPointFolderId();
	}
	@JsonSetter("pointFolderId")
	public void setPointFolder(int id){
		this.data.setPointFolderId(id);
	}
	
	@JsonGetter("purgeOverride")
	public boolean isPurgeOverride(){
		return this.data.isPurgeOverride();
	}
	@JsonSetter("purgeOverride")
	public void setPurgeOverride(boolean purgeOverride){
		this.data.setPurgeOverride(purgeOverride);
	}
	
	@JsonGetter("purgePeriod")
	public TimePeriodModel getPurgePeriod(){
		return new TimePeriodModel(this.data.getPurgePeriod(), this.data.getPurgeType());
	}
	@JsonSetter("purgePeriod")
	public void setPurgePeriod(TimePeriodModel model){
		this.data.setPurgePeriod(model.getPeriods());
		this.data.setPurgeType(Common.TIME_PERIOD_CODES.getId(model.getPeriodType()));
	}
	
	public LoggingPropertiesModel getLoggingProperties(){
		return loggingProperties;
	}

	public void setLoggingProperties(LoggingPropertiesModel props){
		this.loggingProperties = props;
		LoggingPropertiesModelFactory.updateDataPoint(this.data, this.loggingProperties);
	}
	

	public String getTemplateXid() {
		return templateXid;
	}
	public void setTemplateXid(String templateXid){
		this.templateXid = templateXid;
	}
	
	@CSVColumnGetter(header="unit", order=9)
	@JsonGetter("unit")
	public String getUnit(){
		String unit = null;
		try{
			unit = UnitUtil.formatLocal(this.data.getUnit());
		}catch(Exception e){ /*munch*/ }
		
		return unit;
	}
	@CSVColumnSetter(header="unit", order=9)
	@JsonSetter("unit")
	public void setUnit(String unit){
		try{
			this.data.setUnit(UnitUtil.parseLocal(unit));
		}catch(IllegalArgumentException e){
			this.data.setUnit(null); //So we catch this on validation
		}
	}
	
	@CSVColumnGetter(header="useIntegralUnit", order=10)
	@JsonGetter("useIntegralUnit")
	public boolean isUseIntegralUnit(){
		return this.data.isUseIntegralUnit();
	}
	@CSVColumnSetter(header="useIntegralUnit", order=10)
	@JsonSetter("useIntegralUnit")
	public void setUseIntegralUnit(boolean useIntegralUnit){
		this.data.setUseIntegralUnit(useIntegralUnit);
	}
	
	@CSVColumnGetter(header="integralUnit", order=11)
	@JsonGetter("integralUnit")
	public String getIntegralUnit(){
		String unit = null;
		try{
			unit = UnitUtil.formatLocal(this.data.getIntegralUnit());
		}catch(Exception e){ /* munch */ }
		
		return unit;
	}
	@CSVColumnSetter(header="integralUnit", order=11)
	@JsonSetter("integralUnit")
	public void setIntegralUnit(String unit){
		try{
			this.data.setIntegralUnit(UnitUtil.parseLocal(unit));
		}catch(Exception e){ 
			this.data.setIntegralUnit(null);
		}

	}

	@CSVColumnGetter(header="useRenderedUnit", order=12)
	@JsonGetter("useRenderedUnit")
	public boolean isUseRenderedUnit(){
		return this.data.isUseRenderedUnit();
	}
	@CSVColumnSetter(header="useRenderedUnit", order=12)
	@JsonSetter("useRenderedUnit")
	public void setUseRenderedUnit(boolean useRenderedUnit){
		this.data.setUseRenderedUnit(useRenderedUnit);
	}
	
	@CSVColumnGetter(header="renderedUnit", order=13)
	@JsonGetter("renderedUnit")
	public String getRenderedUnit(){
		String unit = null;
		try{
			unit = UnitUtil.formatLocal(this.data.getRenderedUnit());
		}catch(Exception e){ /* munch */}
		return unit;
	}
	@CSVColumnSetter(header="useRenderedUnit", order=13)
	@JsonSetter("renderedUnit")
	public void setRenderedUnit(String unit){
		try{
			this.data.setRenderedUnit(UnitUtil.parseLocal(unit));
		}catch(Exception e){
			this.data.setRenderedUnit(null);
		}
	}

	@CSVColumnGetter(header="pointLocatorType", order=14)
	@JsonGetter("pointLocator")
	public PointLocatorModel<?> getPointLocator(){
		PointLocatorVO vo = this.data.getPointLocator();
		if(vo == null)
			return null;
		else
			return this.data.getPointLocator().asModel();
	}
	
	@CSVColumnSetter(header="pointLocatorType", order=14)
	@JsonSetter("pointLocator")
	public void setPointLocator(PointLocatorModel<?> pl){
		if(pl != null)
			this.data.setPointLocator((PointLocatorVO)pl.getData());
	}
	
	@JsonGetter("chartColour")
	public String getChartColour(){
		return this.data.getChartColour();
	}
	@JsonSetter("chartColour")
	public void setChartColour(String colour){
		this.data.setChartColour(colour);
	}

	@JsonGetter("plotType")
	public String getPlotType(){
		return DataPointVO.PLOT_TYPE_CODES.getCode(this.data.getPlotType());
	}
	@JsonSetter("plotType")
	public void setPlotType(String plotType){
		this.data.setPlotType(DataPointVO.PLOT_TYPE_CODES.getId(plotType));
	}
	
	@JsonGetter("id")
	public int getId(){
		return this.data.getId();
	}
	@JsonSetter("id")
	public void setId(){ }//No Op 
	
	public BaseTextRendererModel<?> getTextRenderer(){
		return this.textRenderer;
	}
	
	public void setTextRenderer(BaseTextRendererModel<?> renderer){
		this.textRenderer = renderer;
		TextRendererFactory.updateDataPoint(this.data, renderer);
	}
	
	public BaseChartRendererModel<?> getChartRenderer(){
		return this.chartRenderer;
	}
	
	public void setChartRenderer(BaseChartRendererModel<?> renderer){
		this.chartRenderer = renderer;
		ChartRendererFactory.updateDataPoint(this.data, renderer);
	}
	
	@JsonGetter("dataSourceName")
	public String getDataSourceName(){
		return this.data.getDataSourceName();
	}
	
	//Removing until we have models (And decide if we are going to use a separate end point to set these)
//	@JsonGetter("eventDetectors")
//	public List<PointEventDetectorVO> getEventDetectors(){
//		if(this.data.getEventDetectors() == null)
//			return new ArrayList<PointEventDetectorVO>();
//		else
//			return this.data.getEventDetectors();
//	}
//	@JsonSetter("eventDetectors")
//	public void setEventDetectors(List<PointEventDetectorVO> eventDetectors){
//		this.data.setEventDetectors(eventDetectors);
//	}
	
	/**
	 * Ensure all Complex properties are set in the Data Point prior to returning
	 */
	@Override
	public DataPointVO getData(){
		
		if(templateXid != null){
			BaseTemplateVO<?> template = TemplateDao.instance.getByXid(templateXid);
			if(template != null)
				this.data.setTemplateId(template.getId());
			
		}

		return this.data;
	}
	
	public String getModelType() {
		return DataPointModelDefinition.TYPE_NAME;
	}
}
