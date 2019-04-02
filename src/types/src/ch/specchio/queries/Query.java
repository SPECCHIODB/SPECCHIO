package ch.specchio.queries;

import java.util.ArrayList;
import java.util.ListIterator;

import javax.xml.bind.annotation.*;


/**
 * This class represents a query to the database.
 */
@XmlRootElement(name="query")
public class Query {
	
	public static final String COUNT_QUERY = "count";
	public static final String SELECT_QUERY = "select";
	
	private String query_type = SELECT_QUERY;
	
	private String tablename;
	private String tablename_alias;
	
	private ArrayList<String> columns = new ArrayList<String>();

	private ArrayList<QueryCondition> cond_fields = new ArrayList<QueryCondition>();
	private ArrayList<QueryCondition> standard_cond_fields = new ArrayList<QueryCondition>();
	private ArrayList<QueryCondition> eav_cond_fields = new ArrayList<QueryCondition>();
	
	private ArrayList<String> join_tables = new ArrayList<String>();
	private ArrayList<QueryCondition> join_conds = new ArrayList<QueryCondition>();;
	
	private String order_by = "";
	
	public Query()
	{
		this.tablename = "spectrum"; // default value
	}
	
	public Query(String tablename)
	{
		this(tablename, tablename);
	}
	
	public Query(String tablename, String tablename_alias)
	{
		this.tablename = tablename;
		this.tablename_alias = tablename_alias;
	}
	
	@XmlElement(name="columns")
	public ArrayList<String> getColumns() { return this.columns; }
	public void setColumns(ArrayList<String> columns) { this.columns = columns; }
	public String getColumn(int i) { return this.columns.get(i); }
	public void addColumn(String column) { this.columns.add(column); }
	
	@XmlElement(name="standard_cond_fields")
	public ArrayList<QueryCondition> getStandardConditionFields() { return this.standard_cond_fields; }
	public void setStandardConditionFields(ArrayList<QueryCondition> cond_fields) { this.standard_cond_fields = cond_fields; }

	@XmlElement(name="eav_cond_fields")
	public ArrayList<QueryCondition> getEAVConditionFields() { return this.eav_cond_fields; }
	public void setEAVConditionFields(ArrayList<QueryCondition> cond_fields) { this.eav_cond_fields = cond_fields; }	
	
	@XmlElement(name="cond_fields")
	public ArrayList<QueryCondition> getConditionFields() { return this.cond_fields; }
	public void setConditionFields(ArrayList<QueryCondition> cond_fields) { this.cond_fields = cond_fields; }	
	
	@XmlElement(name="query_type")
	public String getQueryType() { return this.query_type; }
	public void setQueryType(String query_type) { this.query_type = query_type; }
	
	@XmlElement(name="order_by")
	public String getOrderBy() { return this.order_by; }
	public void setOrderBy(String order_by) { this.order_by = order_by; }
	
	@XmlElement(name="tablename_alias")
	public String getTableAlias() { return this.tablename_alias; }
	public void setTableAlias(String tablename_alias) { this.tablename_alias = tablename_alias; }
	
	@XmlElement(name="tablename")
	public String getTableName() { return this.tablename; }
	public void setTableName(String tablename) { this.tablename = tablename; }
	
			
	public void add_condition(QueryCondition cond)
	{
		boolean exists = false;
		// only add if this condition is not yet existing
		
		ListIterator<QueryCondition> li = cond_fields.listIterator();
		while(li.hasNext())
		{
			QueryCondition co = li.next();
			 
//			if(co.getClass() == EAVQueryConditionObject.class)
//				exists = cond.matches( (EAVQueryConditionObject)co);
//			else
				exists = cond.matches(co);

		}
		
		if (!exists)
		{
			cond_fields.add(cond);
			
			// add to lists for normal conditions or EAV conditions
			// System.out.println(cond.getClass().getName());
			if(cond.getClass().getName().equals("ch.specchio.queries.EAVQueryConditionObject"))
			{
				eav_cond_fields.add(cond);				
			}
			else
			{
				standard_cond_fields.add(cond);				
			}			
			
		}
		
	}
	
	
	public void add_join(String tablename, QueryCondition cond)
	{
		this.join_tables.add(tablename);
		this.join_conds.add(cond);
	}
	
	
	public void remove_all_conditions()
	{
		cond_fields.clear();
		eav_cond_fields.clear();
		standard_cond_fields.clear();
		join_tables.clear();
		join_conds.clear();
	}

}
