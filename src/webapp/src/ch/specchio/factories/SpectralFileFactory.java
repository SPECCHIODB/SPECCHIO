package ch.specchio.factories;

import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.ListIterator;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import ch.specchio.spaces.MeasurementUnit;
import ch.specchio.types.Campaign;
import ch.specchio.types.Instrument;
import ch.specchio.types.MetaDate;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.MetaSpatialGeometry;
import ch.specchio.types.Metadata;
import ch.specchio.types.Point2D;
import ch.specchio.types.SpecchioMessage;
import ch.specchio.types.SpectralFile;
import ch.specchio.types.SpectralFileInsertResult;
import ch.specchio.types.SpectrumDataLink;
import ch.specchio.eav_db.SQL_StatementBuilder;
import ch.specchio.eav_db.SpectralFileInsertStruct;
import ch.specchio.eav_db.id_and_op_struct;


/**
 * Class for manipulating spectral files in the database.
 */
public class SpectralFileFactory extends SPECCHIOFactory {
	
	/** the campaign to which this factory belongs */
	private Campaign campaign = null;
	
	/** campaign factory for inserting hierarchy nodes */
	private SpecchioCampaignFactory campaign_factory = null;
	
	/**
	 * Constructor. This constructor builds a spectral file factory that is not associated with
	 * a particular campaign. Some factory methods cannot be used without a campaign.
	 * 
	 * @param db_user		database account user name
	 * @param db_password	database account password
	 * @param is_admin	is the user an administrator? 
	 * 
	 * @throws SPECCHIOFactoryException	could not establish initial context
	 */
	public SpectralFileFactory(String db_user, String db_password, String ds_name, boolean is_admin) throws SPECCHIOFactoryException {

		super(db_user, db_password, ds_name, is_admin);
		
	}
	
	
	public SpectralFileFactory(String ds_name) throws SPECCHIOFactoryException {

		super(ds_name);
		
	}
	
	
	/**
	 * Constructor. The constructor builds a spectral file factory that is associated with a
	 * particular campaign.
	 * 
	 * @param db_user		database account user name
	 * @param db_password	database account password
	 * @param ds_name		datasource name
	 * @param is_admin	is the user an administrator? 
	 * @param campaign_id	the identifier of the campaign to which the spectra will belong
	 * 
	 * @throws SPECCHIOFactoryException	could not establish initial context
	 */
	public SpectralFileFactory(String db_user, String db_password, boolean is_admin, String ds_name, int campaign_id) throws SPECCHIOFactoryException {
		
		this(db_user, db_password, ds_name, is_admin);
		
		// save campaign information for later
		campaign_factory = new SpecchioCampaignFactory(this);
		campaign = campaign_factory.getCampaign(campaign_id, is_admin);
		
	}

	
	/**
	 * Copy constructor. Construct a new factory that uses the same database connection
	 * as an existing factory.
	 * 
	 * @param factory	the existing factory
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public SpectralFileFactory(SPECCHIOFactory factory) throws SPECCHIOFactoryException {
		
		super(factory);
		
	}
		
	
	
	/**
	 * Get the file format identifier for a given file format name.
	 * 
	 * @param file_format_name	the file format name
	 * 
	 * @returns the identifier corresponding to file_format_name, or -1 of the name was not found
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	public int getIdForFileFormat(String file_format_name) throws SPECCHIOFactoryException {
		
		Integer file_format_id = -1;
		
		try {
		// initialise to "not found"
//		int file_format_id = -1;
//		
//		try {
//		
//			String query = "SELECT file_format_id from file_format where name = '" + file_format_name + "'";
//			Statement stmt = getStatementBuilder().createStatement();
//			ResultSet rs = stmt.executeQuery(query);	
//			while (rs.next())
//			{
//				file_format_id = rs.getInt(1);
//			}
//			rs.close();
//			stmt.close();
//			
//		}
//		catch (SQLException ex) {
//			// bad SQL
//			throw new SPECCHIOFactoryException(ex);
//		}
//		
//		return file_format_id;
		
			file_format_id = this.getDataCache().get_file_format_id(file_format_name);
			System.out.println(file_format_name);
		}
		catch (NullPointerException ex) {
			
			throw new SPECCHIOFactoryException("New file format could not be inserted; check that the file_format_name is set in the spectral file.");
		}
		
		return file_format_id;
	
	}
	

	/**
	 * Get the identifier of a sub-hierarchy with a given name, creating the
	 * hierarchy if it doesn't exist.
	 * 
	 * @param parent_id			the identifier of the the parent of the hierarchy
	 * @param hierarchy_name	the name of the desired hierarchy
	 * 
	 * @return the identifier of the child of parent_id with the name hierarchy_name
	 *
	 * @throws SPECCHIOFactoryException	database error
	 */
	public int getSubHierarchyId(int parent_id, String hierarchy_name) throws SPECCHIOFactoryException {
		
		campaign_factory = new SpecchioCampaignFactory(this);
		
		// see if the node already exists
		int sub_hierarchy_id = campaign_factory.getHierarchyNodeId(hierarchy_name, parent_id);
		
		if (sub_hierarchy_id == -1) {
			// the sub-hierarchy doesn't exist; create it
			sub_hierarchy_id = campaign_factory.insertHierarchyNode(hierarchy_name, parent_id);
		}
		
		return sub_hierarchy_id;
		
	}
	
	
	private void getSubHierarchyId(Hashtable<String, Integer> subhierarchies, int parent_id, String hierarchy_name) throws SPECCHIOFactoryException
	{
		Integer subhierarchy_id = getSubHierarchyId(parent_id, hierarchy_name);		
		subhierarchies.put(hierarchy_name, subhierarchy_id);		
	}
	
	
	
