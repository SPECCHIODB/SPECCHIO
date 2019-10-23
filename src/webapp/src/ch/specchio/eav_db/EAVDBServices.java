package ch.specchio.eav_db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.ListIterator;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ch.specchio.factories.SPECCHIOFactory;
import ch.specchio.factories.SPECCHIOFactoryException;
import ch.specchio.types.EAVTableAndRelationsInfoStructure;
import ch.specchio.types.MetaDate;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.MetaSpatialGeometry;
import ch.specchio.types.PdfDocument;
import ch.specchio.types.attribute;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.Metadata;


public class EAVDBServices extends Thread {
	
	public SQL_StatementBuilder SQL;
	public Attributes ATR;
	public ArrayList<String> aliases = new ArrayList<String>();
	
	//ArrayList<MetaParameter> known_metaparameters = new ArrayList<MetaParameter>();
	private static Hashtable<String, ArrayList<MetaParameter>> known_metaparameters_hash = new Hashtable<String, ArrayList<MetaParameter>>();
	
//	private String primary_x_eav_tablename = "frame_x_eav";
//	private String primary_x_eav_viewname = "frame_x_eav_view";
//	private String primary_id_name = "frame_id";
	EAVTableAndRelationsInfoStructure[] eav_table_infos;
	private String eav_view_name = "eav"; //required for SPECCHIO as inserts work on the view
	private boolean spatially_enabled= false;
	
	private String databaseUserName;
	public SPECCHIOFactory specchioFactory;
	
