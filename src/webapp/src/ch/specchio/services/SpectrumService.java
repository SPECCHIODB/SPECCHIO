package ch.specchio.services;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import ch.specchio.constants.UserRoles;
import ch.specchio.factories.MetadataFactory;
import ch.specchio.factories.SPECCHIOFactoryException;
import ch.specchio.factories.SpaceFactory;
import ch.specchio.factories.SpectrumFactory;
import ch.specchio.jaxb.XmlInteger;
import ch.specchio.jaxb.XmlIntegerAdapter;
import ch.specchio.jaxb.XmlString;
import ch.specchio.plots.GonioSamplingPoints;
import ch.specchio.queries.Query;
import ch.specchio.spaces.MeasurementUnit;
import ch.specchio.spaces.ReferenceSpaceStruct;
import ch.specchio.spaces.Space;
import ch.specchio.spaces.SpaceQueryDescriptor;
import ch.specchio.spaces.SpectralSpace;
import ch.specchio.types.AVMatchingList;
import ch.specchio.types.AVMatchingListCollection;
import ch.specchio.types.ArrayListWrapper;
import ch.specchio.types.MetadataSelectionDescriptor;
import ch.specchio.types.PictureTable;
import ch.specchio.types.SpectraMetadataUpdateDescriptor;
import ch.specchio.types.Spectrum;
import ch.specchio.types.SpectrumDataLink;
import ch.specchio.types.SpectrumIdsDescriptor;
import ch.specchio.types.SpectrumFactorTable;


/**
 * Spectrum service.
 */
