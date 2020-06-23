package ch.specchio.types;

import java.util.Hashtable;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ch.specchio.jaxb.XmlMapAdapter;

/**
 * Server capabilities descriptor
 */
@XmlRootElement(name="capabilities")
public class Capabilities {
	
	/** mapping of capability names to values */
	private Hashtable<String, String> table;
	
	/** capability name for version number of server WAR file */
	public static final String VERSION = "version";
	
	/** capability name for version number of server WAR file */
	public static final String DB_VERSION = "db_version";
	
	/** capability name for the maximum object size */
	public static final String MAX_OBJECT_SIZE = "max_object_size";
	
	/** capability name for the spatial extension */
	public static final String SPATIAL_EXTENSION = "spatial_extension";

	/** capability name for the server version */
	public static final String SERVER_VERSION = "server_version";

	/** capability name for the server build number */
	public static final String SERVER_BUILD_NUMBER = "server_build_number";
	
	/** capability name for the read only user switch */
	public static final String CREATE_READ_ONLY_USERS_BY_DEFAULT = "CREATE_READ_ONLY_USERS_BY_DEFAULT";
	
	
	/**
	 * Default constructor. Constructs an empty capabilities object.
	 */
	public Capabilities() {
		
		this.table = new Hashtable<String, String>();
		
	}
	
	
	@XmlElement(name="table")
	@XmlJavaTypeAdapter(XmlMapAdapter.class)
	public Hashtable<String, String> getTable() { return this.table; }
	public void setTable(Hashtable<String, String> table) { this.table = table; }
	
	
	/**
	 * Get the value of a capability.
	 * 
	 * @param capability	the capability
	 * 
	 * @return the value of the capability, or null if the capability is not known
	 */
	public String getCapability(String capability) {
		
		return table.get(capability);
		
	}
	
	
	/**
	 * Set the value of a capability.
	 * 
	 * @param capability	the capability
	 * @param value			the new value
	 */
	public void setCapability(String capability, String value) {
		
		table.put(capability, value);
		
	}
	

}
