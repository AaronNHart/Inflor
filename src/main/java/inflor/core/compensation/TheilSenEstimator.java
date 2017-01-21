package main.java.inflor.core.compensation;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Median;

public class TheilSenEstimator {
  
  private TheilSenEstimator(){}
  
  public static double evaluate(double[] x, double[] y){
    if (x!=null && x.length == y.length && x.length > 0){
      double[] ms = new double[x.length*y.length];
      for (int i=0;i<x.length;i++){
        for (int j=0;j<x.length;j++){
          ms[i*x.length + j] = (y[j]-y[i])/(x[j] - x[i]);
        }
      }
      Median median = new Median();
      return median.evaluate(ms);
    } else {
      throw new IllegalArgumentException("x and y must not be null and must have a length > 0");
    }
  }

  public static double evaluate2(double[] x1, double[] x2, double[] y1, double[] y2) {
    double[] spills = new double[x1.length*x2.length];
    if (x1.length==y1.length&&x2.length==y2.length){
      for (int i=0;i<x1.length;i++){
        for (int j=0;j<x2.length;j++){
          spills[i*x2.length + j] = (y2[j]-y1[i])/(x2[j] - x1[i]);
        }
      }
    }
    Median median = new Median();
    return median.evaluate(spills);
  }
  
  public static double[] evaluateHiLo(double[] x1, double[] x2, double[] y1, double[] y2) {    
    
    StandardDeviation dev = new StandardDeviation();
    Median med = new Median();
    double brightX = med.evaluate(x2);
    double brightY = med.evaluate(y2);
    double dimX = med.evaluate(x1);
    double dimY = med.evaluate(y1);
    
    double dx = brightX-dimX;
    double dy = brightY - dimY;
    double ds = dev.evaluate(y2)-dev.evaluate(y1);
    
    return new double[]{dy/dx, ds/Math.pow(dx,0.5)};
  }
}