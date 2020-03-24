package ch.specchio.eav_db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;

import ch.specchio.types.*;

import com.googlecode.cqengine.*;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.QueryFactory;
import org.joda.time.DateTime;

import static com.googlecode.cqengine.query.QueryFactory.*;

import javax.xml.transform.Result;


public class Attributes {
	
	private SQL_StatementBuilder SQL; 
	
	ArrayList<attribute> attributes;
	ArrayList<Units> units;
	ArrayList<Category> categories;

	// CREATE THE COLLECTION
	ConcurrentIndexedCollection<attribute> cqEngine_attributes;

	private boolean lists_are_filled = false;
	
	public boolean new_attributes_were_inserted = false;

	public int boolean_unit_id = -1;

	
	public Attributes(SQL_StatementBuilder SQL) throws SQLException
	{
		this.SQL = SQL;
		attributes = new ArrayList<attribute>();
		units = new ArrayList<Units>();
		categories = new ArrayList<Category>();
		fill_lists();
	}
	
	// required to call upon connection to a new database
	synchronized public void clear_lists()
	{
		attributes.clear();
		units.clear();
		lists_are_filled = false;
	}
	
	
	synchronized private void fill_lists() throws SQLException
	{
		if (lists_are_filled) return;
		
		
		String query = "select unit_id, name, description, short_name from unit";
		
		// create a list of existing attributes
		Statement stmt = SQL.createStatement();
		ResultSet rs = stmt.executeQuery(query);

		
		while (rs.next()) 
		{
			Units u = new Units();
			
			u.id = rs.getInt(1);
			u.name = rs.getString(2);
			u.description = rs.getString(3);
			u.short_name = rs.getString(4);
			
			if(u.short_name.equals("Boolean"))
				this.boolean_unit_id = u.id;
			
			
			units.add(u);
					
		}
		
		rs.close();			
		
		
		query = "select * from information_schema.tables where table_schema = database() and table_name = 'blob_data_type'";
		boolean blob_data_type_exists = false;
		rs = stmt.executeQuery(query);
		while (rs.next()) 
		{
			blob_data_type_exists = true;
		}
		
		
		//query = "select a.attribute_id, a.name, c.category_id, c.name, c.string_val, a.default_unit_id, a.default_storage_field, a.description, a.cardinality" + (blob_data_type_exists ? ", b.data_type_name" : "") + " from attribute a, category c" + (blob_data_type_exists ? ", blob_data_type b" : "") + " where a.category_id = c.category_id" + (blob_data_type_exists ? " and a.blob_data_type_id = c.blob_data_type_id" : "") + " order by a.name";
		
		query = "select a.attribute_id, a.name, c.category_id, c.name, c.string_val, a.default_unit_id, a.default_storage_field, a.description, a.cardinality" + (blob_data_type_exists ? ", b.data_type_name" : "") + " from category c,  attribute a " + (blob_data_type_exists ? "left join blob_data_type b on b.blob_data_type_id = a.blob_data_type_id" : "") + " where a.category_id = c.category_id order by a.name";
		
		rs = stmt.executeQuery(query);
		
		while (rs.next()) 
		{
			attribute a = new attribute();
			
			a.id = rs.getInt(1);
			a.name = rs.getString(2);
			a.category_id = rs.getInt(3);
			a.cat_name = rs.getString(4);
			a.cat_string_val = rs.getString(5);
			a.default_unit_id = rs.getInt(6);
			a.default_storage_field = rs.getString(7);
			a.description = rs.getString(8);
			a.cardinality = rs.getInt(9);
			if(blob_data_type_exists) 
			{
				String blob_data_type = rs.getString(10);
				if(blob_data_type != null)
					a.blob_data_type = blob_data_type;
			}

			// special case: handling of boolean values stored in int_val fields
			if(a.default_unit_id == this.boolean_unit_id) 
				a.is_boolean_value = true;
			
			attributes.add(a);
					
		}
		
		rs.close();		
		
		

		
		query = "select category_id, name from category order by name";
		
		rs = stmt.executeQuery(query);
		
		while (rs.next()) 
		{
			categories.add(new Category(rs.getInt(1), rs.getString(2)));
		}
		
		rs.close();	
		stmt.close();
		
		lists_are_filled = true;
		
	}
	
	
	
