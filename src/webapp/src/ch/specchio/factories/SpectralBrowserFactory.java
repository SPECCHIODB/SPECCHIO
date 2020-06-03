package ch.specchio.factories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetadataSelectionDescriptor;
import ch.specchio.types.campaign_node;
import ch.specchio.types.database_node;
import ch.specchio.types.hierarchy_node;
import ch.specchio.types.spectral_node_object;
import ch.specchio.types.spectrum_node;

public class SpectralBrowserFactory extends SPECCHIOFactory {
	
	/**
	 * Constructor.
	 * 
	 * @param db_user		database account user name
	 * @param db_password	database account password
	 * @param is_admin	is the user an administrator? 
	 * 
	 * @throws SPECCHIOFactoryException	could not establish initial context
	 */
	public SpectralBrowserFactory(String db_user, String db_password, String ds_name, boolean is_admin) throws SPECCHIOFactoryException {

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
	public SpectralBrowserFactory(SPECCHIOFactory factory) throws SPECCHIOFactoryException {
		
		super(factory);
		
	}	
	
	
	/**
	 * Create an internal child node (i.e. not a spectrum) object.
	 * 
	 * @param node			the node to which the child will belong
	 * @param child_id		the child's identifier
	 * @param child_name	the child's name
	 * 
	 * @return a new spectral node object corresponding to the input data
	 */
	private spectral_node_object createChildInternalNode(spectral_node_object node, int child_id, String child_name) {
		
		if (node instanceof campaign_node) {
			return new hierarchy_node(child_id, child_name, node.getOrderBy());
		} else if (node instanceof database_node) {
			return new campaign_node(child_id, child_name, node.getRestrictToView(), node.getOrderBy());
		} else if (node instanceof hierarchy_node) {
			return new hierarchy_node(child_id, child_name, node.getOrderBy());
		} else {
			// spectrum nodes cannot have children
			return null;
		}
		
	}
	
	
	/**
	 * Create a leaf node (i.e. a spectrum).
	 * 
	 * @param node			the hierarchy node to which the child will belong
	 * @param child_id		the child's identifier
	 * @param child_name	the child's name
	 * 
	 * @return a new spectral node object corresponding to the input data
	 */
	private spectral_node_object createChildSpectrumNode(hierarchy_node node, int child_id, String child_name) {
		
		return new spectrum_node(child_id, child_name);
		
	}
	
	
	/**
	 * Get a campaign node.
	 * 
	 * @param campaign_id		the campaign identifier
	 * @param restrict_to_view	list this user's data only?
	 * @param order_by			the attribute by which to order the campaign's descendents
	 * 
	 * @return a new campaign node object corresponding to the input parameters
	 * 
	 * @throws SPECCHIOFactoryException	could not find a campaign with this identifier
	 */
	public campaign_node getCampaignNode(int campaign_id, boolean restrict_to_view, String order_by) throws SPECCHIOFactoryException {
		
		try {
			
			// get the name of the node
			String name = null;
			Statement stmt = getStatementBuilder().createStatement();
			ResultSet rs = stmt.executeQuery("select name from campaign where campaign_id = " + campaign_id);
			while (rs.next()) {
				name = rs.getString(1);
			}
			rs.close();
			stmt.close();
			
			// throw an exception if the campaign was not found
			if (name == null) {
				throw new SPECCHIOFactoryException("Campaign id " + campaign_id + " does not exist.");
			}
			
			// construct the node
			return new campaign_node(campaign_id, name, restrict_to_view, order_by);
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	
	/**
	 * Get the parent_id for a given hierarchy_id
	 * 
	 * @param hierarchy_id	the hierarchy_id identifying the required node
	 * 
	 * @return id of the parent of given hierarchy
	 * 
	 * @throws SPECCHIOFactoryException	could not find a campaign with this identifier
	 */
	public int getHierarchyParentId(int hierarchy_id) throws SPECCHIOFactoryException {
		
		try {		
			int parent_id = 0;
			// get parent id
			Statement stmt = getStatementBuilder().createStatement();
			ResultSet rs = stmt.executeQuery("select parent_level_id from hierarchy_level where hierarchy_level_id = " + hierarchy_id);
			while (rs.next()) {
				parent_id = rs.getInt(1);	
			}
			rs.close();
			stmt.close();
			
			// throw an exception if the campaign was not found
			if (parent_id == 0) {
				throw new SPECCHIOFactoryException("Parent hierarchy id of hierarchy " + hierarchy_id + " does not exist.");
			}
			
			return parent_id;
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
		
	
	
	/**
	 * Get the name of the view that contains the visible children of a campaign node.
	 * 
	 * @param node	the node
	 * 
	 * @return "hierarchy_level" or "hierarchy_level_view"
	 */
	private String getChildViewName(campaign_node node) {
		
		return node.getRestrictToView() ? "hierarchy_level_view" : "hierarchy_level";
		
	}
	
	
	/**
	 * Get the name of the view that contains the visible children of a database node.
	 *
	 * @param node	the node
	 * 
	 * @return "campaign" or "campaign_view"
	 */
	private String getChildViewName(database_node node) {
		
		return node.getRestrictToView() ? "campaign_view" : "campaign";
		
	}
	
	
	
	/**
	 * Get the children of a given node.
	 * 
	 * @param node	the parent
	 * 
	 * @return an array containing a spectral node object for each child of the given node
	 * 
	 * @throws SPECCHIOFactoryException invalid node
	 */
	public List<spectral_node_object> getChildNodes(spectral_node_object node) throws SPECCHIOFactoryException {
		
		List<spectral_node_object> children = new LinkedList<spectral_node_object>();
		
		if (node instanceof spectrum_node) {
		
			// spectrum nodes do not have any descendents, so leave list empty
			
		} else {
			
			try {
				
				// build a query that will return the node's internal children
				String query = null;
				if (node instanceof campaign_node) {
					query = "select hierarchy_level_id, name from " + getChildViewName((campaign_node)node) +
							" where campaign_id = " + node.getId() + " and parent_level_id is null" + " order by name";
				} else if (node instanceof database_node) {
					query = "select campaign_id, name from " + getChildViewName((database_node)node);
				} else if (node instanceof hierarchy_node) {
					query = "select hierarchy_level_id, name from hierarchy_level where parent_level_id = " + node.getId() + " order by name";
				}
				
				Statement stmt = getStatementBuilder().createStatement();
				
				// get a list of nodes that match the query
				if (query != null) {
					ResultSet rs = stmt.executeQuery(query);
					while (rs.next()) {
						Integer child_id = rs.getInt(1);
						String child_name = rs.getString(2);
						children.add(createChildInternalNode(node, child_id, child_name));
					}	
					rs.close();
				}
				
				// add spectrum nodes to hierarchy nodes
				if (node instanceof hierarchy_node) {
					int order_by_attribute_id = getAttributes().get_attribute_id(node.getOrderBy());
					String order_by_storage_field = getAttributes().get_default_storage_field(order_by_attribute_id);
//					query = "select t1.spectrum_id, t1.string_val from " +
//						"(" +
//							"select spectrum.spectrum_id, eav.string_val " +
//							"from spectrum, spectrum_x_eav, eav " +
//							"where spectrum.hierarchy_level_id = " + node.getId() + " " +
//								"and spectrum_x_eav.spectrum_id = spectrum.spectrum_id " +
//								"and spectrum_x_eav.eav_id = eav.eav_id " +
//								"and eav.attribute_id = " + getAttributes().get_attribute_id("File Name") +
//						") t1 " +
//						"left join " +
//						"(" +
//							"select spectrum_x_eav.spectrum_id, eav.eav_id, eav." + order_by_storage_field + " " +
//							"from spectrum_x_eav, eav " +
//							"where spectrum_x_eav.eav_id = eav.eav_id " +
//								"and eav.attribute_id = " + order_by_attribute_id +
//						") t2 " +
//						"on t2.spectrum_id = t1.spectrum_id " +
//						"order by t2." + order_by_storage_field;
					
					query = "select spectrum.spectrum_id " +
								"from spectrum spectrum, spectrum_x_eav, eav eav " +
								"where spectrum.hierarchy_level_id = " + node.getId() + " " +
									"and spectrum_x_eav.spectrum_id = spectrum.spectrum_id " +
									"and spectrum_x_eav.eav_id = eav.eav_id " +
									"and eav.attribute_id = " + order_by_attribute_id + " order by " + "eav." + order_by_storage_field;
					
//					+
//							") t1 " +
//							"left join " +
//							"(" +
//								"select spectrum_x_eav.spectrum_id, eav.eav_id, eav." + order_by_storage_field + " " +
//								"from spectrum_x_eav, eav " +
//								"where spectrum_x_eav.eav_id = eav.eav_id " +
//									"and eav.attribute_id = " + order_by_attribute_id +
//							") t2 " +
//							"on t2.spectrum_id = t1.spectrum_id " +
//							"order by t2." + order_by_storage_field;					
//					
					ArrayList<Integer> ids = new ArrayList<Integer>();
					
					
					ResultSet rs = stmt.executeQuery(query);
					while (rs.next()) {
						ids.add(rs.getInt(1));
//						Integer child_id = rs.getInt(1);
//						String child_name = rs.getString(2);
//						children.add(createChildSpectrumNode((hierarchy_node)node, child_id, child_name));
					}	
					rs.close();
					
					// complement with spectra that do not have the current attribute and therefore cannot be ordered by it
					query = "select spectrum.spectrum_id " +
							"from spectrum spectrum where spectrum.hierarchy_level_id = " + node.getId() + 
							((ids.size() > 0 )  ?  
									(" and spectrum_id not in (" + getStatementBuilder().conc_ids(ids) + ")") : "")
							;
					
					rs = stmt.executeQuery(query);
					while (rs.next()) {
						ids.add(rs.getInt(1));
					}
					rs.close();
					
					
					if(ids.size() > 0)
					{
						// select file name attribute for display
						MetadataFactory factory = new MetadataFactory(this);
						
						MetadataSelectionDescriptor ms_d = new MetadataSelectionDescriptor();
						ms_d.setIds(ids);
						ms_d.setAttribute_id(getAttributes().get_attribute_id("File Name"));
						
						List<MetaParameter> mp_list = factory.getMetaParameters(MetaParameter.SPECTRUM_LEVEL, ms_d.getIds(), ms_d.getAttribute_id(), false);
						factory.dispose();
						
						for(int i=0;i<ids.size();i++)
						{ 
							children.add(createChildSpectrumNode((hierarchy_node)node, ids.get(i), (String) mp_list.get(i).getValue()));
									
						}
					}
					
				}
				
				stmt.close();
				
				
			} catch (SQLException ex) {
				// bad SQL
				throw new SPECCHIOFactoryException(ex);
			}
		}
		
		return children;
		
	}
	
	
	/**
	 * Get a database node.
	 * 
	 * @param order_by			the criterion to order children by
	 * @param restrict_to_view	show this user's data only?
	 * 
	 * @returns a campaign node
	 * 
	 * @throws SPECCHIOFactoryException	could not create the node
	 */
	public database_node getDatabaseNode(String order_by, boolean restrict_to_view) throws SPECCHIOFactoryException {
		
		try {
			
			// get the name of the database
			String name = null;
			Statement stmt = getStatementBuilder().createStatement();
			ResultSet rs = stmt.executeQuery("select schema()");
			while (rs.next()) {
				name = rs.getString(1);
			}
			rs.close();
			stmt.close();
		
			// construct the node
			return new database_node(0, name, restrict_to_view, order_by);
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	
	/**
	 * Get the identifiers of all the spectra that descend from a given node 
	 * 
	 * @param node	the node
	 * 
	 * @returns an array containing the identifier of every spectrum that descends from the given node
	 * 
	 * @throws SPECCHIOFactoryException invalid node
	 */
	public List<Integer> getDescendentSpectrumIds(spectral_node_object node) throws SPECCHIOFactoryException {
		
		List<Integer> ids = new LinkedList<Integer>();
		
		if (node instanceof database_node) {
			
			// not implemented; leave list empty
			
		} else if (node instanceof spectrum_node) {
			
			// spectrum nodes do not have any descendents, so just return the node's own id
			ids.add(node.getId());
			
		} else {
			
			// search the database for descendents of a campaign or hierarchy node
			try {
				
				// work out the query string
				//SQL_StatementBuilder sql = getStatementBuilder();
				String query = null;
				if (node instanceof campaign_node) {
					query = "select spectrum_id from spectrum where " +
							"campaign_id = " + node.getId();
				} else if (node instanceof hierarchy_node) {
					query = "select hxs.spectrum_id from hierarchy_level_x_spectrum hxs where " +
							"hxs.hierarchy_level_id = " + node.getId();
				}
				
				// build a list of ids from the database
				if (query != null) {
					Statement stmt = getStatementBuilder().createStatement();
					ResultSet rs = stmt.executeQuery(query);
					while (rs.next()) {
						ids.add(new Integer(rs.getInt(1)));			
					}
					rs.close();
					stmt.close();
				}
				
			}
			catch (SQLException ex) {
				// bad SQL
				throw new SPECCHIOFactoryException(ex);
			}
			
		}
		
		return ids;
		
	}

}
