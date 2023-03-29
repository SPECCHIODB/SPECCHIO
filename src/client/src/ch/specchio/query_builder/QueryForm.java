package ch.specchio.query_builder;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.ListIterator;

import ch.specchio.queries.EAVQueryConditionObject;
import ch.specchio.queries.QueryCondition;
import ch.specchio.queries.SpectrumQueryCondition;


public class QueryForm {
	
	ArrayList<QueryCategoryContainer> containers = new ArrayList<QueryCategoryContainer>();
	Hashtable<String, QueryCategoryContainer> containers_hash = new Hashtable<String, QueryCategoryContainer>();

	public QueryForm() {
	}

	public QueryCategoryContainer addCategoryContainer(String category) {
	
		QueryCategoryContainer cc = new QueryCategoryContainer(category);
		
		addCategoryContainer(cc);
		
		return cc;		
		
	}
	
	public void addCategoryContainer(QueryCategoryContainer cc)
	{
		containers.add(cc);
		containers_hash.put(cc.category_name, cc);		
	}
	
	public void textReport()
	{
		// loop over containers
		ListIterator<QueryCategoryContainer> li = containers.listIterator();
		
		while(li.hasNext())
		{
			QueryCategoryContainer cc = li.next();

			cc.textReport();	
		}
		
	}

	public ArrayList<QueryCategoryContainer> getCategoryContainers() {
		return containers;
	}
	
	
	public QueryCategoryContainer getCategoryContainer(String name) {
		return containers_hash.get(name);
	}	
	
	
	public ArrayList<QueryCondition> getListOfConditions()
	{
		ArrayList<QueryCondition> conds = new  ArrayList<QueryCondition>();
		
		// loop over containers
		ListIterator<QueryCategoryContainer> li = containers.listIterator();
		
		while(li.hasNext())
		{
			EAVQueryConditionObject cond = null;
			QueryCategoryContainer cc = li.next();
			
			ArrayList<QueryField> fields = cc.getFields();
			
			ListIterator<QueryField> field_li = fields.listIterator();
			
			while(field_li.hasNext())
			{
				QueryField field = field_li.next();
				
				// add condition if not empty
				if(field.isSet())
				{
					
					// create new condition
					if(field instanceof EAVQueryField)
					{	
						cond = new EAVQueryConditionObject("eav", "spectrum_x_eav", field.getLabel(), field.get_fieldname());
						if (field.getNative_value() instanceof Integer) {
							cond.setIntegerValue((Integer) field.getNative_value()); // native value to allow more flexibility in future ...
						}
						else {
							cond.setValue(field.getValue());
						}
						cond.setOperator(field.getOperator());
					}
					else
					{
						cond = new SpectrumQueryCondition("spectrum", field.get_fieldname() + "_id");
						cond.setValue(field.getValue());
						
					}	
					
					conds.add(cond);					
				}
			}
			
		}
		
		return conds;
	}

	public void addFieldToContainer(QueryCategoryContainer c,
			SpectrumQueryField spectrum_field) {
		
		c.addField(spectrum_field);
		
	}
	
	public void clearSetFields() {
		// loop over containers
		ListIterator<QueryCategoryContainer> li = containers.listIterator();
		
		while(li.hasNext())
		{
			QueryCategoryContainer cc = li.next();

			cc.clearSetFields();
		}		
	}

}
