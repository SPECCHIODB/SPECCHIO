package ch.specchio.factories;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.ListIterator;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletInputStream;
import javax.sql.DataSource;

import ch.specchio.eav_db.Attributes;
import ch.specchio.eav_db.EAVDBServices;
import ch.specchio.eav_db.SQL_StatementBuilder;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.MetaSpatialPoint;
import ch.specchio.types.User;
import ch.specchio.types.attribute;


/**
 * Base class for all factories. This class encapsulates the database
 * information common to all factories.
 */
public class SPECCHIOFactory {
	
	/** the suffix used for the name of the temporary database */
	private static final String TEMP_DATABASE_SUFFIX = "_temp";
	
	/** the data source name */
	protected String datasource_name = null;	
	
	/** the data source */
//	private DataSource ds = null;
	
	/** the database connection */
	private Connection conn = null;
	
	/** did this object create its own database connection? */
	private boolean my_conn = false;
	
	/** the data cache */
	private static Hashtable<String, DataCache> caches = new Hashtable<String, DataCache>();	
	
	/** attributes server */
	private static Hashtable<String, Attributes> attrs = new Hashtable<String, Attributes>();
	
	/** eav services */
	private EAVDBServices eav = null;
	
	/** SQL statement builder */
	private SQL_StatementBuilder sql = null;
	
	/** database name */
	private String databaseName = null;
	
	/** user part of the database user string */
	private String databaseUserName = null;
	
	/** database version */
	private Double databaseVersion = null;
	
	/** host part of the database user string */
	private String databaseUserHost = null;

	/** is the user an administrator? */
	private boolean is_admin;
	
	
	public boolean Is_admin() {
		return is_admin;
	}


