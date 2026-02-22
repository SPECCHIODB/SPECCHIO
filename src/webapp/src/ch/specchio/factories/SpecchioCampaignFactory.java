package ch.specchio.factories;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import ch.specchio.constants.UserRoles;
import ch.specchio.db_import_export.CampaignExport;
import ch.specchio.db_import_export.CampaignImport;
import ch.specchio.eav_db.SQL_StatementBuilder;
import ch.specchio.eav_db.id_and_op_struct;
import ch.specchio.types.Campaign;
import ch.specchio.types.ChildParentIdContainer;
import ch.specchio.types.Hierarchy;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.Point2D;
import ch.specchio.types.ResearchGroup;
import ch.specchio.types.SpecchioCampaign;
import ch.specchio.types.User;
import ch.specchio.types.hierarchy_node;
import ch.specchio.types.spectral_node_object;

public class SpecchioCampaignFactory extends SPECCHIOFactory {
	
	
	/**
	 * Constructor.
	 * 
	 * @param db_user		database account user name
	 * @param db_password	database account password
	 * @param ds_name		data source name
	 * @param is_admin		is the user an administrator? 
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public SpecchioCampaignFactory(String db_user, String db_password, String ds_name, boolean is_admin) throws SPECCHIOFactoryException {
		
		super(db_user, db_password, ds_name, is_admin);
		
	}
	
	
	/**
	 * Copy constructor. Construct a new factory that uses the same database connection
	 * as an existing factory.
	 * 
	 * @param factory	the existing factory
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public SpecchioCampaignFactory(SPECCHIOFactory factory) throws SPECCHIOFactoryException {
		
		super(factory);
		
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
	public int copyHierarchy(int hierarchy_id, int target_hierarchy_id, String new_name) {
		
		
		int copy_hierarchy_id = 0;
		
				
		
		String query = "INSERT INTO hierarchy_level_view ("
		+ "name, parent_level_id, campaign_id) "
		+ "select "
		+ this.getStatementBuilder().quote_string(new_name) + ", " + target_hierarchy_id + ", campaign_id"
		+ " from hierarchy_level where hierarchy_level_id = " + hierarchy_id;
		
		Statement stmt;
		try {
			stmt = getStatementBuilder().createStatement();
			
			
			stmt.executeUpdate(query);
			
			ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			while (rs.next())
				copy_hierarchy_id = rs.getInt(1);
			rs.close();
			
			stmt.close();		
			
			
			// copy all eav references
			ArrayList<Integer> eav_ids = getEavServices().get_eav_ids(MetaParameter.HIERARCHY_LEVEL, hierarchy_id, false); // only get metadata of this hierarchy			
			getEavServices().insert_primary_x_eav(MetaParameter.HIERARCHY_LEVEL, copy_hierarchy_id, eav_ids);					
			
			
		} catch (SQLException e) {
			throw new SPECCHIOFactoryException(e);
		}		
		
		
		return copy_hierarchy_id;
	}

	
	
	
	/**
	 * Create a new research group.
	 * 
	 * @param name	the name of the new research group
	 * 
	 * @return	a ResearchGroup object representing the new group
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private ResearchGroup createResearchGroup(String name) throws SPECCHIOFactoryException {
		
		ResearchGroup group = new ResearchGroup(name);
		
		try {
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			String query;
			
			// create an entry in the research_group table
			query = "insert into " + (Is_admin()?"research_group":"research_group_view") +  "(name) values(" + SQL.quote_string(name) + ")";
			stmt.executeUpdate(query);
			
			// get the identifier for the new group
			query = "select last_insert_id()";
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				group.setId(rs.getInt(1));
			}
			rs.close();
			
			// make the current user into a member of the group
			UserFactory uf = new UserFactory(this);
			User user = uf.getUser(getDatabaseUserName());
			query = "insert into " + (Is_admin()?"research_group_members":"research_group_members_view") +  "(research_group_id,member_id) " +
					"values(" + Integer.toString(group.getId()) + "," + Integer.toString(user.getUserId()) + ")";
			stmt.executeUpdate(query);
			
			// clean up
			stmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return group;
		
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
	public void exportCampaign(int campaign_id, OutputStream os) throws SPECCHIOFactoryException, IOException {
		
		CampaignExport cex = new CampaignExport(getStatementBuilder(), getDatabaseName(), campaign_id);
		cex.export(os);
		
	}
	
	
	/**
	 * Get the campaign object for a given identifier.
	 * 
	 * @param int campaign_id	the identifier of the desired campaign
	 * @param is_admin	is the user an administrator?	
	 * 
	 * @return a reference to a campaign object with the given identifier. Additional metadata is not filled.
	 */
	public Campaign getCampaign(int campaign_id, boolean is_admin) throws SPECCHIOFactoryException {

		return getCampaign(campaign_id, is_admin, false);
		
	}
	
	
	/**
	 * Get the campaign object for a given identifier.
	 * 
	 * @param int campaign_id		the identifier of the desired campaign
	 * @param is_admin				is the user an administrator?	
	 * @param load_detailed_meta	loads all metadata if set to true	
	 * 
	 * @return a reference to a campaign object with the given identifier
	 */
	public Campaign getCampaign(int campaign_id, boolean is_admin, boolean load_detailed_meta) throws SPECCHIOFactoryException {
		
		try {
			
			// create a campaign object
			Campaign campaign = new SpecchioCampaign();
			campaign.setId(campaign_id);
			
			// create SQL-building objects
			Statement stmt = getConnection().createStatement();
			String query;
			ResultSet rs;
			
			// load campaign data from the database
			query = "SELECT name,description,path,user_id,research_group_id FROM " + ((is_admin)? "campaign" : "campaign_view") + " WHERE campaign_id=" + campaign_id; 
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				campaign.setName(rs.getString(1));
				campaign.setDescription(rs.getString(2));
				campaign.setPath(rs.getString(3));
				campaign.setUser_id(rs.getInt(4));
				campaign.setResearchGroupId(rs.getInt(5));
			}
			rs.close();
			
			setCampaignMetadata(campaign, load_detailed_meta);
			
			// clean up
			stmt.close();
			
			return campaign;
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	
	private void setCampaignMetadata(Campaign campaign, boolean load_detailed_meta)
	{
		try {

			// set the campaign paths
			
			Statement stmt = getConnection().createStatement();
			String query = "SELECT path FROM campaign_path_view WHERE campaign_id=" + campaign.getId();
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				campaign.addKnownPath(rs.getString(1));
			}
			rs.close();

			// set the campaign investigator
			if (campaign.getUser_id() != 0) {
				UserFactory uf = new UserFactory(this);
				User investigator = uf.getUser(campaign.getUser_id());
				if (investigator != null) {
					campaign.setInvestigator(investigator.toString());
					campaign.setUser(investigator);
				}
				uf.dispose();
			}

			// set the research group
			if (campaign.getResearchGroupId() != 0) {
				ResearchGroup rg = getResearchGroup(campaign.getResearchGroupId());
				if (rg != null) {
					campaign.setResearchGroup(rg);
				}
			}	
			
			if(load_detailed_meta)
			{
				campaign.setMetadata_details_loaded(true);
				
				// get  number of spectra
				query = "select count(s.spectrum_id) from spectrum s where s.campaign_id = " + campaign.getId();
				rs = stmt.executeQuery(query);
				while (rs.next()) {
					campaign.setNumber_of_spectra(rs.getInt(1));
				}
				rs.close();


				// get metadata space density for spectra
				int eav_spectrum_link_count = 0;
				float spectrum_MSD = 0;
				query = "select count(sxe.spectrum_id) from spectrum_x_eav sxe where sxe.campaign_id = " + campaign.getId();
				rs = stmt.executeQuery(query);
				while (rs.next()) {
					eav_spectrum_link_count = rs.getInt(1);
					spectrum_MSD = ((float) eav_spectrum_link_count) / campaign.getNumber_of_spectra();
				}
				rs.close();

				// get metadata space density for hierarchy: per hierarchy get number of eav entries and number of spectra per hierarchy

				ArrayList<Integer> hierarchy_ids = getHierarchyIds( campaign.getId());
				ArrayList<Float> msd_contributions = new ArrayList<Float>();
				ArrayList<Integer> eav_hierarchy_link_counts = new ArrayList<Integer>();
				ArrayList<Integer> spectrum_in_hierarchy_counts = new ArrayList<Integer>();
				float msd_contribution_sum = 0;
				float msd_contribution = 0;

				//			for(int h_id : hierarchy_ids)
				//			{

				//				int eav_hierarchy_link_count = 0;
				//				int spectrum_in_hierarchy_count = 0;
				//query = "select count(hierarchy_level_id) from hierarchy_x_eav where hierarchy_level_id = " + h_id;
				query = "select count(eav_id), hl.hierarchy_level_id from hierarchy_level hl left join hierarchy_x_eav hxe on hl.hierarchy_level_id = hxe.hierarchy_level_id where hl.hierarchy_level_id in (" + this.getEavServices().SQL.conc_ids(hierarchy_ids) + ") group by hl.hierarchy_level_id";
				rs = stmt.executeQuery(query);
				while (rs.next()) {
					eav_hierarchy_link_counts.add(rs.getInt(1));
				}
				rs.close();

				//				query = "select count(spectrum_id) from hierarchy_level_x_spectrum  where hierarchy_level_id = " + h_id;
				query = "select count(spectrum_id), hl.hierarchy_level_id from hierarchy_level hl left join hierarchy_level_x_spectrum hxs on hl.hierarchy_level_id = hxs.hierarchy_level_id where hl.hierarchy_level_id in (" + this.getEavServices().SQL.conc_ids(hierarchy_ids) + ") group by hl.hierarchy_level_id";
				rs = stmt.executeQuery(query);
				while (rs.next()) {
					spectrum_in_hierarchy_counts.add(rs.getInt(1));
				}
				rs.close();

				//			query = "select count(hxs.spectrum_id) , hxs.hierarchy_level_id, count(hxe.eav_id) from hierarchy_level_x_spectrum hxs LEFT JOIN  hierarchy_x_eav hxe on hxe.hierarchy_level_id = hxs.hierarchy_level_id where  hxs.hierarchy_level_id in (" + this.getEavServices().SQL.conc_ids(hierarchy_ids) + ") group by hierarchy_level_id";
				//			rs = stmt.executeQuery(query);
				//			while (rs.next()) {
				//				spectrum_in_hierarchy_counts.add(rs.getInt(1));
				//				int hierarchy_level_id = (rs.getInt(2));
				//				int eav_hierarchy_link_count = rs.getInt(3);
				//				eav_hierarchy_link_counts.add(eav_hierarchy_link_count);
				//				
				//				if(eav_hierarchy_link_count > 0)
				//				{
				//					int x = 1;
				//				}
				//			}
				//			rs.close();


				// hierarchy contribution of MSD is given by number of spectra under hierarchy normalised by total number of spectra in campaign
				Iterator<Integer> spectrum_in_hierarchy_counts_it = spectrum_in_hierarchy_counts.iterator();
				for(int eav_hierarchy_link_count : eav_hierarchy_link_counts)
				{
					try {
						//					msd_contribution = ((float) (eav_hierarchy_link_count)*spectrum_in_hierarchy_count/campaign.getNumber_of_spectra());
						int spectrum_in_hierarchy_count = spectrum_in_hierarchy_counts_it.next();
						msd_contribution = ((float) (eav_hierarchy_link_count)*spectrum_in_hierarchy_count/campaign.getNumber_of_spectra());

						//						if(msd_contribution > 0)
						//						{
						//							int x = 1;
						//						}
					}
					catch(java.lang.ArithmeticException e)
					{
						msd_contribution = 0.0f;
					}

					//					if(msd_contribution > 0)
					//					{
					//						int x = 0;
					//					}

					msd_contributions.add(msd_contribution);
					msd_contribution_sum = msd_contribution_sum + msd_contribution;
				}

				//			}

				campaign.setMetadata_space_density(spectrum_MSD + msd_contribution_sum);


				// get average location of campaign


				query = "select avg(ST_X(spatial_val)), avg(ST_Y(spatial_val)) from eav where campaign_id = " + campaign.getId() + " and attribute_id in (" + this.getAttributes().get_attribute_id("Spatial Position") + ")";


				rs = stmt.executeQuery(query);
				while (rs.next()) {
					double lon = rs.getDouble(1);
					double lat = rs.getDouble(2);

					Point2D p = new Point2D(lon, lat);
					campaign.setAverage_location(p);
				}
				rs.close();

			}

			// clean up
			stmt.close();
			

		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}

		
	}
	
	
	/**
	 * Get the campaign id for a given spectrum identifier.
	 * 
	 * @param int spectrum_id	the identifier of the spectrum
	 * 
	 * @return id of the campaign
	 */
	public Integer getCampaignIdForSpectrum(int spectrum_id) throws SPECCHIOFactoryException {
		
		try {
			
			Integer campaign_id = 0;
			
			// create SQL-building objects
			Statement stmt = getConnection().createStatement();
			String query;
			ResultSet rs;
			
			// load campaign data from the database
			query = "SELECT campaign_id FROM spectrum WHERE spectrum_id=" + spectrum_id; 
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				campaign_id = rs.getInt(1);
			}
			rs.close();

			
			// clean up
			stmt.close();

			return campaign_id;
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}	
	
