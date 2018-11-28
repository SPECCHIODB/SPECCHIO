package ch.specchio.types;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ch.specchio.jaxb.XmlMapAdapter;

/**
 * This class represents a table of metadata conflicts. It is basically
 * a wrapper around Hashtable.
 */
@XmlRootElement(name="conflict_table")
@XmlSeeAlso(ConflictInfo.class)
public class ConflictTable {
	
	/** mapping of field names and attribute ids to conflict information structures */
	private Hashtable<String, ConflictInfo> table;
	
	public ConflictTable() { this.table = new Hashtable<String, ConflictInfo>(); }
	
	
	@XmlElement(name="table")
	@XmlJavaTypeAdapter(XmlMapAdapter.class) 
	public Hashtable<String, ConflictInfo> getHashtable() { return this.table; }
	public void setHashtable(Hashtable<String, ConflictInfo> table) { this.table = table; }
	
	public ConflictInfo get(String fieldname) { return this.table.get(fieldname); }
	public void put(String fieldname, ConflictInfo info) { this.table.put(fieldname, info); }
	
	public ConflictInfo get(int attribute_id) { return this.table.get(Integer.toString(attribute_id)); }
	public void put(int attribute_id, ConflictInfo info) { this.table.put(Integer.toString(attribute_id), info); }
	
	public Enumeration<String> conflicts() { return this.table.keys(); }
	
	public void addConflictTable(ConflictTable in)
	{
		
		Enumeration<String> input_keys = in.conflicts();
		
		while(input_keys.hasMoreElements())
		{
			String key = input_keys.nextElement();
			
			ConflictInfo entry = table.get(key);
			
			if(entry == null)
			{
				// no such entry yet
				table.put(key, in.get(key));
			}
			else
			{
				// conflict infos must be combined
				ConflictInfo input_entry = in.get(key);
				
				Hashtable<Integer, ConflictStruct> in_conf_structs = input_entry.getConflictStructs();
				Enumeration<Integer> in_eav_keys = in_conf_structs.keys();
				
				while(in_eav_keys.hasMoreElements())
				{
					Integer in_eav_key = in_eav_keys.nextElement();
					entry.addConflict(in_eav_key, in_conf_structs.get(in_eav_key));
				}
				
			}
			
			
		}
		
		
		
	}

}
