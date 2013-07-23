package org.cytoscape.ClusterViz.internal;
import java.awt.event.ActionEvent;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.model.CyNetworkViewManager;

public class MCODEHelpAction extends AbstractVizAction
{
  private static final long serialVersionUID = -8129187221346920847L;
  private final OpenBrowser openBrowser;

  public MCODEHelpAction(String name, CyApplicationManager applicationManager, CySwingApplication swingApplication, CyNetworkViewManager netViewManager, OpenBrowser openBrowser)
  {
    super(name, applicationManager, swingApplication, netViewManager, "always");
    this.openBrowser = openBrowser;
    setPreferredMenu("Apps.ClusterViz");
  }

  public void actionPerformed(ActionEvent actionEvent)
  {
    this.openBrowser.openURL("http://netlab.csu.edu.cn/");
  }
}