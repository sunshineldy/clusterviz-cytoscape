package org.cytoscape.ClusterViz.internal.algorithm;



import org.cytoscape.ClusterViz.internal.Cluster;
import org.cytoscape.ClusterViz.internal.ClusterUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyEdge;

import java.util.*;


import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyEdge;



public class MCODE extends Algorithm{
	
	public MCODE(Long networkID,ClusterUtil clusterUtil){
		super(networkID, clusterUtil);
	}




	@Override
	public List<Cluster> run(CyNetwork inputNetwork, int resultTitle) {
		// TODO Auto-generated method stub
		int i=0;
		return null;
//		return(this.K_CoreFinder(inputNetwork, resultTitle));
	}
}
