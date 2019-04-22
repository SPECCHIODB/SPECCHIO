package ch.specchio.db_import_export;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;

import ch.specchio.eav_db.SQL_StatementBuilder;
import ch.specchio.eav_db.id_and_op_struct;

abstract class FieldValue
{
	Object value;
	Boolean is_key_field = false;
	
	abstract void read_value(ResultSet rs, String name)  throws SQLException;
	@Override
	public String toString()
	{
		if(isNull())
			return "null";
		else	
			return value.toString();
	};
	
	abstract public boolean isNull();
	abstract public void fromString(String str);
	public String insertable_string(){return toString();}; // default is the same as toString
	
	public void is_key_field(boolean is_key)
	{
		is_key_field = is_key;
	}
	
	public boolean is_key_field()
	{
		return is_key_field;
	}
	
	void cx_for_null_value(ResultSet rs, String name) throws SQLException
	{
		String str = rs.getString(name); // the getInt returns 0 for null columns!
		if(str == null)
			value = null;	
	}
}

class IntFieldValue extends FieldValue
{
	//Integer value;
	
	@Override
	void read_value(ResultSet rs, String name)  throws SQLException
	{
		value = rs.getInt(name);
		cx_for_null_value(rs, name);
	}
	
	@Override
	public String toString()
	{
		return insertable_string();
	}
	
	@Override
	public boolean isNull()
	{
		if(value == null)
			return true;
		else
			return false;
	}

	@Override
	public void fromString(String str) 
	{
		if(str.equals(""))
			value = null;
		else
			value = Integer.valueOf(str);
	}
	
	@Override
	public String insertable_string()
	{
		if(isNull())
			return "null";
		else
			return value.toString();		
	}
	
}


class StringFieldValue extends FieldValue
{
	//String value;
	
	@Override
	void read_value(ResultSet rs, String name)  throws SQLException
	{
		value = rs.getString(name);
	}

	@Override
	public boolean isNull()
	{
		if(value == null)
			return true;
		else
			return false;
	}

	@Override
	public void fromString(String str) {
		if(str == "")
			value = null;
		else
			value = str;
	}


	@Override
	public String insertable_string() {
		if(isNull())
			return "null";
		else		
			return "\""+toString()+"\"";
	}
	
}


class DateFieldValue extends StringFieldValue{}

class VarcharFieldValue extends StringFieldValue{}

class DateTimeFieldValue extends StringFieldValue{}

class CharFieldValue extends StringFieldValue{}

class BlobFieldValue extends FieldValue
{
	byte[] value;

	@Override
	void read_value(ResultSet rs, String name) throws SQLException 
	{
		Blob b = rs.getBlob(name);
		if(b != null)
		{
			long pos = 1;
			int length = Integer.valueOf((int) b.length());
			value = b.getBytes(pos, length);
		}
		else
		{
			value = null;
		}
	}


	@Override
	public String toString() {
		
		String out = "0x";
		String tmp;
		char[] chararr = new char[value.length*2];
		int i = 0;
		int j = 0;
 
		try
		{
		while(i < value.length)
		{		
			/*
		    int l = 0;
		    l |= value[i++] & 0xFF;
		    l <<= 8;
		    l |= value[i++] & 0xFF;
		    l <<= 8;
		    l |= value[i++] & 0xFF;
		    l <<= 8;
		    l |= value[i++] & 0xFF;
			
			tmp = Integer.toHexString(l);
							
			while(tmp.length() < 8) // make sure all numbers are coded with 8 hex characters
			{
				tmp = "0" + tmp;
			}
			//System.out.println(l);
			
			out = out + tmp;
			*/

		    int l = 0;
		    l |= value[i++] & 0xFF;
			
			tmp = Integer.toHexString(l);
			
			/*
			while(tmp.length() < 2) // make sure all numbers are coded with 2 hex characters
			{
				tmp = "0" + tmp;
			}
			*/
			//System.out.println(l);
			
			if(tmp.length() == 1)
			{
				chararr[j++] = '0';
				chararr[j++] = tmp.charAt(0);				
			}
			else
			{			
				chararr[j++] = tmp.charAt(0);
				chararr[j++] = tmp.charAt(1);
			}
			
			//out = out + tmp;

			
		}

        out += String.valueOf(chararr);
        
		} catch (java.lang.OutOfMemoryError e)
		{
			int x = 1;
		}

		return out;
	}
	
	@Override
	public boolean isNull()
	{
		if(value == null)
			return true;
		else
			return false;
	}


	@Override
	public void fromString(String str) 
	{
		int i = 0;
		String tmp;
		int ival;
		int arr_pos = 0;
		
		if(str.length() > 0){
			value = new byte[(str.length() - 2)/2];
			
			// omit the hex coding start
			i = 2;
			
			// read 8 character hex values from string
			while(i < str.length())
			{
				/*
				tmp = str.substring(i, i+8);
				ival = Integer.parseInt(tmp, 16);
				
				//byte[] word = new byte[4];
				value[arr_pos++] = (byte)((ival >> 24) & 0xFF);
				value[arr_pos++] = (byte)((ival >> 16) & 0xFF);
				value[arr_pos++] = (byte)((ival >> 8) & 0xFF);
				value[arr_pos++] = (byte)(ival & 0xFF);
				
				//for(int j = 0; j < 4; j++)
				//	value[arr_pos++] = word[j];
	
				//System.out.println(ival);
				i+=8;
				*/
				
				tmp = str.substring(i, i+2);
				ival = Integer.parseInt(tmp, 16);
				value[arr_pos++] = (byte)(ival & 0xFF);
				i+=2;
				
			}
			
		}
		else
		{
			value = null;
		}
		

	}


	@Override
	public String insertable_string() {
		// TODO Auto-generated method stub
		return null;
	}
	
}

class MediumBlobFieldValue extends BlobFieldValue{}

class LongBlobFieldValue extends BlobFieldValue{}

class TinyIntFieldValue extends FieldValue
{
	//Boolean value;
	
