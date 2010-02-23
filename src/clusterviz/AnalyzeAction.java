package clusterviz;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelState;
import cytoscape.visual.VisualMappingManager;
import giny.model.Node;

import javax.swing.*;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Classe to handle the action of clicking Button Analyze.
 */
public class AnalyzeAction implements ActionListener {
    final static int FIRST_TIME = 0;
    final static int RESCORE = 1;
    final static int REFIND = 2;
    final static int FIND=3;
    final static int INTERRUPTED = 4;
    final static int FINDCLIQUE=5;
    final static int CLIQUEBASED=6;
    final static int EXISTS=7;
    
    private HashMap networkManager;//Keeps track of netowrks (id is key) and their algorithms 
    private boolean resultFound ;
    private ResultPanel resultPanel;
    int analyze = FIRST_TIME;
    int resultCounter = 0;
    ParameterSet curParams;
    ClusterVisualStyle vistyle;
    private String interruptedMessage="";
    private int resultIndex;
    
    AnalyzeAction () {}

	/**
	 * This is constructor for AnalyzeAction class
	 *
	 * @param curParams current parameter set
	 * @param vistyle
	 */
    AnalyzeAction (ParameterSet curParams, ClusterVisualStyle vistyle) {
        this.curParams = curParams;
        this.vistyle = vistyle;
        networkManager = new HashMap();
    }
	/**
     * This method is called when the user clicks Analyze.
     *
     * @param event Click of the analyzeButton on the MainPanel.
     */
    public void actionPerformed(ActionEvent event) {
        resultFound = false;
        ParameterSet resultParaSet=null;
        //get the network object, this contains the graph
        final CyNetwork network = Cytoscape.getCurrentNetwork();
        curParams.setNetworkID(network.getIdentifier());
        if (network == null) {
            System.err.println("Can't get a network.");
            return;
        }
        if (network.getNodeCount() < 1) {
            JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                    "Network has not been loaded!", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Set selectedNodes = network.getSelectedNodes();
        Integer[] selectedNodesRGI = new Integer[selectedNodes.size()];
        int c = 0;
        for (Iterator i = selectedNodes.iterator(); i.hasNext();) {
            Node node = (Node) i.next();
            selectedNodesRGI[c] = new Integer(node.getRootGraphIndex());
            c++;
        }
        //notice, here we set the selected nodes
        curParams.setSelectedNodes(selectedNodesRGI);

        Algorithm alg;
        boolean newNet=false;
        String halfResultTitle = "Result ";
        if (!networkManager.containsKey(network.getIdentifier())){
        	newNet=true;
            alg = new Algorithm(null);
            networkManager.put(network.getIdentifier(), alg);
        }
        else alg = (Algorithm) networkManager.get(network.getIdentifier());
        //check the validation the input parameters
        analyze=checkParams(curParams, newNet); 
        
        if (analyze == INTERRUPTED || analyze == EXISTS)
            JOptionPane.showMessageDialog(Cytoscape.getDesktop(), 
            		interruptedMessage, "Interrupted", JOptionPane.WARNING_MESSAGE);
        else{            
            //update the parameter set with this result title
        	resultParaSet=ParameterSet.getInstance().setParams(curParams, halfResultTitle + (resultCounter + 1), 
            		network.getIdentifier());
        	AnalyzeTask task = new AnalyzeTask(network, analyze, halfResultTitle + (resultCounter + 1),
        			alg, curParams.getAlgorithm(),curParams);
            JTaskConfig config = new JTaskConfig();
            config.displayCancelButton(true);
            config.displayStatus(true);
            //Execute Task via TaskManager
            //This automatically pops-open a JTask Dialog Box
            TaskManager.executeTask(task, config);
            if (task.isCompletedSuccessfully()) {
                if (task.getComplexes().length > 0) {
                    resultFound = true;
                    resultCounter++;
                    resultPanel = new ResultPanel(
                            task.getComplexes(),
                            task.getAlg(),
                            network,
                            task.getImageList(),
                            halfResultTitle + resultCounter
                    );
                    JOptionPane.showMessageDialog(Cytoscape.getDesktop(), 
                    		""+task.getComplexes().length+" complexes have been identified.","Completed",
                    		JOptionPane.INFORMATION_MESSAGE);
                } else {
                    resultFound = false;
                    JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                            "No clusters were found.\n" +
                            "You can try changing the parameters or\n" +
                            "modifying your node selection if you are using\n" +
                            "a selection-specific scope.",
                            "No Results", JOptionPane.WARNING_MESSAGE);
                }
            }
        }
        //add ResultPanel to right cytopanel
        CytoscapeDesktop desktop = Cytoscape.getDesktop();
        CytoPanel cytoPanel = desktop.getCytoPanel(SwingConstants.EAST);
        //if there is no change, then we simply focus the last produced results (below), otherwise we
        //load the new results panel
        if (resultFound) {
            String resultTitle = halfResultTitle + resultCounter;
            resultPanel.setResultTitle(resultTitle);
            URL iconURL = ClusterPlugin.class.getResource("resources/results.gif");
            if (iconURL != null) {
                Icon icon = new ImageIcon(iconURL);
                String tip = "complexes identified";
                cytoPanel.add(resultTitle, icon, resultPanel, tip);
            } else {
                cytoPanel.add(resultTitle, resultPanel);
            }
            resultParaSet.setResultTitle(resultTitle);
        }
        //Ensures that the east cytopanel is not loaded if there are no results in it
        if (resultFound || analyze == EXISTS || (analyze == INTERRUPTED && cytoPanel.indexOfComponent(resultPanel) >= 0)) {
            //focus the result panel
            if(resultFound)
            	resultIndex = cytoPanel.indexOfComponent(resultPanel);
            System.err.println("-----------ResultPanel:"+resultIndex);
            cytoPanel.setSelectedIndex(resultIndex);
            cytoPanel.setState(CytoPanelState.DOCK);

            //make sure that the visual style is applied whenever new results are produced
            VisualMappingManager vmm = Cytoscape.getVisualMappingManager();
            vistyle.initCalculators();
            vmm.setVisualStyle(vistyle);
            vmm.applyAppearances();
        }
    }
    

