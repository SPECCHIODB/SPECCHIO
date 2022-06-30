package ch.specchio.client;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import au.ands.org.researchdata.RDACollectionDescriptor;
import ch.specchio.interfaces.ProgressReportInterface;
import ch.specchio.plots.GonioSamplingPoints;
import ch.specchio.queries.EAVQueryConditionObject;
import ch.specchio.queries.Query;
import ch.specchio.spaces.MeasurementUnit;
import ch.specchio.spaces.ReferenceSpaceStruct;
import ch.specchio.spaces.Space;
import ch.specchio.spaces.SpectralSpace;
import ch.specchio.types.*;

/**
 * This interfaces defines all of the methods to be implemented by a SPECCHIO client.
 */
public interface SPECCHIOClient {
	
	
	/**
	 * Connect to the server.
	 * 
	 * @throws SPECCHIOClientException could not log in
	 */
	public void connect() throws SPECCHIOClientException;
	
	
	/**
	 * Close this client
	 * 
	 * @throws SPECCHIOClientException could not close client
	 */
	public void close() throws SPECCHIOClientException;

	
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
	public int copySpectrum(int spectrum_id, int target_hierarchy_id) throws SPECCHIOClientException;

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
	public  ArrayList<Integer> copySpectra(ArrayList<Integer> spectrum_id, int target_hierarchy_id, int current_hierarchy_id) throws SPECCHIOClientException;
	
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
	public int copyHierarchy(int hierarchy_id, int target_hierarchy_id, String new_name) throws SPECCHIOClientException;
		
	
	/**
	 * Clears the known metaparameter list held by the server for this user
	 */
	public void clearMetaparameterRedundancyList() throws SPECCHIOClientException;	
	
	
	/**
	 * Create a user account.
	 * 
	 * @param user	a user object describing the new user account
	 * 
	 * @return a new user object containing the complete account details
	 * 
	 * @throws SPECCHIOClientException
	 */
	public User createUserAccount(User user) throws SPECCHIOClientException;
	
	/**
	 * Create a user account on this specchio server. Only for admin users.
	 * 
	 * @param user	a user object describing the new user account
	 * 
	 * @return a new user object containing the complete account details
	 * 
	 * @throws SPECCHIOClientException
	 */
	public User createUserAccountOnCurrentServer(User user) throws SPECCHIOClientException;
		
	
	
	/**
	 * Create a new instrument.
	 * 
	 * @param name	the name of the new instrument
	 * @throws SPECCHIOClientException 
	 */
	public void createInstrument(String name) throws SPECCHIOClientException;
	
	
	/**
	 * Create a new reference.
	 * 
	 * @param name	the name of the new reference
	 * @throws SPECCHIOClientException 
	 */
	public void createReference(String name) throws SPECCHIOClientException;
	
	/**
	 * Database upgrade
	 * 
	 * @param version	DB version to be upgraded to
	 * @param fis	File input stream with SQL upgrade statements
	 * @throws SPECCHIOClientException 
	 */	
	public void dbUpgrade(double version, FileInputStream fis) throws SPECCHIOClientException;
	
	/**
	 * Delete calibration data from the database
	 * 
	 * @param calibration_id	the calibration identifier
	 * @throws SPECCHIOClientException 
	 */
	public void deleteCalibration(int calibration_id) throws SPECCHIOClientException;
	
	
	/**
	 * Delete an instrument from the database.
	 * 
	 * @param instrument_id	the instrument identifier
	 */
	public void deleteInstrument(int instrument_id) throws SPECCHIOClientException;
	
	
	/**
	 * Delete a picture of an instrument from the database.
	 * 
	 * @param picture_id	the picture identifier
	 */
	public void deleteInstrumentPicture(int picture_id) throws SPECCHIOClientException;
	
	/**
	 * Delete a reference from the database.
	 * 
	 * @param reference_id	the reference identifier
	 */
	public void deleteReference(int reference_id) throws SPECCHIOClientException;
	
	
	/**
	 * Delete a picture of a reference from the database.
	 * 
	 * @param picture_id	the picture identifier
	 */
	public void deleteReferencePicture(int picture_id) throws SPECCHIOClientException;
	
	
	/**
	 * Delete target-reference links from the database.
	 * 
	 * @param eav_id		the eav_id identifier
	 * 
	 * @return the number of links deleted
	 */
	public int deleteTargetReferenceLinks(int eav_id) throws SPECCHIOClientException;


	/**
	 * Disconnect from the server.
	 */
	public void disconnect() throws SPECCHIOClientException;
	

	/**
	 * Get the spectrum identifiers that do have a reference to the specified attribute.
	 * 
	 * @param spectrum_ids	list of ids to filter
	 * @param attribute_name	attribute name to filter with
	 * 
	 * @return an array list of spectrum identifiers that match the filter
	 */
	public ArrayList<Integer> filterSpectrumIdsByHavingAttribute(ArrayList<Integer> spectrum_ids, String attribute_name) throws SPECCHIOClientException;
	
	
	
	/**
	 * Get the spectrum identifiers that do not have a reference to the specified attribute.
	 * 
	 * @param spectrum_ids	list of ids to filter
	 * @param attribute_name	attribute name to filter with
	 * 
	 * @return an array list of spectrum identifiers that match the filter
	 */
	public ArrayList<Integer> filterSpectrumIdsByNotHavingAttribute(ArrayList<Integer> spectrum_ids, String attribute_name) throws SPECCHIOClientException;
	
	
	/**
	 * Get the spectrum identifiers that do reference to the specified attribute of a specified value.
	 * 
	 * @param spectrum_ids	list of ids to filter
	 * @param attribute_name	attribute name to filter with
	 * @param value	attribute value to match
	 * 
	 * @return an array list of spectrum identifiers that match the filter
	 */
	
	public ArrayList<Integer> filterSpectrumIdsByHavingAttributeValue(ArrayList<Integer> spectrum_ids, String attribute_name, Object value) throws SPECCHIOClientException;
	
	
	
	/**
	 * Get the metadata categories per application domain
	 * 
	 * @return an array of ApplicationDomainCategories object, or null if the information does not exist
	 */
	public ApplicationDomainCategories[] getMetadataCategoriesForApplicationDomains();
	
	
	/**
	 * Get the attributes for a metadata category.
	 * 
	 * @param category	the category name
	 * 
	 * @return an array of attribute objects
	 */
	public attribute[] getAttributesForCategory(String category) throws SPECCHIOClientException;

	/**
	 * Get the attributes object containing information on all attributes, units and categories.
	 * 
	 * @return Attributes
	 */
	public attribute[] getAttributes() throws SPECCHIOClientException;
	
	
	/**
	 * Get the attributes hashtable
	 * 
	 * @return Attributes stored in hashtable, indexed by attribute_id
	 */
	public Hashtable<Integer, attribute> getAttributesIdHash() throws SPECCHIOClientException;	
	