	@Override
	void read_value(ResultSet rs, String name) throws SQLException 
	{
		value = rs.getBoolean(name);
		cx_for_null_value(rs, name);
	}
	
	@Override
	public boolean isNull()
	{
		if(value == null)
			return true;
		else
			return false;
	}


	@Override
	public void fromString(String str) {
		if(str.equals(""))
			value = null;
		else
			value = Boolean.valueOf(str);		
	}
	
	@Override
	public String insertable_string() {
		if(isNull())
			return "null";
		if((Boolean)value)
			return "1";
		else
			return "0";
	}
	
}

class FloatFieldValue extends FieldValue
{
	//Float value;
	
	@Override
	void read_value(ResultSet rs, String name) throws SQLException 
	{
		value = rs.getFloat(name);
		cx_for_null_value(rs, name);
	}

	
	@Override
	public boolean isNull()
	{
		if(value == null)
			return true;
		else
			return false;
	}


	@Override
	public void fromString(String str) {
		if(str.equals(""))
			value = null;
		else
			value = Float.valueOf(str);
		
	}	
}

class DoubleFieldValue extends FieldValue
{
	//Double value;
	
	@Override
	void read_value(ResultSet rs, String name) throws SQLException 
	{
		value = rs.getDouble(name);	
		cx_for_null_value(rs, name);
	}
	
	@Override
	public boolean isNull()
	{
		if(value == null)
			return true;
		else
			return false;
	}

	@Override
	public void fromString(String str) {
		if(str.equals(""))
			value = null;
		else
			value = Double.valueOf(str);		
	}	
}

class DecimalFieldValue extends FieldValue
{
	
	@Override
	void read_value(ResultSet rs, String name) throws SQLException 
	{
		value = rs.getString(name);	// getting floats leads to problems of rounding when converting to string 
		cx_for_null_value(rs, name);
	}
	
	@Override
	public boolean isNull()
	{
		if(value == null)
			return true;
		else
			return false;
	}

	@Override
	public void fromString(String str) {
		if(str.equals(""))
			value = null;
		else
			value = str; // leave as string		
	}	
}

class GeometryFieldValue extends FieldValue
{
	//String value;
	
	@Override
	void read_value(ResultSet rs, String name)  throws SQLException
	{
		value = rs.getString(name);
		
	}

	@Override
	public boolean isNull()
	{
		if(value == null)
			return true;
		else
			return false;
	}

	@Override
	public void fromString(String str) {
		if(str == "")
			value = null;
		else
			value = str;
	}


	@Override
	public String insertable_string() {
		if(isNull())
			return "null";
		else		
			return "ST_GeomFromText('" +toString()+"')";
	}
	
}




class FieldValueFactory
{
	private static FieldValueFactory instance = null;
	
	protected FieldValueFactory(){}
		
	public static FieldValueFactory getInstance()
	{
		if(instance == null) {
			instance = new FieldValueFactory();
		}
		return instance;
	}     
		
	public FieldValue get_value(String type)
	{
		FieldValue v = null;
		
		if(type.equals("int"))
		{
			v = new IntFieldValue();
		}
		
		if(type.equals("date"))
		{
			v = new DateFieldValue();
		}
		
		if(type.equals("varchar"))
		{
			v = new VarcharFieldValue();
		}
		
		if(type.equals("char"))
		{
			v = new CharFieldValue();
		}
		
		if(type.equals("blob"))
		{
			v = new BlobFieldValue();
		}
		
		if(type.equals("tinyint"))
		{
			v = new TinyIntFieldValue();
		}
		
		if(type.equals("float"))
		{
			v = new FloatFieldValue();
		}
		
		if(type.equals("mediumblob"))
		{
			v = new MediumBlobFieldValue();
		}
		
		if(type.equals("longblob"))
		{
			v = new LongBlobFieldValue();
		}
		
		if(type.equals("double"))
		{
			v = new DoubleFieldValue();
		}
		
		if(type.equals("decimal"))
		{
			v = new DecimalFieldValue();
		}

		if(type.equals("datetime"))
		{
			v = new DateTimeFieldValue();
		}
		
		if(type.equals("timestamp"))
		{
			v = new DateTimeFieldValue();
		}		
		
		if(type.equals("geometry"))
		{
			v = new GeometryFieldValue();
		}		
		
		
		if(v == null)
		{
			System.out.println("!!!!!!!!!!    Cannot build class for: " + type);
		}		
		
		return v;
	}
	
	
}

interface DbField
{
	String get_name();
	@Override
	public String toString();
}


class TableField implements DbField
{
	String name;
	String type_name;
	FieldValue value;
	Boolean not_null_varchar = false;
	
	


	public TableField(String name, String type_name)
	{
		this.name = name;
		//System.out.println("         type: " + type_name);
		this.type_name = type_name;
		
		FieldValueFactory f = FieldValueFactory.getInstance();
		value = f.get_value(type_name);
	}
	
	
	public TableField(String column_name, String data_type, String is_nullable) {
		
		this(column_name, data_type);
		
		if(this.type_name.equals("varchar") && is_nullable.equals("NO")) not_null_varchar = true;
		
	}

	public Boolean isNot_null_varchar() {
		return not_null_varchar;
	}

	void get_value(ResultSet rs) throws SQLException
	{
		//String name_ = this.get_name();
		value.read_value(rs, this.get_resultset_field_name());
	}
	
	public void setValueFromString(String str) {
		
		value.fromString(str);
		
	}
	
//	public String getExistsCond()
//	{
//		String op;
//		String cond = ""; 
//		
//		if(value.isNull())
//			op = " is ";
//		else
//			op = " = ";
//		
//		if(value.insertable_string() != null) // e.g. blobs are null when calling insertable_string
//		{
//		
//			cond = name + op + value.insertable_string();		
//		}
//		
//		return cond;
//	}
	
	@Override
	public String toString()
	{
		if(value.isNull())
		{
			return "";
		}
		else
		{
			return value.toString();
		}
	}	

	@Override
	public String get_name() {
		return name;
	}
	
