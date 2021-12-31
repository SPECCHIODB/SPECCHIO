package ch.specchio.factories;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import ch.specchio.types.Campaign;
import ch.specchio.types.Capabilities;


/**
 * Base class for campaign factories.
 */
public abstract class CampaignFactory extends SPECCHIOFactory {	
	
	
	/**
	 * Constructor.
	 * 
	 * @param db_user		database account user name
	 * @param db_password	database account password
	 * @param db_password	database account password
	 * @param ds_name		data source name
	 * @param is_admin		is the user an administrator? 
	 * @param capabilities		server capabilities and configurations 
	 * 
	 * @throws SPECCHIOFactoryException	could not establish initial context
	 */
	public CampaignFactory(String db_user, String db_password, String ds_name, boolean is_admin, Capabilities capabilities) throws SPECCHIOFactoryException {
		
		super(db_user, db_password, ds_name, is_admin, capabilities);
		
	}
	
	
	/**
	 * Copy constructor. Construct a new factory that uses the same database connection
	 * as an existing factory.
	 * 
	 * @param factory	the existing factory
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public CampaignFactory(SPECCHIOFactory factory) throws SPECCHIOFactoryException {
		
		super(factory);
		
	}
	
	
	/**
	 * Export a campaign to an output stream.
	 * 
	 * @param campaign_id	the identifier of the campaign to be exported
	 * @param os			the output stream to export to
	 * 
	 * @throws SPECCHIOFactoryException	no such campaign
	 * @throws IOException				could not write to the output stream
	 */
	public abstract void exportCampaign(int campaign_id, OutputStream os) throws SPECCHIOFactoryException, IOException;
	
	
	/**
	 * Get the campaign object for a given identifier.
	 * 
	 * @param campaign_id	the identifier of the desired campaign
	 * @param is_admin	is the user an administrator?
	 * 
	 * @return a reference to a campaign object with the given identifier
	 * 
	 * @throws SPECCHIOFactoryException could not create the object
	 */
	public abstract Campaign getCampaign(int campaign_id, boolean is_admin) throws SPECCHIOFactoryException;
	
	
	/**
	 * Get the list of campaigns in the database.
	 * 
	 * @throws SPECCHIOFactoryException the list could not be generated
	 */
	public abstract Campaign[] getCampaigns() throws SPECCHIOFactoryException;
	
	
	/**
	 * Get the campaign factory for a given campaign type.
	 * 
	 * @param db_user		database account user name
	 * @param db_password	database account password
	 * @param type	a string identifying the campaign type
	 * @param datasource_name	a string identifying the datasource
	 * @param is_admin		is the user an administrator? 
	 * 
	 * @throws SPECCHIOFactoryException	invalid campaign type
	 */
	public static SpecchioCampaignFactory getInstance(String db_user, String db_password, String type, String datasource_name, boolean is_admin) throws SPECCHIOFactoryException {
		
		if ("specchio".equals(type)) {
			return new SpecchioCampaignFactory(db_user, db_password, datasource_name, is_admin);
		} else {
			throw new SPECCHIOFactoryException("Unknown campaign type \"" + type + "\".");
		}
		
	}
	
	
	/**
	 * Get the campaign factory for a given campaign type.
	 * 
	 * @param factory	the factory with which the new factory will share a database connection
	 * @param type		a string identifying the campaign type
	 * 
	 * @throws SPECCHIOFactoryException	invalid campaign type
	 */
	public static SpecchioCampaignFactory getInstance(SPECCHIOFactory factory, String type) throws SPECCHIOFactoryException {
		
		if ("specchio".equals(type)) {
			return new SpecchioCampaignFactory(factory);
		} else {
			throw new SPECCHIOFactoryException("Unknown campaign type \"" + type + "\".");
		}
		
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
	public abstract String getHierarchyName(int hierarchy_id) throws SPECCHIOFactoryException;
	
	
	
	/**
	 * Get the identifier of a node in a campaign's hierarchy.
	 * 
	 * @param campaign_id	the identifier of the campaign to be tested
	 * @param name			the name of the node to test
	 * @param parent_id		the identifier of the node's parent
	 * 
	 * @returns the node's identifier, or -1 if the node does not exist
	 * 
	 * @throws SPECCHIOFactoryException	the database could not accessed
	 */
	public abstract int getHierarchyNodeId(int campaign_id, String name, int parent_id) throws SPECCHIOFactoryException;
	
	/**
	 * Get the file path of a hierarchy.
	 * 
	 * @param hierarchy_id		the identifier of the hierarchy
	 * 
	 * @returns path as string
	 * 
	 * @throws SPECCHIOFactoryException	the database could not accessed
	 */	
	public abstract String getHierarchyFilePath(int hierarchy_id) throws SPECCHIOFactoryException;
	
	/**
	 * Import a campaign from an input stream.
	 * 
	 * @param userId	the identifier of the user who will own the imported campaign
	 * @param is		the input stream
	 *
	 * @throws SPECCHIOFactoryException	the input stream is not in a recognised format
	 * @throws IOException				the stream could not be read
	 */
	public abstract void importCampaign(int userId, InputStream is) throws SPECCHIOFactoryException, IOException;
	
	
	/**
	 * Insert a campaign.
	 * 
	 * @param c	the campaign to insert
	 * 
	 * @throws SPECCHIOFactoryException	the campaign could not be inserted
	 */
	public abstract void insertCampaign(Campaign c) throws SPECCHIOFactoryException;
	
	
	/**
	 * Insert a node into a campaign's hierarchy.
	 * 
	 * @param campaign_id	the identifier of the campaign to which the node is to be added
	 * @param name			the name of the new node
	 * @param parent_id		the identifier of the new node's parent
	 * 
	 * @returns the identifier of the new node
	 * 
	 * @throws SPECCHIOFactoryException	the node could not be inserted
	 */
	public abstract int insertHierarchyNode(int campaign_id, String name, int parent_id) throws SPECCHIOFactoryException;
	
	
	/**
	 * Remove a campaign from the database.
	 * 
	 * @param id		the identifier of the campaign to be removed
	 * @param is_admin	is the requesting user an administrator?
	 * 
	 * @throws SPECCHIOFactoryException	the campaign could not be removed
	 */
	public abstract void removeCampaign(int campaign_id, boolean is_admin) throws SPECCHIOFactoryException;
	
	/**
	 * Remove a campaign from the database.
	 * 
	 * @param ids		the identifiers of the campaigns to be removed
	 * @param is_admin	is the requesting user an administrator?
	 * 
	 * @throws SPECCHIOFactoryException	the campaign could not be removed
	 */
	public abstract void removeCampaigns(ArrayList<Integer> ids, boolean is_admin) throws SPECCHIOFactoryException;
	
	
	/**
	 * Remove a whole sub-hierarchy from the database.
	 * 
	 * @param hierarchy_id	the identifier of the node at the root of the sub-hierarchy
	 * @param is_admin		is the requesting user an administrator?
	 * 
	 * @throws SPECCHIOFactoryException	the sub-hierarchy could not be remove
	 */
	public abstract void removeHierarchyNode(int hierarchy_id, boolean is_admin) throws SPECCHIOFactoryException;
	
	/**
	 * Remove a whole sub-hierarchies from the database.
	 * 
	 * @param ids			the identifiers of the nodes at the root of the sub-hierarchies
	 * @param is_admin		is the requesting user an administrator?
	 * 
	 * @throws SPECCHIOFactoryException	the sub-hierarchy could not be remove
	 */
	public abstract void removeHierarchyNodes(ArrayList<Integer> ids, boolean is_admin) throws SPECCHIOFactoryException;	
	
	/**
	 * Rename a hierarchy in the database and also on the file system if path is accessible.
	 * 
	 * @param hierarchy_id	id of the hierarchy to be renamed
	 * @param name	new name of the hierarchy
	 */	
	public abstract void renameHierarchy(int hierarchy_id, String name) throws SPECCHIOFactoryException;
	
	/**
	 * Update campaign information.
	 * 
	 * @param campaign	the new campaign information
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public abstract void updateCampaign(Campaign campaign) throws SPECCHIOFactoryException;

}
