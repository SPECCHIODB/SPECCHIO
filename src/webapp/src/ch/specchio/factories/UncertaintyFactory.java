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
					
			//pstmt.setString (1, "test_node_pstmt");
			//pstmt.setDouble (2, 0.07);
			//pstmt.setString (3, "rel");
			
			pstmt.setString (1, instrument_node.getNodeType());
			pstmt.setDouble (2, instrument_node.getConfidenceLevel());
			//pstmt.setDouble (2, instrument_node.getConfidenceLevel());
			pstmt.setString (3, instrument_node.getAbsRel());
			
			System.out.println(query);
			
			System.out.println(pstmt);
			
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
					
				pstmt.setString (1, spectral_set.getNodeDescription()); //This needs renaming
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
				
				pstmt.setBoolean (1, is_spectrum); 
				pstmt.setInt (2, spectral_set.getInstrumentNodeId());
				
				// Getting uncertainty node id in return
				
				int affectedRows_2 = pstmt_uc_node.executeUpdate();
				
				ResultSet generatedKeys_2 = pstmt_uc_node.getGeneratedKeys();
					
				while (generatedKeys_2.next()) {

					int uc_node_id = generatedKeys_2.getInt(1);
				
					System.out.println("inserted uncertainty node id: " + uc_node_id);			
				
				}
						
				
				
			}
			
			else if(node_type.equals("spectrum")) {
				
				System.out.println("uncertainty node type: spectrum");
				
				// More steps here
				
				//Get spectrum ids
				     
				ArrayList<Integer> spectrum_ids = spectral_set.spectrum_ids;
				
				
				
				
				
				
				
				
			}
			
			// Once node has been created then we check to see if there are any links to other nodes
			
			if (spectral_set.add_uncertainty_source != null) {
				
				// We need an adjacency matrix to exist!!
				
				System.out.println("add_uncertainty_source exists");
				
				
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