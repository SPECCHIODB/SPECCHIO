package ch.specchio.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.*;
import javax.ws.rs.core.*;


import javax.annotation.security.*;


//import com.sun.jersey.api.client.ClientResponse;

import ch.specchio.constants.UserRoles;
import ch.specchio.factories.InstrumentationFactory;
import ch.specchio.factories.SPECCHIOFactoryException;
import ch.specchio.factories.SpecchioCampaignFactory;
import ch.specchio.factories.SpectralFileFactory;
import ch.specchio.jaxb.XmlBoolean;
import ch.specchio.jaxb.XmlInteger;
import ch.specchio.types.Campaign;
import ch.specchio.types.ChildParentIdContainer;
import ch.specchio.types.Hierarchy;
import ch.specchio.types.Instrument;
import ch.specchio.types.SpectralFile;
import ch.specchio.types.SpectrumIdsDescriptor;

/**
 * Campaign services.
 */
@Path("/campaign")
@DeclareRoles({UserRoles.ADMIN, UserRoles.USER})
public class CampaignService extends SPECCHIOService {
	
	
	
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
	@GET
	@Path("copyHierarchy/{hierarchy_id: [0-9]+}/{target_hierarchy_id: [0-9]+}/{new_name}")
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger copySpectrum(
			@PathParam("hierarchy_id") int hierarchy_id,
			@PathParam("target_hierarchy_id") int target_hierarchy_id,
			@PathParam("new_name") String new_name
		) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		int new_hierarchy_id = factory.copyHierarchy(hierarchy_id, target_hierarchy_id, new_name);
		factory.dispose();
		
