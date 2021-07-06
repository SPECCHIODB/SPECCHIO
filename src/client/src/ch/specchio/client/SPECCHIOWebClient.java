package ch.specchio.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.net.ssl.SSLContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;

import au.ands.org.researchdata.RDACollectionDescriptor;
import ch.specchio.interfaces.ProgressReportInterface;
import ch.specchio.jaxb.XmlBoolean;
import ch.specchio.jaxb.XmlInteger;
import ch.specchio.jaxb.XmlIntegerAdapter;
import ch.specchio.jaxb.XmlString;
import ch.specchio.jaxb.XmlStringAdapter;
import ch.specchio.plots.GonioSamplingPoints;
import ch.specchio.proc_modules.SunAngleCalcThread;
import ch.specchio.queries.EAVQueryConditionObject;
import ch.specchio.queries.Query;
import ch.specchio.spaces.MeasurementUnit;
import ch.specchio.spaces.ReferenceSpaceStruct;
import ch.specchio.spaces.Space;
import ch.specchio.spaces.SpaceQueryDescriptor;
import ch.specchio.spaces.SpectralSpace;
import ch.specchio.types.*;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;


/**
 * SPECCHIO web client.
 */
public class SPECCHIOWebClient implements SPECCHIOClient {
	
	/** the URL to which we are connected */
	private URL url;
	
	/** web service client */
	private Client web_client = null;
	
	/** web service */
	private WebResource web_service = null;
	
	/** the username under which we are logged in */
	private String username = null;
	
	/** the user as whom we are logged in as */
	private User user = null;
	
	/** the capabilities of the server */
	private Capabilities capabilities = null;
	
	/** the progress report on which to indicate progress */
	private ProgressReportInterface pr = null;

	private Hashtable<Integer, attribute> attributes_id_hash = null;
	private Hashtable<String, attribute> attributes_name_hash = null;

	private String dataSourceName;
	
	/** trust store configuration */
	private boolean uses_default_trust_store;
	
	
	/**
	 * Construct an anonymous connection to the web application server.
	 * 
	 * @param url		the URL of the SPECCHIO web application
	 * @param uses_default_trust_store
	 * 
	 * @throws SPECCHIOClientException	invalid URL
	 */
	SPECCHIOWebClient(URL url, String dataSourceName, boolean uses_default_trust_store) throws SPECCHIOClientException {
		
		this(url, null, null, dataSourceName, uses_default_trust_store);
		
	}
	
	
	/**
	 * Constructor.
	 * 
	 * @param url		the URL of the SPECCHIO web application
	 * @param username		the user name with which to log in
	 * @param password	the user's password
	 * @param dataSourceName the name of the datasource
	 * @param uses_default_trust_store
	 * 
	 * @throws SPECCHIOClientException	invalid URL
	 */
	SPECCHIOWebClient(URL url, String username, String password, String dataSourceName, boolean uses_default_trust_store) throws SPECCHIOClientException {
		
		// configure member variables
		this.url = url;
		this.username = username;
		this.dataSourceName = dataSourceName;
		this.uses_default_trust_store = uses_default_trust_store;
		
		// create the web services client configuration
		ClientConfig config = new DefaultClientConfig();
		config.getProperties().put(ClientConfig.PROPERTY_CHUNKED_ENCODING_SIZE, 32 * 1024);
		
		if (url.getProtocol().equalsIgnoreCase("https")) {
			// configure SSL
			try {
				SSLContext ssl = SSLContext.getInstance("SSL");
				ssl.init(null, null, null);
				HTTPSProperties https = new HTTPSProperties(null, ssl);
				config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, https);
			}
			catch (NoSuchAlgorithmException ex) {
				// not sure what would cause this
				ex.printStackTrace();
			} catch (KeyManagementException ex) {
				// not sure what would cause this
				ex.printStackTrace();
			}
		}
		
