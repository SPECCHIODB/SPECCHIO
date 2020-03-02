package ch.specchio.db_import_export;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.joda.time.DateTime;

import au.ands.org.researchdata.RDACollectionDescriptor;
import ch.specchio.eav_db.EAVDBServices;
import ch.specchio.factories.MetadataFactory;
import ch.specchio.factories.SPECCHIOFactoryException;
import ch.specchio.factories.UserFactory;
import ch.specchio.model.Address;
import ch.specchio.model.AddressPart;
import ch.specchio.model.Collection;
import ch.specchio.model.Coverage;
import ch.specchio.model.Date;
import ch.specchio.model.Dates;
import ch.specchio.model.Description;
import ch.specchio.model.ElectronicAddress;
import ch.specchio.model.Identifier;
import ch.specchio.model.Location;
import ch.specchio.model.Name;
import ch.specchio.model.NamePart;
import ch.specchio.model.PhysicalAddress;
import ch.specchio.model.RegistryObject;
import ch.specchio.model.RegistryObjects;
import ch.specchio.model.RelatedInfo;
import ch.specchio.model.RelatedObject;
import ch.specchio.model.Relation;
import ch.specchio.model.Rights;
import ch.specchio.model.Spatial;
import ch.specchio.model.Subject;
import ch.specchio.model.Temporal;
import ch.specchio.types.ConflictInfo;
import ch.specchio.types.ConflictStruct;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.User;
import ch.specchio.types.attribute;

public class ANDSCollectionExport {

	private static final String ANDS_COLLECTION_FILE_NAME_PREFIX = "spectral-collection-";
	private static final String XML_FILE_POSTFIX = ".xml";
	private static final String COLLECTION_PREFIX = "uow.edu.au/SL/COL/";
	private static final String ORIGINATING_SOURCE = "http://www.uow.edu.au";
	private static final String LOCAL_IDENTIFIER_PREFIX = "Collection~";
	
	private static final String ACQUISITION_TIME_METAPARAMETER = "Acquisition Time";
	private static final String ANDS_COLLECTION_KEY_METAPARAMETER = "ANDS Collection Key";
	private static final String ANDS_COLLECTION_NAME_METAPARAMETER = "ANDS Collection Name";
	private static final String ANDS_COLLECTION_DESCRIPTION_METAPARAMETER = "ANDS Collection Description";
	private static final String DATA_USAGE_POLICY_METAPARAMETER = "Data Usage Policy";
	private static final String DOI_METAPARAMETER = "Digital Object Identifier";
	private static final String FOR_CODE_METAPARAMETER = "FOR Code";
	private static final String LOCATION_NAME_METAPARAMETER = "Location Name";
	
	private static final String EAVID_TOKEN = "%EAVID%";
	
	/** user factory */
	private UserFactory userFactory;
	
	/** output file location */
	private String andsXMLFileLocation;
	
	/** metadata factory */
	private MetadataFactory metadataFactory;
	
	/** EAV services */
	private EAVDBServices eavDBServices;
	
	/** ANDS party exporter */
	private ANDSPartyExport andsPartyExport;
	
	/** date format for RIF-CS files */
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	
	/** output file */
	private File andsCollectionFile;
	
	/** errors */
	private List<String> errors;
	
	
	/**
	 * Constructor.
	 * 
	 * @param dbUser				the database username
	 * @param dbPassword			the database password
	 * @param ds_name		data source name
	 * @param is_admin		is the user an administrator? 
	 * @param _andsXMLFileLocation	the location of the output file
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public ANDSCollectionExport( String dbUser, String dbPassword, String ds_name, boolean is_admin, String _andsXMLFileLocation)
		throws SPECCHIOFactoryException {

		// create factories, using the same database connection for each
		userFactory = new UserFactory(dbUser, dbPassword, ds_name, is_admin);
		metadataFactory = new MetadataFactory(userFactory);
		
		// initialise EAV services
		eavDBServices = new EAVDBServices( userFactory.getStatementBuilder() , userFactory.getAttributes(), userFactory.getDatabaseUserName());
		
		// initialise ANDS XML stuff
		andsXMLFileLocation = _andsXMLFileLocation;
		andsPartyExport = new ANDSPartyExport(andsXMLFileLocation);
		
		// initialise error list
		errors = new ArrayList<String>();
	}
	
	private boolean createFileAndDirectory( String baseDirectoryString, User pi, String collectionIdString) 
	{
		
		File andsCollectionFileNameBaseDir = new File( baseDirectoryString);
		andsCollectionFileNameBaseDir.mkdirs();
	    boolean directoryExists = andsCollectionFileNameBaseDir.exists();
	    System.out.println("fileName 0.11: directoryCreated.exists() is: " + directoryExists);
	    if(!directoryExists)
	    	return false;

	    String fullyQualifiedFileName = baseDirectoryString + ANDS_COLLECTION_FILE_NAME_PREFIX + extractCollectionIdNumber(collectionIdString) + XML_FILE_POSTFIX;
		andsCollectionFile = new File( fullyQualifiedFileName);

		return true;

	}
	
	
	/**
	 * Get the list of errors from the last invocation of exportCollectionXML().
	 * 
	 * @return a list of strings describing all of the error found
	 */
	public List<String> getErrors() {
		
		return errors;
		
	}
	
