package clusterviz.algorithms;

import cytoscape.CyNetwork;
import cytoscape.task.TaskMonitor;
import giny.model.GraphPerspective;
import giny.model.Node;
import giny.model.Edge;

import java.util.*;

import clusterviz.*;


public class FAGEC extends Algorithm{


	public FAGEC(String networkID){
		super(networkID);
	}	

	/**
     * FAG-EC Algorithm Step 1: 
     * Calculate arc weights which is defined as 
     * ( sizOf(Ni Intersect Nj) +1 )/ min[(ki),(kj)]
     *
     * @param inputNetwork The network that will be calculated
     */    
    public void calEdgeWeight(CyNetwork inputNetwork){
        String callerID = "In Algorithm.calEdgeWeight";
    	String networkID=inputNetwork.getIdentifier();
    	if(!edgeWeightNetworkMap.containsKey(networkID)){
            double weight;
            int degree1,degree2,min;
            ArrayList al;
        	TreeMap edgeWeightsMap=new TreeMap(new Comparator(){
        		//sort Doubles in descending order
        		public int compare(Object o1,Object o2){
        			double d1=((Double)o1).doubleValue();
        			double d2=((Double)o2).doubleValue();
        			if(d1==d2){
        				return 0;
        			}
        			else if(d1<d2){
        				return 1;
        			}
        			else return -1;
        		}
        	});  		
        	Iterator edges=inputNetwork.edgesIterator();
        	while(edges.hasNext()&&!cancelled){	//for each edge, cal the weight
        		weight=0.0;
        		int common=0;
        		Edge e=(Edge)edges.next();
        		int nFrom=e.getSource().getRootGraphIndex();
        		int nTo=e.getTarget().getRootGraphIndex();
        		
        		//cal the edge weght
        		int[] neighbors1=getNeighborArray(inputNetwork,nFrom);
        		int[] neighbors2=getNeighborArray(inputNetwork,nTo);
        		Arrays.sort(neighbors1);
        		for(int i=0;i<neighbors2.length;i++){
        			int key=neighbors2[i];
        			if(Arrays.binarySearch(neighbors1, key)>=0)//exist a common neighbor of both nodes
        				common++;
        		}
        		common++;
        		degree1=inputNetwork.getDegree(e.getSource());
        		degree2=inputNetwork.getDegree(e.getTarget());
        		min=degree1<degree2 ? degree1:degree2;
        		weight=(double)(common)/(double)min;
        		
        		//add to the edge weights map
        		if(edgeWeightsMap.containsKey(new Double(weight))) {
        			al=(ArrayList)edgeWeightsMap.get(new Double(weight));
        			al.add(new Integer(e.getRootGraphIndex()));
        		}else{
        			al=new ArrayList();
        			al.add(new Integer(e.getRootGraphIndex()));
        			edgeWeightsMap.put(new Double(weight), al);
        		}
        	}       	
        	curEdgeWeights=edgeWeightsMap;
        	edgeWeightNetworkMap.put(networkID, edgeWeightsMap);
    	}
    	else{
    		curEdgeWeights=(TreeMap)edgeWeightNetworkMap.get(networkID);
    	}
    }

