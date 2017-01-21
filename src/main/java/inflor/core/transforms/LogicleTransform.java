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
package main.java.inflor.core.transforms;

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import edu.stanford.facs.logicle.FastLogicle;

public class LogicleTransform extends AbstractTransform implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private static final double LOGICLE_W_PERCENTILE = 1;
  private static final double DEFAULT_T = 262144;
  private static final double DEFAULT_W = 0.5;
  private static final double DEFAULT_M = 4.5;
  private static final double DEFAULT_A = 0;
  
  transient FastLogicle logicle;
  
  private double t;
  private double w;
  private double m;
  private double a;

  public LogicleTransform(double t2, double w2, double m2, double a2, String priorID) {
    super(priorID);
    this.t = t2;
    this.w = w2;
    this.m = m2;
    this.a = a2;
    this.logicle = new FastLogicle(t, w, m, a);
  }
  
  public LogicleTransform(double t2, double w2, double m2, double a2) {
    this(t2, w2, m2, a2, null);
  }
  
  public LogicleTransform() {
    this(DEFAULT_T, DEFAULT_W, DEFAULT_M, DEFAULT_A, null);
  }
  
  @Override
  public double[] transform(double[] rawData) {
    double[] newData = new double[rawData.length];
    for (int i = 0; i < rawData.length; i++) {
      newData[i] = transform(rawData[i]);
    }
    return newData;
  }

  public double[] inverse(double[] transformedData) {
    double[] newData = new double[transformedData.length];
    for (int i = 0; i < transformedData.length; i++) {
      newData[i] = logicle.inverse(transformedData[i]);
    }
    return newData;
  }

  public void optimizeW(double[] data) {
    /**
     * Based on the percentile method suggested by Parks/Moore.
     */
    double lowerBound = new Percentile().evaluate(data, LOGICLE_W_PERCENTILE);
    double newWidth = (m - Math.log10(t / Math.abs(lowerBound))) / 2;
    if (newWidth < 0) {
      newWidth = this.w;// reasonable?
    }
    this.w = newWidth;
    this.logicle = new FastLogicle(logicle.T, newWidth, logicle.M, logicle.A);
  }

  @Override
  public double transform(double value) {
    double newValue;
    if (value < getMinRawValue()) {
      newValue = getMinRawValue();
    } else if (value >= getMaxRawValue()) {
      newValue = getMaxRawValue();
    } else {
      newValue = value;
    }

    if (newValue >= logicle.T) {
      return 1;
    }
    return logicle.scale(newValue);
  }

  @Override
  public double inverse(double value) {
    if (value >= 1) {
      return logicle.T;
    } else {
      return logicle.inverse(value);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    this.logicle = new FastLogicle(t, w, m, a);
  }

  @Override
  public double getMinTranformedValue() {
    return 0;
  }

  @Override
  public double getMaxTransformedValue() {
    return 1;
  }

  @Override
  public double getMinRawValue() {
    return inverse(0);
  }

  @Override
  public double getMaxRawValue() {
    return t;
  }
  
  @Override
  public String toString() {
    return "Logicle: w:" + w + " , @" + getID();
  }


  public double[] getAxisValues() {
    return logicle.axisLabels();
  }

  public double getT() {
    return this.t;
  }

  public double getW() {
    return this.w;
  }

  public double getM() {
    return this.m;
  }

  public double getA() {
    return this.a;
  }
}
