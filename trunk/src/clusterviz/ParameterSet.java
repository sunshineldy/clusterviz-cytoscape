package clusterviz;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import cytoscape.Cytoscape;

/**
 * the set of all the parameters used in clustering
 */
public class ParameterSet {
    private static ParameterSet ourInstance = new ParameterSet();
    private static HashMap currentParams = new HashMap();
    private static HashMap resultParams = new HashMap();
    
    //parameters
    public String networkID;
    public String resultTitle;
    //scope
    public static String NETWORK = "network";
    public static String SELECTION = "selection";
    private String scope;
    private Integer[] selectedNodes;    
    //algorithm
    public static String MCODE = "MCODE";
    public static String EAGLE = "EAGLE";
    public static String FAGEC = "FAG-EC";
    private String algorithm;
    //parameters used in MCODE
    //used in scoring stage
    private boolean includeLoops;
    private int degreeThreshold;
    private int kCore;
    //used in cluster finding stage
    private boolean optimize;
    private int maxDepthFromStart;
    private double nodeScoreThreshold;
    private boolean fluff;
    private boolean haircut;
    private double nodeDensityThreshold;   
    //parameter used when clustering using EAGLE
    private int cliqueSizeThreshold1;
    private int complexSizeThreshold1;  
    //used in clustering using FAG-EC
    private boolean overlapped;
    private double fThreshold;
    private int cliqueSizeThreshold;
    private int complexSizeThreshold;
    private boolean isWeak;
    //result viewing parameters (only used for dialog box of results)
    private int defaultRowHeight;

    /**
     * Constructor for the parameter set object. 
     */
    public ParameterSet() {
        setDefaultParams();
        defaultRowHeight = 80;
    }
    /**
     * Constructor for non-default algorithm parameters.
     * Once an alalysis is conducted, new parameters must be saved so that they can be retrieved in the result panel
     * for exploration and export purposes.
     */    
    public ParameterSet(
    		String networkID,
            String scope,
            String algorithm,
            Integer[] selectedNodes,
            boolean includeLoops,
            int degreeThrshold,
            int kCore,
            boolean optimize,
            int maxDepthFromStart,
            double nodeScoreThreshold,
            boolean fluff,
            boolean haircut,
            double nodeDensityThreshold,
            int cliqueSizeThreshold1,
            int complexSizeThreshold1,
            double fThreshold,
            int cliqueSizeThreshold,
            int complexSizeThreshold,
            boolean isWeak,
            boolean overlapped) {
        setAllAlgorithmParams(
        		networkID,
                scope,
                algorithm,
                selectedNodes,
                includeLoops,
                degreeThrshold,
                kCore,
                optimize,
                maxDepthFromStart, 
                nodeScoreThreshold,
                fluff,
                haircut,
                nodeDensityThreshold,
                cliqueSizeThreshold1,
                complexSizeThreshold1,
                fThreshold,
                cliqueSizeThreshold,
                complexSizeThreshold,
                isWeak,
                overlapped
        );
        defaultRowHeight = 80;
    }
    /**
     * staic method to be used with getParamsCopy(String networkID) 
     * @return ourInstance the static instance of CurrentParameter
     */
    public static ParameterSet getInstance() {
        return ourInstance;
    }
    /**
     * Get a copy of the current parameters for a particular network. 
     * usage:
     * Parameters.getInstance().getParamsCopy();  
     */
    public ParameterSet getParamsCopy(String networkID) {
        if (networkID != null) {
            return ((ParameterSet) currentParams.get(networkID)).copy();
        } else {
            ParameterSet newParams = new ParameterSet();
            return newParams.copy();
        }
    }
    
    public HashMap getAllParamSets(){
    	return resultParams;
    }
    
    public ParameterSet getResultParams(String resultSet) {
        return ((ParameterSet) resultParams.get(resultSet));
    }
    