	public EAVDBServices(SQL_StatementBuilder SQL, Attributes ATR, String databaseUserName) 
	{				
		this.SQL = SQL;
		this.ATR = ATR;
		this.databaseUserName = databaseUserName;
		eav_table_infos = new EAVTableAndRelationsInfoStructure[2];
		
		String query = "SELECT count(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = (select schema()) AND TABLE_NAME = 'eav' AND COLUMN_NAME = 'spatial_val'";
		
		Statement stmt;
		try {
			stmt = SQL.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				this.spatially_enabled = rs.getBoolean(1);
			}
			rs.close();
			stmt.close();			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
	
	
	public void getMetadataInsertData(int campaign_id, Metadata md) throws SQLException
	{
		
		ArrayList<String> value_strings = new ArrayList<String>();
		double reduction_count = 0;
		
		ArrayList<Integer> eav_ids = new ArrayList<Integer>();
		
		// loop through all entries
		ListIterator<MetaParameter> li = md.getEntries().listIterator();

		while(li.hasNext())
		{
			MetaParameter e =  li.next();

			e = this.reduce_redundancy(e);

			if(e.getEavId() == 0)
			{					

				value_strings.add(get_metaparameter_value_string(campaign_id, e));			

			}
			else
			{
				eav_ids.add(e.getEavId());
				reduction_count++;
			}
		}		


		md.insert_value_strings = value_strings;
		md.redundancy_reduced_eav_ids = eav_ids;
		
	}
	
	
	
	/**
	 * Insert metadata into the database Only inserts metaparameters that are new
	 * 
	 * @param campaign_id	the identifier of the campaign to which this metadata belongs
	 * @param md			the metadata to be inserted
	 * 
	 * @return the list of all eav_ids, even when not inserted this time
	 */
	public ArrayList<Integer> insert_metadata_into_db(int campaign_id, Metadata md, boolean is_admin) throws SQLException, IOException {
		
		// prepare insert statement
		String query = "insert into eav_view (campaign_id, attribute_id, int_val, double_val, string_val, binary_val, datetime_val, taxonomy_id, " + (isSpatially_enabled() ? "spatial_val," : "") + " unit_id) values";
		ArrayList<String> value_strings = new ArrayList<String>();
		double reduction_count = 0;
		
		ArrayList<Integer> eav_ids = new ArrayList<Integer>();
//		ArrayList<MetaParameter> multi_insert_eavs = new ArrayList<MetaParameter>();
			// loop through all entries
			ListIterator<MetaParameter> li = md.getEntries().listIterator();
		
			while(li.hasNext())
			{
				MetaParameter e =  li.next();
				
				e = this.reduce_redundancy(e);
				
				if(e.getEavId() == 0)
				{					
					
					value_strings.add(get_metaparameter_value_string(campaign_id, e));			
//					multi_insert_eavs.add(e);	
					
					// check if multi insert is possible
//					if(e.allows_multi_insert())
//					{
//						value_strings.add(get_metaparameter_value_string(campaign_id, e));			
//						multi_insert_eavs.add(e);
//					}
//					else
//					{
//						int id = insert_metaparameter_into_db(campaign_id, e, false, is_admin); // redundancy is already reduced
//						eav_ids.add(id);
//					}
				}
				else
				{
					eav_ids.add(e.getEavId());
					reduction_count++;
				}
			}
			
			if(value_strings.size() > 0)
			{
			
				// carry out the multi insert statement
				query = query + SQL.conc_cols(value_strings);
				
				PreparedStatement ps = SQL.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

				//now update
				ps.executeUpdate();		

				ResultSet rs = ps.getGeneratedKeys();
								
			    int _eav_id = -1;
			    int cnt = 0;
			    while (rs.next()) {
			    	_eav_id = rs.getInt(1);
//			    	multi_insert_eavs.get(cnt++).setEavId(_eav_id);
			    	eav_ids.add(_eav_id);
			    }		
				
			    
			    rs.close();
			
			}
			
//			System.out.println("Redundancy reduction = " + (reduction_count / eav_ids.size() *100) + "%");
		
		
		return eav_ids;
	}	
	
	
	public int insert_metaparameter_into_db(int campaign_id, MetaParameter mp, boolean reduce_redundancy, boolean is_admin) throws SQLException, IOException
	{
		if(reduce_redundancy)
		{
			mp = this.reduce_redundancy(mp);
		}
		
		
		if (mp.getEavId() == 0) // only insert if not yet existing
		{		
			int eav_id = 0;
			get_metaparameter_attribute_and_unit_ids(mp);
			
			if(mp instanceof MetaSpatialGeometry)
			{
				eav_id = insert_eav(campaign_id, (MetaSpatialGeometry) mp);
			}
			else
			{
				eav_id = insert_eav(campaign_id, mp.getDefaultStorageField(), mp.getEAVValue(), mp.getAttributeId(), mp.getUnitId(), is_admin);
			}
			mp.setEavId(eav_id);
		}
		return mp.getEavId();
		
	}
	
	private int insert_eav(int campaign_id, String field_name, Object value, int attribute_id, int unit_id, boolean is_admin) throws SQLException, IOException
	{
		if (field_name.equals("binary_val"))
		{
			return insert_eav_binary(campaign_id, field_name, (Serializable) value, attribute_id, unit_id);
		}
		else
		{
			int eav_id = 0;
			
//			String query = "insert into " + this.eav_view_name + " (campaign_id, attribute_id, " + field_name + ", unit_id) " +
//					"values(" +
//						String.valueOf(campaign_id) + "," +
//						String.valueOf(attribute_id) + "," +
//						SQL.quote_value(value) + "," +
//						Integer.toString(unit_id) +
//					")";
			
			// switch to prepared statement to avoid SQL injections in strings with e.g. backslashes
			String query = "insert into " + (is_admin?"eav":this.eav_view_name) +  " (campaign_id, attribute_id, " + field_name + ", unit_id) " +
					"values(" +
						String.valueOf(campaign_id) + "," +
						String.valueOf(attribute_id) + "," +
						"?" + "," +
						Integer.toString(unit_id) +
					")";
			
		
			PreparedStatement stmt = SQL.prepareStatement(query);
			stmt.setString(1, SQL.get_pstatement_value(value));
			stmt.executeUpdate();
				
			ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
				
			while (rs.next())
				eav_id = rs.getInt(1);
				
			rs.close();
			stmt.close();
			
			return eav_id;
		}

	}	
	
	
	private int insert_eav(int campaign_id, MetaSpatialGeometry mp) throws SQLException, IOException
	{

		int eav_id = 0;

		String query = "insert into " + this.eav_view_name + " (campaign_id, attribute_id, " + mp.getDefaultStorageField() + ", unit_id) " +
				"values(" +
				String.valueOf(campaign_id) + "," +
				String.valueOf(mp.getAttributeId()) + "," +
				mp.getEAVValue() + "," +
				Integer.toString(mp.getUnitId()) +
				")";

		Statement stmt = SQL.createStatement();
		stmt.executeUpdate(query);

		ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");

		while (rs.next())
			eav_id = rs.getInt(1);

		rs.close();
		stmt.close();

		return eav_id;

	}	
	
	
	private int insert_eav_binary(int campaign_id, String field, Serializable value, int attribute_id, int unit_id) throws SQLException, IOException {
		
		int eav_id = 0;
		
		StringBuilder sb = new StringBuilder();
				
		ByteArrayOutputStream baos_ = new ByteArrayOutputStream();
		ObjectOutputStream out_;
		try {
			out_ = new ObjectOutputStream(baos_);
			out_.writeObject(value);

			out_.close();	
			
			String test = baos_.toString();
			
			byte[] bytearr = baos_.toByteArray();
			
			
			
			for (int i = 0; i < bytearr.length; i++) {
								
				int b = bytearr[i] & 0xFF;
				
				String tmp = Integer.toHexString(b);
				
				sb.append(tmp);
				
			}
			
			int x = 0;
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		value = "x'" + sb + "'";		
		
		String query = "insert into " + this.eav_view_name + " (campaign_id, attribute_id, unit_id, " + field + ") " + 
				"values(" + campaign_id + "," + attribute_id + "," + unit_id + ","  + value + ")";
		
		Statement stmt = SQL.createStatement();
		stmt.executeUpdate(query);
			
		ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			
		while (rs.next())
			eav_id = rs.getInt(1);
			
		rs.close();
		stmt.close();
			
//		String update_stm = "UPDATE " + this.eav_view_name + " set " + field + " = ? where eav_id = " + eav_id;
//		//update_stm = "UPDATE eav set " + field + " = ? where eav_id = " + eav_id;
//		PreparedStatement statement = SQL.prepareStatement(update_stm);
//			
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		ObjectOutputStream out = new ObjectOutputStream(baos);
//		out.writeObject(value);
//		out.close();
//			
//		//statement.setBinaryStream(1, new ByteArrayInputStream(baos.toByteArray()), baos.size());
//		statement.setBlob(1, new ByteArrayInputStream(baos.toByteArray()));
//		statement.executeUpdate();			
			
//		rs.close();
//		statement.close();
			
		
		return eav_id;

	}
	
	
	public void update_eav_annotation(int eav_id, String annotation) throws SQLException {
		
		String query = "update " + this.eav_view_name + " set string_val = ? where eav_id = " + eav_id;
		
		PreparedStatement stmt = SQL.prepareStatement(query);
		stmt.setString(1, annotation);
		stmt.executeUpdate();		
		
		stmt.close();
	}
	
	
	
	private void get_metaparameter_attribute_and_unit_ids(MetaParameter mp) throws SQLException
	{
		get_metaparameter_attribute_id(mp);
		
		if(mp.getDefaultUnitId() != 0)
		{
			mp.setUnitId(mp.getDefaultUnitId());
		}
		else
		{
			if (mp.getUnitName() == null) mp.setUnitName("RAW"); // default
				
			mp.setUnitId(ATR.get_unit_id(mp.getUnitName()));
		}				
		
	}


	private int get_metaparameter_attribute_id(MetaParameter mp) throws SQLException {
		
		attribute a = null;
		
		if(mp.getAttributeId() != null)
		{
			// catch the case when the attribute id is already set but attribute name and category info is missing (e.g. creation via Metadata editor)
			a = get_metaparameter_attribute_unit_and_category(mp);
		}
		else
		{
			a = ATR.get_attribute_info(mp.getAttributeName(), mp.getCategoryName(), mp.getCategoryValue());
		}
		
		mp.setAttributeId(a.id);
		mp.setDefaultUnitId(a.default_unit_id);
		if (mp.getDefaultStorageField() == null) {
			mp.setDefaultStorageField(a.getDefaultStorageField());
		}
		
		return a.id;
	}
	
	
	private attribute get_metaparameter_attribute_unit_and_category(MetaParameter mp)
	{
		attribute a = ATR.get_attribute_info(mp.getAttributeId());
		mp.setAttributeName(a.name);
		mp.setDefaultStorageField(ATR.get_default_storage_field(mp.getAttributeId()));
		mp.setCategoryName(a.cat_name);
		mp.setCategoryValue(a.cat_string_val);	
		
		return a;
	}

	
	private String get_metaparameter_value_string(int campaign_id, MetaParameter e) throws SQLException
	{	
		try {
			get_metaparameter_attribute_and_unit_ids(e);
		} catch (SQLException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		
//		if (e.allows_multi_insert() == false)
//		{
//			return null; // Matrix values must be inserted by update statements externally
//		}
//		else
//		{
			// fields: campaign_id, attribute_id, int_val, double_val, string_val, binary_val, datetime_val, taxonomy_id, unit_id
			
			String fieldname = e.getDefaultStorageField();
			String value;

			if(fieldname.equals("spatial_val"))
			{
				value = (String) e.getEAVValue();
			}
			else if(fieldname.equals("binary_val"))
			{
				
				
				Serializable value_ = (Serializable) e.getValue();
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream out;
				try {
					out = new ObjectOutputStream(baos);
					out.writeObject(value_);

					out.close();	
					
					String test = baos.toString();
					
					byte[] bytearr = baos.toByteArray();
					
					
					
					int x = 0;
					
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				
				value = "x'" + "'";
			}
			else
			{
				value = SQL.quote_value(e.getValue());
			}

			StringBuffer query = new StringBuffer("(");
			query.append(Integer.toString(campaign_id));
			query.append(",");
			query.append(e.getAttributeId());
			query.append(",");
			query.append("int_val".equals(fieldname) ? value : "null");
			query.append(",");
			query.append("double_val".equals(fieldname) ? value : "null");
			query.append(",");
			query.append("string_val".equals(fieldname) ? value : "null");
			query.append(",");
			query.append("binary_val".equals(fieldname) ? value : "null");
			query.append(",");
			query.append("datetime_val".equals(fieldname) ? value : "null");
			query.append(",");
			query.append("taxonomy_id".equals(fieldname) ? value : "null");
			query.append(",");
			if(this.isSpatially_enabled())
			{
				query.append("spatial_val".equals(fieldname) ? value : "null");
				query.append(",");	
			}
			query.append(e.getUnitId().toString() + ")");
			
			return query.toString();

//		}
		
	}
	
	public String get_primary_id_name(int metadata_level) {		
		return this.eav_table_infos[metadata_level].primary_id_name;		
	}
	
	public String get_primary_x_eav_viewname(int metadata_level) {		
		return this.eav_table_infos[metadata_level].primary_x_eav_viewname;		
	}	
	
	public String get_primary_x_eav_tablename(int metadata_level) {		
		return this.eav_table_infos[metadata_level].primary_x_eav_tablename;
	}
	
	synchronized public int get_campaign_id_for_eav(int eav_id)
	{
		int campaign_id = 0;
		
		try {
			Statement stmt = SQL.createStatement();
			String query = "select campaign_id from " + this.eav_view_name + " where eav_id=" + Integer.toString(eav_id);
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				campaign_id = rs.getInt(1);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}
		
		return campaign_id;
	}
	
	
	synchronized public void clear_redundancy_list()
	{
		ArrayList<MetaParameter> known_metaparameters = known_metaparameters_hash.get(this.databaseUserName);
		
		if(known_metaparameters != null)
		{
			known_metaparameters.clear();
		}
	}
	
	synchronized public int count_metaparameters(int metadata_level, ArrayList<Integer> primary_ids)
	{
		int count = 0;
		try {
			Statement stmt = SQL.createStatement();
			String query = "select count(" + this.get_primary_id_name(metadata_level) + ") from " + this.get_primary_x_eav_tablename(metadata_level) + " where " + this.get_primary_id_name(metadata_level) + " in (" + SQL.conc_ids(primary_ids) + ")";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				count = rs.getInt(1);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}		
		return count;
		
	}
	
	
	synchronized public MetaParameter reduce_redundancy(MetaParameter mp) throws SQLException {
		
		get_metaparameter_attribute_and_unit_ids(mp);
		
		MetaParameter curr_mp = null;
		boolean matches = false;
		
		ArrayList<MetaParameter> known_metaparameters = known_metaparameters_hash.get(this.databaseUserName);
		
		if(known_metaparameters == null)
		{
			known_metaparameters = new ArrayList<MetaParameter>();
			known_metaparameters_hash.put(databaseUserName, known_metaparameters);
		}
		
		// check if it is already contained in the list
		ListIterator<MetaParameter> li = known_metaparameters.listIterator();
		
		while(li.hasNext() && matches == false)
		{
			curr_mp = li.next();
			
			// attribute must be matching
			if(mp.getAttributeId().equals(curr_mp.getAttributeId()))
			{
		
				boolean equalValues = (mp.getValue() == null && curr_mp.getValue() == null) || (mp.getValue() != null && mp.hasEqualValue(curr_mp)); // mp.getValue().equals(curr_mp.getValue())
				if(mp.getUnitId() == 0) // catches the case where the unit was not set by the user or program (it is later enforced as RAW during insert)
				{
					matches = equalValues;
				}
				else
				{
					matches = mp.getUnitId().equals(curr_mp.getUnitId()) && equalValues;
				}
			}

		}
		
		if(matches)
		{
			return curr_mp;
		}
		else
		{
			known_metaparameters.add(mp);
			return mp;
		}
	}	
	
	
	
	synchronized public void insert_hierarchy_links(PrimaryData pd) throws SQLException
	{	
		Statement stmt = SQL.createStatement();
		String query = "insert into frame_x_eav (frame_id, eav_id) values (" + String.valueOf(pd.get_id()) + ", " + String.valueOf(pd.get_metadata().get_hierarchy_id()) +")";
		//stmt.execute("LOCK TABLES frame_x_eav WRITE");
		stmt.executeUpdate(query);
		
		// insert link from image filename to hierarchy
		query = "insert into eav_x_eav (eav1_id, eav2_id) values (" + String.valueOf(pd.get_metadata().get_first_entry("frame_filename").getEavId()) + ", " + String.valueOf(pd.get_metadata().get_hierarchy_id()) +")";
		stmt.executeUpdate(query);
		//stmt.execute("UNLOCK TABLES");
		
		stmt.close();
	}
	
	/**
	 * Get a list of hierarchy ids, covering all hierarchies above these hierarchies
	 * 
	 * @param ids		the identifier of the desired hierarchy
	 * 
	 * @return a list  of hierarchy ids
	 * 
	 * @throws SPECCHIOFactoryException	
	 */	
	public ArrayList<Integer> getParentHierarchyIds(ArrayList<Integer> ids)
	{
		ArrayList<Integer> out_ids = new ArrayList<Integer>();
		
		for(int id : ids)
		{
			out_ids.addAll(getParentHierarchyIds(id));
		}
		
		return out_ids;
	}
	
	
	/**
	 * Get a list of hierarchy ids, covering all hierarchies above these hierarchies
	 * 
	 * @param ids		the identifier of the desired hierarchy
	 * 
	 * @return a list  of hierarchy ids
	 * 
	 * @throws SPECCHIOFactoryException	
	 */	
	public HashMap<Integer, ArrayList<Integer>> getGroupedParentHierarchyIds(ArrayList<Integer> ids)
	{
		HashMap<Integer, ArrayList<Integer>> hm = new HashMap<Integer, ArrayList<Integer>>();
		
		for(int id : ids)
		{
			hm.put(id, getParentHierarchyIds(id));
		}
		
		return hm;
	}	
		
	
	/**
	 * Get a list of hierarchy ids, covering all hierarchies above this hierarchy
	 * 
	 * @param hierarchy_id		the identifier of the desired hierarchy
	 * 
	 * @return a list  of hierarchy ids, sorted ascending
	 * 
	 * @throws SPECCHIOFactoryException	
	 */	
	public ArrayList<Integer> getParentHierarchyIds(Integer hierarchy_id)
	{
		ArrayList<Integer> ids = new ArrayList<Integer>();
		int parent_id = 0;
		try {
			Statement stmt = SQL.createStatement();
			do
			{
				String query = "select parent_level_id from hierarchy_level where hierarchy_level_id = " + hierarchy_id;
				ResultSet rs = stmt.executeQuery(query);
				while (rs.next()) {
					parent_id = rs.getInt(1);
					if(parent_id != 0)
					{
						ids.add(parent_id);
						hierarchy_id = parent_id;
					}
				}
				rs.close();
			} while (parent_id != 0);			
			
			stmt.close();
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}				
		
		return ids;
	}


	/**
	 * Get a list of hierarchy ids, covering all hierarchies above this spectrum
	 * 
	 * @param spectrum_id		the identifier of the desired spectrum
	 * 
	 * @return a list  of hierarchy ids
	 * 
	 * @throws SPECCHIOFactoryException	
	 */	
	public ArrayList<Integer> getHierarchyIds(Integer spectrum_id)
	{
		return getHierarchyIds(spectrum_id.toString());
	}
	
	
	/**
	 * Get a hierarchy id, above this spectrum
	 * 
	 * @param spectrum_id		the identifiers of the desired spectrum
	 * 
	 * @return hierarchy id
	 * 
	 * @throws SPECCHIOFactoryException	
	 */	
	public Integer getDirectHierarchyId(Integer spectrum_id)
	{
		int id = 0;
		try {
			Statement stmt = SQL.createStatement();
			String query = "select hierarchy_level_id from spectrum where spectrum_id = " + spectrum_id;
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				id = rs.getInt(1);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}				
		return id;
	}		
	
	
	/**
	 * Get hierarchy ids, directly above these spectra
	 * 
	 * @param spectrum_ids		the identifiers of the desired spectra
	 * 
	 * @return hierarchy ids
	 * 
	 */	
	public ArrayList<Integer> getDirectHierarchyIds(ArrayList<Integer> spectrum_ids)
	{
		ArrayList<Integer> ids = new ArrayList<Integer>();
		try {
			Statement stmt = SQL.createStatement();
			String query = "select distinct hierarchy_level_id from spectrum where spectrum_id in (" + SQL.conc_ids(spectrum_ids) + ")";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				ids.add(rs.getInt(1));
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}				
		return ids;
	}			
	
	
	
	/**
	 * Get a list of hierarchy ids, covering all hierarchies above these spectra
	 * 
	 * @param spectrum_ids		the identifiers of the desired spectra
	 * 
	 * @return a list  of hierarchy ids
	 * 
	 * @throws SPECCHIOFactoryException	
	 */	
	public ArrayList<Integer> getHierarchyIds(ArrayList<Integer> spectrum_ids)
	{
		return getHierarchyIds(SQL.conc_ids(spectrum_ids));
	}	
	
	
	/**
	 * Get a list of hierarchy ids, covering all hierarchies above these spectra
	 * 
	 * @param spectrum_ids		the identifiers of the desired spectra
	 * 
	 * @return a list  of hierarchy ids
	 * 
	 * @throws SPECCHIOFactoryException	
	 */	
	private ArrayList<Integer> getHierarchyIds(String spectrum_ids)
	{
		ArrayList<Integer> ids = new ArrayList<Integer>();
		try {
			Statement stmt = SQL.createStatement();
			String query = "select distinct hierarchy_level_id from hierarchy_level_x_spectrum where spectrum_id in (" + spectrum_ids + ")";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				ids.add(rs.getInt(1));
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}				
		
		return ids;
	}		
		
	
	public void set_primary_x_eav_tablename(int metadata_level, String primary_x_eav_tablename, String primary_x_eav_viewname, String primary_id_name, String primary_table_name)
	{
		EAVTableAndRelationsInfoStructure info = new EAVTableAndRelationsInfoStructure();
		
		info.primary_x_eav_tablename = primary_x_eav_tablename;
		info.primary_x_eav_viewname = primary_x_eav_viewname;
		info.primary_id_name = primary_id_name;	
		info.primary_table_name = primary_table_name;
		
		eav_table_infos[metadata_level] = info;

	}
	
	public EAVTableAndRelationsInfoStructure getEAVTableAndRelationsInfoStructure(int metadata_level)
	{
		return eav_table_infos[metadata_level];
	}
	
	public void set_eav_view_name(String eav_view_name)
	{
		this.eav_view_name = eav_view_name;
	}
	
	public String get_eav_view_name()
	{
		return this.eav_view_name;
	}
	
	synchronized public void insert_primary_x_eav(int metadata_level, ArrayList<Integer> primary_ids, int eav_id) throws SQLException
	{
		Integer[] eav_ids = new Integer[1];
		eav_ids[0] = eav_id;
		
		insert_primary_x_eav(metadata_level, primary_ids, eav_ids);
	}
	
	synchronized public void insert_primary_x_eav(int metadata_level, Integer primary_id, int eav_id) throws SQLException
	{
		Integer[] eav_ids = new Integer[1];
		eav_ids[0] = eav_id;
		
		 ArrayList<Integer> primary_ids = new ArrayList<Integer>();
		 primary_ids.add(primary_id);
		
		insert_primary_x_eav(metadata_level, primary_ids, eav_ids);
	}
	
	synchronized public void insert_primary_x_eav(int metadata_level, int primary_id, ArrayList<Integer> eav_ids) throws SQLException {
		
		if(eav_ids.size() > 0) insert_primary_x_eav(metadata_level, primary_id, eav_ids.toArray(new Integer[1]));
		
	}
	
	
	synchronized public void insert_primary_x_eav(int metadata_level, int primary_id, Integer[] eav_ids) throws SQLException
	{	
		if(eav_ids.length > 0)
		{
			String query = "insert into " + get_primary_x_eav_viewname(metadata_level) + " (" + get_primary_id_name(metadata_level) + ", eav_id) values ";
			ArrayList<String> value_strings = new ArrayList<String>();

			// build multi insert string
			for (int eav_id : eav_ids) {
				value_strings.add("(" + String.valueOf(primary_id) + ", " + String.valueOf(eav_id) +")");
			}	

			// carry out the multi insert statement
			query = query + SQL.conc_cols(value_strings);		

			try {
				//stmt.execute("LOCK TABLES frame_x_eav WRITE");
				Statement stmt = SQL.createStatement();
				stmt.executeUpdate(query);
				stmt.close();
				//stmt.execute("UNLOCK TABLES");	
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw(e);
			}
		}
		
	}
	
	// this function expects that the eav_ids apply to each frame_id entry
	synchronized public void insert_primary_x_eav(int metadata_level, ArrayList<Integer> primary_ids, Integer[] eav_ids) throws SQLException
	{			

		String query = "insert into " + get_primary_x_eav_viewname(metadata_level) + " (" + get_primary_id_name(metadata_level) + ", eav_id) values ";
		ArrayList<String> value_strings = new ArrayList<String>();
		
		// build multi insert string
		for (int primary_id : primary_ids) {
			for (int eav_id : eav_ids) {
				value_strings.add("(" + String.valueOf(primary_id) + ", " + String.valueOf(eav_id) +")");
			}	
		}
		
		// carry out the multi insert statement
		query = query + SQL.conc_cols(value_strings);
		Statement stmt = SQL.createStatement();
		stmt.executeUpdate(query);
		stmt.close();
		
	}
	
	// inserts links between spectra and eavs by using the redundancy index
	public void insert_primary_x_eav(int metadata_level, ArrayList<Integer> spectrum_ids,
			ArrayList<ArrayList<Integer>> redundancy_reduced_metaparameter_index_per_spectrum,
			ArrayList<Integer> eav_ids) throws SQLException {
		
		String query = "insert into " + get_primary_x_eav_viewname(metadata_level) + " (" + get_primary_id_name(metadata_level) + ", eav_id) values ";
		StringBuffer value_strings = new StringBuffer();
		
		// iterate all spectrum_ids, look up their index and create value strings
		
		StringBuffer values = new StringBuffer();
		
		ListIterator<Integer> sli = spectrum_ids.listIterator();
		
		while(sli.hasNext())
		{
			ArrayList<Integer> redundancy_index = redundancy_reduced_metaparameter_index_per_spectrum.get(sli.nextIndex());
			
			int spectrum_id = sli.next();
			
			ListIterator<Integer> rili = redundancy_index.listIterator();
			
			while(rili.hasNext())
			{
				int index = rili.next();
				int eav_id = eav_ids.get(index);
				
				value_strings.append("(" + spectrum_id + "," + eav_id + ")");
				
				if(sli.hasNext() || rili.hasNext()) value_strings.append(",");

			}
						
			
		}
		
	//	value_strings.deleteCharAt(value_strings.length());
		
		
		
		// carry out the multi insert statement
		query = query + value_strings.toString();
		Statement stmt = SQL.createStatement();
		stmt.executeUpdate(query);
		stmt.close();				
		
	}

	
	
	synchronized public ArrayList<Integer> get_list_of_processing_levels()
	{
		ArrayList<Integer> list = new ArrayList<Integer>();
		

//		String query = "select distinct int_val from eav where attribute_id = " + ATR.get_attribute_id("processing_level");
//		
//		ResultSet rs;
//
//			try {
//				rs = stmt.executeQuery(query);
//				
//				while (rs.next()) 
//				{
//					list.add(rs.getInt(1));						
//				}
//				
//				rs.close();						
//				
//				
//			} catch (SQLException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		
		
		// hardcoded to speed it up (the above query is way slow!!!)
		list.add(0);
		list.add(1);
		list.add(3);
		
		
		return list;
		
	}
	
	
	// for speed reasons
	synchronized public boolean metafile_exists(String filename, int directory_id)
	{
		String[] file_tokens = filename.split("/");	

		int cnt = 0;	
		
		String query = "select count(*) from eav, eav_x_eav where attribute_id = " + ATR.get_attribute_id("metadata_filename") + " and string_val = '" + file_tokens[file_tokens.length-1] + "'" + " and eav_id = eav1_id and eav2_id = " + String.valueOf(directory_id);		
		
		//System.out.print("_");
		
		ResultSet rs;
		try {
			Statement stmt = SQL.createStatement();
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				cnt = rs.getInt(1);			
			}			
			rs.close();						
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (cnt == 0) return false;
		
		return true;
	}
	
	
	// return hashtable with a boolean if existing or not for all metafiles in the specified directory
	synchronized public ArrayList<String> metafiles_exist(int directory_id)
	{
		ArrayList<String> existing = new ArrayList<String>();
		
		
		String query = "select string_val from eav, eav_x_eav where attribute_id = " + ATR.get_attribute_id("metadata_filename") +  " and eav_id = eav1_id and eav2_id = " + String.valueOf(directory_id);		
		
		ResultSet rs;
		try {
			Statement stmt = SQL.createStatement();
			rs = stmt.executeQuery(query);
			
			while (rs.next()) {				
				existing.add(rs.getString(1));			
			}			
			rs.close();
			stmt.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return existing;
	}
	
	
	
	// return all eav_ids of this primary entity (including inherited metadata ids)
	synchronized public ArrayList<Integer> get_eav_ids(int metadata_level, int primary_id)
	{
		return get_exclusive_eav_ids(metadata_level, primary_id, new ArrayList<Integer>(), true);	
	}
	
	synchronized public ArrayList<Integer> get_eav_ids(int metadata_level, int primary_id, boolean inheritance)
	{
		return get_exclusive_eav_ids(metadata_level, primary_id, new ArrayList<Integer>(), inheritance);	
	}	
	
	// if inheritance == true: return also inherited eav_ids
	synchronized public ArrayList<Integer> get_exclusive_eav_ids(int metadata_level, Integer primary_id, ArrayList<Integer> exclusive_attribute_ids, boolean inheritance)
	{
		
		ArrayList<Integer> list = new ArrayList<Integer>();
		
		String query = "select " + this.get_primary_x_eav_tablename(metadata_level) + ".eav_id from " + get_primary_x_eav_tablename(metadata_level) + " " + get_primary_x_eav_tablename(metadata_level) + ", eav eav where " + SQL.prefix(get_primary_x_eav_tablename(metadata_level), get_primary_id_name(metadata_level)) + "= " + primary_id + " and eav.eav_id = " + get_primary_x_eav_tablename(metadata_level) + ".eav_id"; 
		
		if(exclusive_attribute_ids.size() > 0)
			query = query + " and attribute_id not in (" + SQL.conc_ids(exclusive_attribute_ids) + ")";

		
		ResultSet rs;
		try {
			Statement stmt = SQL.createStatement();
			rs = stmt.executeQuery(query);
			
			while (rs.next()) {			
				list.add(rs.getInt(1));
			}			
			rs.close();	
			
			
			// if this is at spectrum level then also search all eav_ids that are stored at all parent hierarchies
			if(metadata_level == MetaParameter.SPECTRUM_LEVEL && inheritance)
			{
				ArrayList<Integer> hierarchy_ids = getHierarchyIds(primary_id);
				
				ArrayList<Integer> hierarchy_eav_ids = get_eav_ids(MetaParameter.HIERARCHY_LEVEL, hierarchy_ids);
				
				list.addAll(hierarchy_eav_ids);
				
			}
			
			
			// if this is at hierarchy level then also search all eav_ids that are stored at all parent hierarchies
			if(metadata_level == MetaParameter.HIERARCHY_LEVEL && inheritance)
			{
				ArrayList<Integer> hierarchy_ids = getParentHierarchyIds(primary_id);
				ArrayList<Integer> hierarchy_eav_ids = get_eav_ids(MetaParameter.HIERARCHY_LEVEL, hierarchy_ids);
				list.addAll(hierarchy_eav_ids);
			}
			
			
			stmt.close();
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return list;
				
		
	}
	

	synchronized public ArrayList<Integer> get_exclusive_eav_ids(int metadata_level, Integer primary_id, int ... exclusive_attribute_ids) 
	{
		ArrayList<Integer> attr_id_list = new ArrayList<Integer>();
		
		for (int i=0; i < exclusive_attribute_ids.length; i++)
		{
			attr_id_list.add(exclusive_attribute_ids[i]);		
		}			

		return get_exclusive_eav_ids(metadata_level, primary_id, attr_id_list, true); // by default include inherited metadata
	}	
	
	// returns all eav_ids of the supplied spectrum_ids or hierarchy_ids
	synchronized public ArrayList<Integer> get_eav_ids_incl_inheritance(int metadata_level, ArrayList<Integer> primary_ids, ArrayList<Integer> attribute_ids)
	{
		
		ArrayList<Integer> list = new ArrayList<Integer>();

		if(metadata_level == MetaParameter.SPECTRUM_LEVEL)
		{
			// search all eav_ids that are stored at all parent hierarchies
			ArrayList<Integer> hierarchy_ids = getHierarchyIds(primary_ids);

			ArrayList<Integer> hierarchy_eav_ids = get_eav_ids(MetaParameter.HIERARCHY_LEVEL, hierarchy_ids, attribute_ids);

			list.addAll(hierarchy_eav_ids);

			ArrayList<Integer> spectrum_eav_ids = get_eav_ids(MetaParameter.SPECTRUM_LEVEL, primary_ids, attribute_ids);

			list.addAll(spectrum_eav_ids);

		}
		else
		{
			//search all eav_ids that are stored at all parent hierarchies
			ArrayList<Integer> hierarchy_ids = getParentHierarchyIds(primary_ids);
			ArrayList<Integer> hierarchy_eav_ids = get_eav_ids(MetaParameter.HIERARCHY_LEVEL, hierarchy_ids);
			list.addAll(hierarchy_eav_ids);			
			
			
		}
		


		return list;

		
	}	
	
	
	synchronized public ArrayList<Integer> get_eav_ids(int metadata_level, int primary_id, boolean distinct, String ... attribute_names)
	{
		ArrayList<Integer> attr_id_list = new ArrayList<Integer>();
		
		for (int i=0; i < attribute_names.length; i++)
		{
			attr_id_list.addAll(ATR.get_attribute_ids(attribute_names[i]));		
		}			
		
		return get_eav_ids(metadata_level, Integer.toString(primary_id), distinct, SQL.conc_ids(attr_id_list));
	}	
	
	synchronized public ArrayList<Integer> get_eav_ids(int metadata_level, int primary_id, String ... attribute_names)
	{
		return get_eav_ids(metadata_level, primary_id, false, attribute_names);
	}		
	
	synchronized public ArrayList<Integer> get_eav_ids(int metadata_level, int primary_id, boolean distinct, int ... attribute_ids)
	{
		ArrayList<Integer> attr_id_list = new ArrayList<Integer>();
		
		for (int i=0; i < attribute_ids.length; i++)
		{
			attr_id_list.add(attribute_ids[i]);		
		}			
		
		return get_eav_ids(metadata_level, Integer.toString(primary_id), distinct, SQL.conc_ids(attr_id_list));
	}
	
	synchronized public ArrayList<Integer> get_eav_ids(int metadata_level, int primary_id, ArrayList<Integer> attribute_ids)
	{		
		return get_eav_ids(metadata_level, Integer.toString(primary_id), false, SQL.conc_ids(attribute_ids));
	}		
	
	synchronized public ArrayList<Integer> get_eav_ids(int metadata_level, int primary_id, boolean distinct, ArrayList<Integer> attribute_ids)
	{		
		return get_eav_ids(metadata_level, Integer.toString(primary_id), distinct, SQL.conc_ids(attribute_ids));
	}	
	
	
	synchronized public ArrayList<Integer> get_eav_ids(int metadata_level, int primary_id, int ... attribute_ids)
	{
		return get_eav_ids(metadata_level, primary_id, false, attribute_ids);
	}
	
	synchronized public ArrayList<Integer> get_eav_ids(int metadata_level, ArrayList<Integer> primary_ids, boolean distinct, int ... attribute_ids)
	{
		ArrayList<Integer> attr_id_list = new ArrayList<Integer>();
		
		for (int i=0; i < attribute_ids.length; i++)
		{
			attr_id_list.add(attribute_ids[i]);		
		}			
		
		return get_eav_ids(metadata_level, SQL.conc_ids(primary_ids), distinct, SQL.conc_ids(attr_id_list));	
	}
	
	synchronized public ArrayList<Integer> get_eav_ids(int metadata_level, ArrayList<Integer> primary_ids, int ... attribute_ids)
	{
		return get_eav_ids(metadata_level, primary_ids, false, attribute_ids);
	}	

	
	synchronized public ArrayList<Integer> get_eav_ids(int metadata_level, ArrayList<Integer> primary_ids, boolean distinct, ArrayList<Integer> attribute_ids)
	{		
		return get_eav_ids(metadata_level, SQL.conc_ids(primary_ids), distinct, SQL.conc_ids(attribute_ids));
	}
	
	synchronized public ArrayList<Integer> get_eav_ids(int metadata_level, ArrayList<Integer> primary_ids, ArrayList<Integer> attribute_ids)
	{		
		return get_eav_ids(metadata_level, SQL.conc_ids(primary_ids), false, SQL.conc_ids(attribute_ids));
	}	

	
	synchronized ArrayList<Integer> get_eav_ids(int metadata_level, String primary_ids, boolean distinct, String attribute_ids)
	{
		
		ArrayList<Integer> list = new ArrayList<Integer>();

		String distinct_str = " ";
		
		if (distinct) distinct_str = " distinct ";		
				
		String query = "select" + distinct_str +"eav.eav_id from eav eav, " + get_primary_x_eav_tablename(metadata_level) + " " + get_primary_x_eav_tablename(metadata_level) + 
				" where " + (attribute_ids.equals("null") ? "":("attribute_id in (" + attribute_ids + ") and ")) + get_primary_x_eav_tablename(metadata_level) + ".eav_id = eav.eav_id and " + 
				get_primary_x_eav_tablename(metadata_level) + "." + get_primary_id_name(metadata_level) + " in (" + primary_ids + ")"
				+ ((!distinct)? " order by FIELD (" + SQL.prefix(get_primary_x_eav_tablename(metadata_level), get_primary_id_name(metadata_level)) + ", "+ primary_ids +")" : "");

		ResultSet rs;
		try {
			Statement stmt = SQL.createStatement();
			rs = stmt.executeQuery(query);
			
			while (rs.next()) {
				list.add(rs.getInt(1));			
			}			
			rs.close();
			stmt.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return list;
		
		
		// is this faster?????!!!????  -> the number of eav_ids is just getting way too big!!!!!!
		
//		String query1 = "select eav_id from frame_x_eav fxe where fxe.frame_id in (" + frame_ids + ")";
//		
//		ResultSet rs;
//		try {
//			rs = stmt.executeQuery(query1);
//			
//			while (rs.next()) {
//				eav_ids.add(rs.getInt(1));			
//			}			
//			rs.close();						
//			
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		
//
//		String distinct_str = " ";
//		
//		if (distinct) distinct_str = " distinct ";		
//				
//		String query = "select" + distinct_str + "eav.eav_id from eav eav where attribute_id in (" + attribute_ids + ") and eav.eav_id in (" + SQL.conc_ids(eav_ids) + ")";
//
//		try {
//			rs = stmt.executeQuery(query);
//			
//			while (rs.next()) {
//				list.add(rs.getInt(1));			
//			}			
//			rs.close();						
//			
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}		
	}
	
	synchronized public SingularFrameEAVStructure get_eav_ids_(int metadata_level, ArrayList<Integer> primary_ids, ArrayList<Integer> attribute_ids)
	{		
		return get_eav_ids_(metadata_level, primary_ids, false, SQL.conc_ids(attribute_ids));
	}	

	
	synchronized SingularFrameEAVStructure get_eav_ids_(int metadata_level, ArrayList<Integer> primary_ids, boolean distinct, String attribute_ids)
	{
		String frame_ids_str = SQL.conc_ids(primary_ids);

		String distinct_str = " ";
		
		SingularFrameEAVStructure feav = new SingularFrameEAVStructure();
		
//		int curr_frame_id = frame_ids.get(0);
//		SingularFrameEAVStructure feav = new SingularFrameEAVStructure(curr_frame_id);
		
		if (distinct) distinct_str = " distinct ";
		
//		String query = "select" + distinct_str +"eav.eav_id from eav eav, " + this.primary_x_eav_tablename + " " + primary_x_eav_tablename + 
//		" where attribute_id in (" + attribute_ids + ") and " + primary_x_eav_tablename + ".eav_id = eav.eav_id and " + primary_x_eav_tablename + "." + this.primary_id_name + " in (" + frame_ids + ")";

//		String query = "select" + distinct_str + "fxe.frame_id, eav.eav_id from eav eav, frame_x_eav fxe where attribute_id in (" + attribute_ids + ") and fxe.eav_id = eav.eav_id and fxe.frame_id in (" + frame_ids_str + ")";
		
				
		String query = "select" + distinct_str + " fxe." + get_primary_id_name(metadata_level) +", eav.eav_id from eav eav, " + get_primary_x_eav_tablename(metadata_level) + " fxe where attribute_id in (" + attribute_ids + ") and fxe.eav_id = eav.eav_id and fxe." + get_primary_id_name(metadata_level) + " in (" + frame_ids_str + ")";

		//System.out.println(query);
		
		ResultSet rs;
		try {
			Statement stmt = SQL.createStatement();
			rs = stmt.executeQuery(query);
			
			while (rs.next()) {
				
				int frame_id = rs.getInt(1);
				int eav_id = rs.getInt(2);
				
				feav.add(frame_id, eav_id);
				
//				if(frame_id != curr_frame_id)
//				{
//					feav_list.add(feav); // add current structure to list
//					feav = new SingularFrameEAVStructure(frame_id); // create new structure
//					curr_frame_id = frame_id;
//				}
//				
//				feav.add(eav_id);
	
			}			
			rs.close();		
			stmt.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return feav;
	}
		
	
	synchronized public ArrayList<Integer> get_statistic_frame_ids(int metadata_level, int primary_id)
	{	
		ArrayList<Integer> ids = new ArrayList<Integer>();
		ids.add(primary_id);
		return get_statistic_frame_ids(metadata_level, ids);
	}

	
	synchronized public ArrayList<Integer> get_statistic_frame_ids(int metadata_level, ArrayList<Integer> primary_ids)
	{
		ArrayList<Integer> stat_ids = new ArrayList<Integer>();
		
		
		ArrayList<Integer> stat_eav_ids = get_eav_ids(metadata_level, primary_ids, ATR.get_attribute_id("mean"), ATR.get_attribute_id("stdev"));
			
			//String query = "select frame_id from frame_x_eav where eav_id in (select eav2_id from eav_x_eav where eav1_id in (select eav.eav_id from frame_x_eav fex, eav eav where frame_id = " + this.frame_id + " and fex.eav_id = eav.eav_id and attribute_id in (select attribute_id from attribute where category_id in (select category_id from category where name = 'statistic'))))";
		
		String query = "select distinct frame_id from frame_x_eav, eav_x_eav where eav_id = eav2_id and eav1_id in (" + SQL.conc_ids(stat_eav_ids)  + ")";
		
		
		ResultSet rs;
		try {
			Statement stmt = SQL.createStatement();
			rs = stmt.executeQuery(query);			
		
			while (rs.next()) {
				stat_ids.add(rs.getInt(1));
			}
			
			
			rs.close();
			stmt.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		
		return stat_ids;
	}
	
	
	synchronized public ArrayList<Integer> filter_by_eav(int metadata_level, ArrayList<Integer> primary_ids, int attribute_id, String field, Object value, ProgressListener pr)
	{
		if (pr != null)
		{
			pr.setVisible(true);
			
			pr.set_operation("Selecting data (" + primary_ids.size() + "records) ...");
		}		
		
		
		ArrayList<Integer> filtered = new ArrayList<Integer>();
		
		String query = "select frame_x_eav." + get_primary_id_name(metadata_level) + ", eav." + field + " from " + get_primary_x_eav_tablename(metadata_level) + " frame_x_eav, eav eav where eav.eav_id = frame_x_eav.eav_id and eav.attribute_id = " + attribute_id + " and frame_x_eav." + get_primary_id_name(metadata_level) + " in (" + SQL.conc_ids(primary_ids)  + ")";		
			
		int cnt = 1;
		
		ResultSet rs;
		try {
			Statement stmt = SQL.createStatement();
			rs = stmt.executeQuery(query);	
			
			if (pr != null) pr.set_operation("Filtering data ...");
		
			while (rs.next()) {
				
				Object o = rs.getObject(2);
				
				if (o.equals(value))
				{
					filtered.add(rs.getInt(1));
				}
				
				if (pr != null) pr.set_progress(cnt++ * 100.0 / (primary_ids.size()-1));
		
				
			}
			
			
			rs.close();
			stmt.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
		if (pr != null) pr.setVisible(false);
		
		
		return filtered;
				
	}
	
	
	
	
	synchronized public ArrayList<Integer> filter_by_eav(int metadata_level, ArrayList<Integer> primary_ids, int attribute_id, String field, Object value)
	{
		return filter_by_eav(metadata_level, primary_ids, attribute_id, field, value, null);
	}

	synchronized public ArrayList<Integer> filter_by_eav(int metadata_level, ArrayList<Integer> primary_ids, int attribute_id)
	{	
		ArrayList<Integer> filtered = new ArrayList<Integer>();
		
		String query = "select frame_x_eav." + get_primary_id_name(metadata_level) + " from " + get_primary_x_eav_tablename(metadata_level) + " frame_x_eav, eav eav where eav.eav_id = frame_x_eav.eav_id and eav.attribute_id = " + attribute_id + " and frame_x_eav." + get_primary_id_name(metadata_level) + " in (" + SQL.conc_ids(primary_ids)  + ")";
		
		ResultSet rs;
		try {
			Statement stmt = SQL.createStatement();
			rs = stmt.executeQuery(query);	
		
			while (rs.next()) {
				
					filtered.add(rs.getInt(1));
			}
						
			rs.close();	
			stmt.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		// for spectrum level queries also add hierarchy level matches: use parent hierarchies of selected spectra
		// if this is at spectrum level then also search all eav_ids that are stored at all parent hierarchies
		if(metadata_level == MetaParameter.SPECTRUM_LEVEL)
		{
			
			// OR: get immediate hierarchy and then do a recursive call?
			
			ArrayList<Integer> hierarchy_ids = getHierarchyIds(primary_ids);
			
			
			ArrayList<Integer> matching_hierarchy_ids = filter_by_eav(MetaParameter.HIERARCHY_LEVEL, hierarchy_ids, attribute_id);
			
			// how to compile? THis should be an OR with the spectrum level metadata (either or is possible)
			
			// get spectra of these hierarchies, but then add only the spectrum ids that were in the original list.
			ArrayList<Integer> matching_spectrum_ids = new ArrayList<Integer>();
			
			query = "select spectrum_id from hierarchy_level_x_spectrum where hierarchy_level_id in (" + this.SQL.conc_ids(matching_hierarchy_ids) + ") and spectrum_id in (" + SQL.conc_ids(primary_ids)  + ")"; 
			
			
			try {
				Statement stmt = SQL.createStatement();
				rs = stmt.executeQuery(query);	
			
				while (rs.next()) {
					
						filtered.add(rs.getInt(1));
				}
							
				rs.close();	
				stmt.close();
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			

		}
		
		
		// if this is at hierarchy level then also search all eav_ids that are stored at all parent hierarchies
//		if(metadata_level == MetaParameter.HIERARCHY_LEVEL && inheritance)
//		{
//			ArrayList<Integer> hierarchy_ids = getParentHierarchyIds(primary_id);
//			ArrayList<Integer> hierarchy_eav_ids = get_eav_ids(MetaParameter.HIERARCHY_LEVEL, hierarchy_ids);
//			list.addAll(hierarchy_eav_ids);
//		}		
		
		
		
		
		return filtered;
				
	}
	
	
	synchronized public ArrayList<Integer> filter_by_eav(int metadata_level, ArrayList<Integer> primary_ids, int attribute_id, int unit_id)
	{
		ArrayList<Integer> filtered = new ArrayList<Integer>();
		
		String query = "select frame_x_eav." + get_primary_id_name(metadata_level) + " from " + get_primary_x_eav_tablename(metadata_level) + " frame_x_eav, eav eav where eav.eav_id = frame_x_eav.eav_id and eav.attribute_id = " + attribute_id + " and eav.unit_id = " + unit_id + " and frame_x_eav." + get_primary_id_name(metadata_level) + " in (" + SQL.conc_ids(primary_ids)  + ")";
		
		ResultSet rs;
		try {
			Statement stmt = SQL.createStatement();
			rs = stmt.executeQuery(query);	
		
			while (rs.next()) {
				
					filtered.add(rs.getInt(1));
			}
						
			rs.close();
			stmt.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return filtered;
				
	}	
	
	
	synchronized public ArrayList<Integer> filter_by_attribute_NOT(int metadata_level, ArrayList<Integer> primary_ids, int attribute_id)
	{		
		
		ArrayList<Integer> positive_finds = filter_by_eav(metadata_level, primary_ids, attribute_id);
		
		
		ArrayList<Integer> filtered = new ArrayList<Integer>(primary_ids); // copy
		
		filtered.removeAll(positive_finds);
		
		
		return filtered;
				
	}
		
	
	
	
	synchronized public ArrayList<FrameMetaparameterStructure> load_metaparameters(int metadata_level, ArrayList<Integer> primary_ids, int ... attribute_ids)
	{
		ArrayList<FrameMetaparameterStructure> fmps_list = new ArrayList<FrameMetaparameterStructure>();
		
		ArrayList<Integer> attr_id_list = new ArrayList<Integer>();
		ArrayList<Integer> attr_ids_for_lazyloading = new ArrayList<Integer>();
		
		for (int i=0; i < attribute_ids.length; i++)
		{
			attr_id_list.add(attribute_ids[i]);		
		}				
		
		// get the eav_ids for the given frames and attribute
		SingularFrameEAVStructure frame_eav_ids = get_eav_ids_(metadata_level, primary_ids, attr_id_list);
		
		// carry out a bulk load for the meta parameters
		Metadata md = new Metadata();
		try {
			metadata_bulk_loader(md, frame_eav_ids.eav_ids);
			
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// sort the metaparameters into a new structure by frame_id
		
		int curr_frame_id = 0;
		FrameMetaparameterStructure fms = null;
		
		ListIterator<Integer> frameli = frame_eav_ids.frame_ids.listIterator();
		ListIterator<Integer> eavli = frame_eav_ids.eav_ids.listIterator();
		
		// here we must look up the metaparameter via the eav_id, as it can happen that there are shared metaparameters
		while(frameli.hasNext())
		{
			int frame_id = frameli.next();
			int eav_id = eavli.next();
			
			if(frame_id != curr_frame_id)
			{				
				fms = new FrameMetaparameterStructure(frame_id); // create new structure
				fmps_list.add(fms); // add current structure to list
				curr_frame_id = frame_id;
			}
			
			int mp_index = md.getEntryIds().indexOf(eav_id);
			
			MetaParameter mp = md.getEntries().get(mp_index);
			
			fms.add(mp);					

		}		
				
		
		return fmps_list;
	}
	
	
	/**
	 * Load a MetaParameter object from the database.
	 * 
	 * @param mp	the MetaParameter object to be loaded
	 * 
	 * @throws SQLException	SQL error
	 */
	 public MetaParameter load_metaparameter(int eav_id) throws SQLException {
		 
		String query = "select eav.int_val, eav.double_val, eav.string_val, eav.binary_val, datetime_val, eav.taxonomy_id, eav.spectrum_id," + (this.spatially_enabled==true ? "ST_AsText(spatial_val)," : "") + " unit.short_name, unit.unit_id, attr.attribute_id from eav eav, attribute attr, unit unit, category cat where " +
		"eav.eav_id = " + Integer.toString(eav_id) + " and eav.attribute_id = attr.attribute_id and eav.unit_id = unit.unit_id and attr.category_id = cat.category_id";

		Statement stmt = SQL.createStatement();
		ResultSet rs = stmt.executeQuery(query);
			
		Object int_val, double_val, string_val, spatial_val = null;
		Long taxonomy_id, spectrum_id;
		Object datetime_val;
		Blob binary_val;
		
		MetaParameter mp = null;
		while (rs.next()) {		
			int ind = 1;
			int_val = rs.getObject(ind++);
			double_val = rs.getObject(ind++);
			string_val = rs.getObject(ind++);
			binary_val = rs.getBlob(ind++);
			datetime_val =  rs.getString(ind++);
			taxonomy_id = rs.getLong(ind++);
			spectrum_id = rs.getLong(ind++);
			if(spatially_enabled) spatial_val = rs.getString(ind++);
			
			if(taxonomy_id == 0) taxonomy_id = null;
			if(spectrum_id == 0) spectrum_id = null;
			
			//String attribute_name = rs.getString(ind++);
			String unit_name = rs.getString(ind++);
			int unit_id = rs.getInt(ind++);
			int attribute_id = rs.getInt(ind++);
			//String category_name = rs.getString(ind++);
			//String category_value = rs.getString(ind++);
			
			attribute attr = this.ATR.get_attribute_info(attribute_id);
			
			try {
				if (int_val != null)
				{
					mp = MetaParameter.newInstance(attr, int_val);
				}
				else if (double_val != null)
				{
					mp = MetaParameter.newInstance(attr, double_val);
				}
				else if (string_val != null && binary_val == null)
				{
					mp = MetaParameter.newInstance(attr, string_val);
				}
				else if (binary_val != null)
				{
					try {
						ObjectInputStream ois = new ObjectInputStream(binary_val.getBinaryStream());
						mp = MetaParameter.newInstance(attr, ois.readObject());
					} catch (IOException e) {
						// don't know why this might happen
						throw new SQLException(e);
					} catch (ClassNotFoundException e) {
						// unrecognised type found in the database; should never happen
						throw new SQLException(e);
					}	
						
				}
				else if (datetime_val != null)
				{
					DateTimeFormatter formatter = DateTimeFormat.forPattern(MetaDate.DEFAULT_DATE_FORMAT + ".S").withZoneUTC();
					DateTime d = formatter.parseDateTime((String) datetime_val); 					
					mp = MetaParameter.newInstance(attr, d);
				}
				else if (taxonomy_id != null)
				{
					mp = MetaParameter.newInstance(attr, taxonomy_id);
				}		
				else if (spectrum_id != null)
				{
					mp = MetaParameter.newInstance(attr, spectrum_id);
				}		
				else if (spatial_val != null)
				{
					mp = MetaParameter.newInstance(attr, spatial_val);
				}											
				else
				{
					mp = MetaParameter.newInstance(attr, null);
					System.out.println("EAV Null value read from DB!");
				}
			}
			catch (java.lang.NullPointerException e) {
				
				System.out.println("EAV Null value read from DB for field " + attr.getName() + "!");
			}						
			catch (MetaParameterFormatException e) {
				// should never happen but let's re-throw it as an SQLException just in case
				throw new SQLException(e);
			}
			
			mp.setEavId(eav_id);
			mp.setAttributeName(attr.getName());
			mp.setUnitName(unit_name);
			mp.setAttributeId(attribute_id);
			mp.setUnitId(unit_id);

		}			
		rs.close();
		stmt.close();
		
		return mp;
			
	}	
	
	
	public void metadata_bulk_loader(Metadata md, ArrayList<Integer> metaparameter_ids) throws SQLException
	{
		
		int blob_lazy_loading_limit = 1000*1024; 
		//boolean blob_lazy_loading = false;
		boolean order_by_eav_id = false; // required in combination with getMetaParameters() in the MetadataFactory and logic_switch = true
										// ; this is the initiative to use the metadata bulk loader for all metaparameter loading ... but is it faster or slower than the original getMetaParameters()?
										// -> it actually is not! We'll leave this order code in here though, it might come in handy at some later stage.
		
		// step 1: figure which rows will have big blobs that need lazy loading
		String query = "select eav.eav_id, OCTET_LENGTH(eav.binary_val) from eav eav where " +
				"eav.eav_id in (" + SQL.conc_ids(metaparameter_ids) + ")";
		if(order_by_eav_id) query = query + " order by FIELD (eav.eav_id, " + SQL.conc_ids(metaparameter_ids) + ")";
		
		Statement stmt = SQL.createStatement();
		ResultSet rs = stmt.executeQuery(query);		
		
		ArrayList<Integer> standard_metaparameter_ids = new ArrayList<Integer>();
		ArrayList<Integer> lazy_metaparameter_ids = new ArrayList<Integer>();
		
		while (rs.next()) {	
			
			int eav_id = rs.getInt(1);
			int size = rs.getInt(2);
			
			if(size > blob_lazy_loading_limit)
			{
				lazy_metaparameter_ids.add(eav_id);
			}
			else
			{
				standard_metaparameter_ids.add(eav_id);
			}
			
		}

		rs.close();
		stmt.close();
		
		
		// non-lazy loading first ...
		if(standard_metaparameter_ids.size() > 0)
		{
			query = "select eav.eav_id, eav.int_val, eav.double_val, eav.string_val, OCTET_LENGTH(eav.binary_val), eav.binary_val, eav.datetime_val, eav.taxonomy_id, eav.spectrum_id, " + 
					(this.spatially_enabled==true ? "ST_AsText(spatial_val)," : "") + " unit.short_name, unit.unit_id, attr.attribute_id, count(hierarchy_level_id) from (eav eav, attribute attr, unit unit, category cat) left join hierarchy_x_eav hxe on hxe.eav_id = eav.eav_id where " +
					"eav.eav_id in (" + SQL.conc_ids(standard_metaparameter_ids) + ") and eav.attribute_id = attr.attribute_id and eav.unit_id = unit.unit_id and attr.category_id = cat.category_id group by eav.eav_id ";
			if(order_by_eav_id) query = query + "order by FIELD (eav.eav_id, " + SQL.conc_ids(standard_metaparameter_ids) + ")";


			stmt = SQL.createStatement();
			rs = stmt.executeQuery(query);

			Object int_val, double_val, string_val, datetime_val, spatial_val = null;
			int binary_val_size;
			Long taxonomy_id, spectrum_id;
			Blob binary_val;

			while (rs.next()) {	

				MetaParameter mp;

				int ind = 1;
				int eav_id = rs.getInt(ind++);
				int_val = rs.getObject(ind++);
				double_val = rs.getObject(ind++);
				string_val = rs.getObject(ind++);
				binary_val_size = rs.getInt(ind++);

				//			if (binary_val_size> blob_lazy_loading_limit)
				//			{
				//				blob_lazy_loading = true;
				//				binary_val= null;
				////				binary_val = SQL.createBlob();
				////				binary_val.setBytes(1, new byte[1]);
				//				ind++; // skip binary val
				//			}
				//			else
				//			{
				//				blob_lazy_loading = false;
				//				binary_val = rs.getBlob(ind++);			
				//			}

				binary_val = rs.getBlob(ind++);		

				datetime_val = rs.getString(ind++);
				taxonomy_id = rs.getLong(ind++);
				spectrum_id = rs.getLong(ind++);
				if(spatially_enabled) spatial_val = rs.getString(ind++);

				if(taxonomy_id == 0) taxonomy_id = null;
				if(spectrum_id == 0) spectrum_id = null;

				//String attribute_name = rs.getString(ind++);
				String unit_name = rs.getString(ind++);
				int unit_id = rs.getInt(ind++);
				int attribute_id = rs.getInt(ind++);
				int hierarchy_id_count = rs.getInt(ind++);
				//String category_name = rs.getString(ind++);
				//String category_value = rs.getString(ind++);
				//String description = rs.getString(ind++);

				attribute attr = this.ATR.get_attribute_info(attribute_id);


				try {
					if (int_val != null)
					{
						mp = MetaParameter.newInstance(attr, int_val);
					}
					else if (double_val != null)
					{
						mp = MetaParameter.newInstance(attr, double_val);
					}
					else if (string_val != null && binary_val == null)
					{
						//					for(int i=0;i<((String) string_val).length();i++)
						//					{
						////						if(((String) string_val).charAt(i) == 0)
						////						{
						////							StringBuilder myName = new StringBuilder((String) string_val);
						////							myName.setCharAt(i, '_');
						////							string_val = myName.toString();
						////							string_val = "!!!!!!!" + string_val;
						////						}
						//					}

						mp = MetaParameter.newInstance(attr, string_val);
					}
					else if (taxonomy_id != null)
					{
						mp = MetaParameter.newInstance(attr, taxonomy_id);
					}	
					else if (spectrum_id != null)
					{
						mp = MetaParameter.newInstance(attr, spectrum_id);
					}						
					else if (datetime_val != null)
					{
						DateTimeFormatter formatter = DateTimeFormat.forPattern(MetaDate.DEFAULT_DATE_FORMAT + ".S").withZoneUTC();
						DateTime d = formatter.parseDateTime((String) datetime_val); 
						mp = MetaParameter.newInstance(attr, d);
					}
					else if (binary_val != null)
					{

						// read the object from the database
						Object value;
						try {
							ByteArrayInputStream bis = new ByteArrayInputStream(binary_val.getBytes(1, (int) binary_val.length()));
							ObjectInputStream in = new ObjectInputStream(bis);
							value = in.readObject();
							in.close();
						}		
						catch (IOException e) {
							// don't know what might cause this; re-throw it as an SQL exception
							throw new SQLException(e);
						}
						catch (ClassNotFoundException e) {
							// unrecognised class stored in the database; shouldn't happen in the ideal world
							throw new SQLException(e);
						}

						// create a meta-parameter object of the appropriate type
						mp = MetaParameter.newInstance(attr, value);

						if (string_val != null)
						{
							// code to find strings that had null values inserted by SQL injections (e.g. \0 due to path names in Windows)
							//						char c = ((String) string_val).charAt(75);
							//						int ic = c;
							//						for(int i=0;i<((String) string_val).length();i++)
							//						{
							//							if(((String) string_val).charAt(i) == 0)
							//							{
							//								StringBuilder myName = new StringBuilder((String) string_val);
							//								myName.setCharAt(i, '_');
							//								string_val = myName.toString();
							//								string_val = "!!!!!!!" + string_val;
							//							}
							//						}
							mp.setAnnotation((String) string_val);
						}

					}
					//				else if(blob_lazy_loading)
					//				{
					//					mp = MetaParameter.newInstance(attr, new PdfDocument()); // trick to ensure a PDF document metaparameter is created; the actual type of the document will defined upon actual loading
					//					mp.setBlob_lazy_loading(true);
					//				}
					else if (spatial_val != null)
					{
						mp = MetaParameter.newInstance(attr, spatial_val);
					}											

					else
					{
						mp = MetaParameter.newInstance(attr, null);					
					}


					mp.setEavId(eav_id);
					mp.setAttributeName(attr.getName());
					mp.setUnitName(unit_name);
					mp.setAttributeId(attribute_id);
					mp.setUnitId(unit_id);
					mp.setDescription(attr.description);
					mp.setLevel((hierarchy_id_count == 0) ? MetaParameter.SPECTRUM_LEVEL:MetaParameter.HIERARCHY_LEVEL);

					md.addEntry(mp);
					md.addEntryId(mp.getEavId());								


				}
				catch (java.lang.NullPointerException e) {

					System.out.println("EAV Null value read from DB for field " + attr.getName() + "!");
				}			
				catch (MetaParameterFormatException e) {
					// should never happen but let's re-throw it as an SQL exception just in case.
					throw new SQLException(e);
				}


			}			
			rs.close();
			stmt.close();
		
		}
		
		
		// lazy loading
		if(lazy_metaparameter_ids.size() > 0)
		{
		query = "select eav.eav_id, OCTET_LENGTH(eav.binary_val), unit.short_name, unit.unit_id, attr.attribute_id from eav eav, attribute attr, unit unit, category cat where " +
		"eav.eav_id in (" + SQL.conc_ids(lazy_metaparameter_ids) + ") and eav.attribute_id = attr.attribute_id and eav.unit_id = unit.unit_id and attr.category_id = cat.category_id";
		
		if(order_by_eav_id) query = query + "order by FIELD (eav.eav_id, " + SQL.conc_ids(lazy_metaparameter_ids) + ")";
		

		stmt = SQL.createStatement();
		rs = stmt.executeQuery(query);

			while (rs.next()) {	

				MetaParameter mp;

				int ind = 1;
				int eav_id = rs.getInt(ind++);
				int binary_val_size = rs.getInt(ind++);

				String unit_name = rs.getString(ind++);
				int unit_id = rs.getInt(ind++);
				int attribute_id = rs.getInt(ind++);

				attribute attr = this.ATR.get_attribute_info(attribute_id);

				try {


					mp = MetaParameter.newInstance(attr, new PdfDocument()); // trick to ensure a PDF document metaparameter is created; the actual type of the document will defined upon actual loading
					mp.setBlob_lazy_loading(true);
					mp.setBlob_size(binary_val_size);

					mp.setEavId(eav_id);
					mp.setAttributeName(attr.getName());
					mp.setUnitName(unit_name);
					mp.setAttributeId(attribute_id);
					mp.setUnitId(unit_id);
					mp.setDescription(attr.description);

					md.addEntry(mp);
					md.addEntryId(mp.getEavId());								


				}
				catch (java.lang.NullPointerException e) {

					System.out.println("EAV Null value read from DB for field " + attr.getName() + "!");
				}			
				catch (MetaParameterFormatException e) {
					// should never happen but let's re-throw it as an SQL exception just in case.
					throw new SQLException(e);
				}

			}
		}


		
	}
	
	
	public void delete_eav(Integer eav_id, boolean is_admin) throws SQLException {
		
		ArrayList<Integer> eav_ids = new ArrayList<Integer>();
		eav_ids.add(eav_id);
		delete_eavs(eav_ids, is_admin);
		
	}
	
	
	
	public void delete_eavs(ArrayList<Integer> eav_ids, boolean is_admin) throws SQLException {
		
		//pr.set_operation("Deleting frame_x_eav's ...");
		
		// remove all eav_x_eav entries that refer to these eav's
		delete_eav_x_eav(eav_ids);
		
		//pr.set_operation("Deleting eav's ...");
				
		String cmd = "delete from eav_view where eav_id in (" + SQL.conc_ids(eav_ids) + ")";
		
		try {
			Statement stmt = SQL.createStatement();
			stmt.executeUpdate(cmd);
			stmt.close();
		} catch (SQLException e) {
			if(e.getSQLState().equals("23000"))
			{
				// this is fine because the EAV's are still referenced by some other entity
			}
			else
			{
				throw e;
			}
			
		} 		
		
	}



	public void delete_eav_x_eav(ArrayList<Integer> eav_ids) throws SQLException {

		// the eav_x_eav table does not exist for e.g. the specchio database
		if (SQL.table_exists("eav_x_eav")) {
			Statement stmt = SQL.createStatement();
			String cmd = "delete from eav_x_eav where eav1_id in (" + SQL.conc_ids(eav_ids) + ") or eav2_id in (" + SQL.conc_ids(eav_ids) + ")";
			stmt.executeUpdate(cmd);
			stmt.close();
		}
		
	}
	
	public void delete_primary_x_eav(int metadata_level, Integer primary_id, Integer eav_id, boolean is_admin) {
		
		ArrayList<Integer> eav_ids = new ArrayList<Integer>();
		eav_ids.add(eav_id);
		delete_primary_x_eav(metadata_level, primary_id, eav_ids, is_admin);
		
	}
	
	public void delete_primary_x_eav(int metadata_level, Integer[] primary_ids, Integer eav_id, boolean is_admin) {
		
		ArrayList<Integer> eav_ids = new ArrayList<Integer>();
		eav_ids.add(eav_id);
		
		ArrayList<Integer> primary_ids_list = new ArrayList<Integer>(primary_ids.length);
		for (Integer frame_id : primary_ids) {
			primary_ids_list.add(frame_id);
		}
		
		delete_primary_x_eav(metadata_level, primary_ids_list, eav_ids, is_admin);
		
	}
	
	public void delete_primary_x_eav(int metadata_level, ArrayList<Integer> primary_ids, Integer eav_id, boolean is_admin) {
		
		ArrayList<Integer> eav_ids = new ArrayList<Integer>();
		eav_ids.add(eav_id);
		delete_primary_x_eav(metadata_level, primary_ids, eav_ids, is_admin);
		
	}
	
	public void delete_primary_x_eav(int metadata_level, Integer primary_id, ArrayList<Integer> eav_ids, boolean is_admin) {
		
		String table_name = (is_admin)? get_primary_x_eav_tablename(metadata_level) : get_primary_x_eav_viewname(metadata_level);
		
		String cmd = "delete from " + table_name + " where " + get_primary_id_name(metadata_level) + " = " + primary_id + " and eav_id in (" + SQL.conc_ids(eav_ids) +")"; // avoid deleting eav links for other frames (shared data!)
		
		try {
			Statement stmt = SQL.createStatement();
			stmt.executeUpdate(cmd);
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 	
		
	}
	
	public void delete_primary_x_eav(int metadata_level, ArrayList<Integer> frame_ids, ArrayList<Integer> eav_ids, boolean is_admin) {
		
		String table_name = (is_admin)? get_primary_x_eav_tablename(metadata_level) : get_primary_x_eav_viewname(metadata_level);
		String cmd = "delete from " + table_name + " where " + get_primary_id_name(metadata_level) + " in (" + SQL.conc_ids(frame_ids) +")" + " and eav_id in (" + SQL.conc_ids(eav_ids) +")"; // avoid deleting eav links for other frames (shared data!)
		
		try {
			Statement stmt = SQL.createStatement();
			stmt.executeUpdate(cmd);
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 	
		
	}
	
	
	public ArrayList<Integer> getPrimaryIds(int metadata_level, Integer eav_id) {
		
		ArrayList<Integer> eav_ids = new ArrayList<Integer>();
		eav_ids.add(eav_id);
		
		return getPrimaryIds(metadata_level, eav_ids);
		
	}
	
	
	public ArrayList<Integer> getPrimaryIds(int metadata_level, ArrayList<Integer> eav_ids)
	{
		String query = "select " + get_primary_id_name(metadata_level) + " from " + get_primary_x_eav_tablename(metadata_level) + " where eav_id in (" + SQL.conc_ids(eav_ids) +")";
		
		ArrayList<Integer> primary_ids = new ArrayList<Integer>();
		
		ResultSet rs;
		try {
			Statement stmt = SQL.createStatement();
			rs = stmt.executeQuery(query);	
		
			while (rs.next()) {
				
				primary_ids.add(rs.getInt(1));
			}
						
			rs.close();
			stmt.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		return primary_ids;
	}
	
	

	synchronized public String get_unique_alias(String tablename)
	{
		
		// add numbers to tablename and check if already in alias list
		int i  = 1;
		
		while(aliases.contains(tablename + Integer.toString(i)))
		{
			i++;
		}
		
		aliases.add(tablename + Integer.toString(i));
		
		return tablename + Integer.toString(i);		

	}

	public void update_metaparameter(MetaParameter mp, boolean is_admin) {
		
		String field = ATR.get_default_storage_field(mp.getAttributeId());
		String table_name = (is_admin)? "eav" : "eav_view";
		
		try {
			get_metaparameter_attribute_and_unit_ids(mp);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if(mp instanceof MetaSpatialGeometry)
		{
	
			String query = "update " + table_name +
					" set attribute_id = " + mp.getAttributeId() +
					", " + field +" = " + (String) mp.getEAVValue() +
					", unit_id = " + mp.getUnitId() +
					" where eav_id = " + mp.getEavId();
			
			Statement stmt;
			try {
				stmt = SQL.createStatement();
				stmt.executeUpdate(query);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		else
		{
			String query = "update " + table_name +
					" set attribute_id = " + mp.getAttributeId() +
					", " + field +" = " + "?" +
					", unit_id = " + mp.getUnitId() +
					" where eav_id = " + mp.getEavId();

			try {
				PreparedStatement stmt = SQL.prepareStatement(query);
				stmt.setString(1, mp.valueAsString());
				stmt.executeUpdate();
				stmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
		}
		
		
	}


	public boolean isSpatially_enabled() {
		return spatially_enabled;
	}


	public void setSPECCHIOFactory(SPECCHIOFactory specchioFactory) {
		// TODO Auto-generated method stub
		this.specchioFactory  = specchioFactory;
	}





	

}





class SingularFrameEAVStructure
{
	//int frame_id;
	ArrayList<Integer> frame_ids = new  ArrayList<Integer>();
	ArrayList<Integer> eav_ids = new  ArrayList<Integer>();	
	
	public SingularFrameEAVStructure()
	{

	}
	
//	public void add(int eav_id)
//	{
//		eav_ids.add(eav_id);
//	}	
	
	
	public void add(int frame_id, int eav_id)
	{
		frame_ids.add(frame_id);
		eav_ids.add(eav_id);
	}
}




//@SuppressWarnings("hiding")
//class MatlabAdaptedArrayList<Object> extends ArrayList<Object>
//{
//
//	private static final long serialVersionUID = 1L;
//
//	public MatlabAdaptedArrayList()
//	{
//		super();
//	}
//	
//	public double[] get_as_double_array()
//	{
//		double [] out = new double [this.size()];
//		
//		System.out.println("Class: " + this.get(0).getClass());
//		
//		if(this.get(0).getClass().getSimpleName().equals("Integer"))
//		{
//			for (int i=0;i<this.size();i++)
//			{
//				out[i] = (Integer) this.get(i);
//			}				
//			
//		}
//		
//		if(this.get(0).getClass().getSimpleName().equals("Double"))
//		{
//			for (int i=0;i<this.size();i++)
//			{
//				out[i] = (Double) this.get(i);
//			}						
//		}		
//		
//		return out;
//	}
//
//	
//	
//}






