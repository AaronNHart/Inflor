package main.java.inflor.knime.nodes.downsample;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;

import main.java.inflor.core.data.FCSFrame;
import main.java.inflor.core.utils.BitSetUtils;
import main.java.inflor.core.utils.FCSUtilities;
import main.java.inflor.knime.ports.fcs.FCSFramePortObject;
import main.java.inflor.knime.ports.fcs.FCSFramePortSpec;

/**
 * This is the model implementation of Downsample.
 * 
 *
 * @author Landysh Incorportated
 */
public class DownsampleNodeModel extends NodeModel {

  // Downsample size
  static final String KEY_SIZE = "size";
  static final int DEFAULT_SIZE = 5000;

  private final SettingsModelInteger mSize = new SettingsModelInteger(KEY_SIZE, DEFAULT_SIZE);

  /**
   * Constructor for the node model.
   */
  protected DownsampleNodeModel() {
    super(new PortType[] {PortTypeRegistry.getInstance().getPortType(FCSFramePortObject.class)},
        new PortType[] {PortTypeRegistry.getInstance().getPortType(FCSFramePortObject.class)});

  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
      throws InvalidSettingsException {
    final FCSFramePortSpec portSpec = (FCSFramePortSpec) inSpecs[0];

    final FCSFramePortSpec outSpec =
        new FCSFramePortSpec(portSpec.getKeywords(), portSpec.getColumnNames(), portSpec.getRowCount());
    return new FCSFramePortSpec[] {outSpec};
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
      throws Exception {
    final FCSFramePortObject inPort = (FCSFramePortObject) inData[0];
    final FCSFramePortSpec inSpec = (FCSFramePortSpec) inPort.getSpec();
    final FCSFrame inColumnStore = inPort.getColumnStore();
    final int inSize = inColumnStore.getRowCount();
    final int downSize = mSize.getIntValue();
    final int finalSize = downSize >= inSize ? downSize : inSize;
    FCSFrame outStore = new FCSFrame(inColumnStore.getKeywords(), finalSize);
    if (downSize >= inSize) {
      outStore.setData(inColumnStore.getData());
    } else {
      final BitSet mask = BitSetUtils.getShuffledMask(inSize, downSize);
      outStore = FCSUtilities.filterColumnStore(mask, inColumnStore);
    }

    final FCSFramePortSpec outSpec = getSpec(inSpec);
    final FileStoreFactory fileStoreFactory = FileStoreFactory.createWorkflowFileStoreFactory(exec);
    final FileStore filestore = fileStoreFactory.createFileStore("column.store");
    final FCSFramePortObject outPort =
        FCSFramePortObject.createPortObject(outSpec, outStore, filestore);

    return new FCSFramePortObject[] {outPort};
  }

  private FCSFramePortSpec getSpec(FCSFramePortSpec inSpec) {        
    return new FCSFramePortSpec(
        inSpec.getKeywords(), 
        inSpec.getColumnNames(), 
        inSpec.getRowCount());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void loadInternals(final File internDir, final ExecutionMonitor exec)
      throws IOException, CanceledExecutionException {/*noop*/}

  /**
   * {@inheritDoc}
   */
  @Override
  protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
      throws InvalidSettingsException {

    mSize.loadSettingsFrom(settings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void reset() {/*noop*/}

  /**
   * {@inheritDoc}
   */
  @Override
  protected void saveInternals(final File internDir, final ExecutionMonitor exec)
      throws IOException, CanceledExecutionException {/*noop*/}

  /**
   * {@inheritDoc}
   */
  @Override
  protected void saveSettingsTo(final NodeSettingsWO settings) {

    mSize.saveSettingsTo(settings);

  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    if (mSize.getIntValue() >= 1) {
      mSize.validateSettings(settings);
    } else {
      throw new InvalidSettingsException("Downsample size must be greater than 1");
    }
  }

}
