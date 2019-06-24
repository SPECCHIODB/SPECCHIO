package ch.specchio.client;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import au.ands.org.researchdata.RDACollectionDescriptor;
import ch.specchio.interfaces.ProgressReportInterface;
import ch.specchio.jaxb.XmlInteger;
import ch.specchio.plots.GonioSamplingPoints;
import ch.specchio.queries.EAVQueryConditionObject;
import ch.specchio.queries.Query;
import ch.specchio.spaces.MeasurementUnit;
import ch.specchio.spaces.ReferenceSpaceStruct;
import ch.specchio.spaces.Space;
import ch.specchio.spaces.SpectralSpace;
import ch.specchio.types.AVMatchingListCollection;
import ch.specchio.types.ApplicationDomainCategories;
import ch.specchio.types.Calibration;
import ch.specchio.types.CalibrationMetadata;
import ch.specchio.types.Campaign;
import ch.specchio.types.Category;
import ch.specchio.types.CategoryTable;
import ch.specchio.types.ConflictTable;
import ch.specchio.types.Country;
import ch.specchio.types.Hierarchy;
import ch.specchio.types.Institute;
import ch.specchio.types.Instrument;
import ch.specchio.types.InstrumentDescriptor;
import ch.specchio.types.MatlabAdaptedArrayList;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetadataSelectionDescriptor;
import ch.specchio.types.MetadataUpdateDescriptor;
import ch.specchio.types.MetaparameterStatistics;
import ch.specchio.types.Picture;
import ch.specchio.types.PictureTable;
import ch.specchio.types.Reference;
import ch.specchio.types.ReferenceBrand;
import ch.specchio.types.ReferenceDescriptor;
import ch.specchio.types.Sensor;
import ch.specchio.types.SpectralFile;
import ch.specchio.types.SpectralFileInsertResult;
import ch.specchio.types.SpectralFiles;
import ch.specchio.types.Spectrum;
import ch.specchio.types.SpectrumDataLink;
import ch.specchio.types.SpectrumFactorTable;
import ch.specchio.types.Taxonomy;
import ch.specchio.types.TaxonomyNodeObject;
import ch.specchio.types.Units;
import ch.specchio.types.User;
import ch.specchio.types.attribute;
import ch.specchio.types.campaign_node;
import ch.specchio.types.database_node;
import ch.specchio.types.spectral_node_object;


/**
 * Client object implementing a cache for another client object.
 */
public class SPECCHIOClientCache implements SPECCHIOClient {
	
	/** the client for which to act as a cache */
	private SPECCHIOClient realClient;
	
	/** attributes cache */
	private attribute[] attributes;
	
	/** cache of attributes by category */
	private Hashtable<String, attribute[]> attributesByCategory;
	
	/** index of attributes by id */
	private Hashtable<Integer, attribute> attributesById;
	
	/** index of attributes by name */
	private Hashtable<String, attribute> attributesByName;
	
	/** metadata categories */
	private Hashtable<String, Hashtable<String, Integer>> metadataCategoriesForNameAccess;
	private Hashtable<String, CategoryTable> metadataCategoriesForIdAccess;
	
	/** taxonomies */
	private Hashtable<Integer, Hashtable<String, Integer>> taxonomiesHash; // key is set to attribute id
	private Hashtable<Integer, Integer[]> applicationDomainCategories;
	
	/** the progress report with which to indicate progress */
	private ProgressReportInterface pr;
	
	/** the server descriptor of the current connection */
	private SPECCHIOServerDescriptor serverDescriptor;	
	
	/**
	 * Constructor.
	 * 
	 * @param client	the client object to be cached
	 */
	public SPECCHIOClientCache(SPECCHIOClient client) {
		
		/** save a reference to the real client */
		realClient = client;
		
		/** initialise member variables */
		pr = null;
		
		// initialise caches
		attributes = null;
		attributesByCategory = new Hashtable<String, attribute[]>();
		attributesById = null;
		attributesByName = null;
		metadataCategoriesForIdAccess = new Hashtable<String, CategoryTable>();
		metadataCategoriesForNameAccess = new Hashtable<String, Hashtable<String, Integer>>();
		taxonomiesHash = new Hashtable<Integer, Hashtable<String, Integer>>();
	}
	
	
	/**
	 * Connect to the server.
	 * 
	 * @throws SPECCHIOClientException could not log in
	 */
	public void connect() throws SPECCHIOClientException, SPECCHIOWebClientException {
		
		// make connection
		if (pr != null) {
			pr.set_progress(0);
		}
		realClient.connect();
		
		if (realClient.getLoggedInUser() != null) {
		
			// pre-populate attribute cache
			if (pr != null) {
				pr.set_progress(50);
				pr.set_operation("Downloading attributes");
			}
			attributes = realClient.getAttributes();
			attributesById = new Hashtable<Integer, attribute>();
			attributesByName = new Hashtable<String, attribute>();
			Set<String> categories = new HashSet<String>();
			for (attribute attr : attributes) {
				attributesById.put(attr.id, attr);
				attributesByName.put(attr.name, attr);
				if (attr.cat_name != null && attr.cat_name.length() > 0) {
					categories.add(attr.cat_name);
				}
			}
			applicationDomainCategories = null; // ensure that they re-cached when required
			if (pr != null) {
				pr.set_progress(75);
			}
			for (String category : categories) {
				attributesByCategory.put(category, realClient.getAttributesForCategory(category));
			}
		}
		
		// finished
		if (pr != null) {
			pr.set_progress(100);
			pr.set_operation("Done");
		}
		
	}
	
	
	/**
	 * Copy a spectrum to a specified hierarchy.
	 * 
	 * @param spectrum_id		the spectrum_id of the spectrum to copy
	 * @param target_hierarchy_id	the hierarchy_id where the copy is to be stored
	 * 
	 * @return new spectrum id
	 * 
	 * @throws SPECCHIOClientException could not log in
	 */
	public int copySpectrum(int spectrum_id, int target_hierarchy_id) throws SPECCHIOClientException {
		
		return realClient.copySpectrum(spectrum_id, target_hierarchy_id);
		
	}
	
	/**
	 * Copy a hierarchy to a specified hierarchy with a new name.
	 * 
	 * @param hierarchy_id		the hierarchy_id of the hierarchy to copy
	 * @param target_hierarchy_id	the hierarchy_id where the copy is to be stored
	 * @param new_name			new name for the copied hierarchy
	 * 
	 * @return new hierarchy_id
	 * 
	 * @throws SPECCHIOClientException could not log in
	 */
	public int copyHierarchy(int hierarchy_id, int target_hierarchy_id, String new_name) throws SPECCHIOClientException {
		return realClient.copyHierarchy(hierarchy_id, target_hierarchy_id, new_name);
	}
	
	
	
	/**
	 * Clears the known metaparameter list held by the server for this user
	 */
	public void clearMetaparameterRedundancyList() throws SPECCHIOClientException {
		
		realClient.clearMetaparameterRedundancyList();
		
	}
	
	
	/**
	 * Create a user account.
	 * 
	 * @param user	a user object describing the new user account
	 * 
	 * @return a new user object containing the complete account details
	 * 
	 * @throws SPECCHIOClientException
	 */
	public User createUserAccount(User user) throws SPECCHIOClientException {
		
		return realClient.createUserAccount(user);
		
	}
	
	
	/**
	 * Create a user account on this specchio server. Only for admin users.
	 * 
	 * @param user	a user object describing the new user account
	 * 
	 * @return a new user object containing the complete account details
	 * 
	 * @throws SPECCHIOClientException
	 */
	public User createUserAccountOnCurrentServer(User user) throws SPECCHIOClientException {
		return realClient.createUserAccountOnCurrentServer(user);
	}	
	
	/**
	 * Create a new instrument.
	 * 
	 * @param name	the name of the new instrument
	 * @throws SPECCHIOClientException 
	 */
	public void createInstrument(String name) throws SPECCHIOClientException {
		
		realClient.createInstrument(name);
		
	}
	
	
	/**
	 * Create a new reference.
	 * 
	 * @param name	the name of the new reference
	 * @throws SPECCHIOClientException 
	 */
	public void createReference(String name) throws SPECCHIOClientException {
		
		realClient.createReference(name);
		
	}
	
