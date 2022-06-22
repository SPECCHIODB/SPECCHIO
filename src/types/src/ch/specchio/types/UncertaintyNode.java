package ch.specchio.types;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class represents an uncertainty node
 */

@XmlRootElement(name="uncertainty_node")
public class UncertaintyNode {

	@XmlElement public int uncertainty_node_id;
	@XmlElement public String node_type;
	@XmlElement public Float[] u_vector;
	@XmlElement public double confidence_level;
	@XmlElement public String abs_rel;
	@XmlElement public int unit_id;
	@XmlElement public String node_description;
	
	@XmlElement public Boolean is_spectrum;
	
	@XmlElement public String uncertainty_node_description;
	
	@XmlElement public ArrayList<UncertaintySourcePair> uncertainty_source_pairs;
	
	public UncertaintyNode() {
	
		this.uncertainty_source_pairs = new ArrayList<UncertaintySourcePair>();
		
	}
	
	// get set statements
	
	/** 
	 * Get the uncertainty node id
	 * @return uncertainty_node_id
	 */
	
	@XmlElement(name = "uncertainty_node_id")
	public int getUncertaintyNodeId() {
		return uncertainty_node_id;
	}
	
	/**
	 * Set the uncertainty node id
	 * @param uncertainty_node_id  
	 */
	
	public void setUncertaintyNodeId(int uncertainty_node_id) {
		this.uncertainty_node_id = uncertainty_node_id;
	}
	
	/**
	 * Get the uncertainty node node_type.
	 * @return node_type   either 'spectrum' or 'instrument'
	 */
	
	@XmlElement(name="node_type")
	public String getNodeType() {
		return node_type;
	}
	
	/**
	 * Set the uncertainty node node_type
	 * @param node_type  a either 'spectrum' or 'instrument'
	 */
	
	public void setNodeType(String node_type) {
		this.node_type = node_type;
	}
	
	/**
	 * Get the instrument node uncertainty vector
	 *
	 * @return u_vector
	 */

	@XmlElement(name="u_vector")
	public Float[] getUncertaintyVector() {
		return u_vector;
	}
	
	/**
	 * Set the instrument node uncertainty vector (fast method).
	 *
	 * @param u_vector_input
	 */
	public void setUncertaintyVector(double[] u_vector_input) {
		
		int rows = u_vector_input.length;	
		
		System.out.println("rows:" + rows);
		
		Float[] u_vector_1d = new Float[rows];
		
		u_vector = new Float[rows];
		
	    // store data
	    for (int r = 0; r < rows;r++)
	    {
	    	   u_vector_1d[r] = (float) u_vector_input[r];
		    	float f = (float)(u_vector_1d[r]);
		    	this.u_vector[r] = Float.valueOf(f);		    	   	
	    }	
	    
	    System.out.println("Injected into vector!");
	
	}	
	
	/**
	 * Get the instrument node confidence level % from 0 to 1.
	 * @return confidence_level   Percentage from 0 to 1
	 */
	
	@XmlElement(name="confidence_level")
	public double getConfidenceLevel() {
		return confidence_level;
	}
	
	/**
	 * Set the instrument node confidence level % from 0 to 1.
	 * @param confidence_level   Percentage from 0 to 1
	 */
	
	public void setConfidenceLevel(double confidence_level) {
		this.confidence_level = confidence_level;
	}

	/**
	 * Get whether the node's uncertainty is absolute or relative.
	 * @return abs_rel   absolute or relative uncertainty 
	 */
	
	@XmlElement(name="abs_rel")
	public String getAbsRel() {
		return abs_rel;
	}
	
	/**
	 * Set whether the node's uncertainty is absolute or relative.
	 * @param abs_rel   absolute or relative uncertainty 
	 */
	
	public void setAbsRel(String abs_rel) {
		this.abs_rel = abs_rel;
	}
	
	/**
	 * Get the uncertainty node unit id.
	 * @return the uncertainty node unit id
	 */
	
	@XmlElement(name="unit_id")
	public int getUnitId() {
		return unit_id;
	}

	/**
	 * Set the uncertainty node unit id.
	 * @param the uncertainty node's unit id
	 */
	
	public void setUnitId(int unit_id) {
		this.unit_id = unit_id;
	}
	
	/**
	 * Get the uncertainty node description.
	 * @return node description  what the uncertainty node describes
	 */
	
	@XmlElement(name="node_description")
	public String getNodeDescription() {
		return node_description;
	}
	
	/**
	 * Set the uncertainty node description.
	 * @param node_description  what the uncertainty node describes
	 */
	
	public void setNodeDescription(String node_description) {
		this.node_description = node_description;
	}

	/**
	 * Return boolean about whether uncertainty node is of type spectrum
	 * @return is_spectrum  a boolean
	 */
	
	@XmlElement(name="is_spectrum")
	public Boolean getIsSpectrum() {
		return is_spectrum;
	}
	
	/**
	 * Set the boolean is_spectrum 
	 * @param is_spectrum  a boolean
	 */
	
	public void setIsSpectrum(Boolean is_spectrum) {
		this.is_spectrum = is_spectrum;
	}
	
	/** 
	 * Get the uncertainty node description 
	 * 
	 * @return uncertainty_node_description the uncertainty node description
	 */
	@XmlElement(name = "uncertainty_node_description")
	public String getUncertaintyNodeDescription() {
		
		return uncertainty_node_description;
	}
	
	/**
	 * Set the uncertainty node description
	 *
	 * @param uncertainty_node_description the uncertainty node description
	 */
	public void setUncertaintyNodeDescription(String uncertainty_node_description) {

		this.uncertainty_node_description = uncertainty_node_description;

	}
	
	/**
	 * Set the arraylist of type UncertaintySourcePair which is a set of source ids and source descriptions
	 * 
	 * @param uncertainty_source_pairs		an arraylist of type UncertaintySourcePair		
	 * 
	 */
	
	public void setUncertaintySourcePairs(ArrayList<UncertaintySourcePair> uncertainty_source_pairs) {
		
		this.uncertainty_source_pairs = uncertainty_source_pairs;
		
	}
	

	/**
	 * Getting uncertainty source pairs
	 * 
	 * @return uncertainty_source_pairs a list of type UncertaintySourcePair
	 * 
	 */
	
	@XmlElement(name = "uncertainty_source_pairs")
	public ArrayList<UncertaintySourcePair> getUncertaintySourcePairs() {
		
		return uncertainty_source_pairs;
	}
	
	/**
	 * Set the id and the edge value (source_link_description) of a linked uncertainty source
	 * 
	 * @param input_source_id		the uncertainty node id of the linked node 
	 * @param input_source_link_description	the description of how this source links to the current node			
	 * 
	 */
	public void add_uncertainty_source_by_id(int input_source_id, String input_source_link_description) {
		
		// Adding source information to uncertainty pair
		
		UncertaintySourcePair source_pair = new UncertaintySourcePair();
		
		source_pair.setSourceId(input_source_id);
		source_pair.setSourceLinkDescription(input_source_link_description);
		
		// adding this source pair to arraylist of source pairs
		
		uncertainty_source_pairs.add(source_pair);
		
	}
	
}