	private Hashtable<String, Integer> getSubHierarchyIds(SpectralFile spec_file, int hierarchy_id) throws SPECCHIOFactoryException
	{
		Hashtable<String, Integer> subhierarchies = new Hashtable<String, Integer>();
		
		if (spec_file.getCompany().equals("SVC")
				&& spec_file.getInstrumentTypeNumber() == 1024
				&& spec_file.getNumberOfSpectra() == 3) {

			// HR 1024 files contain target and reference radiances plus
			// reflectances
			// thus here we must split them into two new hierarchies:
			// reflectance and radiance/DN with radiance/DN having two subfolders
			String unit = null;
			if(spec_file.getMeasurementUnits(0) == MeasurementUnit.Radiance)
			{
				unit = "Radiance";
			}
			else if(spec_file.getMeasurementUnits(0) == MeasurementUnit.Irradiance)
			{
				unit = "Irradiance";
			}
			else
			{
				unit = "DN";
			}
			

			getSubHierarchyId(subhierarchies, hierarchy_id, unit);
			getSubHierarchyId(subhierarchies, hierarchy_id, "Reflectance");
			getSubHierarchyId(subhierarchies, subhierarchies.get(unit), "Targets");
			getSubHierarchyId(subhierarchies, subhierarchies.get(unit), "References");
		}

		if (spec_file.getCompany().equals("COST_OO_CSV")) {

			// COST_OO_CSV files contain target and reference radiances plus
			// reflectances
			// thus here we must split them into two new hierarchies:
			// reflectance and radiance with radiance having two subfolders
			
			getSubHierarchyId(subhierarchies, hierarchy_id, "Radiance");
			getSubHierarchyId(subhierarchies, hierarchy_id, "Reflectance");
			getSubHierarchyId(subhierarchies, subhierarchies.get("Radiance"), "Targets");
			getSubHierarchyId(subhierarchies, subhierarchies.get("Radiance"), "References");		

		}
		
		if (spec_file.getCompany().equals("Spectral Evolution")) {
			
			boolean contains_targets_and_references = false;
			boolean contains_several_units = false;
			
			HashSet<Integer> hs = new HashSet<Integer>();
			hs.addAll(spec_file.getMeasurementUnits());
			
			if(hs.size() > 1)
			{
				contains_several_units = true;
			}
			
			// contains targets and references if there are duplication of units
			if(hs.size() < spec_file.getMeasurementUnits().size())
			{
				contains_targets_and_references = true;
			}
			
			
			if(spec_file.getMeasurementUnits().contains(MeasurementUnit.DN))// && !contains_several_units)
			{
				getSubHierarchyId(subhierarchies, hierarchy_id, "DN");
				
				if (contains_targets_and_references)
				{
					getSubHierarchyId(subhierarchies, subhierarchies.get("DN"), "Targets");
					getSubHierarchyId(subhierarchies, subhierarchies.get("DN"), "References");							
				}
				
			}
			
			if(spec_file.getMeasurementUnits().contains(MeasurementUnit.Radiance))// && !contains_several_units)
			{
				getSubHierarchyId(subhierarchies, hierarchy_id, "Radiance");
				
				if (contains_targets_and_references)
				{
					getSubHierarchyId(subhierarchies, subhierarchies.get("Radiance"), "Targets");
					getSubHierarchyId(subhierarchies, subhierarchies.get("Radiance"), "References");							
				}
				
			}	
			
			if(spec_file.getMeasurementUnits().contains(MeasurementUnit.Reflectance))// && !contains_several_units)
			{
				getSubHierarchyId(subhierarchies, hierarchy_id, "Reflectance");
							
			}
			
			
//			if(contains_targets_and_references && contains_several_units)
//			{
//				getSubHierarchyId(subhierarchies, hierarchy_id, "Targets");
//				getSubHierarchyId(subhierarchies, hierarchy_id, "References");
//			}
			
			
		}
		
//		if (spec_file.getCompany().equals("PP Systems") && spec_file.getCapturingSoftwareName() != null && ( 
//				spec_file.getCapturingSoftwareName().equals("UnispecDCcf")
//				|| spec_file.getCapturingSoftwareName().equals("UniSpec Dual Channel NETCF")
//				)) {
		if (spec_file.getCompany().equals("PP Systems") && spec_file.getFileFormatName().equals("UniSpec_SPU")) 
		{				

			// PP dual channel files contain target and irradiance DNs
			// thus here we must split them into two new hierarchies
			getSubHierarchyId(subhierarchies, hierarchy_id, "DN");
			getSubHierarchyId(subhierarchies, subhierarchies.get("DN"), "channel a");
			getSubHierarchyId(subhierarchies, subhierarchies.get("DN"), "channel b");					
			
		}
		
		if (spec_file.getCompany().equals("Solar Systems") && spec_file.getFileFormatName().equals("MicrotopsCSV")) 
		{				
			getSubHierarchyId(subhierarchies, hierarchy_id, "Spectrum");
			getSubHierarchyId(subhierarchies, hierarchy_id, "Spectrum StdDev");
			getSubHierarchyId(subhierarchies, hierarchy_id, "AOT");	
		}		

		if (spec_file.getCompany().equals("GER") || (spec_file.getCompany().equals("PP Systems") && spec_file.getCapturingSoftwareName() == null) && !spec_file.getFileFormatName().equals("UniSpec_SPU")) {
			// GER files contain target and reference radiances
			// thus here we must split them into two new hierarchies
			getSubHierarchyId(subhierarchies, hierarchy_id, "Targets");
			getSubHierarchyId(subhierarchies, hierarchy_id, "References");
		}

		if (spec_file.getCompany().equals("YES")) {
			// if the supplied hierarchy is a new one then we must insert new
			// sub hierarchies
			getSubHierarchyId(subhierarchies, hierarchy_id, "Total");
			getSubHierarchyId(subhierarchies, hierarchy_id, "Diffuse");
		}
		
		if (spec_file.getCompany().equals("JB Hyperspectral")) {
			// Input data are always digital numbers
			getSubHierarchyId(subhierarchies, hierarchy_id, "DN");
		}

		if (spec_file.getCompany().equals("FGI")) {

			// create new sub hierarchy for this data collection
			String[] tokens = spec_file.getBasename().split("\\.");
			getSubHierarchyId(subhierarchies, hierarchy_id, tokens[0]);
			int FGI_collection_id = subhierarchies.get(tokens[0]);

			// create sub hierarchies within the data collection for HDRF, BRF,
			// etc

			if (spec_file.isFgiHdrfBrfCombined() == false) {
				getSubHierarchyId(subhierarchies, FGI_collection_id, tokens[1]);
				int FGI_sub_id = subhierarchies.get(tokens[1]);
				
				if (spec_file.getSpectralGroupingSize() > 1) {
					getSubHierarchyId(subhierarchies, FGI_sub_id, "R");
					getSubHierarchyId(subhierarchies, FGI_sub_id, "Mueller10");
					getSubHierarchyId(subhierarchies, FGI_sub_id, "Mueller20");
				}


			} else // hdrf_brf_combined == true
			{
				getSubHierarchyId(subhierarchies, FGI_collection_id, "BRF");
				getSubHierarchyId(subhierarchies, FGI_collection_id, "HDRF");
			}

		}

		if (spec_file.getAsdV7() == true) {
			
			if(spec_file.getCreate_DN_folder_for_asd_files() || ((spec_file.getAsdV7RadianceFlag() == false) && (spec_file.getAsdV7ReflectanceFlag() == false)))
				getSubHierarchyId(subhierarchies, hierarchy_id, "DN");

			// insert radiance spectrum
			if (spec_file.getAsdV7RadianceFlag() == true) {
				getSubHierarchyId(subhierarchies, hierarchy_id, "Radiance");
			}

			// insert reflectance spectrum
			if (spec_file.getAsdV7ReflectanceFlag() == true) {
				getSubHierarchyId(subhierarchies, hierarchy_id, "Reflectance");
			}			

		}
		
		if (spec_file.getCreateUnitFolderForasdOldFiles() == true) {
			
			if (spec_file.getMeasurementUnits(0).equals(MeasurementUnit.Reflectance)) {
				getSubHierarchyId(subhierarchies, hierarchy_id, "Reflectance");
			}
			
			if (spec_file.getMeasurementUnits(0).equals(MeasurementUnit.Radiance)) {
				getSubHierarchyId(subhierarchies, hierarchy_id, "Radiance");
			}
			
			if (spec_file.getMeasurementUnits(0).equals(MeasurementUnit.DN)) {
				getSubHierarchyId(subhierarchies, hierarchy_id, "DN");
			}
			
			if (spec_file.getMeasurementUnits(0).equals(MeasurementUnit.Irradiance)) {
				getSubHierarchyId(subhierarchies, hierarchy_id, "Irradiance");
			}			

			
		}
		
		return subhierarchies;
	}
	
	private hierarchy_existence_data getExistanceData(SpectralFile spec_file, int hierarchy_id) {
	
		hierarchy_existence_data exists_struct = new hierarchy_existence_data();
		
		exists_struct.hierarchy_id = hierarchy_id;

		if(spec_file.getNumberOfSpectra() > 1)
		{
			ArrayList<SpectralFile> spectral_file_list = new ArrayList<SpectralFile>();
			spectral_file_list.add(spec_file);
			exists_struct = spectraExist_(spectral_file_list , hierarchy_id);
		}
		else
		{
			exists_struct.exist_array.add(false); // single file entries get always inserted, otherwise the client would not have sent it.
		}
		
		return exists_struct;
	}
	

	void insertHierarchySpectrumReferences(int hierarchy_id, int spectrum_id, int recursion_break_at_parent_id, Statement stmt) throws SPECCHIOFactoryException {
		ArrayList<Integer> spectrum_ids = new ArrayList<Integer>();
	
		spectrum_ids.add(spectrum_id);
		insertHierarchySpectrumReferences(hierarchy_id, spectrum_ids,
				recursion_break_at_parent_id, stmt);
	
	}

	void insertHierarchySpectrumReferences(int hierarchy_id, int spectrum_id, Statement stmt) throws SPECCHIOFactoryException {

		insertHierarchySpectrumReferences(hierarchy_id, spectrum_id, 0, stmt);	
	}
	

	public void insertHierarchySpectrumReferences(int hierarchy_id, ArrayList<Integer> spectrum_ids, Statement stmt) throws SPECCHIOFactoryException {
		insertHierarchySpectrumReferences(hierarchy_id, spectrum_ids, 0, stmt);
	}
	

	void insertHierarchySpectrumReferences(int hierarchy_id, ArrayList<Integer> spectrum_ids, int recursion_break_at_hierarchy_id, Statement stmt) throws SPECCHIOFactoryException {
		if (spectrum_ids.size() == 0)
			return; // catch empty list

		try {
			
			SQL_StatementBuilder SQL = getStatementBuilder();
		
			ArrayList<String> value_strings = new ArrayList<String>();
			String query = "insert into hierarchy_level_x_spectrum_view (hierarchy_level_id, spectrum_id) values ";
//			Statement stmt = getStatementBuilder().createStatement();

			// build value list
			ListIterator<Integer> li = spectrum_ids.listIterator();
			while (li.hasNext()) {
				String str = "(" + hierarchy_id + "," + li.next() + ")";
				value_strings.add(str);
			}
			query = query + SQL.conc_cols(value_strings);
			stmt.executeUpdate(query);

			// check if there is a parent of this hierarchy
			if (hierarchy_id != recursion_break_at_hierarchy_id) {
				query = "select parent_level_id from hierarchy_level where hierarchy_level_id = " + hierarchy_id;
				ResultSet rs = stmt.executeQuery(query);
				if (rs.next()) {
					int parent_id = rs.getInt(1);
	
					if (parent_id != 0) {
						insertHierarchySpectrumReferences(parent_id, spectrum_ids, recursion_break_at_hierarchy_id, stmt);
					}
				}
				rs.close();
			}

//			stmt.close();

		} catch (SQLException ex) {
			throw new SPECCHIOFactoryException(ex);
		}

	}
	

