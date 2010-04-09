package clusterviz;

import cytoscape.*;
import cytoscape.view.*;
import cytoscape.view.cytopanels.*;
import cytoscape.actions.GinyUtils;
import cytoscape.data.CyAttributes;
import cytoscape.util.CyFileFilter;
import cytoscape.util.FileUtil;
import ding.view.DGraphView;
import giny.model.GraphPerspective;
import giny.model.Node;
import giny.view.NodeView;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

import BiNGO.SettingsPanel;

/**
 * Show a Table Browser for the results of clustering. This class sets up the GUI.
 */
public class ResultPanel extends JPanel {
    protected String resultTitle;
    protected Algorithm alg;
    protected Cluster[] complexes;
    protected JTable table;
    protected ResultPanel.ClusterBrowserTableModel complexBrowser;
    //table size parameters
    protected final int picSize = 80;
    protected final int defaultRowHeight = picSize + 8;
    protected int preferredTableWidth = 0; // incremented below

    CyNetwork network;  //original input, used in the table row selection listener
    CyNetworkView networkView;     //Keep a record of this view, if it exists
    CollapsiblePanel explorePanel;
    JPanel browserPanel;
    JPanel[] exploreContents;
    ParameterSet currentParamsCopy;
    Image[] imageList;
    //Keep track of selected attribute for enumeration 
    //so it stays selected for all cluster explorations
    int enumerationSelection = 0; 

    //GraphDrawer drawer;
    Loader loader;

