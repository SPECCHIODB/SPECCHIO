package ch.specchio.query_builder;

import java.util.Hashtable;


public abstract class QueryField {
	
	Hashtable<String, Object> client_properties = new Hashtable<String, Object>();
	
	boolean is_set = false;

	protected String fieldname;
	String operator = "=";	

	private String value;

	public Object getNative_value() {
		return native_value;
	}

	public void setNative_value(Object native_value) {
		this.native_value = native_value;
	}

	private Object native_value;
	
	public void putClientProperty(String key,
			Object o) {
		
		client_properties.put(key, o);
		
	}


	abstract public void textReport();


	public boolean isSet() {
		return is_set;
	}


	public String get_fieldname()
	{
		return fieldname;
	}


	abstract public String getLabel();


	public Object getClientProperty(String key) {
		return client_properties.get(key);
	}


	public void set_value(String string) {
		value = string;
		
		if(!value.equals(""))
		{
			is_set = true;
		}
		else
		{
			is_set = false;
		}
	}


	public String getValue() {
		return value;
	}


	public String getOperator() {
		return operator;
	}
	
	public void set_operator(String operator) {
		this.operator = operator;
		
	}
		

}