	/**
	 * Database upgrade
	 * 
	 * @param version	DB version to be upgraded to
	 * @param fis	File input stream with SQL upgrade statements
	 * @throws SPECCHIOClientException 
	 */	
	public void dbUpgrade(double version, FileInputStream fis)  throws SPECCHIOClientException {

		realClient.dbUpgrade(version, fis);
	}
	
	
	
	/**
	 * Delete calibration data from the database
	 * 
	 * @param calibration_id	the calibration identifier
	 * @throws SPECCHIOClientException 
	 */
	public void deleteCalibration(int calibration_id) throws SPECCHIOClientException {
		
		realClient.deleteCalibration(calibration_id);
		
	}
	
	
	/**
	 * Delete an instrument from the database.
	 * 
	 * @param instrument_id	the instrument identifier
	 */
	public void deleteInstrument(int instrument_id) throws SPECCHIOClientException {
		
		realClient.deleteInstrument(instrument_id);
		
	}
	
	
	/**
	 * Delete a picture of an instrument from the database.
	 * 
	 * @param picture_id	the picture identifier
	 */
	public void deleteInstrumentPicture(int picture_id) throws SPECCHIOClientException {
		
		realClient.deleteInstrumentPicture(picture_id);
		
	}
	
	/**
	 * Delete a reference from the database.
	 * 
	 * @param reference_id	the reference identifier
	 */
	public void deleteReference(int reference_id) throws SPECCHIOClientException {
		
		realClient.deleteReference(reference_id);
		
	}
	
	
	/**
	 * Delete a picture of a reference from the database.
	 * 
	 * @param picture_id	the picture identifier
	 */
	public void deleteReferencePicture(int picture_id) throws SPECCHIOClientException {
		
		realClient.deleteReferencePicture(picture_id);
		
	}
	
	
	/**
	 * Delete target-reference links from the database.
	 * 
	 * @param eav_id		the eav_id identifier
	 * 
	 * @return the number of links deleted
	 */
	public int deleteTargetReferenceLinks(int eav_id) throws SPECCHIOClientException {
		
		return realClient.deleteTargetReferenceLinks(eav_id);
		
	}


	/**
	 * Disconnect from the server.
	 */
	public void disconnect() throws SPECCHIOClientException {
		
		realClient.disconnect();
		
	}
	
	/**
	 * Get the spectrum identifiers that do have a reference to the specified attribute.
	 * 
	 * @param spectrum_ids	list of ids to filter
	 * @param attribute_name	attribute name to filter with
	 * 
	 * @return an array list of spectrum identifiers that match the filter
	 */
	public ArrayList<Integer> filterSpectrumIdsByHavingAttribute(ArrayList<Integer> spectrum_ids, String attribute_name) throws SPECCHIOClientException{
		
		return realClient.filterSpectrumIdsByHavingAttribute(spectrum_ids, attribute_name);		
		
	}
	
	
	
	/**
	 * Get the spectrum identifiers that do not have a reference to the specified attribute.
	 * 
	 * @param spectrum_ids	list of ids to filter
	 * @param attribute_name	attribute name to filter with
	 * 
	 * @return an array list of spectrum identifiers that match the filter
	 */
	public ArrayList<Integer> filterSpectrumIdsByNotHavingAttribute(ArrayList<Integer> spectrum_ids, String attribute_name) throws SPECCHIOClientException {
		
		return realClient.filterSpectrumIdsByNotHavingAttribute(spectrum_ids, attribute_name);
		
	}
	
	
	/**
	 * Get the spectrum identifiers that do reference to the specified attribute of a specified value.
	 * 
	 * @param spectrum_ids	list of ids to filter
	 * @param attribute_name	attribute name to filter with
	 * @param value	attribute value to match
	 * 
	 * @return an array list of spectrum identifiers that match the filter
	 */
	
	public ArrayList<Integer> filterSpectrumIdsByHavingAttributeValue(ArrayList<Integer> spectrum_ids, String attribute_name, Object value) throws SPECCHIOClientException {
		return realClient.filterSpectrumIdsByHavingAttributeValue(spectrum_ids, attribute_name, value);
	}
	
	
	
	/**
	 * Get the attributes for a metadata category.
	 * 
	 * @param category	the category name
	 * 
	 * @return an array of attribute objects
	 */
	public attribute[] getAttributesForCategory(String category) throws SPECCHIOClientException {
		
		if (!attributesByCategory.containsKey(category)) {
			attributesByCategory.put(category, realClient.getAttributesForCategory(category));
		}
		
		return attributesByCategory.get(category);
		
	}

	/**
	 * Get the attributes object containing information on all attributes, units and categories.
	 * 
	 * @return Attributes
	 */
	public attribute[] getAttributes() throws SPECCHIOClientException {
		
		if (attributes == null) {
			attributes = realClient.getAttributes();
		}
		
		return attributes;
		
	}
	
	
	/**
	 * Get the attributes hashtable
	 * 
	 * @return Attributes stored in hashtable, indexed by attribute_id
	 */
	public Hashtable<Integer, attribute> getAttributesIdHash() throws SPECCHIOClientException {
		
		if (attributesById == null) {
			attributesById = realClient.getAttributesIdHash();
		}
		
		return attributesById;
		
	}
	
	/**
	 * Get the attributes hashtable
	 * 
	 * @return Attributes stored in hashtable, indexed by attribute name
	 */
	public Hashtable<String, attribute> getAttributesNameHash() throws SPECCHIOClientException {
		
		if (attributesByName == null) {
			attributesByName = realClient.getAttributesNameHash();
		}
		
		return attributesByName;
		
	}
	
	
	/**
	 * Get the units for an attribute.
	 * 
	 * @param attr	the attribute
	 * 
	 * @return a units object representing the attribute's units
	 */
	public Units getAttributeUnits(attribute attr) throws SPECCHIOClientException {
		
		return realClient.getAttributeUnits(attr);
		
	}
	
	/**
	 * Get a campaign descriptor.
	 * 
	 * @param campaign_id	the campaign identifier
	 * 
	 * @return a new campaign object
	 */
	public Campaign getCampaign(int campaign_id) throws SPECCHIOClientException {
		
		return realClient.getCampaign(campaign_id);
		
	}
	
	
	/**
	 * Get the ids and names of all metadata categories.
	 * 
	 * @return an array list of category information
	 */
	public ArrayList<Category> getCategoriesInfo() throws SPECCHIOClientException {
		
		return realClient.getCategoriesInfo();
		
	}
	
	
	/**
	 * Export a campaign.
	 * 
	 * @param c		the campaign to be exported
	 * 
	 * @return an input stream connected to the exported campaign date
	 */
	public InputStream getCampaignExportInputStream(Campaign c) throws SPECCHIOClientException {
		
		return realClient.getCampaignExportInputStream(c);
		
	}
	
	
	/**
	 * Get a campaign node for the spectral data browser.
	 * 
	 * @param campaign_id		the campaign identifier
	 * @param order_by			the attribute by which to order the campaign's descendents
	 * @param restrict_to_view	show user's data only
	 * 
	 * @return a new campaign node object, or null if the campaign does not exist
	 */
	public campaign_node getCampaignNode(int campaign_id, String order_by, boolean restrict_to_view) throws SPECCHIOClientException {
		
		return realClient.getCampaignNode(campaign_id, order_by, restrict_to_view);
		
	}
	
	
	/**
	 * Get all of the campaigns in the database.
	 *
	 * @return an array of campaign objects descriving each campaign in the database
	 */
	public Campaign[] getCampaigns() throws SPECCHIOClientException {
		
		return realClient.getCampaigns();
		
	}
	
	/**
	 * Get all of the campaigns in the database.
	 * 
	 * @param load_metadata_details		defines if additional detailed metadata should be loaded
	 *
	 * @return an array of campaign objects describing each campaign in the database
	 */
	public Campaign[] getCampaigns(boolean load_metadata_details) throws SPECCHIOWebClientException {	
		
		return realClient.getCampaigns(load_metadata_details);
		
	}
	
