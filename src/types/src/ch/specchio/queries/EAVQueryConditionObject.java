package ch.specchio.queries;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.*;

import ch.specchio.types.attribute;


@XmlRootElement(name="eav_query_condition")
public class EAVQueryConditionObject extends QueryCondition {
	
	protected String tablename;
	protected String table_alias;
	protected String ref_tablename;
	protected String ref_table_alias;	
	protected String fieldname;
	protected String attribute_name;
	protected Object value = null;
	protected String operator = "=";
	
	@XmlElement(name="quote_value")
	protected boolean quote_value = true;
	
	protected boolean condition_handled_at_hierarchy_level = false;
	protected boolean condition_handled_at_spectrum_level = false;
	
	private Integer integerValue = null;
	private String stringValue = null;
	private Object[] arrayValue = null;
	private List<?> listValue = null;
	
	public EAVQueryConditionObject()
	{
		
	}
	
	public EAVQueryConditionObject(String tablename, String fieldname)
	{
		this.tablename = tablename;
		this.fieldname = fieldname;
	}
	
	public EAVQueryConditionObject(attribute attr)
	{
		this("eav", attr.getDefaultStorageField());
		
		this.attribute_name = attr.getName();
		this.ref_tablename = "spectrum_x_eav"; // obsolete
	}
	
	
	
	public EAVQueryConditionObject(String tablename, String ref_tablename, String attribute_name, String fieldname)
	{
		this(tablename, fieldname);
		
		this.attribute_name = attribute_name;
		this.ref_tablename = ref_tablename;
	}
	
	
	@XmlElement(name="attribute_name")
	public String getAttributeName() { return this.attribute_name; }
	public void setAttributeName(String attribute_name) { this.attribute_name = attribute_name; }
	
	@XmlElement(name="fieldname")
	public String getFieldName() { return this.fieldname; }
	public void setFieldName(String fieldname) { this.fieldname = fieldname; }
	
	@XmlElement(name="ref_table_alias")
	public String getRefTableAlias() { return this.ref_table_alias; }
	public void setRefTableAlias(String ref_table_alias) { this.ref_table_alias = ref_table_alias; }
	
	@XmlElement(name="ref_tablename")
	public String getRefTableName() { return this.ref_tablename; }
	public void setRefTableName(String ref_tablename) { this.ref_tablename = ref_tablename; }
	
	@XmlElement(name="tablename")
	public String getTableName() { return this.tablename; }
	public void setTableName(String tablename) { this.tablename = tablename; }
	
	@XmlElement(name="operator")
	public String getOperator() { return this.operator; }
	public void setOperator(String operator) { this.operator = operator; }
	
	@XmlElement(name="arrayValue")
	public Object[] getArrayValue() { return this.arrayValue; }
	public void setArrayValue(Object[] arrayValue) { this.arrayValue = arrayValue; this.value = this.arrayValue; }
	
	@XmlElement(name="integerValue")
	public Integer getIntegerValue() { return this.integerValue; }
	public void setIntegerValue(Integer integerValue) { this.integerValue = integerValue; this.value = this.integerValue; }
	
	@XmlElement(name="stringValue")
	public String getStringValue() { return this.stringValue; }
	public void setStringValue(String stringValue) { this.stringValue = stringValue; this.value = this.stringValue; }
	
	@XmlElement(name="listValue")
	public List<?> getListValue() { return this.listValue; }
	public void setListValue(List<?> listValue) { this.listValue = listValue; this.value = this.listValue; }
	
	
	public boolean QuoteValue() { return this.quote_value; }
	public void setQuoteValue(boolean quote_value) { this.quote_value = quote_value; }	
	
	public Object getValue() { return this.value; }
	public void setValue(Object[] value) { setArrayValue(value); }
	public void setValue(Integer value) { setIntegerValue(value); }
	public void setValue(String value) { setStringValue(value); }
	public void setValue(List<?> value) { setListValue(value); }

	
	protected boolean condition_exists()
	{
		return true;
	}
	
	public String[] get_aliases()
	{	
		if(condition_exists())
		{
			if(this.ref_tablename != null)
			{
				return new String[] {table_alias, ref_table_alias};
			}
			else
			{
				return new String[] {table_alias};
			}
		}
		else
		{
			return null;
		}
		
	}

	public ArrayList<String> get_tablenames()
	{	
		if(condition_exists())
		{
			ArrayList<String> tables = new ArrayList<String>();
			
			tables.add(tablename);
			
			if(this.ref_tablename != null)
			{				
				tables.add(ref_tablename);
			}
		
			return tables;
		}
		else
		{
			return null;
		}
		
	}
	


	// checks if the supplied condition is identical with this one
	public Boolean matches(QueryCondition cond) {
		
		Boolean matches = cond.getFieldName().equals(this.fieldname) &&
				cond.getAttributeName().equals(this.attribute_name) &&
				cond.getValue().equals(this.value) &&
				cond.getOperator().equals(this.operator);
		
		return matches;
	}



	public boolean isCondition_handled_at_hierarchy_level() {
		return condition_handled_at_hierarchy_level;
	}

	public void setCondition_handled_at_hierarchy_level(boolean condition_handled_at_hierarchy_level) {
		this.condition_handled_at_hierarchy_level = condition_handled_at_hierarchy_level;
	}
	
	public boolean isCondition_handled_at_spectrum_level() {
		return condition_handled_at_spectrum_level;
	}

	public void setCondition_handled_at_spectrum_level(boolean condition_handled_at_hierarchy_level) {
		this.condition_handled_at_spectrum_level = condition_handled_at_hierarchy_level;
	}	
	
}


