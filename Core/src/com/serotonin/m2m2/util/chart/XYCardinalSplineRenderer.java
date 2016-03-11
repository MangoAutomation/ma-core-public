/**
 * Copyright (C) 2016 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.serotonin.m2m2.util.chart;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

/**
 * Spline Renderer Pulled from JFree Forum to place XYSplineRenderer 
 * as it was producing artifacts in some plots
 * 
 * @author Terry Packer
 *
 */
public class XYCardinalSplineRenderer extends XYLineAndShapeRenderer {

	private static final long serialVersionUID = -5155759408423090131L;

	private final List<Point2D> points = new ArrayList<>();
	private final double tension;
	private final int segments;

	public XYCardinalSplineRenderer() {
		super();
		tension = 0.5;
		segments = 16;
	}

	/**
	 * 
	 * @param tension - Closer to 0 gives less of a curve
	 * @param segments
	 */
	public XYCardinalSplineRenderer(double tension, int segments) {
		super();
		this.tension = tension;
		this.segments = segments;
	}
	
	@Override
	public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea,
			XYPlot plot, XYDataset data, PlotRenderingInfo info) {

		State state = (State) super.initialise(g2, dataArea, plot, data, info);
		state.setProcessVisibleItemsOnly(false);
		this.points.clear();
		setDrawSeriesLineAsPath(true);
		return state;
	}

	@Override
	protected void drawPrimaryLineAsPath(XYItemRendererState state,
			Graphics2D g2, XYPlot plot, XYDataset dataset, int pass,
			int series, int item, ValueAxis domainAxis, ValueAxis rangeAxis,
			Rectangle2D dataArea) {

		RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
		RectangleEdge yAxisLocation = plot.getRangeAxisEdge();

		// get the data point...
		double x1 = dataset.getXValue(series, item);
		double y1 = dataset.getYValue(series, item);
		double transX1 = domainAxis.valueToJava2D(x1, dataArea, xAxisLocation);
		double transY1 = rangeAxis.valueToJava2D(y1, dataArea, yAxisLocation);

		State s = (State) state;
		// update path to reflect latest point
		if (!Double.isNaN(transX1) && !Double.isNaN(transY1)) {
			this.points.add(new Point2D.Double(transX1, transY1));
		}
		// if this is the last item, draw the path ...
		if (item == s.getLastItemIndex()) {
			List<Point2D> pts = this.getCurvePoints();
			s.seriesPath.moveTo(pts.get(0).getX(), pts.get(0).getY());

			for (int i = 1; i < pts.size(); i++) {
				final Point2D p = pts.get(i);
				s.seriesPath.lineTo(p.getX(), p.getY());
			}

			// draw path
			drawFirstPassShape(g2, pass, series, item, s.seriesPath);
			this.points.clear();
		}
	}

	private List<Point2D> getCurvePoints() {
		List<Point2D> pts = this.points;
		pts.add(0, (Point2D) pts.get(0).clone());
		pts.add((Point2D) pts.get(pts.size() - 1).clone());

		// List<Point2D> result = Lists.newArrayList();
		List<Point2D> result = new ArrayList<>();

		for (int i = 1; i < pts.size() - 2; i++) {
			for (int t = 0; t <= segments; t++) {

				double t1x = (pts.get(i + 1).getX() - pts.get(i - 1).getX())
						* tension;
				double t2x = (pts.get(i + 2).getX() - pts.get(i).getX())
						* tension;

				double t1y = (pts.get(i + 1).getY() - pts.get(i - 1).getY())
						* tension;
				double t2y = (pts.get(i + 2).getY() - pts.get(i).getY())
						* tension;

				double st = (double) t / (double) segments;

				double c1 = 2 * Math.pow(st, 3) - 3 * Math.pow(st, 2) + 1;
				double c2 = -(2 * Math.pow(st, 3)) + 3 * Math.pow(st, 2);
				double c3 = Math.pow(st, 3) - 2 * Math.pow(st, 2) + st;
				double c4 = Math.pow(st, 3) - Math.pow(st, 2);

				double x = c1 * pts.get(i).getX() + c2 * pts.get(i + 1).getX()
						+ c3 * t1x + c4 * t2x;
				double y = c1 * pts.get(i).getY() + c2 * pts.get(i + 1).getY()
						+ c3 * t1y + c4 * t2y;

				result.add(new Point2D.Double(x, y));
			}
		}

		return result;
	}
}