	public ArrayList<attribute> getAttributes() {
		return attributes;
	}

	public synchronized int get_attribute_id(String name)
	{
		return find_in_list(name);	
	}

	public synchronized ArrayList<Integer> get_attribute_ids(String name)
	{
		return find_all_occurences_in_list(name);	
	}	
	
	synchronized public attribute get_attribute_info(Integer attribute_id) {
		return this.find_in_list(attribute_id);
	}
	
	synchronized public attribute get_attribute_info(String attribute_name, String category_name) {

		return  find_in_list(attribute_name, category_name, "");	
		
	}	
	
	synchronized public attribute get_attribute_info(String attribute_name, String category_name, String category_value) {

		return  find_in_list(attribute_name, category_name, category_value);	
		
	}
	
	
	
	public String get_default_storage_field(int attribute_id)
	{
		
		attribute a = find_in_list(attribute_id);
		
		return (a != null)? a.default_storage_field : "string_val";
		
	}
	
	
	synchronized int get_attribute_id(String attribute_name, String category_name, String category_value)
	{
		attribute a = get_attribute_info(attribute_name, category_name, category_value);		
		return a.id;
	}	
	
	
	public synchronized int get_unit_id(String name)
	{
		for (Units u : units) {
			if (u.name != null && u.name.equals(name))
				return  u.id;
			if (u.short_name != null && u.short_name.equals(name))
				return u.id;
		}
		
		return 0;
		
	}
	
	
	public synchronized Units get_units(attribute a)
	{
		if(a.default_unit_id == 0)
		{
			Units u = new Units();
			u.id= 0;
			u.name = null;
			return u;
		}
		
		
		for (Units u : units) {
			if (u.id == a.default_unit_id) {
				return u;
			}
		}
		
		return null;
	}
		
	
	private synchronized int find_in_list(String name)
	{
		ListIterator<attribute> li = attributes.listIterator();
		
		attribute a = null;
		
		boolean found = false;
		
		while(li.hasNext() && !found)
		{
			a = li.next();
			
			if (a.name.equals(name)) found = true;
			
		}
		
		if (found)
			return a.id;
		else
			return 0;
		
	}

	private synchronized attribute find_in_list(Integer id)
	{
		ListIterator<attribute> li = attributes.listIterator();
		
		attribute a = null;
		
		boolean found = false;
		
		while(li.hasNext() && !found)
		{
			a = li.next();
			
			if (a.id == id) found = true;
			
		}

		return a;

		
	}	
	
	private synchronized ArrayList<Integer> find_all_occurences_in_list(String name)
	{
		ArrayList<Integer> ids = new ArrayList<Integer>();
		
		ListIterator<attribute> li = attributes.listIterator();
		
		attribute a = null;

		
		while(li.hasNext())
		{
			a = li.next();
			
			if (a.name.equals(name)) ids.add(a.id);
			
		}
			return ids;
		
	}	
	
	
	private synchronized attribute find_in_list(String attribute_name, String category_name, String category_value)
	{
		ListIterator<attribute> li = attributes.listIterator();
		
		attribute a = null;
		
		boolean found = false;
		
		while(li.hasNext() && !found)
		{
			a = li.next();
			
			if (a.name.equals(attribute_name) && a.cat_name.equals(category_name) && a.cat_string_val.equals(category_value)) found = true;
			
		}
		
		if (found)
			return a;
		else
			return null;
		
	}		
	
	
	private synchronized attribute find_in_list(int attribute_id)
	{
		ListIterator<attribute> li = attributes.listIterator();
		
		attribute a = null;
		
		boolean found = false;
		
		while(li.hasNext() && !found)
		{
			a = li.next();
			
			if (a.id == attribute_id) found = true;
			
		}
		
		if (found)
			return a;
		else
			return null;
		
	}	
	