	/**
	 * Construct a factory using the default connection to the database.
	 * 
	 * @throws SPECCHIOFactoryException	could not establish initial context
	 */
	public SPECCHIOFactory(String ds_name) throws SPECCHIOFactoryException {
		
		try {
			// set up database connection
			datasource_name = ds_name;
			init(getDataSource(ds_name).getConnection());
			this.my_conn = true;
		}
		catch (SQLException ex) {
			// bad username or password
			throw new SecurityException(ex);
		}
		
	}
	
	
	/**
	 * Construct a factory using a specific user's connection to the database.
	 * 
	 * @param db_user		database account user name
	 * @param db_password	database account password
	 * @param is_admin	is the user an administrator? 
	 * 
	 * @throws SPECCHIOFactoryException	could not establish initial context
	 */
	public SPECCHIOFactory(String db_user, String db_password, String ds_name, boolean is_admin) throws SPECCHIOFactoryException {
		
		try {
			// set up database connection
			datasource_name = ds_name;			
			init(getDataSource(ds_name).getConnection(db_user, db_password));
			this.my_conn = true;
			this.is_admin = is_admin;
			String table_name = (is_admin)? "eav" : "eav_view";
			this.eav.set_eav_view_name(table_name);			
			this.eav.setSPECCHIOFactory(this);
		}
		catch (SQLException ex) {
			// bad username or password
			throw new SPECCHIOFactoryException(ex);
		}
		
	}
	
	
	/**
	 * Copy constructor. Construct a new factory that uses the same database connection
	 * as an existing factory.
	 * 
	 * @param factory	the existing factory
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public SPECCHIOFactory(SPECCHIOFactory factory) throws SPECCHIOFactoryException {
		
		datasource_name = factory.getSourceName();
		init(factory.getConnection());
		this.my_conn = false;
		
	}
	
	
	/**
	 * Configure the attributes cache.
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private synchronized void configureCaches() throws SPECCHIOFactoryException {
		
		if (!SPECCHIOFactory.attrs.containsKey(datasource_name) || !SPECCHIOFactory.caches.containsKey(datasource_name)) {
			// need to populate at least one cache
			
			System.out.println("Configuring caches for " + datasource_name);
			
			try {
				// get an SQL statement builder using the default connection to the database
				Connection conn = getDataSource(datasource_name).getConnection();
				SQL_StatementBuilder sql = new SQL_StatementBuilder(conn);
			
//				if (SPECCHIOFactory.attr == null) {
//					// construct and populate the attributes object
//					SPECCHIOFactory.attr = new Attributes(sql);
//				}
				Attributes attr = new Attributes(sql);
				SPECCHIOFactory.attrs.put(datasource_name, attr);
				
				System.out.println("Number of cached attributes " + attr.getAttributes().size());
				
//				if (SPECCHIOFactory.cache == null) {
//					// construct and populate the data cache
//					SPECCHIOFactory.cache = new DataCache(sql, datasource_name);
//				}
				SPECCHIOFactory.caches.put(datasource_name, new DataCache(sql, datasource_name));
				
				// close the connection
				conn.close();
			}
			catch (SQLException ex) {
				// database error
				throw new SPECCHIOFactoryException(ex);
			}
		
		}
		
	}
	
	
	/**
	 * Configure the member variables that store the database user information.
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	private void configureDatabaseUserInfo() throws SPECCHIOFactoryException {
		
		try {
			Statement stmt = sql.createStatement();
			ResultSet rs = stmt.executeQuery("select current_user()");
			while (rs.next()) {
				String userString = rs.getString(1);
				String parts[] = userString.split("@");
				databaseUserName = parts[0];
				databaseUserHost = parts[1];
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
	}

	
	
	/**
	 * Initialise the factory. This is a helper method for the constructors.
	 * 
	 * @param conn	the database connection to use
	 * 
	 * @throws SPECCHIOFactoryException database error
	 */
	private void init(Connection conn) throws SPECCHIOFactoryException {
		
		// make sure static members are configured
		configureCaches();
		
		// save the database connection
		this.conn = conn;
		
		// create a statement builder for this connection
		this.sql = new SQL_StatementBuilder(conn);
		
		// set up EAV services
		this.eav = new EAVDBServices(getStatementBuilder(), getAttributes(), getDatabaseUserName());
		this.eav.set_primary_x_eav_tablename(MetaParameter.SPECTRUM_LEVEL, "spectrum_x_eav", "spectrum_x_eav_view", "spectrum_id", "spectrum");
		this.eav.set_primary_x_eav_tablename(MetaParameter.HIERARCHY_LEVEL, "hierarchy_x_eav", "hierarchy_x_eav_view", "hierarchy_level_id", "hierarchy_level");
		
	}
	
	
	/**
	 * Dispose of the factory.
	 */
	public void dispose() {
		
		try {
			// release the database connection
			if (this.my_conn) {
				this.conn.close();
			}
		}
		catch (SQLException ex) {
			// don't know what might cause this
		}
		
	}
	
	
	/**
	 * Garbage collection.
	 */
	@Override
	public void finalize() {
		
		this.dispose();
		
	}
	
	
	/**
	 * Get a reference to the attributes service.
	 * 
	 * @return a reference to the Attributes object
	 */
	public Attributes getAttributes() {
		
		return SPECCHIOFactory.attrs.get(this.getSourceName());
		
	}
	
	
	/**
	 * Get a connection to the database.
	 */
	public Connection getConnection() {
		
		return conn;
		
	}
	
	
	/**
	 * Get a reference to the data cache, creating the cache if it does not already exist.
	 * 
	 * @return a reference to the unique instance of the data cache.
	 * 
	 * @throws SPECCHIOFactoryException	could not create the cache
	 */
	public DataCache getDataCache() throws SPECCHIOFactoryException {
		
		//System.out.println("Get Cache for data source = " + this.getSourceName());
		//DataCache cache = SPECCHIOFactory.caches.get(this.getSourceName());
		//System.out.println("Source of cache:" + cache.datasource_name);
		return SPECCHIOFactory.caches.get(this.getSourceName());
		
	}
	
	
	/**
	 * Get a reference to the data source.
	 * 
	 * @return a reference to the data source for this factory
	 * 
	 * @throws SPECCHIOFactoryException	could not establish the initial context
	 */
	private static DataSource getDataSource(String DataSource) throws SPECCHIOFactoryException {
		
		DataSource ds;
			//if (ds == null) {
			try {
				Context ctx = new InitialContext();
				ds = (DataSource)ctx.lookup(DataSource); // "jdbc/specchio_test"
			}
			catch (NamingException ex) {
				// not sure what causes this
				throw new SPECCHIOFactoryException(ex);
			}
		//}
		
		return ds;
		
	}
	
	
	/**
	 * Get a reference to the EAV services.
	 * 
	 * @return a reference to the EAVDBServices object
	 */
	public EAVDBServices getEavServices() {
		
		return eav;
		
	}
	
	
	/**
	 * Get the name of the database schema in use.
	 * 
	 * There is surely a better way of doing this, but the following code is
	 * the only suggestion that I could find.
	 * 
	 * @return the name of the database
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public String getDatabaseName() throws SPECCHIOFactoryException {
		
		if (databaseName == null) {
			try {
				Statement stmt = sql.createStatement();
				ResultSet rs = stmt.executeQuery("select schema()");
				while (rs.next()) {
					this.databaseName = rs.getString(1);
				}
				rs.close();
				stmt.close();
			}
			catch (SQLException ex) {
				// database error
				throw new SPECCHIOFactoryException(ex);
			}
		}
		
		return databaseName;
		
	}
	
	
	/**
	 * Get the hostname part of the user string under which we are connected to the database.
	 * 
	 * @return the part of the user string following the "@"
	 * 
	 * @throws SQLException	database error
	 */
	public String getDatabaseUserHost() throws SPECCHIOFactoryException {
		
		// make sure the database user variables are configured
		if (databaseUserHost == null) {
			configureDatabaseUserInfo();
		}
		
		return databaseUserHost;
		
	}
	
	
	/**
	 * Get the username part of the user string under which are connected to the database.
	 * 
	 * @return the part of the user string before the "@"
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public String getDatabaseUserName() throws SPECCHIOFactoryException {
		
		// make sure the database user variables are configured
		if (databaseUserName == null) {
			configureDatabaseUserInfo();
		}
		
		return databaseUserName;
		
	}
	
	
	/**
	 * Get the version of the database schema in use.
	 * 
	 * @return the name of the database
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public Double getDatabaseVersion() throws SPECCHIOFactoryException {
		
		if (databaseVersion == null) {
			try {
				Statement stmt = sql.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT version FROM schema_info order by version desc limit 1");
				while (rs.next()) {
					this.databaseVersion = rs.getDouble(1);
				}
				rs.close();
				stmt.close();
			}
			catch (SQLException ex) {
				// database error
				throw new SPECCHIOFactoryException(ex);
			}
		}
		
		return databaseVersion;
		
	}	
	
	/**
	 * Get the datasource name of the current source identifying the database to be used.
	 * 
	 * @return datasource name
	 * 
	 */
	public String getSourceName() {
		
		return datasource_name;		
	}	
	
	
	/**
	 * Get the maximum size of a database query.
	 * 
	 * @return the value of max_allowed_packet
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public long getMaximumQuerySize() throws SPECCHIOFactoryException {
		
		long max = 0;
		
		try {
			Statement stmt = getStatementBuilder().createStatement();
			ResultSet rs = stmt.executeQuery("show variables like " + getStatementBuilder().quote_string("max_allowed_packet"));
			while (rs.next()) {
				max = rs.getLong(2);
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException ex) {
			// database error
			throw new SPECCHIOFactoryException(ex);
		}
		
		return max;
		
	}
	
	
	/**
	 * Get a reference to the statement builder for this factory.
	 * 
	 * @returns a reference to the SQL_StatementBuilder object
	 */
	public SQL_StatementBuilder getStatementBuilder() {
		
		return sql;
		
	}
	
	
	/**
	 * Get the name of the database in which temporary tables can be created by
	 * ordinary users.
	 *
	 * @return the name of the temporary database
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public String getTempDatabaseName() throws SPECCHIOFactoryException {
		
		return getDatabaseName() + TEMP_DATABASE_SUFFIX;
		
	}
	
	/**
	 * Reloads the caches.
	 * 
	 * @throws SPECCHIOFactoryException	database error
	 */
	public synchronized void reloadCaches() throws SPECCHIOFactoryException {

		caches = new Hashtable<String, DataCache>();
		configureCaches();
				
		// set up EAV services to ensure they have new attribute etc info
		this.eav = new EAVDBServices(getStatementBuilder(), getAttributes(), getDatabaseUserName());
		this.eav.set_primary_x_eav_tablename(MetaParameter.SPECTRUM_LEVEL, "spectrum_x_eav", "spectrum_x_eav_view", "spectrum_id", "spectrum");
		this.eav.set_primary_x_eav_tablename(MetaParameter.HIERARCHY_LEVEL, "hierarchy_x_eav", "hierarchy_x_eav_view", "hierarchy_level_id", "hierarchy_level");
		String table_name = (is_admin)? "eav" : "eav_view";
		this.eav.set_eav_view_name(table_name);
		
		
	}


