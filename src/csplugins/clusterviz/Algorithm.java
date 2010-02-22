package csplugins.clusterviz;

import cytoscape.CyNetwork;
import cytoscape.task.TaskMonitor;
import giny.model.GraphPerspective;
import giny.model.Node;
import giny.model.Edge;

import java.util.*;

/**
 * An implementation of the algorithm
 */
public class Algorithm {
    private boolean cancelled = false;//If set, will schedule the canceled algorithm  at the next convenient opportunity
    private TaskMonitor taskMonitor = null;
    private ParameterSet params;   //the parameters used for this instance of the algorithm
    private CyNetwork currentNetwork;
    //states
    private long lastScoreTime;	//the time taken by the last score operation
    private long lastFindTime;	//the time taken by the last find operation
    private long findCliquesTime=0;//time used to find maximal cliques
    
	//data structures useful to storing information for more than one cluster finding iteration
    private HashMap curNodeInfos = null;    //vector<Node> key is the node index, value is a NodeInfo instance    
    private TreeMap curNodeScores = null; //key is node score, value is nodeIndex
    private TreeMap curEdgeWeights=null;	//key is edge index,value is EdgeInfo instance
    private HashMap curCliques=null;		//key is clique ID,value is Clique instance
    private ArrayList curOptimalDivision=null;
    
    private HashMap nodeInfoResultsMap = new HashMap(); //key is result, value is nodeInfroHashMap
    private HashMap nodeScoreResultsMap = new HashMap();//key is result, value is nodeScoreSortedMap

    private HashMap maximalCliquesNetworkMap=new HashMap();	//key is networkID, value is maximal Cliques
    private HashMap edgeWeightNetworkMap=new HashMap();
    private HashMap optimalDivisionKeyMap=new HashMap();
    
    //data structure for storing information required for each node
    private class NodeInfo {
        double density;         //neighborhood density
        int[] nodeNeighbors;    //stores node indices of all neighbors
        int numNodeNeighbors;	//the number of neighbors
        int coreLevel;          //e.g. 2 = a 2-core
        double coreDensity;     //density of the core neighborhood
        double score;           //node score
        int iComplex;
        ArrayList alComplex=new ArrayList();
        public NodeInfo() {
            this.density = 0.0;
            this.coreLevel = 0;
            this.coreDensity = 0.0;
    		this.iComplex=-1;
    		if(!alComplex.isEmpty())
    			alComplex.clear();
        }
        public void setComplex(int index){
        	iComplex=index;
        }
		public ArrayList getAlComplex() {
			return alComplex;
		}
		public void setAlComplex(ArrayList alComplex) {
			this.alComplex = alComplex;
 		}
    }
     
