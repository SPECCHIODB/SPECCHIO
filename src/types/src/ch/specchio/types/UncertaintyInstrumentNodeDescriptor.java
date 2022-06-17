package ch.specchio.types;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class represents arraylists for uncertainty instrument node and the uncertainty instrument node itself, used for
 * requests to the insertUncertaintyNode service.
 */
@XmlRootElement(name="uc_instrument_node_descriptor")
public class UncertaintyInstrumentNodeDescriptor {
	
	private UncertaintyInstrumentNode uc_instrument_node;
	private int uc_set_id;
	
	/** mandatory to have no arg default constructor */
	public UncertaintyInstrumentNodeDescriptor() {
		
	}
	
	public UncertaintyInstrumentNodeDescriptor(UncertaintyInstrumentNode uc_instrument_node, int uc_set_id) {
		
		System.out.println("UncertaintyInstrumentNodeDescriptor constructor");
		
		this.uc_instrument_node = uc_instrument_node;
		this.uc_set_id = uc_set_id;
		
	}
	
	@XmlElement(name="uc_instrument_node")
	public UncertaintyInstrumentNode getUcInstrumentNode() { return this.uc_instrument_node; }
	public void setUcInstrumentNode(UncertaintyInstrumentNode uc_instrument_node) { this.uc_instrument_node = uc_instrument_node; }
	
	@XmlElement(name="uc_set_id")
	public int getUcSetId() { return this.uc_set_id; }
	public void setUcSetId(int uc_set_id) { this.uc_set_id = uc_set_id; }

}