@Path("/spectrum")
@DeclareRoles({UserRoles.ADMIN, UserRoles.USER})
public class SpectrumService extends SPECCHIOService {
	
	
	/**
	 * Creates a copy of a spectrum in the specified hierarchy
	 * 
	 * @param spectrum_id		the spectrum_id of the spectrum to copy
	 * @param target_hierarchy_id	the hierarchy_id where the copy is to be stored
	 * 
	 * @return new spectrum id
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */	
	@GET
	@Path("copySpectrum/{spectrum_id: [0-9]+}/{target_hierarchy_id: [0-9]+}")
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger copySpectrum(
			@PathParam("spectrum_id") int spectrum_id,
			@PathParam("target_hierarchy_id") int target_hierarchy_id
		) throws SPECCHIOFactoryException {
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		int new_spectrum_id = factory.copySpectrum(spectrum_id, target_hierarchy_id);
		factory.dispose();
		
		return new XmlInteger(new_spectrum_id);
		
	}	
	
	
	/**
	 * Get the number of spectra in database
	 * 
	 * @param String		empty string
	 * 
	 * @return the number of spectra in the database
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	@GET
	@Path("countInDB")
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger countInDB() throws SPECCHIOFactoryException {
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		int count = factory.getSpectrumCountInDB();
		factory.dispose();
		
		return new XmlInteger(count);
		
	}	
	
	
	/**
	 * Delete target-reference links.
	 * 
	 * @param eav_id		the eav_id identifier
	 * 
	 * @return the number of links deleted
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@GET
	@Path("deleteTargetReferenceLinks/{eav_id: [0-9]+}")
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger deleteTargetReferenceLinks(@PathParam("eav_id") int eav_id)throws SPECCHIOFactoryException {
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		int n = factory.deleteTargetReferenceLinks(eav_id);
		factory.dispose();
		
		return new XmlInteger(n);
		
	}
	
	
	/**
	 * Get the spectrum identifiers that do have a reference to the specified attribute.
	 * 
	 * @param MetadataSelectionDescriptor 	specifies ids to filter and attribute to filter by
	 * 
	 * @return an array list of spectrum identifiers that match the filter
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("filterSpectrumIdsByHavingAttribute")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger[] filterSpectrumIdsByHavingAttribute(MetadataSelectionDescriptor mds)throws SPECCHIOFactoryException {
		
		if(mds.getAttribute_id() == 0)
		{
			MetadataFactory factory = new MetadataFactory(getClientUsername(), getClientPassword(), getDataSourceName(), getSecurityContext().isUserInRole(UserRoles.ADMIN));
			mds.setAttribute_id(factory.getAttributes().get_attribute_id(mds.getAttributeName()));			
		}			
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		List<Integer> ids = factory.filterSpectrumIdsByHavingAttribute(mds);
		factory.dispose();
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		return adapter.marshalArray(ids);

		
	}
		
	
	/**
	 * Get the spectrum identifiers that do not have a reference to the specified attribute.
	 * 
	 * @param MetadataSelectionDescriptor 	specifies ids to filter and attribute to filter by
	 * 
	 * @return an array list of spectrum identifiers that match the filter
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("filterSpectrumIdsByNotHavingAttribute")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger[] filterSpectrumIdsByNotHavingAttribute(MetadataSelectionDescriptor mds)throws SPECCHIOFactoryException {
		
		if(mds.getAttribute_id() == 0)
		{
			MetadataFactory factory = new MetadataFactory(getClientUsername(), getClientPassword(), getDataSourceName(), getSecurityContext().isUserInRole(UserRoles.ADMIN));
			mds.setAttribute_id(factory.getAttributes().get_attribute_id(mds.getAttributeName()));			
		}			
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		List<Integer> ids = factory.filterSpectrumIdsByNotHavingAttribute(mds);
		factory.dispose();
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		return adapter.marshalArray(ids);

		
	}
	
	
	/**
	 * Get the spectrum identifiers that do reference to the specified attribute of a specified value.
	 * 
	 * @param MetadataSelectionDescriptor 	specifies ids to filter and attribute and value to filter by
	 * 
	 * @return an array list of spectrum identifiers that match the filter
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("filterSpectrumIdsByHavingAttributeValue")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger[] filterSpectrumIdsByHavingAttributeValue(MetadataSelectionDescriptor mds)throws SPECCHIOFactoryException {
		
		if(mds.getAttribute_id() == 0)
		{
			MetadataFactory factory = new MetadataFactory(getClientUsername(), getClientPassword(), getDataSourceName(), getSecurityContext().isUserInRole(UserRoles.ADMIN));
			mds.setAttribute_id(factory.getAttributes().get_attribute_id(mds.getAttributeName()));			
		}			
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		List<Integer> ids = factory.filterSpectrumIdsByHavingAttributeValue(mds);
		factory.dispose();
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		return adapter.marshalArray(ids);

		
	}	
		
	
	/**
	 * Get a spectrum object.
	 * 
	 * @param spectrum_id		the identifier of the desired spectrum
	 * @param prepare_metadata	load the spectrum's metadata?
	 * 
	 * @return a Spectrum object representing the desired spectrum
	 * 
	 * @throws SPECCHIOFactoryException	spectrum_id does not exist
	 */
	@GET
	@Path("get/{spectrum_id: [0-9]+}/{prepare_metadata}")
	@Produces(MediaType.APPLICATION_XML)
	public Spectrum get(
			@PathParam("spectrum_id") int spectrum_id,
			@PathParam("prepare_metadata") String prepare_metadata
		) throws SPECCHIOFactoryException {
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		Spectrum s = factory.getSpectrum(spectrum_id, Boolean.valueOf(prepare_metadata));
		factory.dispose();
		
		return s;
		
	}
	
	
	/**
	 * Get Space objects that represent calibration factors
	 * 
	 * @param query_d	the query descriptor
	 * 
	 * @return an array of Space objects representing the calibration factors identified in the query descriptor
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("getCalibrationSpaces")
	@Produces(MediaType.APPLICATION_XML)
	public Space[] getCalibrationSpaces(SpaceQueryDescriptor query_d) throws SPECCHIOFactoryException {
		
		SpaceFactory factory = new SpaceFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		ArrayList<Space> spaces = factory.getCalibrationSpaces(query_d.spectrum_ids);
		factory.dispose();
		
		return spaces.toArray(new Space[spaces.size()]);
	}
	

	/**
	 * Get the direct hierarchy id of a single spectrum
	 * 
	 * @param ids_d		container holding the identifier of the desired spectrum
	 * 
	 * @return hierarchy id
	 * 
	 */
	@POST
	@Path("getDirectHierarchyOfSpectrum")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger getDirectHierarchyOfSpectrum(SpectrumIdsDescriptor ids_d)
	{
		int id = 0;
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		id = factory.getDirectHierarchyOfSpectrum(ids_d.getSpectrumIds(1));
		
		return new XmlInteger(id);
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
	@POST
	@Path("getHierarchyIdsOfSpectra")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)	
	public XmlInteger[] getHierarchyIdsOfSpectra(SpectrumIdsDescriptor ids_d)
	{		
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		List<Integer> ids = factory.getHierarchyIdsOfSpectra(ids_d.getSpectrumIds(1));
		factory.dispose();
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		return adapter.marshalArray(ids);				
		
	}	
	