	/**
	 * Get calibration ids for a list of spectra.
	 * 
	 * @param spectrum_ids	the spectrum identifiers
	 * 
	 * @return list of calibration ids, zero where no calibration is defined
	 */
	public ArrayList<Integer> getCalibrationIds(ArrayList<Integer> spectrum_ids) throws SPECCHIOWebClientException {
		
		return realClient.getCalibrationIds(spectrum_ids);
		
	}
	
	/**
	 * Get a calibrated instrument.
	 * 
	 * @param calibration_id	the calibration identifier
	 * 
	 * @return a new Instrument object, or null if the calibrated instrument does not exist
	 */
	public Instrument getCalibratedInstrument(int calibration_id) throws SPECCHIOClientException {
		
		return realClient.getCalibratedInstrument(calibration_id);
		
	}
	
	
	/**
	 * Get the value of a capability.
	 * 
	 * @param capability	the capability name
	 * 
	 * @return the value of the capability, or null if the capability is not recognised
	 */
	public String getCapability(String capability) throws SPECCHIOClientException {
		
		return realClient.getCapability(capability);
		
	}
	
	
	/**
	 * Get the children of a node of the spectral data browser.
	 * 
	 * @param sn	the node
	 * 
	 * @return a list of the node's children
	 */
	public List<spectral_node_object> getChildrenOfNode(spectral_node_object sn) throws SPECCHIOClientException {
		
		return realClient.getChildrenOfNode(sn);
		
	}
	
	
	/**
	 * Get the children of a node of a taxonomy.
	 * 
	 * @param tn	the node
	 * 
	 * @return a list of the node's children
	 */
	public List<TaxonomyNodeObject> getChildrenOfTaxonomyNode(TaxonomyNodeObject tn) throws SPECCHIOClientException {
		
		return realClient.getChildrenOfTaxonomyNode(tn);
		
	}
	
	
	/**
	 * Get the list of countries known to the server.
	 * 
	 * @return an array of Country objects
	 */
	public Country[] getCountries() throws SPECCHIOClientException {
		
		return realClient.getCountries();
		
	}
	
	
	/**
	 * Get a database node for the spectral data browser.
	 * 
	 * @param order_by			the attribute to order by
	 * @param restrict_to_view	display the current user's data only
	 * 
	 * @return a database_node object
	 */
	public database_node getDatabaseNode(String order_by, boolean restrict_to_view) throws SPECCHIOClientException {
		
		return realClient.getDatabaseNode(order_by, restrict_to_view);
		
	}
	
	
	/**
	 * Get the direct hierarchy id of a single spectrum
	 * 
	 * @param spectrum_id		the identifier of the spectrum
	 * 
	 * @return hierarchy id
	 * 
	 * @throws SPECCHIOFactoryException	
	 */	
	public Integer getDirectHierarchyId(int spectrum_id) throws SPECCHIOClientException {
		return realClient.getDirectHierarchyId(spectrum_id);
	}	
	
	/**
	 * Get hierarchy ids, directly above these spectra
	 * 
	 * @param spectrum_ids		the identifiers of the desired spectra
	 * 
	 * @return hierarchy ids
	 * 
	 * @throws SPECCHIOFactoryException	
	 */	
	public ArrayList<Integer> getDirectHierarchyIds(ArrayList<Integer> spectrum_ids) throws SPECCHIOClientException {
		return realClient.getDirectHierarchyIds(spectrum_ids);
	}
	
	/**
	 * Get distinct values of an attribute
	 * 
	 * @param attribute_id	id of the required attribute
	 * 
	 * @return arraylist of metaparameters	
	 */
	public ArrayList<MetaParameter> getDistinctValuesOfAttribute(int attribute_id)	throws SPECCHIOClientException {
		
		return realClient.getDistinctValuesOfAttribute(attribute_id);
				
	}	
	
	
	/**
	 * Get a conflicts in the EAV metadata for a set of spectra.
	 * 
	 * @param metadata_level		storage level identifier
	 * @param ids	the primary entity identifiers
	 * 
	 * @return a ConflictTable object containing all of the conflicts
	 */
	public ConflictTable getEavMetadataConflicts(int metadata_level, ArrayList<Integer> ids) throws SPECCHIOClientException {
		
		return realClient.getEavMetadataConflicts(metadata_level, ids);
		
	}
	
	/**
	 * Return an EAVQueryConditionObject configured for the supplied attribute.
	 * 
	 * @param attr	attribute object
	 * @return 
	 * 
	 * @return EAVQueryConditionObject
	 */
	public EAVQueryConditionObject getEAVQueryConditionObject(attribute attr)
	{
		return realClient.getEAVQueryConditionObject(attr);
	}	

	
	/**
	 * Get the count of existing metaparameters for the supplied spectrum ids and attribute id
	 * 
	 * @param attribute_id	id of the attribute
	 * @param ids	spectrum ids
	 * 
	 * @return Integer
	 */
	
	public Integer getExistingMetaparameterCount(Integer attribute_id, ArrayList<Integer> ids) throws SPECCHIOClientException {
		
		return realClient.getExistingMetaparameterCount(attribute_id, ids);
		
	}
	
	/**
	 * Get the file format identifier for a file format name.
	 * 
	 * @param format	the file format name
	 * 
	 * @return the identifier associated with the specified file format, or -1 if file format is not recognised
	 */
	public int getFileFormatId(String format) throws SPECCHIOClientException {
		
		return realClient.getFileFormatId(format);
		
	}
	
	/**
	 * Get the hierarchy object for a given hierarchy_id
	 * 
	 * @param hierarchy_id	the hierarchy_id identifying the required node
	 * 
	 * @return the hierarchy object, or -1 if the node does not exist
	 */	
	public Hierarchy getHierarchy(Integer hierarchy_id) throws SPECCHIOWebClientException 
	{
		return realClient.getHierarchy(hierarchy_id);
	}	
	
	
	/**
	 * Get the identifier of a hierarchy node.
	 * 
	 * @param campaign	the campaign in which the node is located
	 * @param name		the name of the node
	 * @param parent_id	the parent of the node
	 * 
	 * @return the identifier of the child of parent_id with the given name, or -1 if the node does not exist
	 */
	public int getHierarchyId(Campaign campaign, String name, int parent_id) throws SPECCHIOClientException {
		
		return realClient.getHierarchyId(campaign, name, parent_id);
		
	}
	
	/**
	 * Get a list of hierarchy ids, covering all hierarchies above these spectra
	 * 
	 * @param spectrum_ids		the identifiers of the desired spectra
	 * 
	 * @return hierarchy ids
	 * 
	 * @throws SPECCHIOFactoryException	
	 */	
	public ArrayList<Integer> getHierarchyIdsOfSpectra(ArrayList<Integer> spectrum_ids) throws SPECCHIOClientException {
			
		return realClient.getHierarchyIdsOfSpectra(spectrum_ids);
	}	
		
	
	/**
	 * Get the parent_id for a given hierarchy_id
	 * 
	 * @param hierarchy_id	the hierarchy_id identifying the required node
	 * 
	 * @return id of the parent of given hierarchy
	 */
	public int getHierarchyParentId(int hierarchy_id) throws SPECCHIOClientException {
		
		return realClient.getHierarchyParentId(hierarchy_id);
		
	}
	