    /**
     * The constructor.
     *
     * @param networkID the algorithm use it to get the parameters of the focused network
     */
    public Algorithm(String networkID) {
        params = ParameterSet.getInstance().getParamsCopy(networkID);
    }
    //This method is used in AnalyzeTask
    public void setTaskMonitor(TaskMonitor taskMonitor, String networkID) {
        params = ParameterSet.getInstance().getParamsCopy(networkID);
        this.taskMonitor = taskMonitor;
    }
    public long getLastScoreTime() {
        return lastScoreTime;
    }
    public long getLastFindTime() {
        return lastFindTime;
    }
    public ParameterSet getParams() {
        return params;
    }
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }    
    public long getFindCliquesTIme() {
		return findCliquesTime;
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
     * @param inputnetwork The input network
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
     * Bron-Kerbosch Algorithm for finding all the maximal cliques
     * 
     * @param cur stands for the currently growing clique
     * @param cand the candidate nodes to be added, they are common neighbors of nodes in cur
     * @param not nodes already processed, so there is no common elements between cand and not
     */
    private void expand(HashMap cliques,Vector curr, Vector cand, 
    		Vector not){
    	if(cand.isEmpty() && not.isEmpty()){//the expanding process has come to an end
    		int num=cliques.size();
    		Clique pc=new Clique(num);	//the node in cur can form a new maximal clique
    		ArrayList alNodes=new ArrayList();
    		Iterator it=curr.iterator();
    		while(it.hasNext()){
    			Integer node=(Integer)it.next();
    			alNodes.add(node);
    		}
    		pc.setCliqueNodes(alNodes);
    		cliques.put(new Integer(pc.getCliuqueID()), pc);	//add to the maximal clique list
    	}	
    	else
    	{
    		int p,i;
    		Integer q;
    		Vector candq;
    		Vector notq;
    		Vector cutcand=new Vector((Vector)cand.clone());
    		p=getPivot(cand,not);	//get the index of the pivot node 
    		ArrayList cuts=getNeighbors(p);
    		for(Iterator it=cuts.iterator();it.hasNext();)//get the trimmed candidates
    			cutcand.remove((Integer)it.next());
    		for(i=0;i<cutcand.size();++i)//for each non-adjacent node
    		{
    			q=(Integer)cutcand.get(i);
    			cand.remove(q);	//remove from candidate list 
    			curr.add(q);	//add the expanded node
    			ArrayList adjs=getNeighbors(q.intValue());//1.2 get the adjacent
    			candq=getIntersect( cand,adjs );//2.1 get insertion
    			notq=getIntersect(not,adjs);
    			expand(cliques,curr,candq,notq);//2.3 recursive process
    			curr.remove(curr.lastElement());//pop the top element a new cursive process
                if (cancelled) {
                    break;
                }
    		}
    		//TODO: here we need to set the monitor progress
    		/*
            if (taskMonitor != null) {
                i++;
                taskMonitor.setPercentCompleted((i * 100) / inputNetwork.getNodeCount());
            }*/
    	}
    }
    /**
     * Choose a vertex from cand&not with the largest number of connections to the vertices in cand
     */
    private int getPivot(Vector cand, Vector not){
    	int ret=-1,most=0,nodeIndex;
    	int intNum,i;
    	ArrayList neighbors;
    	//TODO: here 
    	//if(cand.size()==1)	//if there is only one node in cand, then we simply choose it
    		//return ((Integer)cand.get(0)).intValue();
    	for(i=0;i<cand.size();++i){
    		nodeIndex=((Integer)(cand.get(i))).intValue();
    		neighbors=getNeighbors(nodeIndex);
    		intNum=0;
    		for(Iterator it=neighbors.iterator();it.hasNext();){
    			//get the number of intersection between cand and neighbors[i];
    			Integer e = (Integer)it.next();
    			if(cand.contains(e))
    				intNum++;
    		}
    		if(intNum>=most){
    			most=intNum;
    			ret=nodeIndex;
    		}
    	}
    	for(i=0;i<not.size();++i){
    		nodeIndex=((Integer)not.get(i)).intValue();
    		neighbors=getNeighbors(nodeIndex);
    		intNum=0;
    		for(Iterator it=neighbors.iterator();it.hasNext();){
    			Integer e=(Integer)it.next();
    			if(cand.contains(e))
    				intNum++;
    		}
    		if(intNum>=most){
    			most=intNum;
    			ret=nodeIndex;
    		}
    	}
    	return ret;
    }
    private ArrayList getNeighbors(int nodeIndex){
    	ArrayList ret=new ArrayList();
    	int[] o=currentNetwork.neighborsArray(nodeIndex);
    	for(int i=0;i<o.length;++i){
    		if(o[i]!=nodeIndex)
    			ret.add(new Integer(o[i]));
    	}
    	return ret;
    }
    private int[] getNeighborArray(CyNetwork network, int nodeIndex){
    	ArrayList ret=new ArrayList();
    	int[] o=network.neighborsArray(nodeIndex);
    	for(int i=0;i<o.length;++i){
    		if(o[i]!=nodeIndex)
    			ret.add(new Integer(o[i]));
    	}
    	int[] neighbors=ClusterUtil.convertIntArrayList2array(ret);
    	return neighbors;
    }
    private Vector getIntersect(Vector a,ArrayList b){
    	Vector r=new Vector();
    	for(Iterator i=a.iterator();i.hasNext();){
    		Integer n=(Integer)i.next();
    		if(b.contains(n))
    			r.add(n);
    	}
    	return r;
    }
    /**
     * expandedFAC-EC Algorithm for finding complexes
     * @param inputnetwork The input network
     * @param resultTitle Title of the result, used as an identifier in various hash maps
     * @return
     */
    public Cluster[] FAG_ECXFinder(CyNetwork inputNetwork,String resultTitle){        
    	String callerID = "Algorithm.ExpandedFAC_ECFinder";
    	System.out.println("In "+callerID);
    	params=getParams();
    	currentNetwork=inputNetwork;
    	calNodeInfos(inputNetwork);
    	calEdgeWeight(inputNetwork);
        if (curEdgeWeights == null || curNodeInfos==null) {
            System.err.println("In " + callerID + ": nodeInfos Map or edgeWeights Map was null.");
            return (null);
        }
    	curCliques=(HashMap)maximalCliquesNetworkMap.get(inputNetwork.getIdentifier());
        if (curCliques == null) {
            System.err.println("In " + callerID + ": maximal cliques Map was null.");
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
   /****************First, we sort each maximal clique into a complex**************/
        ArrayList rawClusters=new ArrayList();
        int counter=0,totalDegree,inDegree;
        for(int i=0;i<curCliques.size();i++){	//for each clique
            Clique cur=(Clique)curCliques.get(new Integer(i));
            ArrayList nodes=cur.getCliqueNodes();
            if(nodes.size() >= params.getCliqueSizeThreshold()){
            	totalDegree=0;
                Cluster newCluster=new Cluster(counter);
            	ArrayList alNodes=new ArrayList();
            	Iterator it=nodes.iterator();
            	while(it.hasNext()){
            		Integer n=(Integer)it.next();
            		((NodeInfo)curNodeInfos.get(n)).getAlComplex().add(new Integer(counter));
            		alNodes.add(n);
            		totalDegree+=inputNetwork.getDegree(n.intValue());//will there be something wrong?
            	}
            	newCluster.setALNodes(alNodes);
            	newCluster.setTotalDegree(totalDegree);
            	inDegree=alNodes.size()*(alNodes.size()-1);
            	inDegree=inDegree/2;
            	newCluster.setInDegree(inDegree);
            	int out=totalDegree-2*inDegree;
    			double fModule = (double)inDegree/(double)out;
    			if(fModule>params.getFThreshold())
    				newCluster.setModule(true);
            	rawClusters.add(newCluster); 
            	counter++;
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
            		if(searchInComplexes(rawClusters,n)==0){	//a subordinate node
            			Cluster newCluster=new Cluster(counter);
            			ArrayList alNodes=new ArrayList();
            			alNodes.add(n);
            			newCluster.setALNodes(alNodes);
            			totalDegree=inputNetwork.getDegree(n.intValue());
            			newCluster.setTotalDegree(totalDegree);
                		((NodeInfo)curNodeInfos.get(n)).getAlComplex().add(new Integer(counter++));
                    	rawClusters.add(newCluster); 
            		}
        		}
        	}
        }//completed initialization
    /**********************************************************************************************
		Then, Operation UNION:	according to different situation, in which the two nodes consisting 
			this arc may belong to different Complexes or an identical Complex and that the 
			attributes of the Complexes varies, we take on different action 
     ***********************************************************************************************/
        ArrayList alEdgeWithSameWeight;  
        Edge curEdge;                                                                                                                          
        for (Iterator iterator = values.iterator(); iterator.hasNext();) {//for each weight values
            //each weight may be associated with multiple edges, iterate over these lists
        	alEdgeWithSameWeight = (ArrayList) iterator.next();
            for (int j = 0; j < alEdgeWithSameWeight.size(); j++) {//for each edge
                int edgeIndex = ((Integer) alEdgeWithSameWeight.get(j)).intValue();
                curEdge=inputNetwork.getEdge(edgeIndex); 
        		int inFrom = curEdge.getSource().getRootGraphIndex();
        		int inTo   = curEdge.getTarget().getRootGraphIndex();
        		NodeInfo fromNI=(NodeInfo)curNodeInfos.get(new Integer(inFrom));	//source node info
        		NodeInfo toNI=(NodeInfo)curNodeInfos.get(new Integer(inTo));	//target node info
        		
        		ArrayList subFrom=fromNI.getAlComplex();	//complexes including the source node
        		ArrayList subTo=toNI.getAlComplex();		//complexes including the target node
        		if(different(subFrom,subTo))    //we need to take action only if the two sets of complexes is not the same
        		{
        			Cluster c1,c2,c;
        			//merge all possible complexes
    				c1=(Cluster)rawClusters.get( ((Integer)subFrom.get(0)).intValue() );
    				if(subFrom.size()>1){	//merge the first set
    	    			while(true){
    	    				int other=subFrom.indexOf(new Integer(c1.getComplexID()))+1;
    	    				if(cancelled||(other>=subFrom.size()))	
    	    					break;
    	    				c=(Cluster)rawClusters.get(((Integer)subFrom.get(other)).intValue());
	    					if(!c1.isModule() && !c.isModule()){
	    						if(params.isWeak())
	    							mergeComplexes3(c1,c);
	    						else
	    							mergeComplexes4(c1,c);
	    					}else{
    							c1.setMergeable(false);
    							c1=c;
	    					}
    	    			}
    				}
    				c2=(Cluster)rawClusters.get( ((Integer)subTo.get(0)).intValue() );	
    				if(subTo.size()>1 && different(subFrom,subTo)){	//merge the other set
    	    			while(true){
    	    				int other=subTo.indexOf(new Integer(c2.getComplexID()))+1;
    	    				if(cancelled||(other>=subTo.size()))	
    	    					break;
    	    				c=(Cluster)rawClusters.get(((Integer)subTo.get(other)).intValue());
	    					if(!c2.isModule() && !c.isModule())
	    						if(params.isWeak())
	    							mergeComplexes3(c2,c);
	    						else
	    							mergeComplexes4(c2,c);
	    					else{
		    					c2.setMergeable(false);
		    					c2=c;
		    				}    	    				
    	    			}
    				}
    				if(c1.getComplexID()!=c2.getComplexID()){	//merge the two sets
    					if(!c1.isModule() && !c2.isModule())
    						if(params.isWeak())
    							mergeComplexes3(c1,c2);
    						else
    							mergeComplexes4(c1,c2);
    					else
    						c1.setMergeable(false);
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
            }//for each edge of this weight
        }//for each weight value
        ArrayList alClusters=new ArrayList();
        Iterator it1=rawClusters.iterator();
        while(it1.hasNext()){
        	Cluster cluster=(Cluster)it1.next();
        	if(cluster.getALNodes().size()>=params.getComplexSizeThreshold()){
        		ArrayList alNodes=cluster.getALNodes();
        		GraphPerspective gpCluster =Algorithm.createGraphPerspective(alNodes, inputNetwork);
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
    private boolean different(ArrayList a, ArrayList b){
    	if(a.size()!=b.size()) return true;
    	Iterator it=a.iterator();
    	while(it.hasNext()){
    		Integer e=(Integer)it.next();
    		if(!b.contains(e))
    			return true;
    	}
    	return false;
    }
    /**
     * EAGLE Algorithm for finding complexes
     * @param inputnetwork The input network
     * @param resultTitle Title of the result, used as an identifier in various hash maps
     * @return
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
    /**
     * calculate the similarities between a pairs of complexes
     *   S=1/2m*( sumof(Aij-ki*kj/2m) ) of which i IEO C1,j IEO C2, and i!=j
     */
    private double calSimilarity(Cluster c1,Cluster c2){
    	double S=0,temp;
    	int A,degree1,degree2;
    	int[] neigh;
    	int m=currentNetwork.getEdgeCount();
    	ArrayList nodes1=c1.getALNodes();
    	ArrayList nodes2=c2.getALNodes();
    	for(Iterator it1=nodes1.iterator();it1.hasNext();){
    		Integer n1=(Integer)it1.next();
    		neigh=getNeighborArray( currentNetwork,(n1.intValue()) );
    		degree1=neigh.length;
    		Arrays.sort(neigh);
    		for(Iterator it2=nodes2.iterator();it2.hasNext();){
    			Integer n2=(Integer)it2.next();
    			if(n1.intValue()!=n2.intValue()){//n1 and n2 is not the same node
        			degree2=currentNetwork.getDegree(n2.intValue());
        			A=( Arrays.binarySearch(neigh, n2.intValue() )<0 )? 0:1;
        			S+=A;
        			temp=degree1*degree2;        			
        			S-=temp/2/m;
    			}
    		}
    	}
    	//S=S/2/m;
		return S;
    }
    /**
     * evaluate the quality of the resulting complexes,by which we decide which division is optimal
     *    EQ=1/2m*(sumof( (Aij-ki*kj/2m)/(Oi*Oj) ))	of which i,j IEO Cx
     * @param alClusters all the clusters at present
     */
    private double calModularity(ArrayList alClusters){
    	double M=0.0,temp;
    	int A,i,j,n1,n2;
    	int d1,d2,c1,c2;
    	int[] neighs;
    	int m=currentNetwork.getEdgeCount();
    	Iterator i0=alClusters.iterator();
    	while(i0.hasNext()){//for each Complex
    		Cluster cur=(Cluster)i0.next();
    		ArrayList alNodes=cur.getALNodes();
    		int[] nodes=ClusterUtil.convertIntArrayList2array(alNodes);
    		for(i=0;i<nodes.length-1;i++){//for each pairs of nodes
				n1=nodes[i];
				neighs=getNeighborArray(currentNetwork,n1);
				Arrays.sort(neighs);
				d1=neighs.length;	
				c1=searchInComplexes(alClusters,new Integer(n1));
    			for(j=0;j<nodes.length;j++){
    				n2=nodes[j];
        			d2=currentNetwork.getDegree(n2);
        			c2=searchInComplexes(alClusters,new Integer(n2));
        			A=( Arrays.binarySearch(neighs, n2 )<0 )? 0:1;
        			temp=(d1*d2);
        			temp=temp/2/m;
        			temp=-temp;
        			temp+=A;
        			M+=temp/c1/c2;    				
    			}
    		}
    	}
    	//M=M/2/m;
    	return M;    	
    }
    private int searchInComplexes(ArrayList alClusters, Integer node){
    	int counter=0;
    	Iterator it=alClusters.iterator();
    	while(it.hasNext()){
    		Cluster cur=(Cluster) it.next();
    		ArrayList alNodes=cur.getALNodes();
    		if(alNodes.contains(node))
    			counter++;
    	}
    	return counter;
    }
    /**
     * K-Core Algorithm Step 1: 
     * Score the graph and save scores as node attributes.  Scores are also
     * saved internally in your instance of Algorithm.
     *
     * @param inputNetwork The network that will be scored
     * @param resultTitle Title of the result, used as an identifier in various hash maps
     */
    public void scoreGraph(CyNetwork inputNetwork, String resultTitle) {
        params = getParams();
        String callerID = "Algorithm.scorGraph";
        if (inputNetwork == null) {
            System.err.println("In " + callerID + ": inputNetwork was null.");
            return;
        }
        
        long msTimeBefore = System.currentTimeMillis();
        HashMap nodeInfoHashMap = new HashMap(inputNetwork.getNodeCount());
        TreeMap nodeScoreSortedMap = new TreeMap(new Comparator() { //will store Doubles (score) as the key, Lists as values
            //sort Doubles in descending order
            public int compare(Object o1, Object o2) {
                double d1 = ((Double) o1).doubleValue();
                double d2 = ((Double) o2).doubleValue();
                if (d1 == d2) {
                    return 0;
                } else if (d1 < d2) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        //iterate over all nodes and calculate node score
        NodeInfo nodeInfo = null;
        double nodeScore;
        ArrayList al;
        int i = 0;
        Iterator nodes = inputNetwork.nodesIterator();
        while (nodes.hasNext() && (!cancelled)) {
            Node n = (Node) nodes.next();
            nodeInfo = calcNodeInfo(inputNetwork, n.getRootGraphIndex());
            nodeInfoHashMap.put(new Integer(n.getRootGraphIndex()), nodeInfo);
            //score node TODO: add support for other scoring functions (low priority)
            nodeScore = scoreNode(nodeInfo);
            if (nodeScoreSortedMap.containsKey(new Double(nodeScore))) {
                al = (ArrayList) nodeScoreSortedMap.get(new Double(nodeScore));
                al.add(new Integer(n.getRootGraphIndex()));
            } else {
                al = new ArrayList();
                al.add(new Integer(n.getRootGraphIndex()));
                nodeScoreSortedMap.put(new Double(nodeScore), al);
            }
            if (taskMonitor != null) {
                i++;
                taskMonitor.setPercentCompleted((i * 100) / inputNetwork.getNodeCount());
            }
        }
        nodeScoreResultsMap.put(resultTitle, nodeScoreSortedMap);
        nodeInfoResultsMap.put(resultTitle, nodeInfoHashMap);
        curNodeScores = nodeScoreSortedMap;
        curNodeInfos = nodeInfoHashMap;

        long msTimeAfter = System.currentTimeMillis();
        lastScoreTime = msTimeAfter - msTimeBefore;
    }
    /**
     * K-Core Algorithm Step 2: 
     * Find all complexes given a scored graph.  If the input network has not been scored,
     * this method will return null.  This method is called when the user selects network scope or
     * single node scope.
     *
     * @param inputNetwork The scored network to find clusters in.
     * @param resultTitle Title of the result
     * @return An array containing an Cluster object for each cluster.
     */
    public Cluster[] K_CoreFinder(CyNetwork inputNetwork, String resultTitle) {
        String callerID = "Algorithm.K_CliqueFinder";
    	System.out.println("In"+callerID);
        TreeMap nodeScoreSortedMap;
        HashMap nodeInfoHashMap;
        if (!nodeScoreResultsMap.containsKey(resultTitle)) {//use the node score used last time
            nodeScoreSortedMap = curNodeScores;
            nodeInfoHashMap = curNodeInfos;            
            nodeScoreResultsMap.put(resultTitle, nodeScoreSortedMap);
            nodeInfoResultsMap.put(resultTitle, nodeInfoHashMap);
        } else {//the scoring parameters haven't changed
            nodeScoreSortedMap = (TreeMap) nodeScoreResultsMap.get(resultTitle);
            nodeInfoHashMap = (HashMap) nodeInfoResultsMap.get(resultTitle);
        }
        params = getParams();
        Cluster currentCluster;
        if (inputNetwork == null) {
            System.err.println("In " + callerID + ": inputNetwork was null.");
            return (null);
        }
        if ((nodeInfoHashMap == null) || (nodeScoreSortedMap == null)) {
            System.err.println("In " + callerID + ": nodeInfoHashMap or nodeScoreSortedMap was null.");
            return (null);
        }

        //initialization
        long msTimeBefore = System.currentTimeMillis();
        HashMap nodeSeenHashMap = new HashMap(); //key is nodeIndex, value is true/false
        Integer currentNode;
        int findingProgress = 0;
        int findingTotal = 0;
        Collection values = nodeScoreSortedMap.values(); //returns a Collection sorted by key order (descending)
        for (Iterator iterator1 = values.iterator(); iterator1.hasNext();) {
            ArrayList value = (ArrayList) iterator1.next();
            for(Iterator iterator2 = value.iterator(); iterator2.hasNext();) {
                iterator2.next();
                findingTotal++;
            }
        }
        //stores the list of clusters as ArrayLists of node indices in the input Network
        ArrayList alClusters = new ArrayList();
        ArrayList alNodesWithSameScore;                                                                                                                            
        for (Iterator iterator = values.iterator(); iterator.hasNext();) {
            //each score may be associated with multiple nodes, iterate over these lists
            alNodesWithSameScore = (ArrayList) iterator.next();
            for (int j = 0; j < alNodesWithSameScore.size(); j++) {
                currentNode = (Integer) alNodesWithSameScore.get(j);
                if (!nodeSeenHashMap.containsKey(currentNode)) {
                    currentCluster = new Cluster();
                    currentCluster.setSeedNode(currentNode);//store the current node as the seed node
                    //we store the current node seen hash map for later exploration purposes
                    HashMap nodeSeenHashMapSnapShot = new HashMap((HashMap)nodeSeenHashMap.clone());
                    ArrayList alNodes = getClusterCore(currentNode, nodeSeenHashMap, params.getNodeScoreCutoff(), params.getMaxDepthFromStart(), nodeInfoHashMap);//here we use the original node score cutoff
                    if (alNodes.size() > 0) {
                        //make sure seed node is part of cluster, if not already in there
                        if (!alNodes.contains(currentNode)) {
                            alNodes.add(currentNode);
                        }
                        //create an input graph for the filter and haircut methods
                        GraphPerspective gpCluster = Algorithm.createGraphPerspective(alNodes, inputNetwork);
                        if (!filterCluster(gpCluster)) {//only do this when the cluster need not filter
                            if (params.isHaircut()) {
                                haircutCluster(gpCluster, alNodes, inputNetwork);
                            }
                            if (params.isFluff()) {
                                fluffClusterBoundary(alNodes, nodeSeenHashMap, nodeInfoHashMap);
                            }
                            currentCluster.setALNodes(alNodes);;
                            gpCluster = Algorithm.createGraphPerspective(alNodes, inputNetwork);
                            currentCluster.setGPCluster(gpCluster);
                            currentCluster.setClusterScore(scoreCluster(currentCluster));
//                          //store all the nodes that have already been seen and incorporated in other clusters
                            currentCluster.setNodeSeenHashMap(nodeSeenHashMapSnapShot);
                            currentCluster.setResultTitle(resultTitle);
                            //store detected cluster for later
                            alClusters.add(currentCluster);
                        }
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
        //Once the clusters have been found we either return them or in the case of selection scope,
        //we select only the ones that contain the selected node(s) and return those
        for(Iterator it=alClusters.iterator();it.hasNext();){
        	Cluster cluster=(Cluster)it.next();
    		cluster.calModularity(inputNetwork);
        }
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

    private void calNodeInfos(CyNetwork net){
		HashMap nodeInfos=new HashMap();
		Iterator nodes=net.nodesIterator();
		while(nodes.hasNext() && (!cancelled)){
			NodeInfo nodeInfo=new NodeInfo();
			int node=((Node)nodes.next()).getRootGraphIndex();
			ArrayList adjs=getNeighbors(node);
			nodeInfo.nodeNeighbors=ClusterUtil.convertIntArrayList2array(adjs);
			nodeInfos.put(new Integer(node), nodeInfo);
		}
		curNodeInfos=nodeInfos;
    }
    /**
     * Calculates node information for each node.
     * This information is used at the first stage of the K-Clique algorithm.
     * This is a utility function for the algorithm.
     *
     * @param inputNetwork The input network for reference
     * @param nodeIndex    The index of the node in the input network to score
     * @return A NodeInfo object containing node information required for the algorithm
     */
    private NodeInfo calcNodeInfo(CyNetwork inputNetwork, int nodeIndex) {
        int[] neighborhood;
        String callerID = "Algorithm.calcNodeInfo";
        if (inputNetwork == null) {
            System.err.println("In " + callerID + ": gpInputGraph was null.");
            return null;
        }

        //get neighborhood of this node (including the node)
        int[] neighbors = getNeighborArray(inputNetwork,nodeIndex);
        if (neighbors.length < 2) {
            //if there are no neighbors or just one neighbor
            NodeInfo nodeInfo = new NodeInfo();
            if (neighbors.length == 1) {//only one neighbor
                nodeInfo.coreLevel = 1;
                nodeInfo.coreDensity = 1.0;
                nodeInfo.density = 1.0;
                //nodeInfo.numNodeNeighbors=2;//????
                //nodeInfo.nodeNeighbors=????
            }//else it is a isolated node
            return (nodeInfo);
        }        
        //add original node to extract complete neighborhood
        Arrays.sort(neighbors);
        if (Arrays.binarySearch(neighbors, nodeIndex) < 0) {//add itself as its neighbor
            neighborhood = new int[neighbors.length + 1];
            System.arraycopy(neighbors, 0, neighborhood, 1, neighbors.length);
            neighborhood[0] = nodeIndex;
        } else {
            neighborhood = neighbors;
        }
        //extract neighborhood subgraph
        GraphPerspective gpNodeNeighborhood = inputNetwork.createGraphPerspective(neighborhood);
        if (gpNodeNeighborhood == null) {//this shouldn't happen
            System.err.println("In " + callerID + ": gpNodeNeighborhood was null.");
            return null;
        }
        //calculate the node information for each node
        NodeInfo nodeInfo = new NodeInfo();
        //density
        if (gpNodeNeighborhood != null) {
            nodeInfo.density = calcDensity(gpNodeNeighborhood, params.isIncludeLoops());
        }
        nodeInfo.numNodeNeighbors = neighborhood.length;
        //calculate the highest k-core
        GraphPerspective gpCore = null;
        Integer k = null;
        Object[] returnArray = getHighestKCore(gpNodeNeighborhood);
        k = (Integer) returnArray[0];
        gpCore = (GraphPerspective) returnArray[1];
        nodeInfo.coreLevel = k.intValue();
        //calculate the core density - amplifies the density of heavily interconnected regions and attenuates
        //that of less connected regions
        if (gpCore != null) {
            nodeInfo.coreDensity = calcDensity(gpCore, params.isIncludeLoops());
        }
        //record neighbor array for later use in cluster detection step
        nodeInfo.nodeNeighbors = neighborhood;
        return (nodeInfo);
    }
    /**
     * merge two unoverlapped complexes ,weak definition
     */
    private void mergeComplexes1(Cluster c1,Cluster c2){
    	int inDegree=c1.getInDegree();
    	int totalDegree=c1.getTotalDegree()+c2.getTotalDegree();
    	
    	ArrayList alNodes=c1.getALNodes();
    	Iterator i=c2.getALNodes().iterator();
    	while(i.hasNext()){
    		int nodeIndex=((Integer)i.next()).intValue();
    		int[] adjs=getNeighborArray(currentNetwork,nodeIndex);
    		for(int j=0;j<adjs.length;j++)
    			if(alNodes.contains(new Integer(adjs[j])))
    					inDegree++;
    		alNodes.add(new Integer(nodeIndex));
    		NodeInfo node=(NodeInfo)curNodeInfos.get(new Integer(nodeIndex));
    		node.setComplex(c1.getComplexID());
    	}
    	c1.setInDegree(inDegree);
    	c1.setTotalDegree(totalDegree);
    	int outDegree=totalDegree-2*inDegree;
    	if(outDegree<0)
    		System.err.println("Error outDegree!");
    	float fModule = (float)inDegree/(float)(outDegree);
    	if( fModule>params.getFThreshold() )
    		c1.setModule(true);
    	c2.getALNodes().clear();
    }
    /**
     * merge two unoverlapped complexes ,strong definition
     */
    private void mergeComplexes2(Cluster c1,Cluster c2){
    	ArrayList alNodes=c1.getALNodes();
    	Iterator i=c2.getALNodes().iterator();
    	while(i.hasNext()){
    		int nodeIndex=((Integer)i.next()).intValue();
    		NodeInfo node=(NodeInfo)curNodeInfos.get(new Integer(nodeIndex));
    		node.setComplex(c1.getComplexID());
    		alNodes.add(new Integer(nodeIndex));
    	}
    	c2.getALNodes().clear();
    	i=alNodes.iterator();
    	c1.setModule(true);
    	int nodeInDegree,nodeTotalDegree;
    	while(i.hasNext()){
    		int nodeIndex=((Integer)i.next()).intValue();
    		int[] adjs=getNeighborArray(currentNetwork,nodeIndex);
    		nodeInDegree=0;
    		for(int j=0;j<adjs.length;j++)
    			if(alNodes.contains(new Integer(adjs[j])))
    					nodeInDegree++;
    		nodeTotalDegree=currentNetwork.getDegree(nodeIndex);
        	double fModule = (double)nodeInDegree/(double)(nodeTotalDegree);
    		if(fModule<0.5)
    			c1.setModule(false);
    	}
    }
    /**
     * merge overlapped complexes with weak module definition
     */
    private void mergeComplexes3(Cluster c1, Cluster c2){
    	ArrayList nodes1=c1.getALNodes();
    	ArrayList nodes2=c2.getALNodes();
    	//add the unoverlapped nodes and set the subComplexes of all nodes in C2
    	NodeInfo nodeNI;
    	ArrayList subComplexes;
    	for(Iterator it=nodes2.iterator();it.hasNext();){//for each node in C2
    		Integer node=(Integer)it.next();
    		nodeNI=(NodeInfo)curNodeInfos.get(node);
    		subComplexes=nodeNI.getAlComplex();
    		if(!nodes1.contains(node)){//this is not a overlapped node
        		int index=subComplexes.indexOf(new Integer(c2.getComplexID()));
        		subComplexes.remove(index);
        		subComplexes.add(new Integer(c1.getComplexID()));
    			nodes1.add(node);
    		}
    		else{	//this node already exists in C1
        		int index=subComplexes.indexOf(new Integer(c2.getComplexID()));
        		subComplexes.remove(index);
    		}
    	}
    	//calculate the other informations for C1
    	int inDegree=0;
    	int totalDegree=0;
    	for(Iterator it=nodes1.iterator();it.hasNext();){//for each node in merged C1
    		int node=((Integer)it.next()).intValue();
    		totalDegree+=currentNetwork.getDegree(node);//can this be useful?
    		int[] neighbors=getNeighborArray(currentNetwork,node);
    		for(int i=0;i<neighbors.length;i++)
    			if(nodes1.contains(new Integer(neighbors[i])))
    				inDegree++;
    	}
    	int outDegree=totalDegree-inDegree;
    	inDegree=inDegree/2;
    	c1.setInDegree(inDegree);
    	c1.setTotalDegree(totalDegree);
    	double fModule = (double)inDegree/(double)outDegree;
    	if(fModule>params.getFThreshold())
    		c1.setModule(true);
    	//clear the content of nodes2
    	nodes2.clear();
    }
    /**
     * merge overlapped complexes using strong module definition
     */
    private void mergeComplexes4(Cluster c1, Cluster c2){
    	ArrayList nodes1=c1.getALNodes();
    	ArrayList nodes2=c2.getALNodes();
    	//add the unoverlapped nodes and set the subComplexes of all nodes in C2
    	NodeInfo nodeNI;
    	ArrayList subComplexes;
    	for(Iterator it=nodes2.iterator();it.hasNext();){//for each node in C2
    		Integer node=(Integer)it.next();
    		nodeNI=(NodeInfo)curNodeInfos.get(node);
    		subComplexes=nodeNI.getAlComplex();
    		if(!nodes1.contains(node)){//this is not a overlapped node
        		int index=subComplexes.indexOf(new Integer(c2.getComplexID()));
        		subComplexes.remove(index);
        		subComplexes.add(new Integer(c1.getComplexID()));
    			nodes1.add(node);
    		}
    		else{	//this node already exists in C1
        		int index=subComplexes.indexOf(new Integer(c2.getComplexID()));
        		subComplexes.remove(index);
    		}
    	}
    	c1.setModule(true);
    	int nodeInDegree,nodeTotalDegree;
    	for(Iterator i=nodes1.iterator();i.hasNext();){
    		int nodeIndex=((Integer)i.next()).intValue();
    		int[] adjs=getNeighborArray(currentNetwork,nodeIndex);
    		nodeInDegree=0;
    		for(int j=0;j<adjs.length;j++)
    			if(nodes1.contains(new Integer(adjs[j])))
    					nodeInDegree++;
    		nodeTotalDegree=currentNetwork.getDegree(nodeIndex);
        	float fModule = (float)nodeInDegree/(float)(nodeTotalDegree);
    		if(fModule<0.5){
    			c1.setModule(false);
    		}
    	}
    	//clear the content of nodes2
    	nodes2.clear();
    }
    /**
     * Score node using the formula from original paper.
     * This formula selects for larger, denser cores.
     * This is a utility function for the algorithm.
     *
     * @param nodeInfo The internal data structure to fill with node information
     * @return The score of this node.
     */
    private double scoreNode(NodeInfo nodeInfo) {
        if (nodeInfo.numNodeNeighbors > params.getDegreeThreshold()) {
            nodeInfo.score = nodeInfo.coreDensity * (double) nodeInfo.coreLevel;
        } else {
            nodeInfo.score = 0.0;
        }
        return (nodeInfo.score);
    }
    /**
     * Gets the calculated node score of a node from a given result.  Used in ResultsPanel
     * during the attribute setting method.
     *
     * @param rootGraphIndex Integer which is used to identify the nodes in the score-sorted tree map
     * @param resultTitle Title of the results for which we are retrieving a node score
     * @return node score as a Double
     */
    public Double getNodeScore(int rootGraphIndex, String resultTitle) {
        Double nodeScore = new Double(0.0);
        TreeMap nodeScoreSortedMap = (TreeMap) nodeScoreResultsMap.get(resultTitle);        
        for (Iterator score = nodeScoreSortedMap.keySet().iterator(); score.hasNext();) {
        	nodeScore = (Double) score.next();
            ArrayList nodes = (ArrayList) nodeScoreSortedMap.get(nodeScore);
            if (nodes.contains(new Integer(rootGraphIndex))) {
                return nodeScore;
            }
        }
        return nodeScore;
    }
    /**
     * Gets the highest node score in a given result.  Used in the VisualStyleAction class to
     * re-initialize the visual calculators.
     */
    public double getMaxScore(String resultTitle) {
        TreeMap nodeScoreSortedMap = (TreeMap) nodeScoreResultsMap.get(resultTitle);
        Double nodeScore = (Double) nodeScoreSortedMap.firstKey();
        return nodeScore.doubleValue();
    }
    /**
     * create graphPerspective for nodes in a cluster
     * @param alNode the nodes
     * @param inputNetwork the original network
     * @return the graph perspective created
     */
    public static GraphPerspective createGraphPerspective(ArrayList alNode, CyNetwork inputNetwork) {
        //convert Integer array to int array
        int[] clusterArray = new int[alNode.size()];
        for (int i = 0; i < alNode.size(); i++) {
            int nodeIndex = ((Integer) alNode.get(i)).intValue();
            clusterArray[i] = nodeIndex;
        }
        GraphPerspective gpCluster = inputNetwork.createGraphPerspective(clusterArray);
        return gpCluster;
    }
    /**
     * Score a cluster.  Currently this ranks larger, denser clusters higher, although
     * in the future other scoring functions could be created
     *
     * @param cluster - The GINY GraphPerspective version of the cluster
     * @return The score of the cluster
     */
    public double scoreCluster(Cluster cluster) {
        int numNodes = 0;
        double density = 0.0, score = 0.0;
        numNodes = cluster.getGPCluster().getNodeCount();
        density = calcDensity(cluster.getGPCluster(), true);
        score = density * numNodes;
        return (score);
    }
    /**
     * Find the high-scoring central region of the cluster.
     * This is a utility function for the algorithm.
     *
     * @param startNode       The node that is the seed of the cluster
     * @param nodeSeenHashMap The list of nodes seen already
     * @param nodeScoreCutoff Slider input used for cluster exploration
     * @param maxDepthFromStart Limits the number of recursions
     * @param nodeInfoHashMap Provides the node scores
     * @return A list of node IDs representing the core of the cluster
     */
    private ArrayList getClusterCore(Integer seedNode, HashMap nodeSeenHashMap, double nodeScoreCutoff, int maxDepthFromStart, HashMap nodeInfoHashMap) {
        ArrayList cluster = new ArrayList(); //stores Integer nodeIndices
        getClusterCoreInternal(seedNode,((NodeInfo) nodeInfoHashMap.get(seedNode)).score, 
        		nodeSeenHashMap,1, cluster, nodeScoreCutoff, maxDepthFromStart, nodeInfoHashMap);
        return (cluster);
    }

    /**
     * An internal function that does the real work of getClusterCore, implemented to enable recursion.
     *
     * @param startNode         The node that is the seed of the cluster
     * @param nodeSeenHashMap   The list of nodes seen already
     * @param startNodeScore    The score of the seed node
     * @param currentDepth      The depth away from the seed node that we are currently at
     * @param cluster           The cluster to add to if we find a cluster node in this method
     * @param nodeScoreCutoff   Helps determine if the nodes being added are within the given threshold
     * @param maxDepthFromStart Limits the recursion
     * @param nodeInfoHashMap   Provides score info
     * @return true
     */
    private boolean getClusterCoreInternal(Integer startNode, double startNodeScore, 
    		HashMap nodeSeenHashMap, int currentDepth, ArrayList cluster, 
    		double nodeScoreCutoff, int maxDepthFromStart,  HashMap nodeInfoHashMap){
        //base cases for recursion
        if (nodeSeenHashMap.containsKey(startNode)) {
            return (true);  //don't recheck a node
        }
        nodeSeenHashMap.put(startNode, new Boolean(true));
        if (currentDepth > maxDepthFromStart) {
            return (true);  //don't exceed given depth from start node
        }
        //Initialization
        Integer currentNeighbor;
        int i = 0;
        for (i = 0; i < (((NodeInfo) nodeInfoHashMap.get(startNode)).numNodeNeighbors); i++) {
            //go through all currentNode neighbors to check their core density for cluster inclusion
            currentNeighbor = new Integer(((NodeInfo) nodeInfoHashMap.get(startNode)).nodeNeighbors[i]);
            if ((!nodeSeenHashMap.containsKey(currentNeighbor)) &&
                    (((NodeInfo) nodeInfoHashMap.get(currentNeighbor)).score >=
                    (startNodeScore - startNodeScore * nodeScoreCutoff))) {
                //add current neighbor
                if (!cluster.contains(currentNeighbor)) {
                    cluster.add(currentNeighbor);
                }
                //try to extend cluster at this node
                getClusterCoreInternal(currentNeighbor, startNodeScore, nodeSeenHashMap, currentDepth + 1, cluster, nodeScoreCutoff, maxDepthFromStart, nodeInfoHashMap);
            }
        }
        return (true);
    }
    /**
     * Fluff up the cluster at the boundary by adding lower scoring, non cluster-core neighbors
     * This implements the cluster fluff feature.
     *
     * @param cluster         The cluster to fluff
     * @param nodeSeenHashMap The list of nodes seen already
     * @param nodeInfoHashMap Provides neighbour info
     * @return true
     */
    private boolean fluffClusterBoundary(ArrayList cluster, HashMap nodeSeenHashMap, HashMap nodeInfoHashMap) {
        int currentNode = 0, nodeNeighbor = 0;
        //create a temp list of nodes to add to avoid concurrently modifying 'cluster'
        ArrayList nodesToAdd = new ArrayList();

        //Keep a separate internal nodeSeenHashMap because nodes seen during a fluffing should not be marked as permanently seen,
        //they can be included in another cluster's fluffing step.
        HashMap nodeSeenHashMapInternal = new HashMap();

        //add all current neighbour's neighbours into cluster (if they have high enough clustering coefficients) and mark them all as seen
        for (int i = 0; i < cluster.size(); i++) {
            currentNode = ((Integer) cluster.get(i)).intValue();
            for (int j = 0; j < ((NodeInfo) nodeInfoHashMap.get(new Integer(currentNode))).numNodeNeighbors; j++) {
                nodeNeighbor = ((NodeInfo) nodeInfoHashMap.get(new Integer(currentNode))).nodeNeighbors[j];
                if ((!nodeSeenHashMap.containsKey(new Integer(nodeNeighbor))) && (!nodeSeenHashMapInternal.containsKey(new Integer(nodeNeighbor))) &&
                        ((((NodeInfo) nodeInfoHashMap.get(new Integer(nodeNeighbor))).density) > params.getFluffNodeDensityCutoff())) {
                    nodesToAdd.add(new Integer(nodeNeighbor));
                    nodeSeenHashMapInternal.put(new Integer(nodeNeighbor), new Boolean(true));
                }
            }
        }

        //Add fluffed nodes to cluster
        if (nodesToAdd.size() > 0) {
            cluster.addAll(nodesToAdd.subList(0, nodesToAdd.size()));
        }

        return (true);
    }

    /**
     * Checks if the cluster needs to be filtered according to heuristics in this method
     *
     * @param gpClusterGraph The cluster to check if it passes the filter
     * @return true if cluster should be filtered, false otherwise
     */
    private boolean filterCluster(GraphPerspective gpClusterGraph) {
        if (gpClusterGraph == null) {
            return (true);
        }
        //filter if the cluster does not satisfy the user specified k-core
        GraphPerspective gpCore = getKCore(gpClusterGraph, params.getKCore());
        if (gpCore == null) {
            return (true);
        }
        return (false);
    }

    /**
     * Gives the cluster a haircut (removed singly connected nodes by taking a 2-core)
     *
     * @param gpClusterGraph The cluster graph
     * @param cluster        The cluster node ID list (in the original graph)
     * @param gpInputGraph   The original input graph
     * @return true
     */
    private boolean haircutCluster(GraphPerspective gpClusterGraph, ArrayList cluster, GraphPerspective gpInputGraph) {
        //get 2-core
        GraphPerspective gpCore = getKCore(gpClusterGraph, 2);
        if (gpCore != null) {
            //clear the cluster and add all 2-core nodes back into it
            cluster.clear();
            //must add back the nodes in a way that preserves gpInputGraph node indices
            int[] rootGraphIndices = gpCore.getNodeIndicesArray();
            for (int i = 0; i < rootGraphIndices.length; i++) {
                cluster.add(new Integer(gpInputGraph.getRootGraphNodeIndex(rootGraphIndices[i])));
            }
        }
        return (true);
    }

    /**
     * Calculate the density of a graph
     * The density is defined as the number of edges/the number of possible edges
     *
     * @param gpInputGraph The input graph to calculate the density of
     * @param includeLoops Include the possibility of loops when determining the number of
     *                     possible edges.
     * @return The density of the network
     */
    public double calcDensity(GraphPerspective gpInputGraph, boolean includeLoops) {
        int possibleEdgeNum = 0, actualEdgeNum = 0, loopCount = 0;
        double density = 0;
        String callerID = "Algorithm.calcDensity";
        if (gpInputGraph == null) {
            System.err.println("In " + callerID + ": gpInputGraph was null.");
            return (-1.0);
        }
        if (includeLoops) {
            //count loops
            Iterator nodes = gpInputGraph.nodesIterator();
            while (nodes.hasNext()) {
                Node n = (Node) nodes.next();
                if (gpInputGraph.isNeighbor(n, n)) {
                    loopCount++;
                }
            }
            possibleEdgeNum = gpInputGraph.getNodeCount() * gpInputGraph.getNodeCount();
            actualEdgeNum = gpInputGraph.getEdgeCount() - loopCount;
        } else {
            possibleEdgeNum = gpInputGraph.getNodeCount() * gpInputGraph.getNodeCount();
            actualEdgeNum = gpInputGraph.getEdgeCount();
        }

        density = (double) actualEdgeNum / (double) possibleEdgeNum;
        return (density);
    }
    /**
     * Find a k-core of a graph. A k-core is a subgraph of minimum degree k
     *
     * @param gpInputGraph The input network
     * @param k  The k value of the k-core
     * @return Returns a subgraph with k-core, if any was found at given k
     */
    public GraphPerspective getKCore(GraphPerspective gpInputGraph, int k) {
        String callerID = "Algorithm.getKCore";
        if (gpInputGraph == null) {
            System.err.println("In " + callerID + ": gpInputGraph was null.");
            return (null);
        }

        //filter all nodes with degree less than k until convergence
        boolean firstLoop = true;
        int numDeleted;
        GraphPerspective gpOutputGraph = null;
        while (true) {
            numDeleted = 0;
            ArrayList alCoreNodeIndices = new ArrayList(gpInputGraph.getNodeCount());
            Iterator nodes = gpInputGraph.nodesIterator();
            while (nodes.hasNext()) {
                Node n = (Node) nodes.next();
                if (gpInputGraph.getDegree(n) >= k) {
                    alCoreNodeIndices.add(new Integer(n.getRootGraphIndex())); //contains all nodes with degree >= k
                } else {
                    numDeleted++;
                }
            }
            if ((numDeleted > 0) || (firstLoop)) {
                //convert ArrayList to int[] for creation of a GraphPerspective for this core
                int[] outputNodeIndices = new int[alCoreNodeIndices.size()];
                int j = 0;
                for (Iterator i = alCoreNodeIndices.iterator(); i.hasNext(); j++) {
                    outputNodeIndices[j] = ((Integer) i.next()).intValue();
                }
                gpOutputGraph = gpInputGraph.createGraphPerspective(outputNodeIndices);
                if (gpOutputGraph.getNodeCount() == 0) {
                    return (null);
                }
                //iterate again, but with a new k-core input graph
                gpInputGraph = gpOutputGraph;
                if (firstLoop) {
                    firstLoop = false;
                }
            } else 
                break;
        }
        return (gpOutputGraph);
    }
    /**
     * Find the highest k-core in the input graph.
     *
     * @param gpInputGraph The input network
     * @return Returns the k-value and the core as an Object array.
     *         The first object is the highest k value i.e. objectArray[0]
     *         The second object is the highest k-core as a GraphPerspective i.e. objectArray[1]
     */
    public Object[] getHighestKCore(GraphPerspective gpInputGraph) {
        String callerID = "Algorithm.getHighestKCore";
        if (gpInputGraph == null) {
            System.err.println("In " + callerID + ": gpInputGraph was null.");
            return (null);
        }
        int i = 1;
        GraphPerspective gpCurCore = null, gpPrevCore = null;
        while ((gpCurCore = getKCore(gpInputGraph, i)) != null) {
            gpInputGraph = gpCurCore;
            gpPrevCore = gpCurCore;
            i++;
        }
        Integer k = new Integer(i - 1);
        Object[] returnArray = new Object[2];
        returnArray[0] = k;
        returnArray[1] = gpPrevCore;
        return (returnArray);
    }
    /**
     * Finds the cluster based on user's input via size slider.
     * this function is only called in ResultPanel.SizeAction
     *
     * @param cluster cluster being explored
     * @param nodeScoreCutoff slider source value
     * @param inputNetwork network
     * @param resultTitle title of the result set being explored
     * @return explored cluster
     */
    public Cluster exploreCluster(Cluster cluster, double nodeScoreCutoff, CyNetwork inputNetwork, String resultTitle) {
        HashMap nodeInfoHashMap = (HashMap) nodeInfoResultsMap.get(resultTitle);
        ParameterSet params = ParameterSet.getInstance().getResultParams(cluster.getResultTitle()).copy();
        HashMap nodeSeenHashMap;
        if (nodeScoreCutoff <= params.getNodeScoreCutoff()) {
            nodeSeenHashMap = new HashMap(cluster.getNodeSeenHashMap());
        } else
            nodeSeenHashMap = new HashMap();
        Integer seedNode = cluster.getSeedNode();
        ArrayList alNodes = getClusterCore(seedNode, nodeSeenHashMap, nodeScoreCutoff, params.getMaxDepthFromStart(), nodeInfoHashMap);
        if (!alNodes.contains(seedNode))//make sure seed node is part of cluster, if not already in there
            alNodes.add(seedNode);
        //create an input graph for the filter and haircut methods
        GraphPerspective gpCluster = Algorithm.createGraphPerspective(alNodes, inputNetwork);
        if (params.isHaircut())
            haircutCluster(gpCluster, alNodes, inputNetwork);
        if (params.isFluff())
            fluffClusterBoundary(alNodes, nodeSeenHashMap, nodeInfoHashMap);
        cluster.setALNodes(alNodes);
        gpCluster = Algorithm.createGraphPerspective(alNodes, inputNetwork);
        cluster.setGPCluster(gpCluster);
        cluster.setClusterScore(scoreCluster(cluster));
        return cluster;
    }
}
