package ch.specchio.types;

import java.util.ArrayList;

import javax.xml.bind.annotation.*;


/**
 * This class describes selection to spectrum metadata, for use with the metadata services.
 */
@XmlRootElement(name="metadata_selection")
public class MetadataSelectionDescriptor {
	
	/** the identifiers of the objects to be updated */
	private ArrayList<Integer> ids;
	private String attribute_name;
	private Integer attribute_id;
	private Object value;
	private ArrayList<Integer> attribute_ids;
	private Boolean distinct = true;
	private int level = MetaParameter.SPECTRUM_LEVEL; // default value
	private int target_hierarchy_id;
	
	/**
	 * Default Constructor.
	 */
	public MetadataSelectionDescriptor() {
		
		this.ids = new ArrayList<Integer>();
		this.attribute_name = "";
		attribute_id = 0;
	}	
	
	/**
	 * Constructor.
	 */
	public MetadataSelectionDescriptor(ArrayList<Integer> ids, String attribute_name) {
		
		this.ids = ids;
		this.attribute_name = attribute_name;
		
	}

	public MetadataSelectionDescriptor(ArrayList<Integer> ids, int target_hierarchy_id){
		this.ids = ids;
		this.target_hierarchy_id = target_hierarchy_id;
	}
	
	public MetadataSelectionDescriptor(ArrayList<Integer> ids, String attribute_name, Object value) {
		
		this.ids = ids;
		this.attribute_name = attribute_name;
		this.value = value;		
	}	
	
	public MetadataSelectionDescriptor(ArrayList<Integer> ids, Integer attribute_id) {
		
		this.ids = ids;
		this.attribute_id = attribute_id;
		
	}
	
	public MetadataSelectionDescriptor(ArrayList<Integer> ids, ArrayList<Integer> attribute_ids) {
		
		this.ids = ids;
		this.attribute_ids = attribute_ids;
		
	}	


	@XmlElement(name = "target_hierarchy_id")
	public int getTarget_hierarchy_id(){return this.target_hierarchy_id;}
	public void setTarget_hierarchy_id(int target_hierarchy_id){this.target_hierarchy_id = target_hierarchy_id;}
	
	@XmlElement(name="ids")
	public ArrayList<Integer> getIds() { return this.ids; }
	public void setIds(ArrayList<Integer> ids) { this.ids = ids; }
	
	@XmlElement(name="attribute_name")
	public String getAttributeName() { return this.attribute_name; }
	public void setAttributeName(String attribute_name) { this.attribute_name = attribute_name; }

	@XmlElement(name="attribute_id")
	public Integer getAttribute_id() {
		return attribute_id;
	}

	public void setAttribute_id(Integer attribute_id) {
		this.attribute_id = attribute_id;
	}

	@XmlElement(name="attribute_ids")
	public ArrayList<Integer> getAttribute_ids() {
		return attribute_ids;
	}

	public void setAttribute_ids(ArrayList<Integer> attribute_ids) {
		this.attribute_ids = attribute_ids;
	}	
	
	@XmlElement(name="level")
	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}		
	
	@XmlElement(name="value")
	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	@XmlElement(name="distinct")
	public Boolean getDistinct() {
		return distinct;
	}

	public void setDistinct(Boolean distinct) {
		this.distinct = distinct;
	}
	

}
