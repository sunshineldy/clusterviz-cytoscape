package org.cytoscape.ClusterViz.internal;


import org.cytoscape.ClusterViz.internal.algorithm.Algorithm;
import org.cytoscape.model.CyNetwork;


import java.awt.Image;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkView;

/**
 * Stores various cluster information for simple get/set purposes.
 */

public class Cluster {

	private int resultId;
	private Long seedNode;
	private ClusterGraph graph;
	private List<Long> alCluster;

	public void setAlCluster(List<Long> alCluster) {
		this.alCluster = alCluster;
	}

	private CyNetworkView view; // keeps track of layout so that layout process doesn't have to be repeated unnecessarily
	private Map<Long, Boolean> nodeSeenHashMap; // stores the nodes that have already been included in higher ranking clusters
	private double score;
	private String name; // pretty much unused so far, but could store name by user's input
	private int rank;
	private Image image;
	private boolean disposed;
    private int inDegree;		//the indegree of the complex
    private int totalDegree;	//the total degree of the complex

    private double modularity;
    
    private boolean mergeable;	//the flag showing if the complex is mergeable
    private boolean module;		//the flag showing if this complex can be defined as a module

    
	public Cluster(){
		this.inDegree=0;
		this.totalDegree=0;
		 
	}
    
	public Cluster(int ID){
		alCluster=new ArrayList<Long>();
		this.resultId=ID;
		this.inDegree=0;
		this.totalDegree=0;
		this.mergeable=true;
		this.module=false;
		this.modularity=0.0;
	}

	public Cluster(final int resultId,
						final Long seedNode,
						final ClusterGraph graph,
						final double score,
						final List<Long> alCluster,
						final Map<Long, Boolean> nodeSeenHashMap) {
		assert seedNode != null;
		assert graph != null;
		assert alCluster != null;
		assert nodeSeenHashMap != null;
		
		this.resultId = resultId;
		this.seedNode = seedNode;
		this.graph = graph;
		this.score = score;
		this.alCluster = alCluster;
		this.nodeSeenHashMap = nodeSeenHashMap;
		this.modularity=0.0;
		this.inDegree=0;
		this.totalDegree=0;
	}

	
	public double getModularity() {
		return modularity;
	}
	public void setModularity(double modularity) {
		this.modularity = modularity;
	}
	
	public int getTotalDegree() {
		return totalDegree;
	}
	public void setTotalDegree(int totalDegree) {
		this.totalDegree = totalDegree;
	}
	public int getInDegree() {
		return inDegree;
	}
	public void setInDegree(int inDegree) {
		this.inDegree = inDegree;
	}
	
	public int getResultId() {
		return resultId;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		throwExceptionIfDisposed();
		this.name = name;
	}

	public ClusterGraph getGraph() {
		return graph;
	}
	
	public synchronized CyNetworkView getView() {
		return view;
	}

	public synchronized void setView(final CyNetworkView view) {
		throwExceptionIfDisposed();
		
		if (this.view != null)
			this.view.dispose();
		
		this.view = view;
	}

	public synchronized CySubNetwork getNetwork() {
		return graph.getSubNetwork();
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}
	
	public List<Long> getALCluster() {
		return alCluster;
	}

	public Long getSeedNode() {
		return seedNode;
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
	
	public Map<Long, Boolean> getNodeSeenHashMap() {
		return nodeSeenHashMap;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
		this.name = "Cluster " + (rank + 1);
	}
	
	public synchronized Image getImage() {
		return image;
	}

	public synchronized void setImage(Image image) {
		this.image = image;
	}

	public synchronized boolean isDisposed() {
		return disposed;
	}

	public synchronized void dispose() {
		if (isDisposed()) return;
		
		if (view != null)
			view.dispose();
		
		graph.dispose();
		
		disposed = true;
	}
	

	public void setGraph(ClusterGraph graph) {
		this.graph = graph;
	}

	
    public void setResultTitle(int resultTitle) {
        this.resultId = resultTitle;
    }
	   
    public void setSeedNode(Long seedNode) {
        this.seedNode = seedNode;
    }
    
    
    
	public void calModularity(CyNetwork currentNetwork,Algorithm alg){
    	int inDegree=0;
    	int totalDegree=0;
    	List<Long> nodes=this.getALCluster();
    	for(Iterator it=nodes.iterator();it.hasNext();){//for each node in merged C1
    		Long node=((Long)it.next()).longValue();
    		
//   		totalDegree+=currentNetwork.getDegree(node);//can this be useful?
    		
    		totalDegree+=alg.getNodeDegree(currentNetwork,node);//can this be useful?
    		Long[] neighbors=alg.getNeighborArray(currentNetwork, node);
    		
    		
//    		int[] neighbors=currentNetwork.neighborsArray(node);
    		for(int i=0;i<neighbors.length;i++)
    			if(nodes.contains(new Long(neighbors[i])))
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
	
	
	
	@Override
	public String toString() {
		return "Cluster [clusterName=" + name + ", clusterScore=" + score + 
				", rank=" + rank + ", resultId=" + resultId + ", disposed=" + disposed + "]";
	}
	
	@Override
	protected void finalize() throws Throwable {
		dispose();
		super.finalize();
	}

	private void throwExceptionIfDisposed() {
		if (isDisposed())
			throw new RuntimeException("Cluster has been disposed and cannot be used anymore: ");
	}
}
