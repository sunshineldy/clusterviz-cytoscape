package cjry.clusterviz;

import cytoscape.visual.NodeAppearanceCalculator;
import cytoscape.visual.VisualStyle;
import cytoscape.visual.VisualPropertyType;
import cytoscape.visual.calculators.Calculator;
import cytoscape.visual.calculators.BasicCalculator;
import cytoscape.visual.mappings.*;

import java.awt.*;

/**
 * A visual style for ClusterPlugin 
 * set the visual style of diffrent node shape and color.
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
     * @param name name dipsplayed in the vizmap select box
     */
    public ClusterVisualStyle (String name) {
        super(name);
        initCalculators();
    }

    /**
     * Reinitialazes the calculators.  This method is called whenver different results are selected
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
        discreteMapping.putMapValue("Unclustered",new Byte(RECT));

        Calculator nodeShapeCalculator = new BasicCalculator("Node Shape Calculator", discreteMapping, VisualPropertyType.NODE_SHAPE);
        nac.setCalculator(nodeShapeCalculator);
    }

    private void createNodeColor(NodeAppearanceCalculator nac) {
        nac.getDefaultAppearance().set(VisualPropertyType.NODE_FILL_COLOR, Color.GREEN);
        ContinuousMapping continuousMapping = new ContinuousMapping(Color.WHITE, ObjectMapping.NODE_MAPPING);
        continuousMapping.setControllingAttributeName("Cluster_Score", null, false);

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
}
