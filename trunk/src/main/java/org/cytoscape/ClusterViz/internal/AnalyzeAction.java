package org.cytoscape.ClusterViz.internal;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;



import org.cytoscape.ClusterViz.internal.algorithm.Algorithm;
import org.cytoscape.ClusterViz.internal.algorithm.EAGLE;
import org.cytoscape.ClusterViz.internal.algorithm.FAGEC;
import org.cytoscape.ClusterViz.internal.algorithm.MCODE;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

import org.cytoscape.model.events.AddedEdgesEvent;
import org.cytoscape.model.events.AddedEdgesListener;
import org.cytoscape.model.events.AddedNodesEvent;
import org.cytoscape.model.events.AddedNodesListener;
import org.cytoscape.model.events.RemovedEdgesEvent;
import org.cytoscape.model.events.RemovedEdgesListener;
import org.cytoscape.model.events.RemovedNodesEvent;
import org.cytoscape.model.events.RemovedNodesListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Classe to handle the action of clicking Button Analyze.
 */
public class AnalyzeAction extends AbstractVizAction
		implements SetCurrentNetworkListener, AddedNodesListener, AddedEdgesListener, RemovedNodesListener, RemovedEdgesListener {
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
    ParameterSet  curParams;
//    ClusterVisualStyle vistyle;
    private String interruptedMessage="";
    private int resultIndex;
    
    private final ClusterUtil clusterUtil;
    
    private static final long serialVersionUID = 0x1385f3897d8b2b0L;
	public static final int INTERRUPTION = 3;
	private final CyServiceRegistrar registrar;
	private final TaskManager taskManager;
	private Map<Long, Boolean>  dirtyNetworks;
	
	
	
    
    public AnalyzeAction(String title, CyApplicationManager applicationManager, CySwingApplication swingApplication, 
    		CyNetworkViewManager netViewManager, CyServiceRegistrar registrar, 
    		TaskManager taskManager,ParameterSet  curParams,
    		ClusterUtil util)
	{
		super(title, applicationManager, swingApplication, netViewManager, "network");
		analyze = 0;
		this.registrar = registrar;
		this.taskManager = taskManager;
		this.curParams = curParams;
		this.clusterUtil=util;
		this.dirtyNetworks = new HashMap();
	}
    
    
/*    AnalyzeAction () {}
*/
	/**
	 * This is constructor for AnalyzeAction class
	 *
	 * @param curParams current parameter set
	 * @param vistyle
	 */
/*    AnalyzeAction ( ClusterVisualStyle vistyle) {
        
        this.vistyle = vistyle;
        networkManager = new HashMap();
    }*/
	/**
     * This method is called when the user clicks Analyze.
     *
     * @param event Click of the analyzeButton on the MainPanel.
     */
    public void actionPerformed(ActionEvent event) {
        resultFound = false;
        CurrentParameters resultParaSet=null;
        //get the network object, this contains the graph
        final CyNetwork network = applicationManager.getCurrentNetwork();
        final CyNetworkView networkView = this.applicationManager.getCurrentNetworkView();
        
        ParameterSet currentParamsCopy = getMainPanel().getCurrentParamsCopy();
 //       curParams.setNetworkID(network.getSUID());
        if (network == null) {
            System.err.println("Can't get a network.");
            return;
        }
        if (network.getNodeCount() < 1) {
            JOptionPane.showMessageDialog(null/*Cytoscape.getDesktop()*/,
                    "Network has not been loaded!", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        
        //get selected node
        List<CyNode> nodes = network.getNodeList();
        Set<Long> selectedNodes = new HashSet<Long>();
		for (Iterator iterator = nodes.iterator(); iterator.hasNext();)
		{
			CyNode n = (CyNode)iterator.next();
			if (((Boolean)network.getRow(n).get("selected", java.lang.Boolean.class)).booleanValue())
				selectedNodes.add(n.getSUID());
		}
		
		
//        Set selectedNodes = network.getSelectedNodes();
        Long[] selectedNodesRGI = new Long[selectedNodes.size()];
        int c = 0;
        for(Iterator i = selectedNodes.iterator(); i.hasNext();)
        	{
        	Long nodeID = (Long) i.next();
        	selectedNodesRGI[c++]=nodeID;
        	}
        
       /* for (Iterator i = selectedNodes.iterator(); i.hasNext();) {
            CyNode node = (CyNode) i.next();
            selectedNodesRGI[c] = node.getSUID();
            c++;
        }*/
        //notice, here we set the selected nodes

        currentParamsCopy.setSelectedNodes(selectedNodesRGI);

        Algorithm alg1 = null;
        boolean newNet=isDirty(network);//judge new network
        
         

        

        ParameterSet savedParamsCopy;
        if (this.clusterUtil.containsNetworkAlgorithm(network.getSUID().longValue())) {
//        	alg1 = this.clusterUtil.getNetworkAlgorithm(network.getSUID().longValue());

        	
        	
          savedParamsCopy = this.clusterUtil.getCurrentParameters().getParamsCopy(network.getSUID());
          
          analyze=checkParams(currentParamsCopy, newNet); 
          
          
        } else {
        	//clusterUtil.getNetworkAlgorithm(network.getSUID().longValue())
        	
          
          
          savedParamsCopy = this.clusterUtil.getCurrentParameters().getParamsCopy(null);
         
                  
          this.analyze = 0;
      	}
        

        
        

        
        if (analyze == INTERRUPTED || analyze == EXISTS)
        	JOptionPane.showMessageDialog(AnalyzeAction.this.swingApplication.getJFrame(),
            		interruptedMessage, "Interrupted", JOptionPane.WARNING_MESSAGE);
        else{  
        final int resultId = this.clusterUtil.getCurrentResultId();

        if ((this.analyze == 0) || (isDirty(network)) || (!currentParamsCopy.getAlgorithm().equals(savedParamsCopy.getAlgorithm())) ||
          (currentParamsCopy.isIncludeLoops() != savedParamsCopy.isIncludeLoops()) || 
          (currentParamsCopy.getDegreeCutoff() != savedParamsCopy.getDegreeCutoff())) {
          this.analyze = 1;
 //         logger.debug("Analysis: score network, find clusters");
          this.clusterUtil.getCurrentParameters().setParams(currentParamsCopy, resultId, network.getSUID());
        } else if (!checkEqual(savedParamsCopy, currentParamsCopy)) {
          this.analyze = 2;
  //        logger.debug("Analysis: find clusters");
          this.clusterUtil.getCurrentParameters().setParams(currentParamsCopy, resultId, network.getSUID());
        } else {
          this.analyze = 3;
          interruptedMessage = "The parameters you specified have not changed.";
          this.clusterUtil.getCurrentParameters().setParams(currentParamsCopy, resultId, network.getSUID());
        }

        if(currentParamsCopy.getAlgorithm().equals(ParameterSet.MCODE ))
            alg1 = new MCODE(network.getSUID(), this.clusterUtil);
        if(currentParamsCopy.getAlgorithm().equals(ParameterSet.EAGLE ))
            alg1 = new EAGLE(network.getSUID(), this.clusterUtil);
        if(currentParamsCopy.getAlgorithm().equals(ParameterSet.FAGEC ))
            alg1 = new FAGEC(network.getSUID(), this.clusterUtil);
        this.clusterUtil.addNetworkAlgorithm(network.getSUID().longValue(), alg1);
        
 /*       
        //if (!networkManager.containsKey(network.getIdentifier())){
        	newNet=true;
			alg = curParams.getAlg();
            //alg = new Algorithm(null);
            networkManager.put(network.getSUID(), alg);
        //}
        //else alg = (Algorithm) networkManager.get(network.getIdentifier());
        //check the validation the input parameters
        analyze=checkParams(curParams, newNet); 
        
        
        resultId = this.clusterUtil.getCurrentResultId();
        
        if (analyze == INTERRUPTED || analyze == EXISTS)
            JOptionPane.showMessageDialog(nullCytoscape.getDesktop(), 
            		interruptedMessage, "Interrupted", JOptionPane.WARNING_MESSAGE);
        else{            
            //update the parameter set with this result title
        	clusterUtil.getCurrentParameters().setParams(curParams,  (resultCounter + 1), 
            		network.getSUID());*/
        	

        if ((currentParamsCopy.getScope().equals(ParameterSet.SELECTION)) && 
          (currentParamsCopy.getSelectedNodes().length < 1)) {
          this.analyze = 3;
          interruptedMessage = "You must select ONE OR MORE NODES\nfor this scope.";
        }

        if (this.analyze == 3) {
          JOptionPane.showMessageDialog(this.swingApplication.getJFrame(), 
            interruptedMessage, 
            "Analysis Interrupted", 
            2);
        }
        else {
        	
             
         final	Algorithm alg2=alg1;
       	 AnalysisCompletedListener listener = new AnalysisCompletedListener()
            {
              public void handleEvent(AnalysisCompletedEvent e)
              {
                ResultPanel resultsPanel = null;
                boolean resultFound = false;
                AnalyzeAction.this.setDirty(network, false);

                if (e.isSuccessful()) {
                  if ((e.getClusters() != null) && (!e.getClusters().isEmpty())) {
                	  
                	  
                
                			
//                			CyAttributes cyAttributes = Cytoscape.getNodeAttributes();
                			/*int length=0;
                			for(int i=0; i < e.getClusters().size(); i++){
                				Cluster c = (Cluster)e.getClusters().get(i);
                				CyNetwork clusterNetwork = c.getNetwork();
                				
                				length = length + clusterNetwork.getNodeCount();
                				//length = length + complexes[i].getGPCluster().getNodeIndicesArray().length;
                			}
                			int[] clusteredNodes;
                			clusteredNodes = new int[length];
                			int index=0;
                			for(int i=0; i < e.getClusters().size(); i++){
                				Cluster c = (Cluster)e.getClusters().get(i);
                				CyNetwork clusterNetwork = c.getNetwork();
                				Iterator itr = clusterNetwork.getNodeList().iterator();
                				
                				//Iterator itr = complexes[i].getGPCluster().nodesIterator();
                				while(itr.hasNext()){
                					
                					CyNode node = (CyNode) itr.next();
                					System.out.println(node.getSUID());
                					CyRow nodeRow = clusterNetwork.getRow(node);
                					nodeRow.set("Cluster", Integer.valueOf(i) );
                					//cyAttributes.setAttribute(node.getSUID(), "Cluster", i);
                				}
                			}*/
                		
                		
                	  
                    resultFound = true;
                    AnalyzeAction.this.clusterUtil.addNetworkResult(network.getSUID().longValue());

                    DiscardResultAction discardResultAction = new DiscardResultAction(
                      "Discard Result", 
                      resultId, 
                      AnalyzeAction.this.applicationManager, 
                      AnalyzeAction.this.swingApplication, 
                      AnalyzeAction.this.netViewManager, 
                      AnalyzeAction.this.registrar, 
                      AnalyzeAction.this.clusterUtil);

                    resultsPanel = new ResultPanel(e.getClusters(), alg2, AnalyzeAction.this.clusterUtil, network, networkView, 
                      resultId, discardResultAction);

                    AnalyzeAction.this.registrar.registerService(resultsPanel, CytoPanelComponent.class, new Properties());
                  } else {
                    JOptionPane.showMessageDialog(AnalyzeAction.this.swingApplication.getJFrame(), 
                      "No clusters were found.\nYou can try changing the MCODE parameters or\nmodifying your node selection if you are using\na selection-specific scope.", 
                      "No Results", 
                      2);
                  }
                }

                CytoPanel cytoPanel = AnalyzeAction.this.swingApplication.getCytoPanel(CytoPanelName.EAST);

                if ((resultFound) || ((AnalyzeAction.this.analyze == 3) && (cytoPanel.indexOfComponent(resultsPanel) >= 0)))
                {
                  int index = cytoPanel.indexOfComponent(resultsPanel);
                  cytoPanel.setSelectedIndex(index);

                  if (cytoPanel.getState() == CytoPanelState.HIDE) cytoPanel.setState(CytoPanelState.DOCK);
                }
              }
            };
            
        	
        	AnalyzeTaskFactory analyzeTaskFactory = new AnalyzeTaskFactory(network, this.analyze, resultId, alg1, 
                    this.clusterUtil, listener);
            
        	this.taskManager.execute(analyzeTaskFactory.createTaskIterator());
        	
   
            
        }
        }
        //add ResultPanel to right cytopanel
        
/*        CytoPanel cytoPanel = swingApplication.getCytoPanel(CytoPanelName.WEST);
        
        CytoscapeDesktop desktop = Cytoscape.getDesktop();
        CytoPanel cytoPanel = desktop.getCytoPanel(SwingConstants.EAST);
        //if there is no change, then we simply focus the last produced results (below), otherwise we
        //load the new results panel
        if (resultFound) {
            int resultTitle =  resultCounter;
            //resultPanel.setResultId(resultTitle);
            
//            java.net.URL iconURL = Resources.getUrl(org.cytoscape.myappViz.internal.Resources.ImageName.ARROW_EXPANDED);
            
            
//           URL iconURL = ClusterPlugin.class.getResource("resources/results.gif");
            URL iconURL = null;
            if (iconURL != null) {
                Icon icon = new ImageIcon(iconURL);
                String tip = "complexes identified";
                
                
                
                
                registrar.registerService(resultPanel,CytoPanelComponent.class, new Properties());
                
//                cytoPanel.add(resultTitle, icon, resultPanel, tip);
            } else {
            	registrar.registerService(resultPanel,CytoPanelComponent.class, new Properties());
//                cytoPanel.add(resultTitle, resultPanel);
            }
//            resultParaSet.setResultTitle(resultTitle);
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
            
            VisualMappingManager vmm = netViewM.getVisualMappingManager();
            vistyle.initCalculators();
            vmm.setVisualStyle(vistyle);
            vmm.applyAppearances();
        }*/
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
            		Collection clParaSets=this.clusterUtil.getCurrentParameters().getAllParamSets().values();
        			Iterator it=clParaSets.iterator();
    		    	ParameterSet curParaSet;
        			while(it.hasNext()){
        				curParaSet=(ParameterSet)it.next();
        				if(checkEqual(curParams,curParaSet)){	//exists
        					
        					CytoPanel panel = swingApplication.getCytoPanel(CytoPanelName.WEST.EAST);
        					//CytoPanel panel = Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);
        					resultIndex=panel.indexOfComponent(resultPanel);
 //       					analyze=EXISTS;
 //           				interruptedMessage="The result exits";
            				break;
        				}
        			}
    			}
    			if(newNet || analyze != EXISTS){
        	    	if(which.equals(ParameterSet.MCODE)){
        	    		if(!newNet){
        	    			
        	    			
        	    			
        	    			ParameterSet savedParamsCopy;//=this.clusterUtil.getCurrentParameters().getParamsCopy(applicationManager.getCurrentNetwork().getSUID()/*network.getNetworkID()*/);
        	    			 if (this.clusterUtil.containsNetworkAlgorithm(applicationManager.getCurrentNetwork().getSUID().longValue())) {
//        	    		        	alg1 = this.clusterUtil.getNetworkAlgorithm(network.getSUID().longValue());

        	    		          savedParamsCopy = this.clusterUtil.getCurrentParameters().getParamsCopy(applicationManager.getCurrentNetwork().getSUID());
        	    		        } else {
        	    		        	//clusterUtil.getNetworkAlgorithm(network.getSUID().longValue())
        	    		        	
        	    		          
        	    		          
        	    		          savedParamsCopy = this.clusterUtil.getCurrentParameters().getParamsCopy(null);
        	    		         
        	    			
        	    		        }
            	        	if ( savedParamsCopy.getAlgorithm()!=ParameterSet.MCODE ||
            	        			curParams.isIncludeLoops() != savedParamsCopy.isIncludeLoops() ||
            	        			curParams.getDegreeCutoff() != savedParamsCopy.getDegreeCutoff())
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
	
	 private void setDirty(CyNetwork net, boolean dirty) {
		    if (this.clusterUtil.containsNetworkAlgorithm(net.getSUID().longValue()))
		      if (dirty)
		        this.dirtyNetworks.put(net.getSUID(), Boolean.valueOf(dirty));
		      else
		        this.dirtyNetworks.remove(net.getSUID());
		  }
	//check if two sets of Parameter are equal or not 
	private static boolean checkEqual(ParameterSet  PSa, ParameterSet  PSb)
	{
		if(!PSa.getAlgorithm().equals(PSb.getAlgorithm()))
			return false;
		if(!PSa.getScope().equals(PSb.getScope()) || 
				(PSa.getScope().equals(ParameterSet.SELECTION) &&
						PSa.getSelectedNodes() != PSb.getSelectedNodes()))
			return false;
		else{
			if(PSa.getAlgorithm().equals(ParameterSet.FAGEC)){
	        	if (PSa.isWeak() == PSb.isWeak() &&	        			
	        			PSa.getfThreshold() == PSb.getfThreshold() &&
	        			PSa.getComplexSizeThreshold()==PSb.getComplexSizeThreshold()&&
	        			PSa.isOverlapped() == PSb.isOverlapped()&&
	        			PSa.getCliqueSizeThreshold() == PSb.getCliqueSizeThreshold()
	        			 ) 
	        		return true;
			}else {
				if(PSa.getAlgorithm().equals(ParameterSet.MCODE)){
	            	if ( PSa.isIncludeLoops() == PSb.isIncludeLoops() &&
	            			PSa.getDegreeCutoff() == PSb.getDegreeCutoff() &&
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
	/*private static boolean checkEqual(ParameterSet   p1, ParameterSet   p2)
	  {
		    boolean b = !p2.getScope().equals(p1.getScope());
		    b = (b) || ((!p2.getScope().equals(ParameterSet.NETWORK)) && (p2.getSelectedNodes() != p1.getSelectedNodes()));
		    b = (b) || (p2.isOptimize() != p1.isOptimize());
		    b = (b) || ((!p2.isOptimize()) && ((p2.getKCore() != p1.getKCore()) || 
		      (p2.getMaxDepthFromStart() != p1.getMaxDepthFromStart()) || 
		      (p2.isHaircut() != p1.isHaircut()) || 
		      (p2.getNodeScoreCutoff() != p1.getNodeScoreCutoff()) || 
		      (p2.isFluff() != p1.isFluff()) || (
		      (p2.isFluff()) && (p2.getFluffNodeDensityCutoff() != p1.getFluffNodeDensityCutoff()))));
		    return b;
		  }

	
*/


	  public void handleEvent(SetCurrentNetworkEvent e)
	  {
	    updateEnableState();
	  }

	  public void handleEvent(RemovedEdgesEvent e)
	  {
	    setDirty((CyNetwork)e.getSource(), true);
	  }

	  public void handleEvent(RemovedNodesEvent e)
	  {
	    setDirty((CyNetwork)e.getSource(), true);
	  }

	  public void handleEvent(AddedEdgesEvent e)
	  {
	    setDirty((CyNetwork)e.getSource(), true);
	  }

	  public void handleEvent(AddedNodesEvent e)
	  {
	    setDirty((CyNetwork)e.getSource(), true);
	  }
	  
	  
	  private boolean isDirty(CyNetwork net)
	  {
	    return Boolean.TRUE.equals(this.dirtyNetworks.get(net.getSUID()));
	  }
    /**
     * Score the network and find clusters.
     */

	  
	  public class MCODEAnalyzeTask implements Task {

			private final Algorithm alg;
			private final ClusterUtil mcodeUtil;
			private final int analyze;
			private final int resultId;
			private final AnalysisCompletedListener listener;

			private boolean interrupted;
			private CyNetwork network;
			
			private final Logger logger = LoggerFactory.getLogger(MCODEAnalyzeTask.class);

			/**
			 * Scores and finds clusters in a given network
			 *
			 * @param network The network to cluster
			 * @param analyze Tells the task if we need to rescore and/or refind
			 * @param resultId Identifier of the current result set
			 * @param alg reference to the algorithm for this network
			 */
			public MCODEAnalyzeTask(final CyNetwork network,
									final int analyze,
									final int resultId,
									final Algorithm alg,
									final ClusterUtil mcodeUtil,
									final AnalysisCompletedListener listener) {
				this.network = network;
				this.analyze = analyze;
				this.resultId = resultId;
				this.alg = alg;
				this.mcodeUtil = mcodeUtil;
				this.listener = listener;
			}

			/**
			 * Run MCODE (Both score and find steps).
			 */
			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
				if (taskMonitor == null) {
					throw new IllegalStateException("Task Monitor is not set.");
				}

				boolean success = false;
				List<Cluster> clusters = null;
				mcodeUtil.resetLoading();

				try {
					// Run MCODE scoring algorithm - node scores are saved in the alg object
					alg.setTaskMonitor(taskMonitor, network.getSUID());

					// Only (re)score the graph if the scoring parameters have been changed
					if (analyze == AnalyzeAction.RESCORE) {
						taskMonitor.setProgress(0.001);
						taskMonitor.setTitle("MCODE Analysis");
						taskMonitor.setStatusMessage("Scoring Network (Step 1 of 3)");
						alg.scoreGraph(network, resultId);

						if (interrupted) {
							return;
						}

						logger.info("Network was scored in " + alg.getLastScoreTime() + " ms.");
					}

					taskMonitor.setProgress(0.001);
					taskMonitor.setStatusMessage("Finding Clusters (Step 2 of 3)");

					clusters = alg.findClusters(network, resultId);

					if (interrupted) {
						return;
					}

					taskMonitor.setProgress(0.001);
					taskMonitor.setStatusMessage("Drawing Results (Step 3 of 3)");

					// Also create all the images here for the clusters, since it can be a time consuming operation
					mcodeUtil.sortClusters(clusters);
					int imageSize = mcodeUtil.getCurrentParameters().getResultParams(resultId).getDefaultRowHeight();
					int count = 0;

					for (final Cluster c : clusters) {
						if (interrupted) return;
						
						final Image img = mcodeUtil.createClusterImage(c, imageSize, imageSize, null, true, null);
						c.setImage(img);
						taskMonitor.setProgress((++count) / (double) clusters.size());
					}

					success = true;
				} catch (Exception e) {
					throw new Exception("Error while executing the MCODE analysis", e);
				} finally {
					mcodeUtil.destroyUnusedNetworks(network, clusters);
					
					if (listener != null) {
						listener.handleEvent(new AnalysisCompletedEvent(success, clusters));
					}
				}
			}

			@Override
			public void cancel() {
				this.interrupted = true;
				alg.setCancelled(true);
				mcodeUtil.removeNetworkResult(resultId);
				mcodeUtil.removeNetworkAlgorithm(network.getSUID());
			}

			/**
			 * Gets the Task Title.
			 *
			 * @return human readable task title.
			 */
			public String getTitle() {
				return "MCODE Network Cluster Detection";
			}
		}

}