	/**
	 * Get the attributes hashtable
	 * 
	 * @return Attributes stored in hashtable, indexed by attribute name
	 */
	public Hashtable<String, attribute> getAttributesNameHash() throws SPECCHIOClientException;
	
	
	/**
	 * Get the units for an attribute.
	 * 
	 * @param attr	the attribute
	 * 
	 * @return a units object representing the attribute's units
	 */
	public Units getAttributeUnits(attribute attr) throws SPECCHIOClientException;
	
	/**
	 * Get a campaign descriptor.
	 * 
	 * @param campaign_id	the campaign identifier
	 * 
	 * @return a new campaign object
	 */
	public Campaign getCampaign(int campaign_id) throws SPECCHIOClientException;
	
	
	/**
	 * Get the ids and names of all metadata categories.
	 * 
	 * @return an array list of category information
	 */
	public ArrayList<Category> getCategoriesInfo() throws SPECCHIOClientException;

	/**
	 * Get the ids and names of all metadata categories used by a set of given spectra.
	 * @param spectrumIds	list of spectrum ids for which to search used categories
	 *
	 * @return an array list of category information
	 */
	public ArrayList<Category> getNonNullCategories(ArrayList<Integer> spectrumIds);

	public ArrayList<attribute> getNonNullAttributes(ArrayList<Integer> spectrumIds);

	public ArrayList<Integer> findMatchingSpectra(ArrayList<Integer> spectrumIds, ArrayList<QueryAttribute> queryAttributes);

	public void createFilterCollection(ArrayList<Integer> specturmIds, ArrayList<Integer> attributeIds);

	/**
	 * Export a campaign.
	 * 
	 * @param c		the campaign to be exported
	 * 
	 * @return an input stream connected to the exported campaign date
	 */
	public InputStream getCampaignExportInputStream(Campaign c) throws SPECCHIOClientException;
	
	
	/**
	 * Get a campaign node for the spectral data browser.
	 * 
	 * @param campaign_id		the campaign identifier
	 * @param order_by			the attribute by which to order the campaign's descendents
	 * @param restrict_to_view	show user's data only
	 * 
	 * @return a new campaign node object, or null if the campaign does not exist
	 */
	public campaign_node getCampaignNode(int campaign_id, String order_by, boolean restrict_to_view) throws SPECCHIOClientException;
	
	
	/**
	 * Get all of the campaigns in the database.
	 *
	 * @return an array of campaign objects descriving each campaign in the database
	 */
	public Campaign[] getCampaigns() throws SPECCHIOClientException;
	
	/**
	 * Get all of the campaigns in the database.
	 * 
	 * @param load_metadata_details		defines if additional detailed metadata should be loaded
	 *
	 * @return an array of campaign objects describing each campaign in the database
	 */
	public Campaign[] getCampaigns(boolean load_metadata_details) throws SPECCHIOWebClientException;	
	
	
	/**
	 * Get calibration ids for a list of spectra.
	 * 
	 * @param spectrum_ids	the spectrum identifiers
	 * 
	 * @return list of calibration ids, zero where no calibration is defined
	 */
	public ArrayList<Integer> getCalibrationIds(ArrayList<Integer> spectrum_ids) throws SPECCHIOWebClientException;	
	
	/**
	 * Get a calibrated instrument.
	 * 
	 * @param calibration_id	the calibration identifier
	 * 
	 * @return a new Instrument object, or null if the calibrated instrument does not exist
	 */
	public Instrument getCalibratedInstrument(int calibration_id) throws SPECCHIOClientException;
	
	
	/**
	 * Get the value of a capability.
	 * 
	 * @param capability	the capability name
	 * 
	 * @return the value of the capability, or null if the capability is not recognised
	 */
	public String getCapability(String capability) throws SPECCHIOClientException;
	
	
	/**
	 * Get the children of a node of the spectral data browser.
	 * 
	 * @param sn	the node
	 * 
	 * @return a list of the node's children
	 */
	public List<spectral_node_object> getChildrenOfNode(spectral_node_object sn) throws SPECCHIOClientException;
	
	
	/**
	 * Get the children of a node of a taxonomy.
	 * 
	 * @param tn	the node
	 * 
	 * @return a list of the node's children
	 */
	public List<TaxonomyNodeObject> getChildrenOfTaxonomyNode(TaxonomyNodeObject tn) throws SPECCHIOClientException;
	
	
	/**
	 * Get the list of countries known to the server.
	 * 
	 * @return an array of Country objects
	 */
	public Country[] getCountries() throws SPECCHIOClientException;
	
	
	/**
	 * Get a database node for the spectral data browser.
	 * 
	 * @param order_by			the attribute to order by
	 * @param restrict_to_view	display the current user's data only
	 * 
	 * @return a database_node object
	 */
	public database_node getDatabaseNode(String order_by, boolean restrict_to_view) throws SPECCHIOClientException;
	
	
	/**
	 * Get the direct hierarchy id of a single spectrum
	 * 
	 * @param spectrum_id		the identifier of the spectrum
	 * 
	 * @return hierarchy id
	 * 
	 * @throws SPECCHIOClientException
	 */	
	public Integer getDirectHierarchyId(int spectrum_id) throws SPECCHIOClientException;			
	
	
	
	/**
	 * Get hierarchy ids, directly above these spectra
	 * 
	 * @param spectrum_ids		the identifiers of the desired spectra
	 * 
	 * @return hierarchy ids
	 * 
	 * @throws SPECCHIOClientException
	 */	
	public ArrayList<Integer> getDirectHierarchyIds(ArrayList<Integer> spectrum_ids) throws SPECCHIOClientException;			
	
	
	
	/**
	 * Get distinct values of an attribute
	 * 
	 * @param attribute_id	id of the required attribute
	 * 
	 * @return arraylist of metaparameters	
	 */
	public ArrayList<MetaParameter> getDistinctValuesOfAttribute(int attribute_id) throws SPECCHIOClientException;
	
	
	/**
	 * Get a conflicts in the EAV metadata for a set of spectra.
	 * 
	 * @param metadata_level		storage level identifier
	 * @param ids	the primary entity identifiers
	 * 
	 * @return a ConflictTable object containing all of the conflicts
	 */
	public ConflictTable getEavMetadataConflicts(int metadata_level, ArrayList<Integer> ids) throws SPECCHIOClientException;

	
	/**
	 * Return an EAVQueryConditionObject configured for the supplied attribute.
	 * 
	 * @param attr	attribute object
	 * @return 
	 * 
	 * @return EAVQueryConditionObject
	 */	
	public EAVQueryConditionObject getEAVQueryConditionObject(attribute attr);
	