	/**
	 * Check whether or not there were errors in the last invocation of exportCollectionXML().
	 * 
	 * @return true or false
	 */
	public boolean hasErrors() {
		
		return !errors.isEmpty();
		
	}
	
	/**
	 * This method creates the file in the xml file location that is taken in from the web.xml. If the directory supplied does not exist then the 
	 * file is written to the home directory.
	 * @param pi Specchio user object
	 * @param collectionIdString id string of a collection
	 */
	private void setANDSCollectionFilenameDir( User pi, String collectionIdString)
	{	
		if(!createFileAndDirectory( andsXMLFileLocation + System.getProperty("file.separator"), pi, collectionIdString))
		{
		    createFileAndDirectory( System.getProperty("user.home") + andsXMLFileLocation + System.getProperty("file.separator"), pi, collectionIdString);
		} 
	}
	
	
	/**
	 * Extraction the collection ID number from the collection ID.
	 * 
	 * @param collectionId	the ID string
	 * 
	 * @return the collection number (at the end of the string), or zero if it was not found
	 */
	private int extractCollectionIdNumber(String collectionId) {

		// extract the collection number from the end of the collection string
		int i = collectionId.length() - 1;
		int pos = 1;
		int id = 0;
		while (i >= 0 && Character.isDigit(collectionId.charAt(i))) {
			id += (int)(collectionId.charAt(i) - '0') * pos;
			i--;
			pos *= 10;
		}
		
		return id;
		
	}
	
	/**
	 * @param rdaCollectionDescriptor 
	 * @return an ordered list of Dates based on acquisition dates for a spectra.
	 * @throws SPECCHIOFactoryException
	 */
	private List<java.util.Date> obtainFirstLastDates (RDACollectionDescriptor rdaCollectionDescriptor) throws SPECCHIOFactoryException
	{
		// get the acquisition times for all spectra in the collection
		List<MetaParameter> acquisitionTimes = metadataFactory.getMetaParameters(MetaParameter.SPECTRUM_LEVEL, 
				rdaCollectionDescriptor.getSpectrumIds(),
				ACQUISITION_TIME_METAPARAMETER,
				true
			);
		
		// copy the data values into a sorted list
		List<java.util.Date> fromToDates = new ArrayList<java.util.Date>();
		for (MetaParameter mp : acquisitionTimes) {
			DateTime date = (DateTime) mp.getValue();
			fromToDates.add(date.toDate());
		}
		java.util.Collections.sort(fromToDates);

		return fromToDates;
	}
	
	
	/**
	 * Obtain a collection identifier and associate it with every spectrum in the
	 * collection. This method should only be called if the collection is ready to
	 * be created; otherwise it will create and associate an invalid collection key.
	 * 
	 * If all of spectra in the collection already have the same collection key, this
	 * method will return that key. Otherwise, it creates a new key consisting of
	 * COLLECTION_PREFIX followed by the EAV ID of the new ANDS Collection Key metadata
	 * parameter.
	 * 
	 * @param rdaCollectionDescriptor	the collection descriptor
	 * 
	 * @return the new collection identifier
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private String obtainCollectionIdString(RDACollectionDescriptor rdaCollectionDescriptor) throws  SPECCHIOFactoryException
	{
		
		// convert int spectrum identifiers to Integer spectrum identifiers... grrr
		//Integer[] spectrumIds = new Integer[rdaCollectionDescriptor.getSpectrumIds().length];
		ArrayList<Integer> spectrumIds = new ArrayList<Integer>();
		int i = 0;
		for(int value : rdaCollectionDescriptor.getSpectrumIds())
		{
			spectrumIds.add(value);
			//spectrumIds[i++] = Integer.valueOf(value);
		}
		
		MetaParameter andsCollectionKey = updateMetadataWithCreate(ANDS_COLLECTION_KEY_METAPARAMETER, spectrumIds, COLLECTION_PREFIX + EAVID_TOKEN);
		updateMetadataWithCreate(ANDS_COLLECTION_NAME_METAPARAMETER, spectrumIds, rdaCollectionDescriptor.getPrimaryName());
		updateMetadataWithCreate(ANDS_COLLECTION_DESCRIPTION_METAPARAMETER, spectrumIds, rdaCollectionDescriptor.getBriefDescription());
		
		
		return (String)andsCollectionKey.getValue();
	}
	
	/**
	 * Build the description element for a collection.
	 * 
	 * If there is no description available, an error will be added to the current
	 * error list.
	 * 
	 * @param rdaCollectionDescriptor	the collection descriptor
	 * @param campaigns					the campaigns from which the collection is drawn
	 * 
	 * @return a Description object representing the collection, or null if no description is available
	 * 
	 * @throws SPECCHIOFactoryException database error
	 */
	private Description obtainCollectionDescription(RDACollectionDescriptor rdaCollectionDescriptor)
		throws SPECCHIOFactoryException {
		
		Description description = null;
		if (rdaCollectionDescriptor.getBriefDescription() != null &&
				rdaCollectionDescriptor.getBriefDescription().length() > 0) {

			// build the description object
			description = new Description();
			description.setType("brief");
			description.setValue(rdaCollectionDescriptor.getBriefDescription());
			
		} else {
			
			errors.add("This collection has no description. You may need to upgrade SPECCHIO.");
			
		}
		
		return description;
		
	}
	
