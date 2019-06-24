package ch.specchio.services;

import javax.annotation.security.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import au.ands.org.researchdata.ResearchDataAustralia;
import au.ands.org.researchdata.SpectralLibrary;
import ch.specchio.constants.UserRoles;
import ch.specchio.factories.SPECCHIOFactoryException;
import ch.specchio.factories.UserFactory;
import ch.specchio.services.BadRequestException;
import ch.specchio.types.User;


/**
 * User services.
 */
@Path("/user")
@DeclareRoles({UserRoles.ADMIN, UserRoles.USER})
public class UserService extends SPECCHIOService {
	
	
	
	/**
	 * Create a new user account on this service: only possible for admin users already logged into this server and database
	 * 
	 * @param user	an object describing the user to be created
	 *
	 * @throws SecurityException		a non-admin user tried to create an admin user
	 * @throws SPECCHIOFactoryException	database error
	 * 
	 * @return a user object containing the complete details of the account
	 */
	@POST
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	@Path("createUserAccount")
	public User createUserAccount(User user) throws SPECCHIOFactoryException {
		
		
		UserFactory factory = new UserFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());

		if (!getSecurityContext().isUserInRole(UserRoles.ADMIN)) {
			// a non-admin user is trying to create a user on this system
			throw new SecurityException("Non-admin users can not create users. Nice try, guys.");
		}

		if (user.isInRole(UserRoles.ADMIN)) {
			// an anonymous user is trying to create an admin user
			throw new SecurityException("Administrator accounts cannot be created using this service.");
		}
		
		// create a user factory to do the work
//		UserFactory factory = new UserFactory(getDataSourceName());
		try {
		
			// create the account
			factory.insertUser(user);
			
			if (getServerCapability(ResearchDataAustralia.ANDS_SERVER_CAPABILITY) != null) {
				// make sure the user has an ANDS party identifier
				if (user.getExternalId() == null || user.getExternalId().length() == 0) {
					user.setExternalId(ResearchDataAustralia.generatePartyIdentifier(SpectralLibrary.PARTY_ID_PREFIX, user));
					factory.updateUser(user);
				}
			}
			
		}
		catch (IllegalArgumentException ex) {
			// illegal data in the user object
			throw new BadRequestException(ex);
		}
		finally {
			// clean up
			factory.dispose();
		}
		
		return user;
		
	}	
	
	/**
	 * List all of the users in the database with user statistics added.
	 * 
	 * @return an array of User objects
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("getUsersWithStatistics")
	public User[] getUsersWithStatistics() throws SPECCHIOFactoryException {
		
		UserFactory factory = new UserFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		User users[] = factory.getUsersWithStatistics();
		factory.dispose();
		
		return users;
		
	}	
	
	/**
	 * List all of the users in the database.
	 * 
	 * @return an array of User objects
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("list")
	public User[] list() throws SPECCHIOFactoryException {
		
		UserFactory factory = new UserFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		User users[] = factory.getUsers();
		factory.dispose();
		
		return users;
		
	}
	
	
	/**
	 * Log in to the database.
	 * 
	 * @return a user object representing the user
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("login")
	public User login() throws SPECCHIOFactoryException {
		
		UserFactory factory = new UserFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		User user = factory.getUser(getClientUsername());
		factory.dispose();
		
		return user;
		
	}
	
	
	/**
	 * Log out from the database.
	 * 
	 * @return an empty string
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("logout")
	public String logout() {
		
		return "";
		
	}
	
	
	/**
	 * Update a user.
	 * 
	 * @param user	the new user data
	 * 
	 * @throws SecurityException		a non-admin user tried to modify admin-only settings
	 * @throws SPECCHIOFactoryException	database error
	 * 
	 * @return an empty string
	 */
	@POST
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	@Path("update")
	public String updateUser(User user) throws SPECCHIOFactoryException {
		
		// check that non-admin users are behaving themselves
		if (!getSecurityContext().isUserInRole(UserRoles.ADMIN)) {
			if (!getClientUsername().equals(user.getUsername())) {
				// a non-admin user is trying to modify someone else's details
				throw new SecurityException("Non-admin users may only modify their own details.");
			}
			if (!UserRoles.USER.equals(user.getRole())) {
				// a non-admin user is trying to modify someone's role
				throw new SecurityException("Non-admin users may not modify their roles.");
			}
		}
		
		UserFactory factory = new UserFactory(getClientUsername(), getClientPassword(), getDataSourceName(), isAdmin());
		factory.updateUser(user);
		factory.dispose();
		
		return "";
		
	}

}
