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
	@XmlElement public int spectrum_set_id;
	@XmlElement public Float[][] u_vectors;
	
	// Elements specific to spectrum subsets
	@XmlElement public int spectrum_subset_id;
	@XmlElement public ArrayList<Integer> spectrum_subset_ids;
	
	// Elements specific to a single edge connection
	@XmlElement public int uncertainty_source_id; //an uncertainty node id
	@XmlElement public String source_link_description; //description of how source is linked to node

	// Elements specific to a group of edge connections
    public ArrayList<UncertaintySourcePair> uncertainty_source_pairs;
    public ArrayList<Integer> uncertainty_source_ids;
	
	// Elements which are specific to groupings
	@XmlElement public int uncertainty_set_id;
	@XmlElement public String uncertainty_set_description;
	 
	@XmlElement public int uncertainty_node_id;
	@XmlElement public int node_set_id;
	
	// A constructor
	public SpectralSet() {
		
		this.spectrum_ids = new ArrayList<Integer>();
		this.uncertainty_source_pairs = new ArrayList<UncertaintySourcePair>();
		this.uncertainty_source_ids = new ArrayList<Integer>();
		
		UncertaintySourcePair source_pair = new UncertaintySourcePair();
		
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
	 * Get the uncertainty node id - this is where we standardise the instrument/spectrum nodes
	 * 
	 * @return uncertainty_node_id
	 */
	@XmlElement(name = "uncertainty_node_id")
	public int getUncertaintyNodeId() {
		
		return uncertainty_node_id;
	}
	
	/**
	 * Set the uncertainty node id
	 *
	 * @param uncertainty_node_id  
	 */
	public void setUncertaintyNodeId(int uncertainty_node_id) {

		this.uncertainty_node_id = uncertainty_node_id;

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
	 * Get the instrument node uncertainty vector
	 *
	 * @return u_vectors
	 */

	@XmlElement(name="u_vectors")
	public Float[][] getUncertaintyVectors() {
		return u_vectors;
	}
	
	
	/**
	 * Set the instrument node uncertainty vector.
	 *
	 * @param u_vectors
	 */
	public void setUncertaintyVectors(Float[][] u_vectors) {
		this.u_vectors = u_vectors;
	}
	
	/** 
	 * Get the uncertainty source id - an uncertainty node id
	 * 
	 * @return uncertainty_source_id the uncertainty node id of the source
	 */
	@XmlElement(name = "uncertainty_source_id")
	public int getUncertaintySourceId() {
		
		return uncertainty_source_id;
	}
	
	/**
	 * Get the source link description ie. edge value
	 * 
	 * @return source_link_description the description of how the source links to current node
	 */
	@XmlElement(name = "source_link_description")
	public String getSourceLinkDescription() {
		
		return source_link_description;
	}
	
	
	
	/**
	 * Set the id of a linked uncertainty source
	 *
	 * @param input_source_id  the uncertainty node id of the linked node
	 */
	public void add_uncertainty_source_by_id(int input_source_id) {
		
		// Adding source information to uncertainty pair
		
		UncertaintySourcePair source_pair = new UncertaintySourcePair();
		
		source_pair.setSourceId(input_source_id); 
		
		// adding this source pair to arraylist of source pairs
		
		uncertainty_source_pairs.add(source_pair);

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
	 * Set the id of a linked uncertainty source
	 *
	 * @param input_source_id  the uncertainty node id of the linked node
	 */
	public void add_uncertainty_source_id_only(int input_source_id) {
		
		this.uncertainty_source_ids.add(input_source_id);


	}

	/** 
	 * 
	 * Set the arraylist of all uncertainty source ids
	 * 
	 * @param uncertainty_source_ids an arraylist of integers of uncertainty source ids
	 * 
	 */
	
	public void setUncertaintySourceIds(ArrayList<Integer> uncertainty_source_ids) {
		
		this.uncertainty_source_ids = uncertainty_source_ids;
		
	}
	
	
	/**
	 * Getting uncertainty source ids
	 * 
	 * @return uncertainty_source_ids a list of type integer
	 * 
	 */
	
	@XmlElement(name = "uncertainty_source_ids")
	public ArrayList<Integer> getUncertaintySourceIds() {
		
		return uncertainty_source_ids;
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
	 * Get the spectrum subset ids for a spectrum level uncertainty node
	 * 
	 * @param spectrum_subset_ids a list of spectrum subset ids
	 */
	@XmlElement(name="spectrum_subset_ids")
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
