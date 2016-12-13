package io.landysh.inflor.main.core.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import io.landysh.inflor.main.core.dataStructures.DomainObject;
import io.landysh.inflor.main.core.dataStructures.FCSFrame;
import io.landysh.inflor.main.core.gates.Hierarchical;
import io.landysh.inflor.main.knime.nodes.createGates.ui.TreeCellPlotRenderer;

@SuppressWarnings("serial")
public class CellLineageTree extends JTree {

  /**
   * Panel which stores and controls the layout of the plots in a given lineage analysis.
   */

  private DefaultMutableTreeNode root;
  private FCSFrame rootFrame;
  private DefaultMutableTreeNode currentNode;

  public CellLineageTree(FCSFrame rootFrame, List<Hierarchical> nodePool) {
    this.setCellRenderer(new TreeCellPlotRenderer());
    this.rootFrame = rootFrame;
    this.root = new DefaultMutableTreeNode(rootFrame);
    this.setModel(buildTree(root, nodePool));
    for (int i = 0; i < this.getRowCount(); i++) {
      this.expandRow(i);
    }
    
  }

  @Override
  public void updateUI() {
    setCellRenderer(null);
    super.updateUI();
    this.setCellRenderer(new TreeCellPlotRenderer());
    setRowHeight(0);
    setRootVisible(true);
    setShowsRootHandles(true);
  }
  
  private DefaultTreeModel buildTree(DefaultMutableTreeNode root, List<Hierarchical> inNodePool){
    List<Hierarchical> nodePool = inNodePool.stream().collect(Collectors.toList());
    DefaultTreeModel tree = new DefaultTreeModel(root);
    currentNode = root;

    List<DefaultMutableTreeNode> nodesToCheck = new ArrayList<DefaultMutableTreeNode>();
    nodesToCheck.add(root);
    while (nodesToCheck.size()>0){
      String parentID = ((DomainObject) currentNode.getUserObject()).getID();
      List<DefaultMutableTreeNode> dmtNodes = new ArrayList<DefaultMutableTreeNode>(); 
      for (Hierarchical node: nodePool){
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(node);
        if (node.getParentID()!=null && node.getParentID().equals(parentID)){
          dmtNodes.add(childNode);
          nodesToCheck.add(childNode);
        } else if(node.getParentID()==null){
          node.setParentID(rootFrame.getID());
          dmtNodes.add(childNode);  
          nodesToCheck.add(childNode);
        };
      }
      dmtNodes.forEach(child -> tree.insertNodeInto(child, currentNode, currentNode.getChildCount()));
      nodesToCheck.remove(currentNode);
      if (nodesToCheck.size()>0){
        currentNode = nodesToCheck.get(0);
      }
    }
    return tree;
  }
}