		return new XmlInteger(new_hierarchy_id);
		
	}		
	
	/**
	 * Export a campaign.
	 * 
	 * @param campaign_type	the type of campaign to be exported
	 * @param campaign_id	the identifier of the campaign to be exported
	 * 
	 * @throws SPECCHIOFactoryException	the campaign does not exist
	 */
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Path("export/{campaign_type}/{campaign_id: [0-9]+}")
	public Response exportCampaign(
			@PathParam("campaign_type") String campaign_type,
			@PathParam("campaign_id") int campaign_id
		) throws SPECCHIOFactoryException {

		Response response;

		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		try {
			OutputStream os = getResponse().getOutputStream();
			factory.exportCampaign(campaign_id, os);
			os.close();
			response = Response.ok().build();
		}
		catch (IOException ex) {
			// not sure what might cause this
			response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
		factory.dispose();
		
		return response;
		
	}
	
	
	/**
	 * Get the campaign object for a campaign.
	 * 
	 * @param campaign_type	the type of campaign to retrieve
	 * @param campaign_id	the identifier of the campaign to retrieve
	 * 
	 * @return a new campaign object, or null if the identifier does not exit
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("get/{campaign_type}/{campaign_id: [0-9]+}")
	public Campaign get(
			@PathParam("campaign_type") String campaign_type,
			@PathParam("campaign_id") int campaign_id
		) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		Campaign campaign = factory.getCampaign(campaign_id, getSecurityContext().isUserInRole(UserRoles.ADMIN));
		factory.dispose();
		
		return campaign;
		
	}
	
	/**
	 * Get the hierarchy object for a given hierarchy_id
	 * 
	 * @param hierarchy_id	the hierarchy_id identifying the required node
	 * 
	 * @return the hierarchy object, or -1 if the node does not exist
	 * 
	 * @throws SPECCHIOFactoryException	the database could not accessed
	 */	
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("getHierarchy/{hierarchy_id: [0-9]+}")
	public Hierarchy getHierarchy(
			@PathParam("hierarchy_id") int hierarchy_id
		) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		Hierarchy h = factory.getHierarchy(hierarchy_id, getSecurityContext().isUserInRole(UserRoles.ADMIN));
		factory.dispose();
		
		return h;
		
	}
		
	
	
	
	/**
	 * Get the identifier of a node in a campaign's hierarchy (deprecated)
	 * 
	 * @param campaign_type		the type of campaign to test
	 * @param campaign_id		the identifier of the campaign to be tested
	 * @param name				the name of the node to test
	 * @param parent_id			the identifier of the node's parent
	 * 
	 * @returns the node's identifier, or -1 if the node does not exist
	 * 
	 * @throws SPECCHIOFactoryException	the database could not accessed
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("getHierarchyId/{campaign_type}/{campaign_id: [0-9]+}/{name}/{parent_id: [0-9]+}")
	public XmlInteger getHierarchyId(
			@PathParam("campaign_type") String campaign_type,
			@PathParam("campaign_id") int campaign_id,
			@PathParam("name") String name,
			@PathParam("parent_id") int parent_id
		) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		int id = factory.getHierarchyNodeId(name, parent_id);
		factory.dispose();
		
		return new XmlInteger(id);
		
	}
	
	
	/**
	 * Get the identifier of a node in a campaign's hierarchy.
	 * 
	 * @param campaign_type		the type of campaign to test
	 * @param name				the name of the node to test
	 * @param parent_id			the identifier of the node's parent
	 * 
	 * @returns the node's identifier, or -1 if the node does not exist
	 * 
	 * @throws SPECCHIOFactoryException	the database could not accessed
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("getHierarchyId/{campaign_type}/{name}/{parent_id: [0-9]+}")
	public XmlInteger getHierarchyId(
			@PathParam("campaign_type") String campaign_type,
			@PathParam("name") String name,
			@PathParam("parent_id") int parent_id
		) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		int id = factory.getHierarchyNodeId(name, parent_id);
		factory.dispose();
		
		return new XmlInteger(id);
		
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
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("getHierarchyName/{hierarchy_id: [0-9]+}")	
	public String getHierarchyName(@PathParam("hierarchy_id") int hierarchy_id) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		
		String name = factory.getHierarchyName(hierarchy_id);
		
		factory.dispose();
		
		
		return name;
		
	}
			
	
	
	/**
	 * Import a campaign.
	 * 
	 * @param campaign_type	the type of campaign to be import
	 * 
	 * @throws SecurityException		a non-admin user tried to import a campaign
	 * @throws SPECCHIOFactoryException	the request body is not in the correct format
	 */
	@POST
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Path("import/{campaign_type}/{user_id: [0-9]+}")
	public Response importCampaign(
			@PathParam("campaign_type") String campaign_type,
			@PathParam("user_id") int user_id
		) throws SPECCHIOFactoryException {
		
		Response response;
		
		if (!getSecurityContext().isUserInRole(UserRoles.ADMIN)) {
			response = Response.status(Response.Status.FORBIDDEN).build();
		} else {
		
			SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
			try {
				factory.importCampaign(user_id, getRequest().getInputStream());
				response = Response.ok().build();
			}
			catch (IOException ex) {
				// malformed input
				response = Response.status(Response.Status.BAD_REQUEST).build();
			}
			// somehow we always get an IOException at the client end, no matter what exception we throw or response is returned.
//			catch (SPECCHIOFactoryException ex) {
//				response = Response.status(ClientResponse.Status.BAD_REQUEST).tag(ex.getMessage()).build();
//			}
			factory.dispose();
			
		}
		
		return response;
		
	}
	
	
	/**
	 * Import a campaign.
	 * 
	 * @param campaign_type	the type of campaign to be import
	 * 
	 * @throws SecurityException		a non-admin user tried to import a campaign
	 * @throws SPECCHIOFactoryException	the request body is not in the correct format
	 */
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("import_from_server_file")
	public XmlInteger import_from_server_file(Campaign c) throws SPECCHIOFactoryException {
		
		Response response;
		
		if (!getSecurityContext().isUserInRole(UserRoles.ADMIN)) {
			response = Response.status(Response.Status.FORBIDDEN).build();
		} else {
		
			SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
			try {
				
				FileInputStream fis = new FileInputStream(c.getPath());
				
				
				factory.importCampaign(c.getUser().getUserId(), fis);

			}
			catch (IOException ex) {
				// malformed input
				response = Response.status(Response.Status.BAD_REQUEST).build();
			}
			factory.dispose();
			
		}
		
		return new XmlInteger(1);
		
	}
	


	/**
	 * Create a new campaign.
	 * 
	 * @param c		the campaign to be created
	 * 
	 * @return the identifier of the new campaign
	 */
	@POST
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("insert")
	public XmlInteger insert(Campaign c) throws SPECCHIOFactoryException {
		
		System.out.println("Inserting new campaign " + c.getName());
		
		try
		{
		
			SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.insertCampaign(c);
		factory.dispose();
		
		}
		catch(SPECCHIOFactoryException e)
		{
			System.out.println(e.toString());
			throw(e);
		}
		
		return new XmlInteger(c.getId());
		
	}
	
	
	/**
	 * Insert a node into a campaign's hierarchy.
	 * 
	 * @param campaign_type		the type of campaign into which the node will be inserted
	 * @param campaign_id		the identifier of the campaign to which the node is to be added
	 * @param name				the name of the new node
	 * @param parent_id			the identifier of the new node's parent
	 * 
	 * @returns the identifier of the new node
	 * 
	 * @throws SPECCHIOFactoryException	the node could not be inserted
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("insertHierarchy/{campaign_type}/{campaign_id: [0-9]+}/{name}/{parent_id: [0-9]+}")
	public XmlInteger insertHierarchy(
			@PathParam("campaign_type") String campaign_type,
			@PathParam("campaign_id") int campaign_id,
			@PathParam("name") String name,
			@PathParam("parent_id") int parent_id
		) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		int id = factory.insertHierarchyNode(name, parent_id, campaign_id);
		factory.dispose();
		
		return new XmlInteger(id);
		
	}
	
	/**
	 * Get the identifier of a sub-hierarchy with a given name, creating the
	 * hierarchy if it doesn't exist (deprecated)
	 * 
	 * @param campaign	the campaign into which to insert the hierarchy
	 * @param parent_id			the identifier of the the parent of the hierarchy
	 * @param hierarchy_name	the name of the desired hierarchy
	 * 
	 * @return the identifier of the child of parent_id with the name hierarchy_name
	 *
	 * @throws SPECCHIOFactoryException
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("getSubHierarchyId/{campaign_type}/{campaign_id: [0-9]+}/{parent_id: [0-9]+}/{hierarchy_name}")	
	public XmlInteger getSubHierarchyId(
			@PathParam("campaign_type") String campaign_type,
			@PathParam("campaign_id") int campaign_id,			
			@PathParam("parent_id") int parent_id,
			@PathParam("hierarchy_name") String hierarchy_name
			) throws SPECCHIOFactoryException {
		
		SpectralFileFactory factory = new SpectralFileFactory(
				getClientUsername(),
				getClientPassword(),
				getSecurityContext().isUserInRole(UserRoles.ADMIN),
				getDataSourceName(),
				campaign_id
			);		
		
		int id = factory.getSubHierarchyId(parent_id, hierarchy_name);
		
		factory.dispose();
		
		return new XmlInteger(id);		
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
	 *
	 * @throws SPECCHIOFactoryException
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("getSubHierarchyId/{campaign_type}/{parent_id: [0-9]+}/{hierarchy_name}")	
	public XmlInteger getSubHierarchyId(
			@PathParam("campaign_type") String campaign_type,		
			@PathParam("parent_id") int parent_id,
			@PathParam("hierarchy_name") String hierarchy_name
			) throws SPECCHIOFactoryException {
		
		SpectralFileFactory factory = new SpectralFileFactory(
				getClientUsername(),
				getClientPassword(),
				getDataSourceName(),
				getSecurityContext().isUserInRole(UserRoles.ADMIN)		
			);		
		
		int id = factory.getSubHierarchyId(parent_id, hierarchy_name);
		
		factory.dispose();
		
		return new XmlInteger(id);		
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
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("getHierarchyFilePath/{hierarchy_id: [0-9]+}")	
	public String getHierarchyFilePath(@PathParam("hierarchy_id") int hierarchy_id) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		
		String path = factory.getHierarchyFilePath(hierarchy_id);
		
		factory.dispose();
		
		
		return path;
		
	}
	
	
	
	/**
	 * Get a detailed list of the campaigns in the database.
	 * 
	 * @param campaign_type	the type of campaigns to list
	 * 
	 * @throws SPECCHIOFactoryException	the database could not accessed
	 * 
	 * @return a list of the campaigns in the database with additional detailed metadata per campaign
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("detailed_list/{campaign_type}")
	public Campaign[] detailed_list(@PathParam("campaign_type") String campaign_type) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		Campaign[] campaigns = factory.getCampaigns(getSecurityContext().isUserInRole(UserRoles.ADMIN), true);
		factory.dispose();
		
		return campaigns;
		
	}
	
	/**
	 * Get a list of the campaigns in the database.
	 * 
	 * @param campaign_type	the type of campaigns to list
	 * 
	 * @throws SPECCHIOFactoryException	the database could not accessed
	 * 
	 * @return a list of the campaigns in the database
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("list/{campaign_type}")
	public Campaign[] list(@PathParam("campaign_type") String campaign_type) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		Campaign[] campaigns = factory.getCampaigns(getSecurityContext().isUserInRole(UserRoles.ADMIN));
		factory.dispose();
		
		return campaigns;
		
	}	
	
	/**
	 * Move a hierarchy to a new parent hierarchy within the same campaign. If a hierarchy of the same name exists in the target hierarchy then the hierarchies are merged.
	 * 
	 * @param source_and_target_parent_hierarchy_ids		Structure containing source and target hierarchy ids
	 * 
	 * return true if move was done
	 */
	@POST
	@Path("moveHierarchy")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlBoolean moveHierarchy(ChildParentIdContainer source_and_target_parent_hierarchy_ids) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		
		boolean b = factory.moveHierarchy(source_and_target_parent_hierarchy_ids, isAdmin());
		
		factory.dispose();
		
		
		return  new XmlBoolean(b);
	}			
	
	

	
	
	
	/**
	 * Remove a campaign from the database.
	 * 
	 * @param campaign_type	the type of the campaign to be removed
	 * @param campaign_id	the identifier of the campaign to be removed
	 * 
	 * @return 1
	 * 
	 * @throws SPECCHIOFactoryException	the campaign could not be removed
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("remove/{campaign_type}/{campaign_id: [0-9]+}")
	public XmlInteger remove(
			@PathParam("campaign_type") String campaign_type,
			@PathParam("campaign_id") int campaign_id
		) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.removeCampaign(campaign_id, getSecurityContext().isUserInRole(UserRoles.ADMIN));
		factory.dispose();
		
		return new XmlInteger(1);
		
	}
	
	/**
	 * Remove campaigns from the database.
	 * 
	 * @param campaign_id	the identifier of the campaign to be removed
	 * 
	 * @return 1
	 * 
	 * @throws SPECCHIOFactoryException	the campaign could not be removed
	 */
	@POST
	@Path("removeCampaigns")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public XmlInteger removeCampaigns(SpectrumIdsDescriptor d) throws SPECCHIOFactoryException {
	
//	@GET
//	@Produces(MediaType.APPLICATION_XML)
//	@Path("removeCampaigns/{campaign_id: [0-9]+}")
//	public XmlInteger removeCampaigns(
//			@PathParam("campaign_type") String campaign_type,
//			@PathParam("campaign_id") int campaign_id
//		) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.removeCampaigns(d.getSpectrumIds1(), getSecurityContext().isUserInRole(UserRoles.ADMIN));
		factory.dispose();
		
		return new XmlInteger(1);
		
	}	
	
	
	/**
	 * Remove a sub-hierarchy from the database.
	 * 
	 * @param campaign_type		the type of the campaign from which the sub-hierarchy will be removed
	 * @param hierarchy_id		the identifier of the node at the root of the sub-hierarchy to be removed
	 * 
	 * @return 1
	 * 
	 * @throws SPECCHIOFactoryException	the sub-hierarchy could not be removed
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("removeHierarchy/{campaign_type}/{hierarchy_id: [0-9]+}")
	public XmlInteger removeHierarchy(
			@PathParam("campaign_type") String campaign_type,
			@PathParam("hierarchy_id") int hierarchy_id
		) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.removeHierarchyNode(hierarchy_id, getSecurityContext().isUserInRole(UserRoles.ADMIN));
		factory.dispose();
		
		return new XmlInteger(1);
		
	}
	
	/**
	 * Remove a sub-hierarchy from the database.
	 * 
	 * @param hierarchy_ids		the identifier sof the nodes at the root of the sub-hierarchies to be removed
	 * 
	 * @return 1
	 * 
	 * @throws SPECCHIOFactoryException	the sub-hierarchy could not be removed
	 */
	@POST
	@Path("removeHierarchies")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
