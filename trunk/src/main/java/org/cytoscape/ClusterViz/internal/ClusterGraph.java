package org.cytoscape.ClusterViz.internal;

import java.util.*;

import org.cytoscape.ClusterViz.internal.ClusterUtil;
import org.cytoscape.model.*;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;

public class ClusterGraph
{

	private final CyRootNetwork rootNetwork;
	  private final Set<CyNode> nodes;
	  private final Set<CyEdge> edges;
	  private final Map<Long, CyNode> nodeMap;
	  private final Map<Long, CyEdge> edgeMap;
	  private CySubNetwork subNetwork;
	private ClusterUtil clusterUtil;
	private boolean disposed;
	
	public ClusterGraph(CyRootNetwork rootNetwork, Collection<CyNode> nodes, Collection<CyEdge> edges, ClusterUtil mcodeUtil)
	{
		if (rootNetwork == null)
			throw new NullPointerException("rootNetwork is null!");
		if (nodes == null)
			throw new NullPointerException("nodes is null!");
		if (edges == null)
			throw new NullPointerException("edges is null!");
		this.clusterUtil = mcodeUtil;
		this.rootNetwork = rootNetwork;

		
	    this.nodes = Collections.synchronizedSet(new HashSet(nodes.size()));
	    this.edges = Collections.synchronizedSet(new HashSet(edges.size()));
	    this.nodeMap = Collections.synchronizedMap(new HashMap(nodes.size()));
	    this.edgeMap = Collections.synchronizedMap(new HashMap(edges.size()));

	    for (CyNode n : nodes)
	      addNode(n);
	    for (CyEdge e : edges)
	      addEdge(e);
	}

	public boolean addNode(CyNode node)
	{
	    if (this.nodes.contains(node)) {
	        return false;
	      }
	      node = this.rootNetwork.getNode(node.getSUID().longValue());

	      if (this.nodes.add(node)) {
	        this.nodeMap.put(node.getSUID(), node);
	        return true;
	      }

	      return false;
	}

	public boolean addEdge(CyEdge edge)
	{
		if (this.edges.contains(edge)) {
		      return false;
		    }
		    if ((this.nodes.contains(edge.getSource())) && (this.nodes.contains(edge.getTarget()))) {
		      edge = this.rootNetwork.getEdge(edge.getSUID().longValue());

		      if (this.edges.add(edge)) {
		        this.edgeMap.put(edge.getSUID(), edge);
		        return true;
		      }
		    }

		    return false;
	}

	public int getNodeCount()
	{
		return nodes.size();
	}

	public int getEdgeCount()
	{
		return edges.size();
	}

	 public List<CyNode> getNodeList() {
		    return new ArrayList(this.nodes);
		  }

		  public List<CyEdge> getEdgeList() {
		    return new ArrayList(this.edges);
		  }

	public boolean containsNode(CyNode node)
	{
		return nodes.contains(node);
	}

	public boolean containsEdge(CyEdge edge)
	{
		return edges.contains(edge);
	}

	public CyNode getNode(long index)
	{
		return (CyNode)nodeMap.get(Long.valueOf(index));
	}

	public CyEdge getEdge(long index)
	{
		return (CyEdge)edgeMap.get(Long.valueOf(index));
	}

	public List getAdjacentEdgeList(CyNode node, org.cytoscape.model.CyEdge.Type edgeType)
	{
		List rootList = rootNetwork.getAdjacentEdgeList(node, edgeType);
		List list = new ArrayList(rootList.size());
		for (Iterator iterator = rootList.iterator(); iterator.hasNext();)
		{
			CyEdge e = (CyEdge)iterator.next();
			if (containsEdge(e))
				list.add(e);
		}

		return list;
	}

	public List getConnectingEdgeList(CyNode source, CyNode target, org.cytoscape.model.CyEdge.Type edgeType)
	{
		List rootList = rootNetwork.getConnectingEdgeList(source, target, edgeType);
		List list = new ArrayList(rootList.size());
		for (Iterator iterator = rootList.iterator(); iterator.hasNext();)
		{
			CyEdge e = (CyEdge)iterator.next();
			if (containsEdge(e))
				list.add(e);
		}

		return list;
	}

	public CyRootNetwork getRootNetwork()
	{
		return rootNetwork;
	}

	public synchronized CySubNetwork getSubNetwork()
	{
		if (!disposed && subNetwork == null)
			subNetwork = clusterUtil.createSubNetwork(rootNetwork, nodes, SavePolicy.DO_NOT_SAVE);
		return subNetwork;
	}

	public synchronized boolean isDisposed()
	{
		return disposed;
	}

	public synchronized void dispose()
	{
		if (disposed)
			return;
		if (subNetwork != null)
		{
			clusterUtil.destroy(subNetwork);
			subNetwork = null;
		}
		nodes.clear();
		edges.clear();
		nodeMap.clear();
		edgeMap.clear();
		disposed = true;
	}
}