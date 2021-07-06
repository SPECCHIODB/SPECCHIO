package ch.specchio.types;

import javax.xml.bind.annotation.XmlRootElement;

//Imports will go here
import javax.xml.bind.annotation.XmlElement;

/**
 * This class represents an instrument node.
 */

@XmlRootElement(name="instrument_node")
public class InstrumentNode {
	
	//Starting really simple. Just attributes for this class
	
	@XmlElement public int instrument_node_id;
	@XmlElement public String node_type;
	@XmlElement public Float[] u_vector;
	@XmlElement public double confidence_level;
	@XmlElement public String abs_rel;
	@XmlElement public int unit_id;
	
	public InstrumentNode() {
		unit_id = 1;
	}
	
	
	/**
	 * Get the instrument node identifier.
	 *
	 * @return the instrument node identifier
	 */
	@XmlElement(name="instrument_node_id")
	public int getId() {

		return instrument_node_id;

	}


	/**
	 * Set the instrument node identifier.
	 *
	 * @param instrument_node_id  identifier
	 */
	public void setId(int instrument_node_id) {

		this.instrument_node_id = instrument_node_id;

	}
	
	/**
	 * Get the instrument node node_type.
	 *
	 * @return node_type   a description of the instrument node
	 */
	@XmlElement(name="node_type")
	public String getNodeType() {

		return node_type;

	}
	
	/**
	 * Set the instrument node node_type.
	 *
	 * @param node_type  a description of the instrument node
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
	 * Get whether the instrument node uncertainty is absolute or relative.
	 *
	 * @return abs_rel   absolute or relative uncertainty 
	 */
	@XmlElement(name="abs_rel")
	public String getAbsRel() {

		return abs_rel;

	}
	
	/**
	 * Set whether the instrument node uncertainty is absolute or relative.
	 *
	 * @param abs_rel   absolute or relative uncertainty 
	 */
	public void setAbsRel(String abs_rel) {

		this.abs_rel = abs_rel;

	}
	
	/**
	 * Get the instrument node unit id.
	 *
	 * @return the instrument node unit id
	 */
	@XmlElement(name="unit_id")
	public int getUnitId() {

		return unit_id;

	}


	/**
	 * Set the instrument node unit id.
	 *
	 * @param instrument_node_id  unit id
	 */
	public void setUnitId(int unit_id) {

		this.unit_id = unit_id;

	}
	
	
	
	
}