		try {
			// create the web services client and save a reference to the web service
			web_client = Client.create(config);
			web_service = web_client.resource(url.toString());
			
			// add response-fixing filter
			web_client.addFilter(new SPECCHIOWebClientFilter());
			
			if (username != null) {
				// configure HTTP basic authentication
				web_client.addFilter(new HTTPBasicAuthFilter(username, password));
			}
		}
		catch (IllegalArgumentException ex) {
			// invalid URL
			throw new SPECCHIOWebClientException(ex);
		}
		
	}
	
	
	
	/**
	 * Connect to the SPECCHIO web application.
	 * 
	 * @throws SPECCHIOWebClientException could not log in to the server
	 * @throws SPECCHIOClientException 
	 */
	public void connect() throws SPECCHIOWebClientException, SPECCHIOClientException {
		

		
		// reset the capabilities for a possible new server
		capabilities = null;
		
		if (username != null) {
			// execute the log in service
			if (pr != null) {
				pr.set_operation("Connecting to " + url);
			}
			user = getObject(User.class, "user", "login");
		}
		
	}
	
	/**
	 * Close this client
	 * 
	 * @throws SPECCHIOClientException could not close client
	 */
	public void close() throws SPECCHIOClientException{
		this.web_service = null;
		this.web_client.destroy();
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
				
		int new_spectrum_id = getInteger("spectrum", "copySpectrum", Integer.toString(spectrum_id), Integer.toString(target_hierarchy_id));
		return new_spectrum_id;
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
	public ArrayList<Integer> copySpectra(ArrayList<Integer> spectrum_id, int target_hierarchy_id, int current_hierarchy_id) throws SPECCHIOClientException {
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(spectrum_id, target_hierarchy_id, current_hierarchy_id);

		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		Integer[] id_array = adapter.unmarshalArray(postForArray(XmlInteger.class, "spectrum", "copySpectra", mds));

		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int id : id_array) {
			ids.add(id);
		}

		return ids;

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
		
		int new_hierarchy_id = getInteger("campaign", "copyHierarchy", Integer.toString(hierarchy_id), Integer.toString(target_hierarchy_id), new_name);
		return new_hierarchy_id;
	}
	
	

	/**
	 * Clears the known metaparameter list held by the server for this user
	 */
	public void clearMetaparameterRedundancyList() throws SPECCHIOClientException
	{
		getString("metadata", "clear_redundancy_list");
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
		
		return postForObject(User.class, "public", "createUserAccount", user);
		
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
		
		return postForObject(User.class, "user", "createUserAccount", user);
		
	}	
	
	
	/**
	 * Create a new instrument.
	 * 
	 * @param name	the name of the new instrument
	 * @throws SPECCHIOWebClientException 
	 */
	public void createInstrument(String name) throws SPECCHIOWebClientException {
		
		getString("instrumentation", "insert", name);
		
	}
	
	
	/**
	 * Create a new reference.
	 * 
	 * @param name	the name of the new reference
	 * @throws SPECCHIOWebClientException 
	 */
	public void createReference(String name) throws SPECCHIOWebClientException {
		
		getString("instrumentation", "insertReference", name);
		
	}
	
	/**
	 * Database upgrade
	 * 
	 * @param version	DB version to be upgraded to
	 * @param fis	File input stream with SQL upgrade statements
	 * @throws SPECCHIOClientException 
	 */	
	public void dbUpgrade(double version, FileInputStream fis)  throws SPECCHIOClientException {

		postInputStream(fis, "campaign", "dbUpgrade", Double.toString(version));
	}
	
	
	
	/**
	 * Delete calibration data from the database
	 * 
	 * @param calibration_id	the calibration identifier
	 * @throws SPECCHIOWebClientException 
	 */
	public void deleteCalibration(int calibration_id) throws SPECCHIOWebClientException {
		
		getString("instrumentation", "deleteCalibration", Integer.toString(calibration_id));
		
	}
	
	
	/**
	 * Delete an instrument from the database.
	 * 
	 * @param instrument_id	the instrument identifier
	 * 
	 * @throws SPECCHIOWebClientException 
	 */
	public void deleteInstrument(int instrument_id) throws SPECCHIOWebClientException {
		
		getString("instrumentation", "delete", Integer.toString(instrument_id));
		
	}
	
	
	/**
	 * Delete a picture of an instrument from the database.
	 * 
	 * @param picture_id	the picture identifier
	 */
	public void deleteInstrumentPicture(int picture_id) throws SPECCHIOWebClientException {
		
		getString("instrumentation", "deletePicture", Integer.toString(picture_id));
		
	}
	
	
	/**
	 * Delete a reference from the database.
	 * 
	 * @param reference_id	the reference identifier
	 */
	public void deleteReference(int reference_id) throws SPECCHIOWebClientException {
		
		getString("instrumentation", "deleteReference", Integer.toString(reference_id));
		
	}
	
	
	/**
	 * Delete a picture of a reference from the database.
	 * 
	 * @param picture_id	the picture identifier
	 */
	public void deleteReferencePicture(int picture_id) throws SPECCHIOWebClientException {
		
		getString("instrumentation", "deletePicture", Integer.toString(picture_id));
		
	}
	
	
	/**
	 * Delete a target-reference link from the database.
	 * 
	 * @param eav_id		the eav_id identifier
	 * 
	 * @return the number of links deleted
	 */
	public int deleteTargetReferenceLinks(int eav_id) throws SPECCHIOWebClientException {
		
		return getInteger("spectrum", "deleteTargetReferenceLinks", Integer.toString(eav_id));
				
	}


	/**
	 * Disconnect from the SPECCHIO web application.
	 */
	public void disconnect() throws SPECCHIOWebClientException {
		
	}
	
	
	/**
	 * Get the spectrum identifiers that do have a reference to the specified attribute.
	 * 
	 * @param spectrum_ids	list of ids to filter
	 * @param attribute_name	attribute name to filter with
	 * 
	 * @return an array list of spectrum identifiers that match the filter
	 */
	public ArrayList<Integer> filterSpectrumIdsByHavingAttribute(ArrayList<Integer> spectrum_ids, String attribute_name) throws SPECCHIOClientException {
		
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(spectrum_ids, attribute_name);	
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		Integer[] id_array = adapter.unmarshalArray(postForArray(XmlInteger.class, "spectrum", "filterSpectrumIdsByHavingAttribute", mds));
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int id : id_array) {
			ids.add(id);
		}		
		
		return ids;		
		
	}
	
	
	
	
	/**
	 * Get the spectrum identifiers that do not have a reference to the specified attribute.
	 * 
	 * @param spectrum_ids	list of ids to filter
	 * @param attribute_name	attribute_name to filter with
	 * 
	 * @return an array list of spectrum identifiers that match the filter
	 */
	public ArrayList<Integer> filterSpectrumIdsByNotHavingAttribute(ArrayList<Integer> spectrum_ids, String attribute_name) throws SPECCHIOClientException {
		
		
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(spectrum_ids, attribute_name);	
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		Integer[] id_array = adapter.unmarshalArray(postForArray(XmlInteger.class, "spectrum", "filterSpectrumIdsByNotHavingAttribute", mds));
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int id : id_array) {
			ids.add(id);
		}		
		
		return ids;
		
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
		
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(spectrum_ids, attribute_name, value);	
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		Integer[] id_array = adapter.unmarshalArray(postForArray(XmlInteger.class, "spectrum", "filterSpectrumIdsByHavingAttributeValue", mds));
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int id : id_array) {
			ids.add(id);
		}		
		
		return ids;			
	}
		
	
	/**
	 * Get the attributes for a metadata category.
	 * 
	 * @param category	the category name
	 * 
	 * @return an array of attribute objects
	 * @throws SPECCHIOClientException 
	 */
	public attribute[] getAttributesForCategory(String category) throws SPECCHIOWebClientException, SPECCHIOClientException {
		
		attribute[] array = getArray(attribute.class, "metadata", "attributes", category);
	
		return array;
		
	}
	
	
	/**
	 * Get the attributes object containing information on all attributes, units and categories.
	 * 
	 * @return Attributes
	 */
	public attribute[] getAttributes() throws SPECCHIOClientException {
		
		attribute[] atrs = getArray(attribute.class, "metadata", "all_attributes");
		
		return atrs;
		
	}
	
	/**
	 * Get the attributes hashtable
	 * 
	 * @return Attributes stored in hashtable, indexed by attribute_id
	 */
	public Hashtable<Integer, attribute> getAttributesIdHash() throws SPECCHIOClientException {
		
		if(this.attributes_id_hash == null)
		{
			attribute[] attributes = getAttributes();
			
			attributes_id_hash = new Hashtable<Integer, attribute>();

			// generate hashtable
			for(int i=0;i<attributes.length;i++)
			{
				attributes_id_hash.put(attributes[i].getId(), attributes[i]);
			}
		}
		
		return attributes_id_hash;
	}
	
	/**
	 * Get the attributes hashtable
	 * 
	 * @return Attributes stored in hashtable, indexed by attribute name
	 */
	public Hashtable<String, attribute> getAttributesNameHash() throws SPECCHIOClientException {

		if(this.attributes_name_hash == null)
		{
			attribute[] attributes = getAttributes();
			
			attributes_name_hash = new Hashtable<String, attribute>();

			// generate hashtable
			for(int i=0;i<attributes.length;i++)
			{
				attributes_name_hash.put(attributes[i].getName(), attributes[i]);
			}
		}
		
		return attributes_name_hash;		
		
	}
	
	
	/**
	 * Get the units for an attribute.
	 * 
	 * @param attr	the attribute
	 * 
	 * @return a units object representing the attribute's units
	 */
	public Units getAttributeUnits(attribute attr) throws SPECCHIOClientException {
		
		return postForObject(Units.class, "metadata", "units", attr);
		
	}
	

	/**
	 * Get the info of all EAV metadata categories.
	 * 
	 * @return an array list of category names
	 */
	public ArrayList<Category> getCategoriesInfo() throws SPECCHIOClientException {
		
		return (ArrayList<Category>) getList(Category.class, "metadata", "categories_info");
	}

	public ArrayList<Category> getNonNullCategories(ArrayList<Integer> spectrumIds){
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(spectrumIds, "");
		return (ArrayList<Category>) postForList(Category.class, "metadata", "non_null_categories", mds);
	}

	public ArrayList<attribute> getNonNullAttributes(ArrayList<Integer> spectrumIds){
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(spectrumIds, "");
		return (ArrayList<attribute>) postForList(attribute.class, "metadata", "non_null_attributes", mds);
	}

	public ArrayList<Integer> findMatchingSpectra(ArrayList<Integer> spectrumIds, ArrayList<QueryAttribute> queryAttributes){
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(spectrumIds, "");
		mds.setQueryAttributes(queryAttributes);
		return (ArrayList<Integer>) postForList(Integer.class, "metadata", "findMatchingSpectra", mds);
	}

	public void createFilterCollection(ArrayList<Integer> spectrumIds, ArrayList<Integer> attributeIds){
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(spectrumIds, attributeIds);
		postForList(Integer.class, "metadata", "create_filter_collection", mds);
	}
	
	/**
	 * Get a campaign descriptor.
	 * 
	 * @param campaign_id	the campaign identifier
	 * 
	 * @return a new campaign object
	 */
	public Campaign getCampaign(int campaign_id) throws SPECCHIOClientException {
		
		return getObject(Campaign.class, "campaign", "get", "specchio", Integer.toString(campaign_id));
		
	}
	
	
	/**
	 * Export a campaign.
	 * 
	 * @param c		the campaign to be exported
	 * 
	 * @return an input stream connected to the exported campaign date
	 */
	public InputStream getCampaignExportInputStream(Campaign c) throws SPECCHIOWebClientException {
		
		return getInputStream("campaign", "export", c.getType(), Integer.toString(c.getId()));
		
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
	public campaign_node getCampaignNode(int campaign_id, String order_by, boolean restrict_to_view) throws SPECCHIOWebClientException {
		
		return getObject(campaign_node.class,
				"browser", "campaign",
				Integer.toString(campaign_id),
				order_by,
				Boolean.toString(restrict_to_view)
			);
		
	}
	
	
	/**
	 * Get all of the campaigns in the database.
	 * 
	 * @param load_metadata_details		defines if additional detailed metadata should be loaded
	 *
	 * @return an array of campaign objects describing each campaign in the database
	 */
	public Campaign[] getCampaigns(boolean load_metadata_details) throws SPECCHIOWebClientException {
		
		if(load_metadata_details)
			return getArray(Campaign.class, "campaign", "detailed_list", "specchio");
		else
			return getArray(Campaign.class, "campaign", "list", "specchio");
		
	}
	
	/**
	 * Get all of the campaigns in the database.
	 *
	 * @return an array of campaign objects describing each campaign in the database
	 */
	public Campaign[] getCampaigns() throws SPECCHIOWebClientException {
		
		return getCampaigns(false);
		
	}	

	
	/**
	 * Get calibration ids for a list of spectra.
	 * 
	 * @param spectrum_ids	the spectrum identifiers
	 * 
	 * @return list of calibration ids, zero where no calibration is defined
	 */
	public ArrayList<Integer> getCalibrationIds(ArrayList<Integer> spectrum_ids) throws SPECCHIOWebClientException {
		
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(spectrum_ids, "");	
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		Integer[] id_array = adapter.unmarshalArray(postForArray(XmlInteger.class, "metadata", "getCalibrationIds", mds));
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int id : id_array) {
			ids.add(id);
		}		
		
		return ids;	
		
	}
	
	/**
	 * Get a calibrated instrument.
	 * 
	 * @param calibration_id	the calibration identifier
	 * 
	 * @return a new Instrument object, or null if the calibrated instrument does not exist
	 */
	public Instrument getCalibratedInstrument(int calibration_id) throws SPECCHIOClientException {
				
		return getObject(Instrument.class, "instrumentation", "getCalibratedInstrument", Integer.toString(calibration_id));
		
	}
	
	/**
	 * Get the value of a capability.
	 * 
	 * @param capability	the capability name
	 * 
	 * @return the value of the capability, or null if the capability is not recognised
	 */
	public String getCapability(String capability) throws SPECCHIOClientException {
		
		if (capabilities == null) {
			capabilities = getObject(Capabilities.class, "public", "capabilities");
		}
		
		return capabilities.getCapability(capability);
		
	}
	
	
	/**
	 * Get the children of a node of the spectral data browser.
	 * 
	 * @param sn	the node
	 * 
	 * @return a list of the node's children
	 */
	public List<spectral_node_object> getChildrenOfNode(spectral_node_object sn) throws SPECCHIOWebClientException {
		
		return postForList(spectral_node_object.class, "browser", "children", sn);
		
	}
	
	
	/**
	 * Get the children of a node of a taxonomy.
	 * 
	 * @param tn	the node
	 * 
	 * @return a list of the node's children
	 */
	public List<TaxonomyNodeObject> getChildrenOfTaxonomyNode(TaxonomyNodeObject tn) throws SPECCHIOWebClientException {
		
		return postForList(TaxonomyNodeObject.class, "metadata", "getChildrenOfTaxonomyNode", tn);
		
	}
	
	
	/**
	 * Get the list of countries known to the server.
	 * 
	 * @return an array of Country objects
	 */
	public Country[] getCountries() throws SPECCHIOWebClientException {
		
		return getArray(Country.class, "public", "listCountries");
		
	}
	
	
	/**
	 * Get a database node for the spectral data browser.
	 * 
	 * @param order_by			the attribute to order by
	 * @param restrict_to_view	display the current user's data only
	 * 
	 * @return a database_node object
	 */
	public database_node getDatabaseNode(String order_by, boolean restrict_to_view) throws SPECCHIOWebClientException {
		
		return getObject(database_node.class,
				"browser", "database",
				order_by,
				Boolean.toString(restrict_to_view)
			);
		
	}
	
	
	/**
	 * Get the direct hierarchy id of a single spectrum
	 * 
	 * @param spectrum_id		the identifier of the spectrum
	 * 
	 * @return hierarchy id
	 * 
	 * @throws SPECCHIOClientException
	 */	
	public Integer getDirectHierarchyId(int spectrum_id) throws SPECCHIOClientException {
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		ids.add(spectrum_id);
		SpectrumIdsDescriptor d = new SpectrumIdsDescriptor(ids);
		
		Integer id = postForInteger("spectrum", "getDirectHierarchyOfSpectrum", d);

		 return id;
	}
	
	
	
	/**
	 * Get hierarchy ids, directly above these spectra
	 * 
	 * @param spectrum_ids		the identifiers of the desired spectra
	 * 
	 * @return hierarchy ids
	 * 
	 * @throws SPECCHIOClientException
	 */	
	public ArrayList<Integer> getDirectHierarchyIds(ArrayList<Integer> spectrum_ids) throws SPECCHIOClientException {
		
		SpectrumIdsDescriptor d = new SpectrumIdsDescriptor(spectrum_ids);
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		Integer[] id_array = adapter.unmarshalArray(postForArray(XmlInteger.class, "spectrum", "getDirectHierarchyIds", d));
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int id : id_array) {
			ids.add(id);
		}	
		
		return ids;				
		
	}
	
	
	/**
	 * Get distinct values of an attribute
	 * 
	 * @param attribute_id	id of the required attribute
	 * 
	 * @return arraylist of metaparameters	
	 */
	public ArrayList<MetaParameter> getDistinctValuesOfAttribute(int attribute_id) {	// throws SPECCHIOWebClientException {
		
		//return (ArrayList<MetaParameter>) postForList(MetaParameter.class, "metadata", "getDistinctValuesOfAttribute", new XmlInteger(attribute_id) );
		ArrayList<MetaParameter> out_list = new ArrayList<MetaParameter>();
		try {
		
		MetaParameter[] mps = postForArray(MetaParameter.class, "metadata", "getDistinctValuesOfAttribute", new XmlInteger(attribute_id));
		
		
		
		
		for (int i = 0; i < mps.length; i++) {
			out_list.add(mps[i]);
		}		
		
		
		
		
		}catch(ch.specchio.client.SPECCHIOWebClientException e)
		{
			int x=0;
		}
		
		return out_list;		
				
	}
	
	/**
	 * Get a conflicts in the EAV metadata for a set of spectra.
	 * 
	 * @param metadata_level		storage level identifier
	 * @param ids	the primary entity identifiers
	 * 
	 * @return a ConflictTable object containing all of the conflicts
	 */
	public ConflictTable getEavMetadataConflicts(int metadata_level, ArrayList<Integer> ids) throws SPECCHIOWebClientException {

		ConflictDetectionDescriptor cd_d = new ConflictDetectionDescriptor(ids);
		cd_d.setLevel(metadata_level);
		
		return postForObject(ConflictTable.class, "metadata", "conflicts_eav", cd_d);
		
	}
	
	
	/**
	 * Return an EAVQueryConditionObject configured for the supplied attribute.
	 * 
	 * @param attr	attribute object
	 * 
	 * @return EAVQueryConditionObject
	 */
	public EAVQueryConditionObject getEAVQueryConditionObject(attribute attr)
	{
		return new EAVQueryConditionObject(attr);
	}
	
	
	/**
	 * Get the count of existing metaparameters for the supplied spectrum ids and attribute id
	 * 
	 * @param attribute_id	id of the attribute
	 * @param ids	spectrum ids
	 * 
	 * @return Integer
	 */
	
	public Integer getExistingMetaparameterCount(Integer attribute_id, ArrayList<Integer> ids)  throws SPECCHIOClientException
	{
		Integer count = 0;
		
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(ids, attribute_id);
		count =  postForInteger("metadata", "count_existing_metaparameters", mds);
		
		return count;
	}
		
	
	/**
	 * Get the file format identifier for a file format name.
	 * 
	 * @param format	the file format name
	 * 
	 * @return the identifier associated with the specified file format, or -1 if file format is not recognised
	 */
	public int getFileFormatId(String format) throws SPECCHIOWebClientException {
		
		return getInteger("spectral_file", "file_format_id", format);
		
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
		return getObject(Hierarchy.class, "campaign", "getHierarchy", Integer.toString(hierarchy_id));
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
	public int getHierarchyId(Campaign campaign, String name, int parent_id) throws SPECCHIOWebClientException {
		
		return getInteger(
				"campaign", "getHierarchyId",
				"specchio", Integer.toString(campaign.getId()), name, Integer.toString(parent_id)
			);
		
	}
	
	/**
	 * Get a list of hierarchy ids, covering all hierarchies above these spectra
	 * 
	 * @param spectrum_ids		the identifiers of the desired spectra
	 * 
	 * @return hierarchy ids
	 * 
	 * @throws SPECCHIOClientException
	 */	
	public ArrayList<Integer> getHierarchyIdsOfSpectra(ArrayList<Integer> spectrum_ids) throws SPECCHIOClientException {
		
		SpectrumIdsDescriptor d = new SpectrumIdsDescriptor(spectrum_ids);
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		Integer[] id_array = adapter.unmarshalArray(postForArray(XmlInteger.class, "spectrum", "getHierarchyIdsOfSpectra", d));
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int id : id_array) {
			ids.add(id);
		}	
		
		return ids;					
		
	}
	
	
	
	/**
	 * Get the file path of a hierarchy.
	 * 
	 * @param hierarchy_id		the identifier of the hierarchy
	 * 
	 * @return path as string
	 * 
	 * @throws SPECCHIOClientException	the database could not accessed
	 */
	public String getHierarchyFilePath(int hierarchy_id) throws SPECCHIOClientException
	{
		String path = getString("campaign", "getHierarchyFilePath", Integer.toString(hierarchy_id));	

		return path;
	}		
	
	/**
	 * Get the name of a hierarchy.
	 * 
	 * @param hierarchy_id		the identifier of the hierarchy
	 * 
	 * @return name		name as string
	 * 
	 * @throws SPECCHIOClientException	the database could not accessed
	 */	
	public String getHierarchyName(int hierarchy_id) throws SPECCHIOClientException	{
		String name = getString("campaign", "getHierarchyName", Integer.toString(hierarchy_id));	

		return name;
	}		
	
	/**
	 * Get the parent_id for a given hierarchy_id
	 * 
	 * @param hierarchy_id	the hierarchy_id identifying the required node
	 * 
	 * @return id of the parent of given hierarchy
	 */
	public int getHierarchyParentId(int hierarchy_id) throws SPECCHIOClientException {
		
		return getInteger("browser", "get_hierarchy_parent_id", Integer.toString(hierarchy_id));
		
	}	
		
	
	
	/**
	 * Get all of the institutes in the database.
	 * 
	 * @return an array of Institute objects
	 */
	public Institute[] getInstitutes() throws SPECCHIOWebClientException {
		
		return getArray(Institute.class, "public", "listInstitutes");
		
	}
	
	
	/**
	 * Get an instrument.
	 * 
	 * @param instrument_id	the instrument identifier
	 * 
	 * @return a new Instrument object, or null if the instrument does not exist
	 */
	public Instrument getInstrument(int instrument_id) throws SPECCHIOWebClientException {
		
		return getObject(Instrument.class, "instrumentation", "get", Integer.toString(instrument_id));
		
	}
	
	
	/**
	 * Get an instrument that matches the passed object or creates a new instrument and returns it
	 * 
	 * @param instrument instance
	 * 
	 * @return a new Instrument object
	 */
	public Instrument getInstrument(Instrument instrument) throws SPECCHIOWebClientException {
		
		return postForObject(Instrument.class, "instrumentation", "getOrCreateInstrument", instrument);
		
	}
	
	
	
	/**
	 * Get instrument ids for a list of spectra.
	 * 
	 * @param spectrum_ids	the spectrum identifiers
	 * 
	 * @return list of instrument ids, zero where no instrument is defined
	 */
	public ArrayList<Integer> getInstrumentIds(ArrayList<Integer> spectrum_ids) throws SPECCHIOWebClientException {
		
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(spectrum_ids, "");	
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		Integer[] id_array = adapter.unmarshalArray(postForArray(XmlInteger.class, "metadata", "getInstrumentIds", mds));
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int id : id_array) {
			ids.add(id);
		}		
		
		return ids;	
		
	}
		
	
	
	/**
	 * Get the calibration metadata for an instrument.
	 * 
	 * @param instrument_id	the instrument identifier
	 * 
	 * @return an array of CalibrationMetadata objects, or null if the instrument does not exist
	 */
	public CalibrationMetadata[] getInstrumentCalibrationMetadata(int instrument_id) throws SPECCHIOWebClientException {
		
		return getArray(CalibrationMetadata.class,
				"instrumentation", "calibrationMetadata", "instrument", Integer.toString(instrument_id)
			);
		
	}
	
	

	
	/**
	 * Get descriptors for all of the instruments in the database.
	 * 
	 * @return an array of InstrumentDescriptor objects
	 */
	public InstrumentDescriptor[] getInstrumentDescriptors() throws SPECCHIOWebClientException {
		
		return getArray(InstrumentDescriptor.class, "instrumentation", "list");
		
	}
	
	/**
	 * Get a instrument object for a given spectral file object.
	 * 
	 * @param spec_file		the spectral file
	 * 
	 * @return a new Instrument object
	 */	
	public Instrument getInstrumentForSpectralFile(SpectralFile spec_file) throws SPECCHIOClientException {
		
		Instrument instr = null;
		try {
			instr = postForObject(Instrument.class, "instrumentation", "getInstrumentForSpectralFile", spec_file);
		} catch (SPECCHIOWebClientException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println(e.getDetails());
			throw e;
		}
		
		return instr;
	}	
	
	
	/**
	 * Get all of the pictures for an instrument.
	 * 
	 * @param instrument_id	the instrument identifier
	 * 
	 * @return a PictureTable object containing all of the pictures for this instrument, or null if the instrument doesn't exist
	 */
	public PictureTable getInstrumentPictures(int instrument_id) throws SPECCHIOWebClientException {
		
		return getObject(PictureTable.class,
				"instrumentation", "pictures", "instrument", Integer.toString(instrument_id)
			);
		
	}
	
	
	/**
	 * Get a user object representing the user under which the client is logged in.
	 * 
	 * @return a reference to a user object, or null if the client is not connected
	 */
	public User getLoggedInUser() {
		
		return user;
		
	}
	
	/**
	 * Get the metadata categories for application domain
	 * 
	 * @param taxonomy_id	the field name
	 * 
	 * @return a ArrayList<Integer> object, or null if the field does not exist
	 */
	public ArrayList<Integer> getMetadataCategoriesForApplicationDomain(int taxonomy_id) throws SPECCHIOClientException {
		
		return null; // this should never be called as always caught by the cache
	}	
	
	/**
	 * Get the metadata categories per application domain
	 * 
	 * @return a ApplicationDomainCategories object, or null if the information does not exist
	 */
	
	public ApplicationDomainCategories[] getMetadataCategoriesForApplicationDomains() throws SPECCHIOClientException {
		
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor();
		
		return postForArray(ApplicationDomainCategories.class, "metadata", "application_domain_categories", mds);
				
	}	
	
	
	/**
	 * Get the metadata categories for a metadata field.
	 * 
	 * @param field	the field name
	 * 
	 * @return a CategoryTable object, or null if the field does not exist
	 * @throws SPECCHIOClientException 
	 */
	public CategoryTable getMetadataCategoriesForIdAccess(String field) throws SPECCHIOWebClientException, SPECCHIOClientException {
		
		return getObject(CategoryTable.class, "metadata", "categories", field);
		
	}
	
	
	/**
	 * Get the metadata categories for a metadata field, ready for access via name
	 * 
	 * @param field	the field name
	 * 
	 * @return a CategoryTable object, or null if the field does not exist
	 */
	public Hashtable<String, Integer> getMetadataCategoriesForNameAccess(String field) throws SPECCHIOClientException {
		
		return null; // this should never be called as always caught by the cache
	}
	
	
	
	/**
	 * Get the metadata conflicts for a set of spectra and set of fields.
	 * 
	 * @param spectrum_ids	the spectrum identifiers
	 * @param fields		the fields to check
	 * 
	 * @return a ConflictTable object listing the conflicts found
	 */
	public ConflictTable getMetadataConflicts(ArrayList<Integer> spectrum_ids, String fields[]) throws SPECCHIOWebClientException {

		ConflictDetectionDescriptor cd_d = new ConflictDetectionDescriptor(spectrum_ids);
		cd_d.setMetadataFields(fields);
		return postForObject(ConflictTable.class,  "metadata", "conflicts", cd_d);
		
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
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		ids.add(id);
		
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(ids, attribute_name);
		
		MetaParameter[] mps = postForArray(MetaParameter.class, "metadata", "get_list_of_metaparameter_vals", mds);
		
		if(mps.length > 0 && mps[0].getValue() != null)
			return mps[0];		
		else
			return null;
	}			
	
	
	/**
	 * Get metaparameters for spectrum ids and EAV attribute
	 * 
	 * @param ids		spectrum ids
	 * @param attribute_name		attribute name
	 * 
	 * @return list of metaparameters, or null if the field does not exist	 
	 */
	public ArrayList<MetaParameter> getMetaparameters(ArrayList<Integer> ids, String attribute_name) throws SPECCHIOWebClientException {
		
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(ids, attribute_name);
		
		MetaParameter[] mps = postForArray(MetaParameter.class, "metadata", "get_list_of_metaparameter_vals", mds);
		
		
		ArrayList<MetaParameter> out_list = new ArrayList<MetaParameter>();
		
		for (int i = 0; i < mps.length; i++) {
			out_list.add(mps[i]);
		}		
		
		
		return out_list;
		
	}

	@Override
	public void calculateSunAngle(ArrayList<Integer> spectrumIds, SPECCHIOClient client) throws SPECCHIOClientException {
		SunAngleCalcThread sunCalc = new SunAngleCalcThread(spectrumIds, client, null);
//		sunCalc.run();
	}

