package main.java.inflor.knime.nodes.compensation.calculate;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

import main.java.inflor.core.data.FCSFrame;
import main.java.inflor.core.ui.ButtonColumn;
import main.java.inflor.core.ui.TableColumnAdjuster;

/**
 * <code>NodeDialog</code> for the "CalculateCompensation" Node. This node attempts to construct a
 * compensation matrix automatically using heuristics to estimate sample roles and Theil-Sen
 * estimation to calculate individual spillover values.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple
 * dialog with standard components. If you need a more complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Aaron Hart
 */

public class CalculateCompensationNodeDialog extends NodeDialogPane {
  private static final int BUTTON_COLUMN_INDEX = 0;
  private static final int DIMENSION_COLUMN_INDEX = 1;
  private static final int CONTROL_FILE_COLUMN_INDEX = 2;
  private static final int PARTICLE_TYPE_COLUMN_INDEX = 3;

  private static final NodeLogger LOGGER =
      NodeLogger.getLogger(CalculateCompensationNodeDialog.class);

  private static final Object[] COMP_MAP_TABLE_COLUMN_NAMES =
      new String[] {"Remove", "Marker", "Compensation Control", "Type"};

  private CalculateCompensationNodeSettings mSettings;

  private JPanel compMapPanel = new JPanel(new BorderLayout());


  private JComboBox<String> comboBox = new JComboBox<>();
  private JFileChooser fileChooser = new JFileChooser(".");
  private JButton browseButton = new JButton("Browse");

  protected CalculateCompensationNodeDialog() {
    super();
    mSettings = new CalculateCompensationNodeSettings();
    // Main analysis Tab
    JPanel firstTab = new JPanel();
    BorderLayout borderLayout = new BorderLayout();
    firstTab.setLayout(borderLayout);
    firstTab.add(createFileChooser(), BorderLayout.NORTH);
    firstTab.add(compMapPanel, BorderLayout.CENTER);
    super.addTab("Control Mapping", firstTab);
    if (!mSettings.getPath().equals(CalculateCompensationNodeSettings.DEFAULT_PATH)) {
      updateCompensationMapPanel();
    }
    firstTab.setPreferredSize(new Dimension(400,600));
    firstTab.revalidate();
    firstTab.repaint();
  }

  private Component createFileChooser() {
    JPanel panel = new JPanel();
    comboBox.setPreferredSize(new Dimension(400, (int) comboBox.getPreferredSize().getHeight()));
    comboBox.setEditable(true);
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    browseButton.addActionListener(e -> {
      int status = fileChooser.showOpenDialog(panel);
      if (status == JFileChooser.APPROVE_OPTION) {
        String path = fileChooser.getSelectedFile().getAbsolutePath();
        comboBox.addItem(path);
        comboBox.setSelectedItem(path);
      }
    });
    comboBox.addActionListener(e -> {
      String path = (String) comboBox.getSelectedItem();
      mSettings.setPath(path);
      updateCompensationMapPanel();
    });
    panel.add(comboBox);
    panel.add(browseButton);
    return panel;
  }

  @Override
  protected void loadSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs)
      throws NotConfigurableException {
    if (!mSettings.getPath().equals(CalculateCompensationNodeSettings.DEFAULT_PATH)) {
      try {
        mSettings.load(settings);
        updateCompensationMapPanel();
      } catch (InvalidSettingsException e) {
        LOGGER.error("Failed to load Settings.", e);
        throw new NotConfigurableException("Failed to load Settings.", e);
      }

    } else {
      // noop
    }
  }

  private void updateCompensationMapPanel() {

    compMapPanel.removeAll();
    // Create table model
    DefaultTableModel tM = new DefaultTableModel();
    Object[][] tableContent = createTableContent();
    tM.setDataVector(tableContent, COMP_MAP_TABLE_COLUMN_NAMES);
    JTable table = new JTable(tM);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    TableColumnAdjuster adj = new TableColumnAdjuster(table);
    adj.adjustColumns();

    table.getColumnModel().getColumn(BUTTON_COLUMN_INDEX).setMaxWidth(50);

    // Setup combo box for file selector.
    JComboBox<String> fcsFrameComboBox = new JComboBox<>();
    mSettings.getFileNames().forEach(fcsFrameComboBox::addItem);
    fcsFrameComboBox.addActionListener(e -> {
      int modelRow = table.getSelectedRow();
      if (modelRow >= 0) {
        String dimension = (String) table.getModel().getValueAt(modelRow, DIMENSION_COLUMN_INDEX);
        String newValue = (String) table.getModel().getValueAt(modelRow, CONTROL_FILE_COLUMN_INDEX);
        mSettings.updateDimensionMap(dimension, newValue);
        updateCompensationMapPanel();
      }
    });
    table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(fcsFrameComboBox));
    
    //particleType combo box.
    JComboBox<ParticleType> particleTypeComboBox = new JComboBox<>();
    for (ParticleType pt: ParticleType.values()){
      particleTypeComboBox.addItem(pt);
    }
    particleTypeComboBox.addActionListener(e -> {
      int modelRow = table.getSelectedRow();
      if (modelRow >= 0) {
        String dimension = (String) table.getModel().getValueAt(modelRow, DIMENSION_COLUMN_INDEX);
        ParticleType newValue = (ParticleType) table.getModel().getValueAt(modelRow, PARTICLE_TYPE_COLUMN_INDEX);
        mSettings.udpateParticleType(dimension, newValue);
        updateCompensationMapPanel();
      }
    });
    table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(particleTypeComboBox));

    // Create button column.
    @SuppressWarnings("serial")
    Action delete = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JTable table = (JTable) e.getSource();
        int modelRow = Integer.parseInt(e.getActionCommand());
        mSettings.removeDimension(
            (String) table.getModel().getValueAt(modelRow, DIMENSION_COLUMN_INDEX));
        ((DefaultTableModel) table.getModel()).removeRow(modelRow);
        updateCompensationMapPanel();
      }
    };
    ButtonColumn buttonColumn = new ButtonColumn(table, delete, BUTTON_COLUMN_INDEX);
    buttonColumn.setMnemonic(KeyEvent.VK_D);
    compMapPanel.add(table, BorderLayout.CENTER);
    
    //Create the warnings list.
    List<String> warnings = mSettings.getWarnings();
    JPanel warningsPanel = new JPanel();
    warningsPanel.setLayout(new BoxLayout(warningsPanel, BoxLayout.Y_AXIS));
    warnings.stream().map(JLabel::new).forEach(warningsPanel::add);
    compMapPanel.add(warningsPanel, BorderLayout.SOUTH);
    compMapPanel.revalidate();
    compMapPanel.repaint();
  }

  private Object[][] createTableContent() {
    Map<String, FCSFrame> compMap = mSettings.getCompMap();
    Object[][] table = new Object[compMap.size()][3];
    int i = 0;
    for (Entry<String, FCSFrame> entry : compMap.entrySet()) {
      Object[] tableRow = new Object[4];
      tableRow[BUTTON_COLUMN_INDEX] = "remove";
      tableRow[DIMENSION_COLUMN_INDEX] = entry.getKey();
      if (entry.getValue()!=null){
        tableRow[CONTROL_FILE_COLUMN_INDEX] = entry.getValue().getDisplayName();
      } else {
        tableRow[PARTICLE_TYPE_COLUMN_INDEX] = mSettings.getParticleType(entry.getKey());
      }
      table[i] = tableRow;
      i++;
    }
    return table;
  }

  @Override
  protected void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
    mSettings.save(settings);
  }
}