	public void define_default_storage_fields()
	{
		
		try {
			Statement stmt = SQL.createStatement();
			Statement update_stmt = SQL.createStatement();

		
		ArrayList<Integer> attr_id_list = new ArrayList<Integer>();
		
		// get all attributes that have no default storage field defined
		String query = "select attribute_id from attribute where default_storage_field is null";
		
		ResultSet rs;

			rs = stmt.executeQuery(query);	
			
		
			while (rs.next()) {
								
				attr_id_list.add(rs.getInt(1));
				
			}

			rs.close();				
			
		
		
		// for each of these, find the fields that are used for storage of this attribute (i.e. the non-null fields)
		ListIterator<Integer> li = attr_id_list.listIterator();
		
		int[] cnts = new int[5];
		boolean set = false;
		
		ArrayList<String> fields = new ArrayList<String>();
		fields.add("int_val");fields.add("double_val");fields.add("string_val");fields.add("binary_val");fields.add("datetime_val");
		
		while(li.hasNext())
		{
			
			int attr_id = li.next();
			
			query = "select count(eav.int_val), count(eav.double_val), count(eav.string_val), count(eav.binary_val), count(eav.datetime_val) from eav where attribute_id = " + attr_id;
			
			rs = stmt.executeQuery(query);	
			
			while(rs.next())
			{
				for(int i=0;i<5;i++)
				{
					cnts[i] =  rs.getInt(i+1);
					
					if(cnts[i] > 0)
					{
						if(set)
						{
							System.out.println("Attribute" + attr_id + " (attr. id) stored in different fields in EAV!!!");
							break;
						}
						
						
						// update the attribute
						query = "update attribute set default_storage_field = '" + fields.get(i) + "' where attribute_id = " + attr_id;
						
						update_stmt.executeUpdate(query);
						
						set = true;
						
					}
					
					
				}
			}

			set = false;
			rs.close();
			
			
			
		}
		
		
		
		stmt.close();
		update_stmt.close();
		
		new_attributes_were_inserted = false;
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
		
	}

	public ArrayList<attribute> get_attributes(String category_name) {
		
		ArrayList<attribute> matching_attr = new ArrayList<attribute>();
		
		ListIterator<attribute> li = attributes.listIterator();
		
		attribute a = null;
		
		while(li.hasNext())
		{
			a = li.next();
			
			if (a.cat_name.equals(category_name))
			{
				matching_attr.add(a);
			}
			
		}
		
		return matching_attr;
	}
	
	
	public ArrayList<Category> getCategories()
	{
		return categories;
	}

	public ArrayList<Category> getNonNullCategories(ArrayList<Integer> spectrumIds, SQL_StatementBuilder sqlStatementBuilder) throws SQLException {
		ArrayList<Category> nonNullCats = new ArrayList<>();
		String query = "SELECT DISTINCT(cat.category_id), cat.name FROM category AS cat " +
		"INNER JOIN ( " +
				"SELECT attr.category_id FROM attribute AS attr " +
				"INNER JOIN ( " +
					"SELECT eIn.attribute_id FROM eav AS eIn " +
					"INNER JOIN spectrum_x_eav AS sxe " +
					"ON eIn.eav_id = sxe.eav_id " +
					"WHERE sxe.spectrum_id IN (" + SQL.conc_ids(spectrumIds) + ") " +
				") AS j1 " +
				"ON attr.attribute_id = j1.attribute_id " +
		") AS j2 " +
		"ON j2.category_id = cat.category_id " +
		"ORDER BY cat.name";
		PreparedStatement stmt = sqlStatementBuilder.prepareStatement(query);

		ResultSet rs = stmt.executeQuery(query);

		while (rs.next())
		{
			nonNullCats.add(new Category(rs.getInt(1), rs.getString(2)));
		}

		rs.close();
		stmt.close();
		return nonNullCats;
	}

	public ArrayList<attribute> getNonNullAttributes(ArrayList<Integer> spectrumIds, SQL_StatementBuilder sqlStatementBuilder) throws SQLException {

		ArrayList<attribute> nonNullAttrs = new ArrayList<>();
//		String query = " SELECT DISTINCT(eIn.attribute_id) FROM eav AS eIn " +
//			"INNER JOIN spectrum_x_eav AS sxe " +
//			"ON eIn.eav_id = sxe.eav_id " +
//			"WHERE sxe.spectrum_id IN (" + SQL.conc_ids(spectrumIds) + ") ";

		String query = "SELECT attr.name, attr.default_storage_field, j1.* FROM attribute AS attr " +
				"INNER JOIN (" +
				"SELECT eIn.attribute_id, MIN(eIn.int_val), MAX(eIn.int_val), MIN(eIn.double_val), " +
				"MAX(eIn.double_val), MIN(eIn.datetime_val), MAX(eIn.datetime_val) FROM eav AS eIn " +
				"INNER JOIN spectrum_x_eav AS sxe " +
				"ON eIn.eav_id = sxe.eav_id " +
				"WHERE sxe.spectrum_id IN (" + SQL.conc_ids(spectrumIds) + ") " +
				"GROUP BY eIn.attribute_id " +
				") AS j1 " +
				"ON j1.attribute_id = attr.attribute_id " +
				"WHERE default_storage_field NOT IN ('taxonomy_id', 'spatial_val')";

		PreparedStatement stmt = sqlStatementBuilder.prepareStatement(query);

		ResultSet rs = stmt.executeQuery(query);

		while (rs.next())
		{
			attribute attr = find_in_list(rs.getInt(3));
			attr.setMIN_INT_VAL(rs.getString(4));
			attr.setMAX_INT_VAL(rs.getString(5));
			attr.setMIN_DOUBLE_VAL(rs.getString(6));
			attr.setMAX_DOUBLE_VAL(rs.getString(7));
			attr.setMIN_DATETIME_VAL(rs.getString(8));
			attr.setMAX_DATETIME_VAL(rs.getString(9));
			nonNullAttrs.add(attr);
		}

		rs.close();
		stmt.close();
		return nonNullAttrs;
	}

	public ArrayList<attribute> createFilterCollection(ArrayList<Integer> spectrumIds, SQL_StatementBuilder sqlStatementBuilder) throws SQLException {

		ArrayList<attribute> nonNullAtt = getNonNullAttributes(spectrumIds, sqlStatementBuilder);
		HashMap<Integer, SpectrumAndAttributes>
		String query = null;
		for(attribute at : nonNullAtt){

			query = "SELECT spectrum_id, " + at.default_storage_field + " FROM filter_spectrum_view " +
					"WHERE spectrum_id IN (" + SQL.conc_ids(spectrumIds) + ") " +
					"AND attribute_id = " + at.getId();
			PreparedStatement preparedStatement = sqlStatementBuilder.prepareStatement(query);
			ResultSet rs = preparedStatement.executeQuery();
			while(rs.next()){

			}
		}
		// SETUP THE QUERY FOR THE CURRENT IDS
		String query = "SELECT * FROM filter_spectrum_view " +
				"WHERE spectrum_id IN (" + SQL.conc_ids(spectrumIds) + ") " +
				"ORDER BY spectrum_id ASC";
		PreparedStatement preparedStatement = sqlStatementBuilder.prepareStatement(query);
		ResultSet rs = preparedStatement.executeQuery();
//		cqEngine_attributes = new ConcurrentIndexedCollection<attribute>();
//		// CREATE AN INDEX
//		cqEngine_attributes.addIndex(NavigableIndex.onAttribute(attribute.SPECTRUM_ID));
//		cqEngine_attributes.addIndex(NavigableIndex.onAttribute(attribute.INT_VALUE));
//		cqEngine_attributes.addIndex(NavigableIndex.onAttribute(attribute.DOUBLE_VALUE));
//		cqEngine_attributes.addIndex(NavigableIndex.onAttribute(attribute.NAME));

		while(rs.next()){
			attribute newAttr = new attribute();
			newAttr.spectrumId =  rs.getInt(1);
			newAttr.id = rs.getInt(3);
			newAttr.name = rs.getString(4);
			newAttr.int_val = rs.getInt(5);
			newAttr.double_val = rs.getDouble(6);
//			newAttr.string_val = rs.getString(6);
//			newAttr.binary_val = rs.getString(7);
// 			DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
//			newAttr.dt_val = DateTime.parse(rs.getString(8), fmt);
			cqEngine_attributes.add(newAttr);
		}
// and(contains(Car.DESCRIPTION, "flat tyre"), equal(Car.FEATURES, "spare tyre"));
		ArrayList<attribute> results = new ArrayList<>();
		Query<attribute> query1 = and(equal(attribute.NAME, "Irradiance Instability"), between(attribute.DOUBLE_VALUE, 0.0, 0.1));
		Iterator<attribute> matching = cqEngine_attributes.retrieve(query1).iterator();
		while(matching.hasNext()){
			results.add(matching.next());
		}
		results.size();

		results.clear();
		Query<attribute> query2 = and(and(equal(attribute.NAME, "Irradiance Instability"), between(attribute.DOUBLE_VALUE, 0.0, 0.1)), and(equal(attribute.NAME, "PCB Temperature"), between(attribute.DOUBLE_VALUE, 12.0, 13.0)));
		Iterator<attribute> matching2 = cqEngine_attributes.retrieve(query2).iterator();
		while(matching2.hasNext()){
			attribute thisAttr = matching.next();
			int specId = thisAttr.spectrumId;
			if(results.contains(specId)){
				System.out.println("ALREADY PRESENT");
			} else{
				results.add(specId);
			}
		}
		results.size();



		System.out.println(cqEngine_attributes.getIndexes());

//		equal(Car.DOORS, 4)
		// CREATE A QUERY
//		Query<attribute> spectrumQuery = null;

	}
	
	
	public int insert_attribute(String attribute_name, String category_name, String category_value) throws SQLException {
			
		// might ask the user at this point to supply a default unit
		int id = 0;
			
		// insert new attribute
		String query = "insert into attribute (name, description, category_id) values(" +
				SQL.conc_values(new String[] {attribute_name, ""}) +
				", (select category_id from category c where c.name = " + SQL.quote_string(category_name) +
				" AND c.string_val = " +  SQL.quote_string(category_value) + "))";
	
		Statement stmt = SQL.createStatement();
		stmt.executeUpdate(query);
				
		ResultSet rs;
		rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
				
		while (rs.next())
			id = rs.getInt(1);
				
		rs.close();
		stmt.close();		
					
		attribute a = new attribute();
		a.id = id;
		a.name =attribute_name;
		a.category_id = 0; // do we need this?
		a.cat_name = category_name;
		a.cat_string_val = category_value;
			
		attributes.add(a);		
			
		new_attributes_were_inserted = true;
		
		return id;
		
	}
	

	// insert new unit
	public int insert_unit(String name)
	{
		int id = 0;
		
		String query = "insert into unit (short_name) values(" + SQL.quote_string(name) + ")";
		
		ResultSet rs;
		try {
			Statement stmt = SQL.createStatement();
			stmt.executeUpdate(query);
			
			rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			
			while (rs.next())
				id = rs.getInt(1);
			
			rs.close();
			stmt.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		Units u = new Units();
		u.id = id;
		u.name =name;
		
		units.add(u);					
	
		return id;
	}
	

}
