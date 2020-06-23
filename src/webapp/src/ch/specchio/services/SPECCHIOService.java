package ch.specchio.services;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import au.ands.org.researchdata.ResearchDataAustralia;

import ch.specchio.constants.UserRoles;
import ch.specchio.eav_db.EAVDBServices;
import ch.specchio.factories.SPECCHIOFactory;
import ch.specchio.factories.SPECCHIOFactoryException;
import ch.specchio.types.Capabilities;


/**
 * Base class for all SPECCHIO web services. This class provides a few common
 * security services used throughout the web application.
 */
public class SPECCHIOService {
	
	/** service version number */
	private static String VERSION = "";
	
	/** server capabilities */
	private Capabilities capabilities = null;
	
	/** the servlet configuration */
	@Context
	private ServletConfig config;
	
	/** the HTTP request */
	@Context
	private HttpServletRequest request;
	
	/** the HTTP response */
	@Context
	private HttpServletResponse response;
	
	/** the security context */
	@Context
	private SecurityContext security;
	
	/** the "Authorization" header */
	@HeaderParam("Authorization")
	private String auth;
	
	/** the user name associated with the request*/
	private String username = null;
	
	/** the password associated with the request */

	private String password = null;
	
	/** the datasource associated with the request */
	private String ds_name = null;	
	
	
	/**
	 * Extract the username and password from the "Authorization" header
	 */
	private void configureAuthorization() {
		
		// split the "Authorization" header into the type and token
		String auth_parts[] = auth.split(" ");
		
		// decode the cookie
		String cookie = new String(javax.xml.bind.DatatypeConverter.parseBase64Binary(auth_parts[1]));
		
		// split the cookie into the username and password
		String cookie_parts[] = cookie.split(":");
		username = cookie_parts[0];
		password = cookie_parts[1];
		
	}
	
	
	/**
	 * Configure the server's capabilities object.
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private void configureCapabilities() throws SPECCHIOFactoryException {
		
		// create a new capabilities object
		capabilities = new Capabilities();
	
		// set version number
		capabilities.setCapability(Capabilities.VERSION, VERSION);
	
		// enable or disable ANDS features
		String andsParameter = config.getInitParameter(ResearchDataAustralia.ANDS_INIT_PARAM_NAME);
		if (andsParameter != null && !andsParameter.equalsIgnoreCase("disabled")) {
			capabilities.setCapability(ResearchDataAustralia.ANDS_SERVER_CAPABILITY, "enabled");
		}
		
		// enable or disable license features
		String license = config.getInitParameter("END_USER_LICENSE");
		if (license != null && !license.equalsIgnoreCase("disabled")) {
			capabilities.setCapability("END_USER_LICENSE", "enabled");
			capabilities.setCapability("END_USER_LICENSE_SHORT_TEXT", config.getInitParameter("END_USER_LICENSE_SHORT_TEXT"));
			capabilities.setCapability("END_USER_LICENSE_URL", config.getInitParameter("END_USER_LICENSE_URL"));
		}	
		
		
		// enable or disable user account creation restrictions (enabled = only admin users can create new users)
		String user_account_creation_restriction = config.getInitParameter("SCHEMA_WITH_USER_ACCOUNT_CREATION_RESTRICTION");
		if (user_account_creation_restriction != null) {
			capabilities.setCapability("SCHEMA_WITH_USER_ACCOUNT_CREATION_RESTRICTION", user_account_creation_restriction);
		}	
		
		// enable or disable default read only user creation
		String read_only_user_accounts = config.getInitParameter(Capabilities.CREATE_READ_ONLY_USERS_BY_DEFAULT);
		if (read_only_user_accounts != null && read_only_user_accounts.equalsIgnoreCase("enabled")) {
			capabilities.setCapability(Capabilities.CREATE_READ_ONLY_USERS_BY_DEFAULT, read_only_user_accounts);
		}
		else
		{
			capabilities.setCapability(Capabilities.CREATE_READ_ONLY_USERS_BY_DEFAULT, "disabled");		
		}
		
		
		// set database capabilities
		SPECCHIOFactory factory = new SPECCHIOFactory(getDataSourceName());

		Long maxObjectSize = factory.getMaximumQuerySize() - 1024;
		capabilities.setCapability(Capabilities.MAX_OBJECT_SIZE, maxObjectSize.toString());
		
		Double db_version = factory.getDatabaseVersion();
		capabilities.setCapability(Capabilities.DB_VERSION, db_version.toString());
		
		EAVDBServices eav = factory.getEavServices();
		Boolean spatially_enabled = eav.isSpatially_enabled();
		capabilities.setCapability(Capabilities.SPATIAL_EXTENSION, spatially_enabled.toString());

		capabilities.setCapability(Capabilities.SERVER_VERSION, SPECCHIO_ReleaseInfo.getVersion());
		capabilities.setCapability(Capabilities.SERVER_BUILD_NUMBER, Integer.toString(SPECCHIO_ReleaseInfo.getBuildNumber()));
		
		factory.dispose();
		
	}
	
	
	/**
	 * Get the password associated with the request.
	 * 
	 * @return the password sent in the "Authorization" header
	 */
	public String getClientPassword() {
		
		if (password == null) {
			configureAuthorization();
		}
	
		return password;
		
	}
	
	
	/**
	 * Get the user name associated with the request.
	 * 
	 * @return the username sent in the "Authorization" header
	 */
	public String getClientUsername() {
		
		if (username == null) {
			configureAuthorization();
		}
		
		return username;
			
	}

