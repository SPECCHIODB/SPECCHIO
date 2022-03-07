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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.ujmp.core.Matrix;
import org.ujmp.core.matrix.DenseMatrix;

import ch.specchio.eav_db.SQL_StatementBuilder;
import ch.specchio.types.AdjacencyMatrix;
import ch.specchio.types.InstrumentNode;
import ch.specchio.types.SpectralSet;
import ch.specchio.types.UncertaintySourcePair;

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
	* @param uncertainty_set_description the description of the uncertainty set to be created
	* 
	* @throws SPECCHIOFactoryException
	* 
 	*/
	
	public void insertNewUncertaintySet(SpectralSet spectral_set) {
		
		try {
		
		SQL_StatementBuilder SQL = getStatementBuilder();
		
		// Finding max of uncertainty_node_ids in uncertainty_node table
		
		String max_query = "SELECT MAX(node_set_id) from uncertainty_node_set";
		
		PreparedStatement max_pstmt = SQL.prepareStatement(max_query);
		
		ResultSet max_rs = max_pstmt.executeQuery();
		
		 while (max_rs.next()) {
		        int last_node_set_id = max_rs.getInt(1);
		        
		        int new_node_set_id = last_node_set_id + 1;
		        
		        spectral_set.node_set_id = new_node_set_id;
		              
		 }
		 
		max_pstmt.close();
		
		String uc_set_query = "insert into uncertainty_set(uncertainty_set_description, node_set_id) " +
				" values (?, ?)";
	
	
		PreparedStatement uc_set_pstmt = SQL.prepareStatement(uc_set_query, Statement.RETURN_GENERATED_KEYS);
				
		uc_set_pstmt.setString (1, spectral_set.getUncertaintySetDescription());
		uc_set_pstmt.setInt(2, spectral_set.getNodeSetId());
		
		int affectedRows = uc_set_pstmt.executeUpdate();
		
		ResultSet generatedKeys = uc_set_pstmt.getGeneratedKeys();
		
		
		while (generatedKeys.next()) {

			int uc_set_id = generatedKeys.getInt(1);
			
			System.out.println("inserted id: " + uc_set_id);
			
			spectral_set.setUncertaintySetId(uc_set_id);
			
			
		}
		
		
		uc_set_pstmt.close();
		
		// The first node gets id = null
		// There is a foreign key constraint on 'node_id' but because this column is nullable, we can use null
		
		String node_set_query = "insert into uncertainty_node_set(node_set_id, node_num) " +
				"values (?, ?)";
		
		PreparedStatement node_set_pstmt = SQL.prepareStatement(node_set_query, Statement.RETURN_GENERATED_KEYS);
		
		int node_num = 1;
		
		node_set_pstmt.setInt(1, spectral_set.getNodeSetId());
		node_set_pstmt.setInt(2, node_num);
		
		node_set_pstmt.executeUpdate();
		
		node_set_pstmt.close();
		
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	/**
	 * 
	 * Get adjacency matrix and uncertainty node information for an uncertainty set
	 * 
	 * @param uncertainty_set_id the uncertainty_set_id of interest
	 * 
	 * @throws SPECCHIOFactoryException 
	 */
	
	
	public SpectralSet getUncertaintySet(int uncertainty_set_id) throws SPECCHIOFactoryException {
		
		
		SpectralSet selectedUncertaintySet = new SpectralSet();
		
		//1. adjacency matrix info
		//2. node_set_id and uc_set_description
		//3. node_nums and node_ids
		//4. additional node info
		
		AdjacencyMatrix retrievedAdjacencyMatrix = getAdjacencyMatrix(uncertainty_set_id);
		
		// Assigning adjacency matrix to spectral set
		
		selectedUncertaintySet.adjacency_matrix = retrievedAdjacencyMatrix;
		
		try {
			
			// Select statement to get data from uncertainty_set
			
			SQL_StatementBuilder SQL = getStatementBuilder();
			
			String select_uc_set_sql_stmt = "SELECT node_set_id, uncertainty_set_description from uncertainty_set where uncertainty_set_id = ?";
			PreparedStatement select_uc_set_sql_pstmt = SQL.prepareStatement(select_uc_set_sql_stmt);
			select_uc_set_sql_pstmt.setInt(1, uncertainty_set_id);
			ResultSet select_uc_set_rs = select_uc_set_sql_pstmt.executeQuery();
			
			// Selecting data from uncertainty_node_set
			
			String select_uc_node_set_sql_stmt = "SELECT node_num, node_id from uncertainty_node_set where node_set_id = ?";
			PreparedStatement select_uc_node_set_sql_pstmt = SQL.prepareStatement(select_uc_node_set_sql_stmt);
			
			// Creating some lists
			ArrayList<Integer> node_id_list = new ArrayList<Integer>();
			ArrayList<Integer> node_num_list = new ArrayList<Integer>();
			ArrayList<String> node_description_list = new ArrayList<String>();
			
			//Matrix adjacency_matrix = DenseMatrix.factory.zeros(1, 1);;
			
			// Getting data from ResultSet
			
			while (select_uc_set_rs.next()) {
				
				int node_set_id = select_uc_set_rs.getInt("node_set_id");
				String uncertainty_set_description = select_uc_set_rs.getString("uncertainty_set_description");
				
				System.out.println("node_set_id: " + node_set_id);
				System.out.println("uncertainty_set_description: " + uncertainty_set_description);
				
				// Setting these values in the selectedUncertaintySet
				
				selectedUncertaintySet.setNodeSetId(node_set_id);
				selectedUncertaintySet.setUncertaintySetDescription(uncertainty_set_description);
		
				// Using the node_set_id 
				 
				select_uc_node_set_sql_pstmt.setInt(1, node_set_id);
				ResultSet select_uc_node_set_rs = select_uc_node_set_sql_pstmt.executeQuery();
				
				int j = 1;
				
				while (select_uc_node_set_rs.next()) {
					
					System.out.println("i in select_uc_node_set_rs is: " + j);
					
					int node_id = select_uc_node_set_rs.getInt("node_id");
					int node_num = select_uc_node_set_rs.getInt("node_num");
					
					System.out.println("node_num: " + node_num);
					System.out.println("node_id: " + node_id);
					
					// Extract node_num and node_id. I think this is extracted line by line.
					
					node_id_list.add(node_id);
					node_num_list.add(node_num);
					
					System.out.println("Node_id_list: " + node_id_list);
					System.out.println("Node_num_list: " + node_num_list);
					
					j = j + 1;
					
				}
				
				selectedUncertaintySet.setUncertaintyNodeIds(node_id_list);
				selectedUncertaintySet.setNodeNums(node_num_list);
				
				//select_uc_set_sql_pstmt.close();
				//select_uc_node_set_sql_pstmt.close();
				
				// Selecting from uncertainty_node
				
				String select_uc_node_sql_stmt = "SELECT uncertainty_node_description from uncertainty_node where node_id = ?";
				PreparedStatement select_uc_node_sql_pstmt = SQL.prepareStatement(select_uc_node_sql_stmt);
				
				for(int i=0; i<node_id_list.size(); i++) {
				
					int node_id = node_id_list.get(i);
					select_uc_node_sql_pstmt.setInt(1, node_id);
					
   				    ResultSet select_uc_node_rs = select_uc_node_sql_pstmt.executeQuery();
   				 	
   				    String uc_node_description = new String();
   				    
   				    while (select_uc_node_rs.next()) {
					 
   				    	uc_node_description = select_uc_node_rs.getString(1);
   				    	System.out.println("Node description: " + uc_node_description);
   				    	node_description_list.add(uc_node_description);
   				    	
					 }
					
				}
				
				//select_uc_node_sql_pstmt.close();
				
				selectedUncertaintySet.setUncertaintyNodeDescriptions(node_description_list);
				
				System.out.println("node_description_list: " + node_description_list);
				
				}
						
			return selectedUncertaintySet;
		}
		catch(SQLException ex) {

			ex.printStackTrace();
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
	
	public InstrumentNode getInstrumentNode(int instrument_node_id) throws SPECCHIOFactoryException {
		
		InstrumentNode selectedInstrumentNode = new InstrumentNode();
		
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
		        selectedInstrumentNode.setId(instrument_node_id);
		        
				InputStream binstream = u_vector_blob.getBinaryStream();
				DataInput dis = new DataInputStream(binstream);

				try {
					int dim = binstream.available() / 4;

					Float[] u_vector = new Float[dim];		

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
	 * Get all associated values for a given instrument node id including the uncertainty vector
	 * 
	 * @param instrument_node_id the instrument_node_id of interest
	 * 
	 * @throws  SPECCHIOFactoryException database error
	 * 
	 */
	
	public InstrumentNode getSpectrumNode(int spectrum_node_id) throws SPECCHIOFactoryException {
		
		InstrumentNode selectedInstrumentNode = new InstrumentNode();
		
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
		        
		        selectedInstrumentNode.setAbsRel(abs_rel);
		        selectedInstrumentNode.setConfidenceLevel(confidence_level);
		        selectedInstrumentNode.setNodeType("spectrum");
		        selectedInstrumentNode.setUnitId(unit_id);
		        selectedInstrumentNode.setId(spectrum_node_id);
		        
				InputStream binstream = u_vector_blob.getBinaryStream();
				DataInput dis = new DataInputStream(binstream);

				try {
					int dim = binstream.available() / 4;
					
					System.out.println("dim of spectrum_node u_vector: " + dim);

					Float[] u_vector = new Float[dim];		

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
	 * Insert a new instrument node into the database.
	 * 
	 * @param instrument_node	the instrument_node to insert
	 * 
	 * @throws SPECCHIOFactoryException	the instrument_node could not be inserted
	 */
	public void insertInstrumentNode(InstrumentNode instrument_node) throws SPECCHIOFactoryException {
		
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
				
				instrument_node.setId(int_id);
				
				
			}
			
			pstmt.close();
			
			String update_stm = "UPDATE instrument_node set u_vector = ? where instrument_node_id = "
					+ instrument_node.getId();
			
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
	
	/**
	 * Insert a new uncertainty node into the database.
	 * 
	 * @param spectral_set_id	the spectral set id
	 * 
	 * @throws SPECCHIOFactoryException	the uncertainty node could not be inserted
	 */
	
public void insertUncertaintyNode(ArrayList<UncertaintySourcePair> uc_pairs , ArrayList<Integer> uc_source_ids, ArrayList<Integer> uc_spectrum_ids, ArrayList<Integer> uc_spectrum_subset_ids, SpectralSet spectral_set) throws SPECCHIOFactoryException {
		 
		// Re-assigning arraylists
		
		spectral_set.setUncertaintySourceIds(uc_source_ids);
		spectral_set.setUncertaintySourcePairs(uc_pairs);
		spectral_set.setSpectrumIds(uc_spectrum_ids);
		spectral_set.setSpectrumSubsetIds(uc_spectrum_subset_ids);
		
		for (int i=0; i < spectral_set.uncertainty_source_pairs.size(); i++) {
			
			UncertaintySourcePair current_pair =  spectral_set.uncertainty_source_pairs.get(i);
			
		}
		
		ArrayList<Integer> spectrum_subset_list = new ArrayList<Integer>();
		spectrum_subset_list = spectral_set.getSpectrumSubsetIds();
		
		// If we are creating a node using spectrum subsets then we need to assign a few properties to our spectral set
		if (spectrum_subset_list.size() > 0) {
		
			spectral_set.setNodeType("spectrum");
			
			// Checking we have correct elements in spectrum subset list
	
			
		}
		
		try {
			
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
			
			String node_type = spectral_set.node_type;

			// Here we need an if statement to determine whether node is an instrument or spectrum node
			// The first inserts are spectrum/instrument dependent
			// Uncertainty node set and uncertainty set updates are the same process for both
			
			if(node_type.equals("instrument")) {
			
				System.out.println("uncertainty node type: instrument");
				
				String query = "insert into instrument_node(node_description, confidence_level, abs_rel, unit_id) " +
						" values (?, ?, ?, ?)";
			                                                                                        
				PreparedStatement pstmt = SQL.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
					
				pstmt.setString (1, spectral_set.getNodeDescription()); 
				pstmt.setDouble (2, spectral_set.getConfidenceLevel());
				pstmt.setString (3, spectral_set.getAbsRel());
				pstmt.setInt (4, spectral_set.getUnitId());
			
				int affectedRows = pstmt.executeUpdate();
			
				ResultSet generatedKeys = pstmt.getGeneratedKeys();
					
				while (generatedKeys.next()) {

					int instrument_node_id = generatedKeys.getInt(1);
				
					spectral_set.setInstrumentNodeId(instrument_node_id); 
				
				}
			
				pstmt.close();
			
			
				String update_stm = "UPDATE instrument_node set u_vector = ? where instrument_node_id = "
					+ spectral_set.getInstrumentNodeId();
			
				PreparedStatement statement = SQL.prepareStatement(update_stm);		
					
				byte[] temp_buf;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutput dos = new DataOutputStream(baos);
			
				for (int i = 0; i < spectral_set.getUncertaintyVector().length; i++) {
					try {
						dos.writeFloat(spectral_set.getUncertaintyVector()[i]);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				temp_buf = baos.toByteArray();
	
				InputStream vector = new ByteArrayInputStream(temp_buf);
				
				statement.setBinaryStream(1, vector, spectral_set.getUncertaintyVector().length * 4);
				statement.executeUpdate();

				vector.close();
				
				//Next step is to create new entry in uncertainty_node
				
				boolean is_spectrum = false;
				
				String uc_node_sql = "INSERT into uncertainty_node(is_spectrum, instrument_node_id, uncertainty_node_description) " +
							"VALUES (?, ?, ?)";
				
				PreparedStatement pstmt_uc_node = SQL.prepareStatement(uc_node_sql, Statement.RETURN_GENERATED_KEYS);
				
				pstmt_uc_node.setBoolean (1, is_spectrum); 
				pstmt_uc_node.setInt (2, spectral_set.getInstrumentNodeId());
				pstmt_uc_node.setString(3,  spectral_set.getUncertaintyNodeDescription());
				
				// Getting uncertainty node id in return
				
				int affectedRows_2 = pstmt_uc_node.executeUpdate();
				
				ResultSet generatedKeys_2 = pstmt_uc_node.getGeneratedKeys();
					
				while (generatedKeys_2.next()) {

					int uc_node_id = generatedKeys_2.getInt(1);
					
					spectral_set.setUncertaintyNodeId(uc_node_id);
				
				}
				
			}
				
			else if(node_type.equals("spectrum")) {
				
				System.out.println("uncertainty node type: spectrum");
			
				// Creating a new spectrum set
				
				// NB: Spectrum set id is on auto-increment
				
				String spectrum_set_query = "insert into spectrum_set(spectrum_set_description) " +
						" values (?)";
			
			
				PreparedStatement spectrum_set_pstmt = SQL.prepareStatement(spectrum_set_query, Statement.RETURN_GENERATED_KEYS);
						
				spectrum_set_pstmt.setString (1, spectral_set.getSpectrumSetDescription());
				
				int affectedRows = spectrum_set_pstmt.executeUpdate();
				
				ResultSet generatedKeys = spectrum_set_pstmt.getGeneratedKeys();
				
				
				while (generatedKeys.next()) {

					int spectrum_set_id = generatedKeys.getInt(1);
					
					spectral_set.setSpectrumSetId(spectrum_set_id);
					
					
				}
				
				spectrum_set_pstmt.close();
				
				ArrayList<Integer> spectrum_ids = new ArrayList<Integer>();
				spectrum_ids = spectral_set.getSpectrumIds();
				
				// Now need to check whether spectrum_subset_list is empty: 
				// if yes, continue populating spectrum nodes, if not skip to spectrum_set_map
				
				if (spectrum_subset_list.size() == 0) {
					
					System.out.println("spectrum_subset_list empty");
					
					// Checking whether OneToMany relationship exists
					
					boolean is_one_to_many = isOneToMany(spectral_set);
					System.out.println("is one to many: " + is_one_to_many);
					
					// if true: create 1 node then assign this to all spectrum ids in spectrum_subset
					
					// if false: every spectrum_id gets its own unique spectrum node id
					
					// The order for creating spectrum branch of schema: spectrum nodes, spectrum subset, spectrum set map
					
					// These statements are the same for both true/false:
					
					String spectrum_node_insert_sql = "insert into spectrum_node(node_description, confidence_level, abs_rel, unit_id, u_vector) " +
							" values (?, ?, ?, ?, ?)";
					
					PreparedStatement spectrum_node_insert_stmt = SQL.prepareStatement(spectrum_node_insert_sql, Statement.RETURN_GENERATED_KEYS);
					
					String spectrum_subset_insert_sql = "insert into spectrum_subset(spectrum_subset_id, spectrum_node_id, spectrum_id) " +
							" values (?, ?, ?)";
					
					PreparedStatement spectrum_subset_insert_stmt = SQL.prepareStatement(spectrum_subset_insert_sql, Statement.RETURN_GENERATED_KEYS);
					
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
					
					// Here is where one-to-many and many-to-many split:
					
					if(is_one_to_many) {
						
						// insert single spectrum node
						
						spectrum_node_insert_stmt.setString (1, spectral_set.getNodeDescription()); 
						spectrum_node_insert_stmt.setDouble (2, spectral_set.getConfidenceLevel());
						spectrum_node_insert_stmt.setString (3, spectral_set.getAbsRel());
						spectrum_node_insert_stmt.setInt (4, spectral_set.getUnitId());
						
						byte[] temp_buf;
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						DataOutput dos = new DataOutputStream(baos);
						
						for (int i = 0; i < spectral_set.getUncertaintyVector().length; i++) {
							try {
								dos.writeFloat(spectral_set.getUncertaintyVector()[i]);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}

						temp_buf = baos.toByteArray();
				
						InputStream vector = new ByteArrayInputStream(temp_buf);
							
						spectrum_node_insert_stmt.setBinaryStream(5, vector, spectral_set.getUncertaintyVector().length * 4);
						
						int affectedRows_3= spectrum_node_insert_stmt.executeUpdate();
						ResultSet generatedKeys_3 = spectrum_node_insert_stmt.getGeneratedKeys();
						
						int spectrum_node_id = 0;
					
						while (generatedKeys_3.next()) {
							spectrum_node_id = generatedKeys_3.getInt(1);	
						}
						
						spectrum_node_insert_stmt.close();
						
						// insert row in spectrum_subset for each spectrum_id
						
						for(int i=0; i<spectrum_ids.size(); i++) {
							
							spectrum_subset_insert_stmt.setInt (1, spectrum_subset_id);
							spectrum_subset_insert_stmt.setInt (2, spectrum_node_id);
							spectrum_subset_insert_stmt.setInt (3, spectrum_ids.get(i));
						
							int affectedRows_4= spectrum_subset_insert_stmt.executeUpdate();
						
							ResultSet generatedKeys_4 = spectrum_subset_insert_stmt.getGeneratedKeys();
							
							
						}
						spectrum_subset_insert_stmt.close();

					}
					else {
						for(int i=0; i<spectrum_ids.size(); i++) {
						
							spectrum_node_insert_stmt.setString (1, spectral_set.getNodeDescription()); 
							spectrum_node_insert_stmt.setDouble (2, spectral_set.getConfidenceLevel());
							spectrum_node_insert_stmt.setString (3, spectral_set.getAbsRel());
							spectrum_node_insert_stmt.setInt (4, spectral_set.getUnitId());
						
							byte[] temp_buf;
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							DataOutput dos = new DataOutputStream(baos);
					
							// Each row represents a single spectrum's uncertainty vector
						
							for (int j = 0; j < spectral_set.getUncertaintyVectors()[i].length; j++) {
								try {
									//System.out.println("uncertainty_vector: "+ spectral_set.getUncertaintyVectors()[i][j]);
									dos.writeFloat(spectral_set.getUncertaintyVectors()[i][j]);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}

							temp_buf = baos.toByteArray();
			
							InputStream vector = new ByteArrayInputStream(temp_buf);
						
							spectrum_node_insert_stmt.setBinaryStream(5, vector, spectral_set.getUncertaintyVectors()[i].length*4);
						
							int affectedRows_3= spectrum_node_insert_stmt.executeUpdate();
						
							ResultSet generatedKeys_3 = spectrum_node_insert_stmt.getGeneratedKeys();
							
							int spectrum_node_id = 0;
						
							while (generatedKeys_3.next()) {

								spectrum_node_id = generatedKeys_3.getInt(1);	
						
							}
						
							spectrum_subset_insert_stmt.setInt (1, spectrum_subset_id);
							spectrum_subset_insert_stmt.setInt (2, spectrum_node_id);
							spectrum_subset_insert_stmt.setInt (3, spectrum_ids.get(i));
						
							int affectedRows_4= spectrum_subset_insert_stmt.executeUpdate();
						
							ResultSet generatedKeys_4 = spectrum_subset_insert_stmt.getGeneratedKeys();

						}
					
						spectrum_node_insert_stmt.close();
						spectrum_subset_insert_stmt.close();

						}
					
				}
				
				else {
					
					System.out.println("spectrum_subset_list not empty");
					
					
					
					
				}
				
				
				// Now using list of spectrum_subset_ids to populate spectrum_set_map
				
				String spectrum_set_sql = "INSERT into spectrum_set_map(spectrum_set_id, spectrum_subset_id) " +
						"VALUES (?, ?)";
				
				PreparedStatement pstmt_spectrum_set = SQL.prepareStatement(spectrum_set_sql);
				
				// Looping over all spectrum subset ids
				for(int i = 0; i < spectrum_subset_list.size(); i++)
				{
				    int current_spectrum_subset_id = spectrum_subset_list.get(i);
				    
					pstmt_spectrum_set.setInt(1, spectral_set.getSpectrumSetId());
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
				pstmt_uc_node.setInt(2, spectral_set.getSpectrumSetId());
				pstmt_uc_node.setString(3, spectral_set.getUncertaintyNodeDescription());
				
				// Getting uncertainty node id in return
				
				int affectedRows_5 = pstmt_uc_node.executeUpdate();
				
				ResultSet generatedKeys_5 = pstmt_uc_node.getGeneratedKeys();
					
				while (generatedKeys_5.next()) {

					int uc_node_id = generatedKeys_5.getInt(1);
				
					System.out.println("inserted uncertainty node id: " + uc_node_id);	
					
					spectral_set.setUncertaintyNodeId(uc_node_id);
				
				}
				
			}
				
				// This section applies to both spectrum and instrument nodes
				// Updating uncertainty_node_set: 
				// First we get the node set id associated with the uncertainty set id
				
				String node_set_id_sql = "SELECT node_set_id from uncertainty_set where uncertainty_set_id = ?";

				PreparedStatement pstmt_node_set_id = SQL.prepareStatement(node_set_id_sql);

				pstmt_node_set_id.setInt(1, spectral_set.getUncertaintySetId());
				
				ResultSet uc_set_rs = pstmt_node_set_id.executeQuery();
				
				 while (uc_set_rs.next()) {
				        int node_set_id = uc_set_rs.getInt(1);
				        
				        spectral_set.setNodeSetId(node_set_id);
				              
				 }
				 
				 pstmt_node_set_id.close();
				 
				 // Checking corresponding row in uncertainty_node_set. If null then populating, if not then new node_num!

				 // Breaking this into two steps:
				 
				 String find_last_node_num_sql = "SELECT max(node_num) from uncertainty_node_set where node_set_id = ?";
				
				 PreparedStatement pstmt_find_last_node_num = SQL.prepareStatement(find_last_node_num_sql);
				 
				 pstmt_find_last_node_num.setInt(1, spectral_set.getNodeSetId());
				 
				 ResultSet find_last_node_num_rs = pstmt_find_last_node_num.executeQuery();
				 
				 int last_node_num = 0;
				 
				 while (find_last_node_num_rs.next()) {
					 
					 last_node_num = find_last_node_num_rs.getInt(1);
					 
				 }
				 
				 pstmt_find_last_node_num.close();
				 
				 String find_last_node_id_sql = "SELECT node_id from uncertainty_node_set where node_set_id = ? AND node_num =?";
					
				 PreparedStatement pstmt_find_last_node_id = SQL.prepareStatement(find_last_node_id_sql);
				 
				 pstmt_find_last_node_id.setInt(1, spectral_set.getNodeSetId());
				 pstmt_find_last_node_id.setInt(2, last_node_num);
				 
				 ResultSet find_last_node_id_rs = pstmt_find_last_node_id.executeQuery();
				 
				 int last_node_id = 0;
				 
				 while (find_last_node_id_rs.next()) {
					 
					 last_node_id = find_last_node_id_rs.getInt(1);
					 
				 }
				 
				 pstmt_find_last_node_id.close();
				 
				 
				 int input_node_num;
				 PreparedStatement pstmt_node_set;
				 
				 
				 if (last_node_id == 0) {
					 input_node_num = last_node_num;
					 
					 String node_set_sql = "UPDATE uncertainty_node_set set node_id = ? where node_set_id = ? and node_num = ?";
					 
					 pstmt_node_set = SQL.prepareStatement(node_set_sql, Statement.RETURN_GENERATED_KEYS);
						
					 pstmt_node_set.setInt (1, spectral_set.getUncertaintyNodeId()); 
					 pstmt_node_set.setInt (2, spectral_set.getNodeSetId());
					 pstmt_node_set.setInt (3, input_node_num);
					 
				 }
				 else {
					 input_node_num = last_node_num + 1;
					 
					 String node_set_sql = "INSERT into uncertainty_node_set(node_set_id, node_num, node_id) " + "VALUES (?, ?, ?)";
					 
					 pstmt_node_set = SQL.prepareStatement(node_set_sql, Statement.RETURN_GENERATED_KEYS);
						
					 pstmt_node_set.setInt (1, spectral_set.getNodeSetId()); 
					 pstmt_node_set.setInt (2, input_node_num);
					 pstmt_node_set.setInt (3, spectral_set.getUncertaintyNodeId());
					 
				 }
				 
				 pstmt_node_set.executeUpdate();
				 
				 pstmt_node_set.close();
				 
				 // Updating uncertainty set
				 // Statements to get current adjacency matrix
				 
				 String get_adjacency_matrix_sql = "SELECT adjacency_matrix from uncertainty_set where uncertainty_set_id = ?";
				 
				 PreparedStatement pstmt_get_adjacency_matrix = SQL.prepareStatement(get_adjacency_matrix_sql);
				 
				 pstmt_get_adjacency_matrix.setInt(1, spectral_set.getUncertaintySetId());
				 
				 ResultSet get_adjacency_matrix_rs = pstmt_get_adjacency_matrix.executeQuery();
				 
				 // Statements to update adjacency matrix in mysql
				 
				 String update_adjacency_matrix_sql = "UPDATE uncertainty_set SET adjacency_matrix = ? where uncertainty_set_id = "
							+ spectral_set.getUncertaintySetId();
					
				 PreparedStatement update_adjacency_matrix_pstmt = SQL.prepareStatement(update_adjacency_matrix_sql);	
				 
				 Matrix adjacency_matrix = DenseMatrix.factory.zeros(1, 1);;
				 
			     while (get_adjacency_matrix_rs.next()) {
			    	 
			    	 Blob adjacency_blob = get_adjacency_matrix_rs.getBlob("adjacency_matrix");
		        		
		        		// If adjacency matrix is null then we create a 1x1 matrix
		        		// If adjacency matrix is not null then we add an extra row and column for the new node 
		        		
		        		if (adjacency_blob == null) {
		        			System.out.println("adjacency matrix is empty");
		        			
		        			adjacency_matrix = DenseMatrix.factory.zeros(1, 1);
				            
				            System.out.println("Created new adjacency matrix: "  + adjacency_matrix);

		        		}
			    	 
		        		else {
		        			System.out.println("adjacency matrix is not empty");
		        			
		        			// Now we need to convert the blob back to a matrix
		        			// We use node_num to determine dimensions of the matrix
		        			
		        			int final_matrix_dimension = input_node_num;
		        			int input_matrix_dimension = input_node_num - 1; //We've created a new node num but this is the existing dimension
		        			
		        			System.out.println("Matrix dimension: " + input_matrix_dimension);
		        			
		        			adjacency_matrix = DenseMatrix.factory.zeros(final_matrix_dimension,final_matrix_dimension);
		        			
		        			int input_row_num = 0;
		        			int input_col_num = 0; 
		        			int matrix_i = 0;
		        			
		        			InputStream binstream = adjacency_blob.getBinaryStream();
		    				DataInput dis = new DataInputStream(binstream);

		    				
		    				int dim = binstream.available() / 4;	

		    					for(int i = 0; i < dim; i++)
		    					{
		    							matrix_i = i+1; //we are indexing matrix starting at 1 
		    							int blob_int = dis.readInt();
		    						
		    							// Finding modulus of i / dim 
		    							
		    							int remainder = matrix_i % input_matrix_dimension; 
		    							
		    							if(remainder == 0) {
		    								input_col_num = input_matrix_dimension;
		    								
		    							}
		    							else {
		    								input_col_num = remainder;
		    								
		    							}
		    							
		    							// Once we have col num we can calculate row num
		    							
		    							input_row_num = (matrix_i + (input_matrix_dimension - input_col_num))/input_matrix_dimension; 
		    							
		    							System.out.println("input col num: " + input_col_num);
		    							System.out.println("input row num: " + input_row_num);
		    							
		    							//Changing back to java indexing:
		    							
		    							input_row_num = input_row_num - 1;
		    							input_col_num = input_col_num - 1;
		    							
		    						    adjacency_matrix.setAsInt(blob_int, input_row_num, input_col_num );	
		    					}	
		    					
		    					System.out.println("Retrieved adjacency matrix: " + adjacency_matrix);
		    					
		    					binstream.close();
		    					
		    				
		        		}
		        		

		        		// Uncertainty source pairs contain all the information about node and edge links to this uncertainty node
		        		
		        		ArrayList<UncertaintySourcePair> uncertainty_source_pairs = spectral_set.getUncertaintySourcePairs();
		        				        		
		    			if (uncertainty_source_pairs.size() > 0) {
		    				
		    				System.out.println("One or more uncertainty sources exist");
		    				
		    				System.out.println("The number of uncertainty source pairs is:" + uncertainty_source_pairs.size());
	    				
		    				for(int i = 0; i < uncertainty_source_pairs.size(); i++) {
		    					
		    					// Getting individual values of source pair
		    					
		    					UncertaintySourcePair source_pair = uncertainty_source_pairs.get(i);
		    					
		    					int node_id_of_source = source_pair.getSourceId();
		    						
		    					String description_of_source = source_pair.getSourceLinkDescription();
		    					
		    					String get_node_num_of_source_sql = "SELECT node_num from uncertainty_node_set where node_set_id = ? and node_id = ?";
				   				 
			   				 	PreparedStatement pstmt_get_node_num_of_source = SQL.prepareStatement(get_node_num_of_source_sql);
			    				
		    					
		    					pstmt_get_node_num_of_source.setInt(1, spectral_set.getNodeSetId());
			   				    pstmt_get_node_num_of_source.setInt(2, node_id_of_source);
			   				    
			   				    ResultSet get_node_num_of_source_rs = pstmt_get_node_num_of_source.executeQuery();
			   				 	
			   				    int source_node_num = 0;
			   				    
			   				    while (get_node_num_of_source_rs.next()) {
								 
			   				    	source_node_num = get_node_num_of_source_rs.getInt(1);
			   				    	System.out.println("node_num of source: " + source_node_num);

								 }
							 
			   				    pstmt_get_node_num_of_source.close();
		    					
			   				    int edge_id = 0;
			    				
			   				    // If source link description is null then it's a 'simple' edge and edge value = 1
			    				if (description_of_source == null) {
			    					
			    					edge_id = 1;
			    				
			    				}
			    				
			    				// If source link description is not null: 
		    					// insert new row into uncertainty_edge, edge_id will auto-increment
			    				// Edge value = source link description
			    				
			    				else {
			    					
			    					// Checking first whether description already exists in the database
			    					
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
			    				System.out.println("Coordinates of adjacency matrix change: " + source_node_num + "," + input_node_num);
			   				    
			   				    System.out.println("Dimensions of the adjacency matrix: " + adjacency_matrix.getRowCount() + "," + adjacency_matrix.getColumnCount());
			   				    
			   				    // minus 1 for java indexing
			   				    adjacency_matrix.setAsInt(edge_id, source_node_num-1, input_node_num-1);		   				    
			   				    
			   				    System.out.println("Adjacency matrix after source is added: " + adjacency_matrix);
			   				    
			   				    
		    				}		    				
		   				    
		    			}		    			
		        		
		        		byte[] temp_buf;
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						DataOutput dos = new DataOutputStream(baos);
						
						int count = 0;
						
						for (int row = 0; row < adjacency_matrix.getRowCount(); row++) {
							for (int col = 0; col < adjacency_matrix.getColumnCount(); col++) {
								try {
									System.out.println("dos.writeInt for row: " + row + ", col: "
											+ col + ",value: " + adjacency_matrix.getAsInt(row, col));
									dos.writeInt(adjacency_matrix.getAsInt(row, col));
								} catch (IOException e) {
									e.printStackTrace();
								}
							count++;
							}
						}

						temp_buf = baos.toByteArray();
				
						InputStream vector = new ByteArrayInputStream(temp_buf);
						
						update_adjacency_matrix_pstmt.setBinaryStream(1, vector, count * 4);
						
						update_adjacency_matrix_pstmt.executeUpdate();
	
						update_adjacency_matrix_pstmt.close();
						vector.close();
		        		
			     }
	
					  
				 pstmt_get_adjacency_matrix.close();
			
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		} catch (IOException ex) {
		// TODO Auto-generated catch block
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

	public void insertSpectrumSubset(ArrayList<Integer> uc_spectrum_ids, SpectralSet spectral_set) throws SPECCHIOFactoryException {

		// Re-assigning arraylists
		
		spectral_set.setSpectrumIds(uc_spectrum_ids);
		
		// What do we want to do about node_type? Do we want to check this is 'spectrum'?

		try {
		
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
		
			String spectrum_node_insert_sql = "insert into spectrum_node(node_description, confidence_level, abs_rel, unit_id, u_vector) " +
				" values (?, ?, ?, ?, ?)";
			String spectrum_subset_insert_sql = "insert into spectrum_subset(spectrum_subset_id, spectrum_node_id, spectrum_id) " +
				" values (?, ?, ?)";
			String spectrum_subset_select_max_sql = "select max(spectrum_subset_id) from spectrum_subset;";
			
			// PreparedStatements
		
			PreparedStatement spectrum_node_insert_stmt = SQL.prepareStatement(spectrum_node_insert_sql, Statement.RETURN_GENERATED_KEYS);
			PreparedStatement spectrum_subset_insert_stmt = SQL.prepareStatement(spectrum_subset_insert_sql, Statement.RETURN_GENERATED_KEYS);
			PreparedStatement spectrum_subset_select_max_stmt = SQL.prepareStatement(spectrum_subset_select_max_sql, Statement.RETURN_GENERATED_KEYS);
			
			
			// Find max spectrum_subset_id 
			
			ResultSet subset_max_rs = spectrum_subset_select_max_stmt.executeQuery();
			
			int max_spectrum_subset_id = 0;
			
			while (subset_max_rs.next()) {

				max_spectrum_subset_id = subset_max_rs.getInt(1);	
			
			}
			
			int spectrum_subset_id = max_spectrum_subset_id + 1;
			
			System.out.println("Max spectrum subset id: " + max_spectrum_subset_id);
		
			spectrum_subset_select_max_stmt.close();
			
			// Insert into spectrum_node and spectrum_subset
			// Need to make sure that the code below only inserts one spectrum_subset_id
			
			ArrayList<Integer> spectrum_ids = new ArrayList<Integer>();
			spectrum_ids = spectral_set.getSpectrumIds();

			ArrayList<Integer> spectrum_subset_list = new ArrayList<Integer>();
			
			for(int i=0; i<spectrum_ids.size(); i++) {
				
				spectrum_node_insert_stmt.setString (1, spectral_set.getNodeDescription());
				spectrum_node_insert_stmt.setDouble (2, spectral_set.getConfidenceLevel());
				spectrum_node_insert_stmt.setString (3, spectral_set.getAbsRel());
				spectrum_node_insert_stmt.setInt (4, spectral_set.getUnitId());
				
				byte[] temp_buf;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutput dos = new DataOutputStream(baos);
			
				// Each row represents a single spectrum's uncertainty vector
				
				for (int j = 0; j < spectral_set.getUncertaintyVectors()[i].length; j++) {
					try {
						dos.writeFloat(spectral_set.getUncertaintyVectors()[i][j]);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				temp_buf = baos.toByteArray();
	
				InputStream vector = new ByteArrayInputStream(temp_buf);
				
				spectrum_node_insert_stmt.setBinaryStream(5, vector, spectral_set.getUncertaintyVectors()[i].length*4);
				
				int affectedRows_2= spectrum_node_insert_stmt.executeUpdate();
				
				ResultSet generatedKeys_2 = spectrum_node_insert_stmt.getGeneratedKeys();
				
				int spectrum_node_id = 0;
				
				while (generatedKeys_2.next()) {

					spectrum_node_id = generatedKeys_2.getInt(1);	
				
				}
				
				// Now that we have spectrum node id we can use this to populate spectrum subset
				// For the time being, we have 1 spectrum subset for each spectrum id
				// spectrum_subset_id is currently on auto-increment
				
				spectrum_subset_insert_stmt.setInt (1, spectrum_subset_id);
				spectrum_subset_insert_stmt.setInt (2, spectrum_node_id);
				spectrum_subset_insert_stmt.setInt (3, spectrum_ids.get(i));
				
				int affectedRows_3= spectrum_subset_insert_stmt.executeUpdate();
				
			}
			
			spectrum_node_insert_stmt.close();
			spectrum_subset_insert_stmt.close();
			
			spectral_set.setSpectrumSubsetId(spectrum_subset_id);
			
			System.out.println("executed all spectrum ids");
			
			// Returning spectrum subset id

		
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

	
	
}