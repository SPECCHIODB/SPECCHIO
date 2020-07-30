package ch.specchio.factories;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import ch.specchio.eav_db.EAVDBServices;
import ch.specchio.eav_db.SQL_StatementBuilder;
import ch.specchio.spaces.Space;
import ch.specchio.types.ApplicationDomainCategories;
import ch.specchio.types.CategoryTable;
import ch.specchio.types.ConflictInfo;
import ch.specchio.types.ConflictStruct;
import ch.specchio.types.ConflictTable;
import ch.specchio.types.EAVTableAndRelationsInfoStructure;
import ch.specchio.types.MetaDate;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.MetaSpatialGeometry;
import ch.specchio.types.Metadata;
import ch.specchio.types.MetaparameterStatistics;
import ch.specchio.types.Spectrum;
import ch.specchio.types.Taxonomy;
import ch.specchio.types.TaxonomyNodeObject;
import ch.specchio.types.attribute;
import ch.specchio.types.Units;

/**
 * Class for manipulating metadata in the database.
 */
public class MetadataFactory extends SPECCHIOFactory {
	
	/**
	 * Constructor. 
	 * 
	 * @param db_user		database account user name
	 * @param db_password	database account password
	 * @param is_admin	is the user an administrator? 
	 * 
	 * @throws SPECCHIOFactoryException	could not establish initial context
	 */
	public MetadataFactory(String db_user, String db_password, String ds_name, boolean is_admin) throws SPECCHIOFactoryException {

		super(db_user, db_password, ds_name, is_admin);
		
	}
	
	
	/**
	 * Copy constructor. Construct a new factory that uses the same database connection
	 * as an existing factory.
	 * 
	 * @param factory	the existing factory
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public MetadataFactory(SPECCHIOFactory factory) throws SPECCHIOFactoryException {
		
		super(factory);
		
	}
	
	/**
	 * Connect as admin
	 * 
	 * @param ds_name	data source name
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */	
	public MetadataFactory(String ds_name) throws SPECCHIOFactoryException {
		super(ds_name);
	}
	
	
	/**
	 * Check for metadata conflicts.
	 * 
	 * @param cd_d	the conflict detection descriptor
	 * 
	 * @return a hash mapping metadata field names to conflict information structures
	 * 
	 * @throws SPECCHIOFactoryException	could not connect to the database
	 */
	public ConflictTable detectConflicts(List<Integer> list, String[] fieldnames) throws SPECCHIOFactoryException {
		
		
		ConflictTable stati = new ConflictTable();


		try {
			
			ArrayList<String> field_cnt_strings = new ArrayList<String>();

			
			for (int i=0;i<fieldnames.length;i++)
			{
				field_cnt_strings.add("count(distinct " + fieldnames[i] + "_id" + ")");				
			}
			
			
			String query = getStatementBuilder().assemble_sql_select_query(getStatementBuilder().conc_cols(field_cnt_strings), "spectrum", "spectrum_id in (" + 
					getStatementBuilder().conc_ids((ArrayList<Integer>) list) +
					")");

	
			Statement stmt = getStatementBuilder().createStatement();
			ResultSet rs;
			ConflictStruct conflict;

			rs = stmt.executeQuery(query);
			
			while (rs.next()) {
				
				
				for (int j=1;j<=fieldnames.length;j++)
				{
					int cnt = rs.getInt(j);
					
					conflict = new ConflictStruct();
					
					if (cnt == 0) conflict.setStatus(0);
					if (cnt == 1) conflict.setStatus(1);
					if (cnt > 1) conflict.setStatus(2);
					
					ConflictInfo conflict_info = new ConflictInfo(conflict);
					stati.put(fieldnames[j-1], conflict_info);
					
				}
						
			
			}
			
			rs.close();	
			stmt.close();
				
		} catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
		return stati;
	}
	
	
	/**
	 * Check for conflicts in EAV metadata.
	 * 
	 * @param metadata_level		storage level to be checked
	 * @param ids	the attribute identifiers to be checked
	 * 
	 * @return a hash mapping attribute id to conflict information structures
	 * 
	 * @throws SPECCHIOFactoryException	could not connect to the database
	 */
	public ConflictTable detectEavConflicts(int metadata_level, ArrayList<Integer> ids) throws SPECCHIOFactoryException
	{
		ConflictTable statuses = new ConflictTable();
		
		if(metadata_level == MetaParameter.SPECTRUM_LEVEL)
		{
			statuses = detectEavConflicts(metadata_level, ids, MetaParameter.SPECTRUM_LEVEL);
			
			ArrayList<Integer> hierarchy_ids = getEavServices().getDirectHierarchyIds(ids);
			
			
			ConflictTable h_stati = detectEavConflicts(MetaParameter.HIERARCHY_LEVEL, hierarchy_ids, MetaParameter.SPECTRUM_LEVEL);
			statuses.addConflictTable(h_stati);
		}
		
		if(metadata_level == MetaParameter.HIERARCHY_LEVEL)
		{		
			statuses = detectEavConflicts(metadata_level, ids, MetaParameter.HIERARCHY_LEVEL);				
		}
		
		return statuses;
	}
	