	/**
	 * Get the count of existing metaparameters for the supplied spectrum ids and attribute id
	 * 
	 * @param attribute_id	id of the attribute
	 * @param ids	spectrum ids
	 * 
	 * @return Integer
	 */
	
	public Integer getExistingMetaparameterCount(Integer attribute_id, ArrayList<Integer> ids) throws SPECCHIOClientException;
	
	/**
	 * Get the file format identifier for a file format name.
	 * 
	 * @param format	the file format name
	 * 
	 * @return the identifier associated with the specified file format, or -1 if file format is not recognised
	 */
	public int getFileFormatId(String format) throws SPECCHIOClientException;
	
	
	/**
	 * Get the hierarchy object for a given hierarchy_id
	 * 
	 * @param hierarchy_id	the hierarchy_id identifying the required node
	 * 
	 * @return the hierarchy object, or -1 if the node does not exist
	 */	
	public Hierarchy getHierarchy(Integer hierarchy_id) throws SPECCHIOWebClientException ;
	
	
	/**
	 * Get the identifier of a hierarchy node.
	 * 
	 * @param campaign	the campaign in which the node is located
	 * @param name		the name of the node
	 * @param parent_id	the parent of the node
	 * 
	 * @return the identifier of the child of parent_id with the given name, or -1 if the node does not exist
	 */
	public int getHierarchyId(Campaign campaign, String name, int parent_id) throws SPECCHIOClientException;

	
	/**
	 * Get a list of hierarchy ids, covering all hierarchies above these spectra
	 * 
	 * @param spectrum_ids		the identifiers of the desired spectra
	 * 
	 * @return hierarchy ids
	 * 
	 * @throws SPECCHIOClientException
	 */	
	public ArrayList<Integer> getHierarchyIdsOfSpectra(ArrayList<Integer> spectrum_ids) throws SPECCHIOClientException;				
	
	
	/**
	 * Get the parent_id for a given hierarchy_id
	 * 
	 * @param hierarchy_id	the hierarchy_id identifying the required node
	 * 
	 * @return id of the parent of given hierarchy
	 */
	public int getHierarchyParentId(int hierarchy_id) throws SPECCHIOClientException;
		
	/**
	 * Get the file path of a hierarchy.
	 * 
	 * @param hierarchy_id		the identifier of the hierarchy
	 * 
	 * @returns path as string
	 * 
	 * @throws SPECCHIOClientException	the database could not accessed
	 */
	public String getHierarchyFilePath(int hierarchy_id) throws SPECCHIOClientException;
	
	/**
	 * Get the name of a hierarchy.
	 * 
	 * @param hierarchy_id		the identifier of the hierarchy
	 * 
	 * @returns name as string
	 * 
	 * @throws SPECCHIOClientException	the database could not accessed
	 */	
	public String getHierarchyName(int hierarchy_id) throws SPECCHIOClientException;	
	
	
	/**
	 * Get all of the institutes in the database.
	 * 
	 * @rerturn an array of Institute objects
	 */
	public Institute[] getInstitutes() throws SPECCHIOClientException;
	
	
	/**
	 * Get an instrument.
	 * 
	 * @param instrument_id	the instrument identifier
	 * 
	 * @return a new Instrument object, or null if the instrument does not exist
	 */
	public Instrument getInstrument(int instrument_id) throws SPECCHIOClientException;
	
	
	/**
	 * Get an instrument that matches the passed object or creates a new instrument and returns it
	 * 
	 * @param instrument instance
	 * 
	 * @return a new Instrument object
	 */
	public Instrument getInstrument(Instrument instrument) throws SPECCHIOWebClientException;

	
	/**
	 * Get instrument ids for a list of spectra.
	 * 
	 * @param spectrum_ids	the spectrum identifiers
	 * 
	 * @return list of instrument ids, zero where no instrument is defined
	 */
	public ArrayList<Integer> getInstrumentIds(ArrayList<Integer> spectrum_ids) throws SPECCHIOWebClientException;	
	
	
	/**
	 * Get the calibration metadata for an instrument.
	 * 
	 * @param instrument_id	the instrument identifier
	 * 
	 * @return an array of CalibrationMetadata objects, or null if the instrument does not exist
	 */
	public CalibrationMetadata[] getInstrumentCalibrationMetadata(int instrument_id) throws SPECCHIOClientException;
	
	
	/**
	 * Get descriptors for all of the instruments in the database.
	 * 
	 * @return an array of InstrumentDescriptor objects
	 */
	public InstrumentDescriptor[] getInstrumentDescriptors() throws SPECCHIOClientException;
	
	
	/**
	 * Get a instrument object for a given spectral file object.
	 * 
	 * @param spec_file		the spectral file
	 * 
	 * @return a new Instrument object
	 */	
	public Instrument getInstrumentForSpectralFile(SpectralFile spec_file) throws SPECCHIOClientException;
		
	
	/**
	 * Get all of the pictures for an instrument.
	 * 
	 * @param instrument_id	the instrument identifier
	 * 
	 * @return a PictureTable object containing all of the pictures for this instrument, or null if the instrument doesn't exist
	 */
	public PictureTable getInstrumentPictures(int instrument_id) throws SPECCHIOClientException;
	
	
	/**
	 * Get a user object representing the user under which the client is logged in.
	 * 
	 * @return a reference to a user object, or null if the client is not connected
	 */
	public User getLoggedInUser();
	
	
	/**
	 * Get the metadata categories for application domain
	 * 
	 * @param taxonomy_id	the field name
	 * 
	 * @return a ArrayList<Integer> object, or null if the field does not exist
	 */
	public ArrayList<Integer> getMetadataCategoriesForApplicationDomain(int taxonomy_id) throws SPECCHIOClientException;
	
	
	/**
	 * Get the metadata categories for a metadata field, ready for access via ID
	 * 
	 * @param field	the field name
	 * 
	 * @return a CategoryTable object, or null if the field does not exist
	 */
	public CategoryTable getMetadataCategoriesForIdAccess(String field) throws SPECCHIOClientException;

	
	/**
	 * Get the metadata categories for a metadata field, ready for access via name
	 * 
	 * @param field	the field name
	 * 
	 * @return a CategoryTable object, or null if the field does not exist
	 */
	public Hashtable<String, Integer> getMetadataCategoriesForNameAccess(String field) throws SPECCHIOClientException;
	
	/**
	 * Get metaparameters for spectrum ids and EAV attribute
	 * 
	 * @param ids		spectrum ids
	 * @param attribute_name		attribute name
	 * 
	 * @return list of metaparameters, or null if the field does not exist
	 * @throws SPECCHIOClientException
	 */
	public ArrayList<MetaParameter> getMetaparameters(ArrayList<Integer> ids, String attribute_name) throws SPECCHIOClientException;


