package ch.specchio.types;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class represents a spectrum uncertainty node
 */

@XmlRootElement(name="uncertainty_spectrum_node")
public class UncertaintySpectrumNode extends UncertaintyNode {

	@XmlElement public Float[][] u_vectors;
	@XmlElement public int spectrum_node_id;
	
	@XmlElement public ArrayList<Integer> spectrum_ids;
	@XmlElement public int spectrum_subset_id;
	@XmlElement public int spectrum_set_id;
	@XmlElement public String spectrum_set_description;
	
	
	public UncertaintySpectrumNode() {
		
		node_type = "spectrum";
		
	}
	
	public UncertaintySpectrumNode(UncertaintyNode uc_node) {
		
		this.node_type = uc_node.node_type;
		this.u_vector = uc_node.u_vector;
		this.confidence_level = uc_node.confidence_level;
		this.abs_rel = uc_node.abs_rel;
		this.unit_id = uc_node.unit_id;
		this.node_description = uc_node.node_description;
		
		
	}
	
	/**
	 * Set the instrument node uncertainty matrix.
	 * @param u_matrix
	 */
	
	public void setUncertaintyMatrix(double[][] u_matrix) {
		
		int columns = u_matrix[0].length;
		int rows = u_matrix.length;	
		
		System.out.println("columns:" + columns);
		System.out.println("rows:" + rows);
		
		u_vectors = new Float[rows][columns];
		
	    // store data
	    for (int r = 0; r < rows;r++)
	    {
		    for (int c = 0; c < columns;c++)
		    {	
		    	float f = (float)(u_matrix[r][c]);
		    	this.u_vectors[r][c] = Float.valueOf(f);		    	
		    }	    	
	    }	
	    
	    System.out.println("Injected into vectors!");
	
	}	
	
	/** 
	 * Get the spectrum node id
	 * @return spectrum_node_id
	 */
	
	@XmlElement(name = "spectrum_node_id")
	public int getSpectrumNodeId() {
		return spectrum_node_id;
	}
	
	/**
	 * Set the spectrum node id
	 * @param spectrum_node_id  
	 */
	
	public void setSpectrumNodeId(int spectrum_node_id) {
		this.spectrum_node_id = spectrum_node_id;
	}
	
	/**
	 * Get the spectrum ids
	 * 
	 * @param spectrum_ids a list of spectrum ids
	 */
	@XmlElement(name="spectrum_ids")
	public ArrayList<Integer> getSpectrumIds() {
		return spectrum_ids;
	}
	
	/**
	 * Set the spectrum ids 
	 * 
	 * @param spectrum_ids a list of spectrum ids
	 */
	public void setSpectrumIds(ArrayList<Integer> spectrum_ids) {
		
		this.spectrum_ids = spectrum_ids;
	}
	
	/** 
	 * Get the spectrum subset id 
	 * 
	 * @return spectrum_subset_id the spectrum subset identifier
	 */
	@XmlElement(name = "spectrum_subset_id")
	public int getSpectrumSubsetId() {
		
		return spectrum_subset_id;
	}
	
	/**
	 * Set the spectrum subset id
	 *
	 * @param spectrum_subset_id  unique spectrum subset id
	 */
	public void setSpectrumSubsetId(int spectrum_subset_id) {

		this.spectrum_subset_id = spectrum_subset_id;

	}
	
	/** 
	 * Get the spectrum set id 
	 * 
	 * @return spectrum_set_id the spectrum set identifier
	 */
	@XmlElement(name = "spectrum_set_id")
	public int getSpectrumSetId() {
		
		return spectrum_set_id;
	}
	
	/**
	 * Set the spectrum set id
	 *
	 * @param spectrum_set_id  unique spectrum set id
	 */
	public void setSpectrumSetId(int spectrum_set_id) {

		this.spectrum_set_id = spectrum_set_id;

	}
	
	/**
	 * Get the spectrum set description.
	 *
	 * @return spectrum_set_description  
	 */
	@XmlElement(name="spectrum_set_description")
	public String getSpectrumSetDescription() {

		return spectrum_set_description;

	}
	
	/**
	 * Set the spectrum set description
	 *
	 * @param spectrum_set_description  
	 */
	public void setSpectrumSetDescription(String spectrum_set_description) {

		this.spectrum_set_description = spectrum_set_description;

	}
	
}