    public static void removeResultParams(String resultTitle) {
        resultParams.remove(resultTitle);
    } 
    /**
     * Current parameters can only be updated using this method.
     */
    public ParameterSet setParams(ParameterSet newParams, String resultTitle, String networkID) {
        //cannot simply equate the params and newParams classes since that creates a permanent reference
        //and prevents us from keeping 2 sets of the class such that the saved version is not altered
        //until this method is called
        ParameterSet currentParamSet = new ParameterSet(
        		newParams.getNetworkID(),
                newParams.getScope(),
                newParams.getAlgorithm(),
                newParams.getSelectedNodes(),
                newParams.isIncludeLoops(),
                newParams.getDegreeThreshold(),
                newParams.getKCore(),
                newParams.isOptimize(),
                newParams.getMaxDepthFromStart(),
                newParams.getNodeScoreCutoff(),
                newParams.isFluff(),
                newParams.isHaircut(),
                newParams.getFluffNodeDensityCutoff(),
                newParams.getCliqueSizeThreshold1(),
                newParams.getComplexSizeThreshold1(),
                newParams.getFThreshold(),
                newParams.getCliqueSizeThreshold(),
                newParams.getComplexSizeThreshold(),
                newParams.isWeak(),
                newParams.isOverlapped()
        );
        //replace with new value
        currentParams.put(networkID, currentParamSet);
        ParameterSet resultParamSet = new ParameterSet(
        		newParams.getNetworkID(),
                newParams.getScope(),
                newParams.getAlgorithm(),
                newParams.getSelectedNodes(),
                newParams.isIncludeLoops(),
                newParams.getDegreeThreshold(),
                newParams.getKCore(),
                newParams.isOptimize(),
                newParams.getMaxDepthFromStart(),
                newParams.getNodeScoreCutoff(),
                newParams.isFluff(),
                newParams.isHaircut(),
                newParams.getFluffNodeDensityCutoff(),
                newParams.getCliqueSizeThreshold1(),
                newParams.getComplexSizeThreshold1(),
                newParams.getFThreshold(),
                newParams.getCliqueSizeThreshold(),
                newParams.getComplexSizeThreshold(),
                newParams.isWeak(),
                newParams.isOverlapped()
        );
        resultParams.put(resultTitle, resultParamSet);
        return resultParamSet;
    }
    /**
     * Method for setting all parameters to their default values
     */
    public void setDefaultParams() {
        setAllAlgorithmParams(Cytoscape.getCurrentNetwork().getIdentifier(), NETWORK, "", new Integer[0], 
        		false, 2, 2, false, 100, 0.2, false, true, 0.1, 3, 2, 1.0, 3, 2, true,false);
    }

    /**
     * Convenience method to set all the main algorithm parameters
     * 
     * @param networkID the identifier of the network
     * @param scope Scope of the search (equal to one of the two fields NETWORK or SELECTION)
     * @param algorithm The algorithm user choosed to cluster the network
     * @param selectedNodes Node selection for selection-based scope
     * @param includeLoops include loops or not
     * @param degreeThreshold degree threshold
     * @param kCore the value of k in K-Core
     * @param optimize Determines if parameters are customized by user/default or optimized
     * @param maxDepthFromStart max depth from seed node
     * @param nodeScoreThreshold node score threshold
     * @param fluff fluff the resulting clusters or not
     * @param haircut haircut the clusters or not
     * @param nodeDensityThreshold nodedesity thrshold
     * @param cliqueSizeThreshold1
     * @param complexSizeThreshold1
     * @param fThreshold
     * @param cliqueSizeThreshold
     * @param complexSizeThreshold
     * @param isWeak
     * @param overlapped
     */
    public void setAllAlgorithmParams(
    		String networkID,
            String scope,
            String algorithm,
            Integer[] selectedNodes,
            boolean includeLoops,
            int degreeThreshold,
            int kCore,
            boolean optimize,
            int maxDepthFromStart,
            double nodeScoreThreshold,
            boolean fluff,
            boolean haircut,
            double nodeDensityThreshold,
            int cliqueSizeThreshold1,
            int complexSizeThreshold1,
            double fThreshold,
            int cliqueSizeThreshold,
            int complexSizeThreshold,
            boolean isWeak,
            boolean overlapped) {
    	this.networkID=networkID;
        this.scope = scope;
        this.algorithm=algorithm;
        this.selectedNodes = selectedNodes;
        this.includeLoops = includeLoops;
        this.degreeThreshold = degreeThreshold;
        this.kCore = kCore;
        this.optimize = optimize;
        this.maxDepthFromStart = maxDepthFromStart;
        this.nodeScoreThreshold = nodeScoreThreshold;
        this.fluff = fluff;
        this.haircut = haircut;
        this.nodeDensityThreshold = nodeDensityThreshold;
        this.cliqueSizeThreshold1=cliqueSizeThreshold1;
        this.complexSizeThreshold1=complexSizeThreshold1;
        this.fThreshold=fThreshold;
        this.cliqueSizeThreshold=cliqueSizeThreshold;
        this.complexSizeThreshold=complexSizeThreshold;
        this.isWeak=isWeak;
        this.overlapped=overlapped;
    }