	private ConflictTable detectEavConflicts(int metadata_level, ArrayList<Integer> ids, int calling_level) throws SPECCHIOFactoryException
	{	
		ConflictTable statuses = new ConflictTable();
		
		EAVTableAndRelationsInfoStructure eav_info = new EAVTableAndRelationsInfoStructure(this.getEavServices().getEAVTableAndRelationsInfoStructure(metadata_level));

		try {
			Statement stmt = getStatementBuilder().createStatement();
			
	
			boolean double_check = false;
//			int distinct_spectrum_id_cnt;
//			int distinct_eav_id_cnt;
			double multi_value_cnt;
			Integer attribute_id;
			Integer eav_id;
			Integer no_of_spectra = ids.size();
			Integer hierarchy_string_count= 0;
//			Integer eav_id_cnt_dist;
			ConflictStruct conflict;
			ArrayList<Integer> eav_ids_to_double_check = new ArrayList<Integer>();
			ArrayList<Integer> single_attributes = new ArrayList<Integer>();
			ArrayList<Integer> multi_attributes = new ArrayList<Integer>();
			ArrayList<Integer> inherited_eav_ids = new ArrayList<Integer>(); // inherited at hierarchy level
			ArrayList<Integer> all_hierarchies = null;
			
			String conc_ids = getStatementBuilder().conc_ids(ids);
			
			// Number of hierarchy strings:
			if(metadata_level == MetaParameter.HIERARCHY_LEVEL)
			{

				String query = "select count(*) from hierarchy_level where hierarchy_level_id in (" + conc_ids + ") and parent_level_id is null";
			
				ResultSet rs;
				
				rs = stmt.executeQuery(query);

				while (rs.next()) {
					hierarchy_string_count = rs.getInt(1);
				}
					
				rs.close();
				
				
				// a vertical integration of metaparameters is needed here
				 HashMap<Integer, ArrayList<Integer>> parent_hierarchies_grouped = getEavServices().getGroupedParentHierarchyIds(ids);
				
				ArrayList<Integer> parent_hierarchies = new ArrayList<Integer>();

				Set<Entry<Integer, ArrayList<Integer>>> set = parent_hierarchies_grouped.entrySet();
				Iterator<Entry<Integer, ArrayList<Integer>>> iterator = set.iterator();
				while(iterator.hasNext()) {
					Map.Entry<Integer, ArrayList<Integer>> mentry = (Map.Entry<Integer, ArrayList<Integer>>) iterator.next();
					parent_hierarchies.addAll((Collection<? extends Integer>) mentry.getValue());
				}
				
				all_hierarchies = new ArrayList<Integer>(parent_hierarchies);
				all_hierarchies.addAll(ids);

				int count = getEavServices().count_metaparameters(metadata_level, all_hierarchies);
				
				// avoid calls if there are no metadata attached to hierarchy
				if(count == 0)			
					return statuses;
				
			
				// create temporary links between all hierarchies and eav entries to support generic conflict detection
				
				// create temp table
				eav_info.primary_x_eav_tablename = getStatementBuilder().prefix(getTempDatabaseName(), "primary_x_eav");
				
					// create temporary table
				String ddl_string = "CREATE TEMPORARY TABLE IF NOT EXISTS " + eav_info.primary_x_eav_tablename + " " +
						"(" + eav_info.primary_id_name + " INT NOT NULL, " +
						"eav_id INT NOT NULL)";
				stmt.executeUpdate(ddl_string);		
				
				// ensure table is empty
				query = "delete from " + eav_info.primary_x_eav_tablename;
				stmt.executeUpdate(query);
				
				
				// fill temp table: down-propagation of eav_ids
				
				//ArrayList<Integer> metaparameter_ids;				
					
				
				// down propagation for each selected hierarchy
				iterator = set.iterator();
				while(iterator.hasNext()) {
					
					Map.Entry<Integer, ArrayList<Integer>> mentry = (Map.Entry<Integer, ArrayList<Integer>>) iterator.next();
					
					query = "insert into " + eav_info.primary_x_eav_tablename + " values ";
					
					ArrayList<String> values = new ArrayList<String>();					
					ArrayList<Integer> propagated_eav_ids;	
					
					// add bottom hierarchy to start of hierarchy list: ensures that in the following propagation all hierarchies are considered
					mentry.getValue().add(0, mentry.getKey());
					
					// propagation of current hierarchy
					for (ListIterator<Integer> it = mentry.getValue().listIterator(mentry.getValue().size()); it.hasPrevious();) {
						  Integer hierarchy_id = it.previous();
						  
						// get eav ids of current hierarchy
						propagated_eav_ids= getEavServices().get_eav_ids(MetaParameter.HIERARCHY_LEVEL, hierarchy_id);	
						  
						for(int eav_id_ : propagated_eav_ids)
						{
							values.add("(" + hierarchy_id + "," + eav_id_ +")");
							
							// store eav_ids that were inherited by the current hierarchies: used in the client for field colouring in metadata editor
							if(calling_level == MetaParameter.HIERARCHY_LEVEL)
							{
								if(parent_hierarchies.contains(hierarchy_id))
									inherited_eav_ids.add(eav_id_);
							}
							else
							{
								// spectra inherit also from their immediate hierarchy
								inherited_eav_ids.add(eav_id_);
							}
								
							
						}

					}
					
					// only insert into temporary table if any propagated values exist
					if(values.size() > 0)
					{
						query = query + getStatementBuilder().conc_values(values, false);
						stmt.executeUpdate(query);	
					}

					
					
				}
				
//				for(int id : all_hierarchies)
//				{
//					query = "insert into " + eav_info.primary_x_eav_tablename + " values ";
//					
//					ArrayList<String> values = new ArrayList<String>();
//					
//					for(int eav_id_ : metaparameter_ids)
//					{
//						values.add("(" + id + "," + eav_id_ +")");
//						
//						// store eav_ids that were inherited by the current hierarchies: used in the client for field colouring in metadata editor
//						if(parent_hierarchies.contains(id))
//							inherited_eav_ids.add(eav_id_);
//					}
//					
//					query = query + getStatementBuilder().conc_values(values, false);
//					
//					stmt.executeUpdate(query);
//					
//				}
				
				
				
				

				
			}
			
//			ArrayList<Integer> eav_ids_first_spectrum = this.getEavServices().get_eav_ids(ids[0]);
			
			// get the attributes occuring only once or multiple times
//			String query = "SELECT count(eav.eav_id), attribute_id " +
//					"from spectrum_x_eav sxe, eav eav where sxe.spectrum_id in (" +
//					getStatementBuilder().conc_ids(ids[0]) +
//					") and sxe.eav_id = eav.eav_id group by attribute_id order by attribute_id";	
			
			
			// create temp table
			String temp_tablename = getStatementBuilder().prefix(getTempDatabaseName(), "multi_attr_compilation");
			
			
				// create temporary table
			String ddl_string = "CREATE TEMPORARY TABLE IF NOT EXISTS " + temp_tablename + " " +
					"(eav_id_cnt INT NOT NULL, " +
					"attr_id INT NOT NULL)";
			stmt.executeUpdate(ddl_string);			
			
			
			
//			String query = "insert into " + temp_tablename + " " +
//			"(eav_id_cnt, attr_id) " + "SELECT count(eav.eav_id), attribute_id " +
//			"from spectrum_x_eav sxe, eav eav where sxe.spectrum_id in (" +
//			getStatementBuilder().conc_ids(ids) +
//			") and sxe.eav_id = eav.eav_id group by attribute_id, sxe.spectrum_id order by attribute_id";	
			
			String query = "insert into " + temp_tablename + " " +
					"(eav_id_cnt, attr_id) " + "SELECT count(eav.eav_id), attribute_id " +
					"from " + eav_info.primary_x_eav_tablename + " sxe, eav eav where sxe." + eav_info.primary_id_name + " in (" +
					conc_ids +
					") and sxe.eav_id = eav.eav_id group by attribute_id, sxe." + eav_info.primary_id_name + " order by attribute_id";				
		
			stmt.executeUpdate(query);
			
			query = "select eav_id_cnt, attr_id from " + temp_tablename + " group by attr_id, eav_id_cnt";
			
			ResultSet rs;
			
			rs = stmt.executeQuery(query);

			while (rs.next()) {
				
				int eav_cnt = rs.getInt(1);
				attribute_id = rs.getInt(2);
				
				if(eav_cnt == 1)
					single_attributes.add(attribute_id);
				else
					multi_attributes.add(attribute_id);
			}
					
			query = "delete from " + temp_tablename;
			stmt.executeUpdate(query);
			
			// deal with single metaparameters per attribute first
			
			if(single_attributes.size() > 0)
			{

				// Note: a conflict is detect if the attribute has a count different from the spectrum count; this is also happening  even if the value is the same but stored in different eav entries.
				//			query = "SELECT count(eav.binary_val), count(distinct OCTET_LENGTH(eav.binary_val)), " +
				//					"count(eav.int_val), count(distinct eav.int_val)," +
				//					"count(eav.string_val), count(distinct eav.string_val), " +
				//					"count(eav.double_val), count(distinct eav.double_val)," +
				//					"count(eav.datetime_val), count(distinct eav.datetime_val), " +
				//					"count(eav.taxonomy_id), count(distinct eav.taxonomy_id), " +
				//					(getEavServices().isSpatially_enabled()==true ? "count(eav.spatial_val), count(distinct eav.spatial_val), ST_GeometryType(spatial_val), " : "") +
				//					"count(distinct sxe.spectrum_id), eav.attribute_id, eav.eav_id, count(distinct eav.eav_id)" +
				//					" from spectrum_x_eav sxe, eav eav where sxe.spectrum_id in (" +
				//					getStatementBuilder().conc_ids(ids) +
				//				") and sxe.eav_id = eav.eav_id and eav.attribute_id in (" +
				//				getStatementBuilder().conc_ids(single_attributes) + ") " +
				//				"group by attribute_id, eav_id order by attribute_id";	

				// solves the issue of same value stored in different eav entries.
				//			query = "SELECT count(eav.binary_val), count(distinct OCTET_LENGTH(eav.binary_val)), " +
				//					"count(eav.int_val), count(distinct eav.int_val)," +
				//					"count(eav.string_val), count(distinct eav.string_val), " +
				//					"count(eav.double_val), count(distinct eav.double_val)," +
				//					"count(eav.datetime_val), count(distinct eav.datetime_val), " +
				//					"count(eav.taxonomy_id), count(distinct eav.taxonomy_id), " +
				//					(getEavServices().isSpatially_enabled()==true ? "count(eav.spatial_val), count(distinct eav.spatial_val), " : "") +
				//					"count(distinct sxe.spectrum_id), eav.attribute_id, count(distinct eav.eav_id)" +
				//					" from spectrum_x_eav sxe, eav eav where sxe.spectrum_id in (" +
				//					getStatementBuilder().conc_ids(ids) +
				//				") and sxe.eav_id = eav.eav_id and eav.attribute_id in (" +
				//				getStatementBuilder().conc_ids(single_attributes) + ") " +
				//				"group by attribute_id order by attribute_id";	

				query = "SELECT count(eav.binary_val), count(distinct OCTET_LENGTH(eav.binary_val)), " +
						"count(eav.int_val), count(distinct eav.int_val)," +
						"count(eav.string_val), count(distinct eav.string_val), " +
						"count(eav.double_val), count(distinct eav.double_val)," +
						"count(eav.datetime_val), count(distinct eav.datetime_val), " +
						"count(eav.taxonomy_id), count(distinct eav.taxonomy_id), " +
						(getEavServices().isSpatially_enabled()==true ? "count(eav.spatial_val), count(distinct eav.spatial_val), " : "") +
						"count(distinct sxe." + eav_info.primary_id_name + "), eav.attribute_id, count(distinct eav.eav_id)" +
						" from " + eav_info.primary_x_eav_tablename + " sxe, eav eav where sxe." + eav_info.primary_id_name + " in (" +
						conc_ids +
						") and sxe.eav_id = eav.eav_id and eav.attribute_id in (" +
						getStatementBuilder().conc_ids(single_attributes) + ") " +
						"group by attribute_id order by attribute_id";	



				rs = stmt.executeQuery(query);
				int i = 1;
				int status = -1;
				Integer int_val_max = null;
				Integer int_val_min = null;
				Double double_val_max = null;
				Double double_val_min = null;
				Double spat_val_x_min = null;
				Double spat_val_y_min = null;
				Double spat_val_x_max = null;
				Double spat_val_y_max = null;

				int spatial_val_cnt = 0;
				int spatial_val_cnt_dist = 0;
				String spatial_val_geom_type = "";

				ArrayList<Integer> conflicting_int_and_double_attribute_ids = new ArrayList<Integer>();

				//			ArrayList<ConflictStruct> all_conflicts = new ArrayList<ConflictStruct>();
				//			ArrayList<ConflictStruct> no_conflicts = new ArrayList<ConflictStruct>();

				ArrayList<Integer> no_conflict_attributes = new ArrayList<Integer>();

				Hashtable<Integer, ConflictStruct> attr_id_conflict_hash = new Hashtable<Integer, ConflictStruct>();


				while (rs.next()) {
					i = 1;

					int binary_val_cnt = rs.getInt(i++);
					int  binary_val_cnt_dist = rs.getInt(i++);				
					int int_val_cnt = rs.getInt(i++);
					int int_val_cnt_dist = rs.getInt(i++);
					int string_val_cnt = rs.getInt(i++);
					int string_val_cnt_dist = rs.getInt(i++);
					int double_val_cnt = rs.getInt(i++);
					int double_val_cnt_dist = rs.getInt(i++);			
					int datetime_val_cnt = rs.getInt(i++);
					int datetime_val_cnt_dist = rs.getInt(i++);				
					int taxonomy_val_cnt = rs.getInt(i++);
					int taxonomy_val_cnt_dist = rs.getInt(i++);
					if(getEavServices().isSpatially_enabled()==true)
					{
						spatial_val_cnt = rs.getInt(i++);
						spatial_val_cnt_dist = rs.getInt(i++);	
						//					spatial_val_geom_type = rs.getString(i++);	
					}				
					int spectrum_cnt = rs.getInt(i++);
					attribute_id = rs.getInt(i++);
					//				eav_id = rs.getInt(i++);
					//				int eav_id_cnt_dist = rs.getInt(i++);


					//				ConflictInfo conflict_info = stati.get(attribute_id);	
					//				if(conflict_info == null)
					//					conflict_info = new ConflictInfo();

					attribute attr = null;
					try{
						attr = this.getAttributes().get_attribute_info(attribute_id);
					}catch(java.lang.NullPointerException e) 
					{
						int x=1;
					}

					if(attr.default_storage_field.equals(attribute.INT_VAL))
					{
						status = getConflictStatus(int_val_cnt, int_val_cnt_dist, spectrum_cnt, no_of_spectra);	
						if (status == ConflictInfo.conflict)					
							conflicting_int_and_double_attribute_ids.add(attribute_id);
					}
					if(attr.default_storage_field.equals(attribute.STRING_VAL))
						status = getConflictStatus(string_val_cnt, string_val_cnt_dist, spectrum_cnt, no_of_spectra);
					if(attr.default_storage_field.equals(attribute.DOUBLE_VAL))
					{
						status = getConflictStatus(double_val_cnt, double_val_cnt_dist, spectrum_cnt, no_of_spectra);
						if (status == ConflictInfo.conflict)		
							conflicting_int_and_double_attribute_ids.add(attribute_id);			
					}
					if(attr.default_storage_field.equals(attribute.DATETIME_VAL))
						status = getConflictStatus(datetime_val_cnt, datetime_val_cnt_dist, spectrum_cnt, no_of_spectra);				
					if(attr.default_storage_field.equals(attribute.TAXONOMY_VAL))
						status = getConflictStatus(taxonomy_val_cnt, taxonomy_val_cnt_dist, spectrum_cnt, no_of_spectra);
					if(attr.default_storage_field.equals(attribute.BINARY_VAL))
						status = getConflictStatus(binary_val_cnt, binary_val_cnt_dist, spectrum_cnt, no_of_spectra);
					if(attr.default_storage_field.equals(attribute.SPATIAL_VAL))
					{
						status = getConflictStatus(spatial_val_cnt, spatial_val_cnt_dist, spectrum_cnt, no_of_spectra);
						if (status == ConflictInfo.conflict)		//  && spatial_val_geom_type.equals("POINT") -> only do this for points, but check that later as not possible in this query
							conflicting_int_and_double_attribute_ids.add(attribute_id);								
					}



					//				System.out.print(attr.getName() + ": " + status);

					conflict = new ConflictStruct(status, 1, ids.size());


					//				all_conflicts.add(conflict);

					attr_id_conflict_hash.put(attribute_id, conflict);

					if(status == ConflictInfo.no_conflict)
					{
						double_check = true;
						no_conflict_attributes.add(attribute_id);
					}
					else
						double_check = false;



					//				// check if the first spectrum is actually having this attribute
					//				if(eav_ids_first_spectrum.contains(eav_id))
					//				{
					//					conflict_info.addConflict(eav_id, conflict);
					//				}
					//				else
					//				{
					//					//System.out.print("Could not find " + attr.getName() + " for first spectrum");
					//					conflict_info.addConflict(eav_id, conflict);
					//				}
					//				
					//				
					//				
					//				
					//				stati.put(attribute_id, conflict_info);
					//					
					//				if(double_check)
					//					eav_ids_to_double_check.add(eav_id);



				}

				rs.close();

				// get the eav_ids for all conflicts
				//			query = "select attribute_id, eav.eav_id from spectrum_x_eav sxe, eav eav where sxe.spectrum_id in (" +
				//					getStatementBuilder().conc_ids(ids) +
				//				") and sxe.eav_id = eav.eav_id and eav.attribute_id in (" +
				//				getStatementBuilder().conc_ids(single_attributes) + ") " +
				//				"group by attribute_id, eav.eav_id order by attribute_id";	

				query = "select attribute_id, eav.eav_id from " + eav_info.primary_x_eav_tablename + " sxe, eav eav where sxe." + eav_info.primary_id_name + " in (" +
						conc_ids +
						") and sxe.eav_id = eav.eav_id and eav.attribute_id in (" +
						getStatementBuilder().conc_ids(single_attributes) + ") " +
						"group by attribute_id, eav.eav_id order by attribute_id";	


				rs = stmt.executeQuery(query);


				while (rs.next()) {
					i = 1;

					attribute_id = rs.getInt(i++);
					eav_id = rs.getInt(i++);

					conflict = attr_id_conflict_hash.get(attribute_id);

					// for hierarchies: insert information if this is a inherited parameter 
					if(metadata_level == MetaParameter.HIERARCHY_LEVEL)
					{
						if(inherited_eav_ids.contains(eav_id))
							conflict.setInherited(true);
					}					

					// add conflict info for this eav_id
					ConflictInfo conflict_info = statuses.get(attribute_id);	
					if(conflict_info == null)
						conflict_info = new ConflictInfo();				
					conflict_info.addConflict(eav_id, conflict);

					statuses.put(attribute_id, conflict_info);

					if(no_conflict_attributes.contains(attribute_id))
					{
						eav_ids_to_double_check.add(eav_id);
					}

				}

				rs.close();
				
				
				// for hierarchies: figure out if these data are shared by parallel hierarchies that are not selected
				if (metadata_level == MetaParameter.HIERARCHY_LEVEL && calling_level == MetaParameter.HIERARCHY_LEVEL)
				{
										
					temp_tablename = getStatementBuilder().prefix(getTempDatabaseName(), "parallel_eav_entries");


					// create temporary table: counting in left joins did somehow not work due to grouping issues; this way is also more transparent ...
					ddl_string = "CREATE TEMPORARY TABLE IF NOT EXISTS " + temp_tablename + " " +
							"(eav_id INT NOT NULL, " +
							"attr_id INT NOT NULL, " +
							"hierarchy_level_id INT NOT NULL)";
					stmt.executeUpdate(ddl_string);		
					

					query = "insert into " + temp_tablename + " " +
							"select distinct hxe.eav_id, eav.attribute_id, hxe.hierarchy_level_id from " + eav_info.primary_x_eav_tablename + " sxe left join eav eav on sxe.eav_id = eav.eav_id left join hierarchy_x_eav hxe on (sxe.eav_id = hxe.eav_id and hxe.hierarchy_level_id <> sxe.hierarchy_level_id) where hxe.hierarchy_level_id not in" + " (" +	conc_ids +")";				
				
					stmt.executeUpdate(query);
					
					
					query = "select attr_id, eav_id, count(hierarchy_level_id) from " + temp_tablename + " group by eav_id, attr_id";

					rs = stmt.executeQuery(query);
					
					while (rs.next()) {
						i = 1;
						attribute_id = rs.getInt(i++);
						eav_id = rs.getInt(i++);
						int parallel_sharing_cnt = rs.getInt(i++);

						conflict = attr_id_conflict_hash.get(attribute_id);
						
						if(conflict != null)						
							conflict.setNumberOfSharingRecords(parallel_sharing_cnt + conflict.getNumberOfSelectedRecords());
						
						int x = 1;
						
						
					}
					
					query = "delete from " + temp_tablename;
					stmt.executeUpdate(query);
					
				}
				
				

				// get min/max values for numerical values
				if(conflicting_int_and_double_attribute_ids.size()>0)
				{

					boolean spat_val_is_null = false;

					//				query = "select attribute_id, min(int_val), max(int_val),  min(double_val), max(double_val) " +
					//						(getEavServices().isSpatially_enabled()==true ? ", max(ST_x(spatial_val)),min(ST_x(spatial_val)),max(ST_y(spatial_val)),min(ST_y(spatial_val)), ST_GeometryType(spatial_val)" : "") +
					//						" from spectrum_x_eav sxe, eav eav where sxe.spectrum_id in (" +
					//						getStatementBuilder().conc_ids(ids) +
					//					") and sxe.eav_id = eav.eav_id and eav.attribute_id in (" +
					//					getStatementBuilder().conc_ids(conflicting_int_and_double_attribute_ids) + ") " +
					//					"group by attribute_id" + (getEavServices().isSpatially_enabled()==true ?", ST_GeometryType(spatial_val)" : "") + " order by attribute_id";

					query = "select attribute_id, min(int_val), max(int_val),  min(double_val), max(double_val) " +
							(getEavServices().isSpatially_enabled()==true ? ", max(ST_x(spatial_val)),min(ST_x(spatial_val)),max(ST_y(spatial_val)),min(ST_y(spatial_val)), ST_GeometryType(spatial_val)" : "") +
							" from " + eav_info.primary_x_eav_tablename + " sxe, eav eav where sxe." + eav_info.primary_id_name + " in (" +
							conc_ids +
							") and sxe.eav_id = eav.eav_id and eav.attribute_id in (" +
							getStatementBuilder().conc_ids(conflicting_int_and_double_attribute_ids) + ") " +
							"group by attribute_id" + (getEavServices().isSpatially_enabled()==true ?", ST_GeometryType(spatial_val)" : "") + " order by attribute_id";

					rs = stmt.executeQuery(query);

					while (rs.next()) {
						i = 1;

						attribute_id = rs.getInt(i++);
						int_val_min =  rs.getInt(i++);
						boolean int_is_null = rs.wasNull();
						int_val_max =  rs.getInt(i++);
						double_val_min = rs.getDouble(i++);
						boolean double_is_null = rs.wasNull();
						double_val_max = rs.getDouble(i++);
						if(getEavServices().isSpatially_enabled()==true)
						{
							spat_val_x_max = rs.getDouble(i++);
							spat_val_is_null = rs.wasNull();
							spat_val_x_min = rs.getDouble(i++);
							spat_val_y_max = rs.getDouble(i++);
							spat_val_y_min = rs.getDouble(i++);
							spatial_val_geom_type = rs.getString(i++);	
						}

						ConflictInfo conflict_info = statuses.get(attribute_id);

						if(!int_is_null)
						{
							conflict_info.int_val_min = int_val_min;
							conflict_info.int_val_max = int_val_max;
						}					

						if(!double_is_null)
						{
							conflict_info.double_val_min = double_val_min;
							conflict_info.double_val_max = double_val_max;
						}

						if(!spat_val_is_null && spatial_val_geom_type.equals("POINT"))
						{
							conflict_info.spat_val_x_max = spat_val_x_max;
							conflict_info.spat_val_y_max = spat_val_y_max;
							conflict_info.spat_val_x_min = spat_val_x_min;
							conflict_info.spat_val_y_min = spat_val_y_min;
						}					

						statuses.put(attribute_id, conflict_info);

					}



					rs.close();

				}
			}
			
			// deal with multiple metaparameters per attribute
			// Check each attribute exclusively
			for(Integer attr_id : multi_attributes)
			{
				
				attribute attr = this.getAttributes().get_attribute_info(attr_id);
				// get all eav_ids for this attribute from all spectra
				ArrayList<Integer> eav_ids = this.getEavServices().get_eav_ids(metadata_level, (metadata_level == MetaParameter.HIERARCHY_LEVEL ? all_hierarchies : ids), true, attr_id);
				
				for(int eav_id_ : eav_ids)
				{
				
//					query = "SELECT count(eav." + attr.getDefaultStorageField() + "), count(distinct eav." + attr.getDefaultStorageField() + "), " +
//							"count(distinct sxe.spectrum_id), eav.attribute_id, eav.eav_id, count(distinct eav.eav_id)" +
//							", eav." + attr.getDefaultStorageField() + 
//							" from spectrum_x_eav sxe, eav eav where sxe.spectrum_id in (" +
//							getStatementBuilder().conc_ids(ids) +
//						") and sxe.eav_id = eav.eav_id and eav.attribute_id = " + attr_id +
//						 " and eav." + attr.getDefaultStorageField() + " = (select " + attr.getDefaultStorageField() + " from eav where eav_id = " + eav_id_ + ")  group by eav.eav_id";	

					query = "SELECT count(eav." + attr.getDefaultStorageField() + "), count(distinct eav." + attr.getDefaultStorageField() + "), " +
							"count(distinct sxe." + eav_info.primary_id_name + "), eav.attribute_id, eav.eav_id, count(distinct eav.eav_id)" +
							", eav." + attr.getDefaultStorageField() + 
							" from " + eav_info.primary_x_eav_tablename + " sxe, eav eav where sxe." + eav_info.primary_id_name + " in (" +
							conc_ids +
						") and sxe.eav_id = eav.eav_id and eav.attribute_id = " + attr_id +
						 " and eav." + attr.getDefaultStorageField() + " = (select " + attr.getDefaultStorageField() + " from eav where eav_id = " + eav_id_ + ")  group by eav.eav_id";	
					
					rs = stmt.executeQuery(query);

					while (rs.next()) {
						int i = 1;
						
						int val_cnt = rs.getInt(i++);
						int val_cnt_dist = rs.getInt(i++);
						int spectrum_cnt = rs.getInt(i++);
						attribute_id = rs.getInt(i++);
						eav_id = rs.getInt(i++);
//						eav_id_cnt_dist = rs.getInt(i++);
//						Object value = rs.getObject(i++);
						
						
						
//						if(val_cnt == spectrum_cnt && val_cnt_dist == 1 && spectrum_cnt == ids.length)
//							status = ConflictInfo.no_conflict;
//						else if(val_cnt == 0)
//							status = ConflictInfo.non_existent;
//						else
//							status = ConflictInfo.conflict;		
						
						int status = getConflictStatus(val_cnt, val_cnt_dist, spectrum_cnt, no_of_spectra);
						
						
//						System.out.print(attr.getName() + " / " + value.toString() + ": " + status);
						
						conflict = new ConflictStruct(status, 1, ids.size());

						
						if(status == ConflictInfo.no_conflict)
							double_check = true;
						else
							double_check = false;
						
						
						ConflictInfo conflict_info = statuses.get(attribute_id);	
						if(conflict_info == null)
							conflict_info = new ConflictInfo();
						
						// for hierarchies: insert information if this is a inherited parameter 
						if(metadata_level == MetaParameter.HIERARCHY_LEVEL)
						{
							if(inherited_eav_ids.contains(eav_id))
								conflict.setInherited(true);
						}											
						
						conflict_info.addConflict(eav_id_, conflict);
												
						statuses.put(attribute_id, conflict_info);
						
						if(double_check)
							eav_ids_to_double_check.add(eav_id_);

						
					}
					
				
				}
				
			}
			
			
			
			

		
//				query = "SELECT count(distinct sxe.spectrum_id), count(distinct eav.eav_id), count(eav.eav_id)/count(distinct sxe.spectrum_id), attribute_id, eav.eav_id from spectrum_x_eav sxe, eav eav where sxe.spectrum_id in (" +
//				getStatementBuilder().conc_ids(ids) +
//					") and sxe.eav_id = eav.eav_id group by eav_id order by attribute_id";	
//				
//				rs = stmt.executeQuery(query);
//				
//				while (rs.next()) {
//				
//					distinct_spectrum_id_cnt = rs.getInt(1);		
//					distinct_eav_id_cnt = rs.getInt(2);	
//					multi_value_cnt = rs.getDouble(3);	
//					attribute_id  = rs.getInt(4);	
//					eav_id = rs.getInt(5);		
//					
//					conflict = new ConflictStruct();
//					if (distinct_eav_id_cnt == 1 && distinct_spectrum_id_cnt == ids.length)
//					{
//							conflict.setStatus(1); // unique
//							double_check = true;
//					}
//					else if (distinct_eav_id_cnt== 1 && distinct_spectrum_id_cnt != ids.length)
//					{
//						conflict.setStatus(2); // ambiguous
//						double_check = false;
//					}
//					else
//					{
//						System.out.println("unexpected case in conflict detection !!!!");
//					}
//					conflict.setNumberOfSelectedRecords(ids.length);
//					conflict.setNumberOfSharingRecords(1);					
//											
//					ConflictInfo conflict_info = stati.get(attribute_id);	
//					if(conflict_info == null)
//						conflict_info = new ConflictInfo();
//					
//					conflict_info.addConflict(eav_id, conflict);
//					
//					stati.put(attribute_id, conflict_info);
//						
//					if(double_check)
//						eav_ids_to_double_check.add(eav_id);
//					
//	
//				}

					
				// second query to figure out the number of total spectra referring to the eav entries (number of shared records)
			if (metadata_level == MetaParameter.SPECTRUM_LEVEL)
			{
				query = "SELECT count(sxe." + eav_info.primary_id_name + "), attribute_id, eav.eav_id from " + eav_info.primary_x_eav_tablename + " sxe, eav eav where sxe.eav_id in (" +
						getStatementBuilder().conc_ids(eav_ids_to_double_check) +
						") and sxe.eav_id = eav.eav_id group by eav.eav_id order by attribute_id";			

				rs = stmt.executeQuery(query);

				while (rs.next()) {

					multi_value_cnt = rs.getDouble(1);	
					attribute_id  = rs.getInt(2);				
					eav_id = rs.getInt(3);		

					if(multi_value_cnt > 1)
					{
						// update existing conflict data
						ConflictInfo conflict_info = statuses.get(attribute_id);

						conflict = conflict_info.getConflictData(eav_id);
						conflict.setStatus(ConflictInfo.no_conflict); // used to be set to a value of 3 ... I wonder why (AH, 12.07.2017)
						conflict.setNumberOfSharingRecords((int) multi_value_cnt);

					}					

				}				

				rs.close();						
				stmt.close();
			}

			
			
		} catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
		
		return statuses;
		
	}
	
	
	private int getConflictStatus(int val_cnt, int val_cnt_dist,
			int spectrum_cnt, int no_of_spectra) {
		
		int status;
		
		if(val_cnt == spectrum_cnt && val_cnt_dist == 1 && no_of_spectra == spectrum_cnt)
			status = ConflictInfo.no_conflict;
		else if(val_cnt == 0)
			status = ConflictInfo.non_existent;
		else
			status = ConflictInfo.conflict;		
		
		return status;
	}


