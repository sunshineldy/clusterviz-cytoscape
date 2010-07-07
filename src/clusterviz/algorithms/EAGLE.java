package clusterviz.algorithms;

import cytoscape.CyNetwork;
import cytoscape.task.TaskMonitor;
import giny.model.GraphPerspective;
import giny.model.Node;
import giny.model.Edge;

import java.util.*;

import clusterviz.*;

public class EAGLE extends Algorithm{

	public EAGLE(String networkID){
		super(networkID);
	}

	/**
     * EAGLE Step1 and FEA-EC Step2(optional) get Maximal Cliques: 
     * get all the maximal cliques in the network
     * @param inputNetwork the operated network
     */
    public void getMaximalCliques(CyNetwork inputNetwork,String resultTitle){
        String callerID = "Algorithm.getMaximalCliques";
        long startTime=System.currentTimeMillis();
        if (inputNetwork == null) {
            System.err.println("In " + callerID + ": inputNetwork was null.");
            return;
        }
    	currentNetwork=inputNetwork;
    	params=getParams();

		String net=inputNetwork.getIdentifier();
    	if(!maximalCliquesNetworkMap.containsKey(inputNetwork.getIdentifier())){
    		System.out.println("Get MaximalCliques for This Network........");
            long msTimeBefore = System.currentTimeMillis();
    		HashMap cliques = new HashMap();
    		
        	//initialize states
    		Vector alCur=new Vector();
    		Vector alFini=new Vector();
    		Vector alNodes=new Vector(inputNetwork.getNodeCount());
    		for(Iterator i=inputNetwork.nodesIterator();i.hasNext();){
    			Integer node=new Integer(((Node)i.next()).getRootGraphIndex());
    			alNodes.add(node);
    		}    		
    		//The critical internal process
    		expand(cliques,alCur,alNodes,alFini);
    		
    		curCliques=cliques;
    		maximalCliquesNetworkMap.put(net, cliques);
    		findCliquesTime=System.currentTimeMillis()-msTimeBefore;
    	}
    	else
    		curCliques=(HashMap)maximalCliquesNetworkMap.get(net);
    	findCliquesTime=System.currentTimeMillis()-startTime;
    }

