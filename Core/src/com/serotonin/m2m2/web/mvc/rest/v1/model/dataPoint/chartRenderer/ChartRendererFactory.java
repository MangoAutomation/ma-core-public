/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.chartRenderer;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.view.chart.ChartRenderer;
import com.serotonin.m2m2.view.chart.ImageChartRenderer;
import com.serotonin.m2m2.view.chart.ImageFlipbookRenderer;
import com.serotonin.m2m2.view.chart.StatisticsChartRenderer;
import com.serotonin.m2m2.view.chart.TableChartRenderer;
import com.serotonin.m2m2.vo.DataPointVO;
import com.serotonin.m2m2.web.mvc.rest.v1.model.dataPoint.TimePeriodModel;

/**
 * @author Terry Packer
 *
 */
public class ChartRendererFactory {

	/**
	 * @param vo
	 * @return
	 */
	public static BaseChartRendererModel<?> createModel(DataPointVO vo) {
		ChartRenderer renderer = vo.getChartRenderer();
		if(renderer == null)
			return null;
		if(renderer.getTypeName().equals(ImageChartRenderer.getDefinition().getName())){
			ImageChartRenderer r = (ImageChartRenderer)renderer;
			TimePeriodModel tp = new TimePeriodModel(r.getNumberOfPeriods(), r.getTimePeriod());
			ImageChartRendererModel model = new ImageChartRendererModel(tp);
			return model;
		}else if(renderer.getTypeName().equals(ImageFlipbookRenderer.getDefinition().getName())){
			ImageFlipbookRenderer r = (ImageFlipbookRenderer)renderer;
			ImageFlipbookChartRendererModel model = new ImageFlipbookChartRendererModel(r.getLimit());
			return model;
		}else if(renderer.getTypeName().equals(StatisticsChartRenderer.getDefinition().getName())){
			StatisticsChartRenderer r = (StatisticsChartRenderer)renderer;
			TimePeriodModel tp = new TimePeriodModel(r.getNumberOfPeriods(), r.getTimePeriod());
			StatisticsChartRendererModel model = new StatisticsChartRendererModel(tp, r.isIncludeSum());
			return model;
		}else if(renderer.getTypeName().equals(TableChartRenderer.getDefinition().getName())){
			TableChartRenderer r = (TableChartRenderer)renderer;
			TableChartRendererModel model = new TableChartRendererModel(r.getLimit());
			return model;
		}else{
			throw new ShouldNeverHappenException("Unknown Chart Renderer: " + renderer.getDef().getName());
		}
			
	}
	
	/**
	 * @param data
	 * @param renderer
	 */
	public static void updateDataPoint(DataPointVO vo,
			BaseChartRendererModel<?> baseRendererModel) {
		if(baseRendererModel == null)
			return; //Nothing to do

		if(baseRendererModel.getType().equals(ImageChartRenderer.getDefinition().getName())){
			ImageChartRendererModel model = (ImageChartRendererModel)baseRendererModel;
			ImageChartRenderer r = new ImageChartRenderer();
			r.setNumberOfPeriods(model.getTimePeriod().getPeriods());
			r.setTimePeriod(Common.TIME_PERIOD_CODES.getId(model.getTimePeriod().getPeriodType()));
			vo.setChartRenderer(r);
		}else if(baseRendererModel.getType().equals(ImageFlipbookRenderer.getDefinition().getName())){
			ImageFlipbookChartRendererModel model = (ImageFlipbookChartRendererModel)baseRendererModel;
			ImageFlipbookRenderer r = new ImageFlipbookRenderer();
			r.setLimit(model.getLimit());
			vo.setChartRenderer(r);
		}else if(baseRendererModel.getType().equals(StatisticsChartRenderer.getDefinition().getName())){
			StatisticsChartRendererModel model = (StatisticsChartRendererModel)baseRendererModel;
			StatisticsChartRenderer r = new StatisticsChartRenderer();
			r.setNumberOfPeriods(model.getTimePeriod().getPeriods());
			r.setTimePeriod(Common.TIME_PERIOD_CODES.getId(model.getTimePeriod().getPeriodType()));
			r.setIncludeSum(model.isIncludeSum());
			vo.setChartRenderer(r);
		}else if(baseRendererModel.getType().equals(TableChartRenderer.getDefinition().getName())){
			TableChartRendererModel model = (TableChartRendererModel)baseRendererModel;
			TableChartRenderer r = new TableChartRenderer();
			r.setLimit(model.getLimit());
			vo.setChartRenderer(r);
		}else{
			throw new ShouldNeverHappenException("Unknown Chart Renderer: " + baseRendererModel.getType());
		}
		
	}

}
