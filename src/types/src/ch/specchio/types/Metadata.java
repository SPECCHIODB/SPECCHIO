package ch.specchio.types;

import java.util.ArrayList;
import java.util.ListIterator;

import javax.xml.bind.annotation.*;

@XmlRootElement(name="metadata")
public class Metadata {
	
	protected String filename;
	public int directory_id;
	private Integer frame_id;
	
	protected int non_redundant_cnt = 0;
	
	private ArrayList<MetaParameter> entries;
	private ArrayList<Integer> entry_ids;
	public ArrayList<String> insert_value_strings;
	public ArrayList<Integer> redundancy_reduced_eav_ids;
	
	
	public Metadata()
	{
		this.entries = new ArrayList<MetaParameter>();
		entry_ids = new ArrayList<Integer>();
	}
	
	@XmlElement(name="frame_id")
	public Integer getFrameId() { return this.frame_id; }
	public void setFrameId(Integer frame_id) { this.frame_id = frame_id; }
	
	@XmlElement(name="entries")
	public ArrayList<MetaParameter> getEntries() { return this.entries; }
	public void setEntries(ArrayList<MetaParameter> entries) { this.entries = entries; }
	public MetaParameter getEntry(int i) { return this.entries.get(i); }
	public void addEntry(MetaParameter entry) { this.entries.add(entry); }
	
	@XmlElement(name="entry_ids")
	public ArrayList<Integer> getEntryIds() { return this.entry_ids; }
	public void setEntryIds(ArrayList<Integer> entry_ids) { this.entry_ids = entry_ids; }
	public Integer getEntryId(int i) { return this.entry_ids.get(i); }
	public void addEntryId(Integer entry_id) { this.entry_ids.add(entry_id); }

	
	// clears the DB ids of all parameters, which causes them to re-insert
	public void clear_parameter_ids()
	{
		
	}
	
	
	
	public String get_all_metadata_as_text()
	{
		String out = "";
		
		ListIterator<MetaParameter> li = entries.listIterator();
		
		while(li.hasNext())
		{
			MetaParameter e =  li.next();
			out = out + e.getAttributeName() + "\t:" + e.getValue().toString() + " \t (" + e.getUnitName() + ")\n";
		}

		
		return out;
		
	}
	
	
	
	public Integer get_hierarchy_id()
	{
		return directory_id;		
	}
	
	public Integer get_primary_resource_id()
	{
		return frame_id;
	}
	
	
	public void remove_entry(String name)
	{
		
		ListIterator<MetaParameter> li = entries.listIterator();
		
		boolean done = false;	
		
		while(li.hasNext() && !done)
		{
			MetaParameter m = li.next();
			
			if (m.getAttributeName().equals(name)) 
			{
				done = true;
				
				entries.remove(m);
				
			}
		}		
			
	}
	
	
	
	public ArrayList<MetaParameter> get_entries_of_category(String category_name)
	{
		ArrayList<MetaParameter> matching_entries = new ArrayList<MetaParameter>();
				
		ListIterator<MetaParameter> li = entries.listIterator();
		
		while(li.hasNext())
		{
			MetaParameter m = li.next();
			
			if (m.getCategoryName().equals(category_name))
			{
				matching_entries.add(m);
			}

		}
		
		return matching_entries;
	}
	
	
	public MetaParameter get_first_entry(String name)
	{
		
		MetaParameter mp = null;
		
		ListIterator<MetaParameter> li = entries.listIterator();
		
		boolean done = false;
	
		
		while(li.hasNext() && !done)
		{
			MetaParameter m = li.next();
			
			if (m.getAttributeName().equals(name)) 
			{
				done = true;
				mp = m;
			}

		}
		
		return mp;		
			
	}

	public MetaParameter get_first_entry(Integer attribute_id)
	{
		MetaParameter mp = null;
		
		ListIterator<MetaParameter> li = entries.listIterator();
		
		boolean done = false;
	
		
		while(li.hasNext() && !done)
		{
			MetaParameter m = li.next();
				
			if (m.getAttributeId().equals(attribute_id)) 
			{
				done = true;
				mp = m;
			}

		}
		
		return mp;
			
	}
	
	
	public MetaParameter get_first_entry(Integer attribute_id, Integer unit_id)
	{
		MetaParameter mp = null;
		MetaParameter tmp = null;
		
		ListIterator<MetaParameter> li = entries.listIterator();
		
		boolean done = false;
	
		
		while(li.hasNext() && !done)
		{
			tmp = li.next();
				
			if (tmp.getAttributeId().equals(attribute_id) && tmp.getUnitId().compareTo(unit_id) == 0 ) 
			{
				done = true;
				mp = tmp;
			}

		}
		
		return mp;
			
	}
	
	public ArrayList<MetaParameter> get_all_entries(Integer attribute_id) {
		
		ArrayList<MetaParameter> matches = new ArrayList<MetaParameter>();
		for (MetaParameter mp : entries) {
			if (mp.getAttributeId().equals(attribute_id)) {
				matches.add(mp);
			}
		}
		
		return matches;
		
	}
	
	public ArrayList<MetaParameter> get_all_entries(String name) {
		
		ArrayList<MetaParameter> matches = new ArrayList<MetaParameter>();
		for (MetaParameter mp : entries) {
			if (mp.getAttributeName().equals(name)) {
				matches.add(mp);
			}
		}
		
		return matches;
		
	}
	
	
	public void add_entry(MetaParameter mp)
	{
		entries.add(mp);
	}


}
