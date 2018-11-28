package ch.specchio.types;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ch.specchio.jaxb.XmlMapAdapter;

/**
 * This class represents a table of taxonomy entries. It is basically
 * a wrapper around Hashtable.
 */
@XmlRootElement(name="taxonomy")
public class Taxonomy {
	
	private Hashtable<String,Integer> table;
	private attribute attribute;
	
	public Taxonomy() {
		this.table = new Hashtable<String,Integer>(); 
	}	
	
	public Taxonomy(attribute attribute) {
		this.table = new Hashtable<String,Integer>(); 
		this.setAttribute(attribute);
	}
	
	@XmlElement(name="table")
	@XmlJavaTypeAdapter(XmlMapAdapter.class)
	public Hashtable<String,Integer> getHashtable() { return this.table; }
	public void setHashtable(Hashtable<String,Integer> table) { this.table = table; }
	
	public Integer get(String name) { return this.table.get(name); }
	public Enumeration<String> keys() { return this.table.keys(); }
	public void put(String name, int id) { this.table.put(name, id); }
	public void remove(String name) { this.table.remove(name); }

	@XmlElement(name="attribute")
	public attribute getAttribute() {
		return attribute;
	}
	public void setAttribute(attribute attribute) {
		this.attribute = attribute;
	}
	

}