	/**
	 * Get the list of attributes in a given category.
	 * 
	 * @param category_name		the name of the category
	 * 
	 * @returns an array of all the attributes in the given category
	 * 
	 * @throws invalid value for category_name
	 */
	public List<attribute> getAttributesForCategory(String category_name) {
		
		return getAttributes().get_attributes(category_name);
		
	}
	
	
	/**
	 * Get the units for an attribute.
	 * 
	 * @param attr	the attribute
	 * 
	 * @return a Units object representing the attribute's units
	 */
	public Units getAttributeUnits(attribute attr) {
		
		return getAttributes().get_units(attr);
		
		
	}
	
	 	
	/**
	 * Get a hash table mapping identifiers to names.
	 * 
	 * @param category	the category name
	 * 
	 * @returns a category table mapping identifiers to names
	 * 
	 * @throws SPECCHIOFactoryException	invalid value for category
	 */
	public CategoryTable getCategoryTable(String category) throws SPECCHIOFactoryException {
		
		CategoryTable table = new CategoryTable();
		
		String field = "name";
		
		if (category.equals(Spectrum.CALIBRATION))
		{
			field = "calibration_id";
			
		}
		
		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			String query = "select " + SQL.quote_identifier(category + "_id") + ","  + field + " from " + SQL.quote_identifier(category);
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				table.put(rs.getInt(1), rs.getString(2));
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return table;
		
	}
	
	
	/**
	 * Get calibration_ids for a list of spectra.
	 * 
	 * @param spectrum_ids	the spectrum identifiers
	 * 
	 * @return list of calibration_id , zero where no calibration is defined
	 * @throws SPECCHIOFactoryException 
	 */		
	public ArrayList<Integer> getCalibrationIds(ArrayList<Integer> spectrum_ids) throws SPECCHIOFactoryException {
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		
		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			String conc_ids = SQL.conc_ids(spectrum_ids);
			String query = "select calibration_id, spectrum_id from spectrum where spectrum_id in (" + conc_ids + ")" +
			"order by FIELD (spectrum_id, "+ conc_ids +")";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				ids.add(rs.getInt(1));
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return ids;
	}

	
	/**
	 * Get distinct values of an attribute
	 * 
	 * @param attribute_id	id of the required attribute
	 * 
	 * @return array of metaparameters
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public ArrayList<MetaParameter> getDistinctValuesOfAttribute(int attribute_id) {
		
		Metadata md = new Metadata();
		ArrayList<Integer> eav_ids = new ArrayList<Integer>();
		
		attribute attr = this.getAttributes().get_attribute_info(attribute_id);
		
		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			
			// select distinct eav values and get one eav_id as example for the later loading
			String query = "select " + attr.default_storage_field + ", max(eav_id) from eav where attribute_id = " + attribute_id + " group by string_val;";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				eav_ids.add(rs.getInt(2));
			}
			rs.close();
			stmt.close();

		

			
			this.getEavServices().metadata_bulk_loader(md, eav_ids);

		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}	
		
		return md.getEntries();
	}	
	
	
	/**
	 * Get instrument ids for a list of spectra.
	 * 
	 * @param spectrum_ids	the spectrum identifiers
	 * 
	 * @return list of instrument ids, zero where no instrument is defined
	 * @throws SPECCHIOFactoryException 
	 */		
	public ArrayList<Integer> getInstrumentIds(ArrayList<Integer> spectrum_ids) throws SPECCHIOFactoryException {
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		
		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			String conc_ids = SQL.conc_ids(spectrum_ids);
			String query = "select instrument_id, spectrum_id from spectrum where spectrum_id in (" + conc_ids + ")" +
			"order by FIELD (spectrum_id, "+ conc_ids +")";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				ids.add(rs.getInt(1));
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return ids;
	}