	public void dbUpgrade(double version, ServletInputStream inputStream) {
		
		
		boolean not_eof = true;
		String line;	
		String temp = "";		
		
		MetadataFactory mf = new MetadataFactory(this);
		
		// read statements from upgrade script file		
		try {
		
		DataInputStream data_in = new DataInputStream(inputStream);			
			
		BufferedReader d = new BufferedReader(new InputStreamReader(data_in));

		while(not_eof)
		{
			not_eof = skip_comments_and_empty_lines(d);
				
			if(not_eof)
			{
				line = d.readLine();					
				
				// keep concatenating lines while no semicolon is found and not end of file
				while(line !=null && !line.contains(";") && temp != null)
				{
					temp = d.readLine();	
					if (temp != null)
						line = line + " " + temp;
				}
				temp = "";	
				
				if(line == null || temp == null)
					break;

				// remove tabs
				line.replace("\t", "");				
					
				// process line
				process_line(line);	
			}
		}
			
		d.close();						
		data_in.close ();		
		
		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		// -----------------
		// -----------------
		// Version dependent
		// Java upgrade part
		// -----------------
		// -----------------
		
		if (version == 3.3) {
			
			this.reloadCaches();
			
			// store existing point data in new Spatial Position
			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt;
			try {
				stmt = SQL.createStatement();
				
				String query = "select campaign_id, name, user_id from campaign";
				
				ArrayList<Integer> campaign_ids = new ArrayList<Integer>();
				ArrayList<Integer> user_ids = new ArrayList<Integer>();
				ArrayList<String> campaign_names = new ArrayList<String>();

				ResultSet rs = stmt.executeQuery(query);


				while (rs.next()) {

					campaign_ids.add(rs.getInt(1));
					campaign_names.add(rs.getString(2));
					user_ids.add(rs.getInt(3));
				}

				rs.close();
				stmt.close();
				
				
				// loop over all campaigns
				ListIterator<Integer> li = campaign_ids.listIterator();
				ListIterator<String> namli = campaign_names.listIterator();
				
				int lat_attr_id = mf.getAttributes().get_attribute_id("Latitude");
				int lon_attr_id = mf.getAttributes().get_attribute_id("Longitude");
				
				attribute point_attr = mf.getAttributes().get_attribute_info(mf.getAttributes().get_attribute_id("Spatial Position"));
				
				class lat_lon_container
				{
					double lat;
					double lon;
					int spectrum_id;
				}
				
				ArrayList<lat_lon_container> coords = new ArrayList<lat_lon_container>();
				
				while(li.hasNext())
				{
					coords.clear();
					int campaign_id = li.next();
					String cname = namli.next();
					
					System.out.println("===================================");
					System.out.println("Upgrading Campaign spatial metadata: " + cname);
					System.out.println("===================================");
					
					getEavServices().clear_redundancy_list();
					
					stmt = SQL.createStatement();
					
					
					// get spatial data
					query = "select double_val, sxe.spectrum_id, attribute_id from eav, spectrum_x_eav sxe where eav.eav_id = sxe.eav_id and attribute_id in " +
							"(select attribute_id from attribute where name = 'Latitude' OR name = 'Longitude') " + "and eav.campaign_id = " + campaign_id +
							" order by spectrum_id";
					
					
					rs = stmt.executeQuery(query);

					int i=1;
					
					while (rs.next()) {

						lat_lon_container coord = new lat_lon_container();
						i=1;
						double val = rs.getDouble(i++);
						coord.spectrum_id = rs.getInt(i++);
						int attribute_id = rs.getInt(i++);						

						if(attribute_id == lat_attr_id) coord.lat = val; else coord.lon = val;
						
						rs.next();
						i=1;
						val = rs.getDouble(i++);
						int spectrum_id = rs.getInt(i++);
						attribute_id = rs.getInt(i++);	
						
						if(attribute_id == lat_attr_id) coord.lat = val; else coord.lon = val;

						if(coord.spectrum_id == spectrum_id)
						{
							coords.add(coord);
						}
						else
						{
							System.out.println("Lat/Lon not available consistenly for spectrum_id = " + coord.spectrum_id);
						}	


					}
					
					
					rs.close();
					stmt.close();
					
					// create point entries for the coords
					ListIterator<lat_lon_container> it = coords.listIterator();
					
					System.out.println("Updating " + coords.size() + " coordinates.");
					
					while(it.hasNext())
					{
						lat_lon_container coord = it.next();
						
						MetaSpatialPoint mp = (MetaSpatialPoint) MetaParameter.newInstance(point_attr, coord.lat + " " + coord.lon);
						
						// insert into eav and link with spectrum
						int eav_id = getEavServices().insert_metaparameter_into_db(campaign_id, mp, true, this.Is_admin());
						getEavServices().insert_primary_x_eav(MetaParameter.SPECTRUM_LEVEL, coord.spectrum_id, eav_id);

					}

				}
				

				stmt = SQL.createStatement();
				// remove old lat and lon metaparameters and attributes
				query = "delete from spectrum_x_eav where eav_id in (select eav_id from eav where attribute_id in (select attribute_id from attribute where name = 'Latitude' OR name = 'Longitude'))";
				stmt.executeUpdate(query);
				
				query = "delete from eav where attribute_id in (select attribute_id from attribute where name = 'Latitude' OR name = 'Longitude')";
				stmt.executeUpdate(query);
				
				query = "delete from attribute where name = 'Latitude' OR name = 'Longitude'";
				stmt.executeUpdate(query);
				
				stmt.close();
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MetaParameterFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		if (version == 3.32) {	
			
			// new rights for existing users on new table and view


			System.out.println("===================================");
			System.out.println("Upgrading user rights for new hierarchy-eav table and view: ");
			System.out.println("===================================");

			SQL_StatementBuilder SQL = getStatementBuilder();
			Statement stmt;
			try {
				
				// get users
				UserFactory uf = new UserFactory(this);
				User[] users = uf.getUsers();
				
				stmt = SQL.createStatement();		
				
				for(int i=0;i<users.length;i++)
				{
					User user = users[i];
					
					String username = user.getUsername();
					
					if (!username.equals("sdb_admin"))
					{

						String sql = "GRANT SELECT, INSERT, UPDATE, DELETE ON " + this.getDatabaseName() + ".hierarchy_x_eav_view  TO '" + username + "'@'localhost'";
						stmt.execute(sql);

						sql = "GRANT SELECT, INSERT, UPDATE, DELETE ON " + this.getDatabaseName() + ".campaign_x_eav_view  TO '" + username + "'@'localhost'";
						stmt.execute(sql);
						
						sql = "GRANT SELECT ON " + this.getDatabaseName() + ".hierarchy_x_eav  TO '" + username + "'@'localhost'";
						stmt.execute(sql);

						sql = "GRANT SELECT ON " + this.getDatabaseName() + ".campaign_x_eav  TO '" + username + "'@'localhost'";
						stmt.execute(sql);

					}
					
					stmt.execute("flush privileges");
					
				}


			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		// reload system table caches to pick up any new entries
		this.reloadCaches();
		
	}

	private void process_line(String line) 
	{
		if(line.length() > 0)
		{

			// change to current schema if schema is coded in current statement
			if (line.contains("`specchio`"))
			{
				line = line.replaceAll("`specchio`", "`" + getDatabaseName() +"`");
			}

			//	System.out.println(line);

			String[] statements = 
				{
					line
				};

			execute_statements(statements);

		}
	}
	
	void execute_statements(String[] statements)
	{
		try {		
			Statement stmt = getStatementBuilder().createStatement();
			for (int i= 0; i < statements.length; i++)
			{
				System.out.println(statements[i]);
				stmt.executeUpdate(statements[i]);
			}
			stmt.close();
		} catch (SQLException e) {

			int err = e.getErrorCode();
			//error_cnt++;

			if(err == 0)
			{
				// data truncation due to float to decimal conversion
				System.out.println(e.getMessage());
			}
			else
			{
				e.printStackTrace();
			}
		}	

	}	
	

	private boolean skip_comments_and_empty_lines(BufferedReader d) throws IOException
	{
		int ret;
		boolean marked = false;
		d.mark(150);
		ret = d.read();

		if (ret == 10)
		{
			d.mark(150);
			marked = true;
		}
		else
		{
			while(ret != -1 && (char)ret == '-')
			{
				d.readLine(); // read whole line
				d.mark(150);
				marked = true;
				ret = d.read(); // read next char (new line)
			}
		}

		// return to last mark (start of the next valid line)
		d.reset();	 

		if (marked)
		{
			skip_comments_and_empty_lines(d); // recursion
		}

		if(ret == -1)
			return false; // eof
		else
			return true; // not eof
	}	

	

}
