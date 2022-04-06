package ch.specchio.types;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class represents an instrument uncertainty node
 */
public class UncertaintyInstrumentNode extends UncertaintyNode {
	
	@XmlElement public int instrument_node_id; 
	
	public UncertaintyInstrumentNode() {
		
		node_type = "instrument";
		
	}
	
	public UncertaintyInstrumentNode(UncertaintyNode uc_node) {
		
		this.node_type = uc_node.node_type;
		this.u_vector = uc_node.u_vector;
		this.confidence_level = uc_node.confidence_level;
		this.abs_rel = uc_node.abs_rel;
		this.unit_id = uc_node.unit_id;
		this.node_description = uc_node.node_description;
		
	}

	/** 
	 * Get the instrument node id
	 * @return instrument_node_id
	 */
	
	@XmlElement(name = "instrument_node_id")
	public int getInstrumentNodeId() {
		return instrument_node_id;
	}
	
	/**
	 * Set the instrument node id
	 * @param instrument_node_id  
	 */
	
	public void setInstrumentNodeId(int instrument_node_id) {
		this.instrument_node_id = instrument_node_id;
	}
	
	
	
}
