package csplugins.clusterviz;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;

import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelState;
import cytoscape.visual.VisualMappingManager;
import cytoscape.Cytoscape;

public class ClusterPlugin extends CytoscapePlugin {
    public ClusterPlugin () {
        //set-up menu options in plugin menu
        JMenu menu=Cytoscape.getDesktop().getCyMenus().getOperationsMenu();
        JMenuItem item;
        //ClusterViz submenu
        JMenu submenu = new JMenu("ClusterViz");
        submenu.setToolTipText("Plugin for clustering");

        //ClusterViz panel
        item = new JMenuItem("Start");
        item.setToolTipText("Start the plugin");
        item.addActionListener(new MainPanelAction());
        submenu.add(item);
        
        item = new JMenuItem("Stop");
        item.setToolTipText("Stop the plugin");
        item.addActionListener(new StopAction());
        submenu.add(item);
        submenu.addSeparator();

        //Help box
        item = new JMenuItem("Help");
        item.setToolTipText("Get help online");
        item.addActionListener(new HelpAction());
        submenu.add(item);

        //About box
        item = new JMenuItem("About...");
        item.setToolTipText("About the plugin");
        item.addActionListener(new AboutAction());
        submenu.add(item);
        
        //menu.add(submenu);
        menu.add(submenu);
    }
    
    //Some interanal Classes
    /**
     * Action to display the main panel where some clustering parameters are to be modified
     */
    public class MainPanelAction implements ActionListener {
        boolean opened = false;
        MainPanel mainPanel;
        VisualMappingManager vmm;
        ClusterVisualStyle vistyle;

        public MainPanelAction() {
            vistyle = new ClusterVisualStyle("Cluster");//our defined virtual style
            vmm = Cytoscape.getVisualMappingManager();
        }

        /**
         * This method is called when the user wants to start the clustering process.
         *
         * @param event the event that the very Menu Item:"Start" Selected.
         */
        public void actionPerformed(ActionEvent event) {
            //display MainPanel in left cytopanel
            CytoscapeDesktop desktop = Cytoscape.getDesktop();
            CytoPanel cytoPanel = desktop.getCytoPanel(SwingConstants.WEST);//get the west cytopanel

            //First we check if the plugin has already been opened
            if (!opened) {
                //if the visual style has not already been loaded, we load it
                if (!vmm.getCalculatorCatalog().getVisualStyleNames().contains("Cluster")) {
                    vmm.getCalculatorCatalog().addVisualStyle(vistyle);
                }
                //The style is not actually applied until a result is produced (in AnalyzeAction)
                mainPanel = new MainPanel(this, vistyle);
                URL iconURL = ClusterPlugin.class.getResource("resources/logo.gif");
                if (iconURL != null) {
                    ImageIcon icon = new ImageIcon(iconURL);
                    String tip = "Identify Clusters. Select an algorithm, then set the parameters";
                    cytoPanel.add("ClusterViz", icon, mainPanel, tip);	//add the main panel together with a icon with text
                } else {
                    cytoPanel.add("ClusterViz", mainPanel);
                }
            } else {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "The plugin has started already!");
            }    
            int index = cytoPanel.indexOfComponent(mainPanel);
            cytoPanel.setSelectedIndex(index);
            cytoPanel.setState(CytoPanelState.DOCK);
            setOpened(true);
        }