	/**
	 * Insert a link between two spectra.
	 * 
	 * @param dl	the data link
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 * @throws  
	 */
	public void insertLink(SpectrumDataLink dl) throws SPECCHIOFactoryException {
		
		
		// insert eav for target and reference
		// link target to reference
		try {		
			MetaParameter mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Target Data Link", "Data Links"));

			mp.setValue(dl.getReferencingId());


//			Integer[] spectrum_id_array = new Integer[1];
//			spectrum_id_array[0]=dl.getReferencedId();
			ArrayList<Integer> ids = new ArrayList<Integer>();
			ids.add(dl.getReferencedId());
			

			MetadataFactory MF = new MetadataFactory(this);

			MF.updateMetadata(mp, ids);

			// link reference to target
			mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Reference Data Link", "Data Links"));
			mp.setValue(dl.getReferencedId());

			//spectrum_id_array[0]=dl.getReferencingId();
			ids.clear();
			ids.add(dl.getReferencingId());			

			MF.updateMetadata(mp, ids);		

		} catch (MetaParameterFormatException e) {
			// TODO Auto-generated catch block
			throw new SPECCHIOFactoryException(e);
		}		

	}
	
	
	/**
	 * Insert a link between spectra establishing the provenance
	 * 
	 * @param dl	the data link
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 * @throws  
	 */
	public void insertProvenance(SpectrumDataLink dl) throws SPECCHIOFactoryException {
		

		// link child to parent
		try {		
			MetaParameter mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Provenance Data Link", "Data Links"));

			mp.setValue(dl.getReferencedId());


//			Integer[] spectrum_id_array = new Integer[1];
//			spectrum_id_array[0]=dl.getReferencingId();
			ArrayList<Integer> ids = new ArrayList<Integer>();
			ids.add(dl.getReferencingId());			

			MetadataFactory MF = new MetadataFactory(this);

			MF.updateMetadata(mp, ids);


		} catch (MetaParameterFormatException e) {
			// TODO Auto-generated catch block
			throw new SPECCHIOFactoryException(e);
		}		

	}
	

	
	private void addNonNullSpectrumIds(SpectralFileInsertResult insert_results, SpectralFileInsertResult result) {
		
		if(result.getSpectrumIds().get(0) > 0)
		{
			insert_results.add(result);
		}
		
	}
	
	
	/**
	 * Insert a spectral file into the database. This method handles the cases where
	 * there is more than one spectrum stored in a single file.
	 * 
	 * @param spec_file		the file to be inserted
	 * @param hierarchy_id	the identifier of the hierarchy node under which it is to be inserted
	 * 
	 * @return descriptor of a list of the spectrum identifiers and any errors created during the insertion
	 *
	 * @throws SPECCHIOFactoryException	the file could not be inserted
	 */
	public SpectralFileInsertResult insertSpectralFile(SpectralFile spec_file, int hierarchy_id) throws SPECCHIOFactoryException {

		SpectralFileInsertResult insert_result = new SpectralFileInsertResult();
		
		boolean special_hierarchy_files = false;
		spec_file.setLoading_time(null); // reset loading time (JAXB causes it to already be set)
		
		Hashtable<String, Integer> subhierarchies = getSubHierarchyIds(spec_file, hierarchy_id);
		
		Statement stmt = null;
		try {
			stmt = getStatementBuilder().createStatement();
			stmt.getConnection().setAutoCommit(false);
			
			stmt.execute("START TRANSACTION");

		if (spec_file.getCompany().equals("SVC")
				&& spec_file.getInstrumentTypeNumber() == 1024
				&& spec_file.getNumberOfSpectra() == 3) {
			special_hierarchy_files = true;
			SpectralFileInsertResult tgt_spectrum_result, ref_spectrum_result, reflectance_spectrum_result;

			// HR 1024 files contain target and reference radiances or DNs plus
			// reflectances
			// thus here we must split them into two new hierarchies:
			// reflectance and radiance/DN with radiance/DN having two subfolders

			// insert target spectrum
			tgt_spectrum_result = insertSpectrumAndHierarchyLink(spec_file, 1, spectrumExists_(spec_file, subhierarchies.get("Targets")), stmt);
			addNonNullSpectrumIds(insert_result,tgt_spectrum_result);

			// insert reference spectrum
			ref_spectrum_result = insertSpectrumAndHierarchyLink(spec_file, 0, spectrumExists_(spec_file, subhierarchies.get("References")), stmt);
			addNonNullSpectrumIds(insert_result,ref_spectrum_result);

			// insert reflectance spectrum
			reflectance_spectrum_result = insertSpectrumAndHierarchyLink(spec_file, 2, spectrumExists_(spec_file, subhierarchies.get("Reflectance")), stmt);
			addNonNullSpectrumIds(insert_result,reflectance_spectrum_result);
			
			// if spectra were inserted we need to add a datalink that links the
			// target to the reference spectrum
			if (ref_spectrum_result.getSpectrumIds().get(0) != 0) {
				insertLink(new SpectrumDataLink(tgt_spectrum_result.getSpectrumIds().get(0), ref_spectrum_result.getSpectrumIds().get(0), null));
				insertProvenance(new SpectrumDataLink(reflectance_spectrum_result.getSpectrumIds().get(0), tgt_spectrum_result.getSpectrumIds().get(0), null));
				insertProvenance(new SpectrumDataLink(reflectance_spectrum_result.getSpectrumIds().get(0), ref_spectrum_result.getSpectrumIds().get(0), null));
			}

		}

		if (spec_file.getCompany().equals("COST_OO_CSV")) {
			special_hierarchy_files = true;
			SpectralFileInsertResult tgt_spectrum_result, ref_result, reflectance_spectrum_result;

			// COST_OO_CSV files contain target and reference radiances plus
			// reflectances
			// thus here we must split them into two new hierarchies:
			// reflectance and radiance with radiance having two subfolders

			// insert target spectrum
			tgt_spectrum_result = insertSpectrumAndHierarchyLink(spec_file, 1, spectrumExists_(spec_file, subhierarchies.get("Targets")), stmt);
			addNonNullSpectrumIds(insert_result,tgt_spectrum_result);

			// insert reference spectrum
			ref_result = insertSpectrumAndHierarchyLink(spec_file, 0, spectrumExists_(spec_file, subhierarchies.get("References")), stmt);
			addNonNullSpectrumIds(insert_result,ref_result);

			// insert reflectance spectrum
			reflectance_spectrum_result = insertSpectrumAndHierarchyLink(spec_file, 2, spectrumExists_(spec_file, subhierarchies.get("Reflectance")), stmt);
			addNonNullSpectrumIds(insert_result,reflectance_spectrum_result);
			
			// if spectra were inserted we need to add a datalink that links the
			// target to the reference spectrum
			if (ref_result.getSpectrumIds().get(0) != 0) {
				insertLink(new SpectrumDataLink(tgt_spectrum_result.getSpectrumIds().get(0), ref_result.getSpectrumIds().get(0), "Spectralon data"));
				insertProvenance(new SpectrumDataLink(reflectance_spectrum_result.getSpectrumIds().get(0), tgt_spectrum_result.getSpectrumIds().get(0), "Radiance data"));
				insertProvenance(new SpectrumDataLink(reflectance_spectrum_result.getSpectrumIds().get(0), ref_result.getSpectrumIds().get(0), "Radiance data"));
			}

		}
		
		
		if (spec_file.getCompany().equals("Spectral Evolution")) {
			
			SpectralFileInsertResult tgt_spectrum_result = null, ref_result = null;
			
			boolean contains_targets_and_references = false;
			boolean contains_several_units = false;
			
			HashSet<Integer> hs = new HashSet<Integer>();
			hs.addAll(spec_file.getMeasurementUnits());
			
			if(hs.size() > 1)
			{
				contains_several_units = true;
			}
			
			// contains targets and references if there duplication of units
			if(hs.size() < spec_file.getMeasurementUnits().size())
			{
				contains_targets_and_references = true;
			}
			
			if (contains_several_units || contains_targets_and_references)
			{
				special_hierarchy_files = true;
				
			
				// loop over all spectra and insert into appropriate hierarchy
				for (int i=0;i<spec_file.getNumberOfSpectra();i++)
				{
					String first_sub_hierarchy = "";
					int first_sub_hierarchy_id = 0;
					
					if(spec_file.getMeasurementUnits(i) == MeasurementUnit.DN)
					{						
						first_sub_hierarchy = "DN";
						
						first_sub_hierarchy_id = subhierarchies.get(first_sub_hierarchy);
						
					}
					
					if(spec_file.getMeasurementUnits(i) == MeasurementUnit.Radiance)
					{						
						first_sub_hierarchy = "Radiance";
						
						first_sub_hierarchy_id = subhierarchies.get(first_sub_hierarchy);
					}	
					
					if(spec_file.getMeasurementUnits(i) == MeasurementUnit.Reflectance)
					{						
						first_sub_hierarchy = "Reflectance";
						
						first_sub_hierarchy_id = subhierarchies.get(first_sub_hierarchy);
					}						
					
					
					if(contains_targets_and_references)
					{
						if(spec_file.getMeasurandDesignator(i) != SpectralFile.UNSPECIFIED)
						{
							if(spec_file.getMeasurandDesignator(i) == SpectralFile.TARGET)
							{
								hierarchy_id = getSubHierarchyId(first_sub_hierarchy_id, "Targets");								
							}
							else
							{
								hierarchy_id = getSubHierarchyId(first_sub_hierarchy_id, "References");
							}
							
						}
						else
						{
							hierarchy_id = first_sub_hierarchy_id;
						}
					}
					else
					{
						// in this case the first_sub_hierarchy must be set, otherwise we would not be here ...
						
						hierarchy_id = first_sub_hierarchy_id;
						
					}
					
					
					SpectralFileInsertResult res = insertSpectrumAndHierarchyLink(spec_file, i, spectrumExists_(spec_file, hierarchy_id), stmt);
					
					addNonNullSpectrumIds(insert_result,res);
					
					if(spec_file.getMeasurandDesignator(i) == SpectralFile.REFERENCE)
					{
						ref_result = res;
					}

					
					if(spec_file.getMeasurandDesignator(i) == SpectralFile.TARGET)
					{
						tgt_spectrum_result = res;
						// as targets appear after the reference in the input file, we can safely assume that these last two can be linked
						insertLink(new SpectrumDataLink(tgt_spectrum_result.getSpectrumIds().get(0), ref_result.getSpectrumIds().get(0), "Spectralon data"));						
					}
					
					
					if(spec_file.getMeasurementUnits(i) == MeasurementUnit.Reflectance && contains_targets_and_references)
					{
						insertProvenance(new SpectrumDataLink(res.getSpectrumIds().get(0), tgt_spectrum_result.getSpectrumIds().get(0), "Radiance data"));
						insertProvenance(new SpectrumDataLink(res.getSpectrumIds().get(0), ref_result.getSpectrumIds().get(0), "Radiance data"));
					}
						
			
				}
			}
			
		}
		
		
//		if (spec_file.getCompany().equals("PP Systems") && spec_file.getCapturingSoftwareName() != null && ( 
//				spec_file.getCapturingSoftwareName().equals("UnispecDCcf")
//				|| spec_file.getCapturingSoftwareName().equals("UniSpec Dual Channel NETCF" ||)
//				)) {
		
		if (spec_file.getCompany().equals("PP Systems") && spec_file.getFileFormatName().equals("UniSpec_SPU")) 
		{		
			special_hierarchy_files = true;
			
			// insert a spectrum
			addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, 1, spectrumExists_(spec_file, subhierarchies.get("channel a")), stmt));
			// insert b spectrum
			addNonNullSpectrumIds(insert_result, insertSpectrumAndHierarchyLink(spec_file, 0, spectrumExists_(spec_file, subhierarchies.get("channel b")), stmt));
						
		}
		
		
		if (spec_file.getCompany().equals("Solar Systems") && spec_file.getFileFormatName().equals("MicrotopsCSV")) 
		{		
			special_hierarchy_files = true;
			
//			long startTime = System.currentTimeMillis();
			
			// get existance data for full file (must faster than doing the check for each entry in the loop below)
			hierarchy_existence_data Spectrum_exists = spectrumExists_(spec_file, subhierarchies.get("Spectrum"));
			hierarchy_existence_data SpectrumSTD_exists = spectrumExists_(spec_file, subhierarchies.get("Spectrum StdDev"));
			hierarchy_existence_data AOT_exists = spectrumExists_(spec_file, subhierarchies.get("AOT"));
			
			// insert spectrum, stddev and aot
			int i=0;
			while(i<spec_file.getNumberOfSpectra())
			{
				
				
				addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, i++, Spectrum_exists, stmt));
				addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, i++, SpectrumSTD_exists, stmt));
				addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, i++, AOT_exists, stmt));			
			}	
			
