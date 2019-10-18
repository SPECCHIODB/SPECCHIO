package ch.specchio.client;

import ch.specchio.types.User;


/**
 * This interface must be implemented by all server descriptors.
 */
public interface SPECCHIOServerDescriptor {
	
	/**
	 * Create a client object suitable for connecting to the server
	 * described by this descriptor.
	 * 
	 * @return a new SPECCHIOClient object
	 * 
	 * @throws SPECCHIOClientException	could not create the client
	 */
	public SPECCHIOClient createClient() throws SPECCHIOClientException;


	/**
	 * Get the display name of this server.
	 * 
	 * @param showUser	include the user account details in the display?
	 * @param show_datasource_name	include the JNDI details in the display?
	 * 
	 * @return a string describing the server, suitable for display to the user
	 */
	public String getDisplayName(boolean showUser, boolean show_datasource_name);
	
	
	/**
	 * Get a description of the user account under which we are to log in.
	 * 
	 * @return a string describing the user account under which this client is to log in
	 */
	public String getDisplayUser();
	
	/**
	 * Get a name of the datasource (JNDI).
	 * 
	 * @return a string holding the name of the datasource (JNDI)
	 */
	public String getDataSourceName();	
	
	
	/**
	 * Set the user information associated with this account.
	 * 
	 * @param user	the user information
	 */
	public void setUser(User user);
	
	/**
	 * Get the node name in preferences or line number in legacy file
	 * 
	 * @return a string with the name or line number
	 */
	
	public String getPreferenceNodeName();	
	
	/**
	 * Defines if this connection relies on the default JVM trust store
	 * 
	 * @return true is default trust store is used
	 */
	
	public Boolean usesDefaultTrustStore();		
	
	
	/**
	 * Defines if this connection is encrypted
	 * 
	 * @return true if this is a HTTPS connection
	 */
	
	public Boolean isEncrypted();		
	
	/**
	 * index at which this descriptor is found in the list of known connections
	 * 
	 * @return index if known, otherwise -1
	 */	
	public int getList_index();

	/**
	 * set index at which this descriptor is found in the list of known connections
	 *
	 */		
	public void setList_index(int list_index);
	

}