    /**
     * Constructor for the Results Panel which displays the complexes in a browser table and
     * an explore panels for each cluster.
     *
     * @param complexes complexes found by the AnalyzeTask
     * @param alg A reference to the algorithm for this network
     * @param network Network were these complexes were found
     * @param imageList A list of images of the found complex
     * @param resultTitle Title of this result as determined by AnalyzeAction
     */
    public ResultPanel(Cluster[] complexes, Algorithm alg, CyNetwork network, Image[] imageList, String resultTitle) {
        setLayout(new BorderLayout());
        this.setEnabled(false);
        this.alg = alg;
        this.resultTitle = resultTitle;
        this.complexes = complexes;
        this.network = network;        
        this.imageList=imageList;
        networkView = Cytoscape.getNetworkView(network.getIdentifier());//the view may not exist
        currentParamsCopy = ParameterSet.getInstance().getResultParams(resultTitle).copy();
        
        browserPanel = createBrowserPanel(this.imageList);
        JPanel bottomPanel = createBottomPanel();
        add(browserPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        //drawer = new GraphDrawer();
        loader = new Loader(table, picSize, picSize);
        this.setSize(this.getMinimumSize());
    }

    /**
     * Creates a panel that contains the browser table with a scroll bar.
     *
     * @param imageList images of cluster graphs
     * @return panel
     */
    private JPanel createBrowserPanel(Image imageList[]) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        StringBuffer sb=new StringBuffer("Complex Browser( ");
        sb.append(imageList.length);
        sb.append(" in total )");
        panel.setBorder(BorderFactory.createTitledBorder(sb.toString())); 
        
        complexBrowser=new ResultPanel.ClusterBrowserTableModel(imageList);
        table = new JTable(complexBrowser);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(StringBuffer.class, new ResultPanel.JTextAreaRenderer(defaultRowHeight));
        table.setIntercellSpacing(new Dimension(10, 8));   //gives a little vertical room between complexes
        table.setFocusable(false);  //removes an outline that appears when the user clicks on the images

        //Ask to be notified of selection changes.
        ListSelectionModel rowSM = table.getSelectionModel();
        rowSM.addListSelectionListener(new ResultPanel.TableRowSelectionAction());
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.getViewport().setBackground(Color.GRAY); 
        
        //the sortPanel
        JPanel sortPanel=new JPanel();
        sortPanel.setLayout(new FlowLayout());
        //the radio buttons
        boolean set3=currentParamsCopy.getAlgorithm().equals(ParameterSet.MCODE);
        boolean set2=(currentParamsCopy.getAlgorithm().equals(ParameterSet.FAGEC)&& 
        		currentParamsCopy.isWeak());
        boolean set1=((!set2)&&(!set3));
        JRadioButton way1=new JRadioButton("Size",set1);
        JRadioButton way2 = new JRadioButton("Modularity", set2);
        JRadioButton way3 = new JRadioButton("Score", set3);
        way1.setActionCommand("size");
        way2.setActionCommand("modu");
        way3.setActionCommand("score");
        way1.addActionListener(new SortWayAction(table, complexBrowser));
        way2.addActionListener(new SortWayAction(table, complexBrowser));
        way3.addActionListener(new SortWayAction(table, complexBrowser));
        ButtonGroup ways = new ButtonGroup();
        ways.add(way1);
        ways.add(way2);
        if(currentParamsCopy.getAlgorithm().equals(ParameterSet.MCODE))
        	ways.add(way3);
        //the label
        JLabel label=new JLabel("Sort Complexes by (descend):");
        //add components to the sortPanel
        sortPanel.add(label);
        sortPanel.add(way1);
        sortPanel.add(way2);
        if(currentParamsCopy.getAlgorithm().equals(ParameterSet.MCODE))
        	sortPanel.add(way3);
        sortPanel.setToolTipText("Select a way to sort the complexes");
        
        panel.add(sortPanel,BorderLayout.NORTH);
        panel.add(tableScrollPane,BorderLayout.CENTER);
        panel.setToolTipText("information of the identified complexes");
        return panel;
    }
    /**
     * Creates a panel containing the explore collapsable panel and result set specific buttons
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        explorePanel = new CollapsiblePanel("Current");
        explorePanel.setCollapsed(false);
        explorePanel.setVisible(false);

        JPanel buttonPanel = new JPanel();
        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(new ResultPanel.ExportAction());
        exportButton.setToolTipText("Export the resulting complexes into a file");
        JButton closeButton = new JButton("Discard");
        closeButton.setToolTipText("Close this result panel");
        closeButton.addActionListener(new ResultPanel.DiscardAction(this));
        JButton unclusterButton = new JButton("All Clustered Nodes");
        unclusterButton.setToolTipText("Select all clustered nodes");
        unclusterButton.addActionListener(new ResultPanel.UnclusterAction(this));
        JButton goEnrichmentButton = new JButton("GO Enrichment Analysis");
        goEnrichmentButton.setToolTipText("Perform GO Enrichment Analysis, need BiNGO");
        goEnrichmentButton.addActionListener(new ResultPanel.GOenrichmentAction(this));
        
		buttonPanel.add(exportButton);
        buttonPanel.add(closeButton);
        buttonPanel.add(unclusterButton);
        buttonPanel.add(goEnrichmentButton);

        panel.add(explorePanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private class SortWayAction extends AbstractAction {
        JTable browserTable;
        ResultPanel.ClusterBrowserTableModel modelBrowser;
        
        SortWayAction(JTable browserTable,ResultPanel.ClusterBrowserTableModel modelBrowser) {
            this.browserTable = browserTable;
            this.modelBrowser = modelBrowser;
        }
        public void actionPerformed(ActionEvent e) {
            String way = e.getActionCommand();
            switchPlace(way);
            if(imageList!=null){
                //browserPanel=createBrowserPanel(imageList);
            	modelBrowser.listIt();
            	modelBrowser.fireTableDataChanged();
                browserPanel.updateUI();
                ((JPanel)browserPanel.getParent()).updateUI();
            }else System.err.println("list null");
        }
    }
    private void switchPlace(String field){
    	int max;
    	Image image;
    	Cluster cluster;
    	for(int i=0;i<imageList.length-1;i++){
    		max=i;
    		for(int j=i+1;j<imageList.length;j++){
    			if(field.equals("size")){
        			if(complexes[j].getALNodes().size()>complexes[max].getALNodes().size())
        				max=j;
    			}else if(field.equals("modu")){
    				if(complexes[j].getModularity()>complexes[max].getModularity())
    					max=j;
    			}else if(field.equals("score")){
    				if(complexes[j].getClusterScore()>complexes[max].getClusterScore())
    					max=j;
    			}else{
    				System.err.println("In switchPlace:Erro Parameter");
    				return;
    			}
    		}
    		//switch
    		image=imageList[i];
    		imageList[i]=imageList[max];
    		imageList[max]=image;
    		cluster=complexes[i];
    		complexes[i]=complexes[max];
    		complexes[max]=cluster;
    	}
    }
    /**
     * This method creates a JPanel containing a node score cutoff slider 
     * and a node attribute enumeration viewer
     *
     * @param selectedRow The cluster that is selected in the cluster browser
     * @return panel A JPanel with the contents of the explore panel, get's added to the explore collapsable panel's content pane
     */
    private JPanel createExploreContent(int selectedRow) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        //Node attributes Panel
        JPanel nodeAttributesPanel = new JPanel(new BorderLayout());
        nodeAttributesPanel.setBorder(BorderFactory.createTitledBorder("Node Attribute"));        
        String[] availableAttributes = Cytoscape.getNodeAttributes().getAttributeNames();
        Arrays.sort(availableAttributes, String.CASE_INSENSITIVE_ORDER);
        String[] attributesList = new String[availableAttributes.length];
        System.arraycopy(availableAttributes, 0, attributesList, 0, availableAttributes.length);
        JComboBox nodeAttributes = new JComboBox(attributesList);
        nodeAttributes.setToolTipText("Choose the attribute name you want to look into");
        //Create a table listing the node attributes and their enumerations
        ResultPanel.EnumeratorTableModel modelEnumerator;
        modelEnumerator = new ResultPanel.EnumeratorTableModel(new HashMap());        
        JTable enumerationsTable = new JTable(modelEnumerator);
        JScrollPane tableScrollPane = new JScrollPane(enumerationsTable);
        tableScrollPane.getViewport().setBackground(Color.WHITE);
        enumerationsTable.setPreferredScrollableViewportSize(new Dimension(100, picSize));
        enumerationsTable.setGridColor(Color.LIGHT_GRAY);
        enumerationsTable.setFont(new Font(enumerationsTable.getFont().getFontName(), Font.PLAIN, 11));
        enumerationsTable.setDefaultRenderer(StringBuffer.class, new ResultPanel.JTextAreaRenderer(0));
        enumerationsTable.setFocusable(false);
        //Create a combo box that lists all the available node attributes for enumeration
        nodeAttributes.addActionListener(new ResultPanel.enumerateAction(enumerationsTable, modelEnumerator, selectedRow));
        nodeAttributesPanel.add(nodeAttributes, BorderLayout.NORTH);
        nodeAttributesPanel.add(tableScrollPane, BorderLayout.SOUTH);
        