	/**
	 * Build the name element for a collection.
	 * 
	 * If there is no suitable name available, an error will be added to
	 * the current error list.
	 * 
	 * @param rdaCollectionDescriptor	the collection descriptor
	 * 
	 * @return a Name object describing the collection, or null if no name is available
	 * 
	 * @throws SPECCHIOFactoryException database error
	 */
	private Name obtainCollectionNames(RDACollectionDescriptor rdaCollectionDescriptor)
		throws SPECCHIOFactoryException {

		Name name = null;
		if (rdaCollectionDescriptor.getPrimaryName() != null &&
				rdaCollectionDescriptor.getPrimaryName().length() > 0) {
		
			// build the Name object
			name = new Name();
			name.setType("primary");
			NamePart namePart = new NamePart();
			namePart.setValue(rdaCollectionDescriptor.getPrimaryName());
			ArrayList<NamePart> namePartList = new ArrayList<NamePart>();
			namePartList.add(namePart);
			name.setNamePartList(namePartList);
			
		} else {
			
			errors.add("This collection does not have a name. You may need to upgrade SPECCHIO.");
			
		}
		
		return name;
		
	}
	
	/**
	 * Build the related object for the collector.
	 * 
	 * The collector is the principal investigator.
	 * 
	 * @param rdaCollectionDescriptor	the collection descriptor
	 * 
	 * @return a RelatedObject instance describing the collector.
	 */
	private RelatedObject obtainCollector(RDACollectionDescriptor rdaCollectionDescriptor) {

		// get a reference to the principal investigator's details
		User pi = rdaCollectionDescriptor.getPrincipalInvestigator();
		
		// put the create a relation object with type "hasCollector"
		RelatedObject relatedObject = new RelatedObject();
		relatedObject.setKey(pi.getExternalId());
		Relation relation = new Relation();
		relation.setType("hasCollector");
		relatedObject.setRelation(relation);
		
		return relatedObject;
	}
	
