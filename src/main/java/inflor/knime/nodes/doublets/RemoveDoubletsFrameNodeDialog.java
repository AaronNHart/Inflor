package main.java.inflor.knime.nodes.doublets;

import java.util.ArrayList;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

import main.java.inflor.core.singlets.PuleProperties;
import main.java.inflor.core.singlets.SingletsModel;
import main.java.inflor.knime.ports.fcs.FCSFramePortSpec;

/**
 * <code>NodeDialog</code> for the "FindSingletsFrame" Node.
 * 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple
 * dialog with standard components. If you need a more complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Aaron Hart
 */
public class RemoveDoubletsFrameNodeDialog extends DefaultNodeSettingsPane {

  /**
   * New pane for configuring the FindSingletsFrame node.
   */

  private static final String AREA_LABEL = "Area Parameter";
  private static final String HEIGHT_LABEL = "Height Parameter";

  public DialogComponentStringSelection areaComponent;
  public DialogComponentStringSelection heightComponent;

  protected RemoveDoubletsFrameNodeDialog() {
    super();
    final ArrayList<String> defaultChoices = new ArrayList<String>();
    defaultChoices.add("None");
    // Area Selector
    final SettingsModelString m_areaStringSetting =
        new SettingsModelString(RemoveDoubletsFrameNodeModel.KEY_AREA_COLUMN,
            RemoveDoubletsFrameNodeModel.DEFAULT_AREA_COLUMN);
    areaComponent =
        new DialogComponentStringSelection(m_areaStringSetting, AREA_LABEL, defaultChoices);
    addDialogComponent(areaComponent);

    // height Selector
    final SettingsModelString m_heightStringSetting =
        new SettingsModelString(RemoveDoubletsFrameNodeModel.KEY_HEIGHT_COLUMN,
            RemoveDoubletsFrameNodeModel.DEFAULT_HEIGHT_COLUMN);
    heightComponent =
        new DialogComponentStringSelection(m_heightStringSetting, HEIGHT_LABEL, defaultChoices);
    addDialogComponent(heightComponent);

  }

  /** {@inheritDoc} */
  @Override
  public void loadAdditionalSettingsFrom(final NodeSettingsRO settings,
      final PortObjectSpec[] specs) throws NotConfigurableException {
    final FCSFramePortSpec spec = (FCSFramePortSpec) specs[0];
    final String[] vectorNames = spec.getColumnNames();
    final SingletsModel model = new SingletsModel(vectorNames);

    final ArrayList<String> areaChoices = model.findColumns(vectorNames, PuleProperties.AREA);
    final ArrayList<String> heightChoices = model.findColumns(vectorNames, PuleProperties.HEIGHT);

    areaComponent.replaceListItems(areaChoices, areaChoices.get(0));
    heightComponent.replaceListItems(heightChoices, heightChoices.get(0));
  }
}
