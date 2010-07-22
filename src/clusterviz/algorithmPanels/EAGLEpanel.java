package clusterviz.algorithmPanels;

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

import clusterviz.*;

public class EAGLEpanel extends JPanel{

	ParameterSet currentParameters;

    private DecimalFormat decimal; // used in the formatted text fields
	private JFormattedTextField cliqueSizeThreshold;
    private JFormattedTextField complexSizeThreshold;

	public EAGLEpanel(){

        currentParameters = ParameterSet.getInstance().getParamsCopy(null);

		decimal = new DecimalFormat();
        decimal.setParseIntegerOnly(true);

        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createTitledBorder(""));
        //the collapsible panel
        CollapsiblePanel collapsiblePanel = new CollapsiblePanel("EAGLE Options");
        JPanel cliqueSizePanel=createCliqueSizePanel();
        JPanel complexSizePanel=createComplexSizePanel();
        collapsiblePanel.getContentPane().add(cliqueSizePanel, BorderLayout.NORTH);
        collapsiblePanel.getContentPane().add(complexSizePanel, BorderLayout.CENTER);
        collapsiblePanel.setToolTipText("Customize parameters for EAGLE (Optional)");
        this.add(collapsiblePanel);
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
        cliqueSizeThreshold.addPropertyChangeListener("value", new EAGLEpanel.FormattedTextFieldAction());
        String tip = "size cutoff of maximal clique\n" +
                "maximal cliques smaller than this will be\n" +
                "regarded as subordinate and filtered\n"+
                "the value is recommended to be set 2~5";
        cliqueSizeThreshold.setToolTipText(tip);
        cliqueSizeThreshold.setText((new Integer(currentParameters.getCliqueSizeThreshold1()).toString()));
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

    private JPanel createComplexSizePanel(){
        JPanel panel=new JPanel();
        panel.setLayout(new BorderLayout());        
        //the label
        JLabel sizeThresholdLabel2=new JLabel(" ComplexSize Threshold");  //Clique Size Threshold input
        //the input text field
        complexSizeThreshold= new JFormattedTextField(decimal) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        complexSizeThreshold.setColumns(3);
        complexSizeThreshold.addPropertyChangeListener("value", new EAGLEpanel.FormattedTextFieldAction());
        String tip3 = "size cutoff of modules to be outputed\n" +
                "modules smaller than this will be filtered";
        complexSizeThreshold.setToolTipText(tip3);
        complexSizeThreshold.setText((new Integer(currentParameters.getComplexSizeThreshold1()).toString()));
        panel.add(sizeThresholdLabel2,BorderLayout.WEST);
        panel.add(complexSizeThreshold,BorderLayout.EAST);
        return panel;
    }

    private class FormattedTextFieldAction implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            JFormattedTextField source = (JFormattedTextField) e.getSource();
            String message = "Invaluled input\n";
            boolean invalid = false;
            
            if (source == cliqueSizeThreshold) {
                Number value = (Number) cliqueSizeThreshold.getValue();
                if ((value != null) && (value.intValue() >= 0) && (value.intValue() <= 10)) {
                    currentParameters.setCliqueSizeThreshold1(value.intValue());
                } else {
                    source.setValue(new Double (currentParameters.getCliqueSizeThreshold1()));
                    message +=  "clique size cutoff should\n" +
            				"be set between 0 and 10.";
                    invalid = true;
                }
            }else if (source == complexSizeThreshold) {
                Number value = (Number) complexSizeThreshold.getValue();
                if ((value != null) && (value.intValue() >= 0)) {
                    currentParameters.setComplexSizeThreshold1(value.intValue());
                } else {
                    source.setValue(new Double (currentParameters.getComplexSizeThreshold1()));
                    message += "size of output module cutoff should\n" +
    						"be greater than 0.";
                    invalid = true;
                }
			}
            
            if (invalid) {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(), message, "paramter out of boundary", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

}
