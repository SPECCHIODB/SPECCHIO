package ch.specchio.types;

import java.util.Hashtable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ch.specchio.jaxb.XmlMapAdapter;

/**
 * This class represents a table of metadata categories per application domain. It is basically
 * a wrapper around Hashtable.
 */
@XmlRootElement(name="application_domain_categories_table")
public class ApplicationDomainCategories {

	// a table with taxonomy_ids (per application domain) as keys with a list of category ids as value
	//private Hashtable<Integer,Integer[]> table;
	
	int taxonomy_id;
	Integer[] category_ids;
	
	public int getTaxonomy_id() {
		return taxonomy_id;
	}
	public void setTaxonomy_id(int taxonomy_id) {
		this.taxonomy_id = taxonomy_id;
	}
	public Integer[] getCategory_ids() {
		return category_ids;
	}
	public void setCategory_ids(Integer[] category_ids) {
		this.category_ids = category_ids;
	}
	
	
//	public ApplicationDomainCategories() {
//		this.table = new Hashtable<Integer, Integer[]>();
//	}
//	
//	@XmlElement(name="table")
//	@XmlJavaTypeAdapter(XmlMapAdapter.class)
//	public Hashtable<Integer,Integer[]> getHashtable() { return this.table; }
//	public void setHashtable(Hashtable<Integer,Integer[]> table) { this.table = table; }
//	
//	public Integer[] get(int id) { return this.table.get(id); }
//	public void put(int id, Integer[] category_ids) { this.table.put(id, category_ids); }

}
