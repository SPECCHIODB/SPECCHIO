package ch.specchio.query_builder;

import java.util.ArrayList;
import java.util.ListIterator;

import ch.specchio.types.attribute;


public class QueryCategoryContainer {
	
	String category_name;
	ArrayList<QueryField> fields = new ArrayList<QueryField>();

	public QueryCategoryContainer(String category) {
		this.category_name = category;
	}

	
	public String getCategoryName()
	{
		return this.category_name;
	}


	public void addFields(attribute[] attrs) {
		
		for (attribute attr : attrs)
		{
			
			EAVQueryField eav_qc = new EAVQueryField(attr.getName(), attr.default_storage_field);

			this.fields.add(eav_qc);
			
			// automatically add a further condition for the higher value and set the operations for both
			if("int_val".equals(eav_qc.get_fieldname()) || "double_val".equals(eav_qc.get_fieldname()) || "datetime_val".equals(eav_qc.get_fieldname()))
			{
				eav_qc.set_operator(">=");
							
				EAVQueryField upper_eav_qc = new EAVQueryField(attr.getName(), attr.default_storage_field);
				upper_eav_qc.set_operator("<=");
				
				// links to upper and lower conditions
				upper_eav_qc.putClientProperty("lower_bounds_cond", eav_qc);
				eav_qc.putClientProperty("upper_bounds_cond", upper_eav_qc);
				
				
				this.fields.add(upper_eav_qc);
			}		
			else if("string_val".equals(eav_qc.get_fieldname()))
			{
				eav_qc.set_operator("like");
			}
			else if("spatial_val".equals(eav_qc.get_fieldname()))
			{
				eav_qc.set_operator("MBRContains");
				

				// Optionally create a single query interface for all geometries 
				
				
			}
			else
			{
				// unhandled data type for queries
				boolean gotcha =true;
				
			}
			
		}
		
		
	}
	
	public void textReport()
	{
		
		ListIterator<QueryField> li = fields.listIterator();
		
		System.out.println("");
		System.out.println("Category: " + this.category_name);
		System.out.println("----------------------------");
		
		while(li.hasNext())
		{			
			li.next().textReport();
					
		}		
		
		System.out.println("----------------------------");
		System.out.println("");
		
	}


	public ArrayList<QueryField> getFields() {
		return fields;
	}


	public void addField(SpectrumQueryField spectrum_field) {
		this.fields.add(spectrum_field);
	}


	public void clearSetFields() {
		for(QueryField field : fields)
		{
			field.set_value("");
		}
		
	}



	
}
