package cjry.clusterviz;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Classed to handle the action of clicking Button Analyze.
 */
public class AnalyzeAction implements ActionListener {
    final static int FIRST_TIME = 0;
    final static int RESCORE = 1;
    final static int REFIND = 2;
    final static int FIND=3;
    final static int INTERRUPTED = 4;
    final static int FINDCLIQUE=5;
    final static int CLIQUEBASED=6;
    
    private HashMap<String,Algorithm> networkManager;//Keeps track of netowrks (id is key) and their algorithms 
    private boolean resultFound = false;
    private ResultPanel resultPanel;
    int analyze = FIRST_TIME;
    int resultCounter = 0;
    ParameterSet curParams;
    ClusterVisualStyle vistyle;
    
    AnalyzeAction () {}
    AnalyzeAction (ParameterSet curParams, ClusterVisualStyle vistyle) {
        this.curParams = curParams;
        this.vistyle = vistyle;
        networkManager = new HashMap<String,Algorithm>();
    }

    /**
     * This method is called when the user clicks Analyze.
     *
     * @param event Click of the analyzeButton on the MainPanel.
     */
    public void actionPerformed(ActionEvent event) {
        String halfResultTitle = "Result ";
        String callerID = "AnalyzeAction.actionPerformed";
        String interruptedMessage = "";
        
        //get the network object, this contains the graph
        final CyNetwork network = Cytoscape.getCurrentNetwork();
        if (network == null) {
            System.err.println("In " + callerID + ":");
            System.err.println("Can't get a network.");
            return;
        }
        if (network.getNodeCount() < 1) {
            JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                    "网络未加载", "出错！", JOptionPane.WARNING_MESSAGE);
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
        String which=curParams.getAlgorithm();
        ParameterSet savedParamsCopy;
        //Here we determine if we have already run clustering on this network before
        if (!networkManager.containsKey(network.getIdentifier())) {
            alg = new Algorithm(null);
            savedParamsCopy = ParameterSet.getInstance().getParamsCopy(null);
            networkManager.put(network.getIdentifier(), alg);
            analyze = FIRST_TIME;
        } else {
            alg = (Algorithm) networkManager.get(network.getIdentifier());
            //get a copy of the last saved parameters for comparison with the current ones
            savedParamsCopy = ParameterSet.getInstance().getParamsCopy(network.getIdentifier());
        }
        if(which.length()==0){
        	analyze=INTERRUPTED;
        	interruptedMessage="必须选择一种聚类算法！";
        }else{
        	if(which.equals(ParameterSet.KCORE)){
            	if ( analyze == FIRST_TIME || savedParamsCopy.getAlgorithm()!=ParameterSet.KCORE ||
            			curParams.isIncludeLoops() != savedParamsCopy.isIncludeLoops() ||
            			curParams.getDegreeThreshold() != savedParamsCopy.getDegreeThreshold()) {
            		analyze = RESCORE;
            		System.err.println("Analysis: score network, clustering");
            	} 
            	else if (!curParams.getScope().equals(savedParamsCopy.getScope()) ||
            			(!curParams.getScope().equals(ParameterSet.NETWORK) &&
            					curParams.getSelectedNodes() != savedParamsCopy.getSelectedNodes()) ||
            			curParams.isOptimize() != savedParamsCopy.isOptimize() ||
            			(!curParams.isOptimize() &&
            					(curParams.getKCore() != savedParamsCopy.getKCore() ||
                                curParams.getMaxDepthFromStart() != savedParamsCopy.getMaxDepthFromStart() ||
                                curParams.isHaircut() != savedParamsCopy.isHaircut() ||
                                curParams.getNodeScoreCutoff() != savedParamsCopy.getNodeScoreCutoff() ||
                                curParams.isFluff() != savedParamsCopy.isFluff() ||
                                (curParams.isFluff() &&
                                curParams.getFluffNodeDensityCutoff() != savedParamsCopy.getFluffNodeDensityCutoff())))) {
            		analyze = REFIND;
            	}
            	else{
                    analyze = INTERRUPTED;
            		interruptedMessage = "参数未改变！";            		
            	}
        }else
        if(which.equals(ParameterSet.EAGLE)){
        	if (analyze != FIRST_TIME && savedParamsCopy.getAlgorithm().equals(ParameterSet.EAGLE) &&
        			curParams.getCliqueSizeThreshold1()== savedParamsCopy.getCliqueSizeThreshold1()&&
        			curParams.getComplexSizeThreshold1()==savedParamsCopy.getComplexSizeThreshold1()
        			 ) {
                analyze = INTERRUPTED;
        		interruptedMessage = "极大团大小阈值及模块\n" +
        				"大小参数都未改变！";     
        	} 
        	else analyze=CLIQUEBASED;
        }else
        if(which.equals(ParameterSet.FAGEC)){
        	//curParams.setNodeScoreCutoff(0.0);
        	if (analyze != FIRST_TIME && savedParamsCopy.getAlgorithm().equals(ParameterSet.FAGEC) &&
        			curParams.getScope().equals(savedParamsCopy.getScope())&&
        			curParams.isWeak() == savedParamsCopy.isWeak() &&
        			curParams.getFThreshold() == savedParamsCopy.getFThreshold() &&
        			curParams.getComplexSizeThreshold()==savedParamsCopy.getComplexSizeThreshold()&&
        			curParams.isOverlapped() == savedParamsCopy.isOverlapped()&&
        			curParams.getCliqueSizeThreshold() == savedParamsCopy.getCliqueSizeThreshold()
        			 ) {
        		if( (curParams.getScope().equals(ParameterSet.SELECTION) &&
    					curParams.getSelectedNodes() == savedParamsCopy.getSelectedNodes())||
    					curParams.getScope().equals(ParameterSet.NETWORK))
    					analyze = INTERRUPTED;
        		interruptedMessage = "参数未改变！";     
        	} 
        	else if (curParams.isOverlapped()) {
        		analyze = FINDCLIQUE;
        	}
        	else	analyze=FIND;
        }}
        //update the parameter set with this result title
        ParameterSet.getInstance().setParams(curParams, halfResultTitle + (resultCounter + 1), network.getIdentifier());
        if (curParams.getScope().equals(ParameterSet.SELECTION) && curParams.getSelectedNodes().length < 1) {
            analyze = INTERRUPTED;
            interruptedMessage = "至少需要选择一个点！";
        }
        if (analyze == INTERRUPTED) {
            //stem.err.println("Analysis: interrupted");
            JOptionPane.showMessageDialog(Cytoscape.getDesktop(), interruptedMessage, "分析中止！", JOptionPane.WARNING_MESSAGE);
        } else {
        	AnalyzeTask task = new AnalyzeTask(network, analyze, halfResultTitle + (resultCounter + 1),
        			alg, which,curParams);
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
                    		"共找到个"+task.getComplexes().length+"模块","完成",
                    		JOptionPane.INFORMATION_MESSAGE);
                } else {
                    resultFound = false;
                    JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                            "没有找到满足条件的模块！\n" +
                            "你可以尝试改变所设参数\n" +
                            "如果你所选分析范围是选择区域\n"+
                            "可以尝试改变所选区域",
                            "结果为空！", JOptionPane.WARNING_MESSAGE);
                }
            }
        }
        //add ResultPanel to right cytopanel
        CytoscapeDesktop desktop = Cytoscape.getDesktop();
        CytoPanel cytoPanel = desktop.getCytoPanel(SwingConstants.SOUTH);
        //if there is no change, then we simply focus the last produced results (below), otherwise we
        //load the new results panel
        if (resultFound) {
            String resultTitle = halfResultTitle + resultCounter;
            resultPanel.setResultTitle(resultTitle);
            URL iconURL = ClusterPlugin.class.getResource("resources/results.gif");
            if (iconURL != null) {
                Icon icon = new ImageIcon(iconURL);
                String tip = "聚类结果";
                cytoPanel.add(resultTitle, icon, resultPanel, tip);
            } else {
                cytoPanel.add(resultTitle, resultPanel);
            }
        }
        //Ensures that the east cytopanel is not loaded if there are no results in it
        if (resultFound || (analyze == INTERRUPTED && cytoPanel.indexOfComponent(resultPanel) >= 0)) {
            //focus the result panel
            int index = cytoPanel.indexOfComponent(resultPanel);
            cytoPanel.setSelectedIndex(index);
            cytoPanel.setState(CytoPanelState.DOCK);

            //make sure that the visual style is applied whenever new results are produced
            VisualMappingManager vmm = Cytoscape.getVisualMappingManager();
            vistyle.initCalculators();
            vmm.setVisualStyle(vistyle);
            vmm.applyAppearances();
        }
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
            if(which.equals(ParameterSet.KCORE)){
            try {
                alg.setTaskMonitor(taskMonitor, network.getIdentifier());
                //only (re)score the graph if the scoring parameters have been changed
                if (analyze == AnalyzeAction.RESCORE) {
                    taskMonitor.setPercentCompleted(0);
                    taskMonitor.setStatus("Step 1 of 3:Scoring the Network...");
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
            	System.err.println("Complexes length "+complexes.length);
                imageList = new Image[complexes.length];
                int imageSize = ParameterSet.getInstance().getResultParams(resultTitle).getDefaultRowHeight();
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
                    System.err.println("Generate complexes: Time spent " + alg.getLastFindTime() + " ms.");
                    if (interrupted)
                        return;
                    taskMonitor.setPercentCompleted(0);
                    taskMonitor.setStatus("Step 3 of 3: Drawing the Result Network...");
                    //create all the images here for the clusters, it can be a time consuming operation
                    complexes = ClusterUtil.sortClusters2(complexes);
                	System.err.println("Complexes length "+complexes.length);
                    imageList = new Image[complexes.length];
                    int imageSize = ParameterSet.getInstance().getResultParams(resultTitle).getDefaultRowHeight();
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
                    if (analyze == AnalyzeAction.FINDCLIQUE) {
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
                    	System.err.println("Complexes length "+complexes.length);
                        imageList = new Image[complexes.length];
                        int imageSize = ParameterSet.getInstance().getResultParams(resultTitle).getDefaultRowHeight();
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
                    	System.err.println("Complexes length "+complexes.length);
                        taskMonitor.setPercentCompleted(0);
                        taskMonitor.setStatus("Step 3 of 3: Drawing the Result Network...");
                        //create all the images here for the clusters, it can be a time consuming operation
                        if(params.isWeak())
                        	complexes = ClusterUtil.sortClusters3(complexes);
                        else
                        	complexes=ClusterUtil.sortClusters2(complexes);
                        imageList = new Image[complexes.length];
                        int imageSize = ParameterSet.getInstance().getResultParams(resultTitle).getDefaultRowHeight();
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
        	StringBuffer state=new StringBuffer("模块检测...");
        	if(this.which.equals(ParameterSet.EAGLE))
        		state.append("此过程时间可能较长，请耐心等待耐！");
            return state.toString();
        }    
        public Algorithm getAlg() {
            return alg;
        }
    }
    public static void main(String args[]){
    	ClusterPlugin cp=new ClusterPlugin();
    	cp.test();
    }

}