//			long stopTime = System.currentTimeMillis();
//		    long elapsedTime = stopTime - startTime;
//			System.out.println("insert microtops [ms] " + elapsedTime);		
//			
//			int x = 1;
			
		}
		

		if (spec_file.getCompany().equals("GER") || (spec_file.getCompany().equals("PP Systems") && spec_file.getCapturingSoftwareName() == null)  && !spec_file.getFileFormatName().equals("UniSpec_SPU")) {
			special_hierarchy_files = true;
			SpectralFileInsertResult referencing_spectrum_result, referenced_spectrum_result;

			// GER files contain target and reference radiances
			// thus here we must split them into two new hierarchies
			
			int target_sub_hierarchy_id = subhierarchies.get("Targets");
			int reference_sub_hierarchy_id = subhierarchies.get("References");

			// insert target spectrum
			referencing_spectrum_result = insertSpectrumAndHierarchyLink(spec_file, 0, spectrumExists_(spec_file, target_sub_hierarchy_id), stmt);
			addNonNullSpectrumIds(insert_result,referencing_spectrum_result);

			// insert reference spectrum
			referenced_spectrum_result = insertSpectrumAndHierarchyLink(spec_file, 1, spectrumExists_(spec_file, reference_sub_hierarchy_id), stmt);
			addNonNullSpectrumIds(insert_result, referenced_spectrum_result);

			// if spectra were inserted we need to add a datalink that links the
			// target to the reference spectrum
			if (referencing_spectrum_result.getSpectrumIds().get(0) != 0) {
				insertLink(new SpectrumDataLink(referencing_spectrum_result.getSpectrumIds().get(0), referenced_spectrum_result.getSpectrumIds().get(0), "Spectralon data"));
			}

		}

		if (spec_file.getCompany().equals("YES")) {
			special_hierarchy_files = true;

			int total_sub_hierarchy_id = subhierarchies.get("Total");
			int diffuse_sub_hierarchy_id = subhierarchies.get("Diffuse");

			// insert all spectra where the no of spectra is the count of total
			// plus diffuse
			for (int i = 0; i < spec_file.getNumberOfSpectra(); i += 2) {
				// insert total spectrum
				SpectralFileInsertResult tmp = insertSpectrumAndHierarchyLink(spec_file, i, spectrumExists_(spec_file, total_sub_hierarchy_id), stmt);
				addNonNullSpectrumIds(insert_result,tmp);

				// insert diffuse spectrum
				tmp = insertSpectrumAndHierarchyLink(spec_file, i + 1, spectrumExists_(spec_file, diffuse_sub_hierarchy_id), stmt);
				addNonNullSpectrumIds(insert_result,tmp);
			}

		}
		
		if (spec_file.getCompany().equals("JB Hyperspectral")) {
			special_hierarchy_files = true;

			int DN_hierarchy_id = subhierarchies.get("DN");
			
			hierarchy_existence_data exists_info = spectrumExists_(spec_file, DN_hierarchy_id);
						
			insert_result = insertSpectralFileAndHierarchyLink(spec_file, exists_info, stmt);
						
		}

		if (spec_file.getCompany().equals("FGI")) {
			special_hierarchy_files = true;

			// create new sub hierarchy for this data collection
			String[] tokens = spec_file.getBasename().split("\\.");
			int FGI_collection_id = subhierarchies.get(tokens[0]);

			// create sub hierarchies within the data collection for HDRF, BRF,
			// etc

			if (spec_file.isFgiHdrfBrfCombined() == false) {
				int FGI_sub_id = getSubHierarchyId(FGI_collection_id, tokens[1]);
				int FGI_I_id = 0;
				int FGI_Q_id = 0;
				int FGI_U_id = 0;

				if (spec_file.getSpectralGroupingSize() > 1) {
					FGI_I_id = subhierarchies.get("R");
					FGI_Q_id = subhierarchies.get("Mueller10");
					FGI_U_id = subhierarchies.get("Mueller20");
							
				}

				for (int i = 0; i < spec_file.getNumberOfSpectra(); i++) {
					if (spec_file.getSpectralGroupingSize() > 1) {
						addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, i++, spectrumExists_(spec_file, FGI_I_id), stmt));
						addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, i++, spectrumExists_(spec_file, FGI_Q_id), stmt));
						addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, i, spectrumExists_(spec_file, FGI_U_id), stmt));

					} else {
						addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, i, spectrumExists_(spec_file, FGI_sub_id), stmt));
					}
				}

			} else // hdrf_brf_combined == true
			{
				int FGI_BRF_sub_id = subhierarchies.get("BRF");
				int FGI_HDRF_sub_id = subhierarchies.get("HDRF");

				for (int i = 0; i < spec_file.getNumberOfSpectra(); i++) {

					if (spec_file.getFgiHdrfBrfFlag(i).equals("BRF")) {
						addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, i, spectrumExists_(spec_file, FGI_BRF_sub_id), stmt));
					}

					if (spec_file.getFgiHdrfBrfFlag(i).equals("HDRF")) {
						addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, i, spectrumExists_(spec_file, FGI_HDRF_sub_id), stmt));
					}

				}

			}

		}

		if (spec_file.getAsdV7() == true) {
			special_hierarchy_files = true;
			// insert dn spectrum if required by user or if DN are the only data in the input file
			if(spec_file.getCreate_DN_folder_for_asd_files() || ((spec_file.getAsdV7RadianceFlag() == false) && (spec_file.getAsdV7ReflectanceFlag() == false)))
				addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, 0, spectrumExists_(spec_file, subhierarchies.get("DN")), stmt));

			// insert radiance spectrum
			if (spec_file.getAsdV7RadianceFlag() == true) {
				addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, 1, spectrumExists_(spec_file, subhierarchies.get("Radiance")), stmt));
			}

			// insert reflectance spectrum
			if (spec_file.getAsdV7ReflectanceFlag() == true) {
				addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, 1, spectrumExists_(spec_file, subhierarchies.get("Reflectance")), stmt));
			}
			

		}
		
		if (spec_file.getCreateUnitFolderForasdOldFiles() == true) {
			special_hierarchy_files = true;
			
			
			if (spec_file.getMeasurementUnits(0).equals(MeasurementUnit.Reflectance)) {
				addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, 0, spectrumExists_(spec_file, subhierarchies.get("Reflectance")), stmt));
			}
			
			if (spec_file.getMeasurementUnits(0).equals(MeasurementUnit.Radiance)) {
				addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, 0, spectrumExists_(spec_file, subhierarchies.get("Radiance")), stmt));
			}
			
			if (spec_file.getMeasurementUnits(0).equals(MeasurementUnit.DN)) {
				addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, 0, spectrumExists_(spec_file, subhierarchies.get("DN")), stmt));
			}
			
			if (spec_file.getMeasurementUnits(0).equals(MeasurementUnit.Irradiance)) {
				addNonNullSpectrumIds(insert_result,insertSpectrumAndHierarchyLink(spec_file, 0, spectrumExists_(spec_file, subhierarchies.get("Irradiance")), stmt));
			}			
			
			
			

		}		
		

		if (!special_hierarchy_files) {
			
			hierarchy_existence_data exists_struct = new hierarchy_existence_data();
			
			if(spec_file.getNumberOfSpectra() > 1)
			{
				exists_struct = spectrumExists_(spec_file , hierarchy_id);
			}
			else
			{
				exists_struct.exist_array.add(false); // single file entries get always inserted, otherwise the client would not have sent it.
				exists_struct.hierarchy_id = hierarchy_id;
			}

//			for (int i = 0; i < spec_file.getNumberOfSpectra(); i++) {
//				if(exists_struct.exist_array.get(i) == false)
//					addNonNullSpectrumIds(insert_result,insertSpectrum(spec_file, i, exists_struct, stmt));				
//			}
			
			insert_result = insertSpectralFileAndHierarchyLink(spec_file, exists_struct, stmt);	

			// insert links to the hierarchies
//			insertHierarchySpectrumReferences(spec_file.getHierarchyId(), insert_result.getSpectrumIds());
		}
		
//		stmt.getConnection().commit();
		//
		
		stmt.execute("COMMIT");
