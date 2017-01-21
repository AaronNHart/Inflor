package main.java.inflor.core.compensation;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

import com.google.common.primitives.Doubles;

import main.java.inflor.core.data.FCSDimension;
import main.java.inflor.core.data.FCSFrame;
import main.java.inflor.core.fcs.ParameterTypes;
import main.java.inflor.core.gates.RangeGate;
import main.java.inflor.core.utils.BitSetUtils;
import main.java.inflor.core.utils.FCSUtilities;
import main.java.inflor.knime.nodes.compensation.calculate.ParticleType;

public class TheilSenMatrixCalculator {
  
  private static final int SCATTER_MAX = 100;
  private static final int SCATTER_MIN = 90;
  private static final int HIGH_MIN = 50;
  private static final int LOW_MAX = 10;
  private static final int MAX_K = 1000;
  private static final String MSG_MAPPED_FILE_NOT_FOUND = "Mapped file not found in supplied data list";
  private static final String MSG_EMPTY_COMP_LIST = "Input list should contain at least 1 FCSFrame";

  private static final Logger LOGGER = Logger.getLogger(TheilSenMatrixCalculator.class.getName());
  private static final String KEY_US_BEADS = "Unstained Beads";
  private static final String KEY_US_CELLS = "Unstained Cells";
  private static final ParticleType DEFAULT_PARTICLE_TYPE = ParticleType.BEADS;

  private Map<String, FCSFrame> dataMap;
  private boolean isValid = false;
  private List<String> status = new ArrayList<>();
  private ArrayList<String> inDimensions = new ArrayList<>();
  private ArrayList<String> outDimensions = new ArrayList<>();
  private ArrayList<String> ignoredDimensions = new ArrayList<>();
  private List<FCSFrame> dataList;
  private ExecutionContext exec;
  private Map<String, ParticleType> particleTypeMap = new HashMap<>();
  
  public TheilSenMatrixCalculator(List<FCSFrame> newDataList){
    dataList = newDataList;
    Optional<FCSFrame> anyFrame = dataList.stream().findAny();
    if (anyFrame.isPresent()){
      ArrayList<String> dimensionNames = anyFrame.get().getDimensionNames();
      ArrayList<String> fluorescentDims = dimensionNames
          .stream()
          .filter(FCSUtilities::isFluorescent)
          .collect(Collectors.toCollection(ArrayList::new));
      
      inDimensions = fluorescentDims;
      outDimensions = fluorescentDims;
            
      dataMap = initializeDataSet(fluorescentDims, dataList);
      isValid = validate();
    } else {
      throw new IllegalArgumentException(MSG_EMPTY_COMP_LIST);
    }
  }
  
  public TheilSenMatrixCalculator(List<FCSFrame> newDataList, Map<String, String> compensationMap, List<String> inDims, List<String> outDims, Map<String, ParticleType> particleTypeMap) {
    dataList = newDataList;

    inDimensions = inDims
        .stream()
        .collect(Collectors.toCollection(ArrayList::new));
    
    outDimensions = outDims
        .stream()
        .collect(Collectors.toCollection(ArrayList::new));
    dataMap = new HashMap<>();
    for (Entry<String, String> entry: compensationMap.entrySet()){
      Optional<FCSFrame> mappedFrame = dataList
          .stream()
          .filter(frame -> frame.getDisplayName().matches(entry.getValue()))
          .findAny();
      if (mappedFrame.isPresent()){
        dataMap.put(entry.getKey(), mappedFrame.get());
      } else {
        throw new IllegalArgumentException(MSG_MAPPED_FILE_NOT_FOUND);
      }
    }
    
    this.particleTypeMap = particleTypeMap;
    
    isValid = validate();    
  }

  private boolean validate() {
    boolean hasData = !dataMap.isEmpty();
    boolean hasValidDimensions = validateData(dataMap);
    return hasData&&hasValidDimensions;
  }

  private boolean validateData(Map<String, FCSFrame> dataSet2) {
    status.clear();
    boolean valid = true;
    for (Entry<String, FCSFrame> e: dataSet2.entrySet()){
      if (e.getKey()!=KEY_US_BEADS && e.getKey()!=KEY_US_CELLS){
        //Check to see that the dimension exists in the dataFrame.
        boolean hasDimension = e.getValue().hasDimension(e.getKey());
        if (!hasDimension){
          valid = valid&&hasDimension;
          status.add("Missing dimension: " + e.getKey() + " in " + e.getValue().getDisplayName());
        }
        //Check that each file is used only once. 
        for (Entry<String, FCSFrame> e2: dataSet2.entrySet()){
          if (e2.getKey()!=KEY_US_BEADS && e2.getKey()!=KEY_US_CELLS){
            String e1k = e.getKey();
            String e2k = e2.getKey();
            FCSFrame e1v = e.getValue();
            FCSFrame e2v = e2.getValue();
            boolean equalValues = e2v.equals(e1v);
            boolean equalKeys = e1k.equals(e2k);
            if (equalValues&&(!equalKeys)){
                valid = false;
                status.add("Duplicate entries for: " + e.getKey() + " and: " + e2.getKey() + "->" + e2.getValue().getDisplayName());
            }
          }
        } 
      } else {
        if (e.getValue()==null){
          valid = false;
          status.add("Missing file for: " + e.getKey() + ". please remove or select the file from the list.");
        }
      }
    }
    return valid;
  }