        /**
         * Limit the number of open instances of the MainPanel to 1. 
         * If the plugin is being closed,
         * then sets the visual style to the visual style last used.
         */
        public void setOpened(boolean opened) {
            this.opened = opened;
            if (!isOpened() && vmm.getVisualStyle() == vistyle) {
                vmm.setVisualStyle("default");
                vmm.applyAppearances();
            }
        }
        public boolean isOpened() {
            return opened;
        }
    }
    
    /**
     * The action to Stop the Cluster Plugin
     */
    private class StopAction implements ActionListener{
    	MainPanel mainPanel;
        ResultPanel resultPanel;
        StopAction () {
        }
        /**
         * get the MainPanel if the Plugin has started, null otherwise.
         */
        public MainPanel getMainPanel(){
        	MainPanel mainPanel=null;
        	CytoPanel cytoPanel=Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
    		for(int i=cytoPanel.getCytoPanelComponentCount()-1; i>=0; i--){
    			Component comp=cytoPanel.getComponentAt(i);
    			if(comp instanceof MainPanel)
    				mainPanel=(MainPanel)comp;
    		}
        	return mainPanel;
        }

        public void actionPerformed(ActionEvent e) {
        	mainPanel=getMainPanel();
            if (mainPanel==null) {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Plugin has not started yet!");
                return;
            }
            CytoscapeDesktop desktop = Cytoscape.getDesktop();
            CytoPanel cytoPanel = desktop.getCytoPanel(SwingConstants.EAST);//get the east panel
            //close all open result panels
            for (int c = cytoPanel.getCytoPanelComponentCount() - 1; c >= 0; c--) {
                cytoPanel.setSelectedIndex(c);
                Component component = cytoPanel.getSelectedComponent();
                String componentTitle;
                if (component instanceof ResultPanel) {
                    this.resultPanel = (ResultPanel) component;
                    componentTitle = this.resultPanel.getResultTitle();
                    cytoPanel.remove(component);
                    ParameterSet.removeResultParams(componentTitle);
                }
            }
            //hide the result panel
            if (cytoPanel.getCytoPanelComponentCount() == 0) {
                cytoPanel.setState(CytoPanelState.HIDE);
            }
            cytoPanel = desktop.getCytoPanel(SwingConstants.WEST);
            cytoPanel.remove(mainPanel);
            mainPanel.getTrigger().setOpened(false);
        }
    } 
    
    /**
     * Opens a browser connect to the project home page.
     */
    private class HelpAction implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            cytoscape.util.OpenBrowser.openURL("http://code.google.com/p/clusterviz-cytoscape/");
        }
    }    
    /**
     * The action to show the About dialog box
     */
    private class AboutAction implements ActionListener {
        /**
         * Invoked when the about action occurs.
         */
        public void actionPerformed(ActionEvent e) {
            //display about box
            AboutDialog aboutDialog = new AboutDialog();
            aboutDialog.pack();//the dialog must be packed???
            aboutDialog.setVisible(true);
        }
    }    
    /**
     * An about dialog box for this Cluster Plugin
     */
    private class AboutDialog extends JDialog {
    	static final long serialVersionUID=-945045L;
        public AboutDialog() {
             super(Cytoscape.getDesktop(), "About this Plugin", false);
            setResizable(false);

            //main panel for dialog box
            JEditorPane editorPane = new JEditorPane();
            editorPane.setMargin(new Insets(10,10,10,10));
            editorPane.setEditable(false);
            editorPane.setEditorKit(new HTMLEditorKit());
            editorPane.addHyperlinkListener(new HyperlinkAction(editorPane));

            URL logoURL = ClusterPlugin.class.getResource("resources/logo2.png");
            String logoCode = "";
            if (logoURL != null) {
                logoCode = "<center><img src='"+logoURL+"'></center>";
            }

            editorPane.setText(        
                    "<html><body>"+logoCode+"<h1>ClusterViz</h1>" +
                    "<p>ClusterViz is a plugin designed for graph clustering and visualization.</p>"+
                    "<p>Developed by Juan Cai, Gang Chen</p>"+
                    "<p><a href='http://code.google.com/p/clusterviz-cytoscape/'>Please visit the project home page for more details.</a>"+
                     "</p></body></html>");
            setContentPane(editorPane);
        }
        private class HyperlinkAction implements HyperlinkListener {
            JEditorPane pane;
            public HyperlinkAction(JEditorPane pane) {
                this.pane = pane;
            }
            public void hyperlinkUpdate(HyperlinkEvent event) {
                 if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    cytoscape.util.OpenBrowser.openURL(event.getURL().toString());
                }
            }
        }
    }
}