	public String get_select_field_name() {
		if(this.type_name.equals("geometry"))
		{
			return("ST_AsText(" + name + ")");
		}
		else			
			return name;
	}	
	
	public String get_resultset_field_name() {
		if(this.type_name.equals("geometry"))
		{
			return(".ST_AsText(" + name + ")");
		}
		else			
			return name;
	}		

}


class FkTableField extends TableField
{
	String referenced_table;
	String referencing_table;
	String referenced_column_name;
	DbTable referenced_db_table;
	
	public FkTableField(SQL_StatementBuilder SQL, String schema, String name, String type_name, String referencing_table)
	{
		super(name, type_name);
		this.referencing_table = referencing_table;
		
		Statement stmt;
		
		// get the referenced table name
		try {
			stmt = SQL.createStatement();
			
			String query;
			ResultSet rs;	
	
			// search tables that are referenced by this table
			query = "SELECT referenced_table_name, referenced_column_name FROM information_schema.KEY_COLUMN_USAGE where column_name = '" + name + "' AND table_name = '" + referencing_table + "' AND table_schema = '" + schema + "'";
			rs = stmt.executeQuery(query);
		
			while (rs.next()) {
	
				referenced_table = rs.getString(1);
				referenced_column_name = rs.getString(2);
				//System.out.println("         referencing: " + referenced_table);
			}
			
			rs.close();	
			stmt.close();
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		value.is_key_field(true);
		
	}
	
	public int get_id()
	{
		if(value.value != null)
			return (Integer)((IntFieldValue)value).value;
		else
			return 0;
	}
	
	public void set_db_table(DbTable t)
	{
		this.referenced_db_table = t;
	}

	
}


class KeyLookup
{
	String keyname;
	ArrayList<Integer> old_keys = new ArrayList<Integer>();
	ArrayList<Integer> new_keys = new ArrayList<Integer>();
	
	public KeyLookup(String keyname)
	{
		this.keyname = keyname;
	}
}

class ExportedRows
{
	public ArrayList<Integer> ids = new ArrayList<Integer>(); // ids of exported rows
	public String PK_field_name;
	
	public ExportedRows(String PK_field_name)
	{
		this.PK_field_name = PK_field_name;
	}
}


public class DbTable
{
	SQL_StatementBuilder SQL;
	String schema;
	String name;
	String col_names, all_col_names, fk_col_names, col_names_without_blobs, pk_col_names, select_col_names; // comma separated column names of this table
	boolean system_table = false;
	boolean system_table_end_node = false;
	boolean recursive_table = false;
	boolean x_rel_table = false;
	boolean isref_exc;
	SystemTables st;
	ArrayList<TableField> PK = new ArrayList<TableField>();
	ArrayList<FkTableField> FKs = new ArrayList<FkTableField>();
	ArrayList<TableField> cols = new ArrayList<TableField>();
	ArrayList<TableField> non_blob_cols = new ArrayList<TableField>();
	ArrayList<TableField> blob_cols = new ArrayList<TableField>();
	Hashtable<String, TableField> all_cols = new Hashtable<String, TableField>(); 
	static ArrayList<String> system_tables = new ArrayList<String>();
	ArrayList<DbTable> referencing_tables = new ArrayList<DbTable>();
	ArrayList<DbTable> referenced_user_tables = new ArrayList<DbTable>();
	ArrayList<ExportedRows> exported_rows = new ArrayList<ExportedRows>();
	ArrayList<KeyLookup> key_lookup = new ArrayList<KeyLookup>();
	
	DbStructure cex;
	private ArrayList<TableField> string_cols = new ArrayList<TableField>();
	private boolean do_info_print = false;
	private boolean isDummy = false;
	
	// default constructor to enable basic handling of table reading for the case of unknown tables in input data stream
	public DbTable()
	{		
		isDummy = true;
	}
	
