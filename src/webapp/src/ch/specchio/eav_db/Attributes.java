package ch.specchio.eav_db;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.*;
import java.text.ParseException;

import java.util.ArrayList;

import java.util.ListIterator;


import ch.specchio.types.*;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import static com.googlecode.cqengine.query.QueryFactory.*;

import com.googlecode.cqengine.index.navigable.NavigableIndex;

import com.googlecode.cqengine.query.Query;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


public class Attributes {
	
	private SQL_StatementBuilder SQL; 
	
	ArrayList<attribute> attributes;
	ArrayList<Units> units;
	ArrayList<Category> categories;

	private boolean lists_are_filled = false;
	
	public boolean new_attributes_were_inserted = false;

	public int boolean_unit_id = -1;

	// CQE Collection
	private IndexedCollection<SpectrumXAttributes> spectrumXAttributes = new ConcurrentIndexedCollection<SpectrumXAttributes>();

	
	public Attributes(SQL_StatementBuilder SQL) throws SQLException
	{
		this.SQL = SQL;
		attributes = new ArrayList<attribute>();
		units = new ArrayList<Units>();
		categories = new ArrayList<Category>();
		fill_lists();
		addIndices();
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

	public void createFilterCollection(ArrayList<Integer> spectrumIds, ArrayList<Integer> attributeIds, SQL_StatementBuilder sqlStatementBuilder) throws SQLException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, ClassNotFoundException, ParseException {
		spectrumXAttributes.clear();
		String query = "SELECT * FROM filter_spectrum_view WHERE spectrum_id IN(" + SQL.conc_ids(spectrumIds) + ") " +
				"AND attribute_id IN(" + SQL.conc_ids(attributeIds) + ")";
		PreparedStatement pstmt = sqlStatementBuilder.prepareStatement(query);
		ResultSet rs = pstmt.executeQuery();
		int spectrum_id;
		while(rs.next()){
			spectrum_id = rs.getInt(1);
			Query<SpectrumXAttributes> query1 = in(SpectrumXAttributes.SPECTRUM_ID, spectrum_id);
			SpectrumXAttributes selectedAttribute;
			try{
				selectedAttribute = spectrumXAttributes.retrieve(query1).uniqueResult();
				handleQueryRow(rs, selectedAttribute);
			} catch (NullPointerException ex){
				selectedAttribute = new SpectrumXAttributes(spectrum_id);
				handleQueryRow(rs, selectedAttribute);
				spectrumXAttributes.add(selectedAttribute);
			} catch (com.googlecode.cqengine.resultset.common.NoSuchObjectException ex){
				selectedAttribute = new SpectrumXAttributes(spectrum_id);
				handleQueryRow(rs, selectedAttribute);
				spectrumXAttributes.add(selectedAttribute);
			}

		}
		testCollection();
	}

	private void handleQueryRow(ResultSet rs, SpectrumXAttributes selectedAttribute) throws SQLException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		int attribute_id;
		String setter;
		Method method;
		String defaultStorage;
		attribute_id = rs.getInt(3);
		setter = "set_" + attribute_id;
		defaultStorage = rs.getString(5);
		switch(defaultStorage) {
			case ("int_val"):
				method = selectedAttribute.getClass().getMethod(setter, int.class);
				method.invoke(selectedAttribute, rs.getInt(6));
				break;
			case ("double_val"):
				method = selectedAttribute.getClass().getMethod(setter, double.class);
				method.invoke(selectedAttribute, rs.getDouble(7));
				break;
			case ("string_val"):
				method = selectedAttribute.getClass().getMethod(setter, String.class);
				method.invoke(selectedAttribute, rs.getString(8));
				break;
			case ("binary_val"):
				method = selectedAttribute.getClass().getMethod(setter, String.class);
				method.invoke(selectedAttribute, rs.getString(9));
				break;
			case ("datetime_val"):
				String dt = rs.getString(10);
				DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.S");
				DateTime date = dateTimeFormatter.parseDateTime(dt);
				method = selectedAttribute.getClass().getMethod(setter, long.class);
				method.invoke(selectedAttribute, date.getMillis());
				break;
		}
	}

	public void testCollection(){
		Query<SpectrumXAttributes> query = and(between(SpectrumXAttributes.ATTRIBUTE_399, 30.0, 31.0), between(SpectrumXAttributes.ATTRIBUTE_398, 20.0, 21.00));
		com.googlecode.cqengine.resultset.ResultSet<SpectrumXAttributes> rs = spectrumXAttributes.retrieve(query);
	}