	/**
	 * Build the coverage element for a collection.
	 * 
	 * @param rdaCollectionDescriptor	the collection descriptor
	 * 
	 * @return a Coverage object, or null if there was no data
	 * 
	 * @throws SPECCHIOFactoryException database error
	 */
	private Coverage obtainCoverage(RDACollectionDescriptor rdaCollectionDescriptor)
		throws SPECCHIOFactoryException {

		// get the spatial location for all spectra in the collection
		ArrayList<MetaParameter> locationNameMetaParameters = metadataFactory.getMetaParameters(MetaParameter.SPECTRUM_LEVEL, 
				rdaCollectionDescriptor.getSpectrumIds(),
				LOCATION_NAME_METAPARAMETER,
				true
			);
		
			
		// convert the list of location metaparameters into a list of Spatial elements
		ArrayList<Spatial> spatialList = new ArrayList<Spatial>(locationNameMetaParameters.size());
		for (MetaParameter mp : locationNameMetaParameters) {
			Spatial spatial = new Spatial();
			spatial.setType("text");
			spatial.setValue((String)mp.getValue());
			spatialList.add(spatial);
		}
		
		// create the temporal coverage, if the spectra have acquisition times
		Temporal temporal = null;
		List<java.util.Date> fromToDateList = obtainFirstLastDates(rdaCollectionDescriptor);
		if (!fromToDateList.isEmpty()) {
			temporal = new Temporal();
			ArrayList<Date> temporalDateList = new ArrayList<Date>();
			Date date1 = new Date();
			date1.setType("dateFrom");
			date1.setDateFormat("W3CDTF");
			date1.setValue(sdf.format(fromToDateList.get(0)));
			temporalDateList.add(date1);
			Date date2 = new Date();
			date2.setType("dateTo");
			date2.setDateFormat("W3CDTF");
			date2.setValue(sdf.format(fromToDateList.get(fromToDateList.size()-1)));
			temporalDateList.add(date2);
			temporal.setDateList(temporalDateList);
		}
		
		if (spatialList.size() > 0 || temporal != null) {
			
			// got data; create a coverage object
			Coverage coverage = new Coverage();
			coverage.setSpatial(spatialList);
			coverage.setTemporal(temporal);
			return coverage;
			
		} else {
			
			// no data; return null
			return null;
			
		}
		
		
	}
	
	/**
	 * Build the list of identifiers for a collection.
	 * 
	 * @param rdaCollectionDescriptor	the collection descriptor
	 * @param collectionId				the collection identifier
	 * 
	 * @return a list of Identifier objects
	 * 
	 * @throws SPECCHIOFactoryException database error
	 */
	private ArrayList<Identifier> obtainIdentifierList(RDACollectionDescriptor rdaCollectionDescriptor, String collectionId)
		throws SPECCHIOFactoryException {

		// start with an empty list
		ArrayList<Identifier> identifierList = new ArrayList<Identifier>();
		
		// add a local identifier
		Identifier identifier = new Identifier();
		identifier.setType("local");
		identifier.setValue(LOCAL_IDENTIFIER_PREFIX + extractCollectionIdNumber(collectionId));
		identifierList.add(identifier);	
		
		// add DOIs
		ArrayList<MetaParameter> doiMetaParameters = metadataFactory.getMetaParameters(MetaParameter.SPECTRUM_LEVEL, 
				rdaCollectionDescriptor.getSpectrumIds(),
				DOI_METAPARAMETER,
				true
			);
		for (MetaParameter mp : doiMetaParameters) {	
			Identifier doiIdentifier = new Identifier();
			doiIdentifier.setType("doi");
			doiIdentifier.setValue((String)mp.getValue());
			identifierList.add(doiIdentifier);
		}
		
		return identifierList;
		
	}
	
