package clusterviz;

import cytoscape.Cytoscape;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelState;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;

import clusterviz.algorithmPanels.*;

public class MainPanel extends JPanel {
    ParameterSet currentParameters; // store panel fields
    ClusterPlugin.MainPanelAction trigger;
    ClusterVisualStyle vistyle;
    DecimalFormat decimal; // used in the formatted text fields
    JScrollPane algorithmPanel;
    CollapsiblePanel clusteringPanel;
    CollapsiblePanel customizePanel;
    JPanel clusteringContent;
    JPanel customizeClusteringContent;
    JPanel option1;
	EAGLEpanel EAGLE;
	FAGECPanel FAGEC;
    JPanel weakPanel;
    JPanel cliqueSizePanel;

    //resetable UI elements
    //Scoring
    JCheckBox includeLoops;
    JFormattedTextField degreeThreshold;
    //clustering
    JFormattedTextField kCore;
    JFormattedTextField nodeScoreThreshold;
    JRadioButton optimize;
    JRadioButton customize;

    JCheckBox haircut;
    JCheckBox fluff;
    JFormattedTextField nodeDensityThreshold;
    JFormattedTextField maxDepth;

    /**
     * @param trigger A reference to the action that triggered the initiation of this class
     * @param vistyle Reference to the used visual style
     */
    public MainPanel(ClusterPlugin.MainPanelAction trigger, ClusterVisualStyle vistyle) {
        this.trigger = trigger;
        this.vistyle = vistyle;
        setLayout(new BorderLayout());
        currentParameters = ParameterSet.getInstance().getParamsCopy(null);
        decimal = new DecimalFormat();
        decimal.setParseIntegerOnly(true);
        
        //TODO: to be implemented here
        //CytoscapeDesktop desktop = Cytoscape.getDesktop();
        //CytoPanel cytoPanel = desktop.getCytoPanel(SwingConstants.EAST);
        //cytoPanel.addCytoPanelListener(vistyle.new VisualStyleAction(cytoPanel, vistyle));/**/
        
        //create the four main panels: scope, algorithm, options, and bottom
        JPanel scopePanel = createScopePanel();
        algorithmPanel=createAlgorithmPanel();
        JPanel bottomPanel = createBottomPanel();
        add(scopePanel, BorderLayout.NORTH);
        add(algorithmPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        clusteringPanel.getContentPane().remove(clusteringContent);
        clusteringPanel.getContentPane().add(customizeClusteringContent, BorderLayout.NORTH);
	}

    /**
     * Creates a JPanel containing scope radio buttons
     *
     * @return panel containing the scope option buttons
     */
    private JPanel createScopePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Scope"));

        JRadioButton scopeNetwork = new JRadioButton("Whole Network", currentParameters.getScope().equals(ParameterSet.NETWORK));
        JRadioButton scopeSelection = new JRadioButton("Selected", currentParameters.getScope().equals(ParameterSet.SELECTION));

        scopeNetwork.setActionCommand(ParameterSet.NETWORK);
        scopeSelection.setActionCommand(ParameterSet.SELECTION);
        scopeNetwork.addActionListener(new ScopeAction());
        scopeSelection.addActionListener(new ScopeAction());
        ButtonGroup scopeOptions = new ButtonGroup();
        scopeOptions.add(scopeNetwork);
        scopeOptions.add(scopeSelection);
        panel.add(scopeNetwork);
        panel.add(scopeSelection);
        //panel.setPreferredSize(new java.awt.Dimension(185, 164));
        panel.setToolTipText("Please select scope for clustring");
        return panel;
    }
    private JScrollPane createAlgorithmPanel() {
    	JPanel choicePanel = new JPanel();
        choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.Y_AXIS));
        
        JRadioButton algorithm1 = new JRadioButton("MCODE", currentParameters.getAlgorithm().equals(ParameterSet.MCODE));
		JRadioButton EAGLEButton = new JRadioButton("EAGLE", currentParameters.getAlgorithm().equals(ParameterSet.EAGLE));
        JRadioButton FAGECButton = new JRadioButton("FAG-EC", currentParameters.getAlgorithm().equals(ParameterSet.FAGEC));
        algorithm1.setToolTipText("Use K-Core-based MCODE algorithm.\nA K-Core is a subgraph with minimum degree of k");
        EAGLEButton.setToolTipText("Use maximal clique-based EAGLE algorithm.\n Overlapped clusters can be identified");
        FAGECButton.setToolTipText("Use fast hierarchical agglomerative FAG-EC algorithm");
    
        algorithm1.setActionCommand(ParameterSet.MCODE);
        EAGLEButton.setActionCommand(ParameterSet.EAGLE);
        FAGECButton.setActionCommand(ParameterSet.FAGEC);
        algorithm1.addActionListener(new AlgorithmAction());
        EAGLEButton.addActionListener(new AlgorithmAction());
        FAGECButton.addActionListener(new AlgorithmAction());

        ButtonGroup algorithmOptions = new ButtonGroup();
        algorithmOptions.add(algorithm1);
        algorithmOptions.add(EAGLEButton);
        algorithmOptions.add(FAGECButton);
        choicePanel.add(FAGECButton);
        choicePanel.add(algorithm1);
        choicePanel.add(EAGLEButton);
        choicePanel.setToolTipText("Please select an algorithm");
        
        JPanel options=new JPanel();        
        options.setLayout(new BoxLayout(options,BoxLayout.Y_AXIS));
        option1=createOptionsPanel1();
        option1.setVisible(currentParameters.getAlgorithm().equals("MCODE"));
		EAGLE = new EAGLEpanel();
        EAGLE.setVisible(currentParameters.getAlgorithm().equals("EAGLE"));

		FAGEC = new FAGECPanel();
        FAGEC.setVisible(currentParameters.getAlgorithm().equals("FAG-EC"));
        options.add(option1);
        options.add(EAGLE);
        options.add(FAGEC);
        
        JPanel p=new JPanel();
        p.setLayout(new BorderLayout());
        p.add(choicePanel,BorderLayout.NORTH);
        p.add(options,BorderLayout.CENTER);
        JScrollPane scrollPanel = new JScrollPane(p);
        scrollPanel.setBorder(BorderFactory.createTitledBorder("Algorithm"));
        return scrollPanel;
    }

    /**
     * Creates a collapsible panel that holds 2 other collapsible panels 
     * for inputing scoring and clustering parameter
     *
     * @return collapsiblePanel
     */
    private JPanel createOptionsPanel1() {
    	JPanel retPanel=new JPanel();
        retPanel.setLayout(new BorderLayout());
        retPanel.setBorder(BorderFactory.createTitledBorder(""));
    	
        CollapsiblePanel collapsiblePanel = new CollapsiblePanel("MCODE Options");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        CollapsiblePanel createScoringPanel = createScoringPanel();
        clusteringPanel = createClusteringPanel();
        panel.add(createScoringPanel);
        panel.add(clusteringPanel);
        collapsiblePanel.getContentPane().add(panel,BorderLayout.NORTH);
        collapsiblePanel.setToolTipText("Customize clustering parameters (Optional)");
        retPanel.add(collapsiblePanel);
        return retPanel;
    }    


    
    /**
     * Create a collapsible panel that holds network scoring parameter inputs
     *
     * @return panel containing the network scoring parameter inputs
     */
    private CollapsiblePanel createScoringPanel() {
        CollapsiblePanel collapsiblePanel = new CollapsiblePanel("Scoring");        
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1));
        
        //Include Loop input
        JLabel includeLoopsLabel = new JLabel("Include Loop");
        includeLoops = new JCheckBox() {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        includeLoops.addItemListener(new MainPanel.IncludeLoopsCheckBoxAction());
        String includeLoopsTip = "Regard loops when clustering.\n" +
                "This will influence the score of cliques";
        includeLoops.setToolTipText(includeLoopsTip);
        includeLoops.setSelected(currentParameters.isIncludeLoops());
        JPanel includeLoopsPanel = new JPanel() {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        includeLoopsPanel.setLayout(new BorderLayout());
        includeLoopsPanel.setToolTipText(includeLoopsTip);
        includeLoopsPanel.add(includeLoopsLabel, BorderLayout.WEST);
        includeLoopsPanel.add(includeLoops, BorderLayout.EAST);

        //Degree Threshold input
        JLabel degreeThresholdLabel = new JLabel("Degree Threshold");
        degreeThreshold = new JFormattedTextField(decimal) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        degreeThreshold.setColumns(3);
        degreeThreshold.addPropertyChangeListener("value", new MainPanel.FormattedTextFieldAction());
        String degreeThresholdTip = "degree cutoff of the nodes";
        degreeThreshold.setToolTipText(degreeThresholdTip);
        degreeThreshold.setText((new Integer(currentParameters.getDegreeThreshold()).toString()));
        JPanel degreeThresholdPanel = new JPanel() {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        degreeThresholdPanel.setLayout(new BorderLayout());
        degreeThresholdPanel.setToolTipText(degreeThresholdTip);
        degreeThresholdPanel.add(degreeThresholdLabel, BorderLayout.WEST);
        degreeThresholdPanel.add(degreeThreshold, BorderLayout.EAST);
        
        //add the components to the panel
        panel.add(includeLoopsPanel);
        panel.add(degreeThresholdPanel);
        collapsiblePanel.getContentPane().add(panel, BorderLayout.NORTH);
        return collapsiblePanel;
    }

    /**
     * Creates a collapsible panel that holds 2 other collapsible panels for 
     * either customizing or optimized clustering parameters
     *
     */
    private CollapsiblePanel createClusteringPanel() {
        CollapsiblePanel collapsiblePanel = new CollapsiblePanel("Clustering");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        customize = new JRadioButton("Customize", !currentParameters.isOptimize());
        optimize = new JRadioButton("Optimize", currentParameters.isOptimize());
        ButtonGroup clusteringChoice = new ButtonGroup();
        clusteringChoice.add(customize);
        clusteringChoice.add(optimize);
        customize.addActionListener(new CustomizeAction());
        optimize.addActionListener(new CustomizeAction());
        
        customizePanel = createClusterParaPanel(customize);
        CollapsiblePanel optimalPanel = createOptimizePanel(optimize);
        panel.add(customizePanel);
        panel.add(optimalPanel);        
        this.clusteringContent = panel;        
        collapsiblePanel.getContentPane().add(panel, BorderLayout.NORTH);
        return collapsiblePanel;
    }

    /**
     * Creates a collapsible panel that holds clustering parameters
     * placed within the cluster finding collapsible panel
     *
     * @param component Any JComponent that may appear in the titled border of the panel
     * @return collapsablePanel
     */
    private CollapsiblePanel createClusterParaPanel(JRadioButton component) {
        CollapsiblePanel collapsiblePanel = new CollapsiblePanel(component);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        //Node Score Threshold
        String nodeScoreThresholdTip = "Sets the acceptable score deviance from\n" +
        		"the seed node's score for expanding a cluster\n" +
        		"(most influental parameter for cluster size).";
        JLabel nodeScoreThresholdLabel = new JLabel("NodeScoreThreshold");
        nodeScoreThreshold = new JFormattedTextField(new DecimalFormat("0.000")) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        nodeScoreThreshold.setColumns(3);
        nodeScoreThreshold.addPropertyChangeListener("value", new MainPanel.FormattedTextFieldAction());
        nodeScoreThreshold.setToolTipText(nodeScoreThresholdTip);
        nodeScoreThreshold.setText((new Double(currentParameters.getNodeScoreCutoff()).toString()));
        JPanel nodeScoreThresholdPanel = new JPanel(new BorderLayout()) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        nodeScoreThresholdPanel.setToolTipText(nodeScoreThresholdTip);
        nodeScoreThresholdPanel.add(nodeScoreThresholdLabel, BorderLayout.WEST);
        nodeScoreThresholdPanel.add(nodeScoreThreshold,BorderLayout.EAST);

        //K-Core input
        JLabel kCoreLabel = new JLabel("K-CoreThreshold");
        kCore = new JFormattedTextField(decimal) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        kCore.setColumns(3);
        kCore.addPropertyChangeListener("value", new MainPanel.FormattedTextFieldAction());
        String kCoreTip = "Filters out clusters lacking a\n" +
        		"maximally inter-connected core\n" +
        		"of at least k edges per node.";
        kCore.setToolTipText(kCoreTip);
        kCore.setText((new Integer(currentParameters.getKCore()).toString()));
        JPanel kCorePanel = new JPanel(new BorderLayout()) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        kCorePanel.setToolTipText(kCoreTip);
        kCorePanel.add(kCoreLabel, BorderLayout.WEST);
        kCorePanel.add(kCore, BorderLayout.EAST);

        //Haircut Input
        String haircutTip = "Remove singly connected\n" +
        		"nodes from clusters.";
        JLabel haircutLabel = new JLabel("Haircut");
        haircut = new JCheckBox() {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        haircut.addItemListener(new MainPanel.HaircutCheckBoxAction());
        haircut.setToolTipText(haircutTip);
        haircut.setSelected(currentParameters.isHaircut());
        JPanel haircutPanel = new JPanel(new BorderLayout()) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        }; 
        haircutPanel.setToolTipText(haircutTip);
        haircutPanel.add(haircutLabel, BorderLayout.WEST);
        haircutPanel.add(haircut, BorderLayout.EAST);

        //Fluffy Input
        JLabel fluffLabel = new JLabel("Fluff");
        fluff = new JCheckBox() {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        fluff.addItemListener(new MainPanel.FluffCheckBoxAction());
        String fluffTip = "Expand core cluster by one\n" +
        		"neighbour shell (applied\n"+
        		"after the optional haircut).";
        fluff.setToolTipText(fluffTip);
        fluff.setSelected(currentParameters.isFluff());
        JPanel fluffPanel = new JPanel(new BorderLayout()) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        fluffPanel.setToolTipText(fluffTip);
        fluffPanel.add(fluffLabel, BorderLayout.WEST);
        fluffPanel.add(fluff, BorderLayout.EAST);

        //Fluff node density cutoff input
        JLabel nodeDensityThresholdLabel = new JLabel("threshold");
        nodeDensityThreshold = new JFormattedTextField(new DecimalFormat("0.000")) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        nodeDensityThreshold.setColumns(3);
        nodeDensityThreshold.addPropertyChangeListener("value", new MainPanel.FormattedTextFieldAction());
        String fluffNodeDensityCutoffTip = "Limits fluffing by setting the acceptable\n" +
        		"node density deviance from the core cluster\n" +
        		"density (allows clusters' edges to overlap).";
        nodeDensityThreshold.setToolTipText(fluffNodeDensityCutoffTip);
        nodeDensityThreshold.setText((new Double(currentParameters.getFluffNodeDensityCutoff()).toString()));
        JPanel nodeDensityThresholdPanel = new JPanel(new BorderLayout()) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        nodeDensityThresholdPanel.setToolTipText(fluffNodeDensityCutoffTip);
        nodeDensityThresholdPanel.add(nodeDensityThresholdLabel, BorderLayout.WEST);
        nodeDensityThresholdPanel.add(nodeDensityThreshold, BorderLayout.EAST);
        nodeDensityThresholdPanel.setVisible(currentParameters.isFluff());

        //Max depth input
        JLabel maxDepthLabel = new JLabel("MaxDepth");
        maxDepth = new JFormattedTextField(decimal) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        maxDepth.setColumns(3);
        maxDepth.addPropertyChangeListener("value", new MainPanel.FormattedTextFieldAction());
        String maxDepthTip = "Limits the cluster size by setting the\n" +
        		"maximum search distance from a seed\n" +
        		"node (100 virtually means no limit).";
        maxDepth.setToolTipText(maxDepthTip);
        maxDepth.setText((new Integer(currentParameters.getMaxDepthFromStart()).toString()));
        JPanel maxDepthPanel = new JPanel(new BorderLayout()) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        maxDepthPanel.setToolTipText(maxDepthTip);
        maxDepthPanel.add(maxDepthLabel, BorderLayout.WEST);
        maxDepthPanel.add(maxDepth, BorderLayout.EAST);
        
        panel.add(haircutPanel);
        panel.add(fluffPanel);
        panel.add(nodeDensityThresholdPanel);
        panel.add(nodeScoreThresholdPanel);
        panel.add(kCorePanel);
        panel.add(maxDepthPanel);
        this.customizeClusteringContent = panel;

        collapsiblePanel.getContentPane().add(panel, BorderLayout.NORTH);
        return collapsiblePanel;
    }

    /**
     * Creates a collapsible panel that holds a benchmark file input, placed within the cluster finding collapsible panel
     *
     * @param component the radio button that appears in the titled border of the panel
     * @return A collapsible panel holding a file selection input
     * @see CollapsiblePanel
     */
    private CollapsiblePanel createOptimizePanel(JRadioButton component) {
        CollapsiblePanel collapsiblePanel = new CollapsiblePanel(component);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel benchmarkStarter = new JLabel("Benchmark file location");
        JPanel benchmarkStarterPanel = new JPanel(new BorderLayout());
        benchmarkStarterPanel.add(benchmarkStarter, BorderLayout.WEST);
        JFormattedTextField benchmarkFileLocation = new JFormattedTextField();
        JButton browseButton = new JButton("Browse...");
        JPanel fileChooserPanel = new JPanel(new BorderLayout());
        fileChooserPanel.add(benchmarkFileLocation, BorderLayout.SOUTH);
        fileChooserPanel.add(browseButton, BorderLayout.EAST);
        panel.add(benchmarkStarterPanel);
        panel.add(fileChooserPanel);
        collapsiblePanel.getContentPane().add(panel, BorderLayout.NORTH);
        return collapsiblePanel;
    }

    /**
     * Utility method that creates a panel for buttons at the bottom of the <code>MainPanel</code>
     *
     * @return a flow layout panel containing the analyze and quite buttons
     */
    private JPanel createBottomPanel() {
    	JPanel bottomPanel=new JPanel();
    	bottomPanel.setLayout(new BoxLayout(bottomPanel,BoxLayout.Y_AXIS));
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        JButton analyzeButton = new JButton("Analyze");
        analyzeButton.setToolTipText("start the process of analyze");
        analyzeButton.addActionListener(new AnalyzeAction(currentParameters, vistyle));        
        panel.add(analyzeButton);
        
        JButton closeButton = new JButton("Close");
        closeButton.setToolTipText("terminate the plugin");
       	panel.add(closeButton);
        closeButton.addActionListener(new MainPanel.CloseAction(this));
        bottomPanel.add(panel);
        
        JPanel vacantContainer = new JPanel(new BorderLayout());
        bottomPanel.add(vacantContainer);
        vacantContainer.setPreferredSize(new java.awt.Dimension(127, 90));
        return bottomPanel;
    }

    /**
     * Handles the press of a scope option. 
     */
    private class ScopeAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            String scope = e.getActionCommand();
            currentParameters.setScope(scope);
        }
    }
    /**
     * Handles the press of a algorithm option. Makes sure that appropriate options
     * inputs are added and removed depending on which algorithm is selected
     */
    private class AlgorithmAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            String algorithm = e.getActionCommand();
            currentParameters.setAlgorithm(algorithm);
            option1.setVisible(currentParameters.getAlgorithm().equals(ParameterSet.MCODE));
            EAGLE.setVisible(currentParameters.getAlgorithm().equals(ParameterSet.EAGLE));
            FAGEC.setVisible(currentParameters.getAlgorithm().equals(ParameterSet.FAGEC));
        }
    }

    /**
    /**
     * Sets the optimization parameter depending on which radio button is selected (cusomize/optimize)
     */
    private class CustomizeAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            if (optimize.isSelected()) {
                currentParameters.setOptimize(true);
            } else {
                currentParameters.setOptimize(false);
            }
        }
    }
    /**
     * Handles the press of the Close button
     */
    private class CloseAction extends AbstractAction {
        MainPanel mainPanel;
        ResultPanel component;
        CloseAction (MainPanel mainPanel) {
            this.mainPanel = mainPanel;
        }
        public void actionPerformed(ActionEvent e) {
            //close all open panels
            CytoscapeDesktop desktop = Cytoscape.getDesktop();
            boolean resultsClosed = true;
            CytoPanel cytoPanel = desktop.getCytoPanel(SwingConstants.EAST);
            for (int c = cytoPanel.getCytoPanelComponentCount() - 1; c >= 0; c--) {
                cytoPanel.setSelectedIndex(c);
                Component component = cytoPanel.getSelectedComponent();
                String componentTitle;
                if (component instanceof ResultPanel) {
                    this.component = (ResultPanel) component;
                    componentTitle = this.component.getResultTitle();
                    String message = "Close" + componentTitle + ".\nContiune?";
                    int result = JOptionPane.showOptionDialog(Cytoscape.getDesktop(), new Object[] {message}, "Comfirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
                    if (result == JOptionPane.YES_OPTION){
                        cytoPanel.remove(component);
                        ParameterSet.removeResultParams(componentTitle);
                    } else {
                        resultsClosed = false;
                    }
                }
            }
            if (cytoPanel.getCytoPanelComponentCount() == 0) {
                cytoPanel.setState(CytoPanelState.HIDE);
            }
            if (resultsClosed) {
                cytoPanel = desktop.getCytoPanel(SwingConstants.WEST);
                cytoPanel.remove(mainPanel);
                trigger.setOpened(false);
            }
        }
    }

    /**
     * Handles setting of the include loops parameter
     */
    private class IncludeLoopsCheckBoxAction implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                currentParameters.setIncludeLoops(false);
            } else {
                currentParameters.setIncludeLoops(true);
            }
        }
    }

    /**
     * Handles setting for the text field parameters that are numbers.
     * Makes sure that the numbers make sense.
     */
    private class FormattedTextFieldAction implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            JFormattedTextField source = (JFormattedTextField) e.getSource();
            String message = "Invaluled input\n";
            boolean invalid = false;
            
            if (source == degreeThreshold) {
                Number value = (Number) degreeThreshold.getValue();
                if ((value != null) && (value.intValue() > 1)) {
                    currentParameters.setDegreeThreshold(value.intValue());
                } else {
                    source.setValue(new Integer (2));
                    message += "the node degree cutoff should no less than 2.";
                    invalid = true;
                }
            } else if (source == nodeScoreThreshold) {
                Number value = (Number) nodeScoreThreshold.getValue();
                if ((value != null) && (value.doubleValue() >= 0.0) && (value.doubleValue() <= 1.0)) {
                    currentParameters.setNodeScoreCutoff(value.doubleValue());
                } else {
                    source.setValue(new Double (currentParameters.getNodeScoreCutoff()));
                    message += "the node score cutoff should set between 0 and 1.";
                    invalid = true;
                }
            } else if (source == kCore) {
                Number value = (Number) kCore.getValue();
                if ((value != null) && (value.intValue() > 1)) {
                    currentParameters.setKCore(value.intValue());
                } else {
                    source.setValue(new Integer (2));
                    message += "the k value of K-Core should be greater than 1.";
                    invalid = true;
                }
            } else if (source == maxDepth) {
                Number value = (Number) maxDepth.getValue();
                if ((value != null) && (value.intValue() > 0)) {
                    currentParameters.setMaxDepthFromStart(value.intValue());
                } else {
                    source.setValue(new Integer (1));
                    message += "max depth should be no less than 1.";
                    invalid = true;
                }
            } else if (source == nodeDensityThreshold) {
                Number value = (Number) nodeDensityThreshold.getValue();
                if ((value != null) && (value.doubleValue() >= 0.0) && (value.doubleValue() <= 1.0)) {
                    currentParameters.setFluffNodeDensityCutoff(value.doubleValue());
                } else {
                    source.setValue(new Double (currentParameters.getFluffNodeDensityCutoff()));
                    message += "fluff node density cutoff should\n" +
                    		"be set between 0 and 1.";
                    invalid = true;
                }
            }
			if (invalid) {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(), message, "paramter out of boundary", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Handles setting of the haircut parameter
     */
    private class HaircutCheckBoxAction implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                currentParameters.setHaircut(false);
            } else {
                currentParameters.setHaircut(true);
            }
        }
    }

    /**
     * Handles setting of the fluff parameter and showing or hiding of the fluff node density cutoff input
     */
    private class FluffCheckBoxAction implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                currentParameters.setFluff(false);
            } else {
                currentParameters.setFluff(true);
            }
            nodeDensityThreshold.getParent().setVisible(currentParameters.isFluff());
        }
    }
    
	public ClusterPlugin.MainPanelAction getTrigger() {
		return trigger;
	}

	public void setTrigger(ClusterPlugin.MainPanelAction trigger) {
		this.trigger = trigger;
	}
}