    /**
     * Copies a parameter set object
     *
     * @return A copy of the parameter set
     */
    public ParameterSet copy() {
        ParameterSet newParam = new ParameterSet();
        newParam.setNetworkID(this.networkID);
        newParam.setScope(this.scope);
        newParam.setAlgorithm(this.algorithm);
        newParam.setSelectedNodes(this.selectedNodes);
        newParam.setIncludeLoops(this.includeLoops);
        newParam.setDegreeThreshold(this.degreeThreshold);
        newParam.setKCore(this.kCore);
        newParam.setOptimize(this.optimize);
        newParam.setMaxDepthFromStart(this.maxDepthFromStart);
        newParam.setNodeScoreCutoff(this.nodeScoreThreshold);
        newParam.setFluff(this.fluff);
        newParam.setHaircut(this.haircut);
        newParam.setFluffNodeDensityCutoff(this.nodeDensityThreshold);
        newParam.setCliqueSizeThreshold1(this.cliqueSizeThreshold1);
        newParam.setComplexSizeThreshold1(this.complexSizeThreshold1);
        newParam.setCliqueSizeThreshold(this.cliqueSizeThreshold);
        newParam.setComplexSizeThreshold(this.complexSizeThreshold);
        newParam.setFThreshold(this.fThreshold);
        newParam.setWeak(this.isWeak);
        newParam.setOverlapped(this.overlapped);
        //results dialog box
        newParam.setDefaultRowHeight(this.defaultRowHeight);
        return newParam;
    }
		
	/**
     * Generates a summary of the parameters. Only parameters that are necessary are included.
     * For example, if fluff is not turned on, the fluff density cutoff will not be included.
     * 
     * @return Buffered string summarizing the parameters
     */
	
    public String toString() {
        String lineSep = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer();
    	sb.append("   Network: "+networkID);
        if(algorithm.equals(MCODE)){
        	sb.append("   Algorithm:  MCODE"+lineSep);
            sb.append("   Scoring:" + lineSep
                    + "      IncludeLoop: " + includeLoops + "  DegreeThreshold: " + degreeThreshold + lineSep);
            sb.append("   Clustering:" + lineSep
                    + "      NodeScoreThreshold: " + nodeScoreThreshold + "  Haircut: " + haircut +lineSep
                    + "      Fluff: " + fluff + ((fluff) ? ("  FluffNodeDensityThreshold " + nodeDensityThreshold) : "")+lineSep
                    + "      K-Core: " + kCore + "  Max.DepthFromSeed: " + maxDepthFromStart + lineSep);
        }
        else if(algorithm.equals(FAGEC)){
        	sb.append("   Algorithm:  FAG-EC"+lineSep);
            sb.append("   Clustering:" + lineSep
                    + "      DefinitionWay: " + ((isWeak)? ("Weak  In/OutThreshold: "+fThreshold ):"Strong") + lineSep
                    + "      Overlapped: " + overlapped + ((overlapped)? (" CliqueSizeThreshold: "+cliqueSizeThreshold ):"")+lineSep
                    + "      OutputThreshold: " + complexSizeThreshold + lineSep);        	
        }
        else if(algorithm.equals(EAGLE)){
        	sb.append("   Algorithm:  EAGLE"+lineSep);
            sb.append("   Clustering:" + lineSep
                    + "      CliqueSizeThrshold: " + cliqueSizeThreshold1 
                    + "  OutputThreshold: " + complexSizeThreshold1 + lineSep);
        }
        return sb.toString();
    }

