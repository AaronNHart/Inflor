package tests.java.inflor.integration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import main.java.inflor.core.compensation.TheilSenMatrixCalculator;
import main.java.inflor.core.data.FCSFrame;
import main.java.inflor.core.fcs.FCSFileReader;

  public class CompMatrixCalculation {
    ArrayList<FCSFrame> dataSet = new ArrayList<>();

    public static void main(String[] args) throws Exception {
      String path = CompensationTestDatasets.OMIP_16.getPath();
      final File folder = new File(path);
      
      File[] files = folder.listFiles();
      
      List<FCSFrame> streamedFiles = Arrays
          .asList(files)
          .stream()
          .map(File::getAbsolutePath)
          .filter(FCSFileReader::isValidFCS)
          .map(FCSFileReader::read)
          .collect(Collectors.toList());
      
      TheilSenMatrixCalculator mCalc = new TheilSenMatrixCalculator(streamedFiles);      
      mCalc.removeCompDimension("Blue Vid-A");
      Optional<FCSFrame> apcFrame = streamedFiles.stream().filter(f -> "BEADS_APC_G08.fcs".equals(f.getDisplayName())).findAny();
      if (apcFrame.isPresent()){//it is
        mCalc.overrideMapping("APC-A",apcFrame.get().getDisplayName());
      }      
    }
  }