	/**
	 * Get the file path of a hierarchy.
	 * 
	 * @param hierarchy_id		the identifier of the hierarchy
	 * 
	 * @returns path as string
	 * 
	 * @throws SPECCHIOFactoryException	the database could not accessed
	 */
	public String getHierarchyFilePath(int hierarchy_id) throws SPECCHIOClientException {
		
		return realClient.getHierarchyFilePath(hierarchy_id);		
	}	
	
	
	/**
	 * Get the name of a hierarchy.
	 * 
	 * @param hierarchy_id		the identifier of the hierarchy
	 * 
	 * @returns name as string
	 * 
	 * @throws SPECCHIOFactoryException	the database could not accessed
	 */	
	public String getHierarchyName(int hierarchy_id) throws SPECCHIOClientException	{
		
		return realClient.getHierarchyName(hierarchy_id);
		
	}			
	
	
	/**
	 * Get all of the institutes in the database.
	 * 
	 * @rerturn an array of Institute objects
	 */
	public Institute[] getInstitutes() throws SPECCHIOClientException {
		
		return realClient.getInstitutes();
		
	}
	
	
	/**
	 * Get an instrument.
	 * 
	 * @param instrument_id	the instrument identifier
	 * 
	 * @return a new Instrument object, or null if the instrument does not exist
	 */
	public Instrument getInstrument(int instrument_id) throws SPECCHIOClientException {
		
		return realClient.getInstrument(instrument_id);
		
	}
	
	
	/**
	 * Get the calibration metadata for an instrument.
	 * 
	 * @param instrument_id	the instrument identifier
	 * 
	 * @return an array of CalibrationMetadata objects, or null if the instrument does not exist
	 */
	public CalibrationMetadata[] getInstrumentCalibrationMetadata(int instrument_id) throws SPECCHIOClientException {
		
		return realClient.getInstrumentCalibrationMetadata(instrument_id);
		
	}

	/**
	 * Get instrument ids for a list of spectra.
	 * 
	 * @param spectrum_ids	the spectrum identifiers
	 * 
	 * @return list of instrument ids, zero where no instrument is defined
	 */
	public ArrayList<Integer> getInstrumentIds(ArrayList<Integer> spectrum_ids) throws SPECCHIOWebClientException {
		return realClient.getInstrumentIds(spectrum_ids);
	}
	
	
	/**
	 * Get descriptors for all of the instruments in the database.
	 * 
	 * @return an array of InstrumentDescriptor objects
	 */
	public InstrumentDescriptor[] getInstrumentDescriptors() throws SPECCHIOClientException {
		
		return realClient.getInstrumentDescriptors();
		
	}
	
	/**
	 * Get a instrument object for a given spectral file object.
	 * 
	 * @param spec_file		the spectral file
	 * 
	 * @return a new Instrument object
	 */	
	public Instrument getInstrumentForSpectralFile(SpectralFile spec_file) throws SPECCHIOClientException {
		
		return realClient.getInstrumentForSpectralFile(spec_file);
		
	}
	
	
	
	/**
	 * Get all of the pictures for an instrument.
	 * 
	 * @param instrument_id	the instrument identifier
	 * 
	 * @return a PictureTable object containing all of the pictures for this instrument, or null if the instrument doesn't exist
	 */
	public PictureTable getInstrumentPictures(int instrument_id) throws SPECCHIOClientException {
		
		return realClient.getInstrumentPictures(instrument_id);
		
	}
	
	
	/**
	 * Get a user object representing the user under which the client is logged in.
	 * 
	 * @return a reference to a user object, or null if the client is not connected
	 */
	public User getLoggedInUser() {
		
		return realClient.getLoggedInUser();
		
	}
	
	
	/**
	 * Get the metadata categories for application domain
	 * 
	 * @param field	the field name
	 * 
	 * @return a ArrayList<Integer> object, or null if the field does not exist
	 */
	public ArrayList<Integer> getMetadataCategoriesForApplicationDomain(int taxonomy_id) throws SPECCHIOClientException {
		
		ArrayList<Integer> categories = new ArrayList<Integer>();
		
		if(applicationDomainCategories == null)
		{
			ApplicationDomainCategories[] tmp = realClient.getMetadataCategoriesForApplicationDomains();
			
			// store in hash table
			applicationDomainCategories = new Hashtable<Integer, Integer[]>();
			
			for(int i=0;i<tmp.length;i++)
			{
				applicationDomainCategories.put(tmp[i].getTaxonomy_id(), tmp[i].getCategory_ids());
			}
			
		}
		
		Integer[] id_arr = applicationDomainCategories.get(taxonomy_id);
		
		if(id_arr != null)
		{
				for (int i = 0; i < id_arr.length; i++) {
					categories.add((Integer) id_arr[i]);
				}
		}

		return categories;
	}

	/**
	 * Get the metadata categories per application domain
	 * 
	 * @return an array of ApplicationDomainCategories object, or null if the information does not exist
	 */
	public ApplicationDomainCategories[] getMetadataCategoriesForApplicationDomains() throws SPECCHIOClientException {
		
		return realClient.getMetadataCategoriesForApplicationDomains();

	}
	
	
	
	/**
	 * Get the metadata categories for a metadata field.
	 * 
	 * @param field	the field name
	 * 
	 * @return a CategoryTable object, or null if the field does not exist
	 */
	public CategoryTable getMetadataCategoriesForIdAccess(String field) throws SPECCHIOClientException {
		
		if (!metadataCategoriesForIdAccess.containsKey(field)) {
			
			CategoryTable categories = realClient.getMetadataCategoriesForIdAccess(field);
			
			metadataCategoriesForIdAccess.put(field, categories);
			
			// build the reverse hash as well
			Hashtable<String, Integer> reverse = new Hashtable<String, Integer>();
			
			Enumeration<Integer> e = categories.keys();
			while(e.hasMoreElements())
			{
				Integer key = e.nextElement();
				reverse.put(categories.get(key), key);
			}
			
			metadataCategoriesForNameAccess.put(field, reverse);
			
		}
		
		return metadataCategoriesForIdAccess.get(field);
		
	}
	
	
	/**
	 * Get the metadata categories for a metadata field, ready for access via name
	 * 
	 * @param field	the field name
	 * 
	 * @return a CategoryTable object, or null if the field does not exist
	 */
	public Hashtable<String, Integer> getMetadataCategoriesForNameAccess(String field) throws SPECCHIOClientException {
		
		if (!metadataCategoriesForIdAccess.containsKey(field)) {
			
			getMetadataCategoriesForIdAccess(field);
		
		}
		
		return metadataCategoriesForNameAccess.get(field);
	}
	
	
	
	
	/**
	 * Get metaparameter for spectrum id and EAV attribute
	 * 
	 * @param id		spectrum id
	 * @param attribute_name		attribute name
	 * 
	 * @return metaparameter, or null if the field does not exist	 
	 */
	public MetaParameter getMetaparameter(Integer id, String attribute_name) throws SPECCHIOWebClientException {
		
		return realClient.getMetaparameter(id, attribute_name);	
	}			
	
	
	/**
	 * Get metaparameters for spectrum ids and EAV attribute
	 * 
	 * @param ids		spectrum ids
	 * @param attribute		attribute name
	 * 
	 * @return list of metaparameters, or null if the field does not exist	 
	 */
	public ArrayList<MetaParameter> getMetaparameters(ArrayList<Integer> ids, String attribute_name) throws SPECCHIOWebClientException{
		
		return realClient.getMetaparameters(ids, attribute_name);
		
	}
	
	/**
	 * Get metaparameters for spectrum ids and EAV attribute
	 * 
	 * @param ids		spectrum ids
	 * @param attribute_name		attribute name
	 * @param distinct		defines if distinct values should be returned or repeated values for the given spectrum ids: distinct = false means parameters for all spectra, even if redundant
	 * 
	 * @return list of metaparameters, or null if the field does not exist	 
	 */
	public ArrayList<MetaParameter> getMetaparameters(ArrayList<Integer> ids, String attribute_name, Boolean distinct) throws SPECCHIOWebClientException {

			return realClient.getMetaparameters(ids, attribute_name, distinct);

	}
	
	
	/**
	 * Get list of metaparameters for spectrum ids and EAV attributes
	 * 
	 * @param ids		spectrum ids
	 * @param attribute_ids		list of attribute ids
	 * 
	 * @return list of list of metaparameters, or null if the field does not exist	 
	 */
	public ArrayList<ArrayList<MetaParameter>> getMetaparameters(ArrayList<Integer> ids, ArrayList<Integer> attribute_ids) throws SPECCHIOWebClientException{
		
		return realClient.getMetaparameters(ids, attribute_ids);
		
	}
	