	/**
	 * Get the data source name associated with the request.
	 * 
	 * @return the data source name sent in the "Authorization" header
	 */
	public String getDataSourceName() {
		
		if (ds_name == null) {
			
			Cookie[] cookies = request.getCookies();
			
			// first cookie should contain the data source name
			if(cookies != null && cookies.length > 0 && cookies[0].getName().equals("DataSourceName"))
			{
				ds_name = cookies[0].getValue();
			}
			else
			{
				ds_name = "jdbc/specchio"; // default name
			}
			
		}
		
		return ds_name;
			
	}	
	
	
	/**
	 * Get the current request.
	 * 
<<<<<<< .merge_file_DGrXfg
	 * @return the HttpServletRequest object for the current request
=======
	 * @return the HttpServletRequest object for the current reuest
>>>>>>> .merge_file_wDkp6h
	 */
	public HttpServletRequest getRequest() {
		
		return request;
		
	}
	
	
	/**
	 * Get the servlet response for the current request.
	 * 
	 * @return the HttpServletResponse object for the current request
	 */
	public HttpServletResponse getResponse() {
		
		return response;
		
	}
	
	
	/**
	 * Get the security context for the current request.
	 * 
	 * @return the SecurityContext object for the current request
	 */
	public SecurityContext getSecurityContext() {
		
		return security;
		
	}
	
	/**
	 * Returns true if current user is admin
	 * 
	 * @return Returns true if current user is admin
	 */
	public boolean isAdmin() {
		
		Principal name = security.getUserPrincipal();
		return security.isUserInRole(UserRoles.ADMIN);		
	}	
	
	
	/**
	 * Get the server capabilities.
	 * 
	 * @return a Capabilities object
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public Capabilities getServerCapabilities() throws SPECCHIOFactoryException {
		
		if (capabilities == null) {
			configureCapabilities();
		}
		
		return capabilities;
		
	}
	
	
	/**
	 * Get the value of a server capability.
	 * 
	 * @param capability	the capability
	 * 
	 * @return the value of the capability, or null if the capability does not exist
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public String getServerCapability(String capability) throws SPECCHIOFactoryException {
		
		return getServerCapabilities().getCapability(capability);
		
	}
	
	
	
	/**
	 * Upgrade the DB.
	 * 
	 * @param campaign_type	the type of campaign to be import
	 * 
	 * @throws SecurityException		a non-admin user tried to upgrade the database
	 * @throws SPECCHIOFactoryException	the request body is not in the correct format
	 */
	@POST
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Path("dbUpgrade/{version: [0-9.]+}")
	public Response dbUpgrade(
			@PathParam("version") double version
		) throws SPECCHIOFactoryException {
		
		Response response;
		
		if (!getSecurityContext().isUserInRole(UserRoles.ADMIN)) {
			response = Response.status(Response.Status.FORBIDDEN).build();
		} else {
		
			SPECCHIOFactory factory = new SPECCHIOFactory(getClientUsername(), getClientPassword(), getDataSourceName(), getSecurityContext().isUserInRole(UserRoles.ADMIN));
			try {
								
				factory.dbUpgrade(version, getRequest().getInputStream());
				response = Response.ok().build();
			}
			catch (IOException ex) {
				// malformed input
				response = Response.status(Response.Status.BAD_REQUEST).build();
			}
			factory.dispose();
			
		}
		
		return response;
		
	}	

}