	public DbTable(SQL_StatementBuilder SQL, String schema, String name, DbStructure cex)
	{

		this.SQL = SQL;
		this.schema = schema;
		this.name = name;
		this.cex = cex;
		
		read_table_structure();
		
		st = new SystemTables(SQL, schema);

		system_table = st.isSystemTable(name);
		system_table_end_node = st.isEndNodeSystemTable(name);
		isref_exc = st.isIRefException(this.name, "");
		
		build_col_name_strings();
				
	}
	
	
	void read_table_structure()
	{
		boolean xrel_fk = false;
		//Statement stmt;
		try {
			//stmt = db_conn.createStatement();
			
			String query;
			ResultSet rs;
			TableField tf = null;
	
			// get fields of this table
			Statement stmt = SQL.createStatement();
			query = "select column_name, data_type, column_key, is_nullable from information_schema.columns where table_name = '" + name + "' AND table_schema = '" + this.schema + "'";
			rs = stmt.executeQuery(query);
		
			while (rs.next()) {
	
				String column_name = rs.getString(1);
				String data_type = rs.getString(2);
				String column_key = rs.getString(3);
				String is_nullable = rs.getString(4);
				
				// x-rel key check:
				// check for x-relation table feature: primary key fields exist as FK fields as well 
				if(column_key.equals("PRI"))
				{
					int cnt = 0;
					ResultSet xrel_rs;
					Statement xrel_stmt = SQL.createStatement();
					query = "select count(*) from information_schema.KEY_COLUMN_USAGE K where column_name = '" + column_name + "' and table_name = '" + name + "' and referenced_table_name is not null AND table_schema = '" + this.schema + "'";
					xrel_rs = xrel_stmt.executeQuery(query);

					while (xrel_rs.next()) 
					{
						cnt = xrel_rs.getInt(1);
					}
					
					if(cnt > 0) 
					{
						xrel_fk = true;
						this.x_rel_table = true;
					}
					
					xrel_rs.close();
					xrel_stmt.close();
				}
				
				
				if(column_key.equals("MUL"))
				{
					//System.out.println("  FK: " + column_name);
					FkTableField fktf = new FkTableField(SQL, this.schema, column_name, data_type, name);
					FKs.add(fktf);
					tf = fktf; // store in overall column list further down ...
					
					// check for recursive tables
					if(fktf.referenced_table.equals(fktf.referencing_table))
					{
						this.recursive_table = true;
					}
				}
				
				if(column_key.equals("PRI"))
				{
					//System.out.println("  PRI: " + column_name);
					if(xrel_fk)
					{
						FkTableField fktf = new FkTableField(SQL, this.schema, column_name, data_type, name);
						FKs.add(fktf);
						tf = fktf; // store in overall column list and PK further down ...						
					}
					else
					{
						tf = new TableField(column_name, data_type);						
						tf.value.is_key_field(true);
					}
					
					PK.add(tf);
										
					this.key_lookup.add(new KeyLookup(column_name));
				}
				
				if(column_key.equals(""))
				{
					//System.out.println("  : " + column_name);
					tf = new TableField(column_name, data_type, is_nullable);
					this.cols.add(tf);
					
					if(!(data_type.equals("blob") || data_type.equals("mediumblob") || data_type.equals("longblob")))
					{
						this.non_blob_cols.add(tf);
					}
					else
					{
						blob_cols.add(tf);
					}
					
					if(data_type.equals("varchar"))
					{
						string_cols.add(tf);
					}
					
				}
				
				all_cols.put(column_name, tf);

			}
			
			rs.close();		
			stmt.close();
				
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}					
		
	}
	
	
	// this is clearing the exported row lists, needed for the density measurement
	public void clear_handled_row_list()
	{
		ExportedRows er = null;
		ListIterator<ExportedRows> li = exported_rows.listIterator();

		while(li.hasNext())
		{
			er = li.next();
			er.ids.clear();
		}		
	}
	
	
	public int measure_density(int id, DbTable caller) throws SQLException
	{
		int non_key_field_cnt = 0;
		int user_fk_field_cnt = 0;
		int sys_fk_field_cnt = 0;
		int iref_cnt = 0;
		int total_cnt = 0;
		String query;
		ResultSet rs;
		
		Statement stmt = SQL.createStatement();
		
		String PK_field_name = this.get_PK_field_name(caller);
		
		// cx if already handled
		if(this.already_exported(PK_field_name, id))
		{
//			System.out.println("already handled: " + this.name+ " " + Integer.toString(id));
		}
		else
		{

//			System.out.println("MSD calc of " + this.name + " " + Integer.toString(id) );
			// add this id
			this.add_exported_row_id(PK_field_name, id);
			
			// count for all non-key fields
			ListIterator<TableField> li = cols.listIterator();
	
			while(li.hasNext())
			{
				TableField f = li.next();		
			
				query = "select count(*) from " + this.name + " where " + f.name + " is not null and " + PK_field_name + "="+Integer.toString(id);
				
				rs = stmt.executeQuery(query);
				
				while (rs.next()) {
		
					non_key_field_cnt += rs.getInt(1);
				}	
				rs.close();
			}
			
			// user tables must be corrected for the count of the user id, which does not count
			if(!this.system_table)
			{
				non_key_field_cnt--;
			}
			
			
			// call density on all FK's referencing user tables
			ListIterator<FkTableField> fkli;
			boolean restricted_MSD = true;
			
			if(!(restricted_MSD && this.name.equals("spectrum_datalink")))
			{
			fkli = this.FKs.listIterator();
	
				while(fkli.hasNext())
				{
					FkTableField f = fkli.next();	
					
					if(!f.referenced_db_table.system_table)
					{
						int fk_value = 0;
						// get the value
						query = "select " + f.name + " from " + this.name + " where " + PK_field_name + "="+Integer.toString(id);
						
						rs = stmt.executeQuery(query);
						
						while (rs.next()) {
				
							fk_value = rs.getInt(1);
						}		
						rs.close();	
						
						if(fk_value != 0)					
							user_fk_field_cnt += f.referenced_db_table.measure_density(fk_value, this);
					}
				
				}	
			}
			else // spectrum_datalink special handling if restricted MSD is true
			{
				user_fk_field_cnt+=1; // add one for the linked spectrum.
			}
			
			// hierarchy levels are always counting the campaign again, therefore, reduce the cnt here
			if(this.name.equals("hierarchy_level"))
				user_fk_field_cnt--;
			
			
			// count not-null system fk's
			fkli = this.FKs.listIterator();
	
			while(fkli.hasNext())
			{
				FkTableField f = fkli.next();	
				
				if(f.referenced_db_table.system_table)
				{
			
					query = "select count(*) from " + this.name + " where " + f.name + " is not null and " + PK_field_name + "="+Integer.toString(id);
					
					rs = stmt.executeQuery(query);
					
					while (rs.next()) {
			
						sys_fk_field_cnt += rs.getInt(1);
					}		
					rs.close();
				
				}
			}			
			
			
			// count all iref rows
			if(this.name.equals("spectrum"))
			{
				ListIterator<DbTable> t_li = referencing_tables.listIterator();
				
				while(t_li.hasNext())
				{
					DbTable t = t_li.next();
					
					// search table rows that are referencing this table
					query = t.get_search_referencing_table_query(name, id);
						
					rs = stmt.executeQuery(query);
				
					while (rs.next()) {
			
						int referencing_id = rs.getInt(1);
	
						iref_cnt += t.measure_density(referencing_id, this);
						
					}
					
					rs.close();		
				
				}
			}
			
			stmt.close();
			
			total_cnt = non_key_field_cnt+user_fk_field_cnt+sys_fk_field_cnt+iref_cnt;
			
			
//			System.out.print("Density at " + this.name + " " + Integer.toString(id) + " : " );
//			System.out.println(total_cnt);
		
		}
		
		
		return total_cnt;
	}
	
	
	// to be called after all table class instances have been created
	void build_referencing_table_list()
	{
		// build a list of referencing tables			
		ListIterator<DbTable> t_li = cex.table_list.listIterator();
		DbTable tmp_table;

		while(t_li.hasNext())
		{
			tmp_table = t_li.next();
			
			if(tmp_table.is_referencing(name))
				referencing_tables.add(tmp_table);
		}		
	}
	
	
	// to be called after all table class instances have been created
//	void build_referenced_user_table_list()
//	{
//		
//		ListIterator<DbTable> t_li = cex.table_list.listIterator();
//		DbTable tmp_table;
//
//		while(t_li.hasNext())
//		{
//			tmp_table = t_li.next();
//			
//			if(tmp_table.system_table &&this.is_referencing(tmp_table.name))
//			{
//				referenced_user_tables.add(tmp_table);								
//			}			
//		}
//		
//	}
	