	/**
	 * Get metaparameter value statistics for spectrum ids and EAV attribute
	 * 
	 * @param ids		spectrum ids
	 * @param attribute_name		attribute name
	 * 
	 * @return structure with Metaparameter holding statistic values	 
	 */
	public MetaparameterStatistics getMetaparameterStatistics(ArrayList<Integer> ids, String attribute_name) throws SPECCHIOWebClientException{
		return realClient.getMetaparameterStatistics(ids, attribute_name);
	}
	
	
	
	
	/**
	 * Get values for spectrum ids and EAV attribute  (non-distinct values by default)
	 * 
	 * @param ids				spectrum ids
	 * @param attribute_name	attribute name
	 * 
	 * @return list of values, or null if the field does not exist
	 */	
	public MatlabAdaptedArrayList<Object> getMetaparameterValues(ArrayList<Integer> ids, String attribute_name) throws SPECCHIOWebClientException {
		
		return realClient.getMetaparameterValues(ids, attribute_name);
		
	}
	
	/**
	 * Get values for spectrum ids and EAV attribute
	 * 
	 * @param ids		spectrum ids
	 * @param attribute		attribute name
	 * @param distinct		defines if distinct values should be returned or repeated values for the given spectrum ids
	 * 
	 * @return list of values, or null if the field does not exist	 
	 */
	public MatlabAdaptedArrayList<Object> getMetaparameterValues(ArrayList<Integer> ids, String attribute_name, Boolean distinct) throws SPECCHIOWebClientException {
	
		return realClient.getMetaparameterValues(ids, attribute_name, distinct);
		
	}
	
	/**
	 * Get the metadata conflicts for a set of spectra and set of fields.
	 * 
	 * @param spectrum_ids	the spectrum identifiers
	 * @param fields		the fields to check
	 * 
	 * @return a ConflictTable object listing the conflicts found
	 */
	public ConflictTable getMetadataConflicts(ArrayList<Integer> spectrum_ids, String fields[]) throws SPECCHIOClientException {
		
		return realClient.getMetadataConflicts(spectrum_ids, fields);
		
	}
	
	/**
	 * Get measurement unit for ASD based coding.
	 * 
	 * @param coding	coding based on ASD coding
	 * 
	 * @return a new MeasurementUnit object, or null if the coding does not exist
	 */	
	public MeasurementUnit getMeasurementUnitFromCoding(int coding) throws SPECCHIOWebClientException {
		
		return realClient.getMeasurementUnitFromCoding(coding);
		
	}
	
	/**
	 * Get newest N spectra.
	 * 
	 * @param N	
	 * 
	 * @return list of spectrum ids ordered by data ingestion time
	 */	
	public ArrayList<Integer> getNewestSpectra(int number_of_spectra) throws SPECCHIOWebClientException
	{
		return realClient.getNewestSpectra(number_of_spectra);		
	}	
	
	/**
	 * Get the data usage policies for a space.
	 * 
	 * @param space	the space
	 * 
	 * @return an array of Policy objects
	 */
	public String[] getPoliciesForSpace(Space space) throws SPECCHIOClientException {
		
		return realClient.getPoliciesForSpace(space);
		
	}
	
	/**
	 * Get an empty query object
	 * 
	 * @return a Query Object
	 */
	public Query getQueryObject() throws SPECCHIOClientException {
		return realClient.getQueryObject();
	}
	
	
	
	/**
	 * Get a reference.
	 * 
	 * @param reference_id	the reference identifier
	 * 
	 * @return a new Reference object, or null if the instrument does not exist
	 */
	public Reference getReference(int reference_id) throws SPECCHIOClientException {
		
		return realClient.getReference(reference_id);
		
	}
	
	
	/**
	 * Get the calibration metadata for a reference.
	 * 
	 * @param reference_id	the reference identifier
	 * 
	 * @return an array of CalibrationMetadata objects, or null if the reference does note exist
	 */
	public CalibrationMetadata[] getReferenceCalibrationMetadata(int reference_id) throws SPECCHIOClientException {
		
		return realClient.getReferenceCalibrationMetadata(reference_id);
		
	}
	
	
	/**
	 * Get descriptors for all of the references in the database.
	 * 
	 * @return an arra of ReferenceDescriptor objects
	 */
	public ReferenceDescriptor[] getReferenceDescriptors() throws SPECCHIOClientException {
		
		return realClient.getReferenceDescriptors();
		
	}
	
	
	/**
	 * Get all of the reference brands in the database.
	 * 
	 * @return an array of ReferenceBrand objects
	 */
	public ReferenceBrand[] getReferenceBrands() throws SPECCHIOClientException {
		
		return realClient.getReferenceBrands();
		
	}
	
	
	/**
	 * Get all of the pictures associated with a reference.
	 * 
	 * @param reference_id	the reference identifier
	 * 
	 * @return a PictureTable containing all of the pictures
	 */
	public PictureTable getReferencePictures(int reference_id) throws SPECCHIOClientException {
		
		return realClient.getReferencePictures(reference_id);
		
	}
	
	
	/**
	 * Get a reference space.
	 * 
	 * @param input_ids
	 * @param local_ids
	 * 
	 * @return a Space object, or null if no space could be found
	 */
	public ReferenceSpaceStruct getReferenceSpace(ArrayList<Integer> input_ids) throws SPECCHIOClientException {
		
		return realClient.getReferenceSpace(input_ids);
		
	}
	
	
	/**
	 * Get a server descriptor that describes the server to which this client is connected.
	 * 
	 * @return a new server descriptor object
	 */
	public SPECCHIOServerDescriptor getServerDescriptor() {
		
		return serverDescriptor;
		
	}
	
	
	public void setServerDescriptor(SPECCHIOServerDescriptor serverDescriptor) {
		this.serverDescriptor = serverDescriptor;
	}


	/**
	 * Get all of the sensors in the database.
	 * 
	 * @return an array of Sensor objects
	 */
	public Sensor[] getSensors() throws SPECCHIOClientException {
		
		return realClient.getSensors();
		
	}
	
	
	/**
	 * Get sensor sampling geometry 
	 * 
	 * @param space		holds spectra ids of which geometry is to retrieved
	 * 
	 * @return a new GonioSamplingPoints, or null if no geometries were found
	 * @throws SPECCHIOWebClientException 
	 */		
	public GonioSamplingPoints getSensorSamplingGeometry(SpectralSpace space) throws SPECCHIOWebClientException {
		
		return realClient.getSensorSamplingGeometry(space);
		
	}


	/**
	 * Get the space objects for a set of spectrum identifiers.
	 * 
	 * @param ids								the spectrum identifiers
	 * @param split_spaces_by_sensor
	 * @param split_spaces_by_sensor_and_unit
	 * @param order_by							the field to order by
	 */
	public Space[] getSpaces(ArrayList<Integer> ids, boolean split_spaces_by_sensor, boolean split_spaces_by_sensor_and_unit, String order_by) throws SPECCHIOClientException {
		
		return realClient.getSpaces(ids, split_spaces_by_sensor, split_spaces_by_sensor_and_unit, order_by);
		
	}
	
	/**
	 * Get the space objects for a set of spectrum identifiers.
	 * Configured to sort by sensor, unit, instrument and calibration
	 * 
	 * @param ids								the spectrum identifiers
	 * @param order_by							the field to order by
	 */
	public Space[] getSpaces(ArrayList<Integer> ids, String order_by) throws SPECCHIOWebClientException {
	
		return realClient.getSpaces(ids, order_by);
	
	}
	
	
	
	/**
	 * Get a spectrum.
	 * 
	 * @param spectrum_id	the spectrum identifier
	 * @param load_metadata	load all spectrum metadata?
	 * 
	 * @return a Spectrum object
	 */
	public Spectrum getSpectrum(int spectrum_id, boolean load_metadata) throws SPECCHIOClientException {
		
		return realClient.getSpectrum(spectrum_id, load_metadata);
		
	}
	
	
	/**
	 * Get the calibration spaces for a set of spectra.
	 * 
	 * @param spectrum_ids	the spectrum identifiers
	 * 
	 * @return an array of Space objects
	 */
	public Space[] getSpectrumCalibrationSpaces(ArrayList<Integer> spectrum_ids) throws SPECCHIOClientException {
		
		return realClient.getSpectrumCalibrationSpaces(spectrum_ids);
		
	}
	
