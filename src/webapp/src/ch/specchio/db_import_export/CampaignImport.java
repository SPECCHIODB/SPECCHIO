package ch.specchio.db_import_export;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.sql.Statement;

import ch.specchio.eav_db.SQL_StatementBuilder;



public class CampaignImport extends DbStructure {
	
	private int user_id;
	
	String source_file;
	BufferedReader d;
	
	public int read_table_cnt = 0;
	
	public CampaignImport(SQL_StatementBuilder SQL, String schema, int user_id)
	{
		super(SQL, schema);
		
		this.user_id = user_id;
	}
	
	public void read_input_stream(InputStream input_stream) throws IOException, SQLException
	{
		
		try {
			
		
		String line;
		
		DataInputStream data_in = new DataInputStream(input_stream);
		d = new BufferedReader(new InputStreamReader(data_in));
		

		d.readLine(); // <campaign>
		line = d.readLine();
		
		// dissect line
		String[] tokens = line.split(" ");
		
		while(!tokens[0].equals("</campaign>"))
		{
			// get table name
			tokens = line.split("\"");
			
			// read a table from input
			DbTable t = read_table(tokens[1]);
			
			if(t!= null && t.isDummy() == false)
			{

				if ("specchio_user".equals(t.name)) {

					// don't insert a new row; just map the old user id to the target user id
					TableField user_id_field = t.all_cols.get("user_id");
					if (user_id_field != null) {
						int old_user_id = (Integer)(user_id_field.value.value);
						KeyLookup kl = get_table("specchio_user").get_keylookup_list("user_id");
						if (kl != null) {
							kl.old_keys.add(old_user_id);
							kl.new_keys.add(user_id);
						}
					} else {
						throw new SQLException("The user_id column is missing from the specchio_user table.");
					}

				} else {

					boolean do_insert = true;

					if (do_insert) // debugging feature ...
					{

						// insert the new row into the target table
						int row_id = t.insert_as_new_row();

						// tidy up tables that refer to the importing user
						if ("campaign".equals(t.name)) {

							// change the owner of the campaign to the importing user
							TableField user_id_field = t.all_cols.get("user_id");
							if (user_id_field != null) {
								user_id_field.setValueFromString(Integer.toString(user_id));
								t.update_row(row_id, user_id_field);
							} else {
								throw new SQLException("The user_id column is missing from the campaign table.");
							}

						} else if ("research_group".equals(t.name) && row_id != 0) {

							// add the importing user to the campaign's research group
							Statement stmt = SQL.createStatement();
							String query = "insert into research_group_members(research_group_id, member_id) " +
									"values(" + Integer.toString(row_id) + "," + Integer.toString(user_id) + ")";
							stmt.executeUpdate(query);
							stmt.close();

						}

					}
				}
			}
			else
			{
				// skipping unknown tables: can happen if an unclean, older dump is loaded ...
			}
			
			line = d.readLine();
			tokens = line.split(" ");
		}
		}
		catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		catch (java.lang.ArrayIndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
		
		
	}
	
	DbTable read_table(String tablename) 
	{
		
		// get table object
		DbTable t = this.get_table(tablename);
		
		if(t == null)
		{
			System.err.println("Unknown table encountered: " + tablename);
		}
		

		
		boolean do_debug = false;
		if(do_debug)
		{
			System.out.println(tablename);
			
			if(tablename.equals("hierarchy_level"))
			{
				boolean gotcha = true;
			}			
		}
		
		// let table read the data from the file
		try {
			t.read_table_data(d);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		return t;
	}

}
