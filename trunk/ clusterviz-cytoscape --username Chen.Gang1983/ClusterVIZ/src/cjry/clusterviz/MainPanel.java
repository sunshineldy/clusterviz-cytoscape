package cjry.clusterviz;

import cytoscape.Cytoscape;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelListener;
import cytoscape.view.cytopanels.CytoPanelState;
import cytoscape.visual.VisualMappingManager;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;

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
    JPanel option2;
    JPanel option3;
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
    //EAGLE
    JFormattedTextField cliqueSizeThreshold1;
    JFormattedTextField complexSizeThreshold1;
    //FAG-EC
    JCheckBox overlapped;
    JFormattedTextField fThreshold;
    JFormattedTextField cliqueSizeThreshold;
    JFormattedTextField complexSizeThreshold;
    JRadioButton weak; // use weak module definition
    JRadioButton strong; // use strong module definition

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
        currentParameters.setDefaultParams();
        decimal = new DecimalFormat();
        decimal.setParseIntegerOnly(true);
        
        /*CytoscapeDesktop desktop = Cytoscape.getDesktop();
        CytoPanel cytoPanel = desktop.getCytoPanel(SwingConstants.SOUTH);
        cytoPanel.addCytoPanelListener(new VisualStyleAction(cytoPanel, vistyle));*/

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
        panel.setToolTipText("选择聚类分析范围");
        return panel;
    }
    private JScrollPane createAlgorithmPanel() {
    	JPanel choicePanel = new JPanel();
        choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.Y_AXIS));
        
        JRadioButton algorithm1 = new JRadioButton("K-Core", currentParameters.getAlgorithm().equals(ParameterSet.KCORE)){
            public JToolTip createToolTip() {
                return new MyTipTool();
            }};
        JRadioButton algorithm2 = new JRadioButton("EAGLE", currentParameters.getAlgorithm().equals(ParameterSet.EAGLE));
        JRadioButton algorithm3 = new JRadioButton("FAG-EC", currentParameters.getAlgorithm().equals(ParameterSet.FAGEC));
        algorithm1.setToolTipText("使用MCODE基于种子点的k-团聚类算法\n一个k-团就是一个所有节点度都不小k的子图");
        algorithm2.setToolTipText("使用EAGLE算法识别出层次化可交叠的模块结构");
        algorithm3.setToolTipText("使用FAG-EC层次化快速聚类算法进行分析");
    
        algorithm1.setActionCommand(ParameterSet.KCORE);
        algorithm2.setActionCommand(ParameterSet.EAGLE);
        algorithm3.setActionCommand(ParameterSet.FAGEC);
        algorithm1.addActionListener(new AlgorithmAction());
        algorithm2.addActionListener(new AlgorithmAction());
        algorithm3.addActionListener(new AlgorithmAction());

        ButtonGroup algorithmOptions = new ButtonGroup();
        algorithmOptions.add(algorithm1);
        algorithmOptions.add(algorithm2);
        algorithmOptions.add(algorithm3);
        choicePanel.add(algorithm1);
        choicePanel.add(algorithm2);
        choicePanel.add(algorithm3);
        choicePanel.setToolTipText("选择聚类算法");
        
        JPanel options=new JPanel();        
        options.setLayout(new BoxLayout(options,BoxLayout.Y_AXIS));
        option1=createOptionsPanel1();
        option1.setVisible(currentParameters.getAlgorithm().equals("K-Core"));
        option2=createOptionsPanel2();
        option2.setVisible(currentParameters.getAlgorithm().equals("EAGLE"));
        option3=createOptionsPanel3();
        option3.setVisible(currentParameters.getAlgorithm().equals("FAG-EC"));
        options.add(option1);
        options.add(option2);
        options.add(option3);
        
        JPanel p=new JPanel();
        p.setLayout(new BorderLayout());
        p.add(choicePanel,BorderLayout.NORTH);
        p.add(options,BorderLayout.CENTER);
        JScrollPane scrollPanel = new JScrollPane(p);
        scrollPanel.setBorder(BorderFactory.createTitledBorder("Algorithm"));
        return scrollPanel;     
    	/*JPanel panel=new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Algorithm"));
        panel.add(scrollPanel,BorderLayout.SOUTH);*/
    }

    /**
     * Creates a collapsible panel that holds 2 other collapsible panels 
     * for inputing scoring and clustering parameter
     *
     * @return collapsablePanel
     */
    private JPanel createOptionsPanel1() {
    	JPanel retPanel=new JPanel();
        retPanel.setLayout(new BorderLayout());
        retPanel.setBorder(BorderFactory.createTitledBorder(""));
    	
        CollapsiblePanel collapsiblePanel = new CollapsiblePanel("K-Cire Options");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        CollapsiblePanel createScoringPanel = createScoringPanel();
        clusteringPanel = createClusteringPanel();
        panel.add(createScoringPanel);
        panel.add(clusteringPanel);
        collapsiblePanel.getContentPane().add(panel,BorderLayout.NORTH);
        collapsiblePanel.setToolTipText("自定义聚类参数（可选）");
        retPanel.add(collapsiblePanel);
        return retPanel;
    }    
    private JPanel createOptionsPanel2() {
    	JPanel panel=new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(""));
        //the collapsible panel
        CollapsiblePanel collapsiblePanel = new CollapsiblePanel("EAGLE Options");
        JPanel cliqueSizePanel1=createCliqueSizePanel1();
        JPanel complexSizePanel1=createComplexSizePanel1();
        collapsiblePanel.getContentPane().add(cliqueSizePanel1, BorderLayout.NORTH);
        collapsiblePanel.getContentPane().add(complexSizePanel1, BorderLayout.CENTER);
        collapsiblePanel.setToolTipText("自定义EAGLE聚类算法参数（可选）");
        panel.add(collapsiblePanel);
        return panel;
    }

    private JPanel createOptionsPanel3() {
    	JPanel retPanel=new JPanel();
        retPanel.setLayout(new BorderLayout());
        retPanel.setBorder(BorderFactory.createTitledBorder(""));
        
        //the collapsible panel
        CollapsiblePanel collapsiblePanel = new CollapsiblePanel("FAG-EC Options");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("")); 
        
        //the radio botton panel
        JPanel functionPanel=new JPanel();
        functionPanel.setLayout(new BorderLayout());
        //the radio bottons
        weak = new JRadioButton("weak",true);
        weak.setToolTipText("使用弱模块定义");
        strong = new JRadioButton("strong", false);
        strong.setToolTipText("使用强模块定义");
        ButtonGroup choices = new ButtonGroup();
        choices.add(weak);
        choices.add(strong);
        weak.addActionListener(new FunctionAction());
        strong.addActionListener(new FunctionAction());
        functionPanel.add(weak,BorderLayout.WEST);
        functionPanel.add(strong,BorderLayout.CENTER);        
        
        //the weak module definition parameter input Panel
        weakPanel = new JPanel();
        weakPanel.setLayout(new BorderLayout());
        //the label
        JLabel label=new JLabel("   threshold");  //Clique Size Threshold input
        fThreshold = new JFormattedTextField(decimal) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        //the input text field
        fThreshold.setColumns(3);
        fThreshold.addPropertyChangeListener("value", new MainPanel.FormattedTextFieldAction());
        String tip2 = "弱模块定义中使用的阈值\n" +
                "为模块内所有节点入度和\n"+
                "与出度和的比";
        fThreshold.setToolTipText(tip2);
        fThreshold.setText((new Double(currentParameters.getFThreshold()).toString()));
        weakPanel.add(fThreshold,BorderLayout.EAST); 
        weakPanel.add(label,BorderLayout.WEST);  
        weakPanel.setVisible(true);
        
        //the cliqueSize Panel
        cliqueSizePanel=createCliqueSizePanel();
        //the ComplexSize Panel
        JPanel complexSizePanel=createComplexSizePanel();
        
        //Produce Overlapped Complexes input
        JLabel overlapLabel = new JLabel(" Produce Overlapped Complexes");
        overlapped = new JCheckBox() {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        overlapped.addItemListener(new overlappedCheckBoxAction());
        String overlapTip = "是否生成有交叠的模块.\n" +
                "使用基于极大团的算法实现";
        overlapped.setToolTipText(overlapTip);
        overlapped.setSelected(currentParameters.isOverlapped());
        JPanel overlapPanel = new JPanel() {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        overlapPanel.setLayout(new BorderLayout());
        overlapPanel.setToolTipText(overlapTip);
        overlapPanel.add(overlapLabel, BorderLayout.WEST);
        overlapPanel.add(overlapped, BorderLayout.EAST);
 
        panel.add(functionPanel);
        panel.add(weakPanel);  
        panel.add(complexSizePanel);
        panel.add(overlapPanel);      
        panel.add(cliqueSizePanel);
        cliqueSizePanel.setVisible(currentParameters.isOverlapped());
        collapsiblePanel.getContentPane().add(panel, BorderLayout.NORTH);
        collapsiblePanel.setToolTipText("自定义FAG-EC算法参数（可选）");
        retPanel.add(collapsiblePanel);
        return retPanel;
    }

    private JPanel createComplexSizePanel1(){
        JPanel panel=new JPanel();
        panel.setLayout(new BorderLayout());        
        //the label
        JLabel sizeThresholdLabel2=new JLabel(" ComplexSize Threshold");  //Clique Size Threshold input
        //the input text field
        complexSizeThreshold1= new JFormattedTextField(decimal) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        complexSizeThreshold1.setColumns(3);
        complexSizeThreshold1.addPropertyChangeListener("value", new MainPanel.FormattedTextFieldAction());
        String tip3 = "定义输出模块的最小尺寸\n" +
                "不输出小于此值的模块";
        complexSizeThreshold1.setToolTipText(tip3);
        complexSizeThreshold1.setText((new Integer(currentParameters.getComplexSizeThreshold1()).toString()));
        panel.add(sizeThresholdLabel2,BorderLayout.WEST);
        panel.add(complexSizeThreshold1,BorderLayout.EAST);
        return panel;
    }
    private JPanel createCliqueSizePanel1(){
    	JPanel panel;
        //the label
        JLabel sizeThresholdLabel=new JLabel(" CliqueSize Threshold");  //Clique Size Threshold input
        cliqueSizeThreshold1 = new JFormattedTextField(decimal) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        //the input text field
        cliqueSizeThreshold1.setColumns(3);
        cliqueSizeThreshold1.addPropertyChangeListener("value", new MainPanel.FormattedTextFieldAction());
        String tip = "定义极大团的最小尺寸\n" +
                "小于此值的极大团将被\n"+
                "视为边界团而过滤掉\n"+
                "一般设为2~5";
        cliqueSizeThreshold1.setToolTipText(tip);
        cliqueSizeThreshold1.setText((new Integer(currentParameters.getCliqueSizeThreshold1()).toString()));
        //the panel 
        panel = new JPanel() {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        //add the components to the panel
        panel.setLayout(new BorderLayout());
        panel.setToolTipText(tip);
        panel.add(sizeThresholdLabel, BorderLayout.WEST);
        panel.add(cliqueSizeThreshold1, BorderLayout.EAST);
    	return panel;
    }
    private JPanel createComplexSizePanel(){
        JPanel panel=new JPanel();
        panel.setLayout(new BorderLayout());        
        //the label
        JLabel sizeThresholdLabel2=new JLabel(" ComplexSize Threshold");  //Clique Size Threshold input
        //the input text field
        complexSizeThreshold = new JFormattedTextField(decimal) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        complexSizeThreshold.setColumns(3);
        complexSizeThreshold.addPropertyChangeListener("value", new MainPanel.FormattedTextFieldAction());
        String tip3 = "定义输出模块的最小尺寸\n" +
                "不输出小于此值的模块";
        complexSizeThreshold.setToolTipText(tip3);
        complexSizeThreshold.setText((new Integer(currentParameters.getComplexSizeThreshold()).toString()));
        panel.add(sizeThresholdLabel2,BorderLayout.WEST);
        panel.add(complexSizeThreshold,BorderLayout.EAST);
        return panel;
    }
    private JPanel createCliqueSizePanel(){
    	JPanel panel;
        //the label
        JLabel sizeThresholdLabel=new JLabel(" CliqueSize Threshold");  //Clique Size Threshold input
        cliqueSizeThreshold = new JFormattedTextField(decimal) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        //the input text field
        cliqueSizeThreshold.setColumns(3);
        cliqueSizeThreshold.addPropertyChangeListener("value", new MainPanel.FormattedTextFieldAction());
        String tip = "定义极大团的最小尺寸\n" +
                "一般设为2~5";
        cliqueSizeThreshold.setToolTipText(tip);
        cliqueSizeThreshold.setText((new Integer(currentParameters.getCliqueSizeThreshold()).toString()));
        //the panel 
        panel = new JPanel() {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        //add the components to the panel
        panel.setLayout(new BorderLayout());
        panel.setToolTipText(tip);
        panel.add(sizeThresholdLabel, BorderLayout.WEST);
        panel.add(cliqueSizeThreshold, BorderLayout.EAST);
    	return panel;
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
        String includeLoopsTip = "聚类时是否包含环.\n" +
                "这将对团的分值产生影响";
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
        String degreeThresholdTip = "定义待计算的节点所需达到的\n" +
                "度数下限";
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
     * Creates a collapsable panel that holds 2 other collapsable panels for 
     * either customizing or optimized clustering parameters
     *
     */
    private CollapsiblePanel createClusteringPanel() {
        CollapsiblePanel collapsiblePanel = new CollapsiblePanel("Clustering");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        customize = new JRadioButton("自定义", !currentParameters.isOptimize());
        optimize = new JRadioButton("选择最优", currentParameters.isOptimize());
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
     * Creates a collapsable panel that holds clustering parameters
     * placed within the cluster finding collapsable panel
     *
     * @param component Any JComponent that may appear in the titled border of the panel
     * @return collapsablePanel
     */
    private CollapsiblePanel createClusterParaPanel(JRadioButton component) {
        CollapsiblePanel collapsiblePanel = new CollapsiblePanel(component);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        //Node Score Threshold
        String nodeScoreThresholdTip = "  聚类分析时所允许的 \n" +
        "距离种子节点分值的最大差值\n" +
        "(决定最终模块大小的重要参数)";
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
        String kCoreTip = "定义一个团内联度数的最小值\n" +
                "小于此值的团将被过滤掉";
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
        String haircutTip = "是否移除团内存在的\n" +
        "简单连接点";
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

        //Fluff Input
        JLabel fluffLabel = new JLabel("Fluff");
        fluff = new JCheckBox() {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        fluff.addItemListener(new MainPanel.FluffCheckBoxAction());
        String fluffTip = "是否通过一个邻居节点核\n" +
                "对核心团进行扩展\n"+
                "(选择此项需先选定haircut选项).";
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
        String fluffNodeDensityCutoffTip = "设置可接受的节点密度\n" +
                "与核心团密度的最大偏离值" +
                "以限制膨松化程度"+
                "（允许不同团之间边的交叠）";
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
        String maxDepthTip = "设定距离种子节点的最大搜索深度\n" +
                "以限制所得团的尺寸大小\n" +
                "(100视为不限制深度).";
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
     * Creates a collapsable panel that holds a benchmark file input, placed within the cluster finding collapsable panel
     *
     * @param component the radio button that appears in the titled border of the panel
     * @return A collapsable panel holding a file selection input
     * @see CollapsiblePanel
     */
    private CollapsiblePanel createOptimizePanel(JRadioButton component) {
        CollapsiblePanel collapsiblePanel = new CollapsiblePanel(component);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel benchmarkStarter = new JLabel("基准文件位置");
        JPanel benchmarkStarterPanel = new JPanel(new BorderLayout());
        benchmarkStarterPanel.add(benchmarkStarter, BorderLayout.WEST);
        JFormattedTextField benchmarkFileLocation = new JFormattedTextField();
        JButton browseButton = new JButton("浏览...");
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
        analyzeButton.setToolTipText("开始分析");
        analyzeButton.addActionListener(new AnalyzeAction(currentParameters, vistyle));        
        panel.add(analyzeButton);
        
        JButton closeButton = new JButton("Close");
        closeButton.setToolTipText("关闭本插件");
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
            option1.setVisible(currentParameters.getAlgorithm().equals(ParameterSet.KCORE));
            option2.setVisible(currentParameters.getAlgorithm().equals(ParameterSet.EAGLE));
            option3.setVisible(currentParameters.getAlgorithm().equals(ParameterSet.FAGEC));
        }
    }

    private class FunctionAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            if (weak.isSelected()) {
                currentParameters.setWeak(true);
                weakPanel.setVisible(true);
            } else {
                currentParameters.setWeak(false);
                weakPanel.setVisible(false);
            }
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
            CytoPanel cytoPanel = desktop.getCytoPanel(SwingConstants.SOUTH);
            for (int c = cytoPanel.getCytoPanelComponentCount() - 1; c >= 0; c--) {
                cytoPanel.setSelectedIndex(c);
                Component component = cytoPanel.getSelectedComponent();
                String componentTitle;
                if (component instanceof ResultPanel) {
                    this.component = (ResultPanel) component;
                    componentTitle = this.component.getResultTitle();
                    String message = "将要关闭结果" + componentTitle + ".\n是否继续?";
                    int result = JOptionPane.showOptionDialog(Cytoscape.getDesktop(), new Object[] {message}, "确定", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
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
     * Handles setting of the produce-overlapped-complexes parameter
     */
    private class overlappedCheckBoxAction implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                currentParameters.setOverlapped(true);
                cliqueSizePanel.setVisible(true);
            } else {
                currentParameters.setOverlapped(false);
                cliqueSizePanel.setVisible(false);
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
            String message = "输入非法！\n";
            boolean invalid = false;
            
            if (source == degreeThreshold) {
                Number value = (Number) degreeThreshold.getValue();
                if ((value != null) && (value.intValue() > 1)) {
                    currentParameters.setDegreeThreshold(value.intValue());
                } else {
                    source.setValue(new Integer (2));
                    message += "节点度的阈值需大于1.";
                    invalid = true;
                }
            } else if (source == nodeScoreThreshold) {
                Number value = (Number) nodeScoreThreshold.getValue();
                if ((value != null) && (value.doubleValue() >= 0.0) && (value.doubleValue() <= 1.0)) {
                    currentParameters.setNodeScoreCutoff(value.doubleValue());
                } else {
                    source.setValue(new Double (currentParameters.getNodeScoreCutoff()));
                    message += "分值的阈值需介于0与1之间.";
                    invalid = true;
                }
            } else if (source == kCore) {
                Number value = (Number) kCore.getValue();
                if ((value != null) && (value.intValue() > 1)) {
                    currentParameters.setKCore(value.intValue());
                } else {
                    source.setValue(new Integer (2));
                    message += "K-Core中的k值必须大于1.";
                    invalid = true;
                }
            } else if (source == maxDepth) {
                Number value = (Number) maxDepth.getValue();
                if ((value != null) && (value.intValue() > 0)) {
                    currentParameters.setMaxDepthFromStart(value.intValue());
                } else {
                    source.setValue(new Integer (1));
                    message += "最大深度值需大于0.";
                    invalid = true;
                }
            } else if (source == nodeDensityThreshold) {
                Number value = (Number) nodeDensityThreshold.getValue();
                if ((value != null) && (value.doubleValue() >= 0.0) && (value.doubleValue() <= 1.0)) {
                    currentParameters.setFluffNodeDensityCutoff(value.doubleValue());
                } else {
                    source.setValue(new Double (currentParameters.getFluffNodeDensityCutoff()));
                    message += "节点密度阈值需介于0和1之间.";
                    invalid = true;
                }
            }else if (source == cliqueSizeThreshold1) {
                Number value = (Number) cliqueSizeThreshold1.getValue();
                if ((value != null) && (value.intValue() >= 0) && (value.intValue() <= 10)) {
                    currentParameters.setCliqueSizeThreshold1(value.intValue());
                } else {
                    source.setValue(new Double (currentParameters.getFluffNodeDensityCutoff()));
                    message += "极大团过滤阈值需介于0和10之间.";
                    invalid = true;
                }
            }else if (source == cliqueSizeThreshold) {
                Number value = (Number) cliqueSizeThreshold.getValue();
                if ((value != null) && (value.intValue() >= 0) && (value.intValue() <= 10)) {
                    currentParameters.setCliqueSizeThreshold(value.intValue());
                } else {
                    source.setValue(new Double (currentParameters.getFluffNodeDensityCutoff()));
                    message += "极大团过滤阈值需介于0和10之间.";
                    invalid = true;
                }
            }else if (source == fThreshold) {
                Number value = (Number) fThreshold.getValue();
                if ((value != null) && (value.doubleValue() >= 1.0) && (value.doubleValue() <= 10.0)) {
                    currentParameters.setFThreshold(value.doubleValue());
                } else {
                    source.setValue(new Double (currentParameters.getFluffNodeDensityCutoff()));
                    message += "阈值需介于1与10之间为宜.";
                    invalid = true;
                }
            }else if (source == complexSizeThreshold1) {
                Number value = (Number) complexSizeThreshold1.getValue();
                if ((value != null) && (value.intValue() >= 0)) {
                    currentParameters.setComplexSizeThreshold1(value.intValue());
                } else {
                    source.setValue(new Double (currentParameters.getFluffNodeDensityCutoff()));
                    message += "输出团的大小应大于0.";
                    invalid = true;
                }
            }else if (source == complexSizeThreshold) {
                Number value = (Number) complexSizeThreshold.getValue();
                if ((value != null) && (value.intValue() >= 0)) {
                    currentParameters.setComplexSizeThreshold(value.intValue());
                } else {
                    source.setValue(new Double (currentParameters.getFluffNodeDensityCutoff()));
                    message += "输出团的大小应大于0.";
                    invalid = true;
                }
            }
            if (invalid) {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(), message, "参数值越界", JOptionPane.WARNING_MESSAGE);
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
    public static void main(String[] args){
    	ClusterPlugin cp=new ClusterPlugin(); 
    	cp.test();
    }/**/
}