	/**
	 * Get the metadata categories per application domain
	 * 
	 * @return an array of ApplicationDomainCategories objects, or null if the information does not exist
	 *
	 * @throws SPECCHIOFactoryException
	 */
	public ApplicationDomainCategories[] getMetadataCategoriesForApplicationDomains() {

		ApplicationDomainCategories[] adcs;
		ArrayList<ApplicationDomainCategories> adcs_list = new ArrayList<ApplicationDomainCategories>();
		ArrayList<Integer> category_ids = new ArrayList<Integer>();
		int taxonomy_id = 0;
		int cur_taxonomy_id;
		int category_id;
		
		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			String query = "select taxonomy_id, category_id from taxonomy_x_category order by taxonomy_id";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				cur_taxonomy_id = rs.getInt(1);
				
				if(taxonomy_id != cur_taxonomy_id)
				{
										
					if(category_ids.size()>0)
					{
						ApplicationDomainCategories adc_tmp = new ApplicationDomainCategories();
						adc_tmp.setTaxonomy_id(taxonomy_id);
						adc_tmp.setCategory_ids(category_ids.toArray(new Integer[0]));						
						adcs_list.add(adc_tmp);
						
						category_ids = new ArrayList<Integer>();
					}
					
					taxonomy_id = cur_taxonomy_id;
					
				}
				
				category_id = rs.getInt(2);
				
				category_ids.add(category_id);
				
			}
			
