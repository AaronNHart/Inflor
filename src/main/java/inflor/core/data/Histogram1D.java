package main.java.inflor.core.data;

import java.util.BitSet;

import main.java.inflor.core.utils.BitSetUtils;

public class Histogram1D {

  private double[] x;
  private double[] y;
  private BitSet mask;

  public Histogram1D(double[] data, double min, double max, int binCount) {

    double deltaX = (max - min) / binCount;

    x = new double[binCount];
    for (int i = 0; i < binCount; i++) {
      x[i] = i * deltaX;
    }
    y = new double[binCount];
    for (int i = 0; i < binCount; i++) {
      y[i] = 0;
    }
    for (int i = 0; i < data.length; i++) {
      int bin = (int) (data[i] / deltaX);
      if (bin >= y.length) {
        bin = binCount - 1;
      }
      y[bin]++;
    }
  }

  public double[][] getData() {
    return new double[][] {x, y};
  }

  public double[] getNonZeroX() {
    if (mask == null) {
      createMask();
    }
    return BitSetUtils.filter(x, mask);
  }

  public double[] getNonZeroY() {
    if (mask == null) {
      createMask();
    }
    return BitSetUtils.filter(y, mask);
  }

  private void createMask() {
    mask = new BitSet(y.length);
    for (int i = 0; i < y.length; i++) {
      if (y[i] > Double.MIN_NORMAL || y[i] < -Double.MIN_NORMAL) {
        mask.set(i);
      }
    }
  }
}