/*	@Override
	public void calculateSunAngle(ArrayList<Integer> spectrumIds, SPECCHIOClient client) throws SPECCHIOClientException {
//		Hashtable<String, attribute> attr = getAttributesNameHash();
//		Integer latId = attr.get("Latitude").id;
//		Integer lonId = attr.get("Longitude").id;
//		Integer acqTime = attr.get("Acquisition Time (UTC)").id;
//		Integer illuAzi = attr.get("Illumination Azimuth").id;
//		Integer illuZen = attr.get("Illumination Zenith").id;
//		ArrayList<Integer> attrIds = new ArrayList<Integer>();
//		attrIds.add(latId);
//		attrIds.add(lonId);
//		attrIds.add(acqTime);
//		attrIds.add(illuAzi);
//		attrIds.add(illuZen);
//		MetadataSelectionDescriptor mdsd = new MetadataSelectionDescriptor(spectrumIds,attrIds);
//		ArrayList<ArrayList<MetaParameter>> params = getMetaparameters(spectrumIds, attrIds);
		SunAngleCalc sunCalc = new SunAngleCalc(spectrumIds, client);
		sunCalc.calculateSunAngle();
		calculateSunAngle(spectrumIds);
	}*/


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
		
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(ids, attribute_name);
		mds.setDistinct(distinct);
		
		MetaParameter[] mps = postForArray(MetaParameter.class, "metadata", "get_list_of_metaparameter_vals", mds);
		
		
		ArrayList<MetaParameter> out_list = new ArrayList<MetaParameter>();
		
		for (int i = 0; i < mps.length; i++) {
			out_list.add(mps[i]);
		}		
		
		
		return out_list;
		
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
		
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(ids, attribute_ids);
		
		MetaParameter[] mp_list = postForArray(MetaParameter.class, "metadata", "get_list_of_multiple_metaparameter_vals", mds);
		
		// rebuild list of lists
		ArrayList<ArrayList<MetaParameter>> out_list = new ArrayList<ArrayList<MetaParameter>>();
		int cnt = 0;
		
		
		for (int i = 0; i <attribute_ids.size(); i++) {
			ArrayList<MetaParameter> temp_list = new ArrayList<MetaParameter>();
			for (int j = 0; j <ids.size(); j++) {
				temp_list.add(mp_list[cnt++]);
			}
			out_list.add(temp_list);
		}		
		
		
		return out_list;		
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
		
		MetaparameterStatistics mps;
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(ids, attribute_name);

		try {
			mps = postForObject(MetaparameterStatistics.class, "metadata", "get_metaparameter_statistics", mds);
		} catch (SPECCHIOWebClientException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println(e.getDetails());
			throw e;
		}
		
		
		return mps;
		
	}
	
	
	
	/**
	 * Get values for spectrum ids and EAV attribute (non-distinct values by default)
	 * 
	 * @param ids		spectrum ids
	 * @param attribute_name		attribute name
	 * 
	 * @return list of values, or null if the field does not exist	 
	 */
	public MatlabAdaptedArrayList<Object> getMetaparameterValues(ArrayList<Integer> ids, String attribute_name) throws SPECCHIOWebClientException {
		
		return getMetaparameterValues(ids, attribute_name, false);
		
//		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(ids, attribute_name);
//		
//		MetaParameter[] mps = postForArray(MetaParameter.class, "metadata", "get_list_of_metaparameter_vals", mds);
//		
//		
//		MatlabAdaptedArrayList<Object> out_list = new MatlabAdaptedArrayList<Object>();
//		
//		for (int i = 0; i < mps.length; i++) {
//			out_list.add(mps[i].getValue());
//		}		
//		
//		
//		return out_list;
		
	}
	
	/**
	 * Get values for spectrum ids and EAV attribute
	 * 
	 * @param ids		spectrum ids
	 * @param attribute_name		attribute name
	 * @param distinct		defines if distinct values should be returned or repeated values for the given spectrum ids
	 * 
	 * @return list of values, or null if the field does not exist	 
	 */
	public MatlabAdaptedArrayList<Object> getMetaparameterValues(ArrayList<Integer> ids, String attribute_name, Boolean distinct) throws SPECCHIOWebClientException {
		
		MetadataSelectionDescriptor mds = new MetadataSelectionDescriptor(ids, attribute_name);
		mds.setDistinct(distinct);
		
		MetaParameter[] mps = postForArray(MetaParameter.class, "metadata", "get_list_of_metaparameter_vals", mds);
		
		
		MatlabAdaptedArrayList<Object> out_list = new MatlabAdaptedArrayList<Object>();
		
		for (int i = 0; i < mps.length; i++) {
			
			if(distinct)
			{
				Object value = mps[i].getValue();
				
				boolean contains =  out_list.contains(value);
				
				if(!contains)
				{
					out_list.add(value);
				}
				
			}
			else
			{
				out_list.add(mps[i].getValue());
			}
		}		
		
		
		return out_list;
		
	}	
	
	
	/**
	 * Get measurement unit for ASD based coding.
	 * 
	 * @param coding	coding based on ASD coding
	 * 
	 * @return a new MeasurementUnit object, or null if the coding does not exist
	 */	
	public MeasurementUnit getMeasurementUnitFromCoding(int coding) throws SPECCHIOWebClientException {
		
		
		MeasurementUnit mu = getObject(MeasurementUnit.class, "metadata", "get_measurement_unit_from_coding", Integer.toString(coding));
		
		return mu;
	}
	
	/**
	 * Get newest N spectra.
	 * 
	 * @param number_of_spectra
	 * 
	 * @return list of spectrum ids ordered by data ingestion time
	 */	
	public ArrayList<Integer> getNewestSpectra(int number_of_spectra) throws SPECCHIOWebClientException
	{
		// arraylist wrapper trial: leads to error 500 ...
//		XmlInteger xml_int = new XmlInteger(number_of_spectra);
//		ArrayListWrapper<Integer> l = postForObject(ArrayListWrapper.class, "spectrum", "getNewestSpectra", xml_int);
//		
//		return (ArrayList<Integer>) l.getList();		
		
		XmlInteger xml_int = new XmlInteger(number_of_spectra);
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		Integer[] id_array = adapter.unmarshalArray(postForArray(XmlInteger.class, "spectrum", "getNewestSpectra", xml_int));
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int id : id_array) {
			ids.add(id);
		}	

		
		return ids;		
	}
	
	
	/**
	 * Get the data usage policies for a space.
	 * 
	 * @param space	the space
	 * 
	 * @return an array of Policy objects
	 */
	public String[] getPoliciesForSpace(Space space) throws SPECCHIOWebClientException {

		XmlStringAdapter adapter = new XmlStringAdapter();
		return adapter.unmarshalArray(postForArray(XmlString.class, "metadata", "getPoliciesForSpace", space));
		
	}
	
	
	/**
	 * Get an empty query object
	 * 
	 * @return a Query Object
	 */
	public Query getQueryObject() throws SPECCHIOClientException {
		return new Query();
	}
	
	
	
	/**
	 * Get a reference.
	 * 
	 * @param reference_id	the reference identifier
	 * 
	 * @return a new Reference object, or null if the instrument does not exist
	 */
	public Reference getReference(int reference_id) throws SPECCHIOWebClientException {
		
		return getObject(Reference.class, "instrumentation", "getReference", Integer.toString(reference_id));
	
	}
	
	
	/**
	 * Get all of the reference brands in the database.
	 * 
	 * @return an array of ReferenceBrand objects
	 */
	public ReferenceBrand[] getReferenceBrands() throws SPECCHIOWebClientException {
		
		return getArray(ReferenceBrand.class, "instrumentation", "listReferenceBrands");
		
	}
	
	
	/**
	 * Get the calibration metadata for a reference.
	 * 
	 * @param reference_id	the reference identifier
	 * 
	 * @return an array of CalibrationMetadata objects, or null if the reference does note exist
	 */
	public CalibrationMetadata[] getReferenceCalibrationMetadata(int reference_id) throws SPECCHIOWebClientException {
		
		return getArray(CalibrationMetadata.class,
				"instrumentation", "calibrationMetadata", "reference", Integer.toString(reference_id)
			);
		
	}
	
	
	/**
	 * Get descriptors for all of the references in the database.
	 * 
	 * @return an arra of ReferenceDescriptor objects
	 */
	public ReferenceDescriptor[] getReferenceDescriptors() throws SPECCHIOWebClientException {
		
		return getArray(ReferenceDescriptor.class, "instrumentation", "listReferences");
		
	}
	
	
	/**
	 * Get all of the pictures associated with a reference.
	 * 
	 * @param reference_id	the reference identifier
	 * 
	 * @return a PictureTable containing all of the pictures
	 */
	public PictureTable getReferencePictures(int reference_id) throws SPECCHIOWebClientException {
		
		return getObject(PictureTable.class,
				"instrumentation", "pictures", "reference", Integer.toString(reference_id)
			);
		
	}
	
	
	/**
	 * Get a reference space.
	 * 
	 * @param input_ids
	 * 
	 * @return a ReferenceSpaceStruct object, or null if no space could be found
	 */
	public ReferenceSpaceStruct getReferenceSpace(ArrayList<Integer> input_ids) throws SPECCHIOWebClientException {
		
		SpectrumIdsDescriptor query_d = new SpectrumIdsDescriptor(input_ids);
		
		return postForObject(ReferenceSpaceStruct.class, "spectrum", "getReferenceSpace", query_d);
		
	}
	
	
	/**
	 * Get a server descriptor that describes the server to which this client is connected.
	 * 
	 * @return a new server descriptor object
	 */
	public SPECCHIOServerDescriptor getServerDescriptor() {
		
		SPECCHIOWebAppDescriptor d = new SPECCHIOWebAppDescriptor(
				url.getProtocol(),
				url.getHost(),
				url.getPort(),
				url.getPath(),
				dataSourceName,
				uses_default_trust_store
			);
		if (user != null) {
			d.setUser(user);
		}
		
		return d;
		
	}
	
	
	/**
	 * Get all of the sensors in the database.
	 * 
	 * @return an array of Sensor objects
	 */
	public Sensor[] getSensors() throws SPECCHIOWebClientException {
		
		return getArray(Sensor.class, "instrumentation", "listSensors");
		
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
		
		
		GonioSamplingPoints g = postForObject(GonioSamplingPoints.class, "spectrum", "getGonioSamplingPoints", space); 
		
		return g;
	}
	
	
	/**
	 * Get the identifier of a sub-hierarchy with a given name, creating the
	 * hierarchy if it doesn't exist (deprecated)
	 * 
	 * @param campaign	the campaign into which to insert the hierarchy
	 * @param parent_id			the identifier of the the parent of the hierarchy
	 * @param name	the name of the desired hierarchy
	 * 
	 * @return the identifier of the child of parent_id with the name hierarchy_name
	 */
	public int getSubHierarchyId(Campaign campaign, String name, int parent_id) throws SPECCHIOClientException {
		
		return getInteger(
				"campaign", "getSubHierarchyId",
				"specchio", Integer.toString(campaign.getId()), Integer.toString(parent_id), name
			);
		
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
	public int getSubHierarchyId(String name, int parent_id) throws SPECCHIOClientException {
		
		return getInteger(
				"campaign", "getSubHierarchyId",
				"specchio", Integer.toString(parent_id), name
			);		
	}		
	


	/**
	 * Get the space objects for a set of spectrum identifiers.
	 * 
	 * @param ids								the spectrum identifiers
	 * @param split_spaces_by_sensor
	 * @param split_spaces_by_sensor_and_unit
	 * @param order_by							the field to order by
	 */
	public Space[] getSpaces(ArrayList<Integer> ids, boolean split_spaces_by_sensor, boolean split_spaces_by_sensor_and_unit, String order_by) throws SPECCHIOWebClientException {
		
		  SpaceQueryDescriptor space_d = new SpaceQueryDescriptor(
				  ids,
				  split_spaces_by_sensor,
				  split_spaces_by_sensor_and_unit,
				  order_by
			);
		  
		  return  postForArray(Space.class, "spectrum", "getSpaces", space_d);
		  
	}
	
	/**
	 * Get the space objects for a set of spectrum identifiers.
	 * Configured to sort by sensor, unit, instrument and calibration
	 * 
	 * @param ids								the spectrum identifiers
	 * @param order_by							the field to order by
	 */
	public Space[] getSpaces(ArrayList<Integer> ids, String order_by) throws SPECCHIOWebClientException {
		
		  SpaceQueryDescriptor space_d = new SpaceQueryDescriptor(
				  ids,
				  false,
				  false,
				  order_by
			);
		  
		  return  postForArray(Space.class, "spectrum", "getSpaces", space_d);
		  
	}
		
	
	
	/**
	 * Get a spectrum.
	 * 
	 * @param spectrum_id	the spectrum identifier
	 * @param load_metadata	load all spectrum metadata?
	 * 
	 * @return a Spectrum object
	 */
	public Spectrum getSpectrum(int spectrum_id, boolean load_metadata) throws SPECCHIOWebClientException {
		
		return getObject(Spectrum.class, "spectrum", "get", Integer.toString(spectrum_id), Boolean.toString(load_metadata));
	
	}
	
	
	/**
	 * Get the calibration spaces for a set of spectra.
	 * 
	 * @param spectrum_ids	the spectrum identifiers
	 * 
	 * @return an array of Space objects
	 */
	public Space[] getSpectrumCalibrationSpaces(ArrayList<Integer> spectrum_ids) throws SPECCHIOWebClientException {
		
		SpaceQueryDescriptor query_d = new SpaceQueryDescriptor(spectrum_ids);
		return postForArray(Space.class, "spectrum", "getCalibrationSpaces", query_d);
		
	}
	
	
	/**
	 * Get the spectrum factor table for a set of spectra.
	 * 
	 * @param spectrum_ids_1
	 * @param spectrum_ids_2
	 * 
	 * @return a SpectrumFactorTable object
	 */
	public SpectrumFactorTable getSpectrumFactorTable(ArrayList<Integer> spectrum_ids_1, ArrayList<Integer> spectrum_ids_2) throws SPECCHIOWebClientException {
		
		SpectrumIdsDescriptor factor_d = new SpectrumIdsDescriptor(
				spectrum_ids_1,
				spectrum_ids_2
			);
		
		return postForObject(SpectrumFactorTable.class, "spectrum", "getSpectrumFactorTable", factor_d);
		
	}
	
	
	/**
	 * Get the number of spectra in the database
	 * 
	 * @return the number of spectra in the database
	 */
	public int getSpectrumCountInDB() throws SPECCHIOClientException {
		return getInteger("spectrum", "countInDB");
	}		
	
	/**
	 * Get the identifiers of all spectra beneath a given node of the spectral data browser
	 * 
	 * @param sn	the node
	 * 
	 * @return a list of spectrum identifiers
	 */
	public List<Integer> getSpectrumIdsForNode(spectral_node_object sn) throws SPECCHIOWebClientException {
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		return adapter.unmarshalList(postForList(XmlInteger.class, "browser", "spectrum_ids", sn));
				
	}
	
	
	/**
	 * Get the identifiers of all spectra that match a full text search.
	 * 
	 * @param search_str		the search string
	 * 
	 * @return an array list of spectrum identifiers
	 */
	public ArrayList<Integer> getSpectrumIdsMatchingFullTextSearch(String search_str) throws SPECCHIOClientException {
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		XmlString xmlstr = new XmlString();
		xmlstr.setString("%" + search_str + "%");
		
		List<Integer> id_array = adapter.unmarshalList(postForList(XmlInteger.class, "spectrum", "full_text_search", xmlstr));

		ArrayList<Integer> ids = new ArrayList<Integer>();
		
		ids.addAll(id_array);
		return ids;
	}

	/**
	 * Get the identifiers of all spectra that match a full text search using metadata hierarchy.
	 * 
	 * @param search_str		the search string
	 * 
	 * @return an array list of spectrum identifiers
	 */
	public ArrayList<Integer> getSpectrumIdsMatchingFullTextSearchUsingHierarchy(String search_str) throws SPECCHIOClientException {
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		XmlString xmlstr = new XmlString();
		xmlstr.setString("%" + search_str + "%");
		
		List<Integer> id_array = adapter.unmarshalList(postForList(XmlInteger.class, "spectrum", "full_text_search_using_hierarchy", xmlstr));

		ArrayList<Integer> ids = new ArrayList<Integer>();
		
		ids.addAll(id_array);
		return ids;
	}
	

	/**
	 * Get the identifiers of all spectra that match a full text search.
	 *
	 * @param campaignId		the search string
	 *
	 * @return an array list of spectrum identifiers
	 */
	public ArrayList<Integer> getUnprocessedHierarchies(String campaignId) throws SPECCHIOClientException{
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		XmlString xmlstr = new XmlString();
		xmlstr.setString(campaignId);

		List<Integer> id_array = adapter.unmarshalList(postForList(XmlInteger.class, "spectrum", "unprocessed_hierarchies", xmlstr));

		ArrayList<Integer> ids = new ArrayList<Integer>();

		ids.addAll(id_array);
		return ids;
	}

	@Override
	public ArrayList<Integer> getIrradiance(String campaignId) throws SPECCHIOClientException {
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		XmlString xmlstr = new XmlString();
		xmlstr.setString(campaignId);

		List<Integer> id_array = adapter.unmarshalList(postForList(XmlInteger.class, "spectrum", "irradiance_spectra", xmlstr));

		ArrayList<Integer> ids = new ArrayList<Integer>();

		ids.addAll(id_array);
		return ids;
	}


	/**
	 * Get the spectrum identifiers that match a given query.
	 * 
	 * @param query	the query
	 * 
	 * @return an array list of spectrum identifiers that match the query
	 */
	public ArrayList<Integer> getSpectrumIdsMatchingQuery(Query query) throws SPECCHIOWebClientException {

		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		Integer[] id_array = adapter.unmarshalArray(postForArray(XmlInteger.class, "spectrum", "query", query));
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int id : id_array) {
			ids.add(id);
		}		
		
		return ids;
	}
	
	
	/**
	 * Get the identifiers of all spectra directly belonging to a hierarchy
	 * 
	 * @param hierarchy_id	the hierarchy_id
	 * 
	 * @return a list of spectrum identifiers
	 */
	public ArrayList<Integer> getDirectSpectrumIdsOfHierarchy(int hierarchy_id) throws SPECCHIOClientException{
		
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		List<Integer> ids_ = adapter.unmarshalList(postForList(XmlInteger.class, "spectrum", "getDirectSpectrumIdsOfHierarchy", Integer.toString(hierarchy_id)));		
		
		ArrayList<Integer> ids = new ArrayList<Integer>();
		
		ids.addAll(ids_);
		return ids;
	}	
	
	
	/**
	 * Get the pictures associated with a spectrum.
	 * 
	 * @param spectrum_id	the spectrum identifier
	 * 
	 * @return a PictureTable containing all of the pictures associated with the spectrum
	 */
	public PictureTable getSpectrumPictures(int spectrum_id) throws SPECCHIOWebClientException {
		
		return getObject(PictureTable.class, "spectrum", "pictures", Integer.toString(spectrum_id));
		
	}
	
	
	/**
	 * Get the number of spectra that matach a given query.
	 * 
	 * @param query	the query
	 * 
	 * @return the number of spectra that match the query
	 */
	public int getSpectrumQueryCount(Query query) throws SPECCHIOWebClientException {
		
		return postForInteger("spectrum", "queryCount", query);
		
	}
	
	
	/**
	 * Get the spectrum data links that refer to a given set of targets and/or references.
	 * 
	 * @param target_ids	the identifiers of the target spectra (null or empty to match all targets)
	 * @param reference_ids	the identifiers of the reference spectra (null or empty to match all references)
	 * 
	 * @return an array of SpectrumDataLink objects
	 */
	public SpectrumDataLink[] getTargetReferenceLinks(ArrayList<Integer> target_ids, ArrayList<Integer> reference_ids) throws SPECCHIOClientException {
		
		return postForArray(SpectrumDataLink.class, "spectrum", "getTargetReferenceLinks", new SpectrumIdsDescriptor(target_ids, reference_ids));
		
	}
		
	
	/**
	 * Get the top node for a given taxonomy
	 * 
	 * @param attribute_id	id of the attribute that defines the taxonomy
	 */
	public TaxonomyNodeObject getTaxonomyRootNode(int attribute_id) throws SPECCHIOWebClientException {				
		return getObject(TaxonomyNodeObject.class, "metadata", "get_taxonomy_root", Integer.toString(attribute_id));		
	}
	
	/**
	 * Get the node for a given taxonomy
	 * 
	 * @param taxonomy_id	taxonomy_id that defines the taxonomy
	 */
	public TaxonomyNodeObject getTaxonomyNode(int taxonomy_id) throws SPECCHIOWebClientException {
		
		return getObject(TaxonomyNodeObject.class, "metadata", "get_taxonomy_object", Integer.toString(taxonomy_id));
	}
	
	/**
	 * Get the id for a given taxonomy node in a given taxonomy
	 * 
	 * @param attribute_id	attribute_id that defines the taxonomy
	 *  @param name		name of the node of which the id is required
	 * 
	 */
	public int getTaxonomyId(int attribute_id, String name)  throws SPECCHIOClientException {
		return 0; 
	}
	
	
	/**
	 * Get the taxonomy hash for a given taxonomy
	 * 
	 * @param attribute_id	attribute_id that defines the taxonomy
	 * 
	 */
	public Hashtable<String, Integer> getTaxonomyHash(int attribute_id)  throws SPECCHIOClientException {
		
		Taxonomy t = getObject(Taxonomy.class, "metadata", "get_taxonomy",Integer.toString( attribute_id));
		
		return t.getHashtable();
		
	}
	
	/**
	 * Get the ID to name hash for a given taxonomy
	 * 
	 * @param attribute_id	attribute_id that defines the taxonomy
	 * 
	 */
	public Hashtable<Integer, String> getTaxonomyIdToNameHash(int attribute_id)  throws SPECCHIOClientException {
		
		Taxonomy t = getObject(Taxonomy.class, "metadata", "get_taxonomy",Integer.toString( attribute_id));
		
		return t.getIdToNameHashtable();
		
	}
	
		
	
	
	/**
	 * Import a campaign.
	 * 
	 * @param user_id	the identifier of the user to whom the campaign will belong
	 * @param is		the input stream from which to read the campaign
	 */
	public void importCampaign(int user_id, InputStream is) throws SPECCHIOWebClientException {
		
		postInputStream(is, "campaign", "import", "specchio", Integer.toString(user_id));
		
	}
	
	/**
	 * Import a campaign from a file that is on the Glassfish server (used for bigger files to prevent timeouts)
	 * 
	 * @param user_id	the identifier of the user to whom the campaign will belong
	 * @param server_filepath		the server filepath from which to read the campaign
	 */
	public void importCampaign(int user_id, String server_filepath) throws SPECCHIOWebClientException {
		
		SpecchioCampaign c = new SpecchioCampaign();
		user = new User();
		user.setUserId(user_id);
		c.setUser(user);
		c.setPath(server_filepath);
		
		postForInteger("campaign", "import_from_server_file", c);
	}
	
	
	
	/**
	 * Get a list of all of the users in the database.
	 * 
	 * @return an array of User objects
	 * 
	 * @throws SPECCHIOClientException
	 */
	public User[] getUsers() throws SPECCHIOWebClientException {
		
		return getArray(User.class, "user", "list");
		
	}
	
	
	/**
	 * Get a list of all of the users in the database with added user statistics (number of loaded spectra, number of campaigns).
	 * 
	 * @return an array of User objects
	 * 
	 * @throws SPECCHIOClientException
	 */
	public User[] getUsersWithStatistics() throws SPECCHIOWebClientException {
		
		return getArray(User.class, "user", "getUsersWithStatistics");
		
	}	
	
	/**
	 * Insert a new campaign into the database
	 * 
	 * @param campaign	the campaign
	 * 
	 * @return the identifier of the new campaign
	 */
	public int insertCampaign(Campaign campaign) throws SPECCHIOWebClientException {
		
		return postForInteger("campaign", "insert", campaign);
		
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
	public int insertHierarchy(Campaign campaign, String name, int parent_id) throws SPECCHIOWebClientException {
		
		return getInteger(
				"campaign", "insertHierarchy",
				"specchio", Integer.toString(campaign.getId()), name, Integer.toString(parent_id)
			);
		
	}
	
	
	/**
	 * Insert a new institute into the database
	 * 
	 * @param institute	an Institute object describing the new institute
	 * 
	 * @return the identifier of the new institute
	 */
	public int insertInstitute(Institute institute) throws SPECCHIOClientException {
		
		return postForInteger("public", "insertInstitute", institute);
		
	}
	
	
	/**
	 * Insert calibration for an instrument.
	 * 
	 * @param c		the calibration data
	 */
	public void insertInstrumentCalibration(Calibration c) throws SPECCHIOWebClientException {
		
		postForString("instrumentation", "insertInstrumentCalibration", c);
		
	}
	
	/**
	 * 
	 * Get instrument node for a given instrument node id
	 * 
	 * @param an instrument node id
	 * 
	 * @return an InstrumentNode
	 * 
	 */
	
	public InstrumentNode getInstrumentNode(int instrument_node_id) throws SPECCHIOWebClientException {
		
		return getObject(InstrumentNode.class, "uncertainty", "getInstrumentNode", Integer.toString(instrument_node_id));
	}
	

	 
	/**
	 * Insert instrument node.
	 * 
	 * @param an InstrumentNode object
	 * 
	 * @return the id of the new instrument node
	 * 
	 */
	
	public int insertInstrumentNode(InstrumentNode instrument_node) throws SPECCHIOClientException {
		
		return postForInteger("uncertainty", "insertInstrumentNode", instrument_node);
		
	}
	
	
	/**
	 * Insert a picture of an instrument into the database.
	 * 
	 * @param picture	the picture
	 */
	public void insertInstrumentPicture(Picture picture) throws SPECCHIOWebClientException {
		
		postForInteger("instrumentation", "insertInstrumentPicture", picture);
		
	}
	
	
	/**
	 * Insert calibration for a reference into the database.
	 * 
	 * @param c		the calibration data
	 */
	public void insertReferenceCalibration(Calibration c) throws SPECCHIOWebClientException {
		
		postForString("instrumentation", "insertReferenceCalibration", c);
		
	}
	
	
	/**
	 * Insert a picture associated with a reference into the database.
	 * 
	 * @param picture	the picture
	 */
	public void insertReferencePicture(Picture picture) throws SPECCHIOWebClientException {
		
		postForInteger("instrumentation", "insertReferencePicture", picture);
		
	}
	
	
	/**
	 * Insert a spectral file into the database.
	 * 
	 * @param spec_file	the file
	 * 
	 * @return a list of spectrum identifiers that were inserted into the database
	 */
	public SpectralFileInsertResult insertSpectralFile(SpectralFile spec_file) throws SPECCHIOClientException {
		
		SpectralFileInsertResult insert_result = null;
		try {
			insert_result = postForObject(SpectralFileInsertResult.class, "spectral_file", "insert", spec_file);
		} catch (SPECCHIOWebClientException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println(e.getDetails());
			throw e;
		}
		return insert_result;
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
		
		return postForInteger("spectrum", "insertClosestTargetReferenceLink", new SpectrumIdsDescriptor(target_id, reference_ids));
		
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
		
		return postForInteger("spectrum", "insertTargetReferenceLinks", new SpectrumIdsDescriptor(target_id, reference_ids));
		
	}
	
	
	
	/**
	 * Insert an uncertainty node
	 * 
	 * @param spectral_set a spectral set
	 * @return the id of the created node
	 * 
	 * @throws SPECCHIOClientException
	 */
	
	public int insertUncertaintyNode(SpectralSet spectral_set) throws SPECCHIOClientException {
		
		SpectralSetDescriptor ssd = new SpectralSetDescriptor(spectral_set.uncertainty_source_pairs, spectral_set.uncertainty_source_ids, spectral_set.spectrum_ids, spectral_set);
		
		return postForInteger("uncertainty", "insertUncertaintyNode", ssd);
		
	}
 	
	/**
	 * 
	 * Insert new uncertainty set
	 * 
	 * @param spectral_set a spectral set with an uncertainty_set_description
	 * 
	 * @return the id of the created uncertainty set
	 * 
	 * @throws SPECCHIOClientException
	 * 
	 */
	
	public int insertNewUncertaintySet(SpectralSet spectral_set) throws SPECCHIOClientException {
		
		return postForInteger("uncertainty", "insertNewUncertaintySet", spectral_set);
		
		
	}
	
	
	
	
	/**
	 * Test for the existence of a calibration in the database.
	 * 
	 * @param cal		calibration object to check
	 * 
	 * @return true if the calibration already exists in the database, false otherwise
	 */
	public boolean instrumentCalibrationExists(Calibration cal) throws SPECCHIOWebClientException {

		return postForBoolean("instrumentation", "instrument_calibration_exists", cal);
		
	}		


	/**
	 * Test whether or not the web client is logged in under a given role.
	 * 
	 * @param roleName	the role to be tested
	 * 
	 * @return "true" if the web client is logged in as a user with role roleName, "false" otherwise
	 */
	public boolean isLoggedInWithRole(String roleName) throws SPECCHIOWebClientException {
		
		return (user != null)? user.isInRole(roleName) : false;
		
	}
	
	
	/**
	 * Get the meta-parameter of the given metaparameter identifier.
	 * 
	 * @param metaparameter_id		the metaparameter identifier for which to retrieve metadata
	 * 
	 * @return the meta-parameter object corresponding to the desired id
	 *
	 * @throws SPECCHIOClientException
	 */
	public MetaParameter loadMetaparameter(int metaparameter_id) throws SPECCHIOClientException {		
		
		return getObject(MetaParameter.class, "metadata", "load_metaparameter", Integer.toString(metaparameter_id));	
	}
	
	
	
	/**
	 * Load a sensor definition into the database from an input stream.
	 * 
	 * @param is	the input stream
	 */
	public void loadSensor(InputStream is) throws SPECCHIOWebClientException {
		
		postInputStream(is, "instrumentation", "loadSensor");
		
	}
	
	
	/**
	 * Load a Space object.
	 * 
	 * @param space	a partially-filled space object
	 * 
	 * @return a complete Space object
	 */
	public Space loadSpace(Space space) throws SPECCHIOWebClientException {
		
		return postForObject(Space.class, "spectrum", "loadSpace", space);
		
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
				
		return postForBoolean("campaign", "moveHierarchy", new ChildParentIdContainer(source_hierarchy_id, target_parent_hierarchy));		
		
	}
	
	
	
	/**
	 * Causes the client to reload data values the specified category upon next request.
	 * 
	 * @param field name
	 * 
	 */	
	public void refreshMetadataCategory(String field) {
		
		// should always be handled by the client cache
		
	}	
	
	
	/**
	 * Remove an item of EAV metadata.
	 * 
	 * @param mp	the meta-parameter to be removed
	 */
	public void removeEavMetadata(MetaParameter mp) throws SPECCHIOWebClientException {
		
		postForInteger("metadata", "remove", new MetadataUpdateDescriptor(mp));
		
	}
	
	
	/**
	 * Remove an item of EAV metadata for a collection of spectra.
	 * 
	 * @param mp			the meta-parameter to be removed
	 * @param spectrum_ids	the spectrum identifiers
	 */
	public void removeEavMetadata(MetaParameter mp, ArrayList<Integer> spectrum_ids) throws SPECCHIOWebClientException {
		
		postForInteger("metadata", "remove", new MetadataUpdateDescriptor(mp, spectrum_ids));
		
	}
	
	
	/**
	 * Remove one or more items of EAV metadata for a collection of spectra for a defined attribute.
	 * 
	 * @param attr			the attribute to be removed
	 * @param spectrum_ids	the spectrum identifiers
	 * @param metadata_level		storage level identifier
	 */	
	public void removeEavMetadata(attribute attr, ArrayList<Integer> spectrum_ids, int metadata_level) throws SPECCHIOClientException
	{
		MetaParameter mp = MetaParameter.newInstance(attr);
		mp.setLevel(metadata_level);
		
		MetadataUpdateDescriptor md = new MetadataUpdateDescriptor(mp, spectrum_ids);
		md.setLevel(metadata_level);
		
		postForInteger("metadata", "remove_metaparameters_of_given_attribute", md);
				
	}
	
	/**
	 * Rename a hierarchy in the database and also on the file system if the path is accessible.
	 * The rename on the database will only be applied if a rename on the file system succeeded.
	 * 
	 * @param hierarchy_id	id of the hierarchy to be renamed
	 * @param name	new name of the hierarchy
	 * @return true if and only if the renaming succeeded; false otherwise
	 */	
	public boolean renameHierarchy(int hierarchy_id, String name) throws SPECCHIOClientException
	{
		boolean success = false;
		// rename the folder on the file system
		String path = getHierarchyFilePath(hierarchy_id);
		//path = "/Volumes/apexdata/APEX2013/_VAL_Data/GPNP_2013-08-31/MM077_GPNP__130831_spectra/s1";
		File f = new File(path);
		
		if(f.exists())
		{
			File new_name = new File(f.getParentFile().getPath() + File.separator + name);
			success = f.renameTo(new_name);
			if (success)
			{
				getInteger("campaign", "renameHierarchy", Integer.toString(hierarchy_id), name);
			}
		}
		else
		{
			String error_msg = "Error: hierarchy renaming can only be carried out on a machine where the specified path is accessible:\n" + 
					path + "\n";
			System.out.print(error_msg);
			throw new SPECCHIOWebClientException(error_msg);						
		}
		
		return success;
			
	}	
	
	
	/**
	 * Set the progress report interface to which progress made by this
	 * client will be reported.
	 * 
	 * @param pr	the progress report; use null to report no progress
	 */
	public void setProgressReport(ProgressReportInterface pr) {
		
		this.pr = pr;
		
	}
	
	
	
	/**
	 * Remove the data corresponding to a node of the spectral data browser.
	 * 
	 * @param sn	the node to be removed
	 */
	public void removeSpectralNode(spectral_node_object sn) throws SPECCHIOWebClientException {
		
		if(sn instanceof campaign_node)
		{
			getInteger("campaign", "remove", "specchio", Integer.toString(sn.getId()));
		}	
		if(sn instanceof hierarchy_node)
		{
			getInteger("campaign", "removeHierarchy", "specchio", Integer.toString(sn.getId()), sn.getName());
		}
		if(sn instanceof spectrum_node)
		{
			getInteger("spectrum", "remove", Integer.toString(sn.getId()));		
		}
		
	}
	
	/**
	 * Remove the data corresponding to a node of the spectral data browser.
	 * 
	 * @param sns	list of nodes to be removed
	 */
	public void removeSpectralNodes(ArrayList<spectral_node_object> sns) throws SPECCHIOWebClientException {
		
		// build lists per node type
		ArrayList<Integer> campaign_ids = new ArrayList<Integer>();
		ArrayList<Integer> hierarchy_ids = new ArrayList<Integer>();
		ArrayList<Integer> spectrum_ids = new ArrayList<Integer>();
		
		ListIterator<spectral_node_object> li = sns.listIterator();
		
		while (li.hasNext())
		{
			spectral_node_object sn = li.next();
			
			if(sn instanceof campaign_node)
			{
				campaign_ids.add(sn.getId());
			}	
			if(sn instanceof hierarchy_node)
			{
				hierarchy_ids.add(sn.getId());
			}
			if(sn instanceof spectrum_node)
			{
				spectrum_ids.add(sn.getId());	
			}			
			
		}
		
		// use SpectrumIdsDescriptor as container to send ids ....
		if (campaign_ids.size()>0)
		{
			SpectrumIdsDescriptor d = new SpectrumIdsDescriptor(campaign_ids);
			postForInteger("campaign", "removeCampaigns", d);
		}
		
		if (hierarchy_ids.size()>0)
		{
			SpectrumIdsDescriptor d = new SpectrumIdsDescriptor(hierarchy_ids);
			postForInteger("campaign", "removeHierarchies", d);
		}
		
		if (spectrum_ids.size()>0)
		{
			SpectrumIdsDescriptor d = new SpectrumIdsDescriptor(spectrum_ids);
			postForInteger("spectrum", "removeSpectra", d);
		}		
		
	}	
	
	
	/**
	 * Sort spectra by the values of the specified attributes
	 * 
	 * @param spectrum_ids	list of ids to sort
	 * @param attribute_names	attribute names to sort by
	 * 
	 * @return a AVMatchingListCollection object (Attention: order of spectrum ids is not guaranteed!)
	 */
	public AVMatchingListCollection sortByAttributes(ArrayList<Integer> spectrum_ids, String... attribute_names) throws SPECCHIOClientException {
		
		
		AVMatchingList av_list = new AVMatchingList(spectrum_ids, attribute_names);
		
		return postForObject(AVMatchingListCollection.class, "spectrum", "sortByAttributes", av_list);
		
	}
	
	
	
	/**
	 * Test for the existence of a spectral file in the database.
	 * 
	 * @param spec_file		spectral file object to check
	 * 
	 * @return true if the file already exists in the database, false otherwise
	 */
	public boolean spectralFileExists(SpectralFile spec_file) throws SPECCHIOWebClientException {

		return postForBoolean("spectral_file", "exists", spec_file);
		
	}
	
	/**
	 * Test for the existence of a spectral files in the database.
	 * 
	 * @param spec_files	container with arraylist of spectral files to check
	 * 
	 * @return array of boolean values indicating existence
	 */
	public boolean[] spectralFilesExist(SpectralFiles spec_files) throws SPECCHIOClientException {
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		
		XmlInteger[] tmp = postForArray(XmlInteger.class, "spectral_file", "exist", spec_files);
		
		Integer[] exists_array_ = adapter.unmarshalArray(tmp);
		
		boolean[] exists_array = new boolean[exists_array_.length];
		
		for(int i=0;i<exists_array_.length;i++)
		{
			exists_array[i] = exists_array_[i] == 1;
		}
		
		return exists_array;
		
	}
	
		
	
	
	/**
	 * Submit a collection to Research Data Australia.
	 * 
	 * @param collection_d	the collection descriptor
	 * 
	 * @return the collection identifier of the new string, or an empty string if publication failed
	 * 
	 * @throws SPECCHIOClientException	could not contact the server
	 */
	public String submitRDACollection(RDACollectionDescriptor collection_d) throws SPECCHIOWebClientException {
		
		return postForString("ands", "submitCollection", collection_d);
		
	}
	
	
	
	/**
	 * Update the information about a campaign
	 * 
	 * @param campaign	the new campaign data
	 */
	public void updateCampaign(Campaign campaign) throws SPECCHIOClientException {
		
		postForString("campaign", "update", campaign);
		
	}
	
	
	/**
	 * Update EAV metadata.
	 * 
	 * @param mp			the meta-parameter to update
	 * @param spectrum_ids	the identifiers for which to update the parameter
	 * 
	 * @return the identifier of the inserted metadata
	 */
	public int updateEavMetadata(MetaParameter mp, ArrayList<Integer> spectrum_ids) throws SPECCHIOWebClientException {
		
		int eav_id = postForInteger("metadata", "update", new MetadataUpdateDescriptor(mp, spectrum_ids));
		if (mp.getEavId() == 0) {
			mp.setEavId(eav_id);
		}
		
		return eav_id;
		
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
	public int updateEavMetadata(MetaParameter mp, ArrayList<Integer> spectrum_ids, MetaParameter mp_old) throws SPECCHIOWebClientException {
		
		int eav_id = postForInteger("metadata", "update", new MetadataUpdateDescriptor(mp, spectrum_ids, mp_old));
		if (mp.getEavId() == 0) {
			mp.setEavId(eav_id);
		}
		
		return eav_id;
		
	}
	
	/**
	 * Update EAV metadata annotation.
	 * 
	 * @param mp			the meta-parameter to update
	 * @param spectrum_ids	the identifiers for which to update the parameter
	 * 
	 * @return the identifier of the inserted metadata
	 */
	public int updateEavMetadataAnnotation(MetaParameter mp, ArrayList<Integer> spectrum_ids) throws SPECCHIOWebClientException {
		
		MetaParameter mp_ = MetaParameter.newInstance();
		mp_.setEavId(mp.getEavId());
		mp_.setAnnotation(mp.getAnnotation());
		
		
		int eav_id = postForInteger("metadata", "update_annotation", new MetadataUpdateDescriptor(mp_, spectrum_ids));

		
		return eav_id;
		
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
		
		int eav_id = postForInteger("metadata", "update_or_insert", new MetadataUpdateDescriptor(mp, spectrum_ids));
		if (mp.getEavId() == 0) {
			mp.setEavId(eav_id);
		}
		
		return eav_id;
		
	}

	/**
	 * Update or insert EAV metadata. Will automatically update existing entries or insert a new metaparameter if not existing.
	 *
	 * @param mp			the meta-parameter to update or insert
	 * @param spectrum_id	the identifiers for which to update or insert the parameter
	 *
	 * @return the identifier of the inserted or updated metadata
	 */
	public int updateOrInsertEavMetadata(MetaParameter mp, int spectrum_id) throws SPECCHIOWebClientException {
		ArrayList<Integer> specId = new ArrayList<>();
		specId.add(spectrum_id);
		int eav_id = postForInteger("metadata", "update_or_insert", new MetadataUpdateDescriptor(mp, specId));
		if (mp.getEavId() == 0) {
			mp.setEavId(eav_id);
		}

		return eav_id;

	}

	/**
	 * Update or insert EAV metadata. Will automatically update existing entries or insert a new metaparameter if not existing.
	 *
	 * @param md			the meta-parameter to update or insert
	 *
	 * @return the identifier of the inserted or updated metadata
	 */
	public int updateOrInsertEavMetadata(ArrayList<Metadata> md, ArrayList<Integer> ids, int campaignId) throws SPECCHIOWebClientException {
		MetadataUpdateDescriptor mud = new MetadataUpdateDescriptor(md, ids, campaignId);
		int result = postForInteger("metadata", "update_or_insert_many", mud);
		return result;
	}

	/**
	 * Update an instrument.
	 * 
	 * @param instrument	the instrument
	 */
	public void updateInstrument(Instrument instrument) throws SPECCHIOWebClientException {
		
		postForString("instrumentation", "update", instrument);
		
	}
	
	
	/**
	 * Update the calibration metadata for an instrument.
	 * 
	 * @param cm	the calibration metadata
	 */
	public void updateInstrumentCalibrationMetadata(CalibrationMetadata cm) throws SPECCHIOWebClientException {
		
		postForString("instrumentation", "updateCalibrationMetadata", cm);
		
	}
	
	
	/**
	 * Update a picture associated with an instrument.
	 * 
	 * @param picture	the picture
	 */
	public void updateInstrumentPicture(Picture picture) throws SPECCHIOClientException {
		
		postForString("instrumentation", "updateInstrumentPicture", picture);
		
	}
	
	
	/**
	 * Update an reference.
	 * 
	 * @param reference	the reference
	 */
	public void updateReference(Reference reference) throws SPECCHIOWebClientException {
		
		postForString("instrumentation", "updateReference", reference);
		
	}
	
	
	/**
	 * Update the calibration metadata for a reference.
	 * 
	 * @param cm	the calibration metadata
	 */
	public void updateReferenceCalibrationMetadata(CalibrationMetadata cm) throws SPECCHIOWebClientException {
		
		postForString("instrumentation", "updateCalibrationMetadata", cm);
		
	}
	
	
	/**
	 * Update a picture associated with a reference.
	 * 
	 * @param picture	the picture
	 */
	public void updateReferencePicture(Picture picture) throws SPECCHIOClientException {
		
		postForString("instrumentation", "updateReferencePicture", picture);
		
	}
	
	
	/**
	 * Update the metadata fields for a set of spectra
	 * 
	 * @param ids	the spectrum identifiers
	 * @param field			the name of the field to be updated
	 * @param id
	 */
	public void updateSpectraMetadata(ArrayList<Integer> ids, String field, int id) throws SPECCHIOWebClientException {
		
		postForInteger("spectrum", "update_metadata", new SpectraMetadataUpdateDescriptor(ids, field, id));
		
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
		
		Spectrum s = new Spectrum();
		
		Float[] vector_ = new Float[vector.length];
		
		for (int i=0; i<vector.length; i++)
		{
			vector_[i] = vector[i];
		}				
		s.setMeasurementVector(vector_);
		s.setSpectrumId(spectrum_id);
		
		postForInteger("spectrum", "update_vector", s);
		
	}

	/**
	 * Update the spectral vector of a spectrum
	 *
	 * @param updateMap	a map containing the spectrum_id as key and the vector as value
	 *
	 * @throws SPECCHIOClientException
	 */
	public void updateSpectrumVectors(HashMap<Integer, double[]> updateMap) throws SPECCHIOClientException {

		Spectrum[] spectra = new Spectrum[updateMap.size()];
		int count = 0;
		for(Integer specId : updateMap.keySet()){
			Spectrum s = new Spectrum();
			double[] vector = updateMap.get(specId);
			Float[] vector_ = new Float[vector.length];

			for (int i=0;i<vector.length;i++)
			{
				vector_[i] = (float) (vector[i]);
			}
			s.setMeasurementVector(vector_);
			s.setSpectrumId(specId);
			spectra[count] = s;
			count++;
		}

		postForInteger("spectrum", "update_vectors", spectra);

//		return ids;

//		postForInteger("spectrum", "update_vector", s);

	}
	
	/**
	 * Update the information about a user.
	 * 
	 * @param newUser	the user data
	 * 
	 * @throws SPECCHIOClientException
	 */
	public void updateUser(User newUser) throws SPECCHIOClientException {
		
		postForString("user", "update", newUser);
		if (user.getUsername().equals(newUser.getUsername())) {
			// update the logged-in user object
			user = new User(newUser);
		}
		
	}


	/**
	 * Build the path to a service
	 * 
	 * @param service	the service name
	 * @param method	the service method
	 * @param args		the service arguments
	 * 
	 * @return the path name of the service call corresponding to the input data
	 */
	private String buildPath(String service, String method, String ... args) {
		
		// start with service then method
		StringBuffer path = new StringBuffer(service + "/" + method);
		
		// add the arguments
		for (String arg : args) {
			path.append("/" + arg);
		}
		
		return path.toString();
		
	}
	
	
	/**
	 * Get web resource builder with cookie for data source
	 * 
	 * @param service	the service name
	 * @param method	the service method
	 * @param args		the service arguments
	 * 
	 * @return builder for this web resource, including a cookie stating the requested data source
	 */
	private WebResource.Builder getWRBuilder(String service, String method, String ... args) {
		
		WebResource wr = web_service.path(buildPath(service, method, args));
		Cookie cookie = new Cookie("DataSourceName", dataSourceName);
		
		WebResource.Builder builder = wr.cookie(cookie);
		
//		ConsumerCredentials consumerCredentials = new ConsumerCredentials(user.getUsername(), user.getPassword());
//		
//		OAuth1AuthorizationFlow authFlow = OAuth1ClientSupport.builder(consumerCredentials);

		return builder;
	}	


	/**
	 * Get an array of objects from a web service.
	 * 
	 * @param objectClass	the type of objects to be in the array
	 * @param service		the service name
	 * @param method		the service method
	 * @param args			the service arguments
	 * 
	 * @return the return value of the service call
	 * @throws SPECCHIOWebClientException
	 */
	@SuppressWarnings("unchecked")
	private <T> T[] getArray(Class<? extends T> objectClass, String service, String method, String ... args) throws SPECCHIOWebClientException {
		
		try {
			// get the array class
			Class<?> arrayClass = Array.newInstance(objectClass, 0).getClass();

			return (T[]) getWRBuilder(service, method, args).accept(MediaType.APPLICATION_XML).get(arrayClass);
			
		}
		catch (UniformInterfaceException ex) {
			// could represent any kind of HTTP error
			throw new SPECCHIOWebClientException(ex);
		}
		catch (ClientHandlerException ex) {
			// could represent any kind of network error
			throw new SPECCHIOWebClientException(ex);
		}
		
	}
	
	
	/**
	 * Get an input stream that reads from the body a web service's response.
	 * 
	 * @param service	the service name
	 * @param method	the service method
	 * @param args		the service arguments
	 * 
	 * @return an InputStream object that reads from the body of the service's response
	 * @throws SPECCHIOWebClientException 
	 */
	private InputStream getInputStream(String service, String method, String ... args) throws SPECCHIOWebClientException {
		
		try {
			Builder builder = getWRBuilder(service, method, args);
			Builder x = builder.accept(MediaType.APPLICATION_OCTET_STREAM);
			ClientResponse response = x.get(ClientResponse.class);
			if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
				throw new ClientHandlerException(
					web_service.path(buildPath(service, method, args)) +
					" returned a response status of " + 
					response.getStatus() + " " + response.getClientResponseStatus()
					);
			}
		
			return response.getEntityInputStream();
			
		}
		catch (UniformInterfaceException ex) {
			// could represent any kind of HTTP error
			throw new SPECCHIOWebClientException(ex);
		}
		catch (ClientHandlerException ex) {
			// could represent any kind of network error
			throw new SPECCHIOWebClientException(ex);
		}
		
	}
	
	
	/**
	 * Get an integer from a web service.
	 * 
	 * @param service	the service name
	 * @param method	the service method
	 * @param args		the service arguments
	 * 
	 * @return the return value of the service call
	 * @throws SPECCHIOWebClientException 
	 */
	private Integer getInteger(String service, String method, String ... args) throws SPECCHIOWebClientException {
		
		try {
			return getWRBuilder(service, method, args).accept(MediaType.APPLICATION_XML).get(XmlInteger.class).getInteger();
			
		}
		catch (UniformInterfaceException ex) {
			// could represent any kind of HTTP error
			throw new SPECCHIOWebClientException(ex);
		}
		catch (ClientHandlerException ex) {
			// could represent any kind of network error
			throw new SPECCHIOWebClientException(ex);
		}
		
	}
	
	/**
	 * Get an integer from a web service.
	 * 
	 * @param service	the service name
	 * @param method	the service method
	 * 
	 * @return the return value of the service call
	 * @throws SPECCHIOWebClientException 
	 */
	private Integer getInteger(String service, String method) throws SPECCHIOWebClientException {
		
		try {
			return getWRBuilder(service, method).accept(MediaType.APPLICATION_XML).get(XmlInteger.class).getInteger();
			
		}
		catch (UniformInterfaceException ex) {
			// could represent any kind of HTTP error
			throw new SPECCHIOWebClientException(ex);
		}
		catch (ClientHandlerException ex) {
			// could represent any kind of network error
			throw new SPECCHIOWebClientException(ex);
		}
		
	}	


	/**
	 * Get an object from a web service.
	 * 
	 * @param objectClass	the class of the objects that will be returned
	 * @param service		the service name
	 * @param method		the service method
	 * @param args			the service arguments
	 * 
	 * @return the return value of the service call
	 * @throws SPECCHIOWebClientException 
	 */
	private <T> T getObject(Class<? extends T> objectClass, String service, String method, String ... args) throws SPECCHIOWebClientException {
		
		try {
			
//			WebResource wr = web_service.path(buildPath(service, method, args));
//			Cookie cookie = new Cookie("DataSourceName", "jdbc/specchio");
//			
//			builder = wr.cookie(cookie);

			//return web_service.path(buildPath(service, method, args)).accept(MediaType.APPLICATION_XML).get(objectClass);
			
			return getWRBuilder(service, method, args).accept(MediaType.APPLICATION_XML).get(objectClass);
			
		}
		catch (UniformInterfaceException ex) {
			// could represent any kind of HTTP error
			throw new SPECCHIOWebClientException(ex);
		}
		catch (ClientHandlerException ex) {
			// could represent any kind of network error
			throw new SPECCHIOWebClientException(ex);
		}
		catch (javax.ws.rs.WebApplicationException ex) {
			// could represent any kind of HTTP error
			throw new SPECCHIOWebClientException(ex);
		}
		
		
		
	}
	
	
	/**
	 * Get a string from a web service.
	 * 
	 * @param service	the service name
	 * @param method	the service method
	 * @param args		the service arguments
	 * 
	 * @return the return value of the service call
	 * @throws SPECCHIOWebClientException 
	 */
	private String getString(String service, String method, String ... args) throws SPECCHIOWebClientException {
		
		try {
			return getWRBuilder(service, method, args).accept(MediaType.APPLICATION_XML).get(String.class);
			
		}
		catch (UniformInterfaceException ex) {
			// could represent any kind of HTTP error
			throw new SPECCHIOWebClientException(ex);
		}
		catch (ClientHandlerException ex) {
			// could represent any kind of network error
			throw new SPECCHIOWebClientException(ex);
		}
		
	}
	
	
	
	/**
	 * Post an object to a web service that returns an array of objects.
	 * 
	 * @param objectClass	the type of objects to be in the array
	 * @param service		the service name
	 * @param method		the service method
	 * @param arg			the object to be posted
	 * 
	 * @return the return value of the service call
	 * @throws SPECCHIOWebClientException 
	 */
	@SuppressWarnings("unchecked")
	private <T> T[] postForArray(Class<? extends T> objectClass, String service, String method, Object arg) throws SPECCHIOWebClientException {
		
		try {
			// get the array class
			Class<?> arrayClass = Array.newInstance(objectClass, 0).getClass();

			return (T[]) getWRBuilder(service, method).accept(MediaType.APPLICATION_XML).post(arrayClass, arg);
			
		}
		catch (UniformInterfaceException ex) {
			// could represent any kind of HTTP error
			System.out.println(ex.getMessage());
			throw new SPECCHIOWebClientException(ex);
		}
		catch (ClientHandlerException ex) {
			// could represent any kind of network error
			throw new SPECCHIOWebClientException(ex);
		}
		
	}
	
	
	/**
	 * Post an object to a web service that returns a boolean.
	 * 
	 * @param service	the service name
	 * @param method	the service method
	 * @param arg		the object to be posted
	 * 
	 * @return the return value of the service call
	 * @throws SPECCHIOWebClientException 
	 */
	private Boolean postForBoolean(String service, String method, Object arg) throws SPECCHIOWebClientException {

		try {
			return getWRBuilder(service, method).accept(MediaType.APPLICATION_XML).post(XmlBoolean.class, arg).getBoolean();
			
		}
		catch (UniformInterfaceException ex) {
			// could represent any kind of HTTP error
			throw new SPECCHIOWebClientException(ex);
		}
		catch (ClientHandlerException ex) {
			// could represent any kind of network error
			throw new SPECCHIOWebClientException(ex);
		}
		
	}
	
	
	/**
	 * Post an object to a web service that returns a list of objects.
	 * 
	 * @param objectClass	the class of the objects that will be returned
	 * @param service		the service name
	 * @param method		the service method
	 * @param arg			the object to be posted
	 * 
	 * @return the return value of the service call
	 * @throws SPECCHIOWebClientException 
	 */
	@SuppressWarnings("unchecked")
	private <T> List<T> postForList(Class<? extends T> objectClass, String service, String method, Object arg) throws SPECCHIOWebClientException {
		
		try {
			// get an array from the web service
			Object results[] = postForArray(objectClass, service, method, arg);
		
			// convert the array to a list
			List<T> list = new ArrayList<T>(results.length);
			for (int i = 0; i < results.length; i++) {
				list.add((T) results[i]);
			}
			
			return list;
		}
		catch (UniformInterfaceException ex) {
			// could represent any kind of HTTP error
			throw new SPECCHIOWebClientException(ex);
		}
		catch (ClientHandlerException ex) {
			// could represent any kind of network error
			throw new SPECCHIOWebClientException(ex);
		}
		
	}
	
	
	/**
	 * Call a web service that returns a list of objects.
	 * 
	 * @param objectClass	the class of the objects that will be returned
	 * @param service		the service name
	 * @param method		the service method
	 * 
	 * @return the return value of the service call
	 * @throws SPECCHIOWebClientException 
	 */
	@SuppressWarnings("unchecked")
	private <T> List<T> getList(Class<? extends T> objectClass, String service, String method) throws SPECCHIOWebClientException {
		
		try {
			// get an array from the web service
			Object results[] = getArray(objectClass, service, method);
		
			// convert the array to a list
			List<T> list = new ArrayList<T>(results.length);
			for (int i = 0; i < results.length; i++) {
				list.add((T) results[i]);
			}
			
			return list;
		}
		catch (UniformInterfaceException ex) {
			// could represent any kind of HTTP error
			throw new SPECCHIOWebClientException(ex);
		}
		catch (ClientHandlerException ex) {
			// could represent any kind of network error
			throw new SPECCHIOWebClientException(ex);
		}
		
	}	
	
	
	/**
	 * Post an object to a web service that returns an integer.
	 * 
	 * @param service	the service name
	 * @param method	the service method
	 * @param arg		the object to be posted
	 * 
	 * @return the return value of the service call
	 * @throws SPECCHIOWebClientException 
	 */
	private Integer postForInteger(String service, String method, Object arg) throws SPECCHIOWebClientException {

		try {
			return getWRBuilder(service, method).accept(MediaType.APPLICATION_XML).post(XmlInteger.class, arg).getInteger();
		}
		catch (UniformInterfaceException ex) {
			// could represent any kind of HTTP error
		    ClientResponse response = ex.getResponse();	
			throw new SPECCHIOWebClientException(ex);
		}
		catch (ClientHandlerException ex) {
			// could represent any kind of network error
			throw new SPECCHIOWebClientException(ex);
		}
		
	}


	/**
	 * Post an object to a web service that will return another object.
	 * 
	 * @param objectClass	the class of the object that will be returned
	 * @param service		the service name
	 * @param method		the service method
	 * @param arg			the object to be posted
	 * 
	 * @return the return value of the service call
	 * @throws SPECCHIOWebClientException 
	 */
	private <T> T postForObject(Class<? extends T> objectClass, String service, String method, Object arg) throws SPECCHIOWebClientException {
		
		try {
			return getWRBuilder(service, method).accept(MediaType.APPLICATION_XML).post(objectClass, arg);
		}
		catch (UniformInterfaceException ex) {
			// could represent any kind of HTTP error
			throw new SPECCHIOWebClientException(ex);
		}
		catch (ClientHandlerException ex) {
			// could represent any kind of network error
			throw new SPECCHIOWebClientException(ex);
		}
		
	}

	
	/**
	 * Post an object to a web service that returns a string.
	 * 
	 * @param service	the service name
	 * @param method	the service method
	 * @param arg		the object to be posted
	 * 
	 * @return the return value of the service call
	 * @throws SPECCHIOWebClientException 
	 */
	private String postForString(String service, String method, Object arg) throws SPECCHIOWebClientException {

		try {
			return getWRBuilder(service, method).accept(MediaType.APPLICATION_XML).post(String.class, arg);
		}
		catch (UniformInterfaceException ex) {
			// could represent any kind of HTTP error
			throw new SPECCHIOWebClientException(ex);
		}
		catch (ClientHandlerException ex) {
			// could represent any kind of network error
			throw new SPECCHIOWebClientException(ex);
		}
		
	}
	
	
	/**
	 * Post data read from an input stream to a web service.
	 * 
	 * @param is		the stream to be read from
	 * @param service	the service name
	 * @param method	the service method
	 * @param args		the service arguments
	 * @throws SPECCHIOWebClientException 
	 */
	private void postInputStream(InputStream is, String service, String method, String ... args) throws SPECCHIOWebClientException {
		
		try {
			ClientResponse response = getWRBuilder(service, method, args).entity(is).post(ClientResponse.class);
			if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
				throw new ClientHandlerException(
					web_service.path(buildPath(service, method, args)) +
					" returned a response status of " + 
					response.getStatus() + " " + response.getClientResponseStatus()
					);
			}
		}
		catch (UniformInterfaceException ex) {
			// could represent any kind of HTTP error
			throw new SPECCHIOWebClientException(ex);
		}
		catch (ClientHandlerException ex) {
			// could represent any kind of network error
			throw new SPECCHIOWebClientException(ex);
		}
		
	}
	
	
//	static {
//	    //for localhost testing only
//	    javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
//	    new javax.net.ssl.HostnameVerifier(){
//
//	        public boolean verify(String hostname,
//	                javax.net.ssl.SSLSession sslSession) {
//	            if (hostname.equals("localhost")) {
//	                return true;
//	            }
//	            return false;
//	        }
//	    });
//	}	
	

		
	
}