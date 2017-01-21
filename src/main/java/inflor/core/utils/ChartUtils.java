/*
 * ------------------------------------------------------------------------
 *  Copyright 2016 by Aaron Hart
 *  Email: Aaron.Hart@gmail.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 * ---------------------------------------------------------------------
 *
 * Created on December 14, 2016 by Aaron Hart
 */
package main.java.inflor.core.utils;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.plot.XYPlot;

import main.java.inflor.core.gates.AbstractGate;
import main.java.inflor.core.gates.PolygonGate;
import main.java.inflor.core.gates.RangeGate;
import main.java.inflor.core.gates.ui.PolygonGateAnnotation;
import main.java.inflor.core.gates.ui.RectangleGateAnnotation;
import main.java.inflor.core.gates.ui.XYGateAnnotation;
import main.java.inflor.core.plots.ChartSpec;
import main.java.inflor.core.plots.FCSChartPanel;
import main.java.inflor.core.ui.LookAndFeel;

public class ChartUtils {

  private ChartUtils(){}
  
  public static Point2D getPlotCoordinates(MouseEvent e, FCSChartPanel panel) {
    Point2D p = panel.translateScreenToJava2D(e.getPoint());
    Rectangle2D plotArea = panel.getScreenDataArea();
    XYPlot plot = panel.getChart().getXYPlot();
    double x = plot.getDomainAxis().java2DToValue(p.getX(), plotArea, plot.getDomainAxisEdge());
    double y = plot.getRangeAxis().java2DToValue(p.getY(), plotArea, plot.getRangeAxisEdge());
    return new Point2D.Double(x, y);
  }

  public static AbstractGate createGate(XYGateAnnotation annotation) {
    AbstractGate gate = null;
    String subsetName = annotation.getSubsetName();
    String domainAxisName = annotation.getDomainAxisName();
    String rangeAxisName = annotation.getRangeAxisName();
    if (annotation instanceof RectangleGateAnnotation) {
      RectangleGateAnnotation rect = (RectangleGateAnnotation) annotation;
      gate = new RangeGate(subsetName, new String[] {domainAxisName, rangeAxisName},
          new double[] {rect.getX0(), rect.getY0()}, new double[] {rect.getX1(), rect.getY1()});

    } else if (annotation instanceof PolygonGateAnnotation) {
      PolygonGateAnnotation polygonGate = (PolygonGateAnnotation) annotation;
      double[] domainPoints = polygonGate.getDomainPoints();
      double[] rangePoints = polygonGate.getRangePoints();
      gate = new PolygonGate(subsetName, domainAxisName, domainPoints, rangeAxisName, rangePoints);
    } // Extend here for other gate types.
    return gate;
  }

  public static AbstractGate updateGate(AbstractGate priorGate, XYGateAnnotation updatedAnnotation,
      String subsetLabel, String domainLabel, String rangeLable) {
    /**
     * creates an updated gate with prior UUID, and new coordinates.
     */
    AbstractGate gate = null;
    if (updatedAnnotation instanceof RectangleGateAnnotation && priorGate instanceof RangeGate) {
      RectangleGateAnnotation rectAnn = (RectangleGateAnnotation) updatedAnnotation;
      gate = new RangeGate(subsetLabel, new String[] {domainLabel, rangeLable},
          new double[] {rectAnn.getX0(), rectAnn.getY0()},
          new double[] {rectAnn.getX1(), rectAnn.getY1()}, priorGate.getID());

    } else
      if (updatedAnnotation instanceof PolygonGateAnnotation && priorGate instanceof PolygonGate) {
      PolygonGateAnnotation udpatedPolyAnn = (PolygonGateAnnotation) updatedAnnotation;
      gate = new PolygonGate(subsetLabel, domainLabel, udpatedPolyAnn.getDomainPoints(), rangeLable,
          udpatedPolyAnn.getRangePoints(), priorGate.getID());
    }
    return gate;
  }

  public static XYGateAnnotation createAnnotation(AbstractGate gate) {
    if (gate instanceof RangeGate){
      RangeGate rangeGate = (RangeGate)gate;
      return new RectangleGateAnnotation(rangeGate.getLabel(), 
          rangeGate.getDomainAxisName(), 
          rangeGate.getRangeAxisName(), 
          rangeGate.getMinValue(0), rangeGate.getMinValue(1), 
          rangeGate.getMaxValue(0), rangeGate.getMaxValue(1),  
          LookAndFeel.DEFAULT_STROKE, 
          LookAndFeel.DEFAULT_GATE_COLOR);
    } else if(gate instanceof PolygonGate){
      PolygonGate polyGate = (PolygonGate)gate;
      return new PolygonGateAnnotation(polyGate.getLabel(), 
          polyGate.getDomainAxisName(), 
          polyGate.getRangeAxisName(), 
          polyGate.getFlatVertexArray(), 
          LookAndFeel.DEFAULT_STROKE, 
          LookAndFeel.DEFAULT_GATE_COLOR);
    } else{
      throw new IllegalArgumentException("unable to create gate annotation.");
    }
  }

  public static boolean gateIsCompatibleWithChart(AbstractGate gate, ChartSpec spec) {
    if (gate.getDomainAxisName().equals(spec.getDomainAxisName())
        &&gate.getRangeAxisName().equals(spec.getRangeAxisName())){
      return true;
    }
    return false;
  }
}