	/**
	 * Build the location object for a collection.
	 * 
	 * The collection's address is the address of the principal investigator. If
	 * the principal investigator does not have an e-mail address or does not
	 * have a physical address, errors will be added to the current error list.
	 * 
	 * 
	 * @param rdaCollectionDescriptor	the collection descriptor
	 * 
	 * @returns a Location object
	 * 
	 * @throws SPECCHIOFactoryException database error
	 */
	private Location obtainLocation(RDACollectionDescriptor rdaCollectionDescriptor)
		throws SPECCHIOFactoryException {

		// get a reference to the principal investigator
		User pi = rdaCollectionDescriptor.getPrincipalInvestigator();
		
		// start with an empty Location object
		Location location = new Location();
		Address address = new Address();
		
		// set e-mail address
		if( pi.getEmailAddress() != null && pi.getEmailAddress().length() > 0)
		{
			ElectronicAddress electronicAddress = new ElectronicAddress();
			electronicAddress.setType("email");
			electronicAddress.setValue(pi.getEmailAddress());
			address.setElectronicAddress(electronicAddress);
		} else {
			errors.add("The principal investigator does not have an e-mail address. Please set an e-mail address before submitting a collection.");
		}
		
		// set physical address
		if( (pi.getInstitute() != null 
				&& ( pi.getInstitute().getDepartment() != null && pi.getInstitute().getDepartment().length() > 0)
					||  (pi.getInstitute().getInstituteName() != null && pi.getInstitute().getInstituteName().length() > 0)))
		{
			PhysicalAddress physicalAddress = new PhysicalAddress();
			physicalAddress.setType("streetAddress");
			AddressPart addressPart = new AddressPart();
			addressPart.setType("text");
			String addressString = "";
			if( pi.getInstitute().getDepartment() != null)
			{
				addressString += pi.getInstitute().getDepartment();
				addressString += " ";
			}
			if( pi.getInstitute().getInstituteName() != null)
			{
				addressString += pi.getInstitute().getInstituteName();
			}
			addressPart.setValue(addressString);
			physicalAddress.setAddressPart(addressPart);
			address.setPhysicalAddress(physicalAddress);
		} else {
			errors.add("The principal investigator does not have a department or institute. Please set these before submitting a collection.");
		}
		
		
		location.setAddress(address);
		return location;
		
	}
	
	/**
	 * Build the rights element.
	 * 
	 * @param rdaCollectionDescriptor	the collection descriptor
	 * 
	 * @return a list of Rights objects
	 * 
	 * @throws SPECCHIOFactoryException database error
	 */
	private ArrayList<Rights> obtainRights(RDACollectionDescriptor rdaCollectionDescriptor)
		throws SPECCHIOFactoryException {
		
		// start with an empty list
		ArrayList<Rights> rightsList = new ArrayList<Rights>();
		
		// get all of the data user policy metaparameters
		ArrayList<MetaParameter> dataUsagePolicyMetaParameters = metadataFactory.getMetaParameters(MetaParameter.SPECTRUM_LEVEL, 
				rdaCollectionDescriptor.getSpectrumIds(),
				DATA_USAGE_POLICY_METAPARAMETER,
				true
			);
		
		if (dataUsagePolicyMetaParameters.size() > 0) {
			
			// create one rights element for every policy
			for (MetaParameter mp : dataUsagePolicyMetaParameters) {
				Rights rights = new Rights();
				rights.setAccessRights((String)mp.getValue());
				rightsList.add(rights);
			}
			
		} else {
			
			// no existing policies; generate a standard-form one
			Rights rights = new Rights();
			rights.setAccessRights(
					"Data can be shared on a case by case basis. " +
					"For further information please contact " +
					rdaCollectionDescriptor.getPrincipalInvestigator().toString() + " " +
					"<" + rdaCollectionDescriptor.getPrincipalInvestigator().getEmailAddress() + ">"
				);
			rightsList.add(rights);
			
		}
		
		return rightsList;
		
	}
	
	/**
	 * Build the related information list. SPECCHIO does not currently store any
	 * related information, so this list is always empty.
	 * 
	 * @param rdaCollectionDescriptor	the collection descriptor
	 * 
	 * @return a list of related info objects
	 * 
	 * @throws SPECCHIOFactoryException database error
	 */
	private ArrayList<RelatedInfo> obtainRelatedInfo(RDACollectionDescriptor rdaCollectionDescriptor)
		throws SPECCHIOFactoryException {
		
		// return an empty related information list
		return new ArrayList<RelatedInfo>();
		
	}
	