  private Map<String, FCSFrame> initializeDataSet(List<String> fluorescentDims, List<FCSFrame> dataList){
    HashMap<String,FCSFrame> compMap = new HashMap<>();
    for (String shortName:fluorescentDims){
      FCSFrame matchingFrame = findCompDimension(shortName, dataList);
      compMap.put(shortName, matchingFrame);
      particleTypeMap.put(shortName, DEFAULT_PARTICLE_TYPE);
    }
    
    compMap.put(KEY_US_BEADS, null);
    particleTypeMap.put(KEY_US_BEADS, ParticleType.BEADS);
    compMap.put(KEY_US_CELLS, null);
    particleTypeMap.put(KEY_US_CELLS, ParticleType.CELLS);

    return compMap;   
  }
  
  private FCSFrame findCompDimension(String shortName, List<FCSFrame> dataList) {
    for (FCSFrame nameMatchedframe: dataList){
      if (nameMatchedframe.getKeywordValue("$FIL").matches(".*"+shortName+".*")){
        return nameMatchedframe;
      }      
    }
    
    FCSFrame brightestFrame = null;
    Double maxMedian = Double.MIN_VALUE;
    for (FCSFrame frame: dataList){
      FCSDimension dimension = FCSUtilities.findCompatibleDimension(frame, shortName);
      if (dimension!=null){
        Double value = new Median().evaluate(dimension.getData());
        if (value > maxMedian){
          maxMedian = value;
          brightestFrame = frame;
        }
      }
    }
    if (brightestFrame == null){
      LOGGER.log(Level.FINE, "No matching frame found, null returned.");
    }
    return brightestFrame;
  }
  
  public boolean isValid(){
    return isValid;
  }
  
  public List<String> getStatus(){
    return status;
  }

  public Map<String, FCSFrame> getCompMap() {
    return dataMap;
  }

  public void removeCompDimension(String shorName) {
    dataMap.remove(shorName);
    inDimensions.remove(shorName);
    outDimensions.remove(shorName);
    particleTypeMap.remove(shorName);
    isValid = validate();
  }
  
  public void overrideMapping(String shortName, String newValue) {
    dataMap.remove(shortName);
    Optional<FCSFrame> newFrame = dataList.stream().filter(frame -> frame.getDisplayName().equals(newValue)).findAny();
    if (newFrame.isPresent()){
      dataMap.put(shortName, newFrame.get());
      particleTypeMap.put(shortName, DEFAULT_PARTICLE_TYPE);
    } else {
      LOGGER.log(Level.FINE, newValue + ": not found in loaded data.");
    }
    isValid = validate();
  }

  public double[][][] calculate() throws CanceledExecutionException {
    if (isValid){
      double[][] mtx = new double[outDimensions.size()][inDimensions.size()];
      double[][] ssm = new double[outDimensions.size()][inDimensions.size()];
      for (int i=0;i<outDimensions.size();i++){
        if (exec!=null){
          exec.checkCanceled();
          exec.setMessage("Processing: " + outDimensions.get(i) + " -> " + dataMap.get(outDimensions.get(i)).getDisplayName());
          exec.setProgress((double)i/outDimensions.size());
        }
        mtx[i] = estimateSpillovers(outDimensions.get(i), inDimensions, dataMap.get(outDimensions.get(i)))[0];
        ssm[i] = estimateSpillovers(outDimensions.get(i), inDimensions, dataMap.get(outDimensions.get(i)))[1];
      }
      return new double[][][]{mtx, ssm};
    } else {
      throw new IllegalAccessError("Matrix may not be calculated until isValid() returns true.");
    }
  }