	/**
	 * Get the hierarchy id for a given spectrum identifier.
	 * 
	 * @param int hierarchy_id	the identifier of the hierarchy
	 * 
	 * @return id of the campaign
	 */
	public Integer getCampaignIdForHierarchy(int hierarchy_id) throws SPECCHIOFactoryException {
		
		try {
			
			Integer campaign_id = 0;
			
			// create SQL-building objects
			Statement stmt = getConnection().createStatement();
			String query;
			ResultSet rs;
			
			// load campaign data from the database
			query = "SELECT campaign_id FROM hierarchy_level WHERE hierarchy_level_id=" + hierarchy_id; 
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				campaign_id = rs.getInt(1);
			}
			rs.close();

			
			// clean up
			stmt.close();

			return campaign_id;
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}		
	
	
	/**
	 * Get the list of campaigns in the database.
	 * 
	 * @param is_admin				is the user an administrator?
	 * 
	 * @return Array of campaigns, detailed metadata is not set
	 * @throws SPECCHIOFactoryException the list could not be generated
	 */
	public Campaign[] getCampaigns(boolean is_admin) throws SPECCHIOFactoryException {
		
		return getCampaigns(is_admin, false);
	}
	
	
	/**
	 * Get the list of campaigns in the database.
	 * 
	 * @param is_admin				is the user an administrator?
	 * @param load_detailed_meta	loads all metadata if set to true
	 * 
	 * @throws SPECCHIOFactoryException the list could not be generated
	 */
	public Campaign[] getCampaigns(boolean is_admin, boolean load_detailed_meta) throws SPECCHIOFactoryException {
		

		ArrayList<Campaign> campaigns= new ArrayList<Campaign>();

		try {

			// create an SQL statement
			Statement stmt = getConnection().createStatement();

			// get the list of campaign info from the database
			String query = "SELECT campaign_id, name,description,path,user_id,research_group_id FROM " + ((is_admin)? "campaign" : "campaign_view"); 
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				Campaign campaign = new SpecchioCampaign();
				campaign.setId(rs.getInt(1));
				campaign.setName(rs.getString(2));
				campaign.setDescription(rs.getString(3));
				campaign.setPath(rs.getString(4));
				campaign.setUser_id(rs.getInt(5));
				campaign.setResearchGroupId(rs.getInt(6));
				
				setCampaignMetadata(campaign, load_detailed_meta);

				campaigns.add(campaign);
				
			}

		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}