	// to be called after all table class instances have been created
	void set_fk_db_tables()
	{
		FkTableField fk;
		ListIterator<FkTableField> li = FKs.listIterator();


		while(li.hasNext())
		{
			fk = li.next();			
			fk.referenced_db_table = cex.get_table(fk.referenced_table);		
		}		
		
	}
	
	
	// build query sub-strings that contain all/non-key/FK fields of this table
	void build_col_name_strings()
	{	
		all_col_names = conc_names(all_cols.values());
		col_names = conc_names(cols);		
		pk_col_names = conc_names(PK);
		fk_col_names = conc_names(FKs);
		col_names_without_blobs = conc_names(this.non_blob_cols);
		
		select_col_names = conc_select_names(all_cols.values());
		
		
		
	}
	
	String conc_names(Collection<? extends TableField> fields)
	{
		StringBuffer res = new StringBuffer();
		for (TableField field : fields) {
			if (res.length() > 0) {
				res.append(", ");
			}
			res.append(field.get_name());
		}
				
		return res.toString();		
	}
	
	String conc_select_names(Collection<? extends TableField> fields)
	{
		StringBuffer res = new StringBuffer();
		for (TableField field : fields) {
			if (res.length() > 0) {
				res.append(", ");
			}
			res.append(field.get_select_field_name());
		}
				
		return res.toString();		
	}	
	
	
	void info_print(String str, String table)
	{
		if(this.do_info_print)
		{
			for(int i = 0; i < cex.level;i++)
			{
				//System.out.print(" ");
				str = " " + str; 
			}
			if(!table.equals("sensor_element")) System.out.println(str);
		}
	}
	
	
	void add_exported_row_id(String PK_field_name, int id)
	{
		// search for the list that holds the ids for this field
		ExportedRows er = null;
		ListIterator<ExportedRows> li = exported_rows.listIterator();
		
		boolean found = false;

		while(li.hasNext())
		{
			er = li.next();
			
			if(er.PK_field_name.equals(PK_field_name))
			{
				er.ids.add(id);
				found= true;
			}

		}
		
		if(!found) // field not yet defined
		{
			er = new ExportedRows(PK_field_name);
			er.ids.add(id);
			exported_rows.add(er);			
		}
		
	}
	
	
	boolean already_exported(String PK_field_name, int id)
	{
		boolean exported = false;
		
		ExportedRows er = null;
		ListIterator<ExportedRows> li = exported_rows.listIterator();

		while(li.hasNext())
		{
			er = li.next();
			
			if(er.PK_field_name.equals(PK_field_name))
			{
				exported = er.ids.contains(id);
			}
		}		
		
		
		return exported;
	}
	
	
	String get_PK_field_name(DbTable caller)
	{
		String PK_field_name = "";
		if(this.x_rel_table)
		{
			// the field name depends on the caller
			PK_field_name = get_referencing_fk(caller.name).name;
		}
		else
		{
			PK_field_name = this.pk_col_names; // just one primary key
		}
		
		return PK_field_name;
	}
	
