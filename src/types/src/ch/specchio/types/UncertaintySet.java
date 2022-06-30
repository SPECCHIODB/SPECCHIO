package ch.specchio.types;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.ujmp.core.Matrix;

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
	@XmlElement public ArrayList<String> uncertainty_node_descriptions;
	@XmlTransient public Matrix adjacency_matrix;
	@XmlElement public int[][] adjacency_matrix_as_int_array;
	
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
	
	/** 
	 * Get the node set id - a node set is a collection of nodes within an uncertainty set
	 * 
	 * @return node_set_id node set identifier
	 */
	@XmlTransient
	public Matrix getAdjacencyMatrix() {
		
		return adjacency_matrix;
	}
	
	/**
	 * Set the node set id
	 *
	 * @param node_set_id  unique node set id
	 */
	public void setAdjacencyMatrix(Matrix adjacency_matrix) {

		this.adjacency_matrix = adjacency_matrix;

	}
	/**
	 * Get uncertainty node descriptions for an uncertainty set
	 * 
	 * @return uncertainty_node_descriptions a list of uncertainty set descriptions
	 */
	public ArrayList<String> getUncertaintyNodeDescriptions() {
		return uncertainty_node_descriptions;
	}
	
	/**
	 * Set the uncertainty node descriptions for an uncertainty set
	 * 
	 * @param uncertainty_node_descriptions a list of uncertainty node descriptions
	 */
	public void setUncertaintyNodeDescriptions(ArrayList<String> uncertainty_node_descriptions) {
		
		this.uncertainty_node_descriptions = uncertainty_node_descriptions;
	}
	
	/**
	 * Get the uncertainty node numbers for an uncertainty set
	 * 
	 * @return node_nums a list of uncertainty node numbers
	 */
	
	@XmlElement(name="node_num")
	public ArrayList<Integer> getNodeNums() {
		return node_nums;
	}
	
	/**
	 * Set the uncertainty node numbers for an uncertainty set
	 * 
	 * @param node_nums a list of uncertainty node numbers
	 */
	
	public void setNodeNums(ArrayList<Integer> node_nums) {
		this.node_nums = node_nums;
	}
	
	/**
	 * Get the uncertainty node ids for an uncertainty set
	 * 
	 * @return uncertainty_node_ids a list of uncertainty node ids for an uncertainty set
	 */
	
	@XmlElement(name="uncertainty_node_ids")
	public ArrayList<Integer> getUncertaintyNodeIds() {
		return uncertainty_node_ids;
	}
	
	/**
	 * Set the uncertainty node ids for an uncertainty set
	 * 
	 * @param uncertainty_set_node_ids a list of uncertainty node ids
	 */
	
	public void setUncertaintyNodeIds(ArrayList<Integer> uncertainty_node_ids) {
		this.uncertainty_node_ids = uncertainty_node_ids;
	}
	
	/**
	 * Get the adjacency matrix as an integer array for an uncertainty set
	 * 
	 * @return adjacency matrix as a 2d integer array
	 */
	  public int[][] getAdjacencyMatrixAsIntArray() { return
	  adjacency_matrix_as_int_array; }
	  
	/**
	 * Set the adjacency matrix as an integer array for an uncertainty set
	 * 
	 * @param adjacency matrix as a 2d integer array
	*/
	  public void setAdjacencyMatrixAsIntArray(int[][]
	  adjacency_matrix_as_int_array) { this.adjacency_matrix_as_int_array =
	  adjacency_matrix_as_int_array; }
	
	  
	
}