  private double[][] estimateSpillovers(String primaryName, ArrayList<String> secondaryDims, FCSFrame fcsFrame) {
    double[] spills = new double[secondaryDims.size()];
    double[] ssms = new double[secondaryDims.size()];

    BitSet[] sampleMasks = downsample(fcsFrame, primaryName);
    
    FCSFrame lowFrame = FCSUtilities.filterColumnStore(sampleMasks[0], fcsFrame);
    FCSFrame highFrame = FCSUtilities.filterColumnStore(sampleMasks[1], fcsFrame);

    for (int i=0;i<secondaryDims.size();i++){
      String secondaryFrame = secondaryDims.get(i);
      double[] x1 = lowFrame.getDimension(primaryName).getData();
      double[] x2 = highFrame.getDimension(primaryName).getData();
      double[] y1 = lowFrame.getDimension(secondaryFrame).getData();
      double[] y2 = highFrame.getDimension(secondaryFrame).getData();
      double[] spilloverAndSSM =  TheilSenEstimator.evaluateHiLo(x1, x2, y1, y2);
      spills[i] = spilloverAndSSM[0];
      ssms[i] = spilloverAndSSM[1];
    }
    return new double[][]{spills, ssms};
  }

  private BitSet[] downsample(FCSFrame fcsFrame, String shortName) {
    FCSDimension primaryDimension = fcsFrame.getDimension(shortName);
    double[] x = primaryDimension.getData();
    //Filter values within advertised dynamic range
    double max = primaryDimension.getRange();//TODO verify -> fencepost
    RangeGate g = new RangeGate(null, new String[] {shortName}, new double[]{Doubles.min(x)}, new double[]{max});
    BitSet dynamicMask = g.evaluate(fcsFrame);
    double[] dynamicX = FCSUtilities.filterColumn(dynamicMask, x);
    //Find bitmask for top n% of dynamic events in control sample. 
    Percentile p = new Percentile();
    p.setData(dynamicX);
    double p90 = p.evaluate(SCATTER_MIN);
    double p100 = p.evaluate(SCATTER_MAX);
    BitSet brightMask = (new RangeGate(null, new String[]{shortName}, new double[]{p90}, new double[]{p100})).evaluate(fcsFrame);
    FCSFrame filteredFrame = FCSUtilities.filterColumnStore(brightMask, fcsFrame);
    //estimate scatter gate
    FCSDimension fscDim = FCSUtilities.findPreferredDimensionType(filteredFrame, ParameterTypes.FORWARD_SCATTER);
    double fscMin = Doubles.min(fscDim.getData());
    double fscMax = Doubles.max(fscDim.getData());

    FCSDimension sscDim = FCSUtilities.findPreferredDimensionType(filteredFrame, ParameterTypes.SIDE_SCATTER);
    double sscMin = Doubles.min(sscDim.getData());
    double sscMax = Doubles.max(sscDim.getData());
    
    BitSet scatterMask = (new RangeGate(
        null, 
        new String[]{fscDim.getShortName(), sscDim.getShortName()}, 
        new double[]{fscMin, sscMin}, 
        new double[]{fscMax, sscMax}))
      .evaluate(fcsFrame);
    
    //combine the inRange mask and the scatter mask to create final mask.
    BitSet finalMask = (BitSet) scatterMask.clone();
    finalMask.and(dynamicMask);
    //do percentile split 
    double[] repX = FCSUtilities.filterColumn(finalMask, x);
    if (repX.length>MAX_K){
      BitSet k = BitSetUtils.getShuffledMask(repX.length, MAX_K);
      repX = FCSUtilities.filterColumn(k, repX);
    }
    p.setData(repX);
    double low = p.evaluate(LOW_MAX);
    double high = p.evaluate(HIGH_MIN);
    
    ArrayList<Integer> lowList = new ArrayList<>();
    ArrayList<Integer> highList = new ArrayList<>();

    for (int i=0;i<finalMask.size();i++){
      if (finalMask.get(i)){
        if (x[i]<low){
          lowList.add(i);
        } else if (x[i]>high){
          highList.add(i);
        } 
      }
    }
    BitSet lowBits = new BitSet(x.length);
    lowList.forEach(lowBits::set);
    BitSet highBits = new BitSet(x.length);
    highList.forEach(highBits::set);
    return new BitSet[] {lowBits, highBits};
  }

  public String[] getOutputDims() {
    return outDimensions.toArray(new String[outDimensions.size()]);
  }

  public String[] getInputDims() {
    return inDimensions.toArray(new String[inDimensions.size()]);
  }

  public String[] getRemovedDims() {
    return ignoredDimensions.toArray(new String[ignoredDimensions.size()] );
  }
  
  public List<String> getPossibleFilesNames(){
    return dataList
        .stream()
        .map(FCSFrame::getDisplayName)
        .collect(Collectors.toList());
  }

  public void setContext(ExecutionContext exec) {
    this.exec = exec;
  }
  
  public Map<String, ParticleType> getParticleTypeMap(){
    return particleTypeMap;
  }

  public ParticleType getParticleType(String key) {
    return particleTypeMap.get(key);
  }
}