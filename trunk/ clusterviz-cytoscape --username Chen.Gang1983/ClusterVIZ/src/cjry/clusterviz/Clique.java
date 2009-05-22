package cjry.clusterviz;

import java.util.ArrayList;

public class Clique{
	private int cliuqueID;
	private ArrayList<Integer>	cliqueNodes=null;	//the vector of the nodes in this clique
	private boolean	subordinate;	//the flag showing if this clique is subordinate
	
	public Clique(int ID){
		this.cliuqueID=ID;
		subordinate=false;
	}
	public int getCliuqueID() {
		return cliuqueID;
	}
	public void setCliuqueID(int cliuqueID) {
		this.cliuqueID = cliuqueID;
	}
	public boolean isSubordinate() {
		return subordinate;
	}
	public void setSubordinate(boolean subordinate) {
		this.subordinate = subordinate;
	}
	public ArrayList<Integer> getCliqueNodes() {
		return cliqueNodes;
	}
	public void setCliqueNodes(ArrayList<Integer> cliqueNodes) {
		this.cliqueNodes = cliqueNodes;
	}
}
