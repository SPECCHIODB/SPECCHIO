package ch.specchio.client;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.prefs.BackingStoreException;

import ch.specchio.gui.SPECCHIOApplication;

public class SPECCHIOClientFactory {
	
	/** singleton instance of the web client factory */
	private static SPECCHIOClientFactory instance = null;
	
	/** the legacy configuration file name */
	private static final File legacyConfigFile = new File(SPECCHIOClientFactory.getApplicationFilepath("db_config.txt"));
	
	/** the list of known servers */
	private List<SPECCHIOServerDescriptor> apps;
	
	/** the current client object */
	private SPECCHIOClient current_client;
	
	private String default_trust_store;
	private String default_trust_store_password = null;

	/**
	 * Constructor.
	 * 
	 * @throws SPECCHIOClientException	the configuration data is invalid or inaccessible
	 */
	private SPECCHIOClientFactory() throws SPECCHIOClientException {
		
		// initialise the server descriptor list
		apps = new LinkedList<SPECCHIOServerDescriptor>();
		
		// set up SSL trust store
		
        Path location = null;
        
        String locationProperty = System.getProperty("javax.net.ssl.trustStore");
        if ((null != locationProperty) && (locationProperty.length() > 0)) {
            Path p = Paths.get(locationProperty);
            File f = p.toFile();
            if (f.exists() && f.isFile() && f.canRead()) {
                location = p;
            }
        } else {
            String javaHome = System.getProperty("java.home");
            location = Paths.get(javaHome, "lib", "security", "jssecacerts");
            if (!location.toFile().exists()) {
                location = Paths.get(javaHome, "lib", "security", "cacerts");
            }
        }
        
        default_trust_store = location.toString();

        String passwordProperty = System.getProperty("javax.net.ssl.trustStorePassword");
        if ((null != passwordProperty) && (passwordProperty.length() > 0)) {
        	default_trust_store_password = passwordProperty;
        } else {
        	default_trust_store_password = "changeit";
        }		
		
		
		loadServerDescriptors();
		
	}
	
	
	private void loadServerDescriptors() throws SPECCHIOClientException
	{
				
		// load server descriptors from the legacy db_config.txt file
		if (legacyConfigFile.exists()) {
			try {
				System.out.println("Loading server descriptors from the legacy db_config.txt file");
				SPECCHIOServerDescriptorStore s = new SPECCHIOServerDescriptorLegacyStore(legacyConfigFile);
				Iterator<SPECCHIOServerDescriptor> iter = s.getIterator();
				while (iter.hasNext()) {
					apps.add(iter.next());
					System.out.print(".");
				}
			}
			catch (IOException ex) {
				// read error; re-throw as a SPECCHIO client exception
				throw new SPECCHIOClientException(ex);
			}
		}		
		else // load server descriptors from the preferences store
		{
			try {
				SPECCHIOServerDescriptorStore s = new SPECCHIOServerDescriptorPreferencesStore();
				Iterator<SPECCHIOServerDescriptor> iter = s.getIterator();
				while (iter.hasNext()) {
					apps.add(iter.next());
				}
			}
			catch (BackingStoreException ex) {
				// the backing store failed; re-throw as a SPECCHIO client exception
				throw new SPECCHIOClientException(ex);
			}	
		}				
		
	}
	
	
	/**
	 * Add the configuration for an account to the configuration file.
	 * 
	 * @param d		the descriptor of the server on which the new account exists
	 * 
	 * @throws IOException file error
	 * @throws SPECCHIOClientException invalid configuration data
	 */
	public void addAccountConfiguration(SPECCHIOServerDescriptor d) throws IOException, SPECCHIOClientException {
		
		try {
			// all new accounts are saved to the Java preferences store
			SPECCHIOServerDescriptorStore store = new SPECCHIOServerDescriptorPreferencesStore();
			store.addServerDescriptor(d);
			
			if (legacyConfigFile.exists()) {
				SPECCHIOServerDescriptorStore s = new SPECCHIOServerDescriptorLegacyStore(legacyConfigFile);
				s.addServerDescriptor(d);
			}
		}
		catch (BackingStoreException ex) {
			// the backing store failed; re-throw as an IOException
			throw new IOException(ex);
		}
		
		// update the internal list of server descriptors
		apps.add(d);
		
	}
	
	
	/**
	 * update the configuration of an existing account.
	 * 
	 * @param d		the descriptor of the server on which the new account exists
	 * 
	 * @throws IOException file error
	 * @throws SPECCHIOClientException invalid configuration data
	 */
	public void updateAccountConfiguration(SPECCHIOServerDescriptor d) throws IOException, SPECCHIOClientException {
		
		try {
			// all new accounts are saved to the Java preferences store
			SPECCHIOServerDescriptorStore store = new SPECCHIOServerDescriptorPreferencesStore();			
			store.updateServerDescriptor(d);		
			
			if (legacyConfigFile.exists()) {
				SPECCHIOServerDescriptorStore s = new SPECCHIOServerDescriptorLegacyStore(legacyConfigFile);
				s.updateServerDescriptor(d);
			}			
			
		}
		catch (BackingStoreException ex) {
			// the backing store failed; re-throw as an IOException
			throw new IOException(ex);
		}
		
		// update the internal list of server descriptors
		apps.add(d);
		
	}	
	
	
	public static String getApplicationFilepath(String name)
	{
		// check if the file is found in the current directory
		File file = new File(name);
		if (file.isFile())
		{
			return file.getPath();
		}

		try {
			File app_dir = new File(SPECCHIOApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			file = new File(app_dir.getParent() + File.separator + name);
			return file.getPath();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		return file.getPath();
	}
	
	/**
	 * Connect to a SPECCHIO web application server.
	 * 
	 * @param app		the descriptor of the SPECCHIO web application to which to connect
	 *
	 * @return a web client object connected to the new application server
	 * 
	 * @throws SPECCHIOClientConnection	could not create the client
	 */
	public SPECCHIOClient createClient(SPECCHIOServerDescriptor app) throws SPECCHIOClientException {
		
		
		// trust store settings (depending on descriptor configuration)
		if(app.usesDefaultTrustStore())
		{
			System.setProperty("javax.net.ssl.trustStore", this.default_trust_store);
			System.setProperty("javax.net.ssl.trustStorePassword", this.default_trust_store_password);				
		}
		else
		{
			System.setProperty("javax.net.ssl.trustStore", SPECCHIOClientFactory.getApplicationFilepath("specchio.keystore"));
			System.setProperty("javax.net.ssl.trustStorePassword", "specchio");			
		}
		
		current_client = new SPECCHIOClientCache(app.createClient());
		((SPECCHIOClientCache) current_client).setServerDescriptor(app);
		return current_client;
	
	}
	
	
	/**
	 * Return the single instance of the SPECCHIO web client factory.
	 * 
	 * @return the single instance of the SPECCHIO web client factory
	 * 
	 * @throws SPECCHIOWebClientException	the configuration file is invalid
	 */
	public static SPECCHIOClientFactory getInstance() throws SPECCHIOClientException {
		
		if (instance == null) {
			instance = new SPECCHIOClientFactory();
		}
		
		return instance;
		
	}
	
	
	/**
	 * Return the list of known web application servers.
	 *
	 * @return the list of known web application servers
	 */
	public List<SPECCHIOServerDescriptor> getAllServerDescriptors() {
		
		return apps;
		
	}


	public void reloadDBConfigFile() {
		// initialise the server descriptor list
		apps = new LinkedList<SPECCHIOServerDescriptor>();
		
		// load server descriptors from the legacy db_config.txt file
		if (legacyConfigFile.exists()) {
			try {
				System.out.println("Reolading server descriptors from the legacy db_config.txt file");
				SPECCHIOServerDescriptorStore s = new SPECCHIOServerDescriptorLegacyStore(legacyConfigFile);
				Iterator<SPECCHIOServerDescriptor> iter = s.getIterator();
				while (iter.hasNext()) {
					apps.add(iter.next());
					System.out.print(".");
				}
			}
			catch (IOException ex) {
				// read error; re-throw as a SPECCHIO client exception
				throw new SPECCHIOClientException(ex);
			}
		}		
	}

}
