package ch.specchio.factories;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

import ch.specchio.types.*;
import org.ujmp.core.Matrix;
import org.ujmp.core.matrix.DenseMatrix;

// New imports here!
import org.ujmp.core.util.SerializationUtil;

import ch.specchio.eav_db.SQL_StatementBuilder;

public class UncertaintyFactory extends SPECCHIOFactory {
	
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
	public UncertaintyFactory(String db_user, String db_password, String ds_name, boolean is_admin) throws SPECCHIOFactoryException {
		
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
	public UncertaintyFactory(SPECCHIOFactory factory) throws SPECCHIOFactoryException {
		
		super(factory);
		
	}
	
	/** 
	*
	* Insert and create new uncertainty set. This creates a new uncertainty set id, a blank adjacency matrix
	* 
	* @param uc_set the uncertainty set to be created
	* 
	* @throws SPECCHIOFactoryException
	* 
 	*/
	
	public void insertNewUncertaintySet(UncertaintySet uc_set) {
		
		try {
			
			SQL_StatementBuilder SQL = getStatementBuilder();
			
			// Finding max of uncertainty_node_ids in uncertainty_node table
			
			String max_query = "SELECT MAX(node_set_id) from uncertainty_node_set";
			
			PreparedStatement max_pstmt = SQL.prepareStatement(max_query);
			
			ResultSet max_rs = max_pstmt.executeQuery();
			
			int last_node_set_id = 0;
			
			 while (max_rs.next()) {
			        last_node_set_id = max_rs.getInt(1);
			              
			 }
			 
			int new_node_set_id = last_node_set_id + 1;
		        
		    uc_set.node_set_id = new_node_set_id;
			 
			max_pstmt.close();
			
			// Now we have the new node set id we need to insert this (and the first node_num) into uncertainty_node_set 
			// That way we fulfil the foreign key requirement in uncertainty_set
			
			int node_num = 1;
			
			String insert_uc_node_set_query = "INSERT into uncertainty_node_set(node_set_id, node_num) " + 
					"values (?,?)";
			
			PreparedStatement insert_uc_node_set_pstmt = SQL.prepareStatement(insert_uc_node_set_query);
			
			insert_uc_node_set_pstmt.setInt (1, uc_set.node_set_id);
			insert_uc_node_set_pstmt.setInt(2, node_num);
			
		    insert_uc_node_set_pstmt.executeUpdate();
		    
		    insert_uc_node_set_pstmt.close();
			
			// Insert into uncertainty_set should now work...
		    
			String uc_set_query = "insert into uncertainty_set(uncertainty_set_description, node_set_id) " +
					" values (?, ?)";
		
			PreparedStatement uc_set_pstmt = SQL.prepareStatement(uc_set_query, Statement.RETURN_GENERATED_KEYS);
					
			uc_set_pstmt.setString (1, uc_set.getUncertaintySetDescription());
			uc_set_pstmt.setInt(2, uc_set.getNodeSetId());
			
			int affectedRows = uc_set_pstmt.executeUpdate();
			
			ResultSet generatedKeys = uc_set_pstmt.getGeneratedKeys();
			
			
			while (generatedKeys.next()) {

				int uc_set_id = generatedKeys.getInt(1);
				
				System.out.println("inserted id: " + uc_set_id);
				
				uc_set.setUncertaintySetId(uc_set_id);
				
				
			}
			
			uc_set_pstmt.close();
			
			
			}
			catch (SQLException ex) {
				// bad SQL
				throw new SPECCHIOFactoryException(ex);
			}

	}
	
	
	
	/**
	 * Get the adjacency matrix
	 * 
	 * @param uncertainty_set_id 
	 * 
	 * @return an array of integers of the adjacency matrix
	 * 
	 * @throws SPECCHIOFactoryException
	 */
	public AdjacencyMatrix getAdjacencyMatrix(int uncertainty_set_id) throws SPECCHIOFactoryException {
		
		AdjacencyMatrix selectedAdjacencyMatrix = new AdjacencyMatrix();
		
		// Getting matrix dimension
		
		int matrix_dimensions = getAdjacencyMatrixDimensions(uncertainty_set_id);
		
		// Extracting data from database for adjacency matrix
		
		try {
			
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
			String sql_stmt = "SELECT adjacency_matrix, node_set_id, uncertainty_set_description from uncertainty_set where uncertainty_set_id = ?";
			PreparedStatement pstmt = SQL.prepareStatement(sql_stmt);
			pstmt.setInt(1, uncertainty_set_id);
			ResultSet rs = pstmt.executeQuery();
		
			while (rs.next()) {
		        	        
		        Blob adjacency_matrix_blob = rs.getBlob("adjacency_matrix");
		      
				InputStream binstream = adjacency_matrix_blob.getBinaryStream();
				DataInput dis = new DataInputStream(binstream);
				

				try {
					int dim = binstream.available() / 4;

					Integer[] adj_matrix_array = new Integer[dim];
					
					System.out.println("dim of binstream in getAdjacencyMatrix:" + dim);

					for(int i = 0; i < dim; i++)
					{
						try {
							Integer f = dis.readInt();
							adj_matrix_array[i] = f.intValue();
						} catch (IOException e) {
							
							e.printStackTrace();
						}				
					}		
					
					AdjacencyMatrix anotherAdjacencyMatrix = new AdjacencyMatrix(uncertainty_set_id, adj_matrix_array, matrix_dimensions);
					
					// Adding this matrix to a list of matrices


					selectedAdjacencyMatrix = anotherAdjacencyMatrix;

				} catch (IOException e) {
					
					e.printStackTrace();
				}


				try {
					binstream.close();
				} catch (IOException e) {
					
					e.printStackTrace();
				}
				
			}
			
			
		} catch (SQLException ex) {

				throw new SPECCHIOFactoryException(ex);
			}
		
		return selectedAdjacencyMatrix;
		
	}
	
	/**
	 * Get adjacency matrix dimension from uncertainty set id
	 * 
	 * @param uncertainty_set_id the uncertainty_set_id of interest
	 * 
	 * @throws SPECCHIOFactoryException
	 * 
	 */
	
	public int getAdjacencyMatrixDimensions(int uncertainty_set_id) throws SPECCHIOFactoryException {
	
		int matrixDimension = 0;
		int node_set_id = 0;
		int max_node_num = 0;

		try { 
		
			SQL_StatementBuilder SQL = getStatementBuilder();
			
			// First selecting node_set_id which corresponds to the uncertainty_set_id
			String node_set_id_sql = "SELECT node_set_id from uncertainty_set where uncertainty_set_id = ?";
			PreparedStatement pstmt_node_set_id = SQL.prepareStatement(node_set_id_sql);
			pstmt_node_set_id.setInt(1, uncertainty_set_id);
			ResultSet uc_set_rs = pstmt_node_set_id.executeQuery();
		
			while (uc_set_rs.next()) {
		        	node_set_id = uc_set_rs.getInt(1);
			}
		 
			pstmt_node_set_id.close();
		
			// Then getting max of node_num which is the same as the adjacency matrix dimensions
			
			String find_max_node_num_sql = "SELECT max(node_num) from uncertainty_node_set where node_set_id = ?";
			PreparedStatement pstmt_find_max_node_num = SQL.prepareStatement(find_max_node_num_sql);
			pstmt_find_max_node_num.setInt(1, node_set_id);
			ResultSet find_max_node_num_rs = pstmt_find_max_node_num.executeQuery();
			 
			 while (find_max_node_num_rs.next()) {
				 max_node_num = find_max_node_num_rs.getInt(1);
			 }
			 
			 pstmt_find_max_node_num.close();

			 matrixDimension = max_node_num;
			
			 return matrixDimension;
			
		}
		
		catch (SQLException ex) {

			throw new SPECCHIOFactoryException(ex);
		}
		
		
	
	}
	
	/**
	 * 
	 * Get the edge value for a given edge id
	 * 
	 * @param edge_id
	 * 
	 * @throws SPECCHIOFactoryException database error
	 * 
	 */
	public String getEdgeValue(int edge_id) throws SPECCHIOFactoryException {

		String selectedEdgeValue = new String();
		
		int edge_id_sql = edge_id;
		
		try {
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt = SQL.createStatement();
			
			String sql_query = "SELECT edge_value from uncertainty_edge where edge_id =" + edge_id_sql;
	
			ResultSet rs = stmt.executeQuery(sql_query);
			
			while (rs.next()) {
				
				selectedEdgeValue = rs.getString("edge_value");
				
			}
			rs.close();
			stmt.close();
			
			if (selectedEdgeValue == null) {
				selectedEdgeValue = "null";
			}
			
		}
		
		catch (SQLException ex) {

			throw new SPECCHIOFactoryException(ex);
		}
		
		return selectedEdgeValue;
	}
	
	
	/**
	 * 
	 * Get all associated values for a given instrument node id including the uncertainty vector
	 * 
	 * @param instrument_node_id the instrument_node_id of interest
	 * 
	 * @throws  SPECCHIOFactoryException database error
	 * 
	 */
	
	public UncertaintyNode getInstrumentNode(int instrument_node_id) throws SPECCHIOFactoryException {

		UncertaintyInstrumentNode selectedInstrumentNode = new UncertaintyInstrumentNode();
		
		try {
		
		// create SQL-building objects
		SQL_StatementBuilder SQL = getStatementBuilder();
		
		String sql_stmt = "SELECT node_description, u_vector, confidence_level, abs_rel, unit_id from instrument_node where instrument_node_id = ?";

		PreparedStatement pstmt = SQL.prepareStatement(sql_stmt);

		pstmt.setInt(1, instrument_node_id);
		
		ResultSet rs = pstmt.executeQuery();

		//Getting information from ResultSet
		
		 while (rs.next()) {
		        String node_description = rs.getString("node_description");
		        double confidence_level = rs.getDouble("confidence_level");
		        String abs_rel = rs.getString("abs_rel");
		        int unit_id = rs.getInt("unit_id");
		        	        
		        Blob u_vector_blob = rs.getBlob("u_vector");
		        
		        System.out.println(node_description + ", " + confidence_level + ", " + abs_rel +
		                           ", " + unit_id +  ", " + u_vector_blob);
		        
		        selectedInstrumentNode.setAbsRel(abs_rel);
		        selectedInstrumentNode.setConfidenceLevel(confidence_level);
		        selectedInstrumentNode.setNodeType("instrument");
		        selectedInstrumentNode.setUnitId(unit_id);
		        selectedInstrumentNode.setUncertaintyNodeId(instrument_node_id);
			 	selectedInstrumentNode.setNodeDescription(node_description);
		        
				InputStream binstream = u_vector_blob.getBinaryStream();
				DataInput dis = new DataInputStream(binstream);

				try {
					int dim = binstream.available() / 4;

					float[] u_vector = new float[dim];

					for(int i = 0; i < dim; i++)
					{
						try {
							Float f = dis.readFloat();
							u_vector[i] = f.floatValue();
						} catch (IOException e) {
							
							e.printStackTrace();
						}				
					}		
					
					
					selectedInstrumentNode.setUncertaintyVector(u_vector);
					

				} catch (IOException e) {
					
					e.printStackTrace();
				}


				try {
					binstream.close();
				} catch (IOException e) {
					
					e.printStackTrace();
				}
		        
		        
		      }
		
		return selectedInstrumentNode;
		}
		catch (SQLException ex) {

			throw new SPECCHIOFactoryException(ex);
		}
	}
	
	/**
	 * 
	 * Get all associated values for a given spectrum node id including the uncertainty vector
	 * 
	 * @param spectrum_node_id the spectrum_node_id of interest
	 * 
	 * @throws  SPECCHIOFactoryException database error
	 * 
	 */
	
	public UncertaintySpectrumNode getSpectrumNode(int spectrum_node_id) throws SPECCHIOFactoryException {

		UncertaintySpectrumNode node = new UncertaintySpectrumNode();
		
		try {
		
		// create SQL-building objects
		SQL_StatementBuilder SQL = getStatementBuilder();
		
		String sql_stmt = "SELECT node_description, u_vector, confidence_level, abs_rel, unit_id from spectrum_node where spectrum_node_id = ?";

		PreparedStatement pstmt = SQL.prepareStatement(sql_stmt);

		pstmt.setInt(1, spectrum_node_id);
		
		ResultSet rs = pstmt.executeQuery();

		//Getting information from ResultSet
		
		 while (rs.next()) {
		        String node_description = rs.getString("node_description");
		        double confidence_level = rs.getDouble("confidence_level");
		        String abs_rel = rs.getString("abs_rel");
		        int unit_id = rs.getInt("unit_id");
		        	        
		        Blob u_vector_blob = rs.getBlob("u_vector");
		        
		        System.out.println(node_description + ", " + confidence_level + ", " + abs_rel +
		                           ", " + unit_id +  ", " + u_vector_blob);

				 node.setAbsRel(abs_rel);
				 node.setConfidenceLevel(confidence_level);
				 node.setNodeType("spectrum");
				 node.setUnitId(unit_id);
				 node.setUncertaintyNodeId(spectrum_node_id);
				 node.setNodeDescription(node_description);
		        
				InputStream binstream = u_vector_blob.getBinaryStream();
				DataInput dis = new DataInputStream(binstream);

				try {
					int dim = binstream.available() / 4;
					
					System.out.println("dim of spectrum_node u_vector: " + dim);

					float[] u_vector = new float[dim];

					for(int i = 0; i < dim; i++)
					{
						try {
							Float f = dis.readFloat();
							u_vector[i] = f.floatValue();
						} catch (IOException e) {
							
							e.printStackTrace();
						}				
					}


					node.setUncertaintyVector(u_vector);
					

				} catch (IOException e) {
					
					e.printStackTrace();
				}


				try {
					binstream.close();
				} catch (IOException e) {
					
					e.printStackTrace();
				}
		        
		        
		      }
		
		return node;
		}
		catch (SQLException ex) {

			throw new SPECCHIOFactoryException(ex);
		}
	}
	
	/**
	 * Insert a new instrument node into the database.
	 * 
	 * @param instrument_node	the instrument_node to insert
	 * 
	 * @throws SPECCHIOFactoryException	the instrument_node could not be inserted
	 */
	public void insertInstrumentNode(UncertaintyInstrumentNode instrument_node) throws SPECCHIOFactoryException {
		
		try {
			
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
			
			String query = "insert into instrument_node(node_description, confidence_level, abs_rel) " +
						" values (?, ?, ?)";
			
			
			PreparedStatement pstmt = SQL.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
							
			pstmt.setString (1, instrument_node.getNodeType());
			pstmt.setDouble (2, instrument_node.getConfidenceLevel());
			pstmt.setString (3, instrument_node.getAbsRel());
					
			int affectedRows = pstmt.executeUpdate();
			
			System.out.println("pstmt executed");
		
			ResultSet generatedKeys = pstmt.getGeneratedKeys();
					
			while (generatedKeys.next()) {

				int int_id = generatedKeys.getInt(1);
				
				System.out.println("inserted id: " + int_id);
				
				instrument_node.setInstrumentNodeId(int_id);
				
				
			}
			
			pstmt.close();
			
			String update_stm = "UPDATE instrument_node set u_vector = ? where instrument_node_id = "
					+ instrument_node.getInstrumentNodeId();
			
			PreparedStatement statement = SQL.prepareStatement(update_stm);		
					
			byte[] temp_buf;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutput dos = new DataOutputStream(baos);
			
			for (int i = 0; i < instrument_node.getUncertaintyVector().length; i++) {
				try {
					dos.writeFloat(instrument_node.getUncertaintyVector()[i]);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			temp_buf = baos.toByteArray();
	
			InputStream vector = new ByteArrayInputStream(temp_buf);
				
			statement.setBinaryStream(1, vector, instrument_node.getUncertaintyVector().length * 4);
			statement.executeUpdate();

			vector.close();
			statement.close();
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		} catch (IOException ex) {
		// TODO Auto-generated catch block
		throw new SPECCHIOFactoryException(ex);
	}	
		
	}


	
	
public void insertUncertaintyNode(UncertaintyInstrumentNode instr_node, int uc_set_id) throws SPECCHIOFactoryException {	
	
	// When we are inserting instrumentNode our node_type is "instrument"
	
	String node_type = instr_node.node_type;
	
	try {
		 
		 SQL_StatementBuilder SQL = getStatementBuilder();
		
		 System.out.println("uncertainty node type: instrument");
	 
		 String query = "insert into instrument_node(node_description, confidence_level, abs_rel, unit_id) " +
					" values (?, ?, ?, ?)";
		                                                                                        
		 PreparedStatement pstmt = SQL.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
				
		 pstmt.setString (1, instr_node.getNodeDescription()); 
		 pstmt.setDouble (2, instr_node.getConfidenceLevel());
		 pstmt.setString (3, instr_node.getAbsRel());
		 pstmt.setInt (4, instr_node.getUnitId());
		
		 int affectedRows = pstmt.executeUpdate();
		
		 ResultSet generatedKeys = pstmt.getGeneratedKeys();
				
		 while (generatedKeys.next()) {

			 int instrument_node_id = generatedKeys.getInt(1);
			
			 instr_node.setInstrumentNodeId(instrument_node_id); 
			
			 // What goes here? What are we returning?
			 // Q for Andy: do we return just an uncertainty node id?
				
		 }
		
			pstmt.close();
			
			String update_stm = "UPDATE instrument_node set u_vector = ? where instrument_node_id = "
					+ instr_node.getInstrumentNodeId();
			
			PreparedStatement statement = SQL.prepareStatement(update_stm);		
					
			byte[] temp_buf;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutput dos = new DataOutputStream(baos);
			
			for (int i = 0; i < instr_node.getUncertaintyVector().length; i++) {
				try {
					dos.writeFloat(instr_node.getUncertaintyVector()[i]);
					} catch (IOException e) {
						e.printStackTrace();
					}
			}

			temp_buf = baos.toByteArray();
	
			InputStream vector = new ByteArrayInputStream(temp_buf);
				
			statement.setBinaryStream(1, vector, instr_node.getUncertaintyVector().length * 4);
			statement.executeUpdate();

			vector.close();
				
			//Next step is to create new entry in uncertainty_node
				
			boolean is_spectrum = false;
				
			String uc_node_sql = "INSERT into uncertainty_node(is_spectrum, instrument_node_id, uncertainty_node_description) " +
							"VALUES (?, ?, ?)";
				
			PreparedStatement pstmt_uc_node = SQL.prepareStatement(uc_node_sql, Statement.RETURN_GENERATED_KEYS);
				
			pstmt_uc_node.setBoolean (1, is_spectrum); 
			pstmt_uc_node.setInt (2, instr_node.getInstrumentNodeId());
			pstmt_uc_node.setString(3,  instr_node.getNodeDescription());
				
			// Getting uncertainty node id in return
				
			int affectedRows_2 = pstmt_uc_node.executeUpdate();
				
			ResultSet generatedKeys_2 = pstmt_uc_node.getGeneratedKeys();
			
			int uc_node_id = 0;
			
			while (generatedKeys_2.next()) {

				uc_node_id = generatedKeys_2.getInt(1);
					
				instr_node.setUncertaintyNodeId(uc_node_id);
				
			}
			
			// Updating node set
			int node_num = updateNodeSet(uc_set_id, uc_node_id);
			
			// Returning uncertainty set
			UncertaintySet uc_set = getUncertaintySetNew(uc_set_id);
			
			Matrix current_adjacency_matrix = uc_set.getAdjacencyMatrix();
			int node_set_id = uc_set.getNodeSetId();
			
			System.out.println("node_num: " + node_num);
			 
			// Next we need to update the adjacency matrix
			Matrix final_adjacency_matrix;
			
			if (node_num == 1) {
				
				// Don't need to do any updates to the adjacency matrix if node_num = 1
				
				 final_adjacency_matrix = current_adjacency_matrix;
				
			 }
		
			else {
				
				// Here the current matrix_dimension is 1 less than the node num
				
				int current_matrix_dimension = node_num - 1;
				int final_matrix_dimension = node_num;
				
				System.out.println("Current matrix dimension " + current_matrix_dimension);
				System.out.println("New matrix dimension: " + final_matrix_dimension);
				
				System.out.println("Current adjacency matrix: " + current_adjacency_matrix);
				
				final_adjacency_matrix = DenseMatrix.factory.zeros(final_matrix_dimension, final_matrix_dimension);
				
				System.out.println("Final adjacency matrix initialisation: " + final_adjacency_matrix);
				
				for(int i= 0; i<current_matrix_dimension; i++) {
					
					for(int j= 0; j<current_matrix_dimension; j++) {
						System.out.println("i: " + i + " j: " + j);
						
						final_adjacency_matrix.setAsDouble(current_adjacency_matrix.getAsDouble(i, j), i, j);
						
					}

				}
				
				System.out.println("Final adjacency matrix before source pairs: " + final_adjacency_matrix);
				
				// Checking for uncertainty source pairs
				
				ArrayList<UncertaintySourcePair> uncertainty_source_pairs = instr_node.getUncertaintySourcePairs();
				
				if (uncertainty_source_pairs.size() > 0) {
					
					System.out.println("One or more uncertainty sources exist");
					System.out.println("The number of uncertainty source pairs is:" + uncertainty_source_pairs.size());
					
					final_adjacency_matrix = updateAdjacencyMatrix(uncertainty_source_pairs, final_adjacency_matrix, node_set_id, node_num);
					
				}
				
				
				
			}
			System.out.println("New adjacency matrix: " + final_adjacency_matrix);
			
			insertAdjacencyMatrix(final_adjacency_matrix, uc_set_id);
			
			System.out.println("End of insert statement");
	
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
	 	catch (IOException ex) {
	 		// TODO Auto-generated catch block
			throw new SPECCHIOFactoryException(ex);
		}	
	
	}


public void insertUncertaintyNode(UncertaintySpectrumNode spectrum_node, int uc_set_id, ArrayList<Integer> uc_spectrum_ids, ArrayList<Integer> uc_spectrum_subset_ids) throws SPECCHIOFactoryException {
	
	// Re-assigning arraylists
	
	spectrum_node.setSpectrumIds(uc_spectrum_ids);
	spectrum_node.setSpectrumSubsetIds(uc_spectrum_subset_ids);
	
	 String node_type = spectrum_node.node_type;
	 
	 // not sure if we need these explicitly:
	 ArrayList<Integer> spectrum_subset_list = new ArrayList<Integer>();
	 spectrum_subset_list = spectrum_node.getSpectrumSubsetIds();
	 
	 ArrayList<Integer> spectrum_ids_list = new ArrayList<Integer>();
	 spectrum_ids_list = spectrum_node.getSpectrumIds();
		
	 System.out.println("spectrum id list: " + spectrum_ids_list);
	 System.out.println("spectrum subset list: " + spectrum_subset_list);
	 
	 try {
	 
		 SQL_StatementBuilder SQL = getStatementBuilder();
		 
		 String spectrum_set_query = "insert into spectrum_set(spectrum_set_description) " +
				" values (?)";
	
	 
		 PreparedStatement spectrum_set_pstmt = SQL.prepareStatement(spectrum_set_query, Statement.RETURN_GENERATED_KEYS);
				
		 spectrum_set_pstmt.setString (1, spectrum_node.getSpectrumSetDescription());
		
		 int affectedRows = spectrum_set_pstmt.executeUpdate();
		
		 ResultSet generatedKeys = spectrum_set_pstmt.getGeneratedKeys();
		
		
		 while (generatedKeys.next()) {

			 int spectrum_set_id = generatedKeys.getInt(1);
			
			 spectrum_node.setSpectrumSetId(spectrum_set_id);
								
		 }
		
		 spectrum_set_pstmt.close(); 
		 
		 ArrayList<Integer> spectrum_ids = new ArrayList<Integer>();
		 spectrum_ids = spectrum_node.getSpectrumIds();
		 
		 if (spectrum_subset_list.size() == 0) {
				
				System.out.println("spectrum_subset_list empty");
				
				// Checking whether OneToMany relationship exists
				
				boolean is_one_to_many = isOneToMany(spectrum_node);
				System.out.println("is one to many: " + is_one_to_many);
				
				// if true: create 1 node then assign this to all spectrum ids in spectrum_subset
				
				// if false: every spectrum_id gets its own unique spectrum node id
				
				// The order for creating spectrum branch of schema: spectrum nodes, spectrum subset, spectrum set map
				
				// These statements are the same for both true/false:
				
				String spectrum_node_insert_sql = "insert into spectrum_node(node_description, confidence_level, abs_rel, unit_id, u_vector) " +
						" values (?, ?, ?, ?, ?)";
				
				PreparedStatement spectrum_node_insert_stmt = SQL.prepareStatement(spectrum_node_insert_sql, Statement.RETURN_GENERATED_KEYS);
				
				String spectrum_subset_insert_sql = "insert into spectrum_subset(spectrum_subset_id) " +
				"values (?)";
				
				PreparedStatement spectrum_subset_insert_stmt = SQL.prepareStatement(spectrum_subset_insert_sql, Statement.RETURN_GENERATED_KEYS);
				
				String spectrum_subset_map_insert_sql = "insert into spectrum_subset_map(spectrum_subset_id, spectrum_node_id, spectrum_id) " +
						" values (?, ?, ?)";
				
				PreparedStatement spectrum_subset_map_insert_stmt = SQL.prepareStatement(spectrum_subset_map_insert_sql, Statement.RETURN_GENERATED_KEYS);
				
				// Getting next spectrum_subset_id. One spectrum_subset_id for all spectrum_ids 
				
				String spectrum_subset_select_max_sql = "select max(spectrum_subset_id) from spectrum_subset;";
				
				PreparedStatement spectrum_subset_select_max_stmt = SQL.prepareStatement(spectrum_subset_select_max_sql, Statement.RETURN_GENERATED_KEYS);
				
				// Find max spectrum_subset_id 
				
				ResultSet subset_max_rs = spectrum_subset_select_max_stmt.executeQuery();
				
				int max_spectrum_subset_id = 0;
				
				while (subset_max_rs.next()) {

					max_spectrum_subset_id = subset_max_rs.getInt(1);	
				
				}
				
				int spectrum_subset_id = max_spectrum_subset_id + 1;
				
				System.out.println("Max spectrum subset id: " + max_spectrum_subset_id);
				
				spectrum_subset_list.add(spectrum_subset_id);
			
				spectrum_subset_select_max_stmt.close();
				
				// Inserting new subset id into spectrum_subset table
				
				spectrum_subset_insert_stmt.setInt (1, spectrum_subset_id);
				
				int affectedRows_subset= spectrum_subset_insert_stmt.executeUpdate();
				
				spectrum_subset_insert_stmt.close();
				
				// Here is where one-to-many and many-to-many split:
				
				if(is_one_to_many) {
					
					// insert single spectrum node
					System.out.println("one-to-many spectrum node");
					
					spectrum_node_insert_stmt.setString (1, spectrum_node.getNodeDescription()); 
					spectrum_node_insert_stmt.setDouble (2, spectrum_node.getConfidenceLevel());
					spectrum_node_insert_stmt.setString (3, spectrum_node.getAbsRel());
					
					int unit_id = spectrum_node.getUnitId();
					
					// If no unit_id is specified then input at NULL in SQL
					if (unit_id == 0) {
						spectrum_node_insert_stmt.setNull(4, Types.INTEGER);
						} else {
							spectrum_node_insert_stmt.setInt(4, unit_id);
						}
					
					byte[] temp_buf;
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					DataOutput dos = new DataOutputStream(baos);
					
					for (int i = 0; i < spectrum_node.getUncertaintyVector().length; i++) {
						try {
							dos.writeFloat(spectrum_node.getUncertaintyVector()[i]);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					temp_buf = baos.toByteArray();
			
					InputStream vector = new ByteArrayInputStream(temp_buf);
						
					spectrum_node_insert_stmt.setBinaryStream(5, vector, spectrum_node.getUncertaintyVector().length * 4);
					
					int affectedRows_3= spectrum_node_insert_stmt.executeUpdate();
					ResultSet generatedKeys_3 = spectrum_node_insert_stmt.getGeneratedKeys();
					
					int spectrum_node_id = 0;
				
					while (generatedKeys_3.next()) {
						spectrum_node_id = generatedKeys_3.getInt(1);	
					}
					
					spectrum_node_insert_stmt.close();
					
					// insert row in spectrum_subset for each spectrum_id
					
					for(int i=0; i<spectrum_ids.size(); i++) {
						
						spectrum_subset_map_insert_stmt.setInt (1, spectrum_subset_id);
						spectrum_subset_map_insert_stmt.setInt (2, spectrum_node_id);
						spectrum_subset_map_insert_stmt.setInt (3, spectrum_ids.get(i));
					
						int affectedRows_4= spectrum_subset_map_insert_stmt.executeUpdate();
					
						ResultSet generatedKeys_4 = spectrum_subset_map_insert_stmt.getGeneratedKeys();
						
						
					}
					spectrum_subset_map_insert_stmt.close();

				}
				else {
					
					System.out.println("many to many spectrum node");
					
					for(int i=0; i<spectrum_node.getUncertaintyVectors().length; i++) { // note: spectrum ids can be zero, hence, use matrix size to loop over inserts
						
						spectrum_node_insert_stmt.setString (1, spectrum_node.getNodeDescription()); 
						spectrum_node_insert_stmt.setDouble (2, spectrum_node.getConfidenceLevel());
						spectrum_node_insert_stmt.setString (3, spectrum_node.getAbsRel());
						
						int unit_id = spectrum_node.getUnitId();
						
						// If no unit_id is specified then input at NULL in SQL
						if (unit_id == 0) {
							spectrum_node_insert_stmt.setNull(4, Types.INTEGER);
							} else {
								spectrum_node_insert_stmt.setInt(4, unit_id);
							}
						
						byte[] temp_buf;
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						DataOutput dos = new DataOutputStream(baos);
				
						// Each row represents a single spectrum's uncertainty vector
					
						for (int j = 0; j < spectrum_node.getUncertaintyVectors()[i].length; j++) {
							try {
								//System.out.println("uncertainty_vector: "+ spectral_set.getUncertaintyVectors()[i][j]);
								dos.writeFloat(spectrum_node.getUncertaintyVectors()[i][j]);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}

						temp_buf = baos.toByteArray();
		
						InputStream vector = new ByteArrayInputStream(temp_buf);
					
						spectrum_node_insert_stmt.setBinaryStream(5, vector, spectrum_node.getUncertaintyVectors()[i].length*4);
					
						int affectedRows_3= spectrum_node_insert_stmt.executeUpdate();
					
						ResultSet generatedKeys_3 = spectrum_node_insert_stmt.getGeneratedKeys();
						
						int spectrum_node_id = 0;
					
						while (generatedKeys_3.next()) {

							spectrum_node_id = generatedKeys_3.getInt(1);	
					
						}
					
						spectrum_subset_map_insert_stmt.setInt (1, spectrum_subset_id);
						spectrum_subset_map_insert_stmt.setInt (2, spectrum_node_id);
						if(spectrum_ids.size() > 0)
							spectrum_subset_map_insert_stmt.setInt (3, spectrum_ids.get(i));
						else
						{
							// No spectrum ids are provided, which means that there is no direct link of this uncertainty matrix to actual spectral vectors in the database
							// This case happens when e.g. data are computed on the fly
							spectrum_subset_map_insert_stmt.setString(3, null);
						}
					
						int affectedRows_4= spectrum_subset_map_insert_stmt.executeUpdate();
					
						ResultSet generatedKeys_4 = spectrum_subset_map_insert_stmt.getGeneratedKeys();

					}
				
					spectrum_node_insert_stmt.close();
					spectrum_subset_map_insert_stmt.close();

					}
				
			}
			
			else {
				
				System.out.println("spectrum_subset_list not empty");
				// This means that spectrum subset insertion has already occurred in a previous step in MATLAB 

			}
		 
		 	// Now using list of spectrum_subset_ids to populate spectrum_set_map
			
			String spectrum_set_sql = "INSERT into spectrum_set_map(spectrum_set_id, spectrum_subset_id) " +
					"VALUES (?, ?)";
			
			PreparedStatement pstmt_spectrum_set = SQL.prepareStatement(spectrum_set_sql);
			
			// Looping over all spectrum subset ids
			for(int i = 0; i < spectrum_subset_list.size(); i++)
			{
			    int current_spectrum_subset_id = spectrum_subset_list.get(i);
			    
				pstmt_spectrum_set.setInt(1, spectrum_node.getSpectrumSetId());
			    pstmt_spectrum_set.setInt(2, current_spectrum_subset_id);
			    
			    //batch here 
			    pstmt_spectrum_set.addBatch();
			    
			}
			
			pstmt_spectrum_set.executeBatch();
			pstmt_spectrum_set.close();
			
			// Now we can create an uncertainty set id which is needed for instrument/spectrum shared section below
			// Same as instrument except that is_spectrum is true and we have an associated spectrum set id rather than an instrument node id
			
			boolean is_spectrum = true; 
			
			String uc_node_sql = "INSERT into uncertainty_node(is_spectrum, spectrum_set_id, uncertainty_node_description) " +
						"VALUES (?, ?, ?)";
			
			PreparedStatement pstmt_uc_node = SQL.prepareStatement(uc_node_sql, Statement.RETURN_GENERATED_KEYS);
			
			pstmt_uc_node.setBoolean (1, is_spectrum); 
			pstmt_uc_node.setInt(2, spectrum_node.getSpectrumSetId());
			pstmt_uc_node.setString(3, spectrum_node.getUncertaintyNodeDescription());
			
			// Getting uncertainty node id in return
			
			int affectedRows_5 = pstmt_uc_node.executeUpdate();
			
			ResultSet generatedKeys_5 = pstmt_uc_node.getGeneratedKeys();
			
			int uc_node_id = 0;
			
			while (generatedKeys_5.next()) {

				uc_node_id = generatedKeys_5.getInt(1);
			
				System.out.println("inserted uncertainty node id: " + uc_node_id);	
				
				spectrum_node.setUncertaintyNodeId(uc_node_id);
			
			}
		
		 int node_num = updateNodeSet(uc_set_id, uc_node_id);	
		
		 // Getting uncertainty set details
		 
		 UncertaintySet uc_set = getUncertaintySetNew(uc_set_id);
		 
		 Matrix current_adjacency_matrix = uc_set.getAdjacencyMatrix();
		 int node_set_id = uc_set.getNodeSetId();
		 
		 System.out.println("node_num: " + node_num);
		 
		 long columns = current_adjacency_matrix.getSize(0);
		 long lines = current_adjacency_matrix.getSize(1);	


		Matrix final_adjacency_matrix; 
		
		 
		 if (node_num == 1) {
			
			// Don't need to do any updates to the adjacency matrix if node_num = 1
			
			 final_adjacency_matrix = current_adjacency_matrix;
			
			
		 }
		else {
			
			// Here the current matrix_dimension is 1 less than the node num
			
			int current_matrix_dimension = node_num - 1;
			int final_matrix_dimension = node_num;
			
			System.out.println("Current matrix dimension " + current_matrix_dimension);
			System.out.println("New matrix dimension: " + final_matrix_dimension);
			
			System.out.println("Current adjacency matrix: " + current_adjacency_matrix);
			
			// Function here to update matrix? 
			// Before we extracted all the values but now we just want to add a row and col? 
			// Is this possible? Or can we iterate over each value and do it this way? 
			
			final_adjacency_matrix = DenseMatrix.factory.zeros(final_matrix_dimension, final_matrix_dimension);
			
			System.out.println("Final adjacency matrix initialisation: " + final_adjacency_matrix);
			
			// Using current_matrix_dimension - 1 for indexing
			
			for(int i= 0; i<current_matrix_dimension; i++) {
				
				for(int j= 0; j<current_matrix_dimension; j++) {
				
					System.out.println("i: " + i + " j: " + j);
					
					final_adjacency_matrix.setAsDouble(current_adjacency_matrix.getAsDouble(i, j), i, j);
					
				}

			}
			
			System.out.println("Final adjacency matrix before source pairs: " + final_adjacency_matrix);
			
			// Now need to check for uncertainty_source_pairs
			
			
			ArrayList<UncertaintySourcePair> uncertainty_source_pairs = spectrum_node.getUncertaintySourcePairs();
			
			if (uncertainty_source_pairs.size() > 0) {
				
				System.out.println("One or more uncertainty sources exist");
				System.out.println("The number of uncertainty source pairs is:" + uncertainty_source_pairs.size());
			

				final_adjacency_matrix = updateAdjacencyMatrix(uncertainty_source_pairs, final_adjacency_matrix, node_set_id, node_num);
				
			}
			
			
			
			
			
		}
		System.out.println("New adjacency matrix: " + final_adjacency_matrix);
		 
		// Updates to adjacency matrix complete. Inputing into sql
		insertAdjacencyMatrix(final_adjacency_matrix, uc_set_id);
					
		 
		 System.out.println("End of insert statement");
		 
	  }
	 
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
	
	
	}
	
	
	

	
	/**
 	* Insert a new spectrum subset into the database.
 	* 
 	* @param spectral_set_id	the spectral set id
 	* 
 	* @throws SPECCHIOFactoryException	the uncertainty node could not be inserted
 	*/

	public void insertSpectrumSubset(ArrayList<Integer> uc_spectrum_ids, UncertaintySpectrumNode spectrum_node) throws SPECCHIOFactoryException {

		// Re-assigning arraylists
		
		spectrum_node.setSpectrumIds(uc_spectrum_ids);

		try {
		
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
		
			String spectrum_node_insert_sql = "insert into spectrum_node(node_description, confidence_level, abs_rel, unit_id, u_vector) " +
				" values (?, ?, ?, ?, ?)";
			String spectrum_subset_map_insert_sql = "insert into spectrum_subset_map(spectrum_subset_id, spectrum_node_id, spectrum_id) " +
				" values (?, ?, ?)";
			String spectrum_subset_select_max_sql = "select max(spectrum_subset_id) from spectrum_subset;";
			
			String spectrum_subset_insert_sql = "insert ignore into spectrum_subset(spectrum_subset_id)" +
			"values (?)";
			
			// PreparedStatements
		
			PreparedStatement spectrum_node_insert_stmt = SQL.prepareStatement(spectrum_node_insert_sql, Statement.RETURN_GENERATED_KEYS);
			PreparedStatement spectrum_subset_map_insert_stmt = SQL.prepareStatement(spectrum_subset_map_insert_sql, Statement.RETURN_GENERATED_KEYS);
			PreparedStatement spectrum_subset_select_max_stmt = SQL.prepareStatement(spectrum_subset_select_max_sql, Statement.RETURN_GENERATED_KEYS);
			PreparedStatement spectrum_subset_insert_stmt = SQL.prepareStatement(spectrum_subset_insert_sql, Statement.RETURN_GENERATED_KEYS); 
			
			// Find max spectrum_subset_id 
			
			ResultSet subset_max_rs = spectrum_subset_select_max_stmt.executeQuery();
			
			int max_spectrum_subset_id = 0;
			
			while (subset_max_rs.next()) {

				max_spectrum_subset_id = subset_max_rs.getInt(1);	
			
			}
			
			int spectrum_subset_id = max_spectrum_subset_id + 1;
			
			System.out.println("Max spectrum subset id: " + max_spectrum_subset_id);
		
			spectrum_subset_select_max_stmt.close();
			
			spectrum_subset_insert_stmt.setInt(1, spectrum_subset_id);
			
			int affectedRows_4 = spectrum_subset_insert_stmt.executeUpdate();
			
			spectrum_subset_insert_stmt.close();
			
			// Insert into spectrum_node and spectrum_subset_map
			
			ArrayList<Integer> spectrum_ids = new ArrayList<Integer>();
			spectrum_ids = spectrum_node.getSpectrumIds();

			ArrayList<Integer> spectrum_subset_list = new ArrayList<Integer>();
			
			for(int i=0; i<spectrum_ids.size(); i++) {
				
				spectrum_node_insert_stmt.setString (1, spectrum_node.getNodeDescription());
				spectrum_node_insert_stmt.setDouble (2, spectrum_node.getConfidenceLevel());
				spectrum_node_insert_stmt.setString (3, spectrum_node.getAbsRel());
				
				int unit_id = spectrum_node.getUnitId();
				
				// If no unit_id is specified then input at NULL in SQL
				if (unit_id == 0) {
					spectrum_node_insert_stmt.setNull(4, Types.INTEGER);
					} else {
						spectrum_node_insert_stmt.setInt(4, unit_id);
					}
				
				byte[] temp_buf;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutput dos = new DataOutputStream(baos);
			
				// Each row represents a single spectrum's uncertainty vector
				
				for (int j = 0; j < spectrum_node.getUncertaintyVectors()[i].length; j++) {
					try {
						dos.writeFloat(spectrum_node.getUncertaintyVectors()[i][j]);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				temp_buf = baos.toByteArray();
	
				InputStream vector = new ByteArrayInputStream(temp_buf);
				
				spectrum_node_insert_stmt.setBinaryStream(5, vector, spectrum_node.getUncertaintyVectors()[i].length*4);
				
				int affectedRows_2= spectrum_node_insert_stmt.executeUpdate();
				
				ResultSet generatedKeys_2 = spectrum_node_insert_stmt.getGeneratedKeys();
				
				int spectrum_node_id = 0;
				
				while (generatedKeys_2.next()) {

					spectrum_node_id = generatedKeys_2.getInt(1);	
				
				}
				
				// Now that we have spectrum node id we can use this to populate spectrum subset
				// For the time being, we have 1 spectrum subset for each spectrum id
				// spectrum_subset_id is currently on auto-increment
				
				spectrum_subset_map_insert_stmt.setInt (1, spectrum_subset_id);
				spectrum_subset_map_insert_stmt.setInt (2, spectrum_node_id);
				spectrum_subset_map_insert_stmt.setInt (3, spectrum_ids.get(i));
				
				int affectedRows_3= spectrum_subset_map_insert_stmt.executeUpdate();
				
			}
						
			spectrum_node_insert_stmt.close();
			spectrum_subset_map_insert_stmt.close();
			
			spectrum_node.setSpectrumSubsetId(spectrum_subset_id);

		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
	}
	
	/**
 	* Check whether spectrum_id-spectrum_node is one-to-many 
 	*   
 	* @param SpectralSet	the spectral set
 	* 
 	* @throws SPECCHIOFactoryException	the uncertainty node could not be inserted
 	*/
	
	public boolean isOneToMany(SpectralSet spectral_set) throws SPECCHIOFactoryException {
		
		System.out.println("Running isOneToMany");
		
		// Checking whether u_vectors or u_vector is populated
		// There is an edge case where we only have 1 spectrum id to insert. Then this could work with either u_vectors or u_vector.
		Float[] u_vector = spectral_set.u_vector;
		Float[][] u_vectors = spectral_set.u_vectors;
		
		if(u_vector != null)
			return true;
		else		
			return false;
		
		
	}
	
	/**
 	* Check whether spectrum_id-spectrum_node is one-to-many 
 	*   
 	* @param SpectralSet	the spectral set
 	* 
 	* @throws SPECCHIOFactoryException	the uncertainty node could not be inserted
 	*/
	
	public boolean isOneToMany(UncertaintySpectrumNode spectrum_node) throws SPECCHIOFactoryException {
		
		System.out.println("Running isOneToMany");
		
		// Checking whether u_vectors or u_vector is populated
		// There is an edge case where we only have 1 spectrum id to insert. Then this could work with either u_vectors or u_vector.
		float[] u_vector = spectrum_node.u_vector;
		float[][] u_vectors = spectrum_node.u_vectors;
		
		if(u_vector != null)
			return true;
		else		
			return false;
		
		
	}
	
	/**
 	* Update NodeSet for a given uncertainty set id
 	*   
 	* @param UncertaintySetId	the uncertainty set id
 	* 
 	* @throws SPECCHIOFactoryException
 	*/
	
	public int updateNodeSet(int uc_set_id, int uc_node_id) throws SPECCHIOFactoryException {
		
		SQL_StatementBuilder SQL = getStatementBuilder();
		
		int current_node_set_id = 0;
		int input_node_num;
		
		try {
		
			current_node_set_id = getNodeSetId(uc_set_id); // function in UncertaintyFactory

			// Checking corresponding row in uncertainty_node_set. If null then populating, if not then new node_num!

			// Breaking this into two steps:
			
			int max_node_num = getMaxNodeNum(current_node_set_id);

			String find_last_node_id_sql = "SELECT node_id from uncertainty_node_set where node_set_id = ? AND node_num =?";
			
			PreparedStatement pstmt_find_last_node_id = SQL.prepareStatement(find_last_node_id_sql);
		 
			pstmt_find_last_node_id.setInt(1, current_node_set_id);
			pstmt_find_last_node_id.setInt(2, max_node_num);
		 
			ResultSet find_last_node_id_rs = pstmt_find_last_node_id.executeQuery();
		 
			int last_node_id = 0;
		 
			while (find_last_node_id_rs.next()) {
			 
				last_node_id = find_last_node_id_rs.getInt(1);
			 
			}
		 
			pstmt_find_last_node_id.close();
		 
			
			PreparedStatement pstmt_node_set;
		 
		 
			if (last_node_id == 0) {
				input_node_num = max_node_num;
			 
				String node_set_sql = "UPDATE uncertainty_node_set set node_id = ? where node_set_id = ? and node_num = ?";
			 
				pstmt_node_set = SQL.prepareStatement(node_set_sql, Statement.RETURN_GENERATED_KEYS);
				
				pstmt_node_set.setInt (1, uc_node_id); 
				pstmt_node_set.setInt (2, current_node_set_id);
				pstmt_node_set.setInt (3, input_node_num);
			 
			}
			else {
				input_node_num = max_node_num + 1;
			 
				String node_set_sql = "INSERT into uncertainty_node_set(node_set_id, node_num, node_id) " + "VALUES (?, ?, ?)";
			 
				pstmt_node_set = SQL.prepareStatement(node_set_sql, Statement.RETURN_GENERATED_KEYS);
				
				pstmt_node_set.setInt (1, current_node_set_id); 
				pstmt_node_set.setInt (2, input_node_num);
				pstmt_node_set.setInt (3, uc_node_id);
			 
			}
		 
		 pstmt_node_set.executeUpdate();
		 
		 pstmt_node_set.close();
		
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
			} 	
		
		return input_node_num;
		
		}

	/**
 	* Get node set id for a given uncertainty set id
 	*   
 	* @param uc_set_id	the uncertainty set id
 	* 
 	* @throws SPECCHIOFactoryException
 	*/
	public int getNodeSetId(int uc_set_id) throws SPECCHIOFactoryException {
		
		SQL_StatementBuilder SQL = getStatementBuilder();
		
		try {
		
		int node_set_id = 0;
		String node_set_id_sql = "SELECT node_set_id from uncertainty_set where uncertainty_set_id = ?";
		PreparedStatement pstmt_node_set_id = SQL.prepareStatement(node_set_id_sql);
		pstmt_node_set_id.setInt(1, uc_set_id);
		ResultSet uc_set_rs = pstmt_node_set_id.executeQuery();
	
		while (uc_set_rs.next()) {
		 
	        node_set_id = uc_set_rs.getInt(1);
	           
		}

		pstmt_node_set_id.close();
		
		return node_set_id; 
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	/**
 	* Get max node num for a given node set id
 	*   
 	* @param node_set_id	the node set id
 	* 
 	* @throws SPECCHIOFactoryException
 	*/
	public int getMaxNodeNum(int node_set_id) throws SPECCHIOFactoryException {
		
		SQL_StatementBuilder SQL = getStatementBuilder();
		
		try {	
			
			String find_last_node_num_sql = "SELECT max(node_num) from uncertainty_node_set where node_set_id = ?";
			
			PreparedStatement pstmt_find_last_node_num = SQL.prepareStatement(find_last_node_num_sql);
		 
			pstmt_find_last_node_num.setInt(1, node_set_id);
		 
			ResultSet find_last_node_num_rs = pstmt_find_last_node_num.executeQuery();
		 
			int max_node_num = 0;
		 
			while (find_last_node_num_rs.next()) {
			 
				max_node_num = find_last_node_num_rs.getInt(1);
			 
			}
		 
			pstmt_find_last_node_num.close();

			return max_node_num;
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	/**
	 * Get adjacency matrix for a given uncertainty set id
	 * 
	 * @param uc_set_id the uncertainty set id
	 * 
	 * @throws SPECCHIOFactoryException
	 */
	
	public Matrix getAdjacencyMatrixNewMethod(int uc_set_id) throws SPECCHIOFactoryException {
		
		// This method uses ujmp SerializationUtil 
		// Should be better because we don't have to try and reconstruct the matrix using dimensions
		
		Matrix adjacency_matrix = DenseMatrix.factory.zeros(1, 1);
		
		// Create query
		// Create resultset
		// Try to deserialize the binstream (magic)
		// Try a test here somewhere with printStackTrace()
		
		try {
			
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
			String sql_stmt = "SELECT adjacency_matrix, node_set_id, uncertainty_set_description from uncertainty_set where uncertainty_set_id = ?";
			PreparedStatement pstmt = SQL.prepareStatement(sql_stmt);
			pstmt.setInt(1, uc_set_id);
			ResultSet rs = pstmt.executeQuery();
			
			while (rs.next()) {
				
				Blob adjacency_blob = rs.getBlob(1);
				
				// If blob is null we need to deal with this scenario
				if (adjacency_blob == null) {
        		
					System.out.println("adjacency matrix is empty");
					
				
				}
				
				else {
			
				InputStream binstream = adjacency_blob.getBinaryStream();
				
				adjacency_matrix = (Matrix)SerializationUtil.deserialize(binstream);
				
				}
				
			}
			
			rs.close();
			
			// Check here the matrix size
			//long[] adj_matrix_size = adjacency_matrix.getSize(); 
			
			long[] cube_size = adjacency_matrix.getSize();
			int dimensions = cube_size.length;
			//int[] size = new int[dimensions];
			
			//for (int i = 0;1<dimensions;i++) {
			//	size[i] = (int) cube_size[i];
 				
			//}
			System.out.println(dimensions);
			
			
		} catch (SQLException ex) {

		throw new SPECCHIOFactoryException(ex);
		}
		catch (IOException ex2) {
			ex2.printStackTrace();
		}
		catch (ClassNotFoundException ex3) {
			ex3.printStackTrace();
		}
		
		return adjacency_matrix;

		
	}
	
	/**
	 * Get adjacency matrix for a given uncertainty set id
	 * 
	 * @param uc_set_id the uncertainty set id
	 * 
	 * @throws SPECCHIOFactoryException
	 */
	
	public UncertaintySet getUncertaintySetNew(int uc_set_id) throws SPECCHIOFactoryException {
		
		UncertaintySet uc_set = new UncertaintySet();
		
		// This method uses ujmp SerializationUtil 
		// Should be better because we don't have to try and reconstruct the matrix using dimensions
		
		Matrix adjacency_matrix = DenseMatrix.factory.zeros(1, 1);
		
		// Create query
		// Create resultset
		// Try to deserialize the binstream (magic)
		// Try a test here somewhere with printStackTrace()
		
		try {
			
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
			String sql_stmt = "SELECT adjacency_matrix, node_set_id, uncertainty_set_description from uncertainty_set where uncertainty_set_id = ?";
			PreparedStatement pstmt = SQL.prepareStatement(sql_stmt);
			pstmt.setInt(1, uc_set_id);
			ResultSet rs = pstmt.executeQuery();
			
			while (rs.next()) {
				
				Blob adjacency_blob = rs.getBlob("adjacency_matrix");
				int node_set_id = rs.getInt("node_set_id");
				String uncertainty_set_description = rs.getString("uncertainty_set_description");
						
				uc_set.setNodeSetId(node_set_id);
				uc_set.setUncertaintySetDescription(uncertainty_set_description);
				uc_set.setUncertaintySetId(uc_set_id);
				
				// If blob is null we need to deal with this scenario
				if (adjacency_blob == null) {
        		
					System.out.println("adjacency matrix is empty");
					
				
				}
				
				else {
			
				InputStream binstream = adjacency_blob.getBinaryStream();
				
				adjacency_matrix = (DenseMatrix)SerializationUtil.deserialize(binstream);
				
				}
				
			}
			
			rs.close();
			
			uc_set.setAdjacencyMatrix(adjacency_matrix);
			
			
			// Check here the matrix size
			//long[] adj_matrix_size = adjacency_matrix.getSize(); 
			
			long[] cube_size = adjacency_matrix.getSize();
			int dimensions = cube_size.length;
			//int[] size = new int[dimensions];
			
			//for (int i = 0;1<dimensions;i++) {
			//	size[i] = (int) cube_size[i];
 				
			//}
			
			
		} catch (SQLException ex) {

		throw new SPECCHIOFactoryException(ex);
		}
		catch (IOException ex2) {
			ex2.printStackTrace();
		}
		catch (ClassNotFoundException ex3) {
			ex3.printStackTrace();
		}
		
		return uc_set;

		
	}


	/**
	 * Retrieve uncertainty node subsets.
	 *
	 * @param uncertainty_node_id
	 *
	 * @return the corresponding uncertainty node components grouped into subsets that make up this combined node
	 *
	 * @throws SPECCHIOFactoryException
	 *
	 */

	public ArrayList<UncertaintyNode> getUncertaintyNodeSubSets(int uncertainty_node_id) {

		ArrayList<UncertaintyNode> nodes = new ArrayList<UncertaintyNode>();

		ArrayList<UncertaintyNode> u_nodes = getUncertaintyNodeComponents(uncertainty_node_id);

		// combine nodes into node per subset (only from spectrum uncertainty nodes)
		if(u_nodes.size() > 0 && u_nodes.get(0) instanceof UncertaintySpectrumNode) {
			ListIterator<UncertaintyNode> li = u_nodes.listIterator();
			int cnt = 0;
			int curr_subset_id = 0;
			UncertaintySpectrumNode subset_node = null;
			ArrayList<float[]> curr_vectors = new ArrayList<float[]>();

			while (li.hasNext()) {
				UncertaintySpectrumNode n = (UncertaintySpectrumNode) li.next();

				if(curr_subset_id != n.spectrum_subset_id)
				{
					if(curr_vectors.size() > 0)
					{
						// set the collected vectors in the last subset node
						// add measurements as float matrix
						subset_node.setUncertaintyVectors(curr_vectors);
					}
					curr_subset_id = n.spectrum_subset_id;
					subset_node = new UncertaintySpectrumNode();

					// no check on congruence of the following metadata: they are assumed to be identical in the whole set.
					subset_node.spectrum_set_id = n.spectrum_set_id;
					subset_node.setNodeDescription(n.getNodeDescription());
					subset_node.setUncertaintyNodeDescription(n.getUncertaintyNodeDescription());
					subset_node.setConfidenceLevel(n.getConfidenceLevel());
					subset_node.setAbsRel(n.getAbsRel());
					subset_node.setUnitId(n.getUnitId());
					nodes.add(subset_node);
				}

				// set data of combined node
				subset_node.spectrum_ids.add(n.spectrum_ids.get(0));
				curr_vectors.add(n.getUncertaintyVector());

			}

			// add final subset matrix
			if(curr_vectors.size() > 0)
			{
				// set the collected vectors in the last subset node
				// add measurements as float matrix
				subset_node.setUncertaintyVectors(curr_vectors);
			}

		}
		else
		{
			nodes = u_nodes;
		}

		return nodes;

	}



	/**
	 * Retrieve uncertainty node components.
	 *
	 * @param uncertainty_node_id
	 *
	 * @return the corresponding uncertainty node components that make up this combined node
	 *
	 * @throws SPECCHIOFactoryException
	 *
	 */

	public ArrayList<UncertaintyNode> getUncertaintyNodeComponents(int uncertainty_node_id) {

		ArrayList<UncertaintyNode> nodes = new ArrayList<UncertaintyNode>();
		boolean is_spectrum = false;
		int spectrum_node_id = 0;
		int spectrum_set_id = 0;
		int instrument_node_id = 0;
		String uncertainty_node_description = "";

		try {

			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
			String sql_stmt = "SELECT is_spectrum, instrument_node_id, spectrum_set_id, uncertainty_node_description FROM uncertainty_node where node_id = ?";
			PreparedStatement pstmt = SQL.prepareStatement(sql_stmt);
			pstmt.setInt(1, uncertainty_node_id);
			ResultSet rs = pstmt.executeQuery();

			// there should be always only one entry for an uncertainty node, so, the while is a bit of an overkill
			while (rs.next()) {

				is_spectrum = rs.getBoolean(1);
				instrument_node_id = rs.getInt(2);
				spectrum_set_id = rs.getInt(3);
				uncertainty_node_description = rs.getString(4);

			}

			rs.close();



			if (is_spectrum)
			{
				sql_stmt = "SELECT spectrum_node_id, spectrum_id, ss.spectrum_subset_id  FROM spectrum_set_map ssm, spectrum_subset ss, spectrum_subset_map sssm where ssm.spectrum_set_id = ? and ssm.spectrum_subset_id = ss.spectrum_subset_id and sssm.spectrum_subset_id = ssm.spectrum_subset_id";
				pstmt = SQL.prepareStatement(sql_stmt);
				pstmt.setInt(1, spectrum_set_id);
				rs = pstmt.executeQuery();

				while (rs.next()) {

					spectrum_node_id = rs.getInt(1);
					int spectrum_id = rs.getInt(2);
					int spectrum_subset_id = rs.getInt(3);

					UncertaintySpectrumNode usn = this.getSpectrumNode(spectrum_node_id);
					usn.spectrum_ids.add(spectrum_id);
					usn.spectrum_node_id = spectrum_node_id;
					usn.spectrum_set_id = spectrum_set_id;
					usn.spectrum_subset_id = spectrum_subset_id;
					usn.uncertainty_node_description = uncertainty_node_description;

					nodes.add(usn);

				}

			}
			else
			{
				UncertaintyNode i_node = this.getInstrumentNode(instrument_node_id);
				nodes.add(i_node);
			}

		} catch (SQLException ex) {

			throw new SPECCHIOFactoryException(ex);
		}

		return nodes;

	}
	
	/**
	 * Get adjacency matrix for a given uncertainty set id
	 * 
	 * @param uc_set_id the uncertainty set id
	 * 
	 * @throws SPECCHIOFactoryException
	 */
	
	public UncertaintySet getUncertaintySet(int uc_set_id) throws SPECCHIOFactoryException {
		
		// This function gets all the data needed in order to populate digraph in MATLAB
		
		UncertaintySet uc_set = new UncertaintySet();
		
		// This method uses ujmp SerializationUtil 
		Matrix adjacency_matrix = DenseMatrix.factory.zeros(1, 1);
		
		// Creating some empty lists
		ArrayList<Integer> node_id_list = new ArrayList<Integer>();
		ArrayList<Integer> node_num_list = new ArrayList<Integer>();
		ArrayList<String> node_description_list = new ArrayList<String>();
		
		
		try {
			
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
			String sql_stmt = "SELECT adjacency_matrix, node_set_id, uncertainty_set_description from uncertainty_set where uncertainty_set_id = ?";
			PreparedStatement pstmt = SQL.prepareStatement(sql_stmt);
			pstmt.setInt(1, uc_set_id);
			ResultSet rs = pstmt.executeQuery();
			
			while (rs.next()) {
				
				Blob adjacency_blob = rs.getBlob("adjacency_matrix");
				int node_set_id = rs.getInt("node_set_id");
				String uncertainty_set_description = rs.getString("uncertainty_set_description");
						
				uc_set.setNodeSetId(node_set_id);
				uc_set.setUncertaintySetDescription(uncertainty_set_description);
				uc_set.setUncertaintySetId(uc_set_id);
				
				// If blob is null we need to deal with this scenario
				if (adjacency_blob == null) {
        		
					System.out.println("adjacency matrix is empty");
					
				
				}
				
				else {
			
				InputStream binstream = adjacency_blob.getBinaryStream();
				
				adjacency_matrix = (DenseMatrix)SerializationUtil.deserialize(binstream);
				
				}
				
			}
			
			rs.close();
			
			uc_set.setAdjacencyMatrix(adjacency_matrix);
			
			// Now we have uc_set data, getting node nums and descriptions
			
			String select_node_set_sql_stmt = "SELECT node_num, node_id from uncertainty_node_set where node_set_id = ?";
			PreparedStatement select_node_set_sql_pstmt = SQL.prepareStatement(select_node_set_sql_stmt);
			
			select_node_set_sql_pstmt.setInt(1, uc_set.getNodeSetId());
			ResultSet select_node_set_rs = select_node_set_sql_pstmt.executeQuery();
			
			int j = 1; // a counter
			while (select_node_set_rs.next()) {
				
				System.out.println("i in select_node_set_rs is: " + j);
				
				int node_id = select_node_set_rs.getInt("node_id");
				int node_num = select_node_set_rs.getInt("node_num");
				
				// Extract node_num and node_id
				
				node_id_list.add(node_id);
				node_num_list.add(node_num);
				
				j = j + 1;
				
			}
			select_node_set_rs.close();
			
			uc_set.setUncertaintyNodeIds(node_id_list);
			uc_set.setNodeNums(node_num_list);
			
			// Now we have populated node_id_list going to use these ids to populate node_description_list
			
			String select_uc_node_sql_stmt = "SELECT uncertainty_node_description from uncertainty_node where node_id = ?";
			PreparedStatement select_uc_node_sql_pstmt = SQL.prepareStatement(select_uc_node_sql_stmt);
			
			for(int i=0; i<node_id_list.size(); i++) {
				
				int node_id = node_id_list.get(i);
				select_uc_node_sql_pstmt.setInt(1, node_id);
				
				    ResultSet select_uc_node_rs = select_uc_node_sql_pstmt.executeQuery();
				 	
				    String uc_node_description = new String();
				    
				    while (select_uc_node_rs.next()) {
				 
				    	uc_node_description = select_uc_node_rs.getString(1);
				    	node_description_list.add(uc_node_description);
				    	
				 }
				
			}
			
			
			uc_set.setUncertaintyNodeDescriptions(node_description_list);

			System.out.println("node_description_list: " + node_description_list);
			
			// New adjacency matrix retrieval
			
			int [][] adjacency_matrix_as_int_array = adjacency_matrix.toIntArray();
			
			System.out.println("adjacency_matrix_as_int_array");
			System.out.println(Arrays.deepToString(adjacency_matrix_as_int_array));
			
			// Assigning integer array to uncertainty set
			
			uc_set.setAdjacencyMatrixAsIntArray(adjacency_matrix_as_int_array);
			
			
		} catch (SQLException ex) {

		throw new SPECCHIOFactoryException(ex);
		}
		catch (IOException ex2) {
			ex2.printStackTrace();
		}
		catch (ClassNotFoundException ex3) {
			ex3.printStackTrace();
		}
		
		return uc_set;

		
	}
	
	/**
	 * Get the ids of uncertainty sets that contain a particular spectrum id
	 * 
	 * @param spectrum_id a spectrum id
	 * 
	 * @throws SPECCHIOFactoryException
	 */
	
	public ArrayList<Integer> getUncertaintySetIds(int spectrum_id) throws SPECCHIOFactoryException {

		ArrayList<Integer> uc_set_id_list = new ArrayList<Integer>();
		
		try {
			
			SQL_StatementBuilder SQL = getStatementBuilder();
			
			String sql_stmt = "\n" + 
					"SELECT DISTINCT us.uncertainty_set_id \n" + 
					"FROM spectrum_subset_map ss \n" + 
					"INNER JOIN spectrum_set_map ssm \n" + 
					"	ON ss.spectrum_subset_id = ssm.spectrum_subset_id \n" + 
					"	AND ss.spectrum_id = ? \n" + 
					"INNER JOIN uncertainty_node un \n" + 
					"	ON un.spectrum_set_id = ssm.spectrum_set_id\n" + 
					"INNER JOIN uncertainty_node_set uns\n" + 
					"	ON uns.node_id = un.node_id\n" + 
					"INNER JOIN uncertainty_set us\n" + 
					"	ON us.node_set_id = uns.node_set_id;\n" + 
					"";
			PreparedStatement pstmt = SQL.prepareStatement(sql_stmt);
			pstmt.setInt(1, spectrum_id);
			ResultSet rs = pstmt.executeQuery();
			
			while (rs.next()) {
				
				int uc_set_id = rs.getInt("uncertainty_set_id");
				
				uc_set_id_list.add(uc_set_id);
				
			}
			
		} catch (SQLException ex) {

			throw new SPECCHIOFactoryException(ex);
		}
		
		return uc_set_id_list;
		
	}


	/**
	 * Get the ids of uncertainty sets that contain a list of spectrum_ids
	 *
	 * @param spectrum_ids spectrum id list
	 *
	 * @return an arraylist of UncertaintySetSpectraList objects
	 *
	 * @throws SPECCHIOFactoryException
	 */

	public ArrayList<UncertaintySetSpectraList> getUncertaintySetSpectraLists(ArrayList<Integer> spectrum_ids) throws SPECCHIOFactoryException {

		ArrayList<UncertaintySetSpectraList> uc_set_spectra_list = new ArrayList<UncertaintySetSpectraList>();

		try {

			SQL_StatementBuilder SQL = getStatementBuilder();

			String spectrum_ids_str = SQL.conc_ids(spectrum_ids);

			String sql_stmt = "\n" +
					"SELECT DISTINCT us.uncertainty_set_id, ss.spectrum_id \n" +
					"FROM spectrum_subset_map ss \n" +
					"INNER JOIN spectrum_set_map ssm \n" +
					"	ON ss.spectrum_subset_id = ssm.spectrum_subset_id \n" +
					"	AND ss.spectrum_id in ( " + spectrum_ids_str + " )\n" +
					"INNER JOIN uncertainty_node un \n" +
					"	ON un.spectrum_set_id = ssm.spectrum_set_id\n" +
					"INNER JOIN uncertainty_node_set uns\n" +
					"	ON uns.node_id = un.node_id\n" +
					"INNER JOIN uncertainty_set us\n" +
					"	ON us.node_set_id = uns.node_set_id\n" +
					" order by us.uncertainty_set_id";
			PreparedStatement pstmt = SQL.prepareStatement(sql_stmt);

			ResultSet rs = pstmt.executeQuery();

			int curr_uc_set_id = 0;
			UncertaintySetSpectraList curr_list = null;

			while (rs.next()) {

				int uc_set_id = rs.getInt("uncertainty_set_id");
				int spectrum_id = rs.getInt("spectrum_id");

				if(uc_set_id != curr_uc_set_id)
				{
					// create a new container
					curr_uc_set_id = uc_set_id;
					curr_list = new UncertaintySetSpectraList();
					curr_list.uncertainty_set_id = uc_set_id;
					curr_list.spectrum_ids.add(spectrum_id);
					uc_set_spectra_list.add(curr_list);
				}
				else
				{
					// container is still valid; keep adding spectrum ids
					curr_list.spectrum_ids.add(spectrum_id);
				}

			}

			pstmt.close();

		} catch (SQLException ex) {

			throw new SPECCHIOFactoryException(ex);
		}

		return uc_set_spectra_list;

	}
	
	/**
 	* Get node num for a given node set id
 	*   
 	* @param node_set_id	the node set id
 	* @param node_id 		the nodeid
 	* 
 	* @throws SPECCHIOFactoryException
 	*/
	public int getNodeNum(int node_set_id, int node_id) throws SPECCHIOFactoryException {
		
		SQL_StatementBuilder SQL = getStatementBuilder();
		
		try {	
			
			String find_node_num_sql = "SELECT node_num from uncertainty_node_set where node_set_id = ? and node_id = ?";
			
			PreparedStatement pstmt_find_node_num = SQL.prepareStatement(find_node_num_sql);
		 
			pstmt_find_node_num.setInt(1, node_set_id);
			pstmt_find_node_num.setInt(2,  node_id);
		 
			ResultSet find_node_num_rs = pstmt_find_node_num.executeQuery();
		 
			int node_num = 0;
		 
			while (find_node_num_rs.next()) {
			 
				node_num = find_node_num_rs.getInt(1);
			 
			}
		 
			pstmt_find_node_num.close();

			return node_num;
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	/**
 	* Get node num for a given node set id
 	*   
 	* @param final_adjacency_matrix		an adjacency matrix
 	* @param uc_set_id 					the uncertainty set id
 	* 
 	* @throws SPECCHIOFactoryException
 	*/
	public void insertAdjacencyMatrix(Matrix final_adjacency_matrix, int uc_set_id) throws SPECCHIOFactoryException {
		
		SQL_StatementBuilder SQL = getStatementBuilder();
		
		try {
		
			String update_uc_set_sql = "UPDATE uncertainty_set SET adjacency_matrix = ? WHERE uncertainty_set_id = ?";
			 
			PreparedStatement pstmt_uc_set = SQL.prepareStatement(update_uc_set_sql, Statement.RETURN_GENERATED_KEYS);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();	
			SerializationUtil.serialize(final_adjacency_matrix, baos);
			
			pstmt_uc_set.setBinaryStream (1, new ByteArrayInputStream(baos.toByteArray()), baos.size()); 
			pstmt_uc_set.setInt (2, uc_set_id);

			pstmt_uc_set.executeUpdate();
			pstmt_uc_set.close();
			
			System.out.println("insertAdjacencyMatrix finished");
			
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		catch (IOException ex2) {
			ex2.printStackTrace();
			
		}
		
	}
	
	/**
 	* Update 
 	*   
 	* @param uncertainty_source_pairs	uncertainty_source_pair arraylist
 	* @param adjacency_matrix			adjacency matrix
 	* 
 	* @throws SPECCHIOFactoryException
 	*/
	public Matrix updateAdjacencyMatrix(ArrayList<UncertaintySourcePair> uncertainty_source_pairs, Matrix adjacency_matrix, int node_set_id, int node_num) {
		
		SQL_StatementBuilder SQL = getStatementBuilder();

		try {
		// Make changes to adjacency matrix using source pairs
		
			for(int i = 0; i < uncertainty_source_pairs.size(); i++) {
			
				// Getting individual values of source pair
			
				UncertaintySourcePair source_pair = uncertainty_source_pairs.get(i);
			
				int node_id_of_source = source_pair.getSourceId();
				String description_of_source = source_pair.getSourceLinkDescription();
			
				int source_node_num  = getNodeNum(node_set_id, node_id_of_source);
			
				int edge_id = 0;
			
			    // If source link description is null then it's a 'simple' edge and edge value = 1
				if (description_of_source == null) {
				
					edge_id = 1;
				}
		
				else {
				
					// Checking whether the edge value exists in the database
		
					String edge_value_check_sql = "select edge_id, edge_value from uncertainty_edge where edge_value = ?";

					PreparedStatement pstmt_edge_value_check = SQL.prepareStatement(edge_value_check_sql, Statement.RETURN_GENERATED_KEYS);
				
					pstmt_edge_value_check.setString(1, description_of_source);
				
					ResultSet edge_value_check_rs = pstmt_edge_value_check.executeQuery();

					String matched_edge_value = null;
					int matched_edge_id = 0;
				
					while (edge_value_check_rs.next()) {
						matched_edge_value = edge_value_check_rs.getString("edge_value");
						matched_edge_id = edge_value_check_rs.getInt("edge_id");
					}
				
					// If edge description doesn't currently exist
					if (matched_edge_value == null) {
					
						System.out.println("description of source does not already exist in uncertainty edge table");
                     
						// inserting new row into uncertainty edge

						String edge_insert_sql = "INSERT into uncertainty_edge(edge_value) " + "VALUES (?)";

						PreparedStatement pstmt_edge_insert = SQL.prepareStatement(edge_insert_sql, Statement.RETURN_GENERATED_KEYS);
					 
						pstmt_edge_insert.setString(1, description_of_source);
					 
						System.out.println("Description of source: " + description_of_source);
					 
						int affectedRows_6= pstmt_edge_insert.executeUpdate();
					 
						ResultSet generatedKeys_6 = pstmt_edge_insert.getGeneratedKeys();
					 
						while (generatedKeys_6.next()) {
						 
							edge_id = generatedKeys_6.getInt(1);
							System.out.println("inserted new edge with id: " + edge_id);
						 
						}
					
					}
					else {
					 
						System.out.println("description of source already exists");
					 
						// Using edge_id that already exists for this description
					 
						edge_id = matched_edge_id;
					 
					}
				}
			
				System.out.println("Coordinates of adjacency matrix change: " + source_node_num + "," + node_num);
			    
				// minus 1 for java indexing
				adjacency_matrix.setAsInt(edge_id, source_node_num-1, node_num-1);	
	
			}

			return adjacency_matrix;
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}



}