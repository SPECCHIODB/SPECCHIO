package ch.specchio.types;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class represents arraylists for uncertainty spectrum node and the uncertainty spectrum node itself, used for
 * requests to the insertUncertaintyNode service.
 */
@XmlRootElement(name="uc_spectrum_node_descriptor")

public class UncertaintySpectrumNodeDescriptor {
	
	private UncertaintySpectrumNode uc_spectrum_node;
	private int uc_set_id;
	private ArrayList<Integer> uc_spectrum_subset_ids;
	private ArrayList<Integer> uc_spectrum_ids;
	
	/** mandatory to have no arg default constructor */
	public UncertaintySpectrumNodeDescriptor() {
		this.uc_spectrum_subset_ids = new ArrayList<Integer>();
		this.uc_spectrum_ids = new ArrayList<Integer>();
	}
	
	public UncertaintySpectrumNodeDescriptor(UncertaintySpectrumNode uc_spectrum_node, int uc_set_id) {
		
		this.uc_spectrum_node = uc_spectrum_node;
		this.uc_set_id = uc_set_id;
		
	}
	
	public UncertaintySpectrumNodeDescriptor(UncertaintySpectrumNode uc_spectrum_node, int uc_set_id,ArrayList<Integer> uc_spectrum_ids, ArrayList<Integer> uc_spectrum_subset_ids) {
		
		this.uc_spectrum_node = uc_spectrum_node;
		this.uc_set_id = uc_set_id;
		this.uc_spectrum_ids = uc_spectrum_ids;
		this.uc_spectrum_subset_ids = uc_spectrum_subset_ids;
		
	}
	
	public UncertaintySpectrumNodeDescriptor(UncertaintySpectrumNode uc_spectrum_node, ArrayList<Integer> uc_spectrum_ids) {
		
		this.uc_spectrum_node = uc_spectrum_node;
		this.uc_spectrum_ids = uc_spectrum_ids;
	
	}
	
	@XmlElement(name="uc_spectrum_node")
	public UncertaintySpectrumNode getUcSpectrumNode() { return this.uc_spectrum_node; }
	public void setUcSpectrumNode(UncertaintySpectrumNode uc_spectrum_node) { this.uc_spectrum_node = uc_spectrum_node; }
	
	@XmlElement(name="uc_set_id")
	public int getUcSetId() { return this.uc_set_id; }
	public void setUcSetId(int uc_set_id) { this.uc_set_id = uc_set_id; }

	@XmlElement(name="uc_spectrum_subset_ids")
	public ArrayList<Integer> getUcSpectrumSubsetIds() { return this.uc_spectrum_subset_ids; }
	public void setUcSpectrumSubsetIds(ArrayList<Integer> uc_spectrum_subset_ids) { this.uc_spectrum_subset_ids = uc_spectrum_subset_ids; }
	
	@XmlElement(name="uc_spectrum_ids")
	public ArrayList<Integer> getUcSpectrumIds() { return this.uc_spectrum_ids; }
	public void setUcSpectrumIds(ArrayList<Integer> uc_spectrum_ids) { this.uc_spectrum_ids = uc_spectrum_ids; }

}