	/**
	 * Get metaparameters for spectrum ids and EAV attribute
	 *
	 * @param spectrumIds		spectrum ids
	 *
	 * @throws SPECCHIOClientException
	 */
	public void calculateSunAngle(ArrayList<Integer> spectrumIds, SPECCHIOClient client) throws SPECCHIOClientException;

	/**
	 * Get metaparameter for spectrum id and EAV attribute
	 * 
	 * @param id		spectrum id
	 * @param attribute_name		attribute name
	 * 
	 * @return metaparameter, or null if the field does not exist	 
	 */
	public MetaParameter getMetaparameter(Integer id, String attribute_name) throws SPECCHIOWebClientException;
	
	/**
	 * Get list of metaparameters for spectrum ids and EAV attributes
	 * 
	 * @param ids		spectrum ids
	 * @param attribute_ids		list of attribute ids
	 * 
	 * @return list of list of metaparameters, or null if the field does not exist	 
	 */
	public ArrayList<ArrayList<MetaParameter>> getMetaparameters(ArrayList<Integer> ids, ArrayList<Integer> attribute_ids) throws SPECCHIOWebClientException;
	
	/**
	 * Get metaparameters for spectrum ids and EAV attribute
	 * 
	 * @param ids		spectrum ids
	 * @param attribute_name		attribute name
	 * @param distinct		defines if distinct values should be returned or repeated values for the given spectrum ids: distinct = false means parameters for all spectra, even if redundant
	 * 
	 * @return list of metaparameters, or null if the field does not exist	 
	 */
	public ArrayList<MetaParameter> getMetaparameters(ArrayList<Integer> ids, String attribute_name, Boolean distinct) throws SPECCHIOWebClientException;
	
	
	/**
	 * Get metaparameter value statistics for spectrum ids and EAV attribute
	 * 
	 * @param ids		spectrum ids
	 * @param attribute_name		attribute name
	 * 
	 * @return structure with Metaparameter holding statistic values	 
	 */
	public MetaparameterStatistics getMetaparameterStatistics(ArrayList<Integer> ids, String attribute_name) throws SPECCHIOWebClientException;
	
	
	/**
	 * Get values for spectrum ids and EAV attribute (non-distinct values by default)
	 * 
	 * @param ids		spectrum ids
	 * @param attribute_name		attribute name
	 * 
	 * @return list of values, or null if the field does not exist
	 * @throws SPECCHIOWebClientException
	 */	
	public MatlabAdaptedArrayList<Object> getMetaparameterValues(ArrayList<Integer> ids, String attribute_name) throws SPECCHIOWebClientException;	
	
	/**
	 * Get values for spectrum ids and EAV attribute
	 * 
	 * @param ids		spectrum ids
	 * @param attribute_name		attribute name
	 * @param distinct		defines if distinct values should be returned or repeated values for the given spectrum ids
	 * 
	 * @return list of values, or null if the field does not exist
	 * @throws SPECCHIOWebClientException
	 */
	public MatlabAdaptedArrayList<Object> getMetaparameterValues(ArrayList<Integer> ids, String attribute_name, Boolean distinct) throws SPECCHIOWebClientException;
	
	
	/**
	 * Get the metadata conflicts for a set of spectra and set of fields.
	 * 
	 * @param spectrum_ids	the spectrum identifiers
	 * @param fields		the fields to check
	 * 
	 * @return a ConflictTable object listing the conflicts found
	 */
	public ConflictTable getMetadataConflicts(ArrayList<Integer> spectrum_ids, String fields[]) throws SPECCHIOClientException;
	
	/**
	 * Get measurement unit for ASD based coding.
	 * 
	 * @param coding	coding based on ASD coding
	 * 
	 * @return a new MeasurementUnit object, or null if the coding does not exist
	 */	
	public MeasurementUnit getMeasurementUnitFromCoding(int coding) throws SPECCHIOWebClientException;
	
	
	/**
	 * Get newest N spectra.
	 * 
	 * @param number_of_spectra
	 * 
	 * @return list of spectrum ids ordered by data ingestion time
	 * @throws SPECCHIOWebClientException
	 */	
	public ArrayList<Integer> getNewestSpectra(int number_of_spectra) throws SPECCHIOWebClientException;
	
	
	/**
	 * Get the data usage policies for a space.
	 * 
	 * @param space	the space
	 * 
	 * @return an array of Policy objects
	 */
	public String[] getPoliciesForSpace(Space space) throws SPECCHIOClientException;
	
	
	/**
	 * Get an empty query object
	 * 
	 * @return a Query Object
	 */
	public Query getQueryObject() throws SPECCHIOClientException;
	
	
	/**
	 * Get a reference.
	 * 
	 * @param reference_id	the reference identifier
	 * 
	 * @return a new Reference object, or null if the instrument does not exist
	 */
	public Reference getReference(int reference_id) throws SPECCHIOClientException;
	
	
	/**
	 * Get the calibration metadata for a reference.
	 * 
	 * @param reference_id	the reference identifier
	 * 
	 * @return an array of CalibrationMetadata objects, or null if the reference does note exist
	 */
	public CalibrationMetadata[] getReferenceCalibrationMetadata(int reference_id) throws SPECCHIOClientException;
	
	
	/**
	 * Get descriptors for all of the references in the database.
	 * 
	 * @return an arra of ReferenceDescriptor objects
	 */
	public ReferenceDescriptor[] getReferenceDescriptors() throws SPECCHIOClientException;
	
	
	/**
	 * Get all of the reference brands in the database.
	 * 
	 * @return an array of ReferenceBrand objects
	 */
	public ReferenceBrand[] getReferenceBrands() throws SPECCHIOClientException;
	
	
	/**
	 * Get all of the pictures associated with a reference.
	 * 
	 * @param reference_id	the reference identifier
	 * 
	 * @return a PictureTable containing all of the pictures
	 */
	public PictureTable getReferencePictures(int reference_id) throws SPECCHIOClientException;
	
	
	/**
	 * Get a reference space.
	 * 
	 * @param input_ids
	 * 
	 * @return a Space object, or null if no space could be found
	 * @throws SPECCHIOClientException
	 */
	public ReferenceSpaceStruct getReferenceSpace(ArrayList<Integer> input_ids) throws SPECCHIOClientException;
	
	
	/**
	 * Get a server descriptor that describes the server to which this client is connected.
	 * 
	 * @return a new server descriptor object
	 */
	public SPECCHIOServerDescriptor getServerDescriptor();
	
	
	/**
	 * Get all of the sensors in the database.
	 * 
	 * @return an array of Sensor objects
	 */
	public Sensor[] getSensors() throws SPECCHIOClientException;
	
	
	/**
	 * Get sensor sampling geometry 
	 * 
	 * @param space		holds spectra ids of which geometry is to retrieved
	 * 
	 * @return a new GonioSamplingPoints, or null if no geometries were found
	 * @throws SPECCHIOWebClientException 
	 */		
	public GonioSamplingPoints getSensorSamplingGeometry(SpectralSpace space) throws SPECCHIOWebClientException;
	

