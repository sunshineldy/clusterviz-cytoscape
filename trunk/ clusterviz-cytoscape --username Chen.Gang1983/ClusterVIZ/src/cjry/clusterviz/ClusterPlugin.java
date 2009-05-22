package cjry.clusterviz;

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
import cytoscape.util.CytoscapeMenuBar;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelState;
import cytoscape.visual.VisualMappingManager;
import cytoscape.Cytoscape;

public class ClusterPlugin extends CytoscapePlugin {
    public ClusterPlugin () {
    	//ActionListener ac=new ActionListener({});
        //String message = "聚类插件加载成功!";
        //System.out.println(message);
        //use the CytoscapeDesktop as parent for a Swing dialog
        //JOptionPane.showMessageDialog( Cytoscape.getDesktop(), "聚类插件加载成功!");                
        //set-up menu options in plugins menu
        CytoscapeMenuBar bar=Cytoscape.getDesktop().getCyMenus().getMenuBar();
        JMenuItem item;
        //ClusterPlugin submenu
        JMenu submenu = new JMenu("ClusterViz");
        submenu.setToolTipText("聚类分析插件");

        //ClusterPlugin panel
        item = new JMenuItem("Start");
        item.setToolTipText("启动插件");
        item.addActionListener(new MainPanelAction());
        submenu.add(item);
        
        item = new JMenuItem("Stop");
        item.setToolTipText("终止插件");
        item.addActionListener(new StopAction());
        submenu.add(item);

        submenu.addSeparator();

        //Help box
        item = new JMenuItem("Help");
        item.setToolTipText("在线帮助，访问我的博客");
        item.addActionListener(new HelpAction());
        submenu.add(item);

        //About box
        item = new JMenuItem("About...");
        item.setToolTipText("关于本插件");
        item.addActionListener(new AboutAction());
        submenu.add(item);
        
        //menu.add(submenu);
        bar.add(submenu);
    }
    
    //Some interanal Classes
    /**
     * Action to display the main panel where the scope(where to act the cluster
     * action) is chosen and scoring and finding parameters are modified
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
         * This method is called when the user wants to start the cluster process.
         *
         * @param event the event that the very Menu Item:"start cluster" Selected.
         */
        public void actionPerformed(ActionEvent event) {
            //display MCODEMainPanel in left cytopanel
            CytoscapeDesktop desktop = Cytoscape.getDesktop();
            CytoPanel cytoPanel = desktop.getCytoPanel(SwingConstants.WEST);//get the west cytopanel

            //First we check if the plugin has already been opened
            if (!opened) {
                //if the visual style has not already been loaded, we load it
                if (!vmm.getCalculatorCatalog().getVisualStyleNames().contains("Cluster")) {
                    vmm.getCalculatorCatalog().addVisualStyle(vistyle);
                }
                //The style is not actually applied until a result is produced (in MCODEScoreAndFindAction)
                mainPanel = new MainPanel(this, vistyle);
                URL iconURL = ClusterPlugin.class.getResource("resources/logo.gif");
                if (iconURL != null) {
                    ImageIcon icon = new ImageIcon(iconURL);
                    String tip = "网络聚类，请选择算法并填写相应参数!";
                    cytoPanel.add("ClusterViz", icon, mainPanel, tip);	//add the main panel together with a icon with text
                } else {
                    cytoPanel.add("Cluster", mainPanel);
                }
            } else {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "聚类插件已启动!");
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

        public void actionPerformed(ActionEvent e) {
            CytoscapeDesktop desktop = Cytoscape.getDesktop();
            CytoPanel cytoPanel = desktop.getCytoPanel(SwingConstants.SOUTH);//get the east panel
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
            cytoPanel=desktop.getCytoPanel(SwingConstants.WEST);
            //remove the main panel
            for (int c = cytoPanel.getCytoPanelComponentCount() - 1; c >= 0; c--) {
                cytoPanel.setSelectedIndex(c);
                Component component = cytoPanel.getSelectedComponent();
                if (component instanceof MainPanel) {
                    this.mainPanel = (MainPanel) component;
                    mainPanel.trigger.setOpened(false);
                    cytoPanel.remove(component);
                    break;
                }
            }
            desktop.dispose();
        }
    }    
    /**
     * Opens a browser connect to my blog page.
     */
    private class HelpAction implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent) {
            cytoscape.util.OpenBrowser.openURL("http://hi.baidu.com/cjry%5F8854/blog/item/1bf16f1e9db0b3fc1bd57683.html");
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
                    "<html><body>"+logoCode+"<P align=center><b>ClusterPlugin (April 2009) </b><BR>" +
                    "<i>An hierarchical clustering method for finding molecular complexes</i><BR>" +
                    "ClusterPlugin finds clusters (highly interconnected regions, protein complexes)<BR>" +
                    "in large protein interaction networks<BR>" +
                    "using maximal clique-based hierarchical algomerative clustering algorithm.<BR><BR>"+
                    "<a href='http://hi.baidu.com/cjry_8854/'>For references, view my blog^-^</a><BR><BR>"+
                    "</P></body></html>");
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

    public void test(){  }
    public static void main(String[] args){
    	ClusterPlugin cp=new ClusterPlugin(); 
    	cp.test();
    }/**/
}
