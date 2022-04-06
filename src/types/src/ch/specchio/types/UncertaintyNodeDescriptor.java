package ch.specchio.types;

import java.util.ArrayList;
import javax.xml.bind.annotation.*;

/**
 * This class represents arraylists for spectralset and the spectralset itself, used for
 * requests to the insertUncertaintyNode service.
 */
@XmlRootElement(name="uc_node_descriptor")

public class UncertaintyNodeDescriptor {

	private UncertaintyNode uc_node;
	private int uc_set_id;
	
	/** mandatory to have no arg default constructor */
	public UncertaintyNodeDescriptor() {
		
	}
	
	public UncertaintyNodeDescriptor(UncertaintyNode uc_node, int uc_set_id) {
		
		this.uc_node = uc_node;
		this.uc_set_id = uc_set_id;
	
	}
	
	@XmlElement(name="uc_node")
	public UncertaintyNode getUcNode() { return this.uc_node; }
	public void setUcNode(UncertaintyNode uc_node) { this.uc_node = uc_node; }
	
	@XmlElement(name="uc_set_id")
	public int getUcSetId() { return this.uc_set_id; }
	public void setUcSetId(int uc_set_id) { this.uc_set_id = uc_set_id; }
	
	
}