	/**
	 * Get the space objects for a set of spectrum identifiers.
	 * 
	 * @param ids								the spectrum identifiers
	 * @param split_spaces_by_sensor			boolean
	 * @param split_spaces_by_sensor_and_unit	boolean
	 * @param order_by							the field to order by
	 */
	public Space[] getSpaces(ArrayList<Integer> ids, boolean split_spaces_by_sensor, boolean split_spaces_by_sensor_and_unit, String order_by) throws SPECCHIOClientException;
	
	/**
	 * Get the space objects for a set of spectrum identifiers.
	 * Configured to sort by sensor, unit, instrument and calibration
	 * 
	 * @param ids								the spectrum identifiers
	 * @param order_by							the field to order by
	 */
	public Space[] getSpaces(ArrayList<Integer> ids, String order_by) throws SPECCHIOWebClientException;
	
	/**
	 * Get a spectrum.
	 * 
	 * @param spectrum_id	the spectrum identifier
	 * @param load_metadata	load all spectrum metadata?
	 * 
	 * @return a Spectrum object
	 */
	public Spectrum getSpectrum(int spectrum_id, boolean load_metadata) throws SPECCHIOClientException;
	
	
	/**
	 * Get the number of spectra in the database
	 * 
	 * @return the number of spectra in the database
	 */
	public int getSpectrumCountInDB() throws SPECCHIOClientException;
	
	
	/**
	 * Get the calibration spaces for a set of spectra.
	 * 
	 * @param spectrum_ids	the spectrum identifiers
	 * 
	 * @return an array of Space objects
	 */
	public Space[] getSpectrumCalibrationSpaces(ArrayList<Integer> spectrum_ids) throws SPECCHIOClientException;
	
	
	/**
	 * Get the spectrum factor table for a set of spectra.
	 * 
	 * @param spectrum_ids_1
	 * @param spectrum_ids_2
	 * 
	 * @return a SpectrumFactorTable object
	 */
	public SpectrumFactorTable getSpectrumFactorTable(ArrayList<Integer> spectrum_ids_1, ArrayList<Integer> spectrum_ids_2) throws SPECCHIOClientException;
	
	
	/**
	 * Get the identifiers of all spectra beneath a given node of the spectral data browser
	 * 
	 * @param sn	the node
	 * 
	 * @return a list of spectrum identifiers
	 */
	public List<Integer> getSpectrumIdsForNode(spectral_node_object sn) throws SPECCHIOClientException;
	
	
	/**
	 * Get the identifiers of all spectra that match a full text search.
	 * 
	 * @param search_str		the search string
	 * 
	 * @return an array list of spectrum identifiers
	 */
	public ArrayList<Integer> getSpectrumIdsMatchingFullTextSearch(String search_str) throws SPECCHIOClientException;

	/**
	 * Get the identifiers of all spectra that match a full text search using hierarchy.
	 * 
	 * @param search_str		the search string
	 * 
	 * @return an array list of spectrum identifiers
	 */
	public ArrayList<Integer> getSpectrumIdsMatchingFullTextSearchUsingHierarchy(String search_str) throws SPECCHIOClientException;

	
	/**
	 * Get the identifiers of all spectra that match a full text search.
	 *
	 * @param campaignId		the search string
	 *
	 * @return an array list of spectrum identifiers
	 */
	public ArrayList<Integer> getUnprocessedHierarchies(String campaignId) throws SPECCHIOClientException;

	/**
	 * Get the identifiers of all spectra that match a full text search.
	 *
	 * @param campaignId		the search string
	 *
	 * @return an array list of spectrum identifiers
	 */
	public ArrayList<Integer> getIrradiance(String campaignId) throws SPECCHIOClientException;


	/**
	 * Get the spectrum identifiers that match a given query.
	 * 
	 * @param query	the query
	 * 
	 * @return an array list of spectrum identifiers that match the query
	 */
	public ArrayList<Integer> getSpectrumIdsMatchingQuery(Query query) throws SPECCHIOClientException;
	
	
	/**
	 * Get the identifiers of all spectra directly belonging to a hierarchy
	 * 
	 * @param hierarchy_id	the hierarchy_id
	 * 
	 * @return a list of spectrum identifiers
	 */
	public ArrayList<Integer> getDirectSpectrumIdsOfHierarchy(int hierarchy_id) throws SPECCHIOClientException;

	
	/**
	 * Get the pictures associated with a spectrum.
	 * 
	 * @param spectrum_id	the spectrum identifier
	 * 
	 * @return a PictureTable containing all of the pictures associated with the spectrum
	 */
	public PictureTable getSpectrumPictures(int spectrum_id) throws SPECCHIOClientException;
	
	
	/**
	 * Get the number of spectra that match a given query.
	 * 
	 * @param query	the query
	 * 
	 * @return the number of spectra that match the query
	 */
	public int getSpectrumQueryCount(Query query) throws SPECCHIOClientException;
	
	
	/**
	 * Get the spectrum data links that refer to a given set of targets and/or references.
	 * 
	 * @param target_ids	the identifiers of the target spectra (null or empty to match all targets)
	 * @param reference_ids	the identifiers of the reference spectra (null or empty to match all references)
	 * 
	 * @returns an array of SpectrumDataLink objects
	 */
	public SpectrumDataLink[] getTargetReferenceLinks(ArrayList<Integer> target_ids, ArrayList<Integer> reference_ids) throws SPECCHIOClientException;
	
	
	/**
	 * Get the identifier of a sub-hierarchy with a given name, creating the
	 * hierarchy if it doesn't exist (deprecated)
	 * 
	 * @param campaign	the campaign into which to insert the hierarchy
	 * @param parent_id			the identifier of the the parent of the hierarchy
	 * @param name	the name of the desired hierarchy
	 * 
	 * @return the identifier of the child of parent_id with the name hierarchy_name
	 * @throws SPECCHIOClientException
	 */
	public int getSubHierarchyId(Campaign campaign, String name, int parent_id) throws SPECCHIOClientException;
	
	/**
	 * Get the identifier of a sub-hierarchy with a given name, creating the
	 * hierarchy if it doesn't exist.
	 * 
	 * @param name	the name of the desired hierarchy
	 * @param parent_id			the identifier of the the parent of the hierarchy	 
	 * 
	 * @return the identifier of the child of parent_id with the name hierarchy_name
	 */
	public int getSubHierarchyId(String name, int parent_id) throws SPECCHIOClientException;
	
	
		