		return campaigns.toArray(new Campaign[0]);


	}
	
	
	/**
	 * Get the identifier of a node in a campaign's hierarchy.
	 * 
	 * @param name			the name of the node to test
	 * @param parent_id		the identifier of the node's parent
	 * 
	 * @returns the node's identifier, or -1 if the node does not exist
	 * 
	 * @throws SPECCHIOFactoryException	the database could not accessed
	 */
	public int getHierarchyNodeId(String name, int parent_id) throws SPECCHIOFactoryException {

		return getHierarchyNodeId(name, parent_id, 0);
	}
	

	/**
	 * Get the identifier of a node in a campaign's hierarchy.
	 * 
	 * @param name			the name of the node to test
	 * @param parent_id		the identifier of the node's parent
	 * @param campaign_id	the identifier of the campaign id to avoid ambiguities with other top hierarchies in other campaigns
	 * 
	 * @returns the node's identifier, or -1 if the node does not exist
	 * 
	 * @throws SPECCHIOFactoryException	the database could not accessed
	 */
	public int getHierarchyNodeId(String name, int parent_id, int campaign_id) throws SPECCHIOFactoryException {
		
		try {
			
			// create an SQL statement
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			
			// build a statement that will find any existing hierarchy nodes
			id_and_op_struct p_id_and_op = new id_and_op_struct(parent_id);
//			String query = "SELECT hierarchy_level_id from hierarchy_level where "
//					+ "campaign_id = " + campaign_id
//					+ " and parent_level_id " + p_id_and_op.op + " " + p_id_and_op.id
//					+ " and name = " + SQL.quote_string(name);
			
			String query = "SELECT hierarchy_level_id from hierarchy_level where "
					+ "parent_level_id " + p_id_and_op.op + " " + p_id_and_op.id
					+ " and name = " + SQL.quote_string(name) + 
					((campaign_id != 0) ? " and campaign_id = " + campaign_id : "");			
			
			// execute the statement
			int id = -1;
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				id = rs.getInt(1);
			}
			
			rs.close();
			stmt.close();
			
			return id;
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
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
	public Hierarchy getHierarchy(int hierarchy_id, boolean userInRole) {

		Hierarchy h = new Hierarchy(hierarchy_id);
		h.setHierarchy_name(this.getHierarchyName(hierarchy_id));
		
		// get metadata of this hierarchy
		MetadataFactory MF = new MetadataFactory(this);	
		h.setMetadata(MF.getMetadataForHierarchy(hierarchy_id));	
		
		return h;
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
	public String getHierarchyFilePath(int hierarchy_id) throws SPECCHIOFactoryException {

		String path = "";
		String folder_path = "";
		String delimiter = "/";
		
		try {		
			
			ArrayList<String> parts = new ArrayList<String>();
			
			Statement stmt = getStatementBuilder().createStatement();
			
			int parent_id = 0;
			int campaign_id;
			do{
				// get parent id
				
				ResultSet rs = stmt.executeQuery("select parent_level_id, name, campaign_id from hierarchy_level where hierarchy_level_id = " + hierarchy_id);
				while (rs.next()) {
					parent_id = rs.getInt(1);	
					parts.add(rs.getString(2));
					campaign_id = rs.getInt(3);	
				}
				rs.close();
				
				hierarchy_id = parent_id;

			} while (parent_id != 0);
			
			
			// try to find the filepath that fits the top directory
			String query = "select path from " + ((Is_admin())? "campaign_path" : "campaign_path_view") + " where path like '%" + parts.get(parts.size()-1) + "'";
			ResultSet rs = stmt.executeQuery(query);
			
			while (rs.next()) {
				folder_path = rs.getString(1);
			}
			rs.close();
			stmt.close();
			
			// build full path
			if (folder_path.indexOf('\\') >= 0)
			{
				delimiter = "\\";
			}
			
			path = folder_path;
			
			for(int i=parts.size()-2; i >=0;i--)
			{
				
				path = path + delimiter + parts.get(i);
			}
			
			
			return path;
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
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
	
	public String getHierarchyName(int hierarchy_id)
			throws SPECCHIOFactoryException {
		String name = "";
		
		try {		
			
			Statement stmt = getStatementBuilder().createStatement();

			ResultSet rs = stmt.executeQuery("select name from hierarchy_level where hierarchy_level_id = " + hierarchy_id);
			while (rs.next()) {
				name = rs.getString(1);
			}
			rs.close();
			stmt.close();
			
			return name;
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}

	}	
	
	
	/**
	 * Get list of ids of campaign's hierarchies.
	 * 
	 * @param campaign_id	the identifier of the campaign
	 * 
	 * @returns list of hierarchy ids
	 * 
	 * @throws SPECCHIOFactoryException	the database could not accessed
	 */
	public ArrayList<Integer> getHierarchyIds(int campaign_id) throws SPECCHIOFactoryException {
		
		try {
			
			ArrayList<Integer> ids = new ArrayList<Integer>();
			
			// create an SQL statement
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			
			// build a statement that will find any existing hierarchy nodes
			String query = "SELECT hierarchy_level_id from hierarchy_level where "
					+ "campaign_id = " + campaign_id;
			
			// execute the statement
			int id = -1;
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				ids.add(rs.getInt(1));
			}
			
			rs.close();
			stmt.close();
			
			return ids;
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}	
	
	
	/**
	 * Import a campaign from an input stream.
	 * 
	 * @param userId	the identifier of the user who will own the campaign
	 * @param is		the input stream
	 *
	 * @throws IOException				the stream could not be read
	 * @throws SPECCHIOFactoryException	database error
	 */
	public void importCampaign(int userId, InputStream is) throws SPECCHIOFactoryException {
		
		try {
			CampaignImport cim = new CampaignImport(getStatementBuilder(), getDatabaseName(), userId);
			cim.read_input_stream(is);
			
			// reload system table caches to pick up any new entries
			this.reloadCaches();
		}
		catch (SQLException ex) {
			// database error
			System.out.print(ex.getMessage());
			throw new SPECCHIOFactoryException(ex);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new SPECCHIOFactoryException(e);
		}
		
	}
	
	
	/**
	 * Get a research group.
	 * 
	 * @param research_group_id	the researh group identifier
	 * 
	 * @return a ResearchGroup object, or null if the group does not exist
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private ResearchGroup getResearchGroup(int research_group_id) throws SPECCHIOFactoryException {
		
		ResearchGroup group = null;
		
		try {
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			String query;
			ResultSet rs;
			
			// get the research group definition
			query = "select name from " + (Is_admin()?"research_group":"research_group_view") +  " where research_group_id=" + Integer.toString(research_group_id);
			rs = stmt.executeQuery(query);
			if (rs.next()) {
				group = new ResearchGroup(research_group_id, rs.getString(1));
			}
			rs.close();
			
			if (group != null) {
				// add the members to the group
				UserFactory uf = new UserFactory(this);
				query = "select member_id from " + (Is_admin()?"research_group_members":"research_group_members_view") +  " where research_group_id=" + Integer.toString(research_group_id);
				rs = stmt.executeQuery(query);
				while (rs.next()) {
					int user_id = rs.getInt(1);
					group.addMember(uf.getUser(user_id));
				}
				rs.close();
				uf.dispose();
			}
			
			// clean up
			stmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return group;
	
	}
	
	
	/**
	 * Insert a new campaign into the database.
	 * 
	 * @param c	the campaign to insert
	 * 
	 * @throws SPECCHIOFactoryException	the campaign could not be inserted
	 */
	public void insertCampaign(Campaign c) throws SPECCHIOFactoryException {
		
		try {
			// create a research group for the campaign
			ResearchGroup rg = createResearchGroup(c.getName());
			
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
			
		
			// insert the campaign into the database
			String query;
			String path;
			
			if (c.getPath() == null)
			{
				query = "INSERT INTO campaign_view(name, path, research_group_id) VALUES (" +
						SQL.quote_string(c.getName()) + "," +
						"?" + "," +
						Integer.toString(rg.getId()) +
						")";
				path = "";
			}
			else
			{
				query = "INSERT INTO campaign_view(name, path, research_group_id) VALUES (" +
					SQL.quote_string(c.getName()) + "," +
					"?" + "," +
					Integer.toString(rg.getId()) +
					")";
				path = c.getPath();
			}
			
			//System.out.println(query);
			PreparedStatement stmt = SQL.prepareStatement(query);
			stmt.setString(1, path);
			stmt.executeUpdate();
		
			// get the campaign id
			ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			while (rs.next())
				c.setId(rs.getInt(1));
			stmt.close();
			
			// update the campaign path table
			if (c.getPath() != null)
			{
				query = "insert into campaign_path_view values(" + Integer.toString(c.getId()) + ",?)";
				PreparedStatement pstmt = SQL.prepareStatement(query);
				for (String p : c.getKnownPaths()) {
					pstmt.setString(1, p);
					pstmt.executeUpdate();
				}
				pstmt.close();
			}
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
	
	}
	
	/**
	 * Insert a node into a campaign's hierarchy.
	 * 
	 * @param name			the name of the new node
	 * @param parent_id		the identifier of the new node's parent
	 * @param campaign_id	the identifier of the campaign (in case the parent id is 0)
	 * 
	 * @returns the identifier of the new node
	 * 
	 * @throws SPECCHIOFactoryException	the node could not be inserted
	 */
	public int insertHierarchyNode(String name, int parent_id, int campaign_id) throws SPECCHIOFactoryException {
		
		try {
			
			// initialise id to a nonsense value
			int id = 0;
						
			
			// create an SQL statement
			Statement stmt = getConnection().createStatement();
			
			
			// insert the node
			id_and_op_struct p_id_and_op = new id_and_op_struct(parent_id);
			String query = "INSERT INTO hierarchy_level_view (parent_level_id, campaign_id, name) "
					+ "VALUES (" + p_id_and_op.id + ", " + campaign_id + ", '" + name + "')";
			
			stmt.executeUpdate(query);
			
			// get id of inserted node
			ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			while (rs.next())
				id = rs.getInt(1);

			rs.close();
			
			return id;
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}	


	/**
	 * Insert a node into a campaign's hierarchy.
	 * 
	 * @param name			the name of the new node
	 * @param parent_id		the identifier of the new node's parent
	 * @param campaign_id	the identifier of the campaign (in case the parent id is 0)
	 * 
	 * @returns the identifier of the new node
	 * 
	 * @throws SPECCHIOFactoryException	the node could not be inserted
	 */
	public int insertHierarchyNode(String name, int parent_id) throws SPECCHIOFactoryException {
		
		try {
			
			// initialise id to a nonsense value
			int id = 0, campaign_id = 0;
			
			
			
			// create an SQL statement
			Statement stmt = getConnection().createStatement();
			
			// get campaign id of parent
			String query = "select campaign_id from hierarchy_level where hierarchy_level_id = " + parent_id;
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next())
				campaign_id = rs.getInt(1);
			
			rs.close();
			
			// insert the node
			return insertHierarchyNode(name, parent_id, campaign_id);
			
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	
	/**
	 * Move a hierarchy to a new parent hierarchy within the same campaign. If a hierarchy of the same name exists in the target hierarchy then the hierarchies are merged.
	 * 
	 * @param source_and_target_ids		Structure containing source and target hierarchy ids
	 * 
	 * return true if move was done
	 */	
	public boolean moveHierarchy(ChildParentIdContainer source_and_target_ids, boolean is_admin) {

		// check if source and target are in same campaign
		if(getCampaignIdForHierarchy(source_and_target_ids.getChild_id()) != getCampaignIdForHierarchy(source_and_target_ids.getParent_id()))
			return false;
		
		
		
		// figure out if we do a re-linking of the source hierarchy content or if the source is linked into the target
		// the criterion is if hierarchy of the same name as the source exists in the target hierarchy
		SpectralBrowserFactory sbf = new SpectralBrowserFactory(this);
		boolean link_content = false;
		int target_sub_hierarchy_id = 0;
		
		List<spectral_node_object> target_children = sbf.getChildNodes(new hierarchy_node(source_and_target_ids.getParent_id(), this.getHierarchyName(source_and_target_ids.getParent_id()), ""));
		
		for(spectral_node_object node : target_children)
		{
			if(node instanceof hierarchy_node && node.getName().equals(getHierarchyName(source_and_target_ids.getChild_id())))
			{
				link_content = true;
				target_sub_hierarchy_id = node.getId();
			}
			
		}
		
		SpectralFileFactory sf_factory = new SpectralFileFactory(this);

		String update_str = "";
		try {
			Statement stmt = getStatementBuilder().createStatement();


			if(link_content)
				// re-link content of source hierarchy
			{
				List<spectral_node_object> source_children = sbf.getChildNodes(new hierarchy_node(source_and_target_ids.getChild_id(), this.getHierarchyName(source_and_target_ids.getChild_id()), ""));

				for(spectral_node_object node : source_children)
				{
					if(node instanceof hierarchy_node)
					{
						update_str = "update hierarchy_level set parent_level_id = " + target_sub_hierarchy_id + " where hierarchy_level_id = " + node.getId();
						stmt.executeUpdate(update_str);
						
						// all data below this hierarchy must also be moved (otherwise the spectra of the sub hierarchies will remain linked to original hierarchy tree)
						// Recursion! Yay!
						ChildParentIdContainer sub_source_and_target_ids = new ChildParentIdContainer(node.getId(), target_sub_hierarchy_id);
						moveHierarchy(sub_source_and_target_ids, is_admin);
						
						
					}
					else
					{
						// remove all links of this spectrum to all upper hierarchies
						String table_name = (is_admin)? "hierarchy_level_x_spectrum" : "hierarchy_level_x_spectrum_view";
						update_str = "delete from " + table_name + " where spectrum_id = " + node.getId();
						stmt.executeUpdate(update_str);
						
						// register in new hierarchy
						sf_factory.insertHierarchySpectrumReferences(target_sub_hierarchy_id, node.getId(), stmt);
						
					
						table_name = (is_admin)? "spectrum" : "spectrum_view";
						update_str = "update " + table_name + " set hierarchy_level_id = " + target_sub_hierarchy_id + " where spectrum_id = " + node.getId();
						stmt.executeUpdate(update_str);

					}

				}

			}
			else
				// link source hierarchy to target hierarchy
			{
				
				//sbf.getDescendentSpectrumIds(new hierarchy_node(source_and_target_ids.getChild_id(), this.getHierarchyName(source_and_target_ids.getChild_id()), ""));
				String table_name = (is_admin)? "hierarchy_level" : "hierarchy_level_view";
				update_str = "update " + table_name + " set parent_level_id = " + source_and_target_ids.getParent_id() + " where hierarchy_level_id = " + source_and_target_ids.getChild_id();
				stmt.executeUpdate(update_str);
				
				// update all spectra and hierarchies under this hierarchy by recursion
				ChildParentIdContainer sub_source_and_target_ids = new ChildParentIdContainer(source_and_target_ids.getChild_id(), source_and_target_ids.getParent_id());
				moveHierarchy(sub_source_and_target_ids, is_admin);				

			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sf_factory.dispose();

		return true;
	}


	
	
	
	
	/**
	 * Remove a campaign from the database.
	 * 
	 * @param id		the identifier of the campaign to be removed
	 * @param is_admin	is the requesting user an administrator?
	 * 
	 * @throws SPECCHIOFactoryException	the campaign could not be removed
	 */
	public void removeCampaign(int campaign_id, boolean is_admin) throws SPECCHIOFactoryException
	{
		
		try {
			
			Statement stmt = getStatementBuilder().createStatement();
			String campaign_cond = "campaign_id = " + Integer.toString(campaign_id);
			
			// remove all hierarchies
			String query = "select hierarchy_level_id from hierarchy_level where " + campaign_cond;
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				int hierarchy_id = rs.getInt(1);		
				removeHierarchyNode(hierarchy_id, is_admin);
			}
			rs.close();
			
			// remove campaign paths
			query = "delete from campaign_path_view where " + campaign_cond;
			stmt.executeUpdate(query);
		
			// get the approprate view name
			String table_name = (is_admin)? "campaign" : "campaign_view";
			
			// save a reference to the research group associated with this campaign
			int research_group_id = 0;
			query = "select research_group_id from " + table_name + " where " + campaign_cond;
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				research_group_id = rs.getInt(1);
			}
		
			// remove campaign itself
			String cmd = "delete from " + table_name + " where " + campaign_cond;
			stmt.executeUpdate(cmd);
			
			// remove the research group
			if (research_group_id != 0) {
				removeResearchGroup(research_group_id);
			}
			
			stmt.close();
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		
		}
		
	}
	
	/**
	 * Remove a campaign from the database.
	 * 
	 * @param ids		the identifiers of the campaigns to be removed
	 * @param is_admin	is the requesting user an administrator?
	 * 
	 * @throws SPECCHIOFactoryException	the campaign could not be removed
	 */
	public void removeCampaigns(ArrayList<Integer> ids, boolean is_admin) throws SPECCHIOFactoryException
	{
		
		try {
			
			Statement stmt = getStatementBuilder().createStatement();
			String campaign_cond = "campaign_id in (" + getStatementBuilder().conc_ids(ids) + ")";
			
			// remove all hierarchies
			String query = "select hierarchy_level_id from hierarchy_level where " + campaign_cond;
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				int hierarchy_id = rs.getInt(1);		
				removeHierarchyNode(hierarchy_id, is_admin);
			}
			rs.close();
			
			// remove campaign paths
			query = "delete from campaign_path_view where " + campaign_cond;
			stmt.executeUpdate(query);
		
			// get the appropriate view name
			String table_name = (is_admin)? "campaign" : "campaign_view";
			
			// save a reference to the research group associated with this campaign
			int research_group_id = 0;
			query = "select research_group_id from " + table_name + " where " + campaign_cond;
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				research_group_id = rs.getInt(1);
			}
			
			
			// ensure that no zombie eav's hang around (this can happen if e.g. a data import fails)
			ArrayList<Integer> eav_ids_to_delete = new ArrayList<Integer>();
			query = " select eav_id from eav where " + campaign_cond; 
			
			rs = stmt.executeQuery(query);
			while (rs.next()) {				
				eav_ids_to_delete.add(rs.getInt(1));	
			}			
			rs.close();	
			
			if(eav_ids_to_delete.size()>0)
			{
				System.err.println("Deleting " + eav_ids_to_delete.size() + " zombie eavs ... Please investigate in case this campaign was supposed to complete.");
				String cmd = "delete from "+ ((is_admin)? "eav" : "eav_view") +" where " +
						"eav_id in (" + getStatementBuilder().conc_ids(eav_ids_to_delete) + ")";	
				stmt.executeUpdate(cmd); 		
			}

		
			// remove campaign itself
			String cmd = "delete from " + table_name + " where " + campaign_cond;
			stmt.executeUpdate(cmd);
			
			// remove the research group
			if (research_group_id != 0) {
				removeResearchGroup(research_group_id);
			}
			
			stmt.close();
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		
		}
		
	}	
	
	
	/**
	 * Remove a whole sub-hierarchy from the database.
	 * 
	 * @param hierarchy_id	the identifier of the node at the root of the sub-hierarchy
	 * @param is_admin		is the requesting user an administrator?
	 * 
	 * @throws SPECCHIOFactoryException	the sub-hierarchy could not be remove
	 */
	public void removeHierarchyNode(int hierarchy_id, boolean is_admin) throws SPECCHIOFactoryException {
//		The process of removing a hierarchy node consists of five steps:
//		1. Remove the spectra directly linked to this hierarchy.
//		2. Find sub-hierarchies and recursively delete them (e.g. including the steps 1 --> 3).
//		3. Delete the eav data related to the current hierarchy.
//		4. Find out if there are eav_ids no longer shared by other spectra (the expensive part).
//		5. Remove the hierarchy itself.
		try {
			
			Statement stmt = getStatementBuilder().createStatement();
			String table_name;
			String cmd;
			
			ArrayList<Integer> spectrum_ids = new ArrayList<Integer>();
			
			// remove all spectrum nodes under this hierarchy
			SpectrumFactory sf = new SpectrumFactory(this);
			String query = "select spectrum_id from spectrum where hierarchy_level_id = " + Integer.toString(hierarchy_id);
			ResultSet rs = stmt.executeQuery(query);	
			while (rs.next()) {	
				spectrum_ids.add(rs.getInt(1));
			}
			rs.close();
			
			sf.removeSpectra(spectrum_ids, is_admin);
			
			// remove all sub-hierarchies
			query = "select hierarchy_level_id from hierarchy_level where parent_level_id = " + Integer.toString(hierarchy_id);
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				int hierarchy_level_id = rs.getInt(1);		
				removeHierarchyNode(hierarchy_level_id, is_admin);
			}	
			rs.close();		
			
			// remove eav data stored at hierarchy level
			// EAV
			// remove entries from eav x table
			
			ArrayList<Integer> eav_ids = new ArrayList<Integer>();
			query = "select eav_id from hierarchy_x_eav where hierarchy_level_id = " + hierarchy_id + ""; 
			
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				eav_ids.add(rs.getInt(1));	
			}			
			rs.close();	
			

			
			table_name = (is_admin)? "hierarchy_x_eav" : "hierarchy_x_eav_view";
			cmd = "delete from "+table_name+" where " +
					"hierarchy_level_id = " + hierarchy_id + "";	
			stmt.executeUpdate(cmd); 	
			
			
			// get eav_ids that are no longer referenced by other spectra
			ArrayList<Integer> eav_ids_to_delete = new ArrayList<Integer>();
			query = "    select eav.eav_id, count(hxe.hierarchy_level_id) from eav eav LEFT JOIN hierarchy_x_eav hxe" + 
					"     ON hxe.eav_id = eav.eav_id where eav.eav_id in (" + getStatementBuilder().conc_ids(eav_ids) + ")  group by eav_id;"; 
			
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				
				int cnt = rs.getInt(2);
				if(cnt == 0) eav_ids_to_delete.add(rs.getInt(1));	
			}			
			rs.close();	
			
			
			cmd = "delete from "+table_name+" where " +
					"eav_id in (" + getStatementBuilder().conc_ids(eav_ids_to_delete) + ")";	
			stmt.executeUpdate(cmd); 						
										
			
			table_name = (is_admin)? "hierarchy_level" : "hierarchy_level_view";
	
			// remove hierarchy itself
			cmd = "delete from "+table_name+" where hierarchy_level_id = " + Integer.toString(hierarchy_id);
			stmt.executeUpdate(cmd); 
			
			stmt.close();
			
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	/**
	 * Remove a whole sub-hierarchies from the database.
	 * 
	 * @param ids			the identifiers of the nodes at the root of the sub-hierarchies
	 * @param is_admin		is the requesting user an administrator?
	 * 
	 * @throws SPECCHIOFactoryException	the sub-hierarchy could not be remove
	 */
	public void removeHierarchyNodes(ArrayList<Integer> ids, boolean is_admin) throws SPECCHIOFactoryException {
		
		try {
			
			Statement stmt = getStatementBuilder().createStatement();
			stmt = getStatementBuilder().createStatement();
			stmt.getConnection().setAutoCommit(false);
			
			ArrayList<Integer> spectrum_ids = new ArrayList<Integer>();
			
			// remove all spectrum nodes under this hierarchy
			SpectrumFactory sf = new SpectrumFactory(this);
			String query = "select spectrum_id from spectrum where hierarchy_level_id in (" + getStatementBuilder().conc_ids(ids) + ")";
			ResultSet rs = stmt.executeQuery(query);	
			while (rs.next()) {	
				spectrum_ids.add(rs.getInt(1));
			}
			rs.close();
			
			sf.removeSpectra(spectrum_ids, is_admin);
			
			// remove all sub-hierarchies
			query = "select hierarchy_level_id from hierarchy_level where parent_level_id in (" + getStatementBuilder().conc_ids(ids) + ")";
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				int hierarchy_level_id = rs.getInt(1);		
				removeHierarchyNode(hierarchy_level_id, is_admin);
			}	
			rs.close();		
			
	
			// remove hierarchy itself
			for(int h_id : ids)
			{
				removeHierarchyNode(h_id, is_admin);
			}
			
			stmt.getConnection().commit();
			//stmt.getConnection().rollback();
			stmt.getConnection().setAutoCommit(true);		
			
			stmt.close();
			
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	/**
	 * Remove a research group.
	 * 
	 * @param research_group_id	the research group identifier
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public void removeResearchGroup(int research_group_id) throws SPECCHIOFactoryException {
		
		try {
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			String research_group_cond = "research_group_id=" + Integer.toString(research_group_id);
			String query;
			
			// need to disable foreign key checks because research_group_view and research_group_members_view refer to each other
			query = "set @@foreign_key_checks = 0";
			stmt.executeQuery(query);
			
			// delete entry from the research group table
			query = "delete from " + (Is_admin()?"research_group":"research_group_view") +  " where " + research_group_cond;
			stmt.executeUpdate(query);
			
			// restore foreign key checks
			query = "set @@foreign_key_checks = 1";
			stmt.executeQuery(query);
			
			// delete members from the research group member table
			query = "delete from " + (Is_admin()?"research_group_members":"research_group_members_view") +  " where " + research_group_cond;
			stmt.executeUpdate(query);
			
			// clean up
			stmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	
	/**
	 * Rename a hierarchy in the database and also on the file system if path is accessible.
	 * 
	 * @param hierarchy_id	id of the hierarchy to be renamed
	 * @param name	new name of the hierarchy
	 */	
	public void renameHierarchy(int hierarchy_id, String name) throws SPECCHIOFactoryException
	{		

		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			String query;
			
			// update the hierarchy_level table
			ArrayList<String> attr_and_vals = new ArrayList<String>(6);
			attr_and_vals.add("name"); attr_and_vals.add(name);
			
			query = SQL.assemble_sql_update_query(
					SQL.conc_attr_and_vals("hierarchy_level_view", attr_and_vals.toArray(new String[attr_and_vals.size()])),
					"hierarchy_level_view",
					"hierarchy_level_id=" + hierarchy_id);
			stmt.executeUpdate(query);			
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}			
		
	}
		
	
	/**
	 * Update campaign information.
	 * 
	 * @param campaign	the new campaign information
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public void updateCampaign(Campaign campaign) throws SPECCHIOFactoryException {
		
		String query = "";
		try {
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			PreparedStatement pstmt;
			
			
			// set up attributes and values to set
			ArrayList<String> attr_and_vals = new ArrayList<String>(6);
			attr_and_vals.add("name"); attr_and_vals.add(campaign.getName());
			attr_and_vals.add("description"); attr_and_vals.add(campaign.getDescription());
			attr_and_vals.add("path"); attr_and_vals.add(campaign.getPath());
			
			// update the campaign table
			query = SQL.assemble_sql_update_query(
					SQL.conc_attr_and_vals("campaign_view", attr_and_vals.toArray(new String[attr_and_vals.size()])),
					"campaign_view",
					"campaign_id=" + Integer.toString(campaign.getId())
				);
			stmt.executeUpdate(query);
			
			// update the campaign path table
			query = "delete from campaign_path_view where campaign_id=" + Integer.toString(campaign.getId());
			stmt.executeUpdate(query);
			query = "insert into campaign_path_view values(" + Integer.toString(campaign.getId()) + ",?)";
			pstmt = SQL.prepareStatement(query);
			for (String path : campaign.getKnownPaths()) {
				pstmt.setString(1, path);
				pstmt.executeUpdate();
			}
			
			// clean up
			stmt.close();
			pstmt.close();
			
			ResearchGroup rg = campaign.getResearchGroup();
			if (rg != null) {
				if (rg.getId() == 0) {
					// the campaign was created without a research group; we need to create a new research group
					ResearchGroup rgNew = createResearchGroup(campaign.getName());
					rg.setId(rgNew.getId());
					rg.setName(rgNew.getName());
					
					// insert the research group identifier into the campaign table
					stmt = SQL.createStatement();
					query = "update campaign_view" +
							" set research_group_id=" + Integer.toString(rgNew.getId()) +
							" where campaign_id=" + Integer.toString(campaign.getId());
					stmt.executeUpdate(query);
					stmt.close();
				}
				
				// update research group tables
				updateResearchGroup(campaign.getResearchGroup());
			}
		}
		catch (SQLException ex) {
			// database error
			SPECCHIOFactoryException ex2 = new SPECCHIOFactoryException(ex);
			ex2.setMessage(ex2.getMessage() + ": \n" + query);
			throw new SPECCHIOFactoryException(ex2);
		}
		
	}
	
	
	/**
	 * Update research group membership
	 * 
	 * @param group	the research group
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private void updateResearchGroup(ResearchGroup group) throws SPECCHIOFactoryException {
		
		try {
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
			String query;
			
			// delete the existing research group members
			query = "delete from " + (Is_admin()?"research_group_members":"research_group_members_view") +  " where research_group_id=" + Integer.toString(group.getId());
			Statement stmt = SQL.createStatement();
			stmt.executeUpdate(query);
			stmt.close();
			
			// add the new research group members
			query = "insert into " + (Is_admin()?"research_group_members":"research_group_members_view") +  " (research_group_id, member_id) values(?, ?)";
			PreparedStatement pstmt = SQL.prepareStatement(query);
			for (User member : group.getMembers()) {
				pstmt.setInt(1, group.getId());
				pstmt.setInt(2, member.getUserId());
				pstmt.executeUpdate();
			}
			pstmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
	}





			
			
		
}
