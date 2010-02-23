package clusterviz;

import cytoscape.Cytoscape;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelListener;
import cytoscape.view.cytopanels.CytoPanelState;
import cytoscape.visual.NodeAppearanceCalculator;
import cytoscape.visual.VisualMappingManager;
import cytoscape.visual.VisualStyle;
import cytoscape.visual.VisualPropertyType;
import cytoscape.visual.calculators.Calculator;
import cytoscape.visual.calculators.BasicCalculator;
import cytoscape.visual.mappings.*;

import java.awt.*;


/**
 * A visual style for ClusterPlugin 
 * set the visual style of different node shape and color.
 */
public class ClusterVisualStyle extends VisualStyle {
    private double maxValue = 0.0;
    //how can these variable be used???
    private static final byte RECT=0;
    private static final byte TRIANGLE=0;
    private static final byte DIAMOND=7;
    
    /**
     * Constructor for the visual style
     *
     * @param name name displayed in the vizmap select box
     */
    public ClusterVisualStyle (String name) {
        super(name);
        initCalculators();
    }

    /**
     * Reinitializes the calculators.  This method is called whenever different results are selected
     * because they may have different node score attributes and may require a redrawing of shapes
     * and colors given the new maximum score.
     */
    public void initCalculators() {
        NodeAppearanceCalculator nac = new NodeAppearanceCalculator();
        createNodeShape(nac);//calculate the shape of a node according to it's clustering status
        createNodeColor(nac);//calculate the color of a node given the score after scoring
        this.setNodeAppearanceCalculator(nac);
    }

    private void createNodeShape(NodeAppearanceCalculator nac) {
        DiscreteMapping discreteMapping = new DiscreteMapping(new Byte(TRIANGLE), "Node_Status", ObjectMapping.NODE_MAPPING);
        //Node shapes are determined by three discrete classifications
        discreteMapping.putMapValue("Clustered",new Byte(DIAMOND));
        discreteMapping.putMapValue("Seed",new Byte(TRIANGLE));
        discreteMapping.putMapValue("Unclustered",new Byte(TRIANGLE));

        Calculator nodeShapeCalculator = new BasicCalculator("Node Shape Calculator", discreteMapping, VisualPropertyType.NODE_SHAPE);
        nac.setCalculator(nodeShapeCalculator);
    }

    private void createNodeColor(NodeAppearanceCalculator nac) {
        nac.getDefaultAppearance().set(VisualPropertyType.NODE_FILL_COLOR, Color.GREEN);
        ContinuousMapping continuousMapping = new ContinuousMapping(Color.WHITE, ObjectMapping.NODE_MAPPING);
        continuousMapping.setControllingAttributeName("Cluster_Size", null, false);

        Interpolator fInt = new LinearNumberToColorInterpolator();
        continuousMapping.setInterpolator(fInt);

        //Node color is based on the score, the lower the score the darker the color
        Color minColor = Color.BLACK;
        Color maxColor = Color.RED;
        //Color minColor = Color.GRAY;
        //Color maxColor = Color.MAGENTA;

        //Create two boundary conditions
        BoundaryRangeValues bv0 = new BoundaryRangeValues(Color.WHITE, Color.WHITE, minColor);
        BoundaryRangeValues bv2 = new BoundaryRangeValues(maxColor, maxColor, maxColor);

        //Set Data Points
        double minValue = 0.0;
        //the max value is set by MCODEVisualStyleAction based on the current result set's max score
        continuousMapping.addPoint(minValue, bv0);
        continuousMapping.addPoint(maxValue, bv2);

        Calculator nodeColorCalculator = new BasicCalculator("Node Color Calculator", continuousMapping, VisualPropertyType.NODE_FILL_COLOR);
        nac.setCalculator(nodeColorCalculator);
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }
    /**
     * A controller for the visualization attributes. Only the onComponentSelected method is
     * used in this listener to determine when a result has been selected.
     */
    public class VisualStyleAction implements CytoPanelListener {
        private CytoPanel cytoPanel;
        private ClusterVisualStyle vistyle;

        public VisualStyleAction(CytoPanel cytoPanel, ClusterVisualStyle vistyle) {
            this.cytoPanel = cytoPanel;
            this.vistyle = vistyle;
        }
        
        public void onStateChange(CytoPanelState newState) {}

        /**
         * Whenever a result tab is selected in the east CytoPanel, the attributes
         * have to be rewritten to correspond to that particular result. At the same time the
         * Visual Style has to redraw the network given the new attributes.
         *
         * @param componentIndex The index of the component that was selected in the east Cytopanel.  This action only occurs if the component is an instance of the MCODEResultsPanel
         */
        public void onComponentSelected(int componentIndex) {
            //When the user selects a tab in the east cytopanel we want to see if it is a results panel
            //and if it is we want to re-draw the network with the visual style and reselect the
            //cluster that may be selected in the results panel
            Component component = cytoPanel.getSelectedComponent();
            if (component instanceof ResultPanel) {
                //to re-initialize the calculators we need the highest score of this particular result set
                double maxDegree = ((ResultPanel) component).setNodeAttributesAndGetMaxDegree();
                //we also need the selected row if one is selected at all
                System.out.println("The max Degree in the network: "+maxDegree);
                ((ResultPanel) component).selectCluster(null);
                int selectedRow = ((ResultPanel) component).getClusterBrowserTable().getSelectedRow();
                ((ResultPanel) component).getClusterBrowserTable().clearSelection();
                if (selectedRow >= 0) {
                    ((ResultPanel) component).getClusterBrowserTable().setRowSelectionInterval(selectedRow, selectedRow);
                }
                vistyle.setMaxValue(maxDegree);
                vistyle.initCalculators();
                VisualMappingManager vmm = Cytoscape.getVisualMappingManager();
                vmm.applyNodeAppearances();
            }
        }
        
        public void onComponentAdded(int count) {}
        public void onComponentRemoved(int count) {}
    }
}
