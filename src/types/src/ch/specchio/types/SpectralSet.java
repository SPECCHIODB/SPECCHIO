package ch.specchio.types;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class represents a spectral set
 */

@XmlRootElement(name="spectral_set")
public class SpectralSet {

	@XmlElement public int instrument_node_id; 
	@XmlElement public String node_type;
	@XmlElement public Float[] u_vector;
	@XmlElement public double confidence_level;
	@XmlElement public String abs_rel;
	@XmlElement public int unit_id;
	@XmlElement public String node_description;
	
	//Elements which are specific to spectrum-level nodes
	@XmlElement public ArrayList<Integer> spectrum_ids;
	@XmlElement public String spectrum_set_description;
	
	// Elements which are specific to edge connections
	@XmlElement public int add_uncertainty_source_by_id;
	@XmlElement public String add_uncertainty_source;
	
	// To add to database specchio_client.insert_uncertainty_of_set(spectral_set)
	
	// Need to create new uncertainty set and then insert_new_uncertainty_node
	
	// specchio_client.create_new_uncertainty_set('name_of_set') returns id of new set
	
	//
	
	// A constructor
	public SpectralSet() {
		
	}
	
	/**
	 * Get the instrument node identifier for an instrument-level node
	 *
	 * @return the instrument node identifier
	 */
	@XmlElement(name="instrument_node_id")
	public int getInstrumentNodeId() {

		return instrument_node_id;

	}
	
	/**
	 * Set the instrument node identifier for an instrument-level node
	 *
	 * @param the instrument node identifier
	 */
	public void setInstrumentNodeId(int instrument_node_id) {

		this.instrument_node_id = instrument_node_id;

	}
	
	/**
	 * Get the uncertainty node node_type.
	 *
	 * @return node_type   either 'spectrum' or 'instrument'
	 */
	@XmlElement(name="node_type")
	public String getNodeType() {

		return node_type;

	}
	
	/**
	 * Set the uncertainty node node_type
	 *
	 * @param node_type  a either 'spectrum' or 'instrument'
	 */
	public void setNodeType(String node_type) {

		this.node_type = node_type;

	}
	
	/**
	 * Get the instrument node confidence level % from 0 to 1.
	 *
	 * @return confidence_level   Percentage from 0 to 1
	 */
	@XmlElement(name="confidence_level")
	public double getConfidenceLevel() {

		return confidence_level;

	}
	
	/**
	 * Set the instrument node confidence level % from 0 to 1.
	 *
	 * @param confidence_level   Percentage from 0 to 1
	 */
	public void setConfidenceLevel(double confidence_level) {

		this.confidence_level = confidence_level;

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
	 * Set the instrument node uncertainty vector.
	 *
	 * @param u_vector
	 */
	public void setUncertaintyVector(Float[] u_vector) {
		this.u_vector = u_vector;
	}
	
	/**
	 * Get whether the node's uncertainty is absolute or relative.
	 *
	 * @return abs_rel   absolute or relative uncertainty 
	 */
	@XmlElement(name="abs_rel")
	public String getAbsRel() {

		return abs_rel;

	}
	
	/**
	 * Set whether the node's uncertainty is absolute or relative.
	 *
	 * @param abs_rel   absolute or relative uncertainty 
	 */
	public void setAbsRel(String abs_rel) {

		this.abs_rel = abs_rel;

	}
	
	/**
	 * Get the uncertainty node unit id.
	 *
	 * @return the uncertainty node unit id
	 */
	@XmlElement(name="unit_id")
	public int getUnitId() {

		return unit_id;

	}

	/**
	 * Set the uncertainty node unit id.
	 *
	 * @param the uncertainty node's unit id
	 */
	public void setUnitId(int unit_id) {

		this.unit_id = unit_id;

	}
	
	/**
	 * Get the uncertainty node description.
	 *
	 * @return node description  what the uncertainty node describes
	 */
	@XmlElement(name="node_description")
	public String getNodeDescription() {

		return node_description;

	}
	
	/**
	 * Set the uncertainty node description.
	 *
	 * @param node_description  what the uncertainty node describes
	 */
	public void setNodeDescription(String node_description) {

		this.node_description = node_description;

	}
	
	/**
	 * Get the spectrum ids for a spectrum level uncertainty node
	 * 
	 * @param spectrum_ids a list of spectrum ids
	 */
	@XmlElement(name="spectrum_ids")
	public ArrayList<Integer> getSpectrumIds() {
		return spectrum_ids;
	}
	
	/**
	 * Set the spectrum ids for a spectrum level uncertainty node
	 * 
	 * @param spectrum_ids a list of spectrum ids
	 */
	public void setSpectrumIds(ArrayList<Integer> spectrum_ids) {
		
		this.spectrum_ids = spectrum_ids;
	}
	
	/**
	 * Get the uncertainty node node_type.
	 *
	 * @return node_type   either 'spectrum' or 'instrument'
	 */
	@XmlElement(name="spectrum_set_description")
	public String getSpectrumSetDescription() {

		return spectrum_set_description;

	}
	
	/**
	 * Set the uncertainty node node_type
	 *
	 * @param node_type  a either 'spectrum' or 'instrument'
	 */
	public void setSpectrumSetDescription(String spectrum_set_description) {

		this.spectrum_set_description = spectrum_set_description;

	}
	
	
	
}
