package ch.specchio.types;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class represents an uncertainty set
 */

@XmlRootElement(name="uncertainty_set")
public class UncertaintySet {
	
	@XmlElement public int uncertainty_set_id;
	@XmlElement public String uncertainty_set_description;
	@XmlElement public int node_set_id;
	
	@XmlElement public ArrayList<Integer> uncertainty_node_ids;
	@XmlElement public ArrayList<Integer> node_nums;
	@XmlElement public AdjacencyMatrix adjacency_matrix;
	
	// So an uncertainty set will contain a list of node_nums and node_ids
	// Alternatively it could contain an object of type 'uc_node_set' which has a list of node nums and node_ids?
	// Going with option 1 for the time being
	
	
	public UncertaintySet() {


	}

	/** 
	 * Get the uncertainty set id 
	 * 
	 * @return uncertainty_set_id the uncertainty set identifier
	 */
	@XmlElement(name = "uncertainty_set_id")
	public int getUncertaintySetId() {
		
		return uncertainty_set_id;
	}
	
	/**
	 * Set the uncertainty set id
	 *
	 * @param uncertainty_set_id  unique uncertainty set id
	 */
	public void setUncertaintySetId(int uncertainty_set_id) {

		this.uncertainty_set_id = uncertainty_set_id;

	}
	
	/** 
	 * Get the uncertainty set description 
	 * 
	 * @return uncertainty_set_description the uncertainty set description
	 */
	@XmlElement(name = "uncertainty_set_description")
	public String getUncertaintySetDescription() {
		
		return uncertainty_set_description;
	}
	
	/**
	 * Set the uncertainty set description
	 *
	 * @param uncertainty_set_description the uncertainty set description
	 */
	public void setUncertaintySetDescription(String uncertainty_set_description) {

		this.uncertainty_set_description = uncertainty_set_description;

	}
	
	/** 
	 * Get the node set id - a node set is a collection of nodes within an uncertainty set
	 * 
	 * @return node_set_id node set identifier
	 */
	@XmlElement(name = "node_set_id")
	public int getNodeSetId() {
		
		return node_set_id;
	}
	
	/**
	 * Set the node set id
	 *
	 * @param node_set_id  unique node set id
	 */
	public void setNodeSetId(int node_set_id) {

		this.node_set_id = node_set_id;

	}
	
	
	
}
