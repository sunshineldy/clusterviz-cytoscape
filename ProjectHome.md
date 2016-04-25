**We are working on porting this plugin to Cytoscape 3.0. Please let us know if you have any comment or suggestion.**

ClusterViz is a Cytoscape plugin for clustering analysis of Biological network. ClusterViz is based on MCODE and BiNGO. Thanks very much for their contribution to the community. ClusterViz has been tested on Cytoscape 2.6.X and the latest Cytoscape 2.7.0.

[Download the manual](http://clusterviz-cytoscape.googlecode.com/files/ClusterViz.pdf).

### 1. Introduction ###
ClusterViz is a Cytoscape plug-in for analysis and visualization of clusters from
network. Based on another Cytoscape plug-in named MCODE, three different graph
clustering algorithms (FAG-EC, EAGLE and MCODE) were implemented in ClusterViz.
This plug-in is developed by Juan Cai and Gang Chen, and supported by Prof.
Jianxin Wang and Dr. Min Li, from Central South University.

### 2. Quick Start ###
Following is a short quick start for the usage of ClusterViz, detailed
information of the algorithms implemented in this plug-in can be found in
related papers.
  1. nstall Cytoscape: Download and install latest Cytoscape 2.7.0 (www.cytoscape.org).
  1. nstall ClusterViz: 1)Download the Jar version of ClusterViz from our website, and then move it to the plug-in directory of Cytoscape; 2) Install ClusterViz using plug-in manager of Cytoscape.
  1. lustering Analysis: Start Cytoscape, and click plugin->ClusterViz->Start to start ClusterViz. Choose clustering algorithm and set parameters, then click Analyze button. Clustering results will be displayed on the right panel.
  1. O Enrichment Analysis: right-click on the interested identified cluster, click "GO Enrichment Analysis on Cluster XX" to active the setting panel of GO enrichment analysis. Click run BiNGO button to perform GO enrichment analysis. Results are provided as a table and a colorful graph.

### 3. Contact Information ###
Please feel free to contact us if any problem exists or you have some suggestion at chengangcs@gmail.com. Or you can contact developers directly.