	/**
	 * Get the Goniometer sampling points for a space.
	 * 
	 * @param space	the space
	 * 
	 * @return a GonioSamplingPoints object
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("getGonioSamplingPoints")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public GonioSamplingPoints getGonioSamplingPoints(SpectralSpace space) throws SPECCHIOFactoryException {
		
		SpaceFactory factory =  new SpaceFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		GonioSamplingPoints sampling_points = factory.getGonioSamplingPoints(space);
		factory.dispose();
		
		return sampling_points;
		
	}
	
	
	
	/**
	 * Get newest N spectra.
	 * 
	 * @param number_of_spectra N
	 * 
	 * @return list of spectrum ids ordered by data ingestion time
	 */	
	@POST
	@Path("getNewestSpectra")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public  XmlInteger[] getNewestSpectra(XmlInteger number_of_spectra) {
		
		MetadataFactory factory = new MetadataFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		
		ArrayList<Integer> ids = factory.getNewestSpectra(number_of_spectra.getInteger());
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		return adapter.marshalArray(ids);
	}	
	
	// arraylist wrapper trial: leads to error 500 ...
//	public ArrayListWrapper<Integer> getNewestSpectra(XmlInteger number_of_spectra) {
//		
//		MetadataFactory factory = new MetadataFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
//		
//		ArrayList<Integer> ids = factory.getNewestSpectra(number_of_spectra.getInteger());
//		
//		ArrayListWrapper<Integer> l = new ArrayListWrapper<Integer>(ids);
//		return l;
//
//	}		
	