        //The sizeSliderPanle
        JPanel sizePanel = new JPanel(new BorderLayout());
        sizePanel.setBorder(BorderFactory.createTitledBorder("Size Slider"));
        //A slider to manipulate node score cutoff 
        //TODO: note here,we need to modify the codes, or we can just remove this panel simpley
        JSlider sizeSlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, 
        		(int) (currentParamsCopy.getNodeScoreCutoff() * 1000)) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        sizeSlider.addChangeListener(new ResultPanel.SizeAction(selectedRow, nodeAttributes));
        sizeSlider.setMajorTickSpacing(200);
        sizeSlider.setMinorTickSpacing(50);
        sizeSlider.setPaintTicks(true);
        sizeSlider.setPaintLabels(true);
        Hashtable labelTable = new Hashtable();
        labelTable.put(new Integer(0), new JLabel("Min"));
        labelTable.put(new Integer(1000), new JLabel("Max"));
        //TODO: note here,we need to modify the codes, or we can just remove this panel simpley
        //TODO: there may be some other situation like this ,we need to find out them
        //Make a special label for the initial position
        labelTable.put(new Integer((int) (currentParamsCopy.getNodeScoreCutoff() * 1000)),
        		new JLabel("^") );
        sizeSlider.setLabelTable(labelTable);
        sizeSlider.setFont(new Font("Arial", Font.PLAIN, 8));
        String sizeTip = "Move the slider to change the size of the complex";
        sizeSlider.setToolTipText(sizeTip);
        sizePanel.add(sizeSlider, BorderLayout.NORTH);

        //JPanel bottomExplorePanel = createBottomExplorePanel(selectedRow);
        JButton createChildButton = new JButton("Create SubNetwork");
        createChildButton.addActionListener(new ResultPanel.CreateChildAction(this, selectedRow));
        createChildButton.setToolTipText("create a network for this complex");JPanel buttonPanel=new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.add(createChildButton,BorderLayout.SOUTH);
        JPanel bottom=new JPanel();
        bottom.setLayout(new BorderLayout());
        if(alg.getParams().getAlgorithm().equals(ParameterSet.MCODE))
        	bottom.add(sizePanel,BorderLayout.CENTER);
        	bottom.add(buttonPanel,BorderLayout.EAST);
        /*}
        else
        	bottom.add(buttonPanel);*/
        panel.add(nodeAttributesPanel);
        panel.add(bottom);
        return panel;
    }

    /**
     * Handles the create child network press in the cluster exploration panel
     */
    private class CreateChildAction extends AbstractAction {
        int selectedRow;
        ResultPanel trigger;
        CreateChildAction (ResultPanel trigger, int selectedRow) {
            this.selectedRow = selectedRow;
            this.trigger = trigger;
        }
        public void actionPerformed(ActionEvent actionEvent) {
            final Cluster cluster = complexes[selectedRow];
            final GraphPerspective gpCluster = cluster.getGPCluster();
            final String title = cluster.getClusterName() + "_" + trigger.getResultTitle()+".sif";
            //create the child network and view
            final SwingWorker worker = new SwingWorker() {
                public Object construct() {
                    CyNetwork newNetwork = Cytoscape.createNetwork(gpCluster.getNodeIndicesArray(), gpCluster.getEdgeIndicesArray(), title, network);
                    DGraphView view = (DGraphView) Cytoscape.createNetworkView(newNetwork);
                    //layout new cluster and fit it to window
                    //randomize node positions before layout so that they don't all layout in a line
                    //(so they don't fall into a local minimum for the SpringEmbedder)
                    //If the SpringEmbedder implementation changes, this code may need to be removed
                    NodeView nv;
                    boolean layoutNecessary = false;
                    for (Iterator in = view.getNodeViewsIterator(); in.hasNext();) {
                        nv = (NodeView) in.next();
                        if (cluster.getDGView() != null && cluster.getDGView().getNodeView(nv.getNode().getRootGraphIndex()) != null) {
                            //If it does, then we take the layout position that was already generated for it
                            nv.setXPosition(cluster.getDGView().getNodeView(nv.getNode().getRootGraphIndex()).getXPosition());
                            nv.setYPosition(cluster.getDGView().getNodeView(nv.getNode().getRootGraphIndex()).getYPosition());
                        } else {
                            //this will likely never occur
                            nv.setXPosition(view.getCanvas().getWidth() * Math.random());
                            //height is small for many default drawn graphs, thus +100
                            nv.setYPosition((view.getCanvas().getHeight() + 100) * Math.random());
                            layoutNecessary = true;
                        }
                    }
                    if (layoutNecessary) {
                        ClusterLayout layout = new ClusterLayout(view);
                        layout.doLayout(0, 0, 0, null);
                    }
                    view.fitContent();
                    return null;
                }
            };
            worker.start();
        }
    }

    /**
     * Generates a string buffer with the cluster's details
     * 
     * @param cluster The cluster
     * @return details String buffer containing the details
     */
    private StringBuffer getClusterDetails(Cluster cluster) {
    	//TODO: codes here need to be modified
        StringBuffer details = new StringBuffer();
        details.append("NO.: ");
        details.append((new Integer(cluster.getRank() + 1)).toString());
        details.append("\n");
        details.append("Nodes: ");
        details.append(cluster.getGPCluster().getNodeCount());
        details.append("\n");
        details.append("Edges: ");
        details.append(cluster.getGPCluster().getEdgeCount());
        details.append("\n");
        if(currentParamsCopy.getAlgorithm().equals(ParameterSet.MCODE)){
            details.append("Score: ");
            NumberFormat nf1 = NumberFormat.getInstance();
            nf1.setMaximumFractionDigits(3);
            details.append(nf1.format(cluster.getClusterScore()));
            details.append("\n");
        }
        details.append("Modularity: ");
        NumberFormat nf2 = NumberFormat.getInstance();
        nf2.setMaximumFractionDigits(3);
        details.append(nf2.format(cluster.getModularity()));
        details.append("\n");
        details.append("InDeg: ");
        details.append(cluster.getInDegree());
        details.append(" OutDeg: ");
        int outDegree=cluster.getTotalDegree()-2*cluster.getInDegree();
        details.append(outDegree);
        return details;
    }

    /**
     * Handles the data to be displayed in the cluster browser table
     * methods need to be implements
     *   public int getRowCount();
     *   public int getColumnCount();
     *   public Object getValueAt(int row, int column);
     */
    private class ClusterBrowserTableModel extends AbstractTableModel {
        //Create column headings
        String[] columnNames = {"Snapshot", "Details"};
        Object[][] data;    //the actual table data
        public ClusterBrowserTableModel(Image imageList[]) {
        	listIt();
        }
        public void listIt(){
            exploreContents = new JPanel[complexes.length];
            data = new Object[complexes.length][columnNames.length];
            //create an image for each cluster - make it a nice layout of the cluster
            for (int i = 0; i < complexes.length; i++) {
                complexes[i].setRank(i);
                Image image;
                if (imageList != null) {
                    image = imageList[i];
                } else {
                    image = ClusterUtil.convertClusterToImage(null, complexes[i], picSize, picSize, null, true);
                }
                data[i][0] = new ImageIcon(image);
                StringBuffer details = new StringBuffer(getClusterDetails(complexes[i]));
                data[i][1] = new StringBuffer(details);
            }        	
        }
        public String getColumnName(int col) {
            return columnNames[col];
        }
        public int getColumnCount() {
            return columnNames.length;
        }
        public int getRowCount() {
            return data.length;
        }
        public Object getValueAt(int row, int col) {
            return data[row][col];
        }
        public void setValueAt(Object object, int row, int col) {
            data[row][col] = object;
            fireTableCellUpdated(row, col);
        }
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
    }
    /**
     * Handles the data to be displayed in the node attribute enumeration table
     */
    private class EnumeratorTableModel extends AbstractTableModel {
        String[] columnNames = {"Attribute Name", "Occurrence Times"};
        Object[][] data = new Object[0][columnNames.length]; 
        public EnumeratorTableModel(HashMap enumerations) {
        	listIt(enumerations);
        }
        public void listIt(HashMap enumerations){
            ArrayList enumerationsSorted = sortMap(enumerations);
            Object[][] newData = new Object[enumerationsSorted.size()][columnNames.length];
            int c = enumerationsSorted.size()-1;
            for (Iterator i = enumerationsSorted.iterator(); i.hasNext();) {
                Map.Entry mp = (Map.Entry) i.next();
                newData[c][0] = new StringBuffer(mp.getKey().toString());
                newData[c][1] = new String(mp.getValue().toString());
                c--;
            }
            if (getRowCount() == newData.length) {
                data = new Object[newData.length][columnNames.length];
                System.arraycopy(newData, 0, data, 0, data.length);
                fireTableRowsUpdated(0, getRowCount());
            } else {
                data = new Object[newData.length][columnNames.length];
                System.arraycopy(newData, 0, data, 0, data.length);
                fireTableDataChanged();
            }
        }
        public String getColumnName(int col) {
            return columnNames[col];
        }
        public int getRowCount() {
            return data.length;
        }
        public int getColumnCount() {
            return columnNames.length;
        }
        public Object getValueAt(int row, int col) {
            return data[row][col];
        }
        public void setValueAt(Object object, int row, int col) {
            data[row][col] = object;
            fireTableCellUpdated(row, col);
        }        
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }

    /**
    * This method uses Arrays.sort for sorting a Map by the entries' values
    *
    * @param map Has values mapped to keys
    * @return outputList of Map.Entries
    */
    private ArrayList sortMap(Map map) {
        ArrayList outputList = null;
        int count = 0;
        Set set = null;
        Map.Entry[] entries = null;

        set = (Set) map.entrySet();
        Iterator iterator = set.iterator();
        entries = new Map.Entry[set.size()];
        while(iterator.hasNext()) {
            entries[count++] = (Map.Entry) iterator.next();
        }

        // Sort the entries with own comparator for the values:
        Arrays.sort(entries, new Comparator() {
            public int compareTo(Object o1, Object o2) {
                Map.Entry le = (Map.Entry)o1;
                Map.Entry re = (Map.Entry)o2;
                return ((Comparable)le.getValue()).compareTo((Comparable)re.getValue());
            }

            public int compare(Object o1, Object o2) {
                Map.Entry le = (Map.Entry)o1;
                Map.Entry re = (Map.Entry)o2;
                return ((Comparable)le.getValue()).compareTo((Comparable)re.getValue());
            }
        });
        outputList = new ArrayList();
        for(int i = 0; i < entries.length; i++) {
            outputList.add(entries[i]);
        }
        return outputList;
    }

    /**
     * Handles the selection of all available node attributes for the enumeration within the cluster
     */
    private class enumerateAction extends AbstractAction {
        JTable enumerationsTable;
        int selectedRow;
        ResultPanel.EnumeratorTableModel modelEnumerator;

        enumerateAction(JTable enumerationsTable,ResultPanel.EnumeratorTableModel modelEnumerator, int selectedRow) {
            this.selectedRow = selectedRow;
            this.enumerationsTable = enumerationsTable;
            this.modelEnumerator = modelEnumerator;
        }

        public void actionPerformed(ActionEvent e) {
            HashMap attributeEnumerations = new HashMap(); //the key is the attribute value and the value is the number of times that value appears in the cluster
            //First we want to see which attribute was selected in the combo box
            String attributeName = (String) ((JComboBox) e.getSource()).getSelectedItem();
            int selectionIndex = (int) ((JComboBox) e.getSource()).getSelectedIndex();
            //If its the generic 'please select' option then we don't do any enumeration
            if (!attributeName.equals("Please Select")) {
                //otherwise, we want to get the selected attribute's value for each node in the selected cluster
                for (Iterator i = complexes[selectedRow].getGPCluster().nodesIterator(); i.hasNext();) {
                    Node node = (Node) i.next();
                    //The attribute value will be stored as a string no matter what it is but we need an array list
                    //because some attributes are maps or lists of any size
                    ArrayList attributeValue = new ArrayList();
                    //Every type of attribute has its own get method so we have to see which one to use
                    //When we find the type, we get its value(s)
                    if (Cytoscape.getNodeAttributes().getType(attributeName) == CyAttributes.TYPE_STRING) {
                        attributeValue.add(Cytoscape.getNodeAttributes().getStringAttribute(node.getIdentifier(), attributeName));
                    } else if (Cytoscape.getNodeAttributes().getType(attributeName) == CyAttributes.TYPE_FLOATING) {
                        attributeValue.add(Cytoscape.getNodeAttributes().getDoubleAttribute(node.getIdentifier(), attributeName));
                    } else if (Cytoscape.getNodeAttributes().getType(attributeName) == CyAttributes.TYPE_INTEGER) {
                        attributeValue.add(Cytoscape.getNodeAttributes().getIntegerAttribute(node.getIdentifier(), attributeName));
                    } else if (Cytoscape.getNodeAttributes().getType(attributeName) == CyAttributes.TYPE_BOOLEAN) {
                        attributeValue.add(Cytoscape.getNodeAttributes().getBooleanAttribute(node.getIdentifier(), attributeName));
                    } else if (Cytoscape.getNodeAttributes().getType(attributeName) == CyAttributes.TYPE_SIMPLE_LIST) {
                        List valueList = Cytoscape.getNodeAttributes().getListAttribute(node.getIdentifier(), attributeName);
                        for (Iterator vli = valueList.iterator(); vli.hasNext();) {
                            attributeValue.add(vli.next());
                        }
                    } else if (Cytoscape.getNodeAttributes().getType(attributeName) == CyAttributes.TYPE_SIMPLE_MAP) {
                        Map valueMap = Cytoscape.getNodeAttributes().getMapAttribute(node.getIdentifier(), attributeName);
                        for (Iterator vmki = valueMap.keySet().iterator(); vmki.hasNext();) {
                            String key = (String) vmki.next();
                            Object value = valueMap.get(key);
                            attributeValue.add(new String(key + " -> " + value));
                        }
                    }
                    //Next we must make a non-repeating list with the attribute values and enumerate the repetitions
                    for (Iterator avi = attributeValue.iterator(); avi.hasNext();) {
                        Object aviElement = avi.next();
                        if (aviElement != null) {
                            String value = aviElement.toString();

                            if (!attributeEnumerations.containsKey(value)) {
                                //If the attribute value appears for the first time, we give it an enumeration of 1 and add it to the enumerations
                                attributeEnumerations.put(value, new Integer(1));
                            } else {
                                //If it already appeared before, we want to add to the enumeration of the value
                                Integer enumeration = (Integer) attributeEnumerations.get(value);
                                enumeration = new Integer(enumeration.intValue()+1);
                                attributeEnumerations.put(value, enumeration);
                            }
                        }
                    }
                }
            }
            modelEnumerator.listIt(attributeEnumerations);
            //Finally we make sure that the selection is stored so that all the cluster explorations are looking at the already selected attribute
            enumerationSelection = selectionIndex;
        }
    }

	/**
	 * Call BiNGO plugin to perform GO enrichment analysis
	 */
	private class GOenrichmentAction extends AbstractAction{
		ResultPanel triger;
		GOenrichmentAction(ResultPanel triger){
			this.triger = triger;
		}
		public void actionPerformed(ActionEvent e){
			String bingoDir;
			String tmp = System.getProperty("user.dir");
        	bingoDir = new File(tmp, "plugins").toString();
			JFrame window = new JFrame("BiNGO Settings");
            SettingsPanel settingsPanel = new SettingsPanel(bingoDir);
            //window.setJMenuBar(new HelpMenuBar(settingsPanel).getHelpMenuBar());
            window.getContentPane().add(settingsPanel);
            window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            window.pack();
            Dimension screenSize =
                    Toolkit.getDefaultToolkit().getScreenSize();
            // for central position of the settingspanel.
            window.setLocation(screenSize.width / 2 - (window.getWidth() / 2),
                    screenSize.height / 2 - (window.getHeight() / 2));
            window.setVisible(true);
            window.setResizable(true);
		}
	}

    /**
     * Handles the select unclustered nodes process for this results panel
     */
    private class UnclusterAction extends AbstractAction {
        ResultPanel trigger;
        UnclusterAction(ResultPanel trigger) {
            this.trigger = trigger;
        }
        public void actionPerformed(ActionEvent e) {
        	int length=0;
        	for(int i=0; i < complexes.length; i++){
        		length = length + complexes[i].getGPCluster().getNodeIndicesArray().length;
        	}
        	int[] clusteredNodes;
        	clusteredNodes = new int[length];
        	int index=0;
        	for(int i=0; i < complexes.length; i++){
        		for(int j=0;j <= complexes[i].getGPCluster().getNodeIndicesArray().length -1; j++){
        			clusteredNodes[index++] = complexes[i].getGPCluster().getNodeIndicesArray()[j];
        		}
        	}

        	selectCluster(network.createGraphPerspective(clusteredNodes));
        }
    }
    /**
     * Handles the close press for this results panel
     */
    private class DiscardAction extends AbstractAction {
        ResultPanel trigger;
        DiscardAction(ResultPanel trigger) {
            this.trigger = trigger;
        }
        public void actionPerformed(ActionEvent e) {
            CytoscapeDesktop desktop = Cytoscape.getDesktop();
            CytoPanel cytoPanel = desktop.getCytoPanel(SwingConstants.EAST);
            String message = "Confirm to close the" + resultTitle + "?";
            int result = JOptionPane.showOptionDialog(Cytoscape.getDesktop(), new Object[]{message}, "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (result == JOptionPane.YES_OPTION) {
                cytoPanel.remove(trigger);
                ParameterSet.removeResultParams(trigger.getResultTitle());
            }
            if (cytoPanel.getCytoPanelComponentCount() == 0) {
                cytoPanel.setState(CytoPanelState.HIDE);
            }
        }
    }

    /**
     * Handles the Export press for this panel (export results to a text file)
     */
    private class ExportAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            File file = FileUtil.getFile("Save", FileUtil.SAVE, new CyFileFilter[]{});
            if (file != null) {
                String fileName = file.getAbsolutePath();
                String message = "Save the complete information \n" +
                		"of the resulting complexes?"+
                		"(Y/Complete��N/Basic)";
                int result = JOptionPane.showOptionDialog(Cytoscape.getDesktop(), new Object[]{message}, "Export Mode", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
                if (result == JOptionPane.YES_OPTION) {
                	ClusterUtil.exportResults(alg, complexes, network, fileName);
                	JOptionPane.showMessageDialog( Cytoscape.getDesktop(), "Completed!"); 
                }
                else if(result==JOptionPane.NO_OPTION){
                    ClusterUtil.exportSimpleClusters(alg, complexes, network, fileName);
                	JOptionPane.showMessageDialog( Cytoscape.getDesktop(), "Succeed!"); 
                }
            }
        }
    }

    /**
     * Handler to select nodes in graph when a row is selected
     */
    private class TableRowSelectionAction implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) return;
            ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            final GraphPerspective gpCluster;
            if (!lsm.isSelectionEmpty()) {
                final int selectedRow = lsm.getMinSelectionIndex();
                gpCluster = complexes[selectedRow].getGPCluster();
                selectCluster(gpCluster);
                if (exploreContents[selectedRow] == null) {
                    exploreContents[selectedRow] = createExploreContent(selectedRow);
                }
                if (explorePanel.isVisible()) {
                    explorePanel.getContentPane().remove(0);
                }
                explorePanel.getContentPane().add(exploreContents[selectedRow], BorderLayout.SOUTH);
                //and set the explore panel to visible so that it can be seen (this only happens once
                //after the first time the user selects a cluster
                if (!explorePanel.isVisible()){
                    explorePanel.setVisible(true);
                }
                //Finally the explore panel must be redrawn upon the selection event to display the
                //new content with the name of the cluster, if it exists
                String title = "Current: ";
                if (complexes[selectedRow].getClusterName() != null) {
                    title = title + complexes[selectedRow].getClusterName();
                } else {
                    title = title + "Complex " + (selectedRow + 1);
                }
                explorePanel.setTitleComponentText(title);
                explorePanel.updateUI();

                //In order for the enumeration to be conducted for this cluster on the same 
                //attribute that might already have been selected
                //we get a reference to the combo box within the explore content
                JComboBox nodeAttributesComboBox = (JComboBox) ((JPanel) exploreContents[selectedRow].getComponent(0)).getComponent(0);
                //and fire the enumeration action
                nodeAttributesComboBox.setSelectedIndex(enumerationSelection);
                table.scrollRectToVisible(table.getCellRect(selectedRow, 0, true));
            }
        }
    }

    /**
     * Selects a cluster in the view that is selected by the user in the browser table
     *
     * @param gpCluster Cluster to be selected
     */
    public void selectCluster(GraphPerspective gpCluster) {
        if (gpCluster != null) {
            if (networkView != null) {
                GinyUtils.deselectAllNodes(networkView);
                network.setSelectedNodeState(gpCluster.nodesList(), true);
                //We want the focus to switch to the appropriate network view but only if the cytopanel is docked
                //If it is not docked then it is best if the focus stays on the panel
                if(Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST).getState() == CytoPanelState.DOCK) {
                    Cytoscape.getDesktop().setFocus(networkView.getIdentifier());
                }
            } else {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(), 
                		"There is no network to select nodes.", "", JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (networkView != null) {
            GinyUtils.deselectAllNodes(networkView);
        }
    }

    /**
     * A text area renderer that creates a line wrapped, non-editable text area
     */
    private class JTextAreaRenderer extends JTextArea implements TableCellRenderer {
        int minHeight;
        public JTextAreaRenderer(int minHeight) {
            this.setFont(new Font(this.getFont().getFontName(), Font.PLAIN, 11));
            this.minHeight = minHeight;
        }

        /**
         * Used to render a table cell.  Handles selection color and cell heigh and width.
         * Note: Be careful changing this code as there could easily be infinite loops created
         * when calculating preferred cell size as the user changes the dialog box size.
         *
         * @param table      Parent table of cell
         * @param value      Value of cell
         * @param isSelected True if cell is selected
         * @param hasFocus   True if cell has focus
         * @param row        The row of this cell
         * @param column     The column of this cell
         * @return The cell to render by the calling code
         */
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            StringBuffer sb = (StringBuffer) value;
            this.setText(sb.toString());
            if (isSelected) {
                this.setBackground(table.getSelectionBackground());
                this.setForeground(table.getSelectionForeground());
            } else {
                this.setBackground(table.getBackground());
                this.setForeground(table.getForeground());
            }
            //row height calculations
            int currentRowHeight = table.getRowHeight(row);
            int rowMargin = table.getRowMargin();
            this.setSize(table.getColumnModel().getColumn(column).getWidth(), currentRowHeight - (2 * rowMargin));
            int textAreaPreferredHeight = (int) this.getPreferredSize().getHeight();
            //JTextArea can grow and shrink here
            if (currentRowHeight != Math.max(textAreaPreferredHeight + (2 * rowMargin) , minHeight + (2 * rowMargin))) {
                table.setRowHeight(row, Math.max(textAreaPreferredHeight + (2 * rowMargin), minHeight + (2 * rowMargin)));
            }
            return this;
        }
    }
    public String getResultTitle() {
        return resultTitle;
    }
    public void setResultTitle(String title) {
        resultTitle = title;
    }
    public JTable getClusterBrowserTable() {
        return table;
    }

    /**
     * Handles the dynamic cluster size manipulation via the JSlider
     */
    private class SizeAction implements ChangeListener {
        private int selectedRow;
        public boolean loaderSet = false;
        private JComboBox nodeAttributesComboBox;
        private GraphDrawer drawer;
        private boolean drawPlaceHolder;
        /**
         * @param selectedRow The selected complex
         * @param nodeAttributesComboBox Reference to the attribute enumeration picker
         */
        SizeAction(int selectedRow, JComboBox nodeAttributesComboBox){
            this.selectedRow = selectedRow;
            this.nodeAttributesComboBox = nodeAttributesComboBox;
            drawer = new GraphDrawer();
            loaderSet = false;
        }
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider)e.getSource();
            //TODO:modify code here
            double nodeScoreCutoff = (((double)source.getValue())/1000);
            ArrayList oldNodes = complexes[selectedRow].getALNodes();
            Cluster cluster = alg.exploreCluster(complexes[selectedRow], nodeScoreCutoff, network, resultTitle);
            ArrayList newNodes = cluster.getALNodes();
            drawPlaceHolder = newNodes.size() > 300;
            //compare the old and new
            if (!newNodes.equals(oldNodes)) {
                drawer.interruptDrawing();
                complexes[selectedRow] = cluster;
                //Update the details
                StringBuffer details = getClusterDetails(cluster);
                table.setValueAt(details, selectedRow, 1);
                //Fire the enumeration action  ????
                nodeAttributesComboBox.setSelectedIndex(nodeAttributesComboBox.getSelectedIndex());
                if (!loaderSet && !drawPlaceHolder) {
                    loader.setLoader(selectedRow, table);
                    loaderSet = true;
                }
                //When expanding, new nodes need random position and thus must go through the layout
                boolean layoutNecessary = newNodes.size() > oldNodes.size();
                drawer.drawGraph(cluster, layoutNecessary, this, drawPlaceHolder);
            }
        }
    }

    /**
     * Threaded method for drawing exploration graphs which allows 
     * the slider to move uninterruptedly despite drawing efforts
     */
    private class GraphDrawer implements Runnable {
        private Thread thread;
        private boolean drawGraph; //run switch
        private boolean placeHolderDrawn;
        private boolean drawPlaceHolder;
        Cluster cluster;
        ClusterLayout layout;
        ResultPanel.SizeAction trigger;
        boolean layoutNecessary;
        boolean clusterSelected;
        GraphDrawer () {
            drawGraph = false;	//if there is need to draw graph for a complex
            drawPlaceHolder = false;	//if a place holder shouldbe draw in the case of big complexes
            layout = new ClusterLayout();
            thread = new Thread(this);
            thread.start();
        }
        /**
         * method for drawing graphs during exploration
         *
         * @param cluster Cluster to be drawn
         * @param layoutNecessary True only if the cluster is expanding in size or lacks a DGView
         * @param trigger Reference to the slider size action
         * @param drawPlaceHolder Determines if the cluster should be drawn 
         * or a place holder in the case of big complexes
         */
        public void drawGraph(Cluster cluster, boolean layoutNecessary, 
        		ResultPanel.SizeAction trigger, boolean drawPlaceHolder) {
            this.cluster = cluster;
            this.trigger = trigger;
            this.layoutNecessary = layoutNecessary;
            drawGraph = !drawPlaceHolder;
            this.drawPlaceHolder = drawPlaceHolder;
            clusterSelected = false;
        }
        public void run () {
            try {
                while (true) {
                    if (drawGraph && !drawPlaceHolder) {
                        Image image = ClusterUtil.convertClusterToImage(loader, cluster, picSize, picSize, layout, layoutNecessary);
                        if (image != null && drawGraph) {
                            loader.setProgress(100, "Selecting Nodes");
                            //Select the new cluster
                            selectCluster(cluster.getGPCluster());
                            clusterSelected = true;
                            //Update the table
                            table.setValueAt(new ImageIcon(image), cluster.getRank(), 0);
                            //Loader is no longer showing, let the SizeAction know that
                            trigger.loaderSet = false;
                            //stop loader from animating and taking up computer processing power
                            loader.loaded();
                            drawGraph = false;
                        }
                        placeHolderDrawn = false;
                    } else if (drawPlaceHolder && !placeHolderDrawn) {
                        //draw place holder, only once
                        Image image = ClusterUtil.getPlaceHolderImage(picSize, picSize);
                        table.setValueAt(new ImageIcon(image), cluster.getRank(), 0);
                        selectCluster(cluster.getGPCluster());
                        trigger.loaderSet = false;
                        loader.loaded();
                        drawGraph = false;
                        //Make sure this block is not run again unless if we need to reload the image
                        placeHolderDrawn = true;                        
                    } else if (!drawGraph && drawPlaceHolder && !clusterSelected) {
                        selectCluster(cluster.getGPCluster());
                        clusterSelected = true;
                    }
                    //This sleep time produces the drawing response time of 1 20th of a second
                    Thread.sleep(100);
                }
            } catch (Exception e) {}
        }
        public void interruptDrawing() {
            drawGraph = false;
            layout.interruptDoLayout();
            ClusterUtil.interruptLoading();
        }
    }
    /**
     * Sets the network node attributes to the current result set's degrees and clusters.
     * This method is accessed from VisualStyleAction only when a results panel is selected in the east cytopanel.
     *
     * @return the maximal degree in the network given the parameters that were used for scoring at the time
     */
    public int setNodeAttributesAndGetMaxDegree() {
        Cytoscape.getNodeAttributes().deleteAttribute("Cluster");
        for (Iterator nodes = network.nodesIterator(); nodes.hasNext();) {
            Node n = (Node) nodes.next();
            int rgi = n.getRootGraphIndex();
            Cytoscape.getNodeAttributes().setAttribute(n.getIdentifier(), "Node_Status", "Unclustered");
            for (int c = 0; c < complexes.length; c++) {
                Cluster complex = complexes[c];
                if (complex.getALNodes().contains(new Integer(rgi))) {
                    ArrayList clusterArrayList = new ArrayList();
                    if (Cytoscape.getNodeAttributes().getListAttribute(n.getIdentifier(), "Cluster") != null) {
                        clusterArrayList = (ArrayList) Cytoscape.getNodeAttributes().getListAttribute(n.getIdentifier(), "MCODE_Cluster");
                        clusterArrayList.add(complex.getClusterName());
                    } else {
                        clusterArrayList.add(complex.getClusterName());
                    }
                    Cytoscape.getNodeAttributes().setListAttribute(n.getIdentifier(), "Cluster", clusterArrayList);

                    if (complex.getSeedNode().intValue() == rgi) {
                        Cytoscape.getNodeAttributes().setAttribute(n.getIdentifier(), "Node_Status", "Seed");
                    } else {
                        Cytoscape.getNodeAttributes().setAttribute(n.getIdentifier(), "Node_Status", "Clustered");
                    }
                }
            }
            Cytoscape.getNodeAttributes().setAttribute(n.getIdentifier(), "Cluster_Size", alg.getNodeScore(n.getRootGraphIndex(), resultTitle));
        }
        if(network==null)
        	return 0;
    	int max=0,degree;
    	for(Iterator it=network.nodesIterator(); it.hasNext();){
    		Node node=(Node)it.next();
    		degree=network.getDegree(node.getRootGraphIndex());
    		if(degree>max)
    			max=degree;
    	}
    	return max;
    }
}