			// store the last group as well
			if(category_ids.size()>0)
			{
				ApplicationDomainCategories adc_tmp = new ApplicationDomainCategories();
				adc_tmp.setTaxonomy_id(taxonomy_id);
				adc_tmp.setCategory_ids(category_ids.toArray(new Integer[0]));				
				adcs_list.add(adc_tmp);
			}
			
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}		
		
		
		adcs = adcs_list.toArray(new ApplicationDomainCategories[0]);
		
		return adcs;
		
		
	}	
	
	
	/**
	 * Get the metadata for a given spectrum.
	 * 
	 * @param hierarchy_id	the identifier of the hierarchy
	 * 
	 * @return the metadata for the specific hierarchy
	 * 
	 * @throws SPECCHIOFactoryException the hierarchy does not exist
	 */
	public Metadata getMetadataForHierarchy(int hierarchy_id) throws SPECCHIOFactoryException {
		
		Metadata md = new Metadata();
			
		md.setFrameId(hierarchy_id);
			
		ArrayList<Integer> metaparameter_ids = new ArrayList<Integer>();
		
		metaparameter_ids = getEavServices().get_eav_ids(MetaParameter.HIERARCHY_LEVEL, hierarchy_id);		
		
//		ArrayList<Integer> attr_ids_for_lazyloading = new ArrayList<Integer>();
		
//		attr_ids_for_lazyloading.add(this.getAttributes().get_attribute_id("Field Protocol"));
//		attr_ids_for_lazyloading.add(this.getAttributes().get_attribute_id("Experimental Design"));

		// bulk reading of metaparameters
		try {
			getEavServices().metadata_bulk_loader(md, metaparameter_ids);
		} catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}

		return md;
			
	}
	
	
	/**
	 * Get the metadata for a given spectrum.
	 * 
	 * @param spectrum_id	the identifier of the spectrum
	 * 
	 * @return the metadata for the specific spectrum
	 * 
	 * @throws SPECCHIOFactoryException the spectrum does not exist
	 */
	public Metadata getMetadataForSpectrum(int spectrum_id) throws SPECCHIOFactoryException {
		
		Metadata md = new Metadata();
			
		md.setFrameId(spectrum_id);
			
		ArrayList<Integer> metaparameter_ids = new ArrayList<Integer>();
		
		metaparameter_ids = getEavServices().get_eav_ids(MetaParameter.SPECTRUM_LEVEL, spectrum_id);		
		
//		ArrayList<Integer> attr_ids_for_lazyloading = new ArrayList<Integer>();
		
//		attr_ids_for_lazyloading.add(this.getAttributes().get_attribute_id("Field Protocol"));
//		attr_ids_for_lazyloading.add(this.getAttributes().get_attribute_id("Experimental Design"));

		// bulk reading of metaparameters
		try {
			getEavServices().metadata_bulk_loader(md, metaparameter_ids);
		} catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}

		return md;
			
	}	
	
	
	/**
	 * Get the meta-parameter of the given metaparameter identifier.
	 * 
	 * @param id		the metaparameter identifier for which to retrieve metadata
	 * 
	 * @return the meta-parameter object corresponding to the desired id
	 *
	 * @throws SPECCHIOFactoryException	database error
	 */
	public MetaParameter loadMetaParameter(int id) throws SPECCHIOFactoryException {

		try {
			return getEavServices().load_metaparameter(id);
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}

	}
	
	
	/**
	 * Get the meta-parameters of the given attributes for a list of spectrum identifiers.
	 * 
	 * @param metadata_level		storage level to be checked
	 * @param ids		the spectrum identifiers for which to retrieve metadata
	 * @param attrId	the attribute id
	 * @param distinct	if true, return distinct values only
	 * 
	 * @return a list of list of meta-parameter objects corresponding to the desired attribute of each input id
	 *
	 * @throws SPECCHIOFactoryException	database error
	 */
	public ArrayList<ArrayList<MetaParameter>> getMetaParameters(int metadata_level, ArrayList<Integer> ids, ArrayList<Integer> attrIds) throws SPECCHIOFactoryException {

		ArrayList<ArrayList<MetaParameter>> mp_lists = new ArrayList<ArrayList<MetaParameter>>();
		
		for(Integer attr_id : attrIds)
		{
			mp_lists.add(this.getMetaParameters(metadata_level, ids, attr_id, false));			
		}				
		
		return mp_lists;
	}
	
	/**
	 * Get the meta-parameters of a given attribute for a list of spectrum identifiers.
	 * 
	 * @param metadata_level		storage level to be checked
	 * @param id		the spectrum identifiers for which to retrieve metadata
	 * @param attrId	the attribute id
	 * @param distinct	if true, return distinct values only
	 * 
	 * @return a list of meta-parameter objects corresponding to the desired attribute of each input id
	 *
	 * @throws SPECCHIOFactoryException	database error
	 */
	public ArrayList<MetaParameter> getMetaParameters(int metadata_level, ArrayList<Integer> ids, Integer attrId, boolean distinct) throws SPECCHIOFactoryException {
		
		ArrayList<MetaParameter> mp_list = new ArrayList<MetaParameter>(ids.size());
		
//		Instant start = Instant.now();
		
		// add empty metaparameters where no values were found
		if(!distinct)
		{
			for(int i=0 ; i < ids.size() ; i++)
			{
				mp_list.add(MetaParameter.newInstance());
			}
		}
		
		boolean logic_switch = false;
		
			
		if (logic_switch)
		{
			// NOTE: this variation is actually a fair deal slower than the original code!
			
			// get metaparameter ids, then use metadata bulk loader
			
			ArrayList<Integer> attribute_ids = new ArrayList<Integer>();
			attribute_ids.add(attrId);
			
			ArrayList<Integer> eav_ids =  this.getEavServices().get_eav_ids_incl_inheritance(metadata_level, ids, attribute_ids);
			
			Metadata md = new Metadata();
			try {
				this.getEavServices().metadata_bulk_loader(md, eav_ids);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new SPECCHIOFactoryException(e);
			}
			
			
			if(!distinct)
			{
				// check if we got a problem: if the number metaparameters differs from the primary_ids, then some of them are supposedly empty
				
				if(ids.size() != md.getEntries().size())
				{
					
					System.out.println("Empty metaparameters are required!");
					int x = 1;
				}
				
//				md.g
//				
//				mp_list.set(ind, mp);
			}
			else
			{
				
			}		
			
			mp_list = md.getEntries();
			
			int x = 1;
			
		}
		else
		{
		try {
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			String primary_id_name = getEavServices().get_primary_id_name(metadata_level);
			String primary_x_eav_tablename = getEavServices().get_primary_x_eav_tablename(metadata_level);
//			String temp_tablename = SQL.prefix(getTempDatabaseName(), "eav_frame_compilation");
			
			
			attribute attr;
			//int attrId = getAttributes().get_attribute_id(attrName);
			if (attrId != 0) {
				// get the descriptor of the desired attribute
				attr = getAttributes().get_attribute_info(attrId);
				
//				long startTime = System.currentTimeMillis();
				
				// create temporary table
//				String ddl_string = "CREATE TEMPORARY TABLE IF NOT EXISTS " + temp_tablename + " " +
//						"(eav_id INT NOT NULL, " +
//						primary_id_name + " " +
//						"INT NOT NULL, " +
//						"id INT NOT NULL " +
//						"AUTO_INCREMENT, PRIMARY KEY (id))";
//				stmt.executeUpdate(ddl_string);
				
//				long stopTime = System.currentTimeMillis();
//			      long elapsedTime = stopTime - startTime;
//				System.out.println("CREATE TEMPORARY TABLE" + elapsedTime);
//				startTime = System.currentTimeMillis();

				// insert eav identifiers into the temporary table
				
				
				String conc_ids = SQL.conc_ids(ids);
//				String query = "insert into " + temp_tablename + " " +
//						"(eav_id, " +  primary_id_name + ") " +
//						"select eav_id, " + primary_id_name + " from " + primary_x_eav_tablename +
//							" where " + primary_id_name + " in (" + conc_ids + ") " +
//							" order by FIELD (" + primary_id_name + ", "+ conc_ids +")";
//				stmt.executeUpdate(query);
				
//				stopTime = System.currentTimeMillis();
//				elapsedTime = stopTime - startTime;
//				System.out.println("insert into TEMPORARY TABLE" + elapsedTime);
//				startTime = System.currentTimeMillis();
				
				// build the list of metaparameters
//				query = "select " + ((distinct)? "distinct " : "") + SQL.prefix("eav", attr.getDefaultStorageField()) + " " +
//						"from eav, " + temp_tablename + " efc " +
//						"where eav.attribute_id = "  + Integer.toString(attrId) + " and eav.eav_id = efc.eav_id order by efc.id";
				
				String storage_field =  SQL.prefix("eav", attr.getDefaultStorageField());
				if(attr.getDefaultStorageField().equals("spatial_val"))
					storage_field = "ST_AsText(" + SQL.prefix("eav", "spatial_val") + ")";				
				
				String query = "select " + ((distinct)? "distinct " : "") + storage_field + ", eav.eav_id " + ((!distinct)? "," + SQL.prefix(primary_x_eav_tablename, primary_id_name) : "") + ", eav.unit_id" +
						" from " + primary_x_eav_tablename + ", eav eav  where " + SQL.prefix(primary_x_eav_tablename, primary_id_name) + " in (" + conc_ids + ") and " +
						primary_x_eav_tablename + ".eav_id =" + " eav.eav_id and eav.attribute_id = "  + Integer.toString(attrId) + ((!distinct)? " order by FIELD (" + SQL.prefix(primary_x_eav_tablename, primary_id_name) + ", "+ conc_ids +")" : "");

				
				
				ResultSet rs = stmt.executeQuery(query);
				
				
				getMetaParametersFromResultSet(rs, mp_list, ids, attr, distinct);
				

				rs.close();		
				
				
//				stopTime = System.currentTimeMillis();
//				elapsedTime = stopTime - startTime;
//				System.out.println("get metaparameters" + elapsedTime);
//				startTime = System.currentTimeMillis();
				
				
				// clear temporary table
//				query = "delete from " + temp_tablename;
//				stmt.executeUpdate(query);
				
				// clean up
				stmt.close();
				
				
				// if this a specrum level call then metadata stored at hierarchy level must also be added
				if(metadata_level == MetaParameter.SPECTRUM_LEVEL)
				{

					stmt = SQL.createStatement();
					// select eav.double_val, eav.eav_id, hes.spectrum_id, eav.unit_id from eav eav, hierarchy_x_eav hex, hierarchy_level_x_spectrum hes  where hex.eav_id = eav.eav_id and  hex.hierarchy_level_id in (540, 538, 537) and hex.hierarchy_level_id = hes.hierarchy_level_id and hes.spectrum_id in (30546) and eav.attribute_id = 79 order by FIELD (hes.spectrum_id, 30546);
					
					primary_x_eav_tablename = getEavServices().get_primary_x_eav_tablename(MetaParameter.HIERARCHY_LEVEL);
					ArrayList<Integer> hierarchy_ids = getEavServices().getHierarchyIds(ids);
					
					query = "select " + ((distinct)? "distinct " : "") + storage_field + ", eav.eav_id " + ((!distinct)? SQL.prefix(", hxs", primary_id_name) : "") + ", eav.unit_id" +
							" from " + primary_x_eav_tablename + ", eav eav, hierarchy_level_x_spectrum hxs  where " + SQL.prefix(primary_x_eav_tablename, "hierarchy_level_id") + " in (" + SQL.conc_ids(hierarchy_ids) + ") and " +
							primary_x_eav_tablename + ".eav_id =" + " eav.eav_id and eav.attribute_id = " + Integer.toString(attrId) + " and " + primary_x_eav_tablename + ".hierarchy_level_id = hxs.hierarchy_level_id and hxs.spectrum_id in (" + conc_ids + ")"   + ((!distinct)? " order by FIELD (" + SQL.prefix("hxs", primary_id_name) + ", "+ conc_ids +")" : "");
					

					rs = stmt.executeQuery(query);
					
					getMetaParametersFromResultSet(rs, mp_list, ids, attr, distinct);
					
//					spectrum_id = 0;
//					ind = 0;
//					
//					while (rs.next()) 
//					{
//						int i = 1;
//						if(attr.getDefaultStorageField().equals("datetime_val"))
//						{
//								o = rs.getString(1);
//								DateTimeFormatter formatter = DateTimeFormat.forPattern(MetaDate.DEFAULT_DATE_FORMAT + ".S").withZoneUTC();
//								DateTime d = formatter.parseDateTime((String) o); 
//								o = d;
//						}
//						else
//							o = rs.getObject(i++);
//							
//
//						Integer id = rs.getInt(i++);
//						if(!distinct)
//						{
//							spectrum_id = rs.getInt(i++);
//							// get position of this spectrum in the list
//							ind = ids.indexOf(spectrum_id);
//						}
//						Integer unit_id = rs.getInt(i++);
//						
//						
//						if (o != null) {
//							try {
//								MetaParameter mp = MetaParameter.newInstance(attr, o);
//								mp.setEavId(id);
//								mp.setUnitId(unit_id);
//								
//								if(!distinct)
//								{
//									mp_list.set(ind, mp);
//								}
//								else
//								{
//									mp_list.add(mp);
//								}
//							}
//							catch (MetaParameterFormatException ex) {
//								// should never happen but we'll log an error just in case
//								System.err.println("Metaparameter format exception when converting " + attr.getDefaultStorageField() + " attribute.");
//							}
//						}
//
//					}
					rs.close();		
					
					// clean up
					stmt.close();
										
					
				}
				
				
			}
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		}
		
//		Instant finish = Instant.now();
//		long timeElapsed = Duration.between(start, finish).toMillis();		
		
		//System.out.println("Elapsed time for metadata loading: " + timeElapsed + "[ms]");
		
		return mp_list;
		
	}
	
	
	private void getMetaParametersFromResultSet(ResultSet rs, ArrayList<MetaParameter> mp_list, ArrayList<Integer> ids, attribute attr, boolean distinct) throws SPECCHIOFactoryException
	{
		
		Object o;
		Integer spectrum_id = 0;
		int ind;

		try {			
			
			while (rs.next()) 
			{
				int i = 1;
				
				if(attr.getDefaultStorageField().equals("datetime_val"))
				{
					o = rs.getString(i++);
					DateTimeFormatter formatter = DateTimeFormat.forPattern(MetaDate.DEFAULT_DATE_FORMAT + ".S").withZoneUTC();
					DateTime d = formatter.parseDateTime((String) o); 
					o = d;
				}
				//			if(attr.getDefaultStorageField().equals("spatial_val"))
				//			{
				//				o = rs.getString(1);
				//				try {
				//					MetaSpatialGeometry mp = (MetaSpatialGeometry) MetaParameter.newInstance(attr, o);
				//					
				//					o = mp.getValue();
				//							
				//				} catch (MetaParameterFormatException e) {
				//					// TODO Auto-generated catch block
				//					e.printStackTrace();
				//				}
				//				
				//				
				//				
				//			}
				else if (attr.getDefaultStorageField().equals("binary_val"))
				{

					// read the object from the database
					Blob binary_val = rs.getBlob(i++);		
					try {
						ByteArrayInputStream bis = new ByteArrayInputStream(binary_val.getBytes(1, (int) binary_val.length()));
						ObjectInputStream in = new ObjectInputStream(bis);
						o = in.readObject();
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


				}				
				else
					o = rs.getObject(i++);


				Integer id = rs.getInt(i++);
				if(!distinct)
				{
					spectrum_id = rs.getInt(i++);
					// get position of this spectrum in the list
					ind = ids.indexOf(spectrum_id);
				}
				Integer unit_id = rs.getInt(i++);

				// get position of this spectrum in the list
				ind = ids.indexOf(spectrum_id);


				if (o != null) {
					try {
						MetaParameter mp = MetaParameter.newInstance(attr, o);
						mp.setEavId(id);
						mp.setUnitId(unit_id);

						if(!distinct)
						{
							mp_list.set(ind, mp);
						}
						else
						{
							mp_list.add(mp);
						}
					}
					catch (MetaParameterFormatException ex) {
						// should never happen but we'll log an error just in case
						System.err.println("Metaparameter format exception when converting " + attr.getDefaultStorageField() + " attribute.");
					}
				}

			}		


		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		
	}
	
	/**
	 * Get statistics of metaparameter values for given spectrum ids and attribute.
	 * 
	 * @param metadata_level		storage level to be checked
	 * @param ids		the spectrum identifiers for which to retrieve metadata
	 * @param attrId	the attribute id
	 * 
	 * @return an object with min, mean and max values stored as Metaparameters
	 * 
	 * @throws SPECCHIOFactoryException	could not connect to the database, or invalid value for attribute name
	 */	
	public MetaparameterStatistics getMetaParameterStatistics(int metadata_level, ArrayList<Integer> ids, Integer attrId) throws SPECCHIOFactoryException {
		
		MetaparameterStatistics mps = new MetaparameterStatistics();
		MetaParameter min_mp = null, mean_mp = null, max_mp = null;
		
		ArrayList<Integer> attrIds = new ArrayList<Integer>();
		attrIds.add(attrId);
		
		ArrayList<Integer> eav_ids = this.getEavServices().get_eav_ids_incl_inheritance(metadata_level, ids, attrIds);
		
		
		attribute attr = getAttributes().get_attribute_info(attrId);
		
		SQL_StatementBuilder SQL = getStatementBuilder();
		try {
			Statement stmt = SQL.createStatement();
			
			String avg_str = "avg(" + attr.getDefaultStorageField() + ")";
			if(attr.getDefaultStorageField().equals("datetime_val"))
			{
				avg_str = "FROM_UNIXTIME(round(avg(UNIX_TIMESTAMP(" + attr.getDefaultStorageField() + "))))";
			}
			
			
			String query = "select min(" + attr.getDefaultStorageField() + "), " + avg_str + ", max(" + attr.getDefaultStorageField() + ") from eav where eav_id in ("
					+ SQL.conc_ids(eav_ids) + ")";
			
			
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				
				Object o = getObjectFromResultSet(rs, attr, 1);	
				min_mp = MetaParameter.newInstance(attr, o);
				min_mp.setEavId(0);
				
				o = getObjectFromResultSet(rs, attr, 2);					
				mean_mp = MetaParameter.newInstance(attr, o);
				mean_mp.setEavId(0);				

				o = getObjectFromResultSet(rs, attr, 3);				
				max_mp = MetaParameter.newInstance(attr, o);
				max_mp.setEavId(0);					
				
				
			}
			rs.close();
			stmt.close();
			
			
			mps.setMp_min(min_mp);
			mps.setMp_mean(mean_mp);
			mps.setMp_max(max_mp);
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MetaParameterFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return mps;
			
		
	}
	
	
	private Object getObjectFromResultSet(ResultSet rs, attribute attr, int i) throws SQLException
	{
		Object o = null;
		
		if(attr.getDefaultStorageField().equals("datetime_val"))
		{
			o = rs.getString(i);
			DateTimeFormatter formatter = DateTimeFormat.forPattern(MetaDate.DEFAULT_DATE_FORMAT + ".S").withZoneUTC();
			DateTime d = formatter.parseDateTime((String) o); 
			o = d;
		}
		else
			o = rs.getObject(i);		
		
		return o;
		
	}
	
	
	/**
	 * Get the values of a given meta-parameter for an array of spectrum identifiers.
	 * 
	 * @param ids		the spectrum identifiers for which to retrieve metadata
	 * @param attrId	the attribute identifier
	 * @param distinct	if true, return distinct values only
	 * 
	 * @return a list of meta-parameter objects corresponding to the desired attribute of each input id
	 *
	 * @throws SPECCHIOFactoryException	database error
	 */
	public ArrayList<MetaParameter> getMetaParameters(int metadata_level, int ids[], Integer attrId, boolean distinct) throws SPECCHIOFactoryException {
		
		// convert the array to a list
		ArrayList<Integer> ids_list = new ArrayList<Integer>(ids.length);
		for (int i : ids) {
			ids_list.add(i);
		}
		
		return getMetaParameters(metadata_level, ids_list, attrId, distinct);
		
	}

	
	/**
	 * Get the values of a given meta-parameter for a list of spectrum identifiers.
	 * 
	 * @param id		the spectrum identifiers for which to retrieve metadata
	 * @param attrName	the attribute name
	 * @param distinct	if true, return distinct values only
	 * 
	 * @return a list of meta-parameter objects corresponding to the desired attribute of each input id
	 *
	 * @throws SPECCHIOFactoryException	database error
	 */
	public ArrayList<MetaParameter> getMetaParameters(int metadata_level, ArrayList<Integer> ids, String attrName, boolean distinct) throws SPECCHIOFactoryException {
		
		return getMetaParameters(metadata_level, ids, getAttributes().get_attribute_id(attrName), distinct);
		
	}

	
	/**
	 * Get the values of a given meta-parameter for an array of spectrum identifiers.
	 * 
	 * @param id		the spectrum identifiers for which to retrieve metadata
	 * @param attrName	the attribute name
	 * @param distinct	if true, return distinct values only
	 * 
	 * @return a list of meta-parameter objects corresponding to the desired attribute of each input id
	 *
	 * @throws SPECCHIOFactoryException	database error
	 */
	public ArrayList<MetaParameter> getMetaParameters(int metadata_level, int ids[], String attrName, boolean distinct) throws SPECCHIOFactoryException {
		
		return getMetaParameters(metadata_level, ids, getAttributes().get_attribute_id(attrName), distinct);
		
	}
	
	
	/**
	 * Get newest N spectra.
	 * 
	 * @param number_of_spectra N
	 * 
	 * @return list of spectrum ids ordered by data ingestion time
	 */	
	public ArrayList<Integer> getNewestSpectra(int number_of_spectra) {

		ArrayList<Integer> ids = new ArrayList<Integer>();
		
		
		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();

			String query = "select s.spectrum_id, eav.datetime_val from spectrum s, spectrum_x_eav sxe, eav eav where s.spectrum_id = sxe.spectrum_id and sxe.eav_id = eav.eav_id and eav.attribute_id = " + getAttributes().get_attribute_id("Loading Time") + " order by eav.datetime_val desc limit " + number_of_spectra;
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				ids.add(rs.getInt(1));
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}	
		
		return ids;
		
	}	
	
	
	
	/**
	 * Get the data policies for a collection of space.
	 * 
	 * @param space	the space
	 * 
	 * @return a list of Strings representing the policies that apply to the input spae
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public ArrayList<String> getPoliciesForSpace(Space space) throws SPECCHIOFactoryException {
		
		// get a list of spectrum identifiers associated with policies
		int data_policy_attr_id = getAttributes().get_attribute_id("Data Usage Policy");
		ArrayList<Integer> spectra_with_policies = getEavServices().filter_by_eav(MetaParameter.SPECTRUM_LEVEL, space.getSpectrumIds(), data_policy_attr_id);
		
		// retrieve the policy objects and convert to strings
		ArrayList<String> policies = new ArrayList<String>();
		if(spectra_with_policies.size() > 0) {
			for (Object value : getMetaParameters(MetaParameter.SPECTRUM_LEVEL, spectra_with_policies, data_policy_attr_id, true)) {
				policies.add(((MetaParameter) value).getValue().toString());
			}
		}
		
		return policies;
		
	}
	
	
	
	/**
	 * Get the root node of a taxonomy
	 * 
	 * @param attribute_id	id of the required taxonomy
	 * 
	 * @return taxonomy node
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */	
	public TaxonomyNodeObject getTaxonomyRoot(int attribute_id) {
	
		 // get attribute info to get name of taxonomy
		 attribute attr_info = this.getAttributes().get_attribute_info(attribute_id);
		 
		 TaxonomyNodeObject root = new TaxonomyNodeObject(attr_info.getName(), attribute_id);
		 			 		 
		 return root;	
	 }
	 
	
	/**
	 * Get the node of a taxonomy
	 * 
	 * @param taxonomy_id	id of the required taxonomy
	 * 
	 * @return taxonomy node
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	
	public TaxonomyNodeObject getTaxonomyObject(int taxonomy_id) throws SPECCHIOFactoryException {
		
		// loading children of root
		String query = "select attribute_id, name, code, description from taxonomy where taxonomy_id = " + taxonomy_id  + " order by name";
		
		TaxonomyNodeObject tn = null;
		
		Statement stmt;
		try {
			stmt = getStatementBuilder().createStatement();
			
			
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {	
				
				int i = 1;
				int attribute_id = rs.getInt(i++);
				String name = rs.getString(i++);
				String code = rs.getString(i++);
				String description = rs.getString(i++);
				
				tn = new TaxonomyNodeObject(name, attribute_id, taxonomy_id, null);
				tn.setCode(code);
				tn.setDescription(description);
						
			}			
			rs.close();
			stmt.close();			
		} catch (SQLException e) {
			throw new SPECCHIOFactoryException(e);
		}

		return tn;
	}
	
	
	/**
	 * Get the children of a taxonomy node
	 * 
	 * @param parent_node	node whose children are required
	 * 
	 * @return taxonomy node list
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */	
	public ArrayList<TaxonomyNodeObject> getTaxonomyChildren(TaxonomyNodeObject parent_node) throws SPECCHIOFactoryException {
		
		ArrayList<TaxonomyNodeObject> children = new ArrayList<TaxonomyNodeObject>();
		
		try {
			Statement stmt = getStatementBuilder().createStatement();
			String query = "select taxonomy_id, name, code, description from taxonomy where " +
					"attribute_id = " + parent_node.getAttribute_id() + " and " +
					( (parent_node.getId() == 0) ? "parent_id is null" : "parent_id = " + parent_node.getId()) + " order by name";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {	
				int i = 1;
				int taxonomy_id = rs.getInt(i++);
				String name = rs.getString(i++);
				String code = rs.getString(i++);
				String description = rs.getString(i++);
				
				TaxonomyNodeObject child = new TaxonomyNodeObject(name, parent_node.getAttribute_id(), taxonomy_id, parent_node);
				child.setCode(code);
				child.setDescription(description);
				children.add(child);
						
			}			
			rs.close();
			stmt.close();			
		} catch (SQLException e) {
			throw new SPECCHIOFactoryException(e);
		}
		
		return children;
		
	}
	
	
	/**
	 * Get all children of a taxonomy node in a recursive manner
	 * 
	 * @param parent_node	node whose children are required
	 * 
	 * @return taxonomy node list
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */	
	public ArrayList<TaxonomyNodeObject> getTaxonomyChildrenRecursively(TaxonomyNodeObject parent_node) throws SPECCHIOFactoryException {
		
		ArrayList<TaxonomyNodeObject> children = new ArrayList<TaxonomyNodeObject>();
		
		try {
			Statement stmt = getStatementBuilder().createStatement();
			String query = "select taxonomy_id, name, code, description from taxonomy where " +
					"attribute_id = " + parent_node.getAttribute_id() + " and " +
					( (parent_node.getId() == 0) ? "parent_id is null" : "parent_id = " + parent_node.getId()) + " order by name";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {	
				int i = 1;
				int taxonomy_id = rs.getInt(i++);
				String name = rs.getString(i++);
				String code = rs.getString(i++);
				String description = rs.getString(i++);
				
				TaxonomyNodeObject child = new TaxonomyNodeObject(name, parent_node.getAttribute_id(), taxonomy_id, parent_node);
				child.setCode(code);
				child.setDescription(description);
				children.add(child);
				
				ArrayList<TaxonomyNodeObject> sub_children = getTaxonomyChildrenRecursively(child);
				
				children.addAll(sub_children);
						
			}			
			rs.close();
			stmt.close();			
		} catch (SQLException e) {
			throw new SPECCHIOFactoryException(e);
		}
		
		return children;
		
	}	
	
	
	public Taxonomy getTaxonomy(int attribute_id) throws SPECCHIOFactoryException {
		
		Hashtable<Integer, String> tree_hash = new Hashtable<Integer, String>();		
		Taxonomy t = new Taxonomy(getAttributes().get_attribute_info(attribute_id));
		ArrayList<Integer> parent_ids = new ArrayList<Integer>();
		
		// build hashtable containing only the leaves
		try {
			Statement stmt = getStatementBuilder().createStatement();
			String query = "select taxonomy_id, name, parent_id from taxonomy where " +
					"attribute_id = " + attribute_id + " order by name";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {	
				int i = 1;
				int taxonomy_id = rs.getInt(i++);
				String name = rs.getString(i++);
				int parent_id = rs.getInt(i++);
				
				// insert into leaf_hash and taxonomy
				tree_hash.put(taxonomy_id, name);
				t.put(name, taxonomy_id);
				parent_ids.add(parent_id);
					
			}			
			rs.close();
			stmt.close();			
		} catch (SQLException e) {
			throw new SPECCHIOFactoryException(e);
		}
		
		// check leaf_hash and remove the ones that are parents: the leaves are left
		for(Integer parent_id : parent_ids)
		{
			if(tree_hash.containsKey(parent_id))
			{
				t.remove(tree_hash.get(parent_id));
			}
		}
		
		
		return t;
		
	}
	

	 
	 
	
	/**
	 * Remove metadata.
	 * 
	 * @param mp	the item of metadata to be removed
	 * @param ids	the list of spectra from which the metadata is to be removed (null for all spectra)
	 */
	public void removeMetadata(MetaParameter mp, ArrayList<Integer> ids) {
		
		getEavServices().delete_primary_x_eav(mp.getLevel(), ids, mp.getEavId(), this.Is_admin());
		
	}
	
	/**
	 * Remove metaparameters of given attribute id from all primary entities.
	 * 
	 * @param mp	the attribute id of the metaparameters to be removed
	 * @param ids	the list of primary entities from which the metadata is to be removed (null for all primary entities)
	 */
	public void removeMetadata(int metadata_level, int attribute_id, ArrayList<Integer> ids) {
		
		// get all eav ids for that attribute and spectra ids
		ArrayList<Integer> eav_ids = getEavServices().get_eav_ids(metadata_level, ids, attribute_id);
		
		for(int eav_id : eav_ids)
		{
			getEavServices().delete_primary_x_eav(metadata_level, ids, eav_id, this.Is_admin());
		}
		
	}
	
	

	/**
	 * Remove metadata from all spectra and hierarchies.
	 * 
	 * @param mp		the item of metadata to be removed
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public void removeMetadata(MetaParameter mp) throws SPECCHIOFactoryException {
		
		try {
			EAVDBServices eav = getEavServices();
			ArrayList<Integer> spectrum_ids = eav.getPrimaryIds(MetaParameter.SPECTRUM_LEVEL, mp.getEavId());
			eav.delete_primary_x_eav(mp.getLevel(), spectrum_ids, mp.getEavId(), this.Is_admin());
			ArrayList<Integer> hierarchy_ids = eav.getPrimaryIds(MetaParameter.HIERARCHY_LEVEL, mp.getEavId());
			eav.delete_primary_x_eav(mp.getLevel(), hierarchy_ids, mp.getEavId(), this.Is_admin());
			
			eav.delete_eav(mp.getEavId(), this.Is_admin());
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	
	/**
	 * Update an item of metadata for a given set of identifiers.
	 * 
	 * @param mp		the new metadata
	 * @param spectrumIds		the identifiers of the spectra to be updated
	 * 
	 * @return the identifier of the inserted metadata
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public int updateMetadata(MetaParameter mp, ArrayList<Integer> spectrumIds) throws SPECCHIOFactoryException {
		
		int eav_id = 0;

		try {
			EAVDBServices eav = getEavServices();
			if(mp.getEavId() == 0)
			{
				// get the campaign to which these metaparameters belong
				int campaign_id = 0;
				if (spectrumIds.size() > 0) {
					SpecchioCampaignFactory scf = new SpecchioCampaignFactory(this);		
					if(mp.getLevel() == MetaParameter.SPECTRUM_LEVEL)
						campaign_id = scf.getCampaignIdForSpectrum(spectrumIds.get(0));
					if(mp.getLevel() == MetaParameter.HIERARCHY_LEVEL)
						campaign_id = scf.getCampaignIdForHierarchy(spectrumIds.get(0));
					
					
					scf.dispose();
				}
				
				eav_id = eav.insert_metaparameter_into_db(campaign_id, mp, true, this.Is_admin()); // reduce redundancy is included in this call
				eav.insert_primary_x_eav(mp.getLevel(), spectrumIds, eav_id);
			}
			else
			{
				eav.update_metaparameter(mp, this.Is_admin());
				eav_id = mp.getEavId();
			}
		}
		catch (IOException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return eav_id;
		
	}
	
	
	/**
	 * Update and re-identify an item of metadata for a given set of identifiers.
	 * 
	 * @param mp		the new metadata
	 * @param ids		the identifiers of the spectra to be updated
	 * 
	 * @return the identifier of the inserted metadata
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public int updateMetadataWithNewId(MetaParameter mp, ArrayList<Integer> ids) throws SPECCHIOFactoryException {
		
		int eav_id;

		try {
			EAVDBServices eav = getEavServices();
			
			// save the old id so that we can delete it later
			Integer old_eav_id = mp.getEavId();
			Integer old_campaign_id = eav.get_campaign_id_for_eav(mp.getEavId());
			
			// force re-insertion of the meta-parameter by setting its eav id to 0
			mp.setEavId(0);
			eav_id = eav.insert_metaparameter_into_db(old_campaign_id, mp, false, this.Is_admin()); // no redundancy reduction
			
			// insert the new id and remove the old one
			eav.insert_primary_x_eav(mp.getLevel(), ids, mp.getEavId());			
			eav.delete_primary_x_eav(mp.getLevel(), ids, old_eav_id, this.Is_admin());
		}
		catch (IOException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return eav_id;
		
	}


	public int updateMetadataAnnotation(MetaParameter mp, ArrayList<Integer> arrayList)  throws SPECCHIOFactoryException {

		try {
			
			EAVDBServices eav = getEavServices();
			eav.update_eav_annotation(mp.getEavId(), mp.getAnnotation());

		} catch (SQLException e) {
			throw new SPECCHIOFactoryException(e);
		}		

		return mp.getEavId();
	}

	/**
	 * Update or insert EAV metadata. Will automatically update existing entries or insert a new metaparameter if not existing.
	 * 
	 * @param metaParameter		the new metadata
	 * @param ids		the identifiers of the spectra to be updated or inserted
	 * 
	 * @return the identifier of the inserted or updated metadata
	 */	
	public int updateOrInsertEavMetadata(MetaParameter metaParameter, ArrayList<Integer> ids, int metadata_level) {

		int mp_id = 0;
		
		// check if metaparameter(s) exists
		ArrayList<Integer> eav_ids = getEavServices().get_eav_ids(metadata_level, ids, true, metaParameter.getAttributeId());
		
		//ArrayList<MetaParameter> mps = getMetaParameters(metadata_level,ids, metaParameter.getAttributeId(), true);
		
		if(eav_ids.size() == 0)
		{
			// must be inserted for all ids
			mp_id = this.updateMetadata(metaParameter, ids);
			
		}
		else
		{
			// exists at least for some of the ids
			MetaParameter mp = null;
			
			// update the values of the existing metaparameters
			for(int eav_id : eav_ids)
			{
				try {
					mp = getEavServices().load_metaparameter(eav_id);
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					mp.setValue(metaParameter.getValue());
				} catch (MetaParameterFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mp_id = updateMetadata(mp, ids);
			}
			
			
			
			
			// figure out if some ids have no entry yet
			ArrayList<Integer> to_insert = getEavServices().filter_by_attribute_NOT(metadata_level, ids, metaParameter.getAttributeId());
			
			if(to_insert.size() > 0) 
			{		
				if(mp.getEavId() == 0)
					mp_id = updateMetadata(mp, to_insert); // use first entry in existing list to insert where the metaparameter does not yet exist
				else
					try {
						getEavServices().insert_primary_x_eav(mp.getLevel(), to_insert, mp.getEavId());
					} catch (SQLException e) {
						throw new SPECCHIOFactoryException(e);
					}
			}
			
			
		}
		
		
		
		return mp_id;
	}










}
