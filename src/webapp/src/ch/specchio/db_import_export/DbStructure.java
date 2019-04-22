package ch.specchio.db_import_export;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.ListIterator;

import ch.specchio.eav_db.SQL_StatementBuilder;


public class DbStructure {
	
	SQL_StatementBuilder SQL;
	String schema;
	protected ArrayList<DbTable> table_list;
	ArrayList<Long> times;
	ArrayList<Long> sensor_time;
	ArrayList<Integer> sensor_type;
	ArrayList<Integer> no_of_fields;
	int row_cnt = 0;
	public int level = 0;
	DbTable campaign_dbt;
	
	public DbStructure(SQL_StatementBuilder SQL, String schema)
	{
		this.SQL = SQL;
		this.schema = schema;
		
		table_list = new ArrayList<DbTable>();
		times= new ArrayList<Long>();
		sensor_time = new ArrayList<Long>();
		sensor_type = new ArrayList<Integer>();
		no_of_fields = new ArrayList<Integer>();
		
		read_db_structure();
	}
	
	
	private void read_db_structure()
	{
		campaign_dbt = null;
		Statement stmt;
		try {
			stmt = SQL.createStatement();
			
			String query;
			ResultSet rs;
	
			// get table names of the current schema
			query = "select table_name from information_schema.tables where table_type = 'BASE TABLE' AND table_schema = '" + this.schema + "'";
			rs = stmt.executeQuery(query);
		
			while (rs.next()) {
	
				String table_name = rs.getString(1);
				//System.out.println(table_name);
				DbTable dbt = new DbTable(this.SQL, this.schema, table_name, this);
				table_list.add(dbt);
				
				if(table_name.equals("campaign"))
				{
					campaign_dbt = dbt;
				}
			}
			
			rs.close();		
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		
		
		ListIterator<DbTable> t_li = table_list.listIterator();
		DbTable tmp_table;
		while(t_li.hasNext())
		{
			tmp_table = t_li.next();
			tmp_table.build_referencing_table_list();
			//tmp_table.build_referenced_user_table_list();
			tmp_table.set_fk_db_tables();
		}
		
		
	}
	
	public DbTable get_table(String tablename)
	{
		DbTable t;
		
		ListIterator<DbTable> li = table_list.listIterator();

		while(li.hasNext())
		{
			t = li.next();
			
			if(t.name.equals(tablename))
			{
				return t;
			}
			
		}
		
		// create a dummy table to avoid null pointer exceptions in case unknown tables are found
		t = new DbTable();
		
			
		return t;
	}
	
	
	public void log_time(int no_of_fields)
	{
		times.add(System.currentTimeMillis());
		this.no_of_fields.add(no_of_fields);
	}
	
	public void log_sensor(int id)
	{
		this.sensor_time.add(System.currentTimeMillis());
		this.sensor_type.add(id);
	}


}