	/**
	 * Build the subject list.
	 * 
	 * The subject list contains the FOR codes for every specrum in the collection.
	 * If no spectra have any FOR codes, an error will be added to the current error
	 * list.
	 * 
	 * @param rdaCollectionDescriptor	the collection description
	 * 
	 * @return a list of Subject objects
	 * 
	 * @throws SPECCHIOFactoryException database error
	 */
	private ArrayList<Subject> obtainSubjectList(RDACollectionDescriptor rdaCollectionDescriptor)
		throws SPECCHIOFactoryException {

		// start with an empty list
		ArrayList<Subject> subjectList = new ArrayList<Subject>();
		
		// get the FOR codes for every spectrum in the collection
		ArrayList<MetaParameter> forCodeMetaParameters = metadataFactory.getMetaParameters(MetaParameter.SPECTRUM_LEVEL, 
				rdaCollectionDescriptor.getSpectrumIds(),
				FOR_CODE_METAPARAMETER,
				true
			);
		
		if (forCodeMetaParameters.size() > 0) {
			
			// convert the metaparameter values into Subject elements
			for (MetaParameter mp : forCodeMetaParameters) {
				Subject subjectForCode = new Subject();
				subjectForCode.setType("anzsrc-for");
				subjectForCode.setValue(
					metadataFactory.getTaxonomyObject(((Long) mp.getValue()).intValue()).getCode()
				);
				subjectList.add(subjectForCode);
			}
			
		} else {
			
			// no FOR code available; record an error
			errors.add(
				"There are no FOR codes associated with any spectra in this collection. " + 
				"You must add codes before submitting the collection."
			);
			
		}
		
		return subjectList;
	}
	
	
	/**
	 * Update a metadata value of a set of spectra, creating it if there is no
	 * suitable existing metadata item.
	 * 
	 * If the value parameter contains the string EAVID_TOKEN, this will be replaced by the EAV ID
	 * of the metaparameter item.
	 * 
	 * @param attributeName	the metadata attribute to be changed
	 * @param spectrumIds	the identifiers of the spectra to be updated
	 * @param value			the new value for the metadata item
	 * 
	 * @return the updated MetaParameter object
	 * 
	 * @throws SPECCHIOFactoryException database error
	 */
	private MetaParameter updateMetadataWithCreate(String attributeName, ArrayList<Integer> spectrumIds, String value)
		throws SPECCHIOFactoryException {

		// get the attribute corresponding to the given attribute name
		int attributeId = eavDBServices.ATR.get_attribute_id(attributeName);
		if (attributeId == 0) {
			throw new SPECCHIOFactoryException("Attribute " + attributeName + " does not exist.");
		}
		attribute attr = eavDBServices.ATR.get_attribute_info(attributeId);
		
		// see if the spectra have an existing shared metadata item with this name
		MetaParameter mp = null;
		if (spectrumIds.size() > 0) {
			
			// start with a set containing all of the metaparameters with the given name for the first spectrum
			Set<MetaParameter> candidates = new HashSet<MetaParameter>();
			for (MetaParameter mp1 : metadataFactory.getMetadataForSpectrum(spectrumIds.get(0)).get_all_entries(attr.id)) {
				candidates.add(mp1);
			}
			
			// get the conflict information the given attribute over the given set of spectra
			ConflictInfo conflicts = metadataFactory.detectEavConflicts(MetaParameter.SPECTRUM_LEVEL, spectrumIds).get(attr.id);
			
			if (conflicts != null) {
				// look for an EAV ID with no conflicts
				Enumeration<Integer> eavIds = conflicts.eavIds();
				while (eavIds.hasMoreElements()) {
					Integer eavId = eavIds.nextElement();
					ConflictStruct conflict = conflicts.getConflictData(eavId);
					if (conflict.getNumberOfSharingRecords() != conflict.getNumberOfSelectedRecords()) {
						// this EAV ID is conflicted; remove it from the candidates list
						for (MetaParameter mp1 : candidates) {
							if (mp1.getEavId().equals(eavId)) {
								candidates.remove(mp1);
								break;
							}
						}
					}
				}	
			}
			
			// if only one candidate remains, use it
			if (candidates.size() == 1) {
				mp = candidates.iterator().next();
			}
			
		}

		if (mp == null) {
		
			// no usable existing metadata item; create a new one
			try {
				mp = MetaParameter.newInstance(attr);
				int eavId = metadataFactory.updateMetadata(mp, spectrumIds);
				mp.setValue(value.replace(EAVID_TOKEN, Integer.toString(eavId)));
			}
			catch (MetaParameterFormatException e) {
				// the attribute does not have type string; re-throw as a database error
				throw new SPECCHIOFactoryException(e);
			}
			
		}
		metadataFactory.updateMetadata(mp, spectrumIds);
		
		return mp;
		
	}
	
	
	/**
	 * Generate a RIF-CS collection document describing a collection.
	 * 
	 * @param rdaCollectionDescriptor	the collection descriptor
	 * 
	 * @return the new collection identifier, or null if insufficient data is available
	 * 
	 * @throws JAXBException could not create a JAXB instance
	 * @throws SPECCHIOFactoryException	database error
	 */
	public String exportCollectionXML(RDACollectionDescriptor rdaCollectionDescriptor)
			throws JAXBException, SPECCHIOFactoryException
	{
		// reset error list
		errors.clear();
		
		// make sure that we have the full details of the principal investigator
		User pi = userFactory.getUser(rdaCollectionDescriptor.getPrincipalInvestigator().getUsername());
		if (pi == null) {
			// the alleged principal investigator does not exist; give up now
			errors.add(
				"The principal investigator \"" +
				rdaCollectionDescriptor.getPrincipalInvestigator().getUsername() +
				"\" does not exist. This collection cannot be submitted."
			);
			return null;
		}
		rdaCollectionDescriptor.setPrincipalInvestigator(pi);
	
		// get the first and last dates of the items in the collection
		List<java.util.Date> fromToDateList = obtainFirstLastDates (rdaCollectionDescriptor);
		
		// create the "collection" element, modified at the current time
		Collection collection = new Collection();
		collection.setType("collection");
		collection.setDateModified(sdf.format(new java.util.Date()));
		
		// the collection's date of submission to be is the date of the first spectrum in the collection
		Date date = new Date();
		date.setType("dateFrom");
		date.setDateFormat("W3CDTF");
		date.setValue(sdf.format(fromToDateList.get(0)));
		Dates dates = new Dates();
		dates.setType("dc.dateSubmitted");
		dates.setDate(date);
		collection.setDates(dates);
		
		// set the collection name and description
		collection.setName(obtainCollectionNames(rdaCollectionDescriptor));
		collection.setDescription(obtainCollectionDescription(rdaCollectionDescriptor));
		
		// set location
		collection.setLocation(obtainLocation(rdaCollectionDescriptor));
		
		// set related objects
		ArrayList<RelatedObject> relatedObjectList = new ArrayList<RelatedObject>();
		relatedObjectList.add(obtainCollector(rdaCollectionDescriptor));
		collection.setRelatedObjectList(relatedObjectList);
		
		// set subjects
		collection.setSubjectList(obtainSubjectList(rdaCollectionDescriptor));
		
		// set coverage
		collection.setCoverage(obtainCoverage(rdaCollectionDescriptor));
		
		// set related information
		ArrayList<RelatedInfo> relatedInfoList = obtainRelatedInfo(rdaCollectionDescriptor);
		if (relatedInfoList.size() > 0)
		{
			collection.setRelatedInfoList(relatedInfoList);
		}
		
		// set the rights
		collection.setRights(obtainRights(rdaCollectionDescriptor));
		
		if (!hasErrors()) {
			// everything is okay; create a collection identifier
			String collectionId = obtainCollectionIdString(rdaCollectionDescriptor);
			
			// add identifiers
			collection.setIdentifierList(obtainIdentifierList(rdaCollectionDescriptor, collectionId));
			
			// create a new registry object to represent the collection
			RegistryObject ro = new RegistryObject();
			ro.setCollection(collection);
			ro.setKey(collectionId);
			ro.setOriginatingSource(ORIGINATING_SOURCE);

			// the registry object's group is the institute of the PI
			if(pi.getInstitute() != null && pi.getInstitute().getInstituteName() != null && pi.getInstitute().getInstituteName().length() > 0)
			{
				ro.setGroup(pi.getInstitute().getInstituteName());
			}
			
			// put the registry object into a list of length 1
			ArrayList<RegistryObject> registryObjectList = new ArrayList<RegistryObject>();
			registryObjectList.add(ro);
			RegistryObjects ros = new RegistryObjects();
			ros.setRegistryObjectList(registryObjectList);
			
			// create JAXB context and instantiate marshaller
		    JAXBContext context = JAXBContext.newInstance(RegistryObjects.class);
		    Marshaller m = context.createMarshaller();
		    m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://ands.org.au/standards/rif-cs/registryObjects http://services.ands.org.au/documentation/rifcs/schema/registryObjects.xsd");
		    m.setProperty("com.sun.xml.bind.namespacePrefixMapper",new MyNsPrefixMapper());
		    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);


		    // write to File
		    setANDSCollectionFilenameDir(pi, collectionId);
		    m.marshal(ros, andsCollectionFile);
		    
			// export the party information for the collection
			andsPartyExport.exportPartyXML(pi, collectionId, collection);
			
			return collectionId;
		
		} else {
			
			// something went wrong; don't create a collection identifier
			return null;
			
		}
		
	}


}
