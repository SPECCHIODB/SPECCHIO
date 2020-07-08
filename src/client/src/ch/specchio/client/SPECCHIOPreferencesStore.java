package ch.specchio.client;

import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


public class SPECCHIOPreferencesStore {
	
	/** the path to the root node of the configuration data */
	private static final String PREFS_CONFIGURATION_PATH = "ch.specchio.client/preferences";
	
	/** the root node of the account configuration data */
	private Preferences specchioConfigurationPreferences;
	

	public SPECCHIOPreferencesStore() throws BackingStoreException {
		
		
		// get the root node of the account configuration data
		specchioConfigurationPreferences = Preferences.userRoot().node(PREFS_CONFIGURATION_PATH);
		
	}
	
	public Boolean getBooleanPreference(String name) throws IOException {
		
		Preferences node = specchioConfigurationPreferences.node("SPECCHIO_CLIENT_PREFERENCES");

		return (node.getBoolean(name, false));

	
	}
	
	
	public void setBooleanPreference(Boolean setting, String name) throws IOException {

		Preferences node = specchioConfigurationPreferences.node("SPECCHIO_CLIENT_PREFERENCES");

		node.putBoolean(name, setting);
		
	}	
	

	public String getStringPreference(String name) throws IOException {
		
		Preferences node = specchioConfigurationPreferences.node("SPECCHIO_CLIENT_PREFERENCES");

		return (node.get(name, System.getProperty("user.dir")));

	
	}
	
	
	public void setStringPreference(String setting, String name) throws IOException {

		Preferences node = specchioConfigurationPreferences.node("SPECCHIO_CLIENT_PREFERENCES");

		node.put(name, setting);
		
	}	
	
	public Preferences getPreferences()
	{
		return this.specchioConfigurationPreferences;
	}
	

}