//	public XmlInteger removeCampaigns(SpectrumIdsDescriptor d) throws SPECCHIOFactoryException {	
//	@GET
//	@Produces(MediaType.APPLICATION_XML)
//	@Path("removeHierarchies/{campaign_type}/{hierarchy_id: [0-9]+}")
	public XmlInteger removeHierarchies(SpectrumIdsDescriptor d) throws SPECCHIOFactoryException {
		
//		@GET
//		@Produces(MediaType.APPLICATION_XML)
//		@Path("removeCampaigns/{campaign_id: [0-9]+}")
//		public XmlInteger removeCampaigns(
//				@PathParam("campaign_type") String campaign_type,
//				@PathParam("campaign_id") int campaign_id
//			) throws SPECCHIOFactoryException {
			
			SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
			factory.removeHierarchyNodes(d.getSpectrumIds1(), getSecurityContext().isUserInRole(UserRoles.ADMIN));
			factory.dispose();
		
		return new XmlInteger(1);
		
	}	
	
	
	/**
	 * Rename a hierarchy
	 * 
	 * @param hierarchy_id	    the identifier of the hierarchy
	 * @param hierarchy_name	the new name of the hierarchy
	 * 
	 * @return 1
	 *
	 * @throws SPECCHIOFactoryException
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("renameHierarchy/{hierarchy_id: [0-9]+}/{hierarchy_name}")	
	public XmlInteger renameHierarchy(		
			@PathParam("hierarchy_id") int hierarchy_id,
			@PathParam("hierarchy_name") String hierarchy_name
			) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		
		factory.renameHierarchy(hierarchy_id, hierarchy_name);
		
		factory.dispose();
		
		return new XmlInteger(1);		
	}	
	
	
	/**
	 * Update campaign information.
	 * 
	 * @param campaign	the new campaign information
	 * 
	 * @return an empty string
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@POST
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	@Path("update")
	public String update(Campaign campaign) throws SPECCHIOFactoryException {
		
		SpecchioCampaignFactory factory = new SpecchioCampaignFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.updateCampaign(campaign);
		factory.dispose();
		
		return "";
		
	}
		
		

}