	public ArrayList<Integer> findMatchingSpectra(ArrayList<Integer> spectrumIds, ArrayList<QueryAttribute> queryAttributList, SQL_StatementBuilder sqlStatementBuilder) throws SQLException {


		ArrayList<Integer> foundSpectrumIds = new ArrayList<>();
		String singleQuery = null;

		int count = 0;
		for(QueryAttribute qt : queryAttributList){
			if(count == 0){
				singleQuery = "SELECT spectrum_id FROM filter_spectrum_view " +
						"WHERE spectrum_id IN (" + SQL.conc_ids(spectrumIds) + ") " +
						"AND attribute_id = " + qt.getAttributeId() + " " +
						"AND " + qt.getDefaultStorageField() + " BETWEEN " + qt.getMinVal() + " " +
						"AND " + qt.getMaxVal();
			} else{
				singleQuery = "SELECT spectrum_id FROM filter_spectrum_view " +
						"WHERE spectrum_id IN (" + SQL.conc_ids(foundSpectrumIds) + ") " +
						"AND attribute_id = " + qt.getAttributeId() + " " +
						"AND " + qt.getDefaultStorageField() + " BETWEEN " + qt.getMinVal() + " " +
						"AND " + qt.getMaxVal();
			}
			foundSpectrumIds.clear();
			PreparedStatement pstmt = sqlStatementBuilder.prepareStatement(singleQuery);
			ResultSet rs = pstmt.executeQuery();
			while(rs.next()){
				foundSpectrumIds.add(rs.getInt(1));
			}
			count++;
		}
		return foundSpectrumIds;
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
	
	private void addIndices(){

		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_1));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_2));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_3));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_4));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_5));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_6));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_7));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_8));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_9));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_10));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_11));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_12));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_13));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_14));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_15));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_16));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_17));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_18));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_19));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_20));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_21));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_22));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_23));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_24));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_25));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_26));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_27));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_28));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_29));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_30));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_31));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_32));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_33));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_34));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_35));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_36));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_37));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_38));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_40));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_41));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_42));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_43));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_44));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_45));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_46));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_47));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_50));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_51));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_52));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_53));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_54));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_55));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_56));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_57));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_58));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_59));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_60));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_61));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_62));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_63));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_64));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_65));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_66));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_67));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_68));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_69));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_70));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_71));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_72));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_73));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_74));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_75));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_76));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_77));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_78));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_79));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_80));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_81));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_82));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_83));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_84));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_86));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_87));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_88));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_89));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_90));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_91));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_92));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_93));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_94));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_95));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_96));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_97));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_98));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_99));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_100));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_101));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_102));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_103));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_104));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_105));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_106));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_107));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_108));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_109));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_110));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_111));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_112));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_113));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_114));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_115));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_116));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_117));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_118));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_119));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_120));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_121));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_122));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_123));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_124));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_125));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_126));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_127));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_128));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_129));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_130));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_131));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_132));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_133));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_134));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_135));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_136));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_137));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_138));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_139));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_140));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_141));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_142));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_143));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_144));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_145));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_146));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_147));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_148));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_149));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_150));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_151));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_152));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_153));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_154));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_155));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_156));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_157));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_158));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_159));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_160));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_161));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_162));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_163));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_164));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_165));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_166));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_167));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_168));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_169));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_170));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_171));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_172));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_173));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_174));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_175));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_176));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_177));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_178));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_179));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_180));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_181));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_182));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_183));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_184));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_185));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_186));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_187));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_188));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_189));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_190));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_191));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_192));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_193));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_194));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_195));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_196));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_197));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_198));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_199));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_200));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_201));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_202));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_203));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_204));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_205));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_206));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_207));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_208));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_209));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_210));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_211));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_212));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_213));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_214));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_215));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_216));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_217));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_218));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_219));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_220));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_221));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_222));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_223));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_224));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_225));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_226));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_227));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_228));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_229));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_230));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_231));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_232));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_233));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_234));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_235));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_236));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_237));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_238));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_239));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_240));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_241));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_242));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_243));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_244));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_245));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_246));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_247));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_248));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_249));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_250));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_251));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_252));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_253));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_254));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_255));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_256));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_257));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_258));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_259));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_260));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_261));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_262));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_263));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_264));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_265));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_266));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_267));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_268));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_269));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_270));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_271));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_272));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_273));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_274));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_275));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_276));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_277));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_278));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_279));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_280));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_281));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_282));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_283));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_284));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_285));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_286));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_287));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_288));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_289));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_290));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_291));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_292));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_293));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_294));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_295));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_296));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_297));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_298));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_299));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_300));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_301));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_302));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_303));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_304));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_305));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_306));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_307));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_308));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_309));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_310));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_311));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_312));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_313));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_314));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_315));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_316));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_317));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_318));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_319));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_320));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_321));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_322));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_323));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_324));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_325));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_326));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_327));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_328));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_329));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_330));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_331));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_332));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_333));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_334));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_335));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_336));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_337));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_338));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_339));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_340));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_341));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_342));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_343));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_344));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_345));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_346));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_347));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_348));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_349));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_350));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_351));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_352));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_353));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_354));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_355));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_356));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_357));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_358));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_359));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_360));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_361));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_362));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_363));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_364));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_365));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_366));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_367));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_368));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_369));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_370));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_371));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_372));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_373));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_374));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_375));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_376));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_377));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_378));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_379));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_380));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_381));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_382));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_383));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_384));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_385));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_386));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_387));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_388));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_389));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_390));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_391));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_392));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_393));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_394));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_395));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_396));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_397));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_398));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_399));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_400));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_401));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_402));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_403));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_404));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_405));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_406));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_407));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_408));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_409));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_410));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_411));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_412));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_413));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_414));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_415));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_416));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_417));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_418));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_419));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_420));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_421));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_422));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_423));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_424));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_425));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_426));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_427));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_428));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_429));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_430));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_431));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_432));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_433));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_434));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_435));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_436));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_437));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_438));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_439));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_440));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.ATTRIBUTE_441));
		spectrumXAttributes.addIndex(NavigableIndex.onAttribute(SpectrumXAttributes.SPECTRUM_ID));

	}
}