	/**
	 * check the values of the input parameters so as to take corresponding action
	 * @param params The set of input parameters
	 * @return the code of action to be taken
	 */
	private int checkParams(ParameterSet curParams, boolean newNet){
		int analyze=-1;

		if (curParams.getScope().equals(ParameterSet.SELECTION) && curParams.getSelectedNodes().length < 1) {
            analyze = INTERRUPTED;
            interruptedMessage= "At least one nodes should be selected";
        }else{
        	String which=curParams.getAlgorithm();      
        	if(which.length()==0){	//if no algorithm is selected
        		analyze=INTERRUPTED;
        		interruptedMessage="An algorithm need to be selected for clustering";
        	}else{
        		if(!newNet){
        			//get list of copys of the saved parameters for comparison with the current ones
            		Collection clParaSets=ParameterSet.getInstance().getAllParamSets().values();
        			Iterator it=clParaSets.iterator();
    		    	ParameterSet curParaSet;
        			while(it.hasNext()){
        				curParaSet=(ParameterSet)it.next();
        				if(checkEqual(curParams,curParaSet)){	//exists
        					CytoPanel panel = Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);
        					resultIndex=panel.indexOfComponent(curParaSet.getResultTitle());
        					analyze=EXISTS;
            				interruptedMessage="The result exits";
            				break;
        				}
        			}
    			}
    			if(newNet || analyze != EXISTS){
        	    	if(which.equals(ParameterSet.MCODE)){
        	    		if(!newNet){
                			ParameterSet savedParamsCopy=ParameterSet.getInstance().getParamsCopy(curParams.getNetworkID());
            	        	if ( savedParamsCopy.getAlgorithm()!=ParameterSet.MCODE ||
            	        			curParams.isIncludeLoops() != savedParamsCopy.isIncludeLoops() ||
            	        			curParams.getDegreeThreshold() != savedParamsCopy.getDegreeThreshold())
            	        		analyze = RESCORE;
            	        	else	analyze = REFIND;
        	    		}
        	    		else    analyze = RESCORE;
        	    	}
        	    	else{
        	    		if(which.equals(ParameterSet.EAGLE))
    	        			analyze = CLIQUEBASED;
        	    		else
            				if (curParams.isOverlapped())	analyze = FINDCLIQUE;
            				else	analyze = FIND;
        	    	}
    			}
        	}
        }
		return analyze;
	}
	//check if two sets of Parameter are equal or not 
	private static boolean checkEqual(ParameterSet PSa, ParameterSet PSb){
		if(!PSa.getNetworkID().equals(PSb.getNetworkID()) ||
				!PSa.getAlgorithm().equals(PSb.getAlgorithm()))
			return false;
		if(!PSa.getScope().equals(PSb.getScope()) || 
				(PSa.getScope().equals(ParameterSet.SELECTION) &&
						PSa.getSelectedNodes() != PSb.getSelectedNodes()))
			return false;
		else{
			if(PSa.getAlgorithm().equals(ParameterSet.FAGEC)){
	        	if (PSa.isWeak() == PSb.isWeak() &&	        			
	        			PSa.getFThreshold() == PSb.getFThreshold() &&
	        			PSa.getComplexSizeThreshold()==PSb.getComplexSizeThreshold()&&
	        			PSa.isOverlapped() == PSb.isOverlapped()&&
	        			PSa.getCliqueSizeThreshold() == PSb.getCliqueSizeThreshold()
	        			 ) 
	        		return true;
			}else {
				if(PSa.getAlgorithm().equals(ParameterSet.MCODE)){
	            	if ( PSa.isIncludeLoops() == PSb.isIncludeLoops() &&
	            			PSa.getDegreeThreshold() == PSb.getDegreeThreshold() &&
	            			PSa.getKCore() == PSb.getKCore() &&
	            			PSa.isHaircut() == PSb.isHaircut() &&
	            			PSa.getNodeScoreCutoff() == PSb.getNodeScoreCutoff() &&
	            			PSa.getMaxDepthFromStart() == PSb.getMaxDepthFromStart() &&
	            			PSa.isFluff() == PSb.isFluff() &&
	            			(!PSa.isFluff() || (PSa.isFluff() && 
	            					PSa.getFluffNodeDensityCutoff() == PSb.getFluffNodeDensityCutoff())))
	            		return true;
				}
				else{	//EAGLE
					if (PSa.getCliqueSizeThreshold1()== PSb.getCliqueSizeThreshold1()&&
							PSa.getComplexSizeThreshold1()==PSb.getComplexSizeThreshold1())
						return true;
				}
			}
		}
		return false;
	}
    
    /**
     * Score the network and find clusters.
     */
    private class AnalyzeTask implements Task {
        private TaskMonitor taskMonitor = null;
        private boolean interrupted = false;
        private CyNetwork network = null;
        private Algorithm alg = null;
        private String which="K-Clique";
        private Cluster[] complexes = null;
        private Image imageList[] = null;
        private boolean completedSuccessfully = false;
        private int analyze;
        private String resultTitle;
        private ParameterSet params;

        /**
         * Scores and finds clusters in a given network
         *
         * @param network The network to cluster
         * @param analyze Tells the task if we need to rescore and/or refind
         * @param resultTitle Identifier of the current result set
         * @param alg reference to the algorithm for this network
         */
        public AnalyzeTask(CyNetwork network, int analyze, String resultTitle, Algorithm alg, 
        		String which,ParameterSet params) {
            this.network = network;
            this.analyze = analyze;
            this.resultTitle = resultTitle;
            this.alg = alg;
            this.which=which;
            this.params=params;
       }

        public void run() {
            if (taskMonitor == null) {
                throw new IllegalStateException("The Task Monitor has not been set.");
            }
            int imageSize = 80;
            if(which.equals(ParameterSet.MCODE)){
            try {
                alg.setTaskMonitor(taskMonitor, network.getIdentifier());
                //only (re)score the graph if the scoring parameters have been changed
                if (analyze == RESCORE) {
                    taskMonitor.setPercentCompleted(0);
                    taskMonitor.setStatus("Step 1 of 3:Scoring the Network...");
                    System.out.println("Step 1 of 3:Scoring the Network...");
                    alg.scoreGraph(network, resultTitle);
                    if (interrupted)
                        return;
                    System.err.println("Scoring: Time spent " + alg.getLastScoreTime() + " ms.");
                }
                taskMonitor.setPercentCompleted(0);
                taskMonitor.setStatus("Step 2 of 3:Identifying Clusters...");
                complexes = alg.K_CoreFinder(network, resultTitle);
                if (interrupted)
                    return;
                taskMonitor.setPercentCompleted(0);
                taskMonitor.setStatus("Step 3 of 3: Drawing the Result Network...");
                //create all the images here for the clusters, it can be a time consuming operation
                complexes = ClusterUtil.sortClusters(complexes);
                imageList = new Image[complexes.length];
                for (int i = 0; i < complexes.length; i++) {
                    if (interrupted) {
                        return;
                    }
                    imageList[i] = ClusterUtil.convertClusterToImage(null, complexes[i], imageSize, imageSize, null, true);
                    taskMonitor.setPercentCompleted((i * 100) / complexes.length);
                }
                completedSuccessfully = true;
            } catch (Exception e) {
                taskMonitor.setException(e, "Clustering cancelled!");
            }}
            else if(which.equals(ParameterSet.EAGLE)){
                try {
                    alg.setTaskMonitor(taskMonitor, network.getIdentifier());
                    taskMonitor.setPercentCompleted(0);
                    taskMonitor.setStatus("Step 1 of 3:Calculate all the maximal Clique...");
                    alg.getMaximalCliques(network,resultTitle);
                    System.err.println("Finding clique: Time spent " + alg.getFindCliquesTIme() + " ms.");
                    if (interrupted )
                        return;
                    taskMonitor.setPercentCompleted(0);
                    taskMonitor.setStatus("Step 2 of 3:Generating Complexes...");
                    complexes = alg.EAGLEFinder(network, resultTitle);
                    if (interrupted)
                        return;
                    taskMonitor.setPercentCompleted(0);
                    taskMonitor.setStatus("Step 3 of 3: Drawing the Result Network...");
                    //create all the images here for the clusters, it can be a time consuming operation
                    complexes = ClusterUtil.sortClusters2(complexes);
                    imageList = new Image[complexes.length];
                    for (int i = 0; i < complexes.length; i++) {
                        if (interrupted) {
                            return;
                        }
                        imageList[i] = ClusterUtil.convertClusterToImage(null, complexes[i], imageSize, imageSize, null, true);
                        taskMonitor.setPercentCompleted((i * 100) / complexes.length);
                    }
                    completedSuccessfully = true;
                } catch (Exception e) {
                    taskMonitor.setException(e, "Clustering cancelled!");
                }
            }
            else if(which.equals(ParameterSet.FAGEC)){
                try {
                    alg.setTaskMonitor(taskMonitor, network.getIdentifier());
                    if (analyze == FINDCLIQUE) {
                        taskMonitor.setPercentCompleted(0);
                        taskMonitor.setStatus("Step 1 of 3:Calculate all the maximal Clique...");
                        alg.getMaximalCliques(network, resultTitle);
                        if (interrupted )
                            return;
                        taskMonitor.setPercentCompleted(0);
                        taskMonitor.setStatus("Step 2 of 3:Generating Complexes...");
                        complexes=alg.FAG_ECXFinder(network, resultTitle);
                        if (interrupted )
                            return;
                        taskMonitor.setPercentCompleted(0);
                        taskMonitor.setStatus("Step 3 of 3: Drawing the Result Network...");
                        //create all the images here for the clusters, it can be a time consuming operation
                        if(params.isWeak())
                        	complexes = ClusterUtil.sortClusters3(complexes);
                        else
                        	complexes=ClusterUtil.sortClusters2(complexes);
                        imageList = new Image[complexes.length];
                        for (int i = 0; i < complexes.length; i++) {
                            if (interrupted) {
                                return;
                            }
                            imageList[i] = ClusterUtil.convertClusterToImage(null, complexes[i], imageSize, imageSize, null, true);
                            taskMonitor.setPercentCompleted((i * 100) / complexes.length);
                        }
                        completedSuccessfully = true;
                    }
                    else{
                        taskMonitor.setPercentCompleted(0);
                        taskMonitor.setStatus("Step 2 of 3:Generating Complexes...");
                        complexes = alg.FAG_ECFinder(network, resultTitle);
                    	System.err.println("After FAG-EC.Time used:"+alg.getLastFindTime());
                        if (interrupted )
                            return;
                        taskMonitor.setPercentCompleted(0);
                        taskMonitor.setStatus("Step 3 of 3: Drawing the Result Network...");
                        //create all the images here for the clusters, it can be a time consuming operation
                        if(params.isWeak())
                        	complexes = ClusterUtil.sortClusters3(complexes);
                        else
                        	complexes=ClusterUtil.sortClusters2(complexes);
                        imageList = new Image[complexes.length];
                        for (int i = 0; i < complexes.length; i++) {
                            if (interrupted) {
                                return;
                            }
                            imageList[i] = ClusterUtil.convertClusterToImage(null, complexes[i], imageSize, imageSize, null, true);
                            taskMonitor.setPercentCompleted((i * 100) / complexes.length);
                        }
                        completedSuccessfully = true;
                    }
                    if (interrupted )
                        return;
                } catch (Exception e) {
                    taskMonitor.setException(e, "Clustering cancelled!");
                }
            }
        }
        public boolean isCompletedSuccessfully() {
            return completedSuccessfully;
        }
        public Cluster[] getComplexes() {
            return complexes;
        }
        public Image[] getImageList() {
            return imageList;
        }
        public void halt() {
            this.interrupted = true;
            alg.setCancelled(true);
        }
        public void setTaskMonitor(TaskMonitor taskMonitor) throws IllegalThreadStateException {
            if (this.taskMonitor != null) {
                throw new IllegalStateException("Task Monitor has already been set.");
            }
            this.taskMonitor = taskMonitor;
        }
        public String getTitle() {
        	StringBuffer state=new StringBuffer("Identifying Modules...");
        	if(this.which.equals(ParameterSet.EAGLE))
        		state.append("This process may take a little longer time. Please wait");
            return state.toString();
        }    
        public Algorithm getAlg() {
            return alg;
        }
    }
}
