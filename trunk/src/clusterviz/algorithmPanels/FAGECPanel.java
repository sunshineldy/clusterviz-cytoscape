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

public class FAGECPanel extends JPanel{

	ParameterSet currentParameters;

    private DecimalFormat decimal; // used in the formatted text fields

    JCheckBox overlapped;
    JFormattedTextField fThreshold;
    JFormattedTextField cliqueSizeThreshold;
    JFormattedTextField complexSizeThreshold;
    JRadioButton weak; // use weak module definition
    JRadioButton strong; // use strong module definition

    JPanel weakPanel;
    JPanel cliqueSizePanel;

	public FAGECPanel(){

        currentParameters = ParameterSet.getInstance().getParamsCopy(null);

		decimal = new DecimalFormat();
        decimal.setParseIntegerOnly(true);

        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createTitledBorder(""));
        
        //the collapsible panel
        CollapsiblePanel collapsiblePanel = new CollapsiblePanel("FAG-EC Options");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("")); 
        
        //the radio botton panel
        JPanel functionPanel=new JPanel();
        functionPanel.setLayout(new BorderLayout());
        //the radio bottons
        weak = new JRadioButton("Weak",true);
        weak.setToolTipText("use weak module definition");
        strong = new JRadioButton("Strong", false);
        strong.setToolTipText("use strong module definition");
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
        JLabel label=new JLabel("Threshold");  //Clique Size Threshold input
        fThreshold = new JFormattedTextField(decimal) {
            public JToolTip createToolTip() {
                return new MyTipTool();
            }
        };
        //the input text field
        fThreshold.setColumns(3);
        fThreshold.addPropertyChangeListener("value", new FAGECPanel.FormattedTextFieldAction());
        String tip2 = "threshold to define a module\n" +
                "It stands for the proportion of\n"+
                "the indegree to the outdegree of a clique";
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
        String overlapTip = "Produce overlapped module.";
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
        collapsiblePanel.setToolTipText("Customize parameters for FAG-EC (Optional)");
        this.add(collapsiblePanel);
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
     * Handles setting for the text field parameters that are numbers.
     * Makes sure that the numbers make sense.
     */
    private class FormattedTextFieldAction implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            JFormattedTextField source = (JFormattedTextField) e.getSource();
            String message = "Invaluled input\n";
            boolean invalid = false;
            
    		if (source == cliqueSizeThreshold) {
                Number value = (Number) cliqueSizeThreshold.getValue();
                if ((value != null) && (value.intValue() >= 0) && (value.intValue() <= 10)) {
                    currentParameters.setCliqueSizeThreshold(value.intValue());
                } else {
                    source.setValue(new Double (currentParameters.getCliqueSizeThreshold()));
                    message += "clique size cutoff should\n" +
    						"be set between 0 and 10.";
                    invalid = true;
                }
            }else if (source == fThreshold) {
                Number value = (Number) fThreshold.getValue();
                if ((value != null) && (value.doubleValue() >= 1.0) && (value.doubleValue() <= 10.0)) {
                    currentParameters.setFThreshold(value.doubleValue());
                } else {
                    source.setValue(new Double (currentParameters.getFThreshold()));
                    message += "module threshold should\n" +
    						"be set between 1 and 10.";
                    invalid = true;
                }
            }else if (source == complexSizeThreshold) {
                Number value = (Number) complexSizeThreshold.getValue();
                if ((value != null) && (value.intValue() >= 0)) {
                    currentParameters.setComplexSizeThreshold(value.intValue());
                } else {
                    source.setValue(new Double (currentParameters.getComplexSizeThreshold()));
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
        cliqueSizeThreshold.addPropertyChangeListener("value", new FAGECPanel.FormattedTextFieldAction());
        String tip = "size cutoff of maximal clique\n" +
        		"maximal cliques smaller than this will be\n" +
        		"regarded as subordinate and filtered\n"+
        		"the value is recommended to be set 2~5";
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
        complexSizeThreshold.addPropertyChangeListener("value", new FAGECPanel.FormattedTextFieldAction());
        String tip3 = "size cutoff of modules to be outputed\n" +
        		"modules smaller than this will be filtered";
        complexSizeThreshold.setToolTipText(tip3);
        complexSizeThreshold.setText((new Integer(currentParameters.getComplexSizeThreshold()).toString()));
        panel.add(sizeThresholdLabel2,BorderLayout.WEST);
        panel.add(complexSizeThreshold,BorderLayout.EAST);
        return panel;
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
}