   /**
     * FAG-EC Algorithm Step 2 Generate complexes: 
     * @param inputNetwork The input network
     * @param resultTitle Title of the result, used as an identifier in various hash maps
     * @return the clusters identified
     */
   public Cluster[] FAG_ECFinder(CyNetwork inputNetwork,String resultTitle){
        String callerID = "Algorithm.FAG_ECFinder";
    	System.out.println("In "+callerID);
    	params=getParams();

    	currentNetwork=inputNetwork;
    	calNodeInfos(inputNetwork);
        calEdgeWeight(inputNetwork);
        if (curEdgeWeights == null || curNodeInfos==null) {
            System.err.println("In " + callerID + ": nodeInfos Map or edgeWeights Map was null.");
            return (null);
        }
        long msTimeBefore = System.currentTimeMillis();
        int findingProgress = 0;
        int findingTotal = 0;
        Collection values = curEdgeWeights.values(); //returns a Collection sorted by key order (descending)
        for (Iterator i1 = values.iterator(); i1.hasNext();) {
            ArrayList value = (ArrayList) i1.next();
            for(Iterator i2 = value.iterator(); i2.hasNext();) {
                i2.next();
                findingTotal++;
            }
        }
        //stores the list of clusters as ArrayLists of node indices in the input Network
        ArrayList alOriginalClusters = new ArrayList(inputNetwork.getNodeCount());
        /************************First, we sort each single node into a clique*************************/
        int i=0;
        Iterator nodes = inputNetwork.nodesIterator();
        while(nodes.hasNext()){
        	Node n=(Node) nodes.next();
    		int degree=inputNetwork.getDegree(n);
    		Cluster newCluster = new Cluster(i);
    		ArrayList alNodes=new ArrayList();
    		alNodes.add(new Integer(n.getRootGraphIndex()));
    		newCluster.setALNodes(alNodes);
    		newCluster.setTotalDegree(degree);
    		Integer nodeIndex=new Integer(n.getRootGraphIndex());
    		((NodeInfo)curNodeInfos.get(nodeIndex)).setComplex(i);
    		i++;
    		alOriginalClusters.add(newCluster);
        }
        /**********************************************************************************************
			Then, Operation UNION:	according to different situation, in which the two nodes consisting 
				this arc may belong to different Complexes or an identical Complex and that the 
				attributes of the Complexes varies, we take on different action 
         ***********************************************************************************************/
        ArrayList alEdgeWithSameWeight;  
        Edge curEdge;                                                                                                                          
        for (Iterator iterator = values.iterator(); iterator.hasNext();) {
            //each weight may be associated with multiple edges, iterate over these lists
        	alEdgeWithSameWeight = (ArrayList) iterator.next();
            for (int j = 0; j < alEdgeWithSameWeight.size(); j++) {//for each edge
                int edgeIndex = ((Integer) alEdgeWithSameWeight.get(j)).intValue();
                curEdge=inputNetwork.getEdge(edgeIndex);                
        		int inFrom = curEdge.getSource().getRootGraphIndex();
        		int inTo   = curEdge.getTarget().getRootGraphIndex();
        		NodeInfo fromNI=(NodeInfo)curNodeInfos.get(new Integer(inFrom));	//source node info
        		NodeInfo toNI=(NodeInfo)curNodeInfos.get(new Integer(inTo));	//target node info
        		
        		int icFrom=fromNI.iComplex;	//complex that the source node belongs to
        		int icTo=toNI.iComplex;		//complex that the target node belongs to 
        		if(icFrom != icTo)    //we have take some actions only if the two complexes are not the same
        		{
        			Cluster cFrom=(Cluster)alOriginalClusters.get(icFrom);
        			Cluster cTo=(Cluster)alOriginalClusters.get(icTo);
        			if(cFrom.isMergeable() && cTo.isMergeable())	//the two complexes are both mergeable
        				if(!cFrom.isModule() || !cTo.isModule())	//either of the two complexes are not modules yet
        					if(cFrom.getALNodes().size() >= cTo.getALNodes().size()){//merge the smaller complexe to the larger one
        						if(params.isWeak()) mergeComplexes1(cFrom, cTo);
        						else	mergeComplexes2(cFrom, cTo);   
        					}
        					else{	//merge the smaller complex to the larger one
        						if(params.isWeak())	mergeComplexes1(cTo, cFrom);
        						else	mergeComplexes2(cFrom, cTo);
        					}
        				else	//both of the two complexes are modules
        				{
        					cFrom.setMergeable(false);
        					cTo.setMergeable(false);
        				}
        			else	//either of the two complexes is not mergeable
        			{
        				cFrom.setMergeable(false);
    					cTo.setMergeable(false);
        			}
        		}
                if (taskMonitor != null) {
                    findingProgress++;
                    //We want to be sure that only progress changes are reported and not
                    //miniscule decimal increments so that the taskMonitor isn't overwhelmed
                    int newProgress = (findingProgress * 100) / findingTotal;
                    int oldProgress = ((findingProgress-1) * 100) / findingTotal;
                    if (newProgress != oldProgress) {
                        taskMonitor.setPercentCompleted(newProgress);
                    }
                }
                if (cancelled) {
                    break;
                }
            }
        }
        ArrayList alClusters = new ArrayList();
        Iterator it=alOriginalClusters.iterator();
        while(it.hasNext()){
        	Cluster cluster=(Cluster)it.next();
        	if(cluster.getALNodes().size()>=params.getComplexSizeThreshold()){
        		ArrayList alNodes=cluster.getALNodes();
        		GraphPerspective gpCluster =Algorithm.createGraphPerspective(alNodes, inputNetwork);
        		//cluster.setComplexID(counter++);
        		cluster.setGPCluster(gpCluster);
        		cluster.setClusterScore(0.0);
        		cluster.setSeedNode((Integer)alNodes.get(0));
        		cluster.setResultTitle(resultTitle);
        		int ind=cluster.getInDegree();
        		int outd=cluster.getTotalDegree()-2*ind;
        		if(ind!=0 && outd!=0)
        			cluster.setModularity((double)ind/(double)outd);
        		else
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


}
