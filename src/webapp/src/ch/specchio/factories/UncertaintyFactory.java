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
import org.ujmp.core.Matrix;
import org.ujmp.core.matrix.DenseMatrix;

import ch.specchio.eav_db.SQL_StatementBuilder;
import ch.specchio.types.Campaign;
import ch.specchio.types.InstrumentNode;
import ch.specchio.types.ResearchGroup;
import ch.specchio.types.SpectralSet;

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
		
		// We're also going to create the first node with id = null because that way this node_set_id is 'locked in'
		// There is a foreign key constraint on 'node_id' but because this column is nullable, we can use null for now
		
		String node_set_query = "insert into uncertainty_node_set(node_set_id, node_num) " +
				"values (?, ?)";
		
		PreparedStatement node_set_pstmt = SQL.prepareStatement(node_set_query, Statement.RETURN_GENERATED_KEYS);
		
		int node_num = 1;
		//int node_id = 0;
		
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
	
	
	
	// getInstrumentNode
	// Retrieve information about instrument node for a given instrument node id
	
	/**
	 * 
	 * Get all associated rows for a given instrument node id including the uncertainty vector
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
		
		String sql_stmt = "SELECT node_type, u_vector, confidence_level, abs_rel, unit_id from instrument_node where instrument_node_id = ?";

		PreparedStatement pstmt = SQL.prepareStatement(sql_stmt);

		pstmt.setInt(1, instrument_node_id);
		
		ResultSet rs = pstmt.executeQuery();

		//Getting information from ResultSet
		
		 while (rs.next()) {
		        String node_type = rs.getString("node_type");
		        double confidence_level = rs.getDouble("confidence_level");
		        String abs_rel = rs.getString("abs_rel");
		        int unit_id = rs.getInt("unit_id");
		        
		        
		        Blob u_vector_blob = rs.getBlob("u_vector");
		       // byte [] bytes = blob.getBytes(1l, (int)blob.length());
		        //for(int i=0; i<bytes.length;i++) {
		         //   System.out.println(Arrays.toString(bytes));
		        //}
		        
		        System.out.println(node_type + ", " + confidence_level + ", " + abs_rel +
		                           ", " + unit_id +  ", " + u_vector_blob);
		        
		        selectedInstrumentNode.setAbsRel(abs_rel);
		        selectedInstrumentNode.setConfidenceLevel(confidence_level);
		        selectedInstrumentNode.setNodeType(node_type);
		        selectedInstrumentNode.setUnitId(unit_id);
		        selectedInstrumentNode.setId(instrument_node_id);
		        
		        //Blob u_vector = rs.getBlob(2);
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
							// don't know what would cause this
							e.printStackTrace();
						}				
					}		
					
					
					selectedInstrumentNode.setUncertaintyVector(u_vector);
					
					
					//selectedInstrumentNode.u_vector = u_vector;
					//selectedInstrumentNode.unit_id = unit_id;
					
					
					


				} catch (IOException e) {
					// dont't know what would cause this
					e.printStackTrace();
				}


				try {
					binstream.close();
				} catch (IOException e) {
					// don't know what would cause this
					e.printStackTrace();
				}
		        
		        
		      }
		
		
		
		
		return selectedInstrumentNode;
		}
		catch (SQLException ex) {
			// bad SQL
			throw new SPECCHIOFactoryException(ex);
		}
	}
	
	
	//insertInstrumentNode
	// void because it assigns an id attribute to the object which can then be found using 'getId' in uncertainty service
	
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
			
		
			// insert the instrument node into the database
			//String query;
			
			//query = "INSERT INTO instrument_node(node_type, confidence_level, abs_rel) VALUES (" +
			//			SQL.quote_string(instrument_node.getNodeType()) + ", 0.07 ," +
			//			SQL.quote_string(instrument_node.getAbsRel()) +
			//			")";
			
			//query = "insert into instrument_node(node_type, confidence_level, abs_rel) values(" +
			//		SQL.conc_values(
			//				instrument_node.getNodeType(),
			//				instrument_node.getConfidenceLevel(),
			//				instrument_node.getAbsRel()
			//		) + ")";
			
			String query = "insert into instrument_node(node_type, confidence_level, abs_rel) " +
						" values (?, ?, ?)";
			
			
			PreparedStatement pstmt = SQL.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
							
			pstmt.setString (1, instrument_node.getNodeType());
			pstmt.setDouble (2, instrument_node.getConfidenceLevel());
			pstmt.setString (3, instrument_node.getAbsRel());
					
			int affectedRows = pstmt.executeUpdate();
			
			System.out.println("pstmt executed");
			
			//ResultSet rs = pstmt.executeQuery("select last_insert_id()");
			//while (rs.next()) {
		//		instrument_node.setId(rs.getInt(1));
		//	}
		
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
			
			
			
			
			
			
			//Statement stmt = SQL.createStatement();
			//stmt.executeUpdate(query);
		
			// get the instrument node id
			//ResultSet rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			//while (rs.next())
		//		instrument_node.setId(rs.getInt(1));
		//	stmt.close();
			
			
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
	
public void insertUncertaintyNode(SpectralSet spectral_set) throws SPECCHIOFactoryException {
		
		try {
			
			// create SQL-building objects
			SQL_StatementBuilder SQL = getStatementBuilder();
			
			String node_type = spectral_set.node_type;
			
			
			// Here we need an if statement to determine whether node is an instrument or spectrum node
			// The first inserts are spectrum/instrument dependent
			// Uncertainty node set and uncertainty set updates are the same process for both
			
			if(node_type.equals("instrument")) {
			
				System.out.println("uncertainty node type: instrument");
				
				// What to do here? Same as instrument node!
				
				String query = "insert into instrument_node(node_type, confidence_level, abs_rel, unit_id) " +
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
				
					System.out.println("inserted id: " + instrument_node_id);
				
					spectral_set.setInstrumentNodeId(instrument_node_id); //Which id? Instrument_node_id
				
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
				
				String uc_node_sql = "INSERT into uncertainty_node(is_spectrum, instrument_node_id) " +
							"VALUES (?, ?)";
				
				PreparedStatement pstmt_uc_node = SQL.prepareStatement(uc_node_sql, Statement.RETURN_GENERATED_KEYS);
				
				pstmt_uc_node.setBoolean (1, is_spectrum); 
				pstmt_uc_node.setInt (2, spectral_set.getInstrumentNodeId());
				
				// Getting uncertainty node id in return
				
				int affectedRows_2 = pstmt_uc_node.executeUpdate();
				
				ResultSet generatedKeys_2 = pstmt_uc_node.getGeneratedKeys();
					
				while (generatedKeys_2.next()) {

					int uc_node_id = generatedKeys_2.getInt(1);
				
					System.out.println("inserted uncertainty node id: " + uc_node_id);	
					
					spectral_set.setUncertaintyNodeId(uc_node_id);
				
				}
				
			}
				
			else if(node_type.equals("spectrum")) {
				
				System.out.println("uncertainty node type: spectrum");
				
				// First creating a new spectrum set
				
				// NB: Spectrum set id is on auto-increment
				
				String spectrum_set_query = "insert into spectrum_set(spectrum_set_description) " +
						" values (?)";
			
			
				PreparedStatement spectrum_set_pstmt = SQL.prepareStatement(spectrum_set_query, Statement.RETURN_GENERATED_KEYS);
						
				spectrum_set_pstmt.setString (1, spectral_set.getSpectrumSetDescription());
				
				int affectedRows = spectrum_set_pstmt.executeUpdate();
				
				ResultSet generatedKeys = spectrum_set_pstmt.getGeneratedKeys();
				
				
				while (generatedKeys.next()) {

					int spectrum_set_id = generatedKeys.getInt(1);
					
					System.out.println("inserted spectrum set with id: " + spectrum_set_id);
					
					spectral_set.setSpectrumSetId(spectrum_set_id);
					
					
				}
				
				spectrum_set_pstmt.close();
				
				
				// More steps here
				
				//Get spectrum ids
				
				ArrayList<Integer> spectrum_ids = new ArrayList<Integer>();
				spectrum_ids = spectral_set.getSpectrumIds();

				ArrayList<Integer> spectrum_subset_list = new ArrayList<Integer>();
				
				
				// The order for creating spectrum branch of schema: spectrum nodes, spectrum subset, spectrum set map
				
				// Finding length of spectrum_ids and creating a spectrum node for each
				
				// Look into batches to update multiple rows in spectrum_node
				
				// Same insert statement for all spectrum ids
				
				String spectrum_node_insert_sql = "insert into spectrum_node(node_type, confidence_level, abs_rel, unit_id, u_vector) " +
						" values (?, ?, ?, ?, ?)";
				
				PreparedStatement spectrum_node_insert_stmt = SQL.prepareStatement(spectrum_node_insert_sql);
				
				System.out.println("size of spectrum_ids: " + spectrum_ids.size());
				
				System.out.println("a list of spectrum ids: " + spectrum_ids);
				
				// Insert statement for spectrum subset 
				
				String spectrum_subset_insert_sql = "insert into spectrum_subset(spectrum_node_id, spectrum_id) " +
						" values (?, ?)";
				
				PreparedStatement spectrum_subset_insert_stmt = SQL.prepareStatement(spectrum_subset_insert_sql);
				
				
				for(int i=0; i<spectrum_ids.size(); i++) {
					
					System.out.println("Running uncertainty node creation for loop for spectrum id: " + spectrum_ids.get(i));
					
					spectrum_node_insert_stmt.setString (1, spectral_set.getNodeDescription()); 
					spectrum_node_insert_stmt.setDouble (2, spectral_set.getConfidenceLevel());
					spectrum_node_insert_stmt.setString (3, spectral_set.getAbsRel());
					spectrum_node_insert_stmt.setInt (4, spectral_set.getUnitId());
					
					// Next we'll look at u_vectors
					
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
					
						System.out.println("inserted uncertainty node id: " + spectrum_node_id);	
					
					}
					
					// Now that we have spectrum node id we can use this to populate spectrum subset
					// For the time being, we have 1 spectrum subset for each spectrum id
					// spectrum_subset_id is currently on auto-increment
					
					spectrum_subset_insert_stmt.setInt (1, spectrum_node_id);
					spectrum_subset_insert_stmt.setInt (2, spectrum_ids.get(i));
					
					int affectedRows_4= spectrum_subset_insert_stmt.executeUpdate();
					
					ResultSet generatedKeys_4 = spectrum_subset_insert_stmt.getGeneratedKeys();
					
					int spectrum_subset_id = 0;
					
					while (generatedKeys_4.next()) {

						spectrum_subset_id = generatedKeys_4.getInt(1);
						System.out.println("inserted spectrum subset id: " + spectrum_subset_id);
					
					}
					
					spectrum_subset_list.add(spectrum_subset_id);
					
					

				}
				
				spectrum_node_insert_stmt.close();
				spectrum_subset_insert_stmt.close();
				
				System.out.println("executed all spectrum ids");
				
				// Once we've looped through each of the spectrum ids 
				// we can use the list of spectrum_subset_ids to populate
				// spectrum_set_map
				
				// We could use batches here?
				
				String spectrum_set_sql = "INSERT into spectrum_set_map(spectrum_set_id, spectrum_subset_id) " +
						"VALUES (?, ?)";
				
				PreparedStatement pstmt_spectrum_set = SQL.prepareStatement(spectrum_set_sql);
				
				// Looping over all spectrum subset ids
				for(int i = 0; i < spectrum_subset_list.size(); i++)
				{
				    int current_spectrum_subset_id = spectrum_subset_list.get(i);
					//System.out.println(spectrum_subset_list.get(i));
				    
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
				
				String uc_node_sql = "INSERT into uncertainty_node(is_spectrum, spectrum_set_id) " +
							"VALUES (?, ?)";
				
				PreparedStatement pstmt_uc_node = SQL.prepareStatement(uc_node_sql, Statement.RETURN_GENERATED_KEYS);
				
				pstmt_uc_node.setBoolean (1, is_spectrum); 
				pstmt_uc_node.setInt(2, spectral_set.getSpectrumSetId());
				
				// Getting uncertainty node id in return
				
				int affectedRows_5 = pstmt_uc_node.executeUpdate();
				
				ResultSet generatedKeys_5 = pstmt_uc_node.getGeneratedKeys();
					
				while (generatedKeys_5.next()) {

					int uc_node_id = generatedKeys_5.getInt(1);
				
					System.out.println("inserted uncertainty node id: " + uc_node_id);	
					
					spectral_set.setUncertaintyNodeId(uc_node_id);
				
				}
				
				System.out.println("finished spectrum-specific entries");
				
				
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

				 String find_last_node_sql = "SELECT max(node_num), node_id from uncertainty_node_set where node_set_id = ?";
				 
				 PreparedStatement pstmt_find_last_node = SQL.prepareStatement(find_last_node_sql);
				 
				 pstmt_find_last_node.setInt(1, spectral_set.getNodeSetId());
				 
				 ResultSet find_last_node_rs = pstmt_find_last_node.executeQuery();
				 
				 int last_node_num = 0;
				 int last_node_id = 0;
				 
				 
				 while (find_last_node_rs.next()) {
					 
					 last_node_num = find_last_node_rs.getInt(1);
					 last_node_id = find_last_node_rs.getInt(2);
					 
					 System.out.println("last_node_num: " + last_node_num);
					 System.out.println("last_node_id: " + last_node_id);
					 
				 }
				 
				 pstmt_find_last_node.close();
				 
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
				 
				 System.out.println("Inserted node_id into uncertainty_node_set");
				 
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
		        		
		        		System.out.println("adjacency_blob: " + adjacency_blob);
		        		
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
		        			// One question is how we know what dimensions it has!
		        			// We can use node_num to determine this!
		        			
		        			int final_matrix_dimension = input_node_num;
		        			int input_matrix_dimension = input_node_num - 1; //We've created a new node num but this is the existing dimension
		        			
		        			System.out.println("Matrix dimension: " + input_matrix_dimension);
		        			System.out.println("input node num: " + input_node_num);
		        			
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
		    							System.out.println("int i has value: "+ blob_int);
		    							
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
		    					
		    					// Now we need to check whether there are any updates to the current matrix
		    					// Maybe we should move the adjacency retrieval to its own function - add to wish list?
		    					// If we are adding a source without id we can use create_instrument_node?
		    					
		    					// Now matrix should be automatically bigger on both sides so all we need to do is put this back into the database
	
		        		}
		        		
		        		// Here's where we can check if node has any links!
		        		// First looking at node_id only
		        		
		    			if (spectral_set.getUncertaintySourceId() != 0) {
		    				
		    				// Here we need logic to determine whether link = simple or has description
		    				
		    				// First checking content of source_link_description
		    				
		    				System.out.println("Edge value checking: " + spectral_set.getSourceLinkDescription());
		    				
		    				// NB: Can use null to determine whether edge value exists 
		    				
		    				
		    				System.out.println("add_uncertainty_source_by_id exists");
		    				
		    				// Testing link
		    				// Here we get a given id of an uncertainty_node
		    				
		    				// Need to find out what index in the matrix this id takes
		    				// This is a source so the train of uncertainty goes from this value to the current id
		    				// At the moment focusing on simple links only ie edge_value = 1
		    				
		    				int node_id_of_source = spectral_set.getUncertaintySourceId();
		    				
		    				// Looking in uncertainty_node_set for the node_id using the current node_set_id too
		    				// We use the current uncertainty set because the node has to reside within the current set
		    				
		    				String get_node_num_of_source_sql = "SELECT node_num from uncertainty_node_set where node_set_id = ? and node_id = ?";
		   				 
		   				 	PreparedStatement pstmt_get_node_num_of_source = SQL.prepareStatement(get_node_num_of_source_sql);
		   				 
		    				// Setting the values of the select statement

		   				    pstmt_get_node_num_of_source.setInt(1, spectral_set.getNodeSetId());
		   				    pstmt_get_node_num_of_source.setInt(2, node_id_of_source);
		   				    
		   				    ResultSet get_node_num_of_source_rs = pstmt_get_node_num_of_source.executeQuery();
		   				 	
		   				    int source_node_num = 0;
		   				    
		   				    while (get_node_num_of_source_rs.next()) {
							 
		   				    	source_node_num = get_node_num_of_source_rs.getInt(1);
		   				    	System.out.println("node_num of source: " + source_node_num);

							 }
						 
		   				    pstmt_get_node_num_of_source.close();
		    				
		   				    // We could create another spectral set to store this data?
		   				    
		   				    // Now finding using our index to change the adjacency matrix
		   				    
		   				    // If source is node_num 1 and our node to insert is node_num 2 
		   				    // Column represents the node which is receiving the source ie 2.
		   				    // Row represents the source node which here would be 1.
		   				    
		   				    // edge value for now is a simple link ie. 1
		   				    
		   				    int edge_value = 1;
		   				    
		   				    System.out.println("Coordinates of adjacency matrix change: " + source_node_num + "," + input_node_num);
		   				    
		   				    System.out.println("Dimensions of the adjacency matrix: " + adjacency_matrix.getRowCount() + "," + adjacency_matrix.getColumnCount());
		   				    
		   				    // minus 1 for java indexing
		   				    adjacency_matrix.setAsInt(edge_value, source_node_num-1, input_node_num-1);		   				    
		   				    
		   				    System.out.println("Adjacency matrix after source is added: " + adjacency_matrix);
		   				    
		   				    
		    			}

		        		
		        		byte[] temp_buf;
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						DataOutput dos = new DataOutputStream(baos);
						
						int count = 0;
						
						for (int row = 0; row < adjacency_matrix.getRowCount(); row++) {
							for (int col = 0; col < adjacency_matrix.getColumnCount(); col++) {
								try {
									dos.writeFloat(adjacency_matrix.getAsInt(row, col));
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
						System.out.println("Inserted new adjacency matrix after update");
		        		
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
	
	
	
	
	
}