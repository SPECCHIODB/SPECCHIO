package ch.specchio.types;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ch.specchio.jaxb.XmlMapAdapter;

/**
 * This class represents a table of metadata categories. It is basically
 * a wrapper around Hashtable.
 */
@XmlRootElement(name="category_table")
public class CategoryTable {
	
	private Hashtable<Integer, String> table;
	
	public CategoryTable() {
		this.table = new Hashtable<Integer, String>();
	}
	
	public CategoryTable(Hashtable<Integer, String> table) {
		this.table = table;
	}
	
	@XmlElement(name="table")
	@XmlJavaTypeAdapter(XmlMapAdapter.class)
	public Hashtable<Integer, String> getHashtable() { return this.table; }
	public void setHashtable(Hashtable<Integer, String> table) { this.table = table; }
	
	public String get(int id) { return this.table.get(id); }
	public Enumeration<Integer> keys() { return this.table.keys(); }
	public void put(int id, String name) { this.table.put(id, name); }
	public void remove(int id) { this.table.remove(id); }

}