	/**
	 * Get the number of spectra in the database
	 * 
	 * @return the number of spectra in the database
	 */
	public int getSpectrumCountInDB() throws SPECCHIOClientException {
		return realClient.getSpectrumCountInDB();
	}	
	
	
	/**
	 * Get the spectrum factor table for a set of spectra.
	 * 
	 * @param spectrum_ids_1
	 * @param spectrum_ids_2
	 * 
	 * @return a SpectrumFactorTable object
	 */
	public SpectrumFactorTable getSpectrumFactorTable(ArrayList<Integer> spectrum_ids_1, ArrayList<Integer> spectrum_ids_2) throws SPECCHIOClientException {
		
		return realClient.getSpectrumFactorTable(spectrum_ids_1, spectrum_ids_2);
		
	}
	
	
	/**
	 * Get the identifiers of all spectra beneath a given node of the spectral data browser
	 * 
	 * @param sn	the node
	 * 
	 * @return a list of spectrum identifiers
	 */
	public List<Integer> getSpectrumIdsForNode(spectral_node_object sn) throws SPECCHIOClientException {
		
		return realClient.getSpectrumIdsForNode(sn);
		
	}
	
	/**
	 * Get the identifiers of all spectra that match a full text search.
	 * 
	 * @param search_str		the search string
	 * 
	 * @return an array list of spectrum identifiers
	 */
	public ArrayList<Integer> getSpectrumIdsMatchingFullTextSearch(String search_str) throws SPECCHIOClientException {
		
		return realClient.getSpectrumIdsMatchingFullTextSearch(search_str);
		
	}
	
	
	
	/**
	 * Get the spectrum identifiers that match a given query.
	 * 
	 * @param query	the query
	 * 
	 * @return an array list of spectrum identifiers that match the query
	 */
	public ArrayList<Integer> getSpectrumIdsMatchingQuery(Query query) throws SPECCHIOClientException {
		
		return realClient.getSpectrumIdsMatchingQuery(query);
		
	}
	
	
	/**
	 * Get the identifiers of all spectra directly belonging to a hierarchy
	 * 
	 * @param hierarchy_id	the hierarchy_id
	 * 
	 * @return a list of spectrum identifiers
	 */
	public ArrayList<Integer> getDirectSpectrumIdsOfHierarchy(int hierarchy_id) throws SPECCHIOClientException{
		
		return realClient.getDirectSpectrumIdsOfHierarchy(hierarchy_id);
	}

	
	
	/**
	 * Get the pictures associated with a spectrum.
	 * 
	 * @param spectrum_id	the spectrum identifier
	 * 
	 * @return a PictureTable containing all of the pictures associated with the spectrum
	 */
	public PictureTable getSpectrumPictures(int spectrum_id) throws SPECCHIOClientException {
		
		return realClient.getSpectrumPictures(spectrum_id);
		
	}
	
	
	/**
	 * Get the number of spectra that matach a given query.
	 * 
	 * @param query	the query
	 * 
	 * @return the number of spectra that match the query
	 */
	public int getSpectrumQueryCount(Query query) throws SPECCHIOClientException {
		
		return realClient.getSpectrumQueryCount(query);
		
	}	
	
	
	
	/**
	 * Get the spectrum data links that refer to a given set of targets and/or references.
	 * 
	 * @param target_ids	the identifiers of the target spectra (null or empty to match all targets)
	 * @param reference_ids	the identifiers of the reference spectra (null or empty to match all references)
	 * 
	 * @returns an array of SpectrumDataLink objects
	 */
	public SpectrumDataLink[] getTargetReferenceLinks(ArrayList<Integer> target_ids, ArrayList<Integer> reference_ids) throws SPECCHIOClientException {
		
		return realClient.getTargetReferenceLinks(target_ids, reference_ids);
		
	}
	
	/**
	 * Get the identifier of a sub-hierarchy with a given name, creating the
	 * hierarchy if it doesn't exist.
	 * 
	 * @param campaign	the campaign into which to insert the hierarchy
	 * @param parent_id			the identifier of the the parent of the hierarchy
	 * @param hierarchy_name	the name of the desired hierarchy
	 * 
	 * @return the identifier of the child of parent_id with the name hierarchy_name
	 */
	public int getSubHierarchyId(Campaign campaign, String name, int parent_id) throws SPECCHIOClientException {
		
		return realClient.getSubHierarchyId(campaign, name, parent_id);
		
	}
	
	/**
	 * Get the identifier of a sub-hierarchy with a given name, creating the
	 * hierarchy if it doesn't exist.
	 * 
	 * @param name	the name of the desired hierarchy
	 * @param parent_id			the identifier of the the parent of the hierarchy	 
	 * 
	 * @return the identifier of the child of parent_id with the name hierarchy_name
	 */
	public int getSubHierarchyId(String name, int parent_id) throws SPECCHIOClientException	{
		
		return realClient.getSubHierarchyId(name, parent_id);
		
	}
		
	
	
		
	/**
	 * Get the top node for a given taxonomy
	 * 
	 * @param attribute_id	id of the attribute that defines the taxonomy
	 */
	public TaxonomyNodeObject getTaxonomyRootNode(int attribute_id) throws SPECCHIOClientException {
		
		return realClient.getTaxonomyRootNode(attribute_id);
		
	}
	
	
	/**
	 * Get the node for a given taxonomy
	 * 
	 * @param taxonomy_id	taxonomy_id that defines the taxonomy
	 */
	public TaxonomyNodeObject getTaxonomyNode(int taxonomy_id) throws SPECCHIOClientException {
		
		return realClient.getTaxonomyNode(taxonomy_id);
		
	}
	
	
	/**
	 * Get the id for a given taxonomy node in a given taxonomy
	 * 
	 * @param attribute_id	attribute_id that defines the taxonomy
	 *  @param name		name of the node of which the id is required
	 * 
	 */
	public int getTaxonomyId(int attribute_id, String name)  throws SPECCHIOClientException {
		
		if(!taxonomiesHash.containsKey(attribute_id))
		{
			getTaxonomyHash(attribute_id);
		}
		
		
		Hashtable<String, Integer> taxonomy_hash = taxonomiesHash.get(attribute_id);
		
		return taxonomy_hash.get(name);		
		
	}
	
	
	/**
	 * Get the taxonomy hash for a given taxonomy
	 * 
	 * @param attribute_id	attribute_id that defines the taxonomy
	 * 
	 */
	public Hashtable<String, Integer> getTaxonomyHash(int attribute_id)  throws SPECCHIOClientException {
		
		if(!taxonomiesHash.containsKey(attribute_id))
			taxonomiesHash.put(attribute_id, realClient.getTaxonomyHash(attribute_id));
				
		return taxonomiesHash.get(attribute_id);
	}
	
	/**
	 * Get the ID to name hash for a given taxonomy
	 * 
	 * @param attribute_id	attribute_id that defines the taxonomy
	 * 
	 */
	public Hashtable<Integer, String> getTaxonomyIdToNameHash(int attribute_id)  throws SPECCHIOClientException {
		
		return realClient.getTaxonomyIdToNameHash(attribute_id);		
	}	
	
	/**
	 * Get a list of all of the users in the database.
	 * 
	 * @return an array of User objects
	 * 
	 * @throws SPECCHIOClientException
	 */
	public User[] getUsers() throws SPECCHIOClientException {
		
		return realClient.getUsers();
		
	}
	
	
	/**
	 * Get a list of all of the users in the database with added user statistics (number of loaded spectra, number of campaigns).
	 * 
	 * @return an array of User objects
	 * 
	 * @throws SPECCHIOClientException
	 */
	public User[] getUsersWithStatistics() throws SPECCHIOWebClientException {
		
		return realClient.getUsersWithStatistics();
	}
	
	/**
	 * Import a campaign.
	 * 
	 * @param user_id	the identifier of the user to whom the campaign will belong
	 * @param is	the input stream from which to read the campaign
	 */
	public void importCampaign(int user_id, InputStream is) throws SPECCHIOClientException {
		
		realClient.importCampaign(user_id, is);
		
	}
	
	/**
	 * Import a campaign from a file that is on the Glassfish server (used for bigger files to prevent timeouts)
	 * 
	 * @param user_id	the identifier of the user to whom the campaign will belong
	 * @param server_filepath		the server filepath from which to read the campaign
	 */
	public void importCampaign(int user_id, String server_filepath) throws SPECCHIOWebClientException {
		
		realClient.importCampaign(user_id, server_filepath);
		
	}



	/**
	 * Insert a new campaign into the database
	 * 
	 * @param campaign	the campaign
	 * 
	 * @return the identifier of the new campaign
	 */
	public int insertCampaign(Campaign campaign) throws SPECCHIOClientException {
		
		return realClient.insertCampaign(campaign);
		
	}
	
	
	/**
	 * Insert a hierarchy node.
	 * 
	 * @param campaign	the campaign into which to insert the hierarchy
	 * @param name		the name of the new hierarchy
	 * @param parent_id	the identifier of the parent of the new hierarchy
	 * 
	 * @return the identifier of the new hierarchy node
	 */
	public int insertHierarchy(Campaign campaign, String name, int parent_id) throws SPECCHIOClientException {
		
		return realClient.insertHierarchy(campaign, name, parent_id);
		
	}
	
	
	/**
	 * Insert a new institute into the database
	 * 
	 * @param institute	an Instite object describing the new institute
	 * 
	 * @return the identifier of the new institute
	 */
	public int insertInstitute(Institute institute) throws SPECCHIOClientException {
		
		return realClient.insertInstitute(institute);
		
	}
	
	
	/**
	 * Insert calibration for an instrument.
	 * 
	 * @param c		the calibration data
	 */
	public void insertInstrumentCalibration(Calibration c) throws SPECCHIOClientException {
		
		realClient.insertInstrumentCalibration(c);
		
	}
	
	
	/**
	 * Insert a picture of an instrument into the database.
	 * 
	 * @param picture	the picture
	 */
	public void insertInstrumentPicture(Picture picture) throws SPECCHIOClientException {
		
		realClient.insertInstrumentPicture(picture);
		
	}
	
	
	/**
	 * Insert calibration for a reference into the database.
	 * 
	 * @param c		the calibration data
	 */
	public void insertReferenceCalibration(Calibration c) throws SPECCHIOClientException {
		
		realClient.insertReferenceCalibration(c);
		
	}
	
	
	/**
	 * Insert a picture associated with a reference into the database.
	 * 
	 * @param picture	the picture
	 */
	public void insertReferencePicture(Picture picture) throws SPECCHIOClientException {
		
		realClient.insertReferencePicture(picture);
		
	}
	
	
	/**
	 * Insert a spectral file into the database.
	 * 
	 * @param spec_file	the file
	 * 
	 * @return a list of spectrum identifiers that were inserted into the database
	 */
	public SpectralFileInsertResult insertSpectralFile(SpectralFile spec_file) throws SPECCHIOClientException {
		
		return realClient.insertSpectralFile(spec_file);
		
	}
	
	
	/**
	 * Insert a target-reference link to closest reference on acquisition timeline
	 * 
	 * @param target_id		the identifier of the target node
	 * @param reference_ids	the identifiers of the reference nodes
	 * 
	 * @return the number of links sucessfully created
	 * 
	 * @throws SPECCHIOClientException
	 */
	public int insertClosestTargetReferenceLink(int target_id, ArrayList<Integer> reference_ids) throws SPECCHIOClientException {
		
		return realClient.insertClosestTargetReferenceLink(target_id, reference_ids);
		
	}

	
	
	
	/**
	 * Insert a target-reference link.
	 * 
	 * @param target_id		the identifier of the target node
	 * @param reference_ids	the identifiers of the reference nodes
	 * 
	 * @return the number of links sucessfully created
	 * 
	 * @throws SPECCHIOClientException
	 */
	public int insertTargetReferenceLinks(int target_id, ArrayList<Integer> reference_ids) throws SPECCHIOClientException {
		
		return realClient.insertTargetReferenceLinks(target_id, reference_ids);
		
	}
	
	/**
	 * Test for the existence of a calibration in the database.
	 * 
	 * @param cal		calibration object to check
	 * 
	 * @return true if the calibration already exists in the database, false otherwise
	 */
	 public boolean instrumentCalibrationExists(Calibration cal) throws SPECCHIOWebClientException {
			
			return realClient.instrumentCalibrationExists(cal);
			
		}		


	/**
	 * Test whether or not the client is logged in under a given role.
	 * 
	 * @param roleName	the role to be tested
	 * 
	 * @return "true" if the client is logged in as a user with role roleIn, "false" otherwise
	 */
	public boolean isLoggedInWithRole(String roleName) throws SPECCHIOClientException {
		
		return realClient.isLoggedInWithRole(roleName);
		
	}
	
	
	/**
	 * Get the meta-parameter of the given metaparameter identifier.
	 * 
	 * @param id		the metaparameter identifier for which to retrieve metadata
	 * 
	 * @return the meta-parameter object corresponding to the desired id
	 *
	 * @throws SPECCHIOClientException
	 */
	public MetaParameter loadMetaparameter(int metaparameter_id) throws SPECCHIOClientException {		
		return realClient.loadMetaparameter(metaparameter_id);

	}


	
	/**
	 * Load a sensor definition into the database from an input stream.
	 * 
	 * @param is	the input stream
	 */
	public void loadSensor(InputStream is) throws SPECCHIOClientException {
		
		realClient.loadSensor(is);
		
	}
	
	
	/**
	 * Load a Space object.
	 * 
	 * @param space	a partially-filled space object
	 * 
	 * @return a complete Space object
	 */
	public Space loadSpace(Space space) throws SPECCHIOClientException {
		
		return realClient.loadSpace(space);
		
	}
	
	/**
	 * Move a hierarchy to a new parent hierarchy within the same campaign. If a hierarchy of the same name exists in the target hierarchy then the hierarchies are merged.
	 * 
	 * @param source_hierarchy_id	hierarchy id of the hierarchy to move
	 * @param target_parent_hierarchy	hierarchy id of the new parent hierarchy
	 * 
	 * return true if move was done
	 */
	public boolean moveHierarchy(int source_hierarchy_id, int target_parent_hierarchy) throws SPECCHIOClientException {
		
		return realClient.moveHierarchy(source_hierarchy_id, target_parent_hierarchy);
		
	}
	
	
	
	/**
	 * Causes the client to reload data values the specified category upon next request.
	 * 
	 * @param field name
	 * 
	 */	
	public void refreshMetadataCategory(String field) {
		
		metadataCategoriesForIdAccess.remove(field);
		
	}
	
	
	
	/**
	 * Remove an item of EAV metadata.
	 * 
	 * @param mp	the meta-parameter to be removed
	 */
	public void removeEavMetadata(MetaParameter mp) throws SPECCHIOClientException {
		
		realClient.removeEavMetadata(mp);
		
	}
	
	
	/**
	 * Remove an item of EAV metadata for a collection of spectra.
	 * 
	 * @param mp			the meta-parameter to be removed
	 * @param spectrum_ids	the spectrum identifiers
	 */
	public void removeEavMetadata(MetaParameter mp, ArrayList<Integer> spectrum_ids) throws SPECCHIOClientException {
		
		realClient.removeEavMetadata(mp, spectrum_ids);
		
	}

	/**
	 * Remove one or more items of EAV metadata for a collection of spectra for a defined attribute.
	 * 
	 * @param attr			the attribute to be removed
	 * @param spectrum_ids	the spectrum identifiers
	 * @param metadata_level		storage level identifier
	 */	
	public void removeEavMetadata(attribute attr, ArrayList<Integer> spectrum_ids, int metadata_level) throws SPECCHIOClientException {
		
		realClient.removeEavMetadata(attr, spectrum_ids, metadata_level);
		
	}
	
	
	
	
	/**
	 * Remove the data corresponding to a node of the spectral data browser.
	 * 
	 * @param sn	the node to be removed
	 */
	public void removeSpectralNode(spectral_node_object sn) throws SPECCHIOClientException {
		
		realClient.removeSpectralNode(sn);
		
	}
	