    //parameter getting and setting	
    public String getNetworkID() {
		return networkID;
	}
    
	public void setNetworkID(String networkID) {
		this.networkID = networkID;
	}
    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public String getAlgorithm() {
        return this.algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Integer[] getSelectedNodes() {
        return selectedNodes;
    }

    public void setSelectedNodes(Integer[] selectedNodes) {
        this.selectedNodes = selectedNodes;
    }

    public boolean isIncludeLoops() {
        return includeLoops;
    }

    public void setIncludeLoops(boolean includeLoops) {
        this.includeLoops = includeLoops;
    }

    public int getDegreeThreshold() {
        return degreeThreshold;
    }

    public void setDegreeThreshold(int degreeThreshold) {
        this.degreeThreshold = degreeThreshold;
    }

    public int getKCore() {
        return kCore;
    }

    public void setKCore(int kCore) {
        this.kCore = kCore;
    }

    public void setOptimize(boolean optimize) {
        this.optimize = optimize;
    }

    public boolean isOptimize() {
        return optimize;
    }

    public int getMaxDepthFromStart() {
        return maxDepthFromStart;
    }

    public void setMaxDepthFromStart(int maxDepthFromStart) {
        this.maxDepthFromStart = maxDepthFromStart;
    }

    public double getNodeScoreCutoff() {
        return nodeScoreThreshold;
    }

    public void setNodeScoreCutoff(double nodeScoreThreshold) {
        this.nodeScoreThreshold = nodeScoreThreshold;
    }

    public boolean isFluff() {
        return fluff;
    }

    public void setFluff(boolean fluff) {
        this.fluff = fluff;
    }

    public boolean isHaircut() {
        return haircut;
    }

    public void setHaircut(boolean haircut) {
        this.haircut = haircut;
    }

    public double getFluffNodeDensityCutoff() {
        return nodeDensityThreshold;
    }

    public void setFluffNodeDensityCutoff(double nodeDensityThreshold) {
        this.nodeDensityThreshold = nodeDensityThreshold;
    }

    public int getDefaultRowHeight() {
        return defaultRowHeight;
    }

    public void setDefaultRowHeight(int defaultRowHeight) {
        this.defaultRowHeight = defaultRowHeight;
    }

	public int getCliqueSizeThreshold1() {
		return cliqueSizeThreshold1;
	}

	public void setCliqueSizeThreshold1(int cliqueSizeThreshold1) {
		this.cliqueSizeThreshold1 = cliqueSizeThreshold1;
	}

	public int getComplexSizeThreshold1() {
		return complexSizeThreshold1;
	}

	public void setComplexSizeThreshold1(int complexSizeThreshold1) {
		this.complexSizeThreshold1 = complexSizeThreshold1;
	}

	public int getCliqueSizeThreshold() {
		return cliqueSizeThreshold;
	}

	public void setCliqueSizeThreshold(int cliqueSizeThreshold) {
		this.cliqueSizeThreshold = cliqueSizeThreshold;
	}

	public int getComplexSizeThreshold() {
		return complexSizeThreshold;
	}

	public void setComplexSizeThreshold(int complexSizeThreshold) {
		this.complexSizeThreshold = complexSizeThreshold;
	}

	public double getFThreshold() {
		return fThreshold;
	}

	public void setFThreshold(double fThreshold) {
		this.fThreshold = fThreshold;
	}

	public boolean isWeak() {
		return isWeak;
	}

	public void setWeak(boolean isWeak) {
		this.isWeak = isWeak;
	}

    public boolean isOverlapped() {
		return overlapped;
	}

	public void setOverlapped(boolean overlapped) {
		this.overlapped = overlapped;
	}	
		
    public String getResultTitle() {
		return resultTitle;
	}
	public void setResultTitle(String resultTitle) {
		this.resultTitle = resultTitle;
	}
}
