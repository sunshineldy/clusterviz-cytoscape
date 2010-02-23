package clusterviz;

import cytoscape.CyNetwork;
import giny.model.GraphPerspective;
import ding.view.DGraphView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Stores various cluster information for simple get/set purposes.
 */
public class Cluster {
    private int complexID;		//the ID of the complex
    private ArrayList alNodes = null;
    private GraphPerspective gpCluster = null;
    private DGraphView dgView = null; //keeps track of layout???
    private Integer seedNode;
    //used in the exploring action
    private HashMap nodeSeenHashMap; //stores the nodes that have already been included in higher ranking clusters
    private double clusterScore;
    private String clusterName; //Pretty much unsed so far, but could store name by user's input
    private int rank;
    private String resultTitle;

    private int inDegree;		//the indegree of the complex
    private int totalDegree;	//the total degree of the complex
    private boolean mergeable;	//the flag showing if the complex is mergeable
    private boolean module;		//the flag showing if this complex can be defined as a module
    private double modularity;

	public Cluster(){
		this.inDegree=0;
		this.totalDegree=0;
	}
	public Cluster(int ID){
		alNodes=new ArrayList();
		this.complexID=ID;
		this.inDegree=0;
		this.totalDegree=0;
		this.mergeable=true;
		this.module=false;
		this.modularity=0.0;
	}
    public int getComplexID() {
		return complexID;
	}
	public void setComplexID(int complexID) {
		this.complexID = complexID;
	}
	public String getResultTitle() {
        return resultTitle;
    }
    public void setResultTitle(String resultTitle) {
        this.resultTitle = resultTitle;
    }
    public String getClusterName() {
        return clusterName;
    }
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }
    public DGraphView getDGView() {
        return dgView;
    }
    public void setDGView(DGraphView dgView) {
        this.dgView = dgView;
    }
    public double getClusterScore() {
        return clusterScore;
    }
    public void setClusterScore(double clusterScore) {
        this.clusterScore = clusterScore;
    }
    public GraphPerspective getGPCluster() {
        return gpCluster;
    }
    public void setGPCluster(GraphPerspective gpCluster) {
        this.gpCluster = gpCluster;
    }
    public ArrayList getALNodes() {
        return alNodes;
    }
    public void setALNodes(ArrayList alNodes) {
        this.alNodes = alNodes;
    }
    public Integer getSeedNode() {
        return seedNode;
    }
    public void setSeedNode(Integer seedNode) {
        this.seedNode = seedNode;
    }
    public HashMap getNodeSeenHashMap() {
        return nodeSeenHashMap;
    }
    public void setNodeSeenHashMap(HashMap nodeSeenHashMap) {
        this.nodeSeenHashMap = nodeSeenHashMap;
    }
    public int getRank() {
        return rank;
    }
    public void setRank(int rank) {
        this.rank = rank;
        this.clusterName = "Complex " + (rank + 1);
    }
	public int getInDegree() {
		return inDegree;
	}
	public void setInDegree(int inDegree) {
		this.inDegree = inDegree;
	}
	public boolean isMergeable() {
		return mergeable;
	}
	public void setMergeable(boolean mergeable) {
		this.mergeable = mergeable;
	}
	public boolean isModule() {
		return module;
	}
	public void setModule(boolean module) {
		this.module = module;
	}
	public int getTotalDegree() {
		return totalDegree;
	}
	public void setTotalDegree(int totalDegree) {
		this.totalDegree = totalDegree;
	}
	public double getModularity() {
		return modularity;
	}
	public void setModularity(double modularity) {
		this.modularity = modularity;
	}
	public void calModularity(CyNetwork currentNetwork){
    	int inDegree=0;
    	int totalDegree=0;
    	ArrayList nodes=this.getALNodes();
    	for(Iterator it=nodes.iterator();it.hasNext();){//for each node in merged C1
    		int node=((Integer)it.next()).intValue();
    		totalDegree+=currentNetwork.getDegree(node);//can this be useful?
    		int[] neighbors=currentNetwork.neighborsArray(node);
    		for(int i=0;i<neighbors.length;i++)
    			if(nodes.contains(new Integer(neighbors[i])))
    				inDegree++;
    	}
    	int outDegree=totalDegree-inDegree;
    	inDegree=inDegree/2;
    	this.setInDegree(inDegree);
    	this.setTotalDegree(totalDegree);
    	double fModule=0;
    	if(inDegree!=0)
    		fModule = (double)inDegree/(double)outDegree;
    	else	fModule=0;
    	setModularity(fModule);
	}
}
