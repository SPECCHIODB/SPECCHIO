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
		
		// We're also going to create the first node with null id because that way this node_set_id is 'locked in'
		// Logic fun to follow
		
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
						
						
				// Updating uncertainty_node_set
				
				// First we need to get the node_set_id associated with uncertainty_set...
				// I think this needs to be set up at the same time as a uc set because you have to have a node set with a uc set! 
				
				
				
				
				// Updating uncertainty set
				
				
				
				
				
				
				
				
			}
			
			else if(node_type.equals("spectrum")) {
				
				System.out.println("uncertainty node type: spectrum");
				
				// More steps here
				
				//Get spectrum ids
				     
				ArrayList<Integer> spectrum_ids = spectral_set.spectrum_ids;
				
				// The order for creating spectrum branch of schema: spectrum nodes, spectrum subset, spectrum set map
				
				// Finding length of spectrum_ids and creating a spectrum node for each
				 
				for(int i=0; i<spectrum_ids.size(); i++) {
					
					System.out.println(i);
					
					
				}
				
				
				
			}
			
			// Once node has been created then we check to see if there are any links to other nodes
			
			if (spectral_set.add_uncertainty_source != null) {
				
				// We need an adjacency matrix to exist!!
				
				System.out.println("add_uncertainty_source exists");
				
				
				
				
				
			}
			
			
			if (spectral_set.add_uncertainty_source_by_id != 0) {
				
				
				System.out.println("add_uncertainty_source_by_id exists");
				
				
				
				
			}
			
			
			
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