	/**
	 * Get the top node for a given taxonomy
	 * 
	 * @param attribute_id	id of the attribute that defines the taxonomy
	 */
	public TaxonomyNodeObject getTaxonomyRootNode(int attribute_id) throws SPECCHIOClientException;
	
	
	/**
	 * Get the node for a given taxonomy
	 * 
	 * @param taxonomy_id	taxonomy_id that defines the taxonomy
	 */
	public TaxonomyNodeObject getTaxonomyNode(int taxonomy_id) throws SPECCHIOClientException;
	
	/**
	 * Get the id for a given taxonomy node in a given taxonomy
	 * 
	 * @param attribute_id	attribute_id that defines the taxonomy
	 *  @param name		name of the node of which the id is required
	 * 
	 */
	public int getTaxonomyId(int attribute_id, String name)  throws SPECCHIOClientException;

	
	/**
	 * Get the taxonomy hash for a given taxonomy
	 * 
	 * @param attribute_id	attribute_id that defines the taxonomy
	 * 
	 */
	public Hashtable<String, Integer> getTaxonomyHash(int attribute_id)  throws SPECCHIOClientException;
		
	
	/**
	 * Get the ID to name hash for a given taxonomy
	 * 
	 * @param attribute_id	attribute_id that defines the taxonomy
	 * 
	 */
	public Hashtable<Integer, String> getTaxonomyIdToNameHash(int attribute_id)  throws SPECCHIOClientException;
	
	/**
	 * 
	 * Get details of an uncertainty set
	 * 
	 * @param uncertainty_set_id, the desired uncertainty_set_id of the uncertainty set
	 * 
	 */
	
	public SpectralSet getUncertaintySetOld(int uncertainty_set_id) throws SPECCHIOClientException;
	
	/**
	 * 
	 * Get details of an uncertainty set
	 * 
	 * @param uncertainty_set_id, the desired uncertainty_set_id of the uncertainty set
	 * 
	 */
	
	public UncertaintySet getUncertaintySet(int uncertainty_set_id) throws SPECCHIOClientException;
	
	
	/**
	 * Get a list of all of the users in the database.
	 * 
	 * @return an array of User objects
	 * 
	 * @throws SPECCHIOClientException
	 */
	public User[] getUsers() throws SPECCHIOClientException;
	
	
	/**
	 * Get a list of all of the users in the database with added user statistics (number of loaded spectra, number of campaigns).
	 * 
	 * @return an array of User objects
	 * 
	 * @throws SPECCHIOClientException
	 */
	public User[] getUsersWithStatistics() throws SPECCHIOWebClientException;	
	
	
	/**
	 * Import a campaign.
	 * 
	 * @param user_id	the identifier of the user to whom the campaign will belong
	 * @param is	the input stream from which to read the campaign
	 */
	public void importCampaign(int user_id, InputStream is) throws SPECCHIOClientException;

	/**
	 * Import a campaign from a file that is on the Glassfish server (used for bigger files to prevent timeouts)
	 * 
	 * @param user_id	the identifier of the user to whom the campaign will belong
	 * @param server_filepath		the server filepath from which to read the campaign
	 */
	public void importCampaign(int user_id, String server_filepath) throws SPECCHIOWebClientException;


	/**
	 * Insert a new campaign into the database
	 * 
	 * @param campaign	the campaign
	 * 
	 * @return the identifier of the new campaign
	 */
	public int insertCampaign(Campaign campaign) throws SPECCHIOClientException;
	
	
	/**
	 * Insert a hierarchy node.
	 * 
	 * @param campaign	the campaign into which to insert the hierarchy
	 * @param name		the name of the new hierarchy
	 * @param parent_id	the identifier of the parent of the new hierarchy
	 * 
	 * @return the identifier of the new hierarchy node
	 */
	public int insertHierarchy(Campaign campaign, String name, int parent_id) throws SPECCHIOClientException;
	
	
	/**
	 * Insert a new institute into the database
	 * 
	 * @param institute	an Instite object describing the new institute
	 * 
	 * @return the identifier of the new institute
	 */
	public int insertInstitute(Institute institute) throws SPECCHIOClientException;
	
	
	/**
	 * Insert calibration for an instrument.
	 * 
	 * @param c		the calibration data
	 */
	public void insertInstrumentCalibration(Calibration c) throws SPECCHIOClientException;
	
	
	/**
	 * Insert a picture of an instrument into the database.
	 * 
	 * @param picture	the picture
	 */
	public void insertInstrumentPicture(Picture picture) throws SPECCHIOClientException;
	
	
	/**
	 * Insert calibration for a reference into the database.
	 * 
	 * @param c		the calibration data
	 */
	public void insertReferenceCalibration(Calibration c) throws SPECCHIOClientException;
	
	
	/**
	 * Insert a picture associated with a reference into the database.
	 * 
	 * @param picture	the picture
	 */
	public void insertReferencePicture(Picture picture) throws SPECCHIOClientException;
	
	
	/**
	 * Insert a spectral file into the database.
	 * 
	 * @param spec_file	the file
	 * 
	 * @return a list of spectrum identifiers that were inserted into the database
	 */
	public SpectralFileInsertResult insertSpectralFile(SpectralFile spec_file) throws SPECCHIOClientException;
	
	
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
	public int insertClosestTargetReferenceLink(int target_id, ArrayList<Integer> reference_ids) throws SPECCHIOClientException;

	/**
	 * 
	 * Insert a spectrum subset
	 * 
	 * @param spectral_subset a spectrum subset
	 * 
	 * @return the id of the created spectrum subset
	 * 
	 * 
	 */
	
	
	public int insertSpectrumSubsetOld(SpectralSet spectral_set) throws SPECCHIOClientException;

	/**
	 * 
	 * Insert a spectrum subset
	 * 
	 * @param spectrum node a spectrum node 
	 * 
	 * @return the id of the created spectrum subset
	 * 
	 * 
	 */
	
	
	public int insertSpectrumSubset(UncertaintySpectrumNode uc_spectrum_node) throws SPECCHIOClientException;

	
	/**
	 * 
	 * Insert an uncertainty node
	 * 
	 * @param uc_spectrum_node an uncertainty node of type spectrum
	 * 
	 * @return the id of the created node
	 * 
	 * 
	 */
	
	
	public int insertUncertaintyNode(UncertaintySpectrumNode uc_spectrum_node, int uc_set_id) throws SPECCHIOClientException;

	
	/**
	 * 
	 * Insert an uncertainty node
	 * 
	 * @param uc_instrument_node an uncertainty node of type instrument
	 * 
	 * @return the id of the created node
	 * 
	 * 
	 */
	
	
	public int insertUncertaintyNode(UncertaintyInstrumentNode uc_instrument_node, int uc_set_id) throws SPECCHIOClientException;

	
	/**
	 * 
	 * Insert an uncertainty node
	 * 
	 * @param spectral_set a spectral set
	 * 
	 * @return the id of the created node
	 * 
	 * 
	 */
	
	
	public int insertUncertaintyNodeOld(SpectralSet spectral_set) throws SPECCHIOClientException;

	
	/**
	 * 
	 * Insert new uncertainty set
	 * 
	 * @param uc_set
	 * 
	 * @return the id of the created set
	 * 
	 */
	
	public int insertNewUncertaintySet(UncertaintySet uc_set) throws SPECCHIOClientException;	
	
	
	/**
	 * 
	 * Insert new uncertainty set
	 * 
	 * @param spectral_set
	 * 
	 * @return the id of the created set
	 * 
	 */
	
	public int insertNewUncertaintySetOld(SpectralSet spectral_set) throws SPECCHIOClientException;	
	


	/**
	 * Get the adjacency matrix for a given uncertainty set
	 * 
     * @param uncertainty_set_id	the uncertainty set identifier
	 * 
	 * @return an array of integers of the adjacency matrix
	 */
		public AdjacencyMatrix getAdjacencyMatrix(int uncertainty_set_id) throws SPECCHIOClientException;


	 /**
	  * Get an edge value for a given edge id
	  * 
	  * @param edge_id
	  * 
	  * @return an edge value
	  * 
	  */
		
	public String getEdgeValue(int edge_id) throws SPECCHIOClientException;	
		
		
	/**
	 * 
	 * Get instrument node for a selected instrument id
	 * 
	 * @param an instrument node id
	 * 
	 * @return an InstrumentNode
	 * 
	 */
	
	
	public InstrumentNode getInstrumentNode(int instrument_node_id) throws SPECCHIOClientException;
	
	/**
	 * Insert an instrument node into the database.
	 * 
	 * @param InstrumentNode type
	 * 
	 * @return an instrument_node_id
	 * 
	 * @throws SPECCHIOClientException
	 */
	
	public int insertInstrumentNode(InstrumentNode instrument_node) throws SPECCHIOClientException;

	/**
	 * 
	 * Get spectrum node for a selected spectrum node id
	 * 
	 * @param a spectrum node id
	 * 
	 * @return an InstrumentNode
	 * 
	 */
	
	
	public InstrumentNode getSpectrumNode(int spectrum_node_id) throws SPECCHIOClientException;

	
	/**
	 * Insert a target-reference links.
	 * 
	 * @param target_id		the identifier of the target node
	 * @param reference_ids	the identifiers of the reference nodes
	 * 
	 * @return the number of links sucessfully created
	 * 
	 * @throws SPECCHIOClientException
	 */
	public int insertTargetReferenceLinks(int target_id, ArrayList<Integer> reference_ids) throws SPECCHIOClientException;

	
	/**
	 * Test for the existence of a calibration in the database.
	 * 
	 * @param cal		calibration object to check
	 * 
	 * @return true if the calibration already exists in the database, false otherwise
	 */
	 public boolean instrumentCalibrationExists(Calibration cal) throws SPECCHIOWebClientException;	

	/**
	 * Test whether or not the client is logged in under a given role.
	 * 
	 * @param roleName	the role to be tested
	 * 
	 * @return "true" if the client is logged in as a user with role roleIn, "false" otherwise
	 */
	public boolean isLoggedInWithRole(String roleName) throws SPECCHIOClientException;

	
	/**
	 * Get the meta-parameter of the given metaparameter identifier.
	 * 
	 * @param metaparameter_id		the metaparameter identifier for which to retrieve metadata
	 * 
	 * @return the meta-parameter object corresponding to the desired id
	 *
	 * @throws SPECCHIOClientException
	 */
	public MetaParameter loadMetaparameter(int metaparameter_id) throws SPECCHIOClientException;

	
	/**
	 * Load a sensor definition into the database from an input stream.
	 * 
	 * @param is	the input stream
	 */
	public void loadSensor(InputStream is) throws SPECCHIOClientException;
	
	
	/**
	 * Load a Space object.
	 * 
	 * @param space	a partially-filled space object
	 * 
	 * @return a complete Space object
	 */
	public Space loadSpace(Space space) throws SPECCHIOClientException;


	/**
	 * Move a hierarchy to a new parent hierarchy within the same campaign. If a hierarchy of the same name exists in the target hierarchy then the hierarchies are merged.
	 * 
	 * @param source_hierarchy_id	hierarchy id of the hierarchy to move
	 * @param target_parent_hierarchy	hierarchy id of the new parent hierarchy
	 * 
	 * return true if move was done
	 */
	public boolean moveHierarchy(int source_hierarchy_id, int target_parent_hierarchy) throws SPECCHIOClientException;
	
	
	
	/**
	 * Causes the client to reload data values the specified category upon next request.
	 * 
	 * @param field name
	 * 
	 */	
	public void refreshMetadataCategory(String field);
	
	/**
	 * Remove an item of EAV metadata.
	 * 
	 * @param mp	the meta-parameter to be removed
	 */
	public void removeEavMetadata(MetaParameter mp) throws SPECCHIOClientException;
	
	
	/**
	 * Remove an item of EAV metadata for a collection of spectra.
	 * 
	 * @param mp			the meta-parameter to be removed
	 * @param spectrum_ids	the spectrum identifiers
	 */
	public void removeEavMetadata(MetaParameter mp, ArrayList<Integer> spectrum_ids) throws SPECCHIOClientException;

	/**
	 * Remove one or more items of EAV metadata for a collection of spectra for a defined attribute.
	 * 
	 * @param attr			the attribute to be removed
	 * @param spectrum_ids	the spectrum identifiers
	 * @param metadata_level		storage level identifier
	 */	
	public void removeEavMetadata(attribute attr, ArrayList<Integer> spectrum_ids, int metadata_level) throws SPECCHIOClientException;
	
	
	
	
	/**
	 * Remove the data corresponding to a node of the spectral data browser.
	 * 
	 * @param sn	the node to be removed
	 */
	public void removeSpectralNode(spectral_node_object sn) throws SPECCHIOClientException;
	
	/**
	 * Remove the data corresponding to a node of the spectral data browser.
	 * 
	 * @param sns	list of nodes to be removed
	 */
	public void removeSpectralNodes(ArrayList<spectral_node_object> sns) throws SPECCHIOWebClientException;
	
	
	
	/**
	 * Rename a hierarchy in the database and also on the file system if path is accessible.
	 * The rename on the database will only be applied if a rename on the file system succeeded.
	 * 
	 * @param hierarchy_id	id of the hierarchy to be renamed
	 * @param name	new name of the hierarchy
	 * @return true if and only if the renaming succeeded; false otherwise
	 */	
	public boolean renameHierarchy(int hierarchy_id, String name) throws SPECCHIOClientException;	
	
	/**
	 * Set the progress report interface to which progress made by this
	 * client will be reported.
	 * 
	 * @param pr	the progress report; use null to report no progress
	 */
	public void setProgressReport(ProgressReportInterface pr);

	
	/**
	 * Sort spectra by the values of the specified attributes
	 * 
	 * @param spectrum_ids	list of ids to sort
	 * @param attribute_names	attribute names to sort by
	 * 
	 * @return a AVMatchingListCollection object
	 */
	public AVMatchingListCollection sortByAttributes(ArrayList<Integer> spectrum_ids, String... attribute_names) throws SPECCHIOClientException;
	
	
	/**
	 * Test for the existence of a spectral file in the database.
	 * 
	 * @param spec_file		spectral file object to check
	 * 
	 * @return true if the file already exists in the database, false otherwise
	 */
	public boolean spectralFileExists(SpectralFile spec_file) throws SPECCHIOClientException;
	
	/**
	 * Test for the existence of a spectral files in the database.
	 * 
	 * @param spec_files	container with arraylist of spectral files to check
	 * 
	 * @return array of boolean values indicating existence
	 */
	public boolean[] spectralFilesExist(SpectralFiles spec_files) throws SPECCHIOClientException;	
	
	
	/**
	 * Submit a collection to Research Data Australia.
	 * 
	 * @param collection_d	the collection descriptor
	 * 
	 * @return the collection identifier of the new collection, or an empty string if publication failed
	 * 
	 * @throws SPECCHIOClientException	could not contact the server
	 */
	public String submitRDACollection(RDACollectionDescriptor collection_d) throws SPECCHIOClientException;
		
	
	/**
	 * Update the information about a campaign
	 * 
	 * @param campaign	the new campaign data
	 */
	public void updateCampaign(Campaign campaign) throws SPECCHIOClientException;
	
	
	/**
	 * Update EAV metadata.
	 * 
	 * @param mp			the meta-parameter to update
	 * @param spectrum_ids	the identifiers for which to update the parameter
	 * 
	 * @return the identifier of the inserted metadata
	 */
	public int updateEavMetadata(MetaParameter mp, ArrayList<Integer> spectrum_ids) throws SPECCHIOClientException;
	
	
	/**
	 * Update EAV metadata.
	 * 
	 * @param mp			the meta-parameter to update
	 * @param spectrum_ids	the identifiers for which to update the parameter
	 * @param mp_old		the old meta-parameter
	 * 
	 * @return the identifier of the inserted metadata
	 */
	public int updateEavMetadata(MetaParameter mp, ArrayList<Integer> spectrum_ids, MetaParameter mp_old) throws SPECCHIOClientException;
	
	/**
	 * Update EAV metadata annotation.
	 * 
	 * @param mp			the meta-parameter to update
	 * @param spectrum_ids	the identifiers for which to update the parameter
	 * 
	 * @return the identifier of the inserted metadata
	 */
	public int updateEavMetadataAnnotation(MetaParameter mp, ArrayList<Integer> spectrum_ids) throws SPECCHIOWebClientException;	
	
	/**
	 * Update or insert EAV metadata. Will automatically update existing entries or insert a new metaparameter if not existing.
	 * 
	 * @param mp			the meta-parameter to update or insert
	 * @param spectrum_ids	the identifiers for which to update or insert the parameter
	 * 
	 * @return the identifier of the inserted or updated metadata
	 */
	public int updateOrInsertEavMetadata(MetaParameter mp, ArrayList<Integer> spectrum_ids) throws SPECCHIOWebClientException;

	/**
	 * Update or insert EAV metadata. Will automatically update existing entries or insert a new metaparameter if not existing.
	 *
	 * @param mp			the meta-parameter to update or insert
	 * @param spectrum_id	the identifiers for which to update or insert the parameter
	 *
	 * @return the identifier of the inserted or updated metadata
	 */
	public int updateOrInsertEavMetadata(MetaParameter mp, int spectrum_id) throws SPECCHIOWebClientException;

	/**
	 * Update or insert EAV metadata. Will automatically update existing entries or insert a new metaparameter if not existing.
	 *
	 * @param md	list containing all metadata
	 *
	 * @return the identifier of the inserted or updated metadata
	 */
	public int updateOrInsertEavMetadata(ArrayList<Metadata> md, ArrayList<Integer> ids, int campaignId) throws SPECCHIOWebClientException;


	/**
	 * Update an instrument.
	 * 
	 * @param instrument	the instrument
	 */
	public void updateInstrument(Instrument instrument) throws SPECCHIOClientException;
	
	
	/**
	 * Update the calibration metadata for an instrument.
	 * 
	 * @param cm	the calibration metadata
	 */
	public void updateInstrumentCalibrationMetadata(CalibrationMetadata cm) throws SPECCHIOClientException;
	
	
	/**
	 * Update a picture associated with an instrument.
	 * 
	 * @param picture	the picture
	 */
	public void updateInstrumentPicture(Picture picture) throws SPECCHIOClientException;
	
	
	/**
	 * Update an reference.
	 * 
	 * @param reference	the reference
	 */
	public void updateReference(Reference reference) throws SPECCHIOClientException;
	
	
	/**
	 * Update the calibration metadata for a reference.
	 * 
	 * @param cm	the calibration metadata
	 */
	public void updateReferenceCalibrationMetadata(CalibrationMetadata cm) throws SPECCHIOClientException;
	
	
	/**
	 * Update a picture associated with a reference.
	 * 
	 * @param picture	the picture
	 */
	public void updateReferencePicture(Picture picture) throws SPECCHIOClientException;
	
	
	/**
	 * Update the metadata fields for a set of spectra
	 * 
	 * @param ids	the spectrum identifiers
	 * @param field			the name of the field to be updated
	 * @param id
	 * @throws SPECCHIOClientException
	 */
	public void updateSpectraMetadata(ArrayList<Integer> ids, String field, int id) throws SPECCHIOClientException;
	
	
	/**
	 * Update the spectral vector of a spectrum
	 * 
	 * @param spectrum_id	the spectrum identifier
	 * @param vector		new spectral data
	 * 
	 * @throws SPECCHIOClientException
	 */
	public void updateSpectrumVector(int spectrum_id, float[] vector) throws SPECCHIOClientException;


	/**
	 * Update the spectral vector of a spectrum
	 *
	 * @param updateMap	a map containing the spectrum_id as key and the vector as value
	 *
	 * @throws SPECCHIOClientException
	 */
	public void updateSpectrumVectors(HashMap<Integer, double[]> updateMap) throws SPECCHIOClientException;
	
	
	/**
	 * Update the information about a user.
	 * 
	 * @param user	the user data
	 * 
	 * @throws SPECCHIOClientException
	 */
	public void updateUser(User user) throws SPECCHIOClientException;



}
