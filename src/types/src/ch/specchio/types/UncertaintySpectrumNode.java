package ch.specchio.types;

import java.util.ArrayList;

import javax.xml.bind.annotation.*;

/**
 * This class represents a spectrum uncertainty node
 */

@XmlRootElement(name="uncertainty_spectrum_node")
@XmlSeeAlso({UncertaintySpectrumNode.class})
public class UncertaintySpectrumNode extends UncertaintyNode {

	@XmlElement public float[][] u_vectors;
	@XmlElement public int spectrum_node_id;
	
	@XmlElement public ArrayList<Integer> spectrum_ids = new ArrayList<Integer>();
	@XmlElement public int spectrum_subset_id;// used to distinguish uncertainty nodes by their subsets (if existing, otherwise a single subset is the default for a group of uncertainty nodes)
	@XmlElement public ArrayList<Integer> spectrum_subset_ids = new ArrayList<Integer>();
	@XmlElement public int spectrum_set_id;
	@XmlElement public String spectrum_set_description;
	
	
	public UncertaintySpectrumNode() {
		
		node_type = "spectrum";
		this.is_spectrum = true;
	}
	
	public UncertaintySpectrumNode(UncertaintyNode uc_node) {
		
		this.node_type = uc_node.node_type;
		this.u_vector = uc_node.u_vector;
		this.confidence_level = uc_node.confidence_level;
		this.abs_rel = uc_node.abs_rel;
		this.unit_id = uc_node.unit_id;
		this.node_description = uc_node.node_description;
		this.is_spectrum = true;
		
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
		
		u_vectors = new float[rows][columns];
		
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


	//@XmlElement(name="u_vectors")
	public void setUncertaintyVectors(float[][] u_vectors) {
		this.u_vectors = u_vectors;
	}

	public void setUncertaintyVectors(ArrayList<float[]> vector_list)
	{
		float[][] f = new float[vector_list.size()][vector_list.get(0).length];

		for(int i = 0;i<vector_list.size();i++)
		{
			f[i] = vector_list.get(i);
		}

		this.setUncertaintyVectors(f);

	}
	
	/**
	 * Get the instrument node uncertainty vector
	 *
	 * @return u_vectors
	 */

	// Avoid duplication of entries by not adding the @XmlElement(name="u_vectors") again (already specified during definition) (https://stackoverflow.com/questions/52667244/duplicate-elements-created-during-jaxb-marshalling)
	//@XmlElement(name="u_vectors")
	public float[][] getUncertaintyVectors() {
		return u_vectors;
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
	 * @return  spectrum_ids a list of spectrum ids
	 */
	//@XmlElement(name="spectrum_ids")
	public ArrayList<Integer> getSpectrumIds() {
		return spectrum_ids;
	}
	
	/**
	 * Set the spectrum ids 
	 * 
	 * @param spectrum_ids a list of spectrum ids
	 */
	//@XmlElement(name="spectrum_ids")
	public void setSpectrumIds(ArrayList<Integer> spectrum_ids) {
		
		this.spectrum_ids = spectrum_ids;
	}
	
	/** 
	 * Get the spectrum subset id 
	 * 
	 * @return spectrum_subset_id the spectrum subset identifier
	 */
	//@XmlElement(name = "spectrum_subset_id")
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
	//@XmlElement(name = "spectrum_set_id")
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
	//@XmlElement(name="spectrum_set_description")
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
	
	/**
	 * Get the spectrum subset ids for a spectrum level uncertainty node
	 * 
	 * @return  spectrum_subset_ids a list of spectrum subset ids
	 */
	//@XmlElement(name="spectrum_subset_ids")
	public ArrayList<Integer> getSpectrumSubsetIds() {
		return spectrum_subset_ids;
	}
	
	/**
	 * Set the spectrum subset ids for a spectrum level uncertainty node
	 * 
	 * @param spectrum_subset_ids a list of spectrum subset ids
	 */
	public void setSpectrumSubsetIds(ArrayList<Integer> spectrum_subset_ids) {
		
		this.spectrum_subset_ids = spectrum_subset_ids;
	}
	
}
