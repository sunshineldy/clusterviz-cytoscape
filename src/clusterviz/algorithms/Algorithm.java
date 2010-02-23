package clusterviz.algorithms;

import cytoscape.CyNetwork;
import cytoscape.task.TaskMonitor;
import giny.model.GraphPerspective;
import giny.model.Node;
import giny.model.Edge;

import clusterviz.*;

import java.util.*;
 

/**
 * Abstract class for clustering algorithms
 */

abstract class Algorithm{
	private ParameterSet parameters;
	private CyNetwork currentNetwork;

	public Algorithm(ParameterSet parameters, CyNetwork currentNetwork){};
	public Cluster[] runCluster(CyNetwork currentNetwork, ParameterSet parameters){
		Cluster[] test=null;
		return test;
	};

}
