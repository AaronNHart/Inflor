package main.java.inflor.knime.nodes.doublets;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import main.java.inflor.core.data.FCSFrame;
import main.java.inflor.core.singlets.SingletsModel;
import main.java.inflor.core.utils.FCSUtilities;
import main.java.inflor.knime.data.type.cell.fcs.FCSFrameFileStoreDataCell;
import main.java.inflor.knime.nodes.fcs.read.set.ReadFCSSetNodeModel;

/**
 * This is the model implementation of RemoveDoublets.
 * 
 *
 * @author Landysh Co.
 */
public class RemoveDoubletsNodeModel extends NodeModel {

  private static final NodeLogger logger = NodeLogger.getLogger(ReadFCSSetNodeModel.class);

  RemoveDoubletsSettingsModel m_settings = new RemoveDoubletsSettingsModel();

  /**
   * Constructor for the node model.
   */
  protected RemoveDoubletsNodeModel() {
    super(1, 1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
      throws InvalidSettingsException {
    final DataTableSpec spec = createSpec(inSpecs[0]);
    return new DataTableSpec[] {spec};
  }

  private DataTableSpec createSpec(DataTableSpec dataTableSpec) {
    return dataTableSpec;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
      final ExecutionContext exec) throws Exception {

    logger.info("Beginning Execution.");
    final FileStoreFactory fileStoreFactory = FileStoreFactory.createWorkflowFileStoreFactory(exec);

    // Create the output spec and data container.
    final DataTableSpec outSpec = createSpec(inData[0].getDataTableSpec());
    final BufferedDataContainer container = exec.createDataContainer(outSpec);
    final String columnName = m_settings.getSelectedColumnSettingsModel().getStringValue();
    final int index = outSpec.findColumnIndex(columnName);
    final String areaColumn = m_settings.getAreaColumnSettingsModel().getStringValue();
    final String heightColumn = m_settings.getHeightColumnSettingsModel().getStringValue();

    int i = 0;
    for (final DataRow inRow : inData[0]) {
      final DataCell[] outCells = new DataCell[inRow.getNumCells()];
      final FCSFrame columnStore = ((FCSFrameFileStoreDataCell) inRow.getCell(index)).getFCSFrameValue();
      final SingletsModel model = new SingletsModel(columnStore.getDimensionNames().toArray(new String[columnStore.getDimensionNames().size()]));
      final double[] areaData = columnStore.getDimension(areaColumn).getData();
      final double[] heightData = columnStore.getDimension(heightColumn).getData();//TODO: DisplayName or shortName?
      final double[] ratio = model.buildModel(areaData, heightData);
      BitSet mask = model.scoreModel(ratio);

      // now create the output row
      final FCSFrame outStore = FCSUtilities.filterColumnStore(mask, columnStore);
      final String fsName = i + "ColumnStore.fs";
      final FileStore fileStore = fileStoreFactory.createFileStore(fsName);
      final FCSFrameFileStoreDataCell fileCell = new FCSFrameFileStoreDataCell(fileStore, outStore);

      for (int j = 0; j < outCells.length; j++) {
        if (j == index) {
          outCells[j] = fileCell;
        } else {
          outCells[j] = inRow.getCell(j);
        }
      }
      final DataRow outRow = new DefaultRow("Row " + i, outCells);
      container.addRowToTable(outRow);
      i++;
    }
    container.close();
    return new BufferedDataTable[] {container.getTable()};
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void loadInternals(final File internDir, final ExecutionMonitor exec)
      throws IOException, CanceledExecutionException {}

  /**
   * {@inheritDoc}
   */
  @Override
  protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
      throws InvalidSettingsException {
    m_settings.load(settings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void reset() {}

  /**
   * {@inheritDoc}
   */
  @Override
  protected void saveInternals(final File internDir, final ExecutionMonitor exec)
      throws IOException, CanceledExecutionException {}

  /**
   * {@inheritDoc}
   */
  @Override
  protected void saveSettingsTo(final NodeSettingsWO settings) {
    m_settings.save(settings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
    m_settings.validate(settings);
  }
}
// EOF