	public void export(int id, DbTable caller)
	{
		Statement stmt = null;
		cex.level++;
		
		String PK_field_name = this.get_PK_field_name(caller);

//		if(this.name.equals("spectrum_x_eav"))
//		System.out.println("gotcha");
		
		// check if this row is already exported (avoid multiple exports) 
		// Note: for xrel tables, the column name must also be checked, i.e. one 
		// needs a list of primary keys with the according column names
		if(this.already_exported(PK_field_name, id))
		{
			// do nothing
		}
		else
		{
//			try {
//				stmt = SQL.createStatement();
//			} catch (SQLException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
			
			
			// Step 1: export all tables that are referenced by this table via foreign keys
			// ---------------------------------------------------------------------------
			info_print("ref(" + this.name + ") id:" + id, name);
			FkTableField fk;
			ListIterator<FkTableField> li = FKs.listIterator();
			
			
			if(this.name.equals("eav") && id == 64947)
			{
				int x = 1;
			}
			
			// li = FKs.listIterator();
	
			while(li.hasNext())
			{
				fk = li.next();
								
				// call export on all rows of that referenced table that are having their primary key equal to the foreign key value 
				// (actually only one such row should ever exist)
				
				// avoid rebounding to the calling table for xrel tables (for speed reasons)
				if(!(this.x_rel_table && fk.referenced_table.equals(caller.name)))
				{
									
					try {
						stmt = SQL.createStatement();
						String query;
						ResultSet rs;
				
						// search table rows that are referenced by this table
						// use first primary key in list (apart from crossrelation tables, all tables only got one primary key)
						// -> crossrelation tables must be treated differently
						if(this.x_rel_table) {
							query = SQL.assemble_sql_select_query(fk.name, this.name, get_referencing_fk(caller.name).name + " = " + Integer.toString(id));
						} else {
							query = SQL.assemble_sql_select_query(fk.name, this.name, this.PK.get(0).name + " = " + Integer.toString(id));
						}
							
						rs = stmt.executeQuery(query);
					
						ArrayList<Integer> referenced_ids = new ArrayList<Integer>();
						
						while (rs.next()) {
				
							int referenced_id = rs.getInt(1);
							referenced_ids.add(referenced_id);
						}
						
						rs.close();		
						stmt.close();
						
						Iterator<Integer> it = referenced_ids.iterator();
						
						while(it.hasNext())
						{
							int referenced_id = it.next();
							
							// only export non null references
							if(referenced_id != 0)
							{
								
								// export
								// find the object in the list first
								ListIterator<DbTable> t_li = cex.table_list.listIterator();
								DbTable ref_table = null;
								DbTable tmp_table;
			
								while(t_li.hasNext() && ref_table == null)
								{
									tmp_table = t_li.next();
									if(tmp_table.name.equals(fk.referenced_table))
									{
										ref_table = tmp_table;
										ref_table.export(referenced_id, this);
									}
								}
							}							
						}
						
					
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	
				
				}
								
			}
			
			
			// Step 2: export this table itself
			// ---------------------------------
			info_print("exp(" + this.name + ") " + PK_field_name + ":" + id, name);			
			
			if(!this.already_exported(PK_field_name, id)) // another check, in case it got exported in the meantime
			{
				add_exported_row_id(PK_field_name, id);
				export_this(id, caller);

//				if(this.name.equals("spectrum_x_eav") && id == 3113)
//					System.out.println("gotcha");

				// if this is a xrel table, then we need add the second primary key to the list of exported rows as well
				if (this.x_rel_table)
				{
					// get the complementary PK
					ListIterator<TableField> pk_ite = this.PK.listIterator();
					TableField pk;

					while(pk_ite.hasNext())
					{
						pk = pk_ite.next();

						if(!pk.name.equals(PK_field_name))
						{
							String query = SQL.assemble_sql_select_query(pk.name, name, PK_field_name + " = " + Integer.toString(id));

							try {
								stmt = SQL.createStatement();

								ResultSet rs;	
								rs = stmt.executeQuery(query);
								int compl_pk_id = 0;

								while (rs.next()) {								
									compl_pk_id = rs.getInt(1);
									add_exported_row_id(pk.name, compl_pk_id);								
								}

								rs.close();		
								stmt.close();

							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}							

						}
					}		

				}
			}

			
			// Step 3: export tables that are referencing this table 
			// further rule: system tables can export only other system tables.
			// although there are some exceptions to this: end-node tables do no iref ops at all
			// some further exceptions are defined in isIRefException
			// xrel tables are excluded for speed reasons (by definition there are never tables that reference an x-ref table).
			// ---------------------------------------------------------------------------------------------
			
			
			
			if(!(this.system_table_end_node || this.x_rel_table || isref_exc))
			{
				info_print("iref(" + this.name + ") id:" + id, name);
				ListIterator<DbTable> t_li = referencing_tables.listIterator();
				DbTable tmp_table;
				boolean export_allowed = true;
		
				while(t_li.hasNext())
				{
					tmp_table = t_li.next();
					
					// also: system tables can only export other system tables					
					if(this.system_table && !tmp_table.system_table)
					{
						export_allowed = false;
					}
					else
					{
						export_allowed = true;
					}
					
//					if(this.name.equals("attribute") && tmp_table.name.equals("spectrum"))
//					System.out.println("gotcha");
					
//					if(this.name.equals("attribute"))
//						System.out.println("gotcha");
					
					
					// check the special iref exceptions
					if(this.st.isIRefException(this.name, tmp_table.name))
					{
						export_allowed = false;
					}
					
				
					
					if(export_allowed)
					{					
						
						try {
							stmt = SQL.createStatement();
							String query;
							ResultSet rs;
					
							// search table rows that are referencing this table
							query = tmp_table.get_search_referencing_table_query(name, id);
								
							rs = stmt.executeQuery(query);
							
							ArrayList<Integer> referencing_ids = new ArrayList<Integer>();
						
							while (rs.next()) {
					
								int referencing_id = rs.getInt(1);		
								referencing_ids.add(referencing_id);								
							}
							
							rs.close();
							stmt.close();
							
							Iterator<Integer> it = referencing_ids.iterator();
							
							while(it.hasNext())
							{
								int referencing_id = it.next();
								// export
								tmp_table.export(referencing_id, this);		
							}
							
						
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}		
						
					}
				}
			}
			else
			{
				//info_print(name + " (stop)");
			}	
			
			
//			try {
//				stmt.close();
//			} catch (SQLException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
		}
		
		cex.level--;
		
	}	
	
	boolean is_referencing(String table_name)
	{
		boolean references = false;
		
		// search through all foreign keys to find if the given table is referenced
		FkTableField fk;
		ListIterator<FkTableField> li = FKs.listIterator();

		while(li.hasNext())
		{
			fk = li.next();
			//System.out.println(fk.referenced_table + "," + fk.referencing_table + "," + fk.name);
			if(fk.referenced_table.equals(table_name))
			{
				references = true;
			}
		}
		
		return references;
	}
	
	// return the foreign key field that is referencing the supplied table_name
	// -> should actually return a list because there can be more than one field referencing the supplied table!
	FkTableField get_referencing_fk(String table_name)
	{
		FkTableField fk = null;

		ListIterator<FkTableField> li = FKs.listIterator();
		FkTableField fk_tmp;

		while(li.hasNext())
		{
			fk_tmp = li.next();
			if(fk_tmp.referenced_table.equals(table_name))
			{
				fk = fk_tmp;
			}
		}
	
		return fk;
	}
	
	String get_search_referencing_table_query(String referenced_table, int referenced_id)
	{
		String query;
		
		FkTableField fk = get_referencing_fk(referenced_table);
		
		
		if(this.x_rel_table)
		{
			query = SQL.assemble_sql_select_query("distinct " + fk.name, name, fk.name + " = " + Integer.toString(referenced_id));			
		}
		else // normal tables (not crossrelation tables)
		{
			query = SQL.assemble_sql_select_query(PK.get(0).name, name, fk.name + " = " + Integer.toString(referenced_id)); //, " and " `+ PK.get(0).name + "=" + this.PK.get(0));
		}
		
		return query;
	}
	
	String get_count_referencing_table_query(String referenced_table, int referenced_id)
	{
		String query;
		
		FkTableField fk = get_referencing_fk(referenced_table);
		
		query = SQL.assemble_sql_select_query("count(*) " , name, fk.name + " = " + Integer.toString(referenced_id));			
		
		return query;
	}
	

	void export_this(Integer id, DbTable caller)
	{
		String query;
		
		// build SQL query
		if(this.x_rel_table)
		{
			FkTableField fk = get_referencing_fk(caller.name);
			query = SQL.assemble_sql_select_query(this.select_col_names, name, fk.name + " = " + id.toString());			
		}
		else // normal tables (not crossrelation tables)
		{
			query = SQL.assemble_sql_select_query(this.select_col_names, name, PK.get(0).name + " = " + id.toString());
		}	
		
//		if(id == 281509)
//		{
//			int x = 0;
//		}
		
		// read and write data
		try {
			
			Statement stmt = SQL.createStatement();	
			ResultSet rs = stmt.executeQuery(query);
		
			while (rs.next()) {
				
				for (TableField field : all_cols.values()) {
					field.get_value(rs);
				}
				
				// write data to file
				write();
			}
			
			rs.close();
			stmt.close();
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		

		
	}
	
	void write()
	{
		CampaignExport cex = (CampaignExport)(this.cex); // casting, as this is always a campaign export instance ... 
		cex.log_time(this.all_cols.size());
		
		try {
			Hashtable<String, String> table = new Hashtable<String, String>();
			for (TableField tf : all_cols.values()) {
				table.put(tf.name, tf.toString());
			}
			cex.w.write_table(name, table);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	void read_table_data(BufferedReader d) throws IOException
	{
		try
		{
		String line = d.readLine();
		System.out.println(line);
		if (line == null) {
			throw new IOException("Unexpected end of input.");
		}
		while (line.startsWith("<field")) {
			
			// handle data that contains newlines
			// keep reading while the line does not end with </field>
			while (line.indexOf("</field>") == -1)
			{
		 		String extra = d.readLine();
				if (extra ==  null) {
					throw new IOException("Unexpected end of input.");
				}
				line = line + extra;
			}
		
			// extract the content between the <field></field> tags
			String content = "";
			int start_of_content = line.indexOf('>');
			if(start_of_content + 1 != line.length() - 8) // only try to get fields that contain data!
			{
				content = line.substring(start_of_content + 1, line.length() - 8); // - </field>			
			}
			
			// assign the content to the appropriate field
			String fieldname = line.substring(13, start_of_content-1);
			TableField field = all_cols.get(fieldname);
			if (field != null) {
				field.setValueFromString(content);
			} else {
				System.err.println("Unrecognised table field: " + fieldname);
			}
			
			// read next line
			line = d.readLine();
			
		}	
		} catch(IOException e)
		{
			System.err.println("Error while reading table data: " + e.getMessage());
		}
	}
	
	
	int insert_as_new_row() throws SQLException
	{
		boolean insert = true;
		Integer row_id = 0;
		String query;
		ResultSet rs;
		//Statement stmt = db_conn.createStatement();
		CampaignImport cex = (CampaignImport)(this.cex); // casting, as this is always a campaign export instance ... 
		
		//cex.read_row_rep.set_curr_op_desc("Table: " + name);
		
		// if it is a system table, we need to check if insert is needed
		if(system_table)
		{
			insert = !exists();
		}
		
		if(insert)
		{
			
			cex.log_time(this.all_cols.size());
			
			
			//cex.row_rep.set_curr_op_desc("Table: " + name);
			
//			if(this.name.equals("reference_brand"))
//				System.out.println("reference_brand");
			
			String col_str = col_names_without_blobs;
			if(FKs.size() > 0)
			{
				col_str = SQL.comma_conc_strings(col_str, fk_col_names);
			}
			
//			if(this.name.equals("picture"))
//				System.out.println("picture");
				
			query = "insert into " + name + " (" + col_str + ") values (" + get_values_str() + ")";
			
			System.out.println(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()) + query);
			
			Statement stmt = SQL.createStatement();
			stmt.executeUpdate(query);
			
			// selection of the last insert id: this only works for tables with single primary keys
			// i.e. not for the x-rel tables that have a composite primary key
			// Thus, the criterion is: only one PK			
			if(PK.size() == 1)
			{
				rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
				
				while (rs.next())
					row_id = rs.getInt(1);
				
				// insert the old PK and the new PK into the lookup tables
				// insert filebased PK into old list
				int old_pk = (Integer) PK.get(0).value.value;
				key_lookup.get(0).old_keys.add(old_pk); 
				
				// insert current db PK into new list
				key_lookup.get(0).new_keys.add(row_id);		
				
				rs.close();

			}
			
			stmt.close();
			
			// deal with problematic string content by updating the string rows again (not the smoothest way to do it, better would be to do it in the insert statement)
			if(this.string_cols.size() > 0)
			{
				
				ListIterator<TableField> li = string_cols.listIterator();

				while(li.hasNext())
				{
					TableField tf = li.next();
					if(!tf.value.isNull())
					{
						update_row(row_id, tf);
					}
				}
								
				
			}
			
			
			
			
			// update blobs if needed
			// note: this only works for non-x-rel tables (however, the current db schema has no x-rel tables with blobs anyway)
			if(contains_blobs())
			{
				ListIterator<TableField> li = blob_cols.listIterator();

				// we know the order of the structure, so just do x-checks if really the case ...
				while(li.hasNext())
				{
					TableField tf = li.next();
					if(!tf.value.isNull())
					{
						update_row(row_id, tf);
					}
				}				
				
				
			}	
			
		}		
		
		
		return row_id;
		
	}
	
	
	void update_row(int row_id, TableField ... fields) throws SQLException
	{
		for (TableField tf : fields) {
			String update_stm = "UPDATE " + name + " set " + tf.name + " = ? where " + PK.get(0).name + " = " + row_id;
			PreparedStatement statement = SQL.prepareStatement(update_stm);
			
			if (tf.value instanceof IntFieldValue) {
				statement.setInt(1, (Integer)((IntFieldValue)tf.value).value);
			} else if (tf.value instanceof StringFieldValue) {
				statement.setString(1, (String)((StringFieldValue)tf.value).value);
			} else if (tf.value instanceof BlobFieldValue) {
				InputStream refl = new ByteArrayInputStream(((BlobFieldValue)tf.value).value);
				statement.setBinaryStream(1, refl, ((BlobFieldValue)tf.value).value.length);
				try {
					refl.close();
				} catch (IOException e) {
					// not sure why this would happen
					e.printStackTrace();
				}
			}
			
			statement.executeUpdate();
			
			statement.close();
		}
		
		
	}
	
	
	String get_values_str() throws SQLException
	{
		String values = "";
		FkTableField f;
		
		try
		{
			

		// non blob values
		ListIterator<TableField> li = this.non_blob_cols.listIterator();	
		while(li.hasNext())
		{
			TableField tf = li.next();
			if(tf.type_name.equals("varchar") && (tf.not_null_varchar || tf.value.value != null))
			{
				// strings are updated later to deal with problematic content, insert here as dummy in case a non-null constraint exists
				values = SQL.comma_conc_strings(values,"''");
			}
			else
			{
				values = SQL.comma_conc_strings(values,tf.value.insertable_string());
			}			
			
		}
		
		// FK's 		
		ListIterator<FkTableField> fkli = FKs.listIterator();
		while(fkli.hasNext())
		{
			f = fkli.next();
			// get the referenced table
			DbTable ref = cex.get_table(f.referenced_table);
				
			// get the new key if old key is not null
			Integer new_key = 0;
			if(!f.value.isNull())
				new_key = ref.get_new_key(f.referenced_column_name, f.get_id());
			
			id_and_op_struct info = SQL.is_null_key_get_val_and_op(new_key);
			
			values = SQL.comma_conc_strings(values, info.id);		
		}
		}		
		catch(java.util.NoSuchElementException e) {
			System.out.println(e);
		}
		return values;
	}
	
	int get_new_key(String keyname, int old_key) throws SQLException
	{
		int new_key = 0;
		int ind = -1;
		
		// get correct lookup table
		KeyLookup kl = get_keylookup_list(keyname);
		
		if(kl == null)
			throw new SQLException("key lookup table not found!");
		
		// search key in old key list
		ind = kl.old_keys.indexOf(old_key);
		
		if(ind != -1)
		{
			new_key = kl.new_keys.get(ind);	
			//System.out.println("Key change for " + keyname + " " + Integer.toString(old_key) + " -> " + Integer.toString(new_key));
		}
		else
		{
			System.err.println("key exchange failed for column " + keyname + " id " + old_key + " as key entry not found!");
		}
		
		return new_key;
	}
	
	KeyLookup get_keylookup_list(String keyname)
	{
		ListIterator<KeyLookup> li = this.key_lookup.listIterator();
		
		while(li.hasNext())
		{
			KeyLookup kl = li.next();
			
			if(kl.keyname.equals(keyname))
				return kl;
		}
		
		return null;
	}
	
	// checks if this row already exists. If so, the primary keys are entered into the lookup tables
	boolean exists() throws SQLException
	{		
		String query, conds;
		//Statement stmt = db_conn.createStatement();
		ResultSet rs;	
		
		// build conds
		ListIterator<TableField> li = this.cols.listIterator();
		conds = "";
		String op;

		while(li.hasNext())
		{
			TableField f = li.next();
			if(f.value.isNull())
				op = " is ";
			else
				op = " = ";
			
			if(f.value.insertable_string() != null) // e.g. blobs are null when calling insertable_string
			{
			
				if(f.value instanceof StringFieldValue && f.value.isNull())
				{
					conds += "(" + f.name + " is NULL OR " +  f.name + " = '')";
					
				}
				else
				{
					conds += f.name + op + f.value.insertable_string();
				}
				
				
				if(li.hasNext() || FKs.size() > 0)
				{
					conds += " AND ";
				}
			}
			else
			{
				// this is a blob
				//
				// -> If blobs need comparing, it is probably simplest to get the existing blob from the database and compare in Java!!!!
				//
				// if this is the last field and there are no FK's then remove the last AND
				if(!li.hasNext() && FKs.size() == 0)
				{
					conds = conds.substring(0, conds.length() - 5);
				}
			}
		}
		
		// add foreign keys
		ListIterator<FkTableField> fkli = this.FKs.listIterator();
		while(fkli.hasNext())
		{
			FkTableField f = fkli.next();

			// get the referenced table
			DbTable ref = cex.get_table(f.referenced_table);
				
			// get the new key if old key is not null
			Integer new_key = 0;
			if(!f.value.isNull())
				new_key = ref.get_new_key(f.referenced_column_name, f.get_id());
			
			id_and_op_struct info = SQL.is_null_key_get_val_and_op(new_key);
			
			 
			conds += f.name + " " +info.op + " " + info.id;
			
			if(fkli.hasNext())
			{
				conds += " AND ";
			}
		}	
			
		Statement stmt = SQL.createStatement();
		query = SQL.assemble_sql_select_query(this.pk_col_names, name, conds);
		
		rs = stmt.executeQuery(query);
	
		int cnt = 0;
		while (rs.next()) 
		{
			for(int i = 0; i < this.key_lookup.size(); i++)
			{
				// insert filebased PK into old list
				int old_pk = (Integer) PK.get(i).value.value;
				key_lookup.get(i).old_keys.add(old_pk); 
				
				// insert current db PK into new list
				int new_pk = rs.getInt(i+1); // result indices start at 1 ...
				key_lookup.get(i).new_keys.add(new_pk);				
			}
			
			cnt++; 
		}
		
		rs.close();
		stmt.close();
		
		if(cnt == 0)
			return false;
		else
			return true;

	}
	
	
	boolean contains_blobs()
	{
		if(blob_cols.size() > 0)
			return true;
		else
			return false;
	}

	public boolean isDummy() {
		// TODO Auto-generated method stub
		return isDummy ;
	}
	
}