	/**
	 * Remove the data corresponding to a node of the spectral data browser.
	 * 
	 * @param sns	list of nodes to be removed
	 */
	public void removeSpectralNodes(ArrayList<spectral_node_object> sns) throws SPECCHIOWebClientException {
		
		realClient.removeSpectralNodes(sns);
		
	}	
	
	/**
	 * Rename a hierarchy in the database and also on the file system if path is accessible.
	 * The rename on the database will only be applied if a rename on the file system succeeded.
	 * 
	 * @param hierarchy_id	id of the hierarchy to be renamed
	 * @param name	new name of the hierarchy
	 * 
	 * @return true if and only if the renaming succeeded; false otherwise
	 */	
	public boolean renameHierarchy(int hierarchy_id, String name) throws SPECCHIOClientException {
		
		return realClient.renameHierarchy(hierarchy_id, name);
		
	}		
	
	
	
	/**
	 * Set the progress report interface to which progress made by this
	 * client will be reported.
	 * 
	 * @param pr	the progress report; use null to report no progress
	 */
	public void setProgressReport(ProgressReportInterface pr) {
		
		this.pr = pr;
		realClient.setProgressReport(pr);
		
	}
	
	
	/**
	 * Sort spectra by the values of the specified attributes
	 * 
	 * @param spectrum_ids	list of ids to sort
	 * @param attribute_names	attribute names to sort by
	 * 
	 * @return a AVMatchingListCollection object
	 */
	public AVMatchingListCollection sortByAttributes(ArrayList<Integer> spectrum_ids, String... attribute_names) throws SPECCHIOClientException {
		
		return realClient.sortByAttributes(spectrum_ids, attribute_names);
	}


	
	/**
	 * Test for the existence of a spectral file in the database.
	 * 
	 * @param spec_file		spectral file object to check
	 * 
	 * @return true if the file already exists in the database, false otherwise
	 */
	public boolean spectralFileExists(SpectralFile spec_file) throws SPECCHIOClientException {
		
		return realClient.spectralFileExists(spec_file);
		
	}
	
	/**
	 * Test for the existence of a spectral files in the database.
	 * 
	 * @param spec_files	container with arraylist of spectral files to check
	 * 
	 * @return array of boolean values indicating existence
	 */
	public boolean[] spectralFilesExist(SpectralFiles spec_files) throws SPECCHIOClientException {
		
		return realClient.spectralFilesExist(spec_files);
		
	}
	
	
	
	/**
	 * Submit a collection to Research Data Australia.
	 * 
	 * @param collection_d	the collection descriptor
	 * 
	 * @return the collection identifier of the new collection, or an empty string if publication failed
	 * 
	 * @throws SPECCHIOClientException	could not contact the server
	 */
	public String submitRDACollection(RDACollectionDescriptor collection_d) throws SPECCHIOClientException {
		
		return realClient.submitRDACollection(collection_d);
		
	}
		
	
	/**
	 * Update the information about a campaign
	 * 
	 * @param campaign	the new campaign data
	 */
	public void updateCampaign(Campaign campaign) throws SPECCHIOClientException {
		
		realClient.updateCampaign(campaign);
		
	}
	
	
	/**
	 * Update EAV metadata.
	 * 
	 * @param mp			the meta-parameter to update
	 * @param spectrum_ids	the identifiers for which to update the parameter
	 * 
	 * @return the identifier of the inserted metadata
	 */
	public int updateEavMetadata(MetaParameter mp, ArrayList<Integer> spectrum_ids) throws SPECCHIOClientException {
		
		return realClient.updateEavMetadata(mp, spectrum_ids);
		
	}
	
	
	/**
	 * Update EAV metadata.
	 * 
	 * @param mp			the meta-parameter to update
	 * @param spectrum_ids	the identifiers for which to update the parameter
	 * @param mp_old		the old meta-parameter
	 * 
	 * @return the identifier of the inserted metadata
	 */
	public int updateEavMetadata(MetaParameter mp, ArrayList<Integer> spectrum_ids, MetaParameter mp_old) throws SPECCHIOClientException {
		
		return realClient.updateEavMetadata(mp, spectrum_ids, mp_old);
		
	}
	
	/**
	 * Update EAV metadata annotation.
	 * 
	 * @param mp			the meta-parameter to update
	 * @param spectrum_ids	the identifiers for which to update the parameter
	 * 
	 * @return the identifier of the inserted metadata
	 */
	public int updateEavMetadataAnnotation(MetaParameter mp, ArrayList<Integer> spectrum_ids) throws SPECCHIOWebClientException{
		
		return realClient.updateEavMetadataAnnotation(mp, spectrum_ids);
		
	}
	
	
	
	/**
	 * Update an instrument.
	 * 
	 * @param instrument	the instrument
	 */
	public void updateInstrument(Instrument instrument) throws SPECCHIOClientException {
		
		realClient.updateInstrument(instrument);
		
	}
	
	
	/**
	 * Update or insert EAV metadata. Will automatically update existing entries or insert a new metaparameter if not existing.
	 * 
	 * @param mp			the meta-parameter to update or insert
	 * @param spectrum_ids	the identifiers for which to update or insert the parameter
	 * 
	 * @return the identifier of the inserted or updated metadata
	 */
	public int updateOrInsertEavMetadata(MetaParameter mp, ArrayList<Integer> spectrum_ids) throws SPECCHIOWebClientException {
		
		return realClient.updateOrInsertEavMetadata(mp, spectrum_ids);
		
	}		
	
	
	/**
	 * Update the calibration metadata for an instrument.
	 * 
	 * @param cm	the calibration metadata
	 */
	public void updateInstrumentCalibrationMetadata(CalibrationMetadata cm) throws SPECCHIOClientException {
		
		realClient.updateInstrumentCalibrationMetadata(cm);
		
	}
	
	
	/**
	 * Update a picture associated with an instrument.
	 * 
	 * @param picture	the picture
	 */
	public void updateInstrumentPicture(Picture picture) throws SPECCHIOClientException {
		
		realClient.updateInstrumentPicture(picture);
		
	}
	
	
	/**
	 * Update an reference.
	 * 
	 * @param reference	the reference
	 */
	public void updateReference(Reference reference) throws SPECCHIOClientException {
		
		realClient.updateReference(reference);
		
	}
	
	
	/**
	 * Update the calibration metadata for a reference.
	 * 
	 * @param cm	the calibration metadata
	 */
	public void updateReferenceCalibrationMetadata(CalibrationMetadata cm) throws SPECCHIOClientException {
		
		realClient.updateReferenceCalibrationMetadata(cm);
		
	}
	
	
	/**
	 * Update a picture associated with a reference.
	 * 
	 * @param picture	the picture
	 */
	public void updateReferencePicture(Picture picture) throws SPECCHIOClientException {
		
		realClient.updateReferencePicture(picture);
		
	}
	
	
	/**
	 * Update the metadata fields for a set of spectra
	 * 
	 * @param spectrum_ids	the spectrum identifiers
	 * @param field			the name of the field to be updated
	 * @param id
	 */
	public void updateSpectraMetadata(ArrayList<Integer> ids, String field, int id) throws SPECCHIOClientException {
		
		realClient.updateSpectraMetadata(ids, field, id);
		
	}
	
	/**
	 * Update the spectral vector of a spectrum
	 * 
	 * @param spectrum_id	the spectrum identifier
	 * @param vector		new spectral data
	 * 
	 * @throws SPECCHIOClientException
	 */
	public void updateSpectrumVector(int spectrum_id, float[] vector) throws SPECCHIOClientException {
		
		realClient.updateSpectrumVector(spectrum_id, vector);
		
	}
	
	
	
	/**
	 * Update the information about a user.
	 * 
	 * @param user	the user data
	 * 
	 * @throws SPECCHIOClientException
	 */
	public void updateUser(User user) throws SPECCHIOClientException {
		
		realClient.updateUser(user);
		
	}






}