//		stmt.getConnection().setAutoCommit(true);		
		
		stmt.close();
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				stmt.getConnection().rollback();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}		
		
		return insert_result;
	}
	
	
	 
	
	 public SpectralFileInsertResult insertSpectralFileAndHierarchyLink(SpectralFile spec_file, hierarchy_existence_data exists_info, Statement stmt) throws SPECCHIOFactoryException {

		 try {


			 SpectralFileInsertResult insert_result = new SpectralFileInsertResult();

			 ArrayList<SpectralFileInsertStruct> spec_file_insert_structs = new ArrayList<SpectralFileInsertStruct>();

			 
			 Instant start = Instant.now();

			 for (int i = 0; i < spec_file.getNumberOfSpectra(); i++) {

				 SpectralFileInsertStruct spec_file_insert_struct = getSpectrumInsertStruct(spec_file, i, exists_info);
				 spec_file_insert_structs.add(spec_file_insert_struct);
				 
				 if(spec_file_insert_struct.getErrors().size()>0) insert_result.addErrors(spec_file_insert_struct.getErrors());
				 if(spec_file_insert_struct.getAdded_new_instrument().size()>0) insert_result.addAdded_new_instruments(spec_file_insert_struct.getAdded_new_instrument());

			 }
			 
			 Instant end = Instant.now();
			 
			 SpecchioMessage info = new SpecchioMessage("getSpectrumInsertStructs: " + Duration.between(start, end).getSeconds(), SpecchioMessage.INFO);
			insert_result.addError(info );
			
			start = Instant.now();

			 reduce_metadata_redundancy_of_file(spec_file);

			 end = Instant.now();
			 info = new SpecchioMessage("reduce_metadata_redundancy_of_file: " + Duration.between(start, end).getSeconds(), SpecchioMessage.INFO);
			 insert_result.addError(info );

			 // insert process follows hereafter
			 //------------
			 // step 1: insert spectrum rows as single statement
			 //------------
			 
			 start = Instant.now();
			 
			 ArrayList<Integer> spectrum_ids = insert_spectrum_rows(spec_file_insert_structs, stmt);

			 insert_result.setSpectrumIds(spectrum_ids);

			 //------------
			 // step 2: insert unique metaparameters
			 //------------

			 Metadata md = new Metadata();
			 md.setEntries(spec_file.getUniqueMetaParameters());
			 ArrayList<Integer> eav_ids = getEavServices().insert_metadata_into_db(campaign.getId(), md, this.Is_admin(), stmt);
			 
			 
			 //------------
			 // step 3: insert links between spectra and eav entries
			 //------------
			 
			 getEavServices().insert_primary_x_eav(MetaParameter.SPECTRUM_LEVEL, spectrum_ids, spec_file.getRedundancy_reduced_metaparameter_index_per_spectrum(), eav_ids, stmt);

			 
			 //------------
			 // step 4: insert hierarchy links
			 //------------
			 
			 
			 insertHierarchySpectrumReferences(exists_info.hierarchy_id, spectrum_ids, 0, stmt);		
			 
			 
			 
			 
			 
			 end = Instant.now();
			 info = new SpecchioMessage("all SQL inserts: " + Duration.between(start, end).getSeconds(), SpecchioMessage.INFO);
			 insert_result.addError(info );
			 insert_result.addError(new SpecchioMessage("SQL stats: #spectra = " + spec_file.getNumberOfSpectra() + ", " + "# of MP = " +  md.getEntries().size()
			  , SpecchioMessage.INFO));
			 
			 

			 return insert_result;

		 } catch (SQLException ex) {
			 // TODO Auto-generated catch block
			 ex.printStackTrace();
			 try {
				stmt.getConnection().rollback();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 throw new SPECCHIOFactoryException(ex);
		 } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new SPECCHIOFactoryException(e);
		}
		
	 }
	
	
	private ArrayList<Integer> insert_spectrum_rows(ArrayList<SpectralFileInsertStruct> spec_file_insert_structs,
			Statement stmt) throws SQLException {
		 ArrayList<Integer> spectrum_ids = new ArrayList<Integer>();

		 StringBuffer query = new StringBuffer("INSERT INTO spectrum_view "
				 + "("
				 + " hierarchy_level_id, sensor_id, campaign_id, "
				 + "file_format_id, instrument_id, calibration_id, "
				 + "measurement_unit_id, measurement) "
				 + "VALUES ");


		 ListIterator<SpectralFileInsertStruct> li = spec_file_insert_structs.listIterator();

		 while(li.hasNext())
		 {
			 SpectralFileInsertStruct sfis = li.next();

			 query.append("(" + sfis.hierarchy_id_and_op.id);
			 query.append(",");
			 query.append(sfis.sensor_id);
			 query.append(",");
			 query.append(sfis.campaign_id_and_op.id);
			 query.append(",");
			 query.append(sfis.file_format_id);
			 query.append(",");
			 query.append(sfis.instrument_id);
			 query.append(",");
			 query.append(sfis.calibration_id);
			 query.append(",");			
			 query.append(sfis.measurement_unit_id);
			 query.append(",");			
			 query.append("x'" + sfis.measurement_as_hex + "')");
			 
			 if(li.hasNext()) query.append(",");

		 }

//			PreparedStatement ps = getStatementBuilder().prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
			//now update
		 
		 	stmt.executeUpdate(query.toString(), Statement.RETURN_GENERATED_KEYS);		

			ResultSet rs = stmt.getGeneratedKeys();
							
		    while (rs.next()) {
		    	spectrum_ids.add(rs.getInt(1));
		    }		
		 
		 rs.close();
		 
		return spectrum_ids;
	}


	public void reduce_metadata_redundancy_of_file(SpectralFile spec_file) {
		
		
		// go through all metaparameters of all spectra in file and create unique list of metaparameters
		// spectra in spectral file get an index into the unique list of metaparameters
		// these indices are later used to create spectrum_x_eav links
		
		ArrayList<Integer> redundancy_reduced_metaparameter_index = null;
		ArrayList<ArrayList<Integer>> redundancy_reduced_metaparameter_index_per_spectrum = new ArrayList<ArrayList<Integer>>();
		ArrayList<MetaParameter> unique_mps = new ArrayList<MetaParameter>();
		MetaParameter curr_mp = null;
		boolean matches = false;
		int mp_cnt = 0;
		int mp_index = 0;
		
		for (int i = 0; i < spec_file.getNumberOfSpectra(); i++) {
		
			redundancy_reduced_metaparameter_index = new ArrayList<Integer>();
			ListIterator<MetaParameter> li = spec_file.getEavMetadata(i).getEntries().listIterator();
			
			String check = new String();
			 mp_index = 0;

			while(li.hasNext())
			{
				MetaParameter mp =  li.next();
				mp_cnt++;
				
				// check if it is already contained in the list
				ListIterator<MetaParameter> u_li = unique_mps.listIterator();
				matches = false;
				int curr_u_li_ind = -1;
				
				while(u_li.hasNext() && matches == false)
				{
					curr_u_li_ind = u_li.nextIndex();
					curr_mp = u_li.next();
					
					// attribute must be matching
					if(mp.getAttributeId().equals(curr_mp.getAttributeId()))
					{
				
						boolean equalValues = (mp.getValue() == null && curr_mp.getValue() == null) || (mp.getValue() != null && mp.hasEqualValue(curr_mp)); 
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
				
				check = check + mp_index + " - " + curr_u_li_ind + ": " + mp.valueAsString();
				
				if(!matches)
				{
					unique_mps.add(mp);
					redundancy_reduced_metaparameter_index.add(unique_mps.size()-1);
				}	
				else
				{
				
					redundancy_reduced_metaparameter_index.add(curr_u_li_ind);
					check = check + " -> " + curr_mp.valueAsString();
				}
				
				check = check + "\n";
				
				mp_index++;
			
			}
			
			
			redundancy_reduced_metaparameter_index_per_spectrum.add(redundancy_reduced_metaparameter_index);
			
			
		
		}
		
		float redundancy_reduction = (mp_cnt - unique_mps.size() * 1.0f) / mp_cnt * 100;
		
		spec_file.setRedundancy_reduced_metaparameter_index_per_spectrum(redundancy_reduced_metaparameter_index_per_spectrum);
		spec_file.setUniqueMetaParameters(unique_mps);
		
		
		
	}


	/**
	 * Insert a spectrum into the database.
	 * 
	 * @param spec_file		the spectral file from which the spectrum will be drawn
	 * @param spec_no		the index of the spectrum in the file
	 * @param hierarchy_existence_data info on existence of the spectra of this file within the hierarchy
	 * 
	 * @return the identifier of the new spectrum
	 * 
	 * @throws SPECCHIOFactoryException	could not insert the spectrum
	 */
	public SpectralFileInsertResult insertSpectrum(SpectralFile spec_file, int spec_no, hierarchy_existence_data exists_struct, Statement stmt) throws SPECCHIOFactoryException {
		
		SpectralFileInsertResult insert_result = new SpectralFileInsertResult();
		
		// this is the final check if the spectrum does exist.
		// the first check on the client side will check for all subhierachies and cause a load if it is missing in one of the automatically create sub-hierarchies
		// therefore, for each hierarchy the check must be carried out again.
		// this second check is reduced to only spectral files where subhierarchies exist
		
		
		if(exists_struct.exist_array.get(spec_no) == false)
		{
			try {
				
				Integer id = 0;
				
				SpectralFileInsertStruct insert_struct = getSpectrumInsertStruct(spec_file, spec_no, exists_struct);
				ArrayList<SpectralFileInsertStruct> spec_file_insert_structs = new ArrayList<SpectralFileInsertStruct>();
				spec_file_insert_structs.add(insert_struct);

				
				ArrayList<Integer> spectrum_ids = insert_spectrum_rows(spec_file_insert_structs, stmt);
				
				id = spectrum_ids.get(0);	
		
				// automatic processing of EAV data
				if (spec_file.getEavMetadata(spec_no) != null) {
					ArrayList<Integer> eav_ids = getEavServices().insert_metadata_into_db(campaign.getId(), spec_file.getEavMetadata(spec_no), this.Is_admin(), stmt);
					getEavServices().insert_primary_x_eav(MetaParameter.SPECTRUM_LEVEL, id, eav_ids);
				}
		
				// update of the attribute default storage field information if needed
				// removed this call, as administrators should always define the default storage field (no more auto-generation of attributes for SPECCHIO instances)
				//getAttributes().define_default_storage_fields();
				
				insert_result.addSpectrumId(id);
		
				return insert_result;
				
			}
			catch (IOException ex) {
				// database error
				throw new SPECCHIOFactoryException(ex);
			}
			catch (SQLException ex) {
				// database error
				try {
					stmt.getConnection().rollback();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				throw new SPECCHIOFactoryException(ex);
			}

		}
		else
		{
			insert_result.addSpectrumId(0);
			return insert_result;
		}

	}
	
	
	
	/**
	 * Insert a spectrum into the database.
	 * 
	 * @param spec_file		the spectral file from which the spectrum will be drawn
	 * @param spec_no		the index of the spectrum in the file
	 * @param insert_result 
	 * @param hierarchy_existence_data info on existence of the spectra of this file within the hierarchy
	 * 
	 * @return the identifier of the new spectrum
	 * 
	 * @throws SPECCHIOFactoryException	could not insert the spectrum
	 */
	public SpectralFileInsertStruct getSpectrumInsertStruct(SpectralFile spec_file, int spec_no, hierarchy_existence_data exists_struct) throws SPECCHIOFactoryException {
		
		SpectralFileInsertStruct insert_struct = new SpectralFileInsertStruct();
		
		// this is the final check if the spectrum does exist.
		// the first check on the client side will check for all subhierachies and cause a load if it is missing in one of the automatically create sub-hierarchies
		// therefore, for each hierarchy the check must be carried out again.
		// this second check is reduced to only spectral files where subhierarchies exist
		
		
		if(exists_struct.exist_array.get(spec_no) == false)
		{
			try {
				
				SQL_StatementBuilder SQL = new SQL_StatementBuilder(getConnection());
		
				insert_struct.campaign_id_and_op = SQL.is_null_key_get_val_and_op(campaign.getId());
				insert_struct.hierarchy_id_and_op = SQL.is_null_key_get_val_and_op(exists_struct.hierarchy_id);
				
				Metadata md = spec_file.getEavMetadata(spec_no);
				
		
				// if there is a position available for the spectrum create new position
				// record
				if (spec_file.getPos().size() > spec_no && spec_file.getPos(spec_no) != null) {
		
					MetaParameter mp;
					
					if(getEavServices().isSpatially_enabled())
					{
						mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Spatial Position", "Location"));
						ArrayList<Point2D> value = new ArrayList<Point2D>();
						Point2D coord = new Point2D(spec_file.getPos(spec_no).latitude, spec_file.getPos(spec_no).longitude);
						value.add(coord);						
						((MetaSpatialGeometry) mp).setValue(value);						
						md.addEntry(mp);	
					}
					else
					{
						mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Longitude", "Location"));
						mp.setValue(spec_file.getPos(spec_no).longitude, "Degrees");
						md.addEntry(mp);			
						
						mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Latitude", "Location"));
						mp.setValue(spec_file.getPos(spec_no).latitude, "Degrees");
						md.addEntry(mp);						
					}
					
	
					
					if(spec_file.getPos(spec_no).altitude != null)
					{
						mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Altitude", "Location"));
						mp.setValue(spec_file.getPos(spec_no).altitude, "Degrees");
						md.addEntry(mp);	
					}
					
					if (!spec_file.getPos(spec_no).location_name.equals(""))
					{
						mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Location Name", "Location"));
						mp.setValue(spec_file.getPos(spec_no).location_name, "String");
						md.addEntry(mp);	
					}
		
				}
		
				// illum
//				if (spec_file.getLightSource() != null) {
//					
//					int attr_id = getAttributes().get_attribute_id("Illumination Sources");
//					
//					// process all spectra
//					query = "select t.taxonomy_id from taxonomy t  where " + spec_file.getLightSource() +  " = t.name and t.attribute_id = " + attr_id;
//
//					rs = stmt.executeQuery(query);
//
//					while (rs.next()) {
//
//						int taxonomy_id = rs.getInt(1);
//						
//						MetaParameter mp = MetaParameter.newInstance(getAttributes().get_attribute_info(attr_id));
//						mp.setValue(taxonomy_id);
//						md.addEntry(mp);	
//					}	
//		
//					rs.close();
//		
//				}
//		
//				// sampling environment
//				if (spec_file.getSamplingEnvironment() != null) {								
//					
//					int attr_id = getAttributes().get_attribute_id("Sampling Environment");
//					
//					// process all spectra
//					query = "select t.taxonomy_id from taxonomy t  where " + spec_file.getSamplingEnvironment() +  " = t.name and t.attribute_id = " + attr_id;
//
//					rs = stmt.executeQuery(query);
//
//					while (rs.next()) {
//
//						int taxonomy_id = rs.getInt(1);
//						
//						MetaParameter mp = MetaParameter.newInstance(getAttributes().get_attribute_info(attr_id));
//						mp.setValue(taxonomy_id);
//						md.addEntry(mp);	
//					}	
//		
//					rs.close();					
//					
//				}				
				
				// geometry via EAV: SPECCHIO V3.0
				if (spec_file.getIlluminationAzimuths().size() > 0)
				{
					MetaParameter mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Illumination Azimuth", "Sampling Geometry"));
					mp.setValue(spec_file.getIlluminationAzimuth(spec_no), "Degrees");
					md.addEntry(mp);										
				}
				
				if (spec_file.getIlluminationZeniths().size() > 0)
				{
					MetaParameter mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Illumination Zenith", "Sampling Geometry"));
					mp.setValue(spec_file.getIlluminationZenith(spec_no), "Degrees");
					md.addEntry(mp);										
				}		
				
				if (spec_file.getSensorAzimuths().size() > 0)
				{
					MetaParameter mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Sensor Azimuth", "Sampling Geometry"));
					mp.setValue(spec_file.getSensorAzimuth(spec_no), "Degrees");
					md.addEntry(mp);										
				}	
				
				if (spec_file.getSensorZeniths().size() > 0)
				{
					MetaParameter mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Sensor Zenith", "Sampling Geometry"));
					mp.setValue(spec_file.getSensorZenith(spec_no), "Degrees");
					md.addEntry(mp);										
				}		
				
				if (spec_file.getArmLengths() != null && spec_file.getArmLength(spec_no) != null) {
					MetaParameter mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Sensor Distance", "Sampling Geometry"));
					mp.setValue(spec_file.getArmLength(spec_no), "m");
					md.addEntry(mp);											
				}
				
				SpecchioMessage msg = new SpecchioMessage();
			
				// file format: unknown file formats are automatically added.
				int file_format_id = getIdForFileFormat(spec_file.getFileFormatName());
				
				// check if this is an unknown file format in this database
				// handling of calls from e.g. Matlab where these values may not be set ...
				if(file_format_id == -1 && 	spec_file.getFileFormatName() != null)
				{
					// add new file format to DB
					SpectralFileFactory sff = new SpectralFileFactory(getSourceName()); // connects as admin
					file_format_id = sff.addFileFormat(spec_file);
				}
				
				//System.out.println("Get sensor via cache");
				int sensor_id = getDataCache().get_sensor_id_for_file(spec_file, spec_no, this.getDatabaseUserName(), msg);
				//System.out.println("Got sensor_id via cache: " + sensor_id);
				if (msg.getMessage() != null)
				{
					insert_struct.addError(msg);
				}				
				
				// this should not happen, as we auto-insert sensor ...
				if (sensor_id == 0)
				{
					insert_struct.addError(new SpecchioMessage("No matching sensor found.", SpecchioMessage.WARNING));
				}
				
				
				msg = new SpecchioMessage();
				//System.out.println("Get instrument via cache");
				Instrument instrument = getDataCache().get_instrument_id_for_file(spec_file, spec_no, msg);
				
				//System.out.println("Got instrument via cache: " + instrument.toString());
				
				String instrument_id = "null";
				int calibration_id = 0;
				
				if(instrument != null)
				{
					instrument_id = Integer.toString(instrument.getInstrumentId());			
					calibration_id = instrument.getCalibrationId();
					insert_struct.addAdded_new_instrument(instrument.isNewly_inserted());
				}
				
				if (msg.getMessage() != null)
				{
					insert_struct.addError(msg);
				}
											
				// fill spectrum_view values
				insert_struct.sensor_id = SQL.is_null_key_get_val_and_op(sensor_id).id;
				insert_struct.file_format_id = (file_format_id == -1 ? "null" : Integer.toString(file_format_id));
				insert_struct.instrument_id = instrument_id;
				insert_struct.calibration_id = SQL.is_null_key_get_val_and_op(calibration_id).id;
				insert_struct.measurement_unit_id = SQL.is_null_key_get_val_and_op(getDataCache().get_measurement_unit_id_for_file(spec_file, spec_no)).id;
				
				
				String hex = spec_file.measurementsToHex(spec_no);

				insert_struct.measurement_as_hex = hex;
								
				// filename
				MetaParameter mp = MetaParameter.newInstance(getAttributes().get_attribute_info("File Name", "General"));
				mp.setValue(spec_file.getSpectrumFilename(spec_no), "String");
				md.addEntry(mp);
				
				// capture and insert times
				DateTime capture_date = spec_file.getCaptureDate(spec_no);
				if (capture_date != null) {
					MetaDate mpd = (MetaDate)MetaParameter.newInstance(getAttributes().get_attribute_info("Acquisition Time", "General"));
					mpd.setValue(capture_date);
					md.addEntry(mpd);
				}
				
				// UTC insert time
				DateTime now = spec_file.getLoading_time();
				
				MetaDate mpd = (MetaDate)MetaParameter.newInstance(getAttributes().get_attribute_info("Loading Time", "General"));	
				mpd.setValue(now);
				md.addEntry(mpd);				
				
				// add instrument number as EAV if instrument is not defined in DB but not empty
				if(instrument_id.equals("null") &&
						spec_file.getInstrumentNumber() != null &&
						!spec_file.getInstrumentNumber().equals(""))			
				{			
					mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Instrument Serial Number", "Instrument"));
					mp.setValue(spec_file.getInstrumentNumber());
					md.addEntry(mp);				
				}
				
				// cloud cover (convert from oktas)
				if (spec_file.getWeather() != null && spec_file.getWeather().length() > 0) {
					String oktas = spec_file.getWeather().substring(0, 1);
					
					int okta_as_int = Integer.parseInt(oktas);			
					double cloud_cover =  okta_as_int / 8.0 * 100;
					
					mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Cloud Cover", "Environmental Conditions"));
					mp.setValue(cloud_cover, "%");
					md.addEntry(mp);				
					
		
				}
				
				// add foreoptic to EAV if defined
				if(spec_file.getForeopticDegrees() != 0)
				{
					mp = MetaParameter.newInstance(getAttributes().get_attribute_info("FOV", "Optics"));
					mp.setValue(spec_file.getForeopticDegrees(), "Degrees");
					md.addEntry(mp);								
				}
				
				if(spec_file.getCalibrationSeries() >= 0)
				{
					mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Calibration Number", "Instrument"));
					mp.setValue(spec_file.getCalibrationSeries());
					md.addEntry(mp);								
				}				
				
				if(spec_file.getComment() != null)
				{
					mp = MetaParameter.newInstance(getAttributes().get_attribute_info("File Comments", "General"));
					mp.setValue(spec_file.escape_string(spec_file.getComment()), "String");
					md.addEntry(mp);								
				}		
		
				// update the EAV with possible garbage flags
				if(spec_file.getGarbageIndicator() == true)
				{
					mp = MetaParameter.newInstance(getAttributes().get_attribute_info("Garbage Flag", "General"));
					mp.setValue(1, "Raw");
					md.addEntry(mp);
				}		
				
				
				// spectra names
				 if (spec_file.getSpectraNames().size() > 0) {			 
					 mp = MetaParameter.newInstance(getAttributes().get_attribute_info(spec_file.getSpectrumNameType(), "Names"));
					 mp.setValue(spec_file.getSpectrumName(spec_no), "String");
					 md.addEntry(mp);			 
				 }
				
				// processing of EAV data: get value strings and redundancy reduced eav_ids, stored in md object
				 getEavServices().getMetadataInsertData(campaign.getId(), md);
				 
		
				return insert_struct;
				
			}
			catch (SQLException ex) {
				// database error
				throw new SPECCHIOFactoryException(ex);
			}
			catch (MetaParameterFormatException ex) {
				// a metaparameter object has the wrong type
				throw new SPECCHIOFactoryException(ex);
			}
		}
		else
		{
			return insert_struct;
		}

	}	
	
	
	private int addFileFormat(SpectralFile spec_file) {
		
		int file_format_id = -1;
		String query;
		ResultSet rs;
		SQL_StatementBuilder SQL = new SQL_StatementBuilder(getConnection());		
		
		query = "insert into file_format (name, file_extension) values (" + SQL.quote_string(spec_file.getFileFormatName()) + ", " + SQL.quote_string(spec_file.getExt()) + ")";
		
		try {
			Statement stmt = SQL.createStatement();
			
			stmt.executeUpdate(query);
			
			// get the identifier of the new instrument
			query = "select last_insert_id()";
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				file_format_id = rs.getInt(1);
			}
			rs.close();					
			stmt.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		getDataCache().add_file_format_id(spec_file.getFileFormatName(), file_format_id);
		
		return file_format_id;
		
	}


	/**
	 * Helper method for insertSpectra(). Insert both a spectrum and a hierarchy link.
	 * 
	 * @param spec_file		the spectral file to be inserted
	 * @param spec_no		the index of the spectrum to be inserted
	 * @param hierachy_and_exists_struct	contains the identifier of the hierarchy node into which to insert and booleans indicating if this spectrum already exists or not (used to ensure that all calls first do a spectrum exists check, preferably as a multiple file call to save time)	
	 * 
	 * @return insert result descriptor of the new spectrum
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	private SpectralFileInsertResult insertSpectrumAndHierarchyLink(SpectralFile spec_file,
			int spec_no, hierarchy_existence_data exists_struct, Statement stmt)
			throws SPECCHIOFactoryException {

			SpectralFileInsertResult insert_result = insertSpectrum(spec_file, spec_no, exists_struct, stmt); 

			if (insert_result.getSpectrumIds().get(0) > 0) // always only one spectrum id is returned in the list
			{
				insertHierarchySpectrumReferences(exists_struct.hierarchy_id, insert_result.getSpectrumIds().get(0),
						0, stmt);			
			}
			return insert_result;

		
	}
		
	
	/**
	 * Helper method for insertSpectra(). Insert both a spectrum and a hierarchy link.
	 * 
	 * @param spec_file		the spectral file to be inserted
	 * @param spec_no		the index of the spectrum to be inserted
	 * @param hierarchy_id	the identifier of the hierarchy node into which to insert
	 * @param recusion_break_at_parent_id   id of a hierarchy where to break the recursive insert (deprecated)
	 * 
	 * @return insert result descriptor of the new spectrum
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	private SpectralFileInsertResult insertSpectrumAndHierarchyLink(SpectralFile spec_file,
			int spec_no, hierarchy_existence_data exists_struct, int recursion_break_at_hierarchy_id, Statement stmt)
			throws SPECCHIOFactoryException {
		

		SpectralFileInsertResult insert_result = insertSpectrum(spec_file, spec_no, exists_struct, stmt); // always a spectrum that prompted auto-subhierarchy creation

		if (insert_result.getSpectrumIds().get(0) > 0) // always only one spectrum id is returned in the list
		{
			insertHierarchySpectrumReferences(exists_struct.hierarchy_id, insert_result.getSpectrumIds().get(0),
					recursion_break_at_hierarchy_id, stmt);			
		}

		return insert_result;
	
	}
	
	
	/**
	 * Test for the existence of a given spectral file in the database.
	 * 
	 * @param descriptor	the descriptor of the file to be tested for
	 * 
	 * @return true if the file exists in the database, and false otherwise
	 */
	public boolean spectrumExists(SpectralFile spec_file, int hierarchy_id) throws SPECCHIOFactoryException {
		
		// get sub hierarchy ids for this spectral file
		Hashtable<String, Integer> sub_ids = this.getSubHierarchyIds(spec_file, hierarchy_id);
		
		boolean exists = true;
		
		if(sub_ids.size() > 0)
		{		
			// check if the spectrum exists in all the sub hierarchies
			Enumeration<Integer> li = sub_ids.elements();
			
			
			
			while(li.hasMoreElements())
			{
				exists = exists & spectrumExists(spec_file.getSpectrumFilename(0), li.nextElement());
			}			
		}
		else
		{
			exists = spectrumExists(spec_file.getSpectrumFilename(0), hierarchy_id);			
		}
		
		return exists;
	}
	
	
	/**
	 * Test for the existence of given spectral files in the database.
	 * 
	 * @param descriptor	list of spectral files
	 * 
	 * @return list of existence per file encoded as 0/1, order of the files in the input list is equal to boolean indicator order
	 */
	public ArrayList<Boolean> spectraExist(ArrayList<SpectralFile> spectral_file_list, int hierarchy_id) throws SPECCHIOFactoryException {
		
		ArrayList<hierarchy_existence_data> exists_struct_array = new ArrayList<hierarchy_existence_data>();
		hierarchy_existence_data exists_struct = new hierarchy_existence_data();
		hierarchy_existence_data final_exists_struct = new hierarchy_existence_data();
		
//		for (int i=0;i<spectral_file_list.size();i++)
//		{
//			exists_struct.combo_exist_array.add(1); // all files exist by default
//		}
		
		int no_of_handled_files = 0;
		
		while(no_of_handled_files < spectral_file_list.size())
		{
				
			// get sub hierarchy ids of current spectral file
			Hashtable<String, Integer> sub_ids = this.getSubHierarchyIds(spectral_file_list.get(no_of_handled_files), hierarchy_id);
			
			ArrayList<SpectralFile> same_file_type_list = new ArrayList<SpectralFile>();
			same_file_type_list.add(spectral_file_list.get(no_of_handled_files)); // first spectrum of a filetype is always part of the list
			
			// build file list for same file type, also need to check on the ASD V7 flag because the two ASD formats currently share the same file format name
			no_of_handled_files++;
			while(no_of_handled_files < spectral_file_list.size() && same_file_type_list.get(0).getFileFormatName().equals(spectral_file_list.get(no_of_handled_files).getFileFormatName()) && same_file_type_list.get(0).getAsdV7() == spectral_file_list.get(no_of_handled_files).getAsdV7())
			{
				same_file_type_list.add(spectral_file_list.get(no_of_handled_files));
				no_of_handled_files++;
			}
			
			
			
			if(sub_ids.size() > 0)
			{		
				// check if the spectra exist in all the sub hierarchies
				Enumeration<Integer> li = sub_ids.elements();
				
				exists_struct = new hierarchy_existence_data();
				
				for (int i=0;i<same_file_type_list.size();i++)
				{
					exists_struct.combo_exist_array.add(true); // all files exist by default
				}				
				
				
				while(li.hasMoreElements())
				{
					hierarchy_existence_data exists_struct_tmp = spectraExist_(same_file_type_list, li.nextElement());
					
					for (int i=0;i<same_file_type_list.size();i++)
					{
						exists_struct.combo_exist_array.set(i, exists_struct.combo_exist_array.get(i) & exists_struct_tmp.combo_exist_array.get(i)); // AND operation on existence in subhierarchies
						//exists_combo = exists_combo & exists_struct_tmp.combo_exist_array.get(i); // AND operation on existence in subhierarchies
					}
	
				}	


			}
			else
			{
				exists_struct = spectraExist_(same_file_type_list, hierarchy_id);			
			}
			
			exists_struct_array.add(exists_struct);
			
		}
		
		// compile final boolean array
		for(int i=0;i<exists_struct_array.size();i++)
		{
			final_exists_struct.combo_exist_array.addAll(exists_struct_array.get(i).combo_exist_array);
		}
		
		
		
		return final_exists_struct.combo_exist_array;
	}	
	
	
	/**
	 * Test for the existence of a filename within a sub-hierarchy.
	 * 
	 * @oaram filename		the filename
	 * @param hierarchy_id	the identifier of the hierarchy to check
	 * 
	 * @return true if a spectrum with the specified filename exists within the identified hierarchy
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private boolean spectrumExists(String filename, int hierarchy_id) throws SPECCHIOFactoryException {
		
		try {
			// build a query that will return "1" if the file exists
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			id_and_op_struct p_id_and_op = new id_and_op_struct(hierarchy_id);
			String query = "select count(spectrum.spectrum_id) from spectrum, spectrum_x_eav, eav, hierarchy_level_x_spectrum" +
					" where spectrum.spectrum_id=spectrum_x_eav.spectrum_id" +
					" and spectrum_x_eav.eav_id=eav.eav_id" +
					" and eav.attribute_id=" + getAttributes().get_attribute_id("File Name") +
					" and eav.string_val=" + SQL.quote_string(filename) +
					" and hierarchy_level_x_spectrum.spectrum_id=spectrum.spectrum_id" +
					" and hierarchy_level_x_spectrum.hierarchy_level_id " + p_id_and_op.op + " " + p_id_and_op.id;

			// execute the statement
			ResultSet rs = stmt.executeQuery(query);
			int exists = 0;
			while (rs.next()) {
				exists = rs.getInt(1);
			}
			rs.close();
			stmt.close();
			
			return exists > 0;
			
		} catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		                          
		
	}	
	
	/**
	 * Test for the existence of a filename within a sub-hierarchy.
	 * 
	 * @oaram filename		the filename
	 * @param hierarchy_id	the identifier of the hierarchy to check
	 * 
	 * @return hierarchy_existence_data
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private hierarchy_existence_data spectrumExists_(SpectralFile spec_file, int hierarchy_id) throws SPECCHIOFactoryException {
		
		ArrayList<SpectralFile> spectral_file_list = new ArrayList<SpectralFile>();
		spectral_file_list.add(spec_file);
		hierarchy_existence_data exists_struct = spectraExist_(spectral_file_list , hierarchy_id);
		
		return exists_struct;
		
	}
	
	
	
	/**
	 * Test for the existence of a filename within a sub-hierarchy.
	 * 
	 * @oaram filename		the filename
	 * @param hierarchy_id	the identifier of the hierarchy to check
	 * 
	 * @return hierarchy_existence_data
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private hierarchy_existence_data spectraExist_(ArrayList<SpectralFile> spectral_file_list, int hierarchy_id) throws SPECCHIOFactoryException {
		
//		ArrayList<Integer> exist_array = new ArrayList<Integer>();
//		ArrayList<Integer> combo_exist_array = new ArrayList<Integer>(); // final output with combined existence info for all entries within the spectral files
		
		hierarchy_existence_data ed = new hierarchy_existence_data();
		ed.hierarchy_id = hierarchy_id;
		
		ArrayList<String> filenames = new ArrayList<String>();
		ArrayList<Integer> filenames_per_spectral_file = new ArrayList<Integer>();
		
		ListIterator<SpectralFile> sf_li = spectral_file_list.listIterator();
		
//		String filenames_for_insert ="";
		
		boolean first = true;
		
		while(sf_li.hasNext()) {
			SpectralFile spec_file = sf_li.next();		
			
			// deal with spectral files that have multiple spectral file names, like XLS where filenames need to be autogenerated
			//String all_spectral_file_filenames = this.getEavServices().SQL.conc_values(spec_file.getSpectraFilenames());
			
			filenames.addAll(spec_file.getSpectraFilenames());
			
			filenames_per_spectral_file.add(spec_file.getSpectraFilenames().size());
			
//			if (!first)
//				filenames_for_insert = filenames_for_insert + ", ";
//			
//			filenames_for_insert = filenames_for_insert +  this.getEavServices().SQL.conc_values(spec_file.getSpectraFilenames());
//			
//			first = false; 
		}
		
		
		try {
			// build a query that will return "1" if the file exists
			
			
			
			// build temp table

			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			id_and_op_struct p_id_and_op = new id_and_op_struct(hierarchy_id);
			
			String quoted_filenames = SQL.conc_values_for_multiple_insert(filenames);
			
			String ddl_string = "CREATE TEMPORARY TABLE IF NOT EXISTS " +
					SQL.prefix(getTempDatabaseName(), "spectra_existence_check") +
					"(spectrum_id INT, filename varchar(200) not null)";
			stmt.executeUpdate(ddl_string);		
			
			ddl_string = "CREATE TEMPORARY TABLE IF NOT EXISTS " +
					SQL.prefix(getTempDatabaseName(), "spectra_existence_in_db") +
					"(spectrum_id INT, filename varchar(200) not null)";
			stmt.executeUpdate(ddl_string);					
			
			// clear temporary tables (in case they already existed)
			String delete_string = "delete from " + SQL.prefix(getTempDatabaseName(), "spectra_existence_check");
			stmt.executeUpdate(delete_string);
			
			delete_string = "delete from " + SQL.prefix(getTempDatabaseName(), "spectra_existence_in_db");
			stmt.executeUpdate(delete_string);			
			
			
			
			String insert_string = "insert into " + SQL.prefix(getTempDatabaseName(), "spectra_existence_check (filename) values ") + quoted_filenames;			
			
			stmt.executeUpdate(insert_string);
			
//			insert_string = "insert into " + SQL.prefix(getTempDatabaseName(), "spectra_existence_in_db") + "(spectrum_id, filename) (select spectrum.spectrum_id, eav.string_val from spectrum, spectrum_x_eav, eav, hierarchy_level_x_spectrum "  +
//					" where spectrum.spectrum_id=spectrum_x_eav.spectrum_id" +
//					" and spectrum_x_eav.eav_id=eav.eav_id" +
//					" and eav.attribute_id=" + getAttributes().get_attribute_id("File Name") +
//					" and eav.string_val in (" + quoted_filenames + ") " +
//					" and hierarchy_level_x_spectrum.spectrum_id=spectrum.spectrum_id" +
//					" and hierarchy_level_x_spectrum.hierarchy_level_id " + p_id_and_op.op + " " + p_id_and_op.id + ")";
			
			insert_string = "insert into " + SQL.prefix(getTempDatabaseName(), "spectra_existence_in_db") + "(spectrum_id, filename) (select spectrum.spectrum_id, eav.string_val from spectrum, spectrum_x_eav, eav "  +
					" where spectrum.spectrum_id=spectrum_x_eav.spectrum_id" +
					" and spectrum_x_eav.eav_id=eav.eav_id" +
					" and eav.attribute_id=" + getAttributes().get_attribute_id("File Name") +
					" and eav.string_val in (" + quoted_filenames + ") " +
					" and spectrum.hierarchy_level_id " + p_id_and_op.op + " " + p_id_and_op.id + ")";
			
			
			
			stmt.executeUpdate(insert_string);
			
			String update_string = "update " + SQL.prefix(getTempDatabaseName(), "spectra_existence_check sec") + " join "+ SQL.prefix(getTempDatabaseName(), "spectra_existence_in_db seb") +
					" on sec.filename = seb.filename set sec.spectrum_id = seb.spectrum_id"; 
			
			stmt.executeUpdate(update_string);
			
			String query = "select spectrum_id from "+ SQL.prefix(getTempDatabaseName(), "spectra_existence_check sec");

			// execute the statement
			ResultSet rs = stmt.executeQuery(query);

			while (rs.next()) {
				int id = rs.getInt(1);
				
				if(id > 0)
					ed.exist_array.add(true);
				else
					ed.exist_array.add(false);
					
				
			}
			rs.close();
			stmt.close();
			
			// compile the multiple entries per spectral file into one combined indicator per spectral file			
			
			ListIterator<Integer> li = filenames_per_spectral_file.listIterator();
			int index = 0;
			
			
			while(li.hasNext())
			{
				int no_of_entries = li.next();
				Boolean combined_exist = ed.exist_array.get(index); // initial value of first entry of the current spectral file
				if(no_of_entries > 1)
				{
					for(int i=0;i<no_of_entries-1;i++) // first entry is already handled, therefore reduce the loop size by one
					{
						index++;
						combined_exist = combined_exist & ed.exist_array.get(index);
					}
				}
				else
				{
					index++;
				}
				ed.combo_exist_array.add(combined_exist);				
			}
			
			return ed;
			
		} catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		                          
		
	}		
	
	// array list contain true if a spectrum with the specified filename exists within the identified hierarchy
	class hierarchy_existence_data {	
		
		ArrayList<Boolean> exist_array = new ArrayList<Boolean>();
		ArrayList<Boolean> combo_exist_array = new ArrayList<Boolean>(); // final output with combined existence info for all entries within the spectral files
		int hierarchy_id; // the hierarchy for which the check was carried out
	}
	

}