	/**
	 * Get hierarchy ids, directly above these spectra
	 * 
	 * @param spectrum_ids		the identifiers of the desired spectra
	 * 
	 * @return hierarchy ids
	 * 
	 * @throws SPECCHIOFactoryException	
	 */	
	@POST
	@Path("getDirectHierarchyIds")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)	
	public XmlInteger[] getDirectHierarchyIds(SpectrumIdsDescriptor ids_d)
	{		
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		List<Integer> ids = factory.getDirectHierarchyIds(ids_d.getSpectrumIds(1));
		factory.dispose();
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		return adapter.marshalArray(ids);				
		
	}

	
	/**
	 * Get a reference space.
	 * 
	 * @param query_d	the lists of input (position 1) and output (position 2) identifiers
	 * 
	 * @return a ReferenceSpaceStruct object
	 * 
	 * @throws SPECCHIOFacotryException	database error
	 */
	@POST
	@Path("getReferenceSpace")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public ReferenceSpaceStruct getReferenceSpace(SpectrumIdsDescriptor query_d) throws SPECCHIOFactoryException {
		
		SpaceFactory factory = new SpaceFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		ReferenceSpaceStruct ref_space_struct = factory.getReferenceSpace(query_d.getSpectrumIds(1));
		factory.dispose();
		
		return ref_space_struct;
	}
	
	
	/**
	 * Get Space objects for a given set of spectra.
	 * 
	 * @param query_d	the query descriptor
	 * 
	 * @return an array of Space objects corresponding to the query descriptor
	 *
	 * @throws SPECCHIOFactoryException	could not connect to the database
	 */
	@POST
	@Path("getSpaces")
	@Produces(MediaType.APPLICATION_XML)
	public Space[] getSpaces(SpaceQueryDescriptor query_d) throws SPECCHIOFactoryException {
		
		//System.out.println("spectrum ids to get space for:" + query_d.spectrum_ids);
		
		SpaceFactory factory = new SpaceFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.setMatchOnlySensor(query_d.split_spaces_by_sensor);
		factory.setMatchOnlySensorAndUnit(query_d.split_spaces_by_sensor_and_unit);
		factory.setOrderByAttribute(query_d.order_by);
		ArrayList<Space> spaces = factory.getSpaces(query_d.spectrum_ids);
		factory.dispose();
		
		return spaces.toArray(new Space[spaces.size()]);
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
	@GET
	@Path("getDirectSpectrumIdsOfHierarchy/{hierarchy_id: [0-9]+}")
	@Produces(MediaType.APPLICATION_XML)	
	public XmlInteger[] getDirectSpectrumIdsOfHierarchy(@PathParam("hierarchy_id") int hierarchy_id) throws SPECCHIOFactoryException {
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		List<Integer> ids = factory.getDirectSpectrumIdsOfHierarchy(hierarchy_id);
		factory.dispose();
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		return adapter.marshalArray(ids);		
		
		
//		@GET
//		@Path("get/{spectrum_id: [0-9]+}/{prepare_metadata}")
//		@Produces(MediaType.APPLICATION_XML)
//		public Spectrum get(
//				@PathParam("spectrum_id") int spectrum_id,
//				@PathParam("prepare_metadata") String prepare_metadata		

	}
	
	/**
	 * Get the identifiers of all spectra that match a full text search.
	 * 
	 * @param search_str		the search string
	 * 
	 * @return an array of identifiers
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */	
	@POST
	@Path("full_text_search")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger[] full_text_search(XmlString search_str) throws SPECCHIOFactoryException {
		
		
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		List<Integer> ids = factory.getSpectrumIdsMatchingFullTextSearch(search_str.getString());
		factory.dispose();
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		return adapter.marshalArray(ids);
		
	}

	/**
	 * Get the identifiers of all spectra that match a full text search.
	 *
	 * @param campaignId		the selected campaign
	 *
	 * @return an array of identifiers
	 *
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	@POST
	@Path("unprocessed_hierarchies")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger[] unprocessed_Hierarchies(XmlString campaignId) throws SPECCHIOFactoryException{
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		List<Integer> ids = factory.getUnprocessedHierarchies(campaignId.getString());
		factory.dispose();

		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		return adapter.marshalArray(ids);
	}
	
	
	/**
	 * Get target-reference links that refer to a given sets of targets and references.
	 * 
	 * @param ids_d	the spectrum identifiers (set 1 = targets, set 2 = references)
	 * 
	 * @return an array of SpectrumDataLink objects
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("getTargetReferenceLinks")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public SpectrumDataLink[] getTargetReferenceLinks(SpectrumIdsDescriptor ids_d) throws SPECCHIOFactoryException {
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		SpectrumDataLink datalinks[] = factory.getTargetReferenceLinks(ids_d.getSpectrumIds(1), ids_d.getSpectrumIds(2), getSecurityContext().isUserInRole(UserRoles.ADMIN));
		factory.dispose();
		
		return datalinks;
		
	}
	
	
	
	/**
	 * Insert links from a target to a set of references.
	 * 
	 * @param id_d	a spectrum identifier descriptor with the target in the first list and the references in the second list
	 * 
	 * @return the number of links successfully created
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("insertClosestTargetReferenceLink")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger insertClosestTargetReferenceLink(SpectrumIdsDescriptor id_d) throws SPECCHIOFactoryException {
		
		int num = 0;
		if (id_d.getSpectrumIds1().size() > 0) {
			SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
			try {
				num = factory.insertTargetReferenceLinks(id_d.getSpectrumId(1, 0), id_d.getSpectrumIds2(), true);
			}
			catch (IllegalArgumentException ex) {
				throw new BadRequestException(ex);
			}
			finally {
				factory.dispose();
			}
		}
		
		return new XmlInteger(num);
	}
		
	/**
	 * Insert links from a target to a set of references.
	 * 
	 * @param id_d	a spectrum identifier descriptor with the target in the first list and the references in the second list
	 * 
	 * @return the number of links successfully created
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("insertTargetReferenceLinks")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger insertTargetReferenceLinks(SpectrumIdsDescriptor id_d) throws SPECCHIOFactoryException {
		
		int num = 0;
		if (id_d.getSpectrumIds1().size() > 0) {
			SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
			try {
				num = factory.insertTargetReferenceLinks(id_d.getSpectrumId(1, 0), id_d.getSpectrumIds2(), false);
			}
			catch (IllegalArgumentException ex) {
				throw new BadRequestException(ex);
			}
			finally {
				factory.dispose();
			}
		}
		
		return new XmlInteger(num);
	}
			
	
	/**
	 * Load a Space object from the database.
	 * 
	 * @param space	a partially-filled Space object
	 * 
	 * @return the input Space object with all of its fields filled
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Path("loadSpace")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public Space loadSpace(Space space) throws SPECCHIOFactoryException {
		
		SpaceFactory factory = new SpaceFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.loadSpace(space);
		factory.dispose();
		
		return space;
		
	}
	
	
	/**
	 * Remove a spectrum from the database.
	 * 
	 * @param spectrum_id	the identifier of the spectrum to be removed
	 * 
	 * @returns 1
	 * 
	 * @throws SPECCHIOFactoryException	the spectrum could not be removed
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("remove/{spectrum_id: [0-9]+}")
	public XmlInteger remove(@PathParam("spectrum_id") int spectrum_id) throws SPECCHIOFactoryException {
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		ArrayList<Integer> spectrum_ids = new ArrayList<Integer>();
		spectrum_ids.add(spectrum_id);
		factory.removeSpectra(spectrum_ids, getSecurityContext().isUserInRole(UserRoles.ADMIN));
		factory.dispose();
		
		return new XmlInteger(1);
		
	}
	
	/**
	 * Remove a sub-hierarchy from the database.
	 * 
	 * @param d		structure holding the identifiers of the spectra to be removed
	 * 
	 * @return 1
	 * 
	 * @throws SPECCHIOFactoryException	the spectra could not be removed
	 */
	@POST
	@Path("removeSpectra")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger removeSpectra(SpectrumIdsDescriptor d) throws SPECCHIOFactoryException {
		
//		@GET
//		@Produces(MediaType.APPLICATION_XML)
//		@Path("removeCampaigns/{campaign_id: [0-9]+}")
//		public XmlInteger removeCampaigns(
//				@PathParam("campaign_type") String campaign_type,
//				@PathParam("campaign_id") int campaign_id
//			) throws SPECCHIOFactoryException {
			
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
			factory.removeSpectra(d.getSpectrumIds1(), getSecurityContext().isUserInRole(UserRoles.ADMIN));
			factory.dispose();
		
		return new XmlInteger(1);
		
	}		
	
	
	/**
	 * Get the pictures associated with a given spectrum.
	 * 
	 * @param spectrum_id	the spectrum identifier
	 * 
	 * @return a PictureTable object containing the picture data
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@GET
	@Path("pictures/{spectrum_id: [0-9]+}")
	@Produces(MediaType.APPLICATION_XML)
	public PictureTable pictures(@PathParam("spectrum_id") int spectrum_id) throws SPECCHIOFactoryException {
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		PictureTable pictures = factory.getPictures(spectrum_id);
		factory.dispose();
		
		return pictures;
		
	}
	
	
	/**
	 * Get the identifiers of all spectra that match a given query.
	 * 
	 * @param query		the query
	 * 
	 * @return an array of identifiers
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	@POST
	@Path("query")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger[] query(Query query) throws SPECCHIOFactoryException {
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		List<Integer> ids = factory.getIdsMatchingQuery(query);
		factory.dispose();
		
		XmlIntegerAdapter adapter = new XmlIntegerAdapter();
		return adapter.marshalArray(ids);
		
	}
	
	
	/**
	 * Get the number of spectra that match a given query.
	 * 
	 * @param query		the query
	 * 
	 * @return the number of spectra in the database that match the given query
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	@POST
	@Path("queryCount")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger queryCount(Query query) throws SPECCHIOFactoryException {
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		int count = factory.countIdsMatchingQuery(query);
		factory.dispose();
		
		return new XmlInteger(count);
		
	}
	
	
	/**
	 * Sort spectra by the values of the specified attributes
	 * 
	 * @param spectrum_ids	list of ids to sort
	 * @param attribute_names	attribute names to sort by
	 * 
	 * @return a AVMatchingListCollection object
	 */	
	@POST
	@Path("sortByAttributes")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public AVMatchingListCollection sortByAttributes(AVMatchingList av_list)throws SPECCHIOFactoryException {
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		return factory.sortByAttributes(av_list);
		
	}	
	
	
	
	/**
	 * Update spectrum metadata.
	 * 
	 * @param update_d	the update descriptor
	 * 
	 * @return 0
	 * 
	 * @throws SPECCHIOFactoryException	could not access the database
	 */
	@POST
	@Path("update_metadata")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger update_metadata(SpectraMetadataUpdateDescriptor update_d) throws SPECCHIOFactoryException {
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.updateMetadata(update_d.getIds(), update_d.getField(), update_d.getValue(), getSecurityContext().isUserInRole(UserRoles.ADMIN));
		factory.dispose();
		
		return new XmlInteger(0);
		
	}
	
	/**
	 * Update the spectral vector of a spectrum
	 * 
	 * @param spectrum	the spectrum to be updated
	 * 
	 * @throws SPECCHIOClientException
	 */
	@POST
	@Path("update_vector")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger update_vector(Spectrum spectrum) throws SPECCHIOFactoryException {
		
		SpectrumFactory factory = new SpectrumFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.updateSpectrumVector(spectrum);
		factory.dispose();
		
		return new XmlInteger(0);
		
	}	
	

}