	/**
     * EAGLE Algorithm for finding complexes
     * @param inputNetwork The input network
     * @param resultTitle Title of the result, used as an identifier in various hash maps
     * @return array of clusters
     */
    public Cluster[] EAGLEFinder(CyNetwork inputNetwork,String resultTitle){
        String callerID = "Algorithm.EAGLEFinder";
    	System.out.println("In "+callerID);
    	currentNetwork=inputNetwork;
    	params=getParams();
    	curCliques=(HashMap)maximalCliquesNetworkMap.get(inputNetwork.getIdentifier());
        if (curCliques == null) {
            System.err.println("In " + callerID + ": maximal cliques Map was null.");
            return (null);
        }
        long msTimeBefore = System.currentTimeMillis();
    	StringBuffer sb=new StringBuffer(inputNetwork.getIdentifier());
    	sb.append(params.getCliqueSizeThreshold1());
    	String key=sb.toString();
        if(optimalDivisionKeyMap.containsKey(key)){
        	curOptimalDivision=(ArrayList)optimalDivisionKeyMap.get(key);
        }
        else{
        	System.out.println("ReGenerate Optimal Complexes Division........");

       /********************Initialize the complexes*************************/
        	ArrayList alClusters=new ArrayList();
            ArrayList optimals=null;
            for(int i=0;i<curCliques.size();i++){
                Clique cur=(Clique)curCliques.get(new Integer(i));
                ArrayList nodes=cur.getCliqueNodes();
                if(nodes.size() >= params.getCliqueSizeThreshold1()){
                    Cluster newCluster=new Cluster();
                	ArrayList alNodes=new ArrayList();
                	Iterator it=nodes.iterator();
                	while(it.hasNext()){
                		Integer n=(Integer)it.next();
                		alNodes.add(n);
                	}
                	newCluster.setALNodes(alNodes);
                	alClusters.add(newCluster);   
                }
                else	cur.setSubordinate(true);
            }
            for(int i=0;i<curCliques.size();i++){
            	Clique cur=(Clique)curCliques.get(new Integer(i));
            	if(cur.isSubordinate()){
            		ArrayList nodes=cur.getCliqueNodes();
            		Iterator it=nodes.iterator();
            		while(it.hasNext()){
                		Integer n=(Integer) it.next();
                		if(searchInComplexes(alClusters,n)==0){	//a subordinate node
                			Cluster newCluster=new Cluster();
                			ArrayList alNodes=new ArrayList();
                			alNodes.add(n);
                			newCluster.setALNodes(alNodes);
                			alClusters.add(newCluster);
                		}
            		}
            	}
            }/******End of initialization*******/
            int findingProgress = 0;
            int findingTotal = alClusters.size();
            double max=0,cur;int num=0;
            //merge two complexes with largest similarity each time until there is only on left
            do{
            	mergeComplex(alClusters);
            	cur=calModularity(alClusters);
        		if(max==0 || cur>max){
        			max=cur;
        			ArrayList temp=new ArrayList();
        	    	for(Iterator i=alClusters.iterator();i.hasNext();){
        	    		Cluster c=(Cluster)i.next();
        	    		Cluster newC=new Cluster();
        	    		ArrayList al=c.getALNodes();
        	    		ArrayList newAl=new ArrayList();
        	        	for(Iterator i1=al.iterator();i1.hasNext();)
        	        		newAl.add((Integer)i1.next());
        	        	newC.setALNodes(newAl);
            	    	temp.add(newC);
        	    	}
        			optimals=temp;
        			//optimal=new ArrayList((ArrayList)alClusters.clone());
        		}
                if (taskMonitor != null) {
                    findingProgress++;
                    int newProgress = (findingProgress * 100) / findingTotal;
                    int oldProgress = ((findingProgress-1) * 100) / findingTotal;
                    if (newProgress != oldProgress) {
                        taskMonitor.setPercentCompleted(newProgress);
                    }
                }
                if (cancelled) {
                    break;
                }
            }while(alClusters.size()>1);
            curOptimalDivision=optimals;
            optimalDivisionKeyMap.put(key, optimals);
        }
        
        ArrayList alClusters=new ArrayList();
        Iterator it=curOptimalDivision.iterator();
        while(it.hasNext()){
        	Cluster cluster=(Cluster)it.next();
        	if(cluster.getALNodes().size()>=params.getComplexSizeThreshold1()){
        		ArrayList alNodes=cluster.getALNodes();
        		GraphPerspective gpCluster =Algorithm.createGraphPerspective(alNodes, inputNetwork);
        		cluster.setGPCluster(gpCluster);
        		cluster.setClusterScore(0.0);
        		cluster.setSeedNode((Integer)alNodes.get(0));
        		cluster.setResultTitle(resultTitle);
        		cluster.calModularity(inputNetwork);
        		alClusters.add(cluster);
        	}
        }
        
        //Once the clusters have been found we either return them or in the case of selection scope,
        //we select only the ones that contain the selected node(s) and return those
        ArrayList selectedALClusters = new ArrayList();
        if (!params.getScope().equals(ParameterSet.NETWORK)) {
            for (Iterator ic = alClusters.iterator(); ic.hasNext();){
                Cluster cluster = (Cluster) ic.next();
                ArrayList alNodes = cluster.getALNodes();
                ArrayList alSelectedNodes = new ArrayList();
                for (int c = 0; c < params.getSelectedNodes().length; c++) {
                    alSelectedNodes.add(params.getSelectedNodes()[c]);
                }
                //method for returning all clusters that contain any of the selected nodes
                boolean hit = false;
                for (Iterator in = alSelectedNodes.iterator(); in.hasNext();) {
                    if (alNodes.contains((Integer) in.next())) {
                        hit = true;
                    }
                }
                if (hit) {
                    selectedALClusters.add(cluster);
                }
            }
            alClusters = selectedALClusters;
        }
        //Finally convert the arraylist into a fixed array
        Cluster[] clusters = new Cluster[alClusters.size()];
        for (int c = 0; c < clusters.length; c++) {
            clusters[c] = (Cluster) alClusters.get(c);
        }
        long msTimeAfter = System.currentTimeMillis();
        lastFindTime = msTimeAfter - msTimeBefore;
        return clusters;
    }

	
    /**
     * Find two complexes with largest similarity, and then merge them
     * @param complexes the list of complexes to be merged
     */
    private void mergeComplex(ArrayList complexes)//the number of complexes must be no less than two
    {
    	double max=-100000,simval;
    	int index1=0,index2=1;
    	int i,j,flag;
    	for(i=0;i<complexes.size()-1;++i){//for each pair of complexes,calculate their similarity
    		for(j=i+1;j<complexes.size();++j){
    			simval=calSimilarity((Cluster)complexes.get(i),(Cluster)complexes.get(j));
    			if(simval>max){
    				max=simval;
    				index1=i;//multiple equalizations, how to resolve?????
    				index2=j;
    			}
    		}
    	}
    	ArrayList nodes1=((Cluster)complexes.get(index1)).getALNodes();
    	ArrayList nodes2=((Cluster)complexes.get(index2)).getALNodes();   
    	for(Iterator it=nodes2.iterator();it.hasNext();){
    		Integer node=(Integer)it.next();
    		if(!nodes1.contains(node))
    			nodes1.add(node);
    	}
    	complexes.remove(index2);
    }

	public Cluster[] run(CyNetwork inputNetwork, String resultTitle){
		getMaximalCliques(inputNetwork, resultTitle);
		return EAGLEFinder(inputNetwork, resultTitle);
	}

}
