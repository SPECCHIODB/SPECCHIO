package ch.specchio.gui;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.GridBagLayout;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.prefs.BackingStoreException;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.client.SPECCHIOClientFactory;
import ch.specchio.client.SPECCHIOPreferencesStore;
import ch.specchio.client.SPECCHIOServerDescriptor;
import ch.specchio.client.SPECCHIOServerDescriptorLegacyStore;
import ch.specchio.client.SPECCHIOServerDescriptorPreferencesStore;
import ch.specchio.constants.UserRoles;
import ch.specchio.metadata.MetaDataFromTabModel;
import ch.specchio.queries.Query;
import ch.specchio.types.Campaign;
import ch.specchio.types.Capabilities;
import ch.specchio.types.Category;
import ch.specchio.types.Institute;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.SpecchioCampaign;
import ch.specchio.types.TaxonomyNodeObject;
import ch.specchio.types.Units;
import ch.specchio.types.User;
import ch.specchio.types.attribute;


class MainMenu implements ActionListener, ItemListener {
	URI uri;
   JMenuBar menuBar;
   String create_new_campaign = "Create new campaign";
   String load_campaign_data = "Load campaign data";
   String create_user_account = "Create a new user account";
   String connect_to_db = "Connect to database";
   String edit_user_account = "Edit user information";
   String edit_db_config_file = "Edit db_config file";
   String upgrade_db = "Upgrade database";
   String preferences = "Preferences";
   String metadata_editor = "Edit metadata";
   String metadata_from_xls = "Get metadata from XLS";
   String data_removal = "Remove data";
   String data_browser = "Browse data hierarchy";
   String query_builder = "Build query";
   String fluoSpecchioVis = "DataVis";
   String measure_density = "Measure Metadata Space Density";
   String gonio_explorer = "Explore goniometer data";
   String data_explorer = "Explore & process data";
   String gonio_data_processor = "Process gonio data";
   String data_processor = "Interactive Data Processor";
   String cal_manager = "Manage calibrations";
   String instr_admin = "Instrumentation admin.";
   String load_sensor = "Load sensor definition";
   String campaign_export = "Export campaign";
   String campaign_import = "Import campaign";
   String add_target_reference_links = "Target-reference links";
   String time_shift = "Compute UTC";
   String sun_angle_calc = "Sun angle calculation";
   String gonio_angle_calc = "Gonio angle calculation";
   String test = "test";
   String info = "About";
   String list_eav_metadata_attributes = "List available Metadata Elements";
   String get_user_contacts = "Get SPECCHIO user contacts";
   String move_data = "Move data";
   
   /** menu items accessible to all clients */
   Hashtable<String, JMenuItem> public_menu_items;
   
   /** menu items accessible to all logged-in users */
   Hashtable<String, JMenuItem> user_menu_items;
   
   /** menu items accessiable only to administrative users */
   Hashtable<String, JMenuItem> admin_menu_items;
private JMenuItem dbConfigmenuItem;
   
   private static MainMenu instance = null;

   protected MainMenu() 
   {
	   // initialise menu item tables
	   public_menu_items = new Hashtable<String, JMenuItem>();
	   user_menu_items = new Hashtable<String, JMenuItem>();
	   admin_menu_items = new Hashtable<String, JMenuItem>();

	  // create the menu bar.
      JMenu menu;
      JMenuItem menuItem;

      menuBar = new JMenuBar();
      menu = new JMenu("Database");
      
      menuItem = new JMenuItem(create_user_account);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      public_menu_items.put(create_user_account, menuItem);
      
      menuItem = new JMenuItem(connect_to_db);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      public_menu_items.put(connect_to_db, menuItem);
      
      menuItem = new JMenuItem(edit_user_account);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      user_menu_items.put(edit_user_account, menuItem);
      
      menu.addSeparator();
      
      dbConfigmenuItem = new JMenuItem(edit_db_config_file);
      dbConfigmenuItem.addActionListener(this);
      menu.add(dbConfigmenuItem);
      try {
		SPECCHIOPreferencesStore prefs = new SPECCHIOPreferencesStore();		
		enable_db_config_tool(prefs.getBooleanPreference("DB_CONFIG_FILE_CREATION_AND_EDITING"));		
	} catch (BackingStoreException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      
      //public_menu_items.put(edit_db_config_file, menuItem);
      
      menu.addSeparator();
      
      menuItem = new JMenuItem(preferences);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      public_menu_items.put(preferences, menuItem);

      menu.addSeparator();
      
      menuItem = new JMenuItem(upgrade_db);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      admin_menu_items.put(upgrade_db, menuItem);
      
      
      menuBar.add(menu);
      
      // Data Input 
      menu = new JMenu("Data Input");
      
      menuItem = new JMenuItem(create_new_campaign);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      user_menu_items.put(create_new_campaign, menuItem);

      menuItem = new JMenuItem(load_campaign_data);
      menuItem.addActionListener(this);
      menu.add(menuItem); 
      user_menu_items.put(load_campaign_data, menuItem);
      
      menuItem = new JMenuItem(metadata_editor);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      user_menu_items.put(metadata_editor, menuItem);
      
      menuItem = new JMenuItem(metadata_from_xls);
      menuItem.addActionListener(this);
      menu.add(menuItem); 
      user_menu_items.put(metadata_from_xls, menuItem);
      
      menuBar.add(menu);
           
      // Data Output 
      menu = new JMenu("Data Selection & Output");
      
      menuItem = new JMenuItem(this.data_browser);
      menuItem.addActionListener(this);
      menu.add(menuItem);   
      user_menu_items.put(data_browser, menuItem);
        
      menuItem = new JMenuItem(query_builder);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      user_menu_items.put(query_builder, menuItem);

      menuBar.add(menu);

      // Data Visualizations
//	   menu = new JMenu("Visual Analysis Toolbox");

//	   menuItem = new JMenuItem(this.fluoSpecchioVis);
//	   menuItem.addActionListener(this);
//	   menu.add(menuItem);
//	   user_menu_items.put(fluoSpecchioVis, menuItem);

//	   menuBar.add(menu);
      
      // Data Maintenance 
      menu = new JMenu("Data Maintenance");
      
      menuItem = new JMenuItem(data_removal);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      user_menu_items.put(data_removal, menuItem);
  
      menuItem = new JMenuItem(move_data);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      user_menu_items.put(move_data, menuItem);    
      
      menuItem = new JMenuItem(campaign_export);
      menuItem.addActionListener(this);
      menu.add(menuItem);  
      user_menu_items.put(campaign_export, menuItem);
      
      menuItem = new JMenuItem(campaign_import);
      menuItem.setEnabled(false);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      admin_menu_items.put(campaign_import, menuItem);
      
      menuItem = new JMenuItem(load_sensor);
      menuItem.setEnabled(false);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      admin_menu_items.put(load_sensor, menuItem);
      
      menuItem = new JMenuItem(instr_admin);
      menuItem.addActionListener(this);
      menu.add(menuItem);     
      user_menu_items.put(instr_admin, menuItem);
     
      menuBar.add(menu);
      
      // Special Functions
      menu = new JMenu("Special Functions");
      
      menuItem = new JMenuItem(add_target_reference_links);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      user_menu_items.put(add_target_reference_links, menuItem);
      
      menuItem = new JMenuItem(time_shift);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      user_menu_items.put(time_shift, menuItem);
            
      menuItem = new JMenuItem(sun_angle_calc);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      user_menu_items.put(sun_angle_calc, menuItem);
      
      menuItem = new JMenuItem(gonio_angle_calc);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      user_menu_items.put(gonio_angle_calc, menuItem);
      
      menuBar.add(menu);

      
      // Help 
      menu = new JMenu("Help");
      try {
		uri = new URI("http://www.specchio.ch");
	} catch (URISyntaxException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      
      menuItem = new JMenuItem(list_eav_metadata_attributes);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      user_menu_items.put(list_eav_metadata_attributes, menuItem);      
      
      menuItem = new JMenuItem(get_user_contacts);
      menuItem.setEnabled(false);
      menuItem.addActionListener(this);
      menu.add(menuItem);
      admin_menu_items.put(get_user_contacts, menuItem);      
      
      
      menuItem = new JMenuItem(info);
      menuItem.addActionListener(this);
      menu.add(menuItem);      
      public_menu_items.put(info, menuItem);
      
      // uncomment for sandbox
//      menuItem = new JMenuItem(test);
//      menuItem.addActionListener(this);
//      menu.add(menuItem);      
//      public_menu_items.put(test, menuItem);      
      
      menuBar.add(menu);
   }
   
	public static MainMenu getInstance() 
	{
		if(instance == null) {
			instance = new MainMenu();
		}
		return instance;
	}    
	
	public void enable_tools_for_client(SPECCHIOClient client) throws SPECCHIOClientException
	{
		// always enable tools for unconnected clients
		for (JMenuItem menuItem : public_menu_items.values()) {
			menuItem.setEnabled(true);
		}
		
		if (client != null) {
			// enable menu items for all authenticated users
			enable_user_tools(true);
			
			// enable menu items for admin users only
			enable_admin_tools(client.isLoggedInWithRole(UserRoles.ADMIN));
		} else {
			// disable all tools
			enable_user_tools(false);
			enable_admin_tools(false);
		}
	}
	
	private void enable_admin_tools(boolean enable)
	{
		for (JMenuItem menuItem : admin_menu_items.values()) {
			menuItem.setEnabled(enable);
		}
	}
	
	private void enable_user_tools(boolean enable)
	{
		for (JMenuItem menuItem : user_menu_items.values()) {
			menuItem.setEnabled(enable);
		}
	}
	
	public void enable_db_config_tool(boolean enable)
	{
		if(enable)
		{
			dbConfigmenuItem.setEnabled(true);
		}
		else
		{
			dbConfigmenuItem.setEnabled(false);
		}				
	}

   JMenuBar getMenu() {
      return menuBar;
   }

   public void actionPerformed(ActionEvent e) 
   {
      if(create_new_campaign.equals(e.getActionCommand()))
      {
    	 try {
    		 NewCampaignDialog d = new NewCampaignDialog(new SpecchioCampaign(), SPECCHIOApplication.getInstance().get_frame());
    		 d.setVisible(true);
    	 }
    	 catch (SPECCHIOClientException ex) {
    		 JOptionPane.showMessageDialog(
    				 SPECCHIOApplication.getInstance().get_frame(),
    				 ex.getMessage(),
    				 "Error",
    				 JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
    	 }
         
      }
      if(load_campaign_data.equals(e.getActionCommand()))
      {
    	 try {
    		 LoadCampaignDataDialog d = new LoadCampaignDataDialog(SPECCHIOApplication.getInstance().get_frame(), false);
    		 d.setVisible(true);
    	 }
    	 catch (SPECCHIOClientException ex) {
    		 JOptionPane.showMessageDialog(
    				 SPECCHIOApplication.getInstance().get_frame(),
    				 ex.getMessage(),
    				 "Error",
    				 JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
    	 }
      }
      
      if(create_user_account.equals(e.getActionCommand()))
      {
    	  try {
    		  UserAccountDialog d = new UserAccountDialog(SPECCHIOApplication.getInstance().get_frame(), SPECCHIOApplication.getInstance().getClient(), null);
    		  d.setVisible(true);
    	  }
    	  catch (SPECCHIOClientException ex) {
    		  JOptionPane.showMessageDialog(
    				  SPECCHIOApplication.getInstance().get_frame(),
    				  ex.getMessage(),
    				  "Could not download data from the server",
    				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
    	  }
      }

      if(connect_to_db.equals(e.getActionCommand()))
      {
    	  try {
    		  DatabaseConnectionDialog d = new DatabaseConnectionDialog();
    		  d.setLocationRelativeTo(SPECCHIOApplication.getInstance().get_frame());
    		  d.setVisible(true);
    	  }
    	  catch (SPECCHIOClientException ex) {
    		  String msg = ((ex.getMessage()==null) ? "" : ex.getMessage()) +" : " +ex.getUserMessage();
    		  JOptionPane.showMessageDialog(
    				  SPECCHIOApplication.getInstance().get_frame(),
    				  msg,
    				  "Invalid configuration",
    				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
    	  } catch (FileNotFoundException e1) {
    		  JOptionPane.showMessageDialog(
    				  SPECCHIOApplication.getInstance().get_frame(),
    				  e1.getMessage(),
    				  "Could not read db_config.txt file",
    				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
		} catch (IOException e1) {
  		  JOptionPane.showMessageDialog(
				  SPECCHIOApplication.getInstance().get_frame(),
				  e1.getMessage(),
				  "Could not read db_config.txt file",
				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
			);
		}
      }
      
      if(edit_user_account.equals(e.getActionCommand()))
      {
    	  try {
    		  SPECCHIOClient specchio_client = SPECCHIOApplication.getInstance().getClient();
    		  UserAccountDialog d = new UserAccountDialog(
    				  SPECCHIOApplication.getInstance().get_frame(),
    				  specchio_client,
    				  specchio_client.getLoggedInUser()
    			);
    		  d.setVisible(true);
    	  }
          catch (SPECCHIOClientException ex) {
        	  JOptionPane.showMessageDialog(
        			  SPECCHIOApplication.getInstance().get_frame(),
        			  ex.getMessage(),
        			  "Error",
        			  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
        		);
          }
      }
      
      if(edit_db_config_file.equals(e.getActionCommand()))
      {
    	  File temp = new File(SPECCHIOClientFactory.getApplicationFilepath("db_config.txt"));

    	  try {
    		  if (Desktop.isDesktopSupported()) {
    			  Desktop.getDesktop().open(temp);
    		  }
    		  else
    		  {
    			  JOptionPane.showMessageDialog(
    					  SPECCHIOApplication.getInstance().get_frame(),
    					  "This operating system/Java VM does not support the opening of files from Java in the Desktop.\n"
    					  + "Please open this file manually: " + temp.getPath(),
    					  "Error",
    					  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    					  );    			  
    		  }
    		  
    	  } catch (java.lang.IllegalArgumentException ex) {
    		  
    		  // the file does not exist yet: create a new one and fill it with info from the preferences
    		  try {
				temp.createNewFile();
				
				// write existing configuration data into the file
				SPECCHIOServerDescriptorPreferencesStore prefs = new SPECCHIOServerDescriptorPreferencesStore();
				SPECCHIOServerDescriptorLegacyStore legacy_prefs = new SPECCHIOServerDescriptorLegacyStore(temp);
				
				Iterator<SPECCHIOServerDescriptor> pref_it = prefs.getIterator();
				
				while(pref_it.hasNext())
				{
					legacy_prefs.addServerDescriptor(pref_it.next());
				}
				
				SPECCHIOApplication.openInDesktop(temp);
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (BackingStoreException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
    		  
    	  }
    	  catch (IOException e1) {
    		  // TODO Auto-generated catch block
    		  e1.printStackTrace();
    	  } catch (SPECCHIOClientException ex) {
    		  String msg = ((ex.getMessage()==null) ? "" : ex.getMessage()) +" : " +ex.getUserMessage();
        	  JOptionPane.showMessageDialog(
        			  SPECCHIOApplication.getInstance().get_frame(),
        			  msg,
        			  "Error",
        			  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
        		);
		}		


      }
      
      
      if(preferences.equals(e.getActionCommand()))
      {
    	  try {
    		  new Preferences();
    	  }
    	  catch (SPECCHIOClientException ex) {
    		  JOptionPane.showMessageDialog(
    				  SPECCHIOApplication.getInstance().get_frame(),
    				  ex.getMessage(),
    				  "Error",
    				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
    	  }
    	  

      }
      
      if(upgrade_db.equals(e.getActionCommand()))
      {
    	  try {
    		  DbUpgrade d = new DbUpgrade();
    		  d.setVisible(true);
    	  }
    	  catch (SPECCHIOClientException ex) {
    		  JOptionPane.showMessageDialog(
    				  SPECCHIOApplication.getInstance().get_frame(),
    				  ex.getMessage(),
    				  "Error",
    				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
    	  }
      
      }
      

      if(metadata_editor.equals(e.getActionCommand()))
      {
    	  try {
    		  new MetaDataEditorView(SPECCHIOApplication.getInstance().get_frame());
    	  }
    	  catch (SPECCHIOClientException ex) {
    		  JOptionPane.showMessageDialog(
    				  SPECCHIOApplication.getInstance().get_frame(),
    				  ex.getMessage(),
    				  "Error",
    				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
    	  }
      }
      
      if(metadata_from_xls.equals(e.getActionCommand()))
      {
    	  try {
    		  new MetaDataFromTabView();
    	  }
    	  catch (SPECCHIOClientException ex) {
    		  JOptionPane.showMessageDialog(
    				  SPECCHIOApplication.getInstance().get_frame(),
    				  ex.getMessage(),
    				  "Error",
    				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
    	  }
      }
      
      if (add_target_reference_links.equals(e.getActionCommand()))
      {
    	  try {
    		  TargetReferenceLinkDialog d = new TargetReferenceLinkDialog(SPECCHIOApplication.getInstance().get_frame(), false);
    		  d.setVisible(true);
    	  }
    	  catch (SPECCHIOClientException ex) {
    		  JOptionPane.showMessageDialog(
    				  SPECCHIOApplication.getInstance().get_frame(),
    				  ex.getMessage(),
    				  "Error",
    				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
    	  }
      }
      
      if(data_removal.equals(e.getActionCommand()))
      {
    	  try {
    		  DataRemoverDialog d = new DataRemoverDialog();
    		  d.setVisible(true);
    	  }
    	  catch (SPECCHIOClientException ex) {
    		  JOptionPane.showMessageDialog(
    				  SPECCHIOApplication.getInstance().get_frame(),
    				  ex.getMessage(),
    				  "Error",
    				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
    	  }
      }
      
      
      if(move_data.equals(e.getActionCommand()))
      {
    	  try {
    		  DataMoverDialog d = new DataMoverDialog();
    		  d.setVisible(true);
    	  }
    	  catch (SPECCHIOClientException ex) {
    		  JOptionPane.showMessageDialog(
    				  SPECCHIOApplication.getInstance().get_frame(),
    				  ex.getMessage(),
    				  "Error",
    				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
    	  }
      }
     
      
      

      if(campaign_export.equals(e.getActionCommand()))
      {
    	  try {
    		  CampaignExportDialog d = new CampaignExportDialog();
    		  d.setVisible(true);
    	  }
    	  catch (SPECCHIOClientException ex) {
    		  JOptionPane.showMessageDialog(
    				  SPECCHIOApplication.getInstance().get_frame(),
    				  ex.getMessage(),
    				  "Error",
    				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
    	  }
      }
      
      if(campaign_import.equals(e.getActionCommand()))
      {
    	  try {
    		  CampaignImportDialog d = new CampaignImportDialog();
    		  d.setVisible(true);
    	  }
    	  catch (SPECCHIOClientException ex) {
    		  JOptionPane.showMessageDialog(
				  SPECCHIOApplication.getInstance().get_frame(),
				  ex.getMessage(),
				  "Error",
				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
			);
    	  }
      }
      
      
      if(this.data_browser.equals(e.getActionCommand()))
      {
		try {
			QueryBuilder d = new QueryBuilder("Data Browser (V3)", "hierarchy_browser");
			d.setVisible(true);
		}
		catch (SPECCHIOClientException ex) {
  		  JOptionPane.showMessageDialog(
				  SPECCHIOApplication.getInstance().get_frame(),
				  ex.getMessage(),
				  "Error",
				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
			);
		}
         
      }      
  
      if(query_builder.equals(e.getActionCommand()))
      {
		try {
			QueryBuilder d = new QueryBuilder("Query Builder", "combined");
			d.setVisible(true);
		} 
		catch (SPECCHIOClientException ex) {
	  		  JOptionPane.showMessageDialog(
					  SPECCHIOApplication.getInstance().get_frame(),
					  ex.getMessage(),
					  "Error",
					  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
				);
			}
         
      }

//      if(fluoSpecchioVis.equals(e.getActionCommand())){
//      	try{
//      		InfoVis vis = new InfoVis();
//		}
//      	catch (SPECCHIOClientException ex){
//      		JOptionPane.showMessageDialog(
//					SPECCHIOApplication.getInstance().get_frame(),
//					ex.getMessage(),
//					"Error",
//					JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
//			);
//		}
//	  }
      
      if(load_sensor.equals(e.getActionCommand()))
      {
    	  try {
    		  SensorDefinitionLoaderDialog d = new SensorDefinitionLoaderDialog();
    		  d.setVisible(true);
    	  }
    	  catch (SPECCHIOClientException ex) {
    		  JOptionPane.showMessageDialog(
    				  SPECCHIOApplication.getInstance().get_frame(),
    				  ex.getMessage(),
    				  "Error",
    				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
    	  }
      }
 
      if(instr_admin.equals(e.getActionCommand()))
      {
    	  try {
    		  InstrumentationMetadataEditor d = new InstrumentationMetadataEditor();
    		  d.setVisible(true);
    	  }
    	  catch (SPECCHIOClientException ex) {
    		  JOptionPane.showMessageDialog(
    				  SPECCHIOApplication.getInstance().get_frame(),
    				  ex.getMessage(),
    				  "Error",
    				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
    	  }
      }
      
      if(info.equals(e.getActionCommand()))
      {
//         ImageIcon icon = new ImageIcon("SPECCHIO_Icon_Mid_Res_small.jpg");
         
         
//         JOptionPane.showMessageDialog(null,
//        		 SPECCHIOApplication.version + "\n" + 
//        		 "(c) 2006-2012 by Remote Sensing Laboratories (RSL),\n" +
//        		 "Dept of Geography, " +
//        		 "University of Zurich\n" + 
//        		 "www.specchio.ch\n\n" +
//        		 "Please refer to the User Guide for more information.\n",
//        		    "About",
//        		    JOptionPane.INFORMATION_MESSAGE,
//        		    icon);
    	  
    	  BufferedImage myPicture;
		try {
			myPicture = ImageIO.read(ClassLoader.getSystemClassLoader().getResource("SPECCHIO_Icon_Mid_Res_small.jpg"));
//			myPicture = ImageIO.read(new File("SPECCHIO_Icon_Mid_Res_small.jpg"));
			JLabel picLabel = new JLabel(new ImageIcon(myPicture));	  


    	  
         
         JFrame frame = new JFrame("About");
         frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
         frame.setSize(550, 200);
         Container container = frame.getContentPane();
         container.setLayout(new GridBagLayout());
         
         container.add(picLabel);
         
         JButton button = new JButton();
         
         //  create text
         String text = "<html>" + "Version: " + SPECCHIO_ReleaseInfo.getVersion() + "<br><br>" +
                 "Build Number: " + SPECCHIO_ReleaseInfo.getBuildNumber() + "<br>" +
                 "Build Date: " + SPECCHIO_ReleaseInfo.getBuildDate()
                		 + "<br><br>" +
                		 "(c) 2006-2020 by Remote Sensing Laboratories (RSL)<br>" +
                		 "Dept. of Geography, " +
                		 "University of Zurich (CH)<br>" +
                		 "(c) 2013-2014 by University of Wollongong (AU)<br><br>" +
                		 " For more information visit: " +
                		 "<FONT color=\"#000099\"><U>www.specchio.ch</U></FONT>" +
                		 " or <br>" +
                		 "refer to the user guide.";
         
         // add server details if available
         SPECCHIOClient specchio_client = SPECCHIOApplication.getInstance().getClient();
         
         if(specchio_client != null)
         {
        	 text = text  + "<br><br>" + "Server Settings" +  "<br>" + "-------------" + "<br><br>";
        	 
        	 
             String create_read_only_users, max_object_size;
             if(specchio_client.getCapability(Capabilities.CREATE_READ_ONLY_USERS_BY_DEFAULT) != null)
             {
            	 create_read_only_users = specchio_client.getCapability(Capabilities.CREATE_READ_ONLY_USERS_BY_DEFAULT);

            	 text = text  + Capabilities.CREATE_READ_ONLY_USERS_BY_DEFAULT + ": " + create_read_only_users  + "<br>";
             }
             
             
             if(specchio_client.getCapability(Capabilities.MAX_OBJECT_SIZE) != null)
             {
            	 max_object_size = specchio_client.getCapability(Capabilities.MAX_OBJECT_SIZE);

            	 text = text  + Capabilities.MAX_OBJECT_SIZE + ": " + max_object_size  + " [bytes]<br>";
             }
        	 
         }





         text = text + " </HTML>";
        
         button.setText(text);
         button.setHorizontalAlignment(SwingConstants.LEFT);
         button.setBorderPainted(false);
         button.setOpaque(false);
         button.setBackground(Color.WHITE);
         button.setToolTipText(uri.toString());
         button.addActionListener(new OpenUrlAction());
         container.add(button);
         frame.pack();
         frame.setVisible(true);     
         
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();         
         
		}
         
      }   
      
      if(list_eav_metadata_attributes.equals(e.getActionCommand()))
      {
    	  // user metadata from tabular data model to get the attributes sorted by categories
    	  MetaDataFromTabModel model = new MetaDataFromTabModel();
    	  SPECCHIOClient specchio_client = SPECCHIOApplication.getInstance().getClient();
    	  
    	// write the meta-parameter value to a temporary file
			File temp;
			try {
				temp = File.createTempFile("specchio", ".txt");
				
				temp.deleteOnExit();
				FileOutputStream fos = new FileOutputStream(temp);
				
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

				bw.write("\n");
				bw.write("List of Metadata Elements by Category available in this database");
				bw.write("\n");
				bw.write("================================================================");
				bw.write("\n");
				bw.write("\n");
				bw.write("Element Name \tElement Description \tStorage Field in Database \tUnit(short) \tUnit");
				bw.write("\n");
				bw.write("----------------------------------------------------------------");
				bw.write("\n");
				bw.write("\n");
				bw.write("\n");
				
				ArrayList<Category> categories =  model.getPossible_categories();
				
				for(Category category : categories)
				{
					bw.write(category.name);
					bw.write("\n");
					bw.write("----------------------------------------------------------------");
					bw.write("\n");
					
					// list all attributes
					attribute[] attr_array = specchio_client.getAttributesForCategory(category.name);
					
					
					for (attribute attr : attr_array) {		
						
						Units unit = specchio_client.getAttributeUnits(attr);
						
						bw.write(attr.getName());
						//bw.write("[" + attr. + "]");				// prepared to write units, but must read them first from DB		
						bw.write("\t");
						if(attr.description != null)
						{
							bw.write(attr.description);						
						}
						bw.write("\t");
						bw.write(attr.default_storage_field);
						bw.write("\t");
						if(unit.id != 0)
						{
							bw.write(unit.short_name);
							bw.write("\t");
							if(unit.name != null)
								bw.write(unit.name);
						}
						bw.write("\n");
					}
					
					
					bw.write("----------------------------------------------------------------");
					bw.write("\n");
					bw.write("\n");
					bw.write("\n");
				}
				
				
				bw.close();

				//mp_file.writeValue(fos);
				fos.close();

				// launch the external viewer
				SPECCHIOApplication.openInDesktop(temp);			
				
				
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (SPECCHIOClientException e1) {
				e1.printStackTrace();
			}
      }
      
      if(get_user_contacts.equals(e.getActionCommand()))
      {
    	  
    	  SPECCHIOClient specchio_client = SPECCHIOApplication.getInstance().getClient();
    	  
    	  User[] users = specchio_client.getUsers();
    	  
    	// write the meta-parameter value to a temporary file
    	  File temp;
    	  try {
    		  temp = File.createTempFile("specchio", ".txt");

    		  temp.deleteOnExit();
    		  FileOutputStream fos = new FileOutputStream(temp);

    		  BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

    		  bw.write("\n");
    		  bw.write("User contacts of this database");
    		  bw.write("\n");
    		  bw.write("================================================================");
    		  bw.write("\n");
    		  bw.write("\n");
    		  bw.write("Emails:");
    		  bw.write("\n");
    		  bw.write("\n");
    		  
    		  for(User user : users)
    		  {
    			  
    			  bw.write(user.getEmailAddress());
    			  bw.write(",");
    		  }
    		  
    		  bw.write("\n");

    		  bw.close();

    		  //mp_file.writeValue(fos);
    		  fos.close();

    		  // launch the external viewer
    		  SPECCHIOApplication.openInDesktop(temp);				


    	  } catch (IOException e1) {
    		  e1.printStackTrace();
    	  } catch (SPECCHIOClientException e1) {
    		  e1.printStackTrace();
    	  }


      }
      
      
      if(test.equals(e.getActionCommand()))
      {
    	// TODO The Sandbox is here ....
    	  // Sandbox
    	  SPECCHIOClient specchio_client = SPECCHIOApplication.getInstance().getClient();
    	  
    	  try {
    		  
//    		  User[] users = specchio_client.getUsersWithStatistics();
    		  
//    		  Institute[] inst = specchio_client.getInstitutes();
    		  int attr_id = specchio_client.getAttributesNameHash().get("File Name").getId();
//    		  
//    		  ArrayList<Integer> ids = new ArrayList<Integer>();
//    		  ids.add(251403);
//    		  specchio_client.getMetaparameters(ids, "File Name");
//    		  
    		  ArrayList<MetaParameter> values = specchio_client.getDistinctValuesOfAttribute(attr_id);
    		  
//    		  ArrayList<Integer> lists = specchio_client.getNewestSpectra(10);
//    		  Campaign[] c = specchio_client.getCampaigns();
    		  
//    		  Campaign c = specchio_client.getCampaign(1);
    		  
//    		  TaxonomyNodeObject node = specchio_client.getTaxonomyRootNode(specchio_client.getAttributesNameHash().get("Basic Target Type").getId());
    		  
    		  //Hashtable<String, Integer> hash = specchio_client.getTaxonomyHash(specchio_client.getAttributesNameHash().get("Basic Target Type").getId());
    		  
//    		  ArrayList<Integer> ids = new ArrayList<Integer>();
//    		  ids.add(251403);
//    		  ids.add(251412);
//    		  ids.add(251413);
//    		  
//    		  ArrayList<Integer> attribute_ids = new ArrayList<Integer>();
//    		  attribute_ids.add(specchio_client.getAttributesNameHash().get("Basic Target Type").getId());
//    		  attribute_ids.add(specchio_client.getAttributesNameHash().get("Integration Time").getId());
//    		  
//    		  ArrayList<ArrayList<MetaParameter>> data = specchio_client.getMetaparameters(ids, attribute_ids);
    		  
    	//	  specchio_client.getSpectrumIdsMatchingQuery(null);
    		  
    		  //specchio_client.renameHierarchy(2758, "CCCC");
//    		  specchio_client.renameHierarchy(2164, "s2_t1_Water");
    		  
    		  
//    		  specchio_client.getSpectrumIdsMatchingFullTextSearch("%grass%");
//    		  specchio_client.renameHierarchy(2173, "Artificial");
    		  //specchio_client.getHierarchyFilePath(2530);
    		  
//    		  Spectrum s = specchio_client.getSpectrum(268081, false);

    		  //Object cond = new EAVQueryConditionObject(attr);

    		  
//			String has_license = specchio_client.getCapability("END_USER_LICENSE");
//			
//			String short_license = specchio_client.getCapability("END_USER_LICENSE_SHORT_TEXT");
			
			int x = 1;
		} catch (SPECCHIOClientException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
    	  
//    	  try {
//			//int tax_id = specchio_client.getTaxonomyId(160, "Hemispherical-conical (CASE 8)");
////    		Instrument i = specchio_client.getCalibratedInstrument(22);
////			int x = 1;
//    		  
////    		  specchio_client.getInstrumentIds(spectrum_ids)  
//    		  
//    		  
//		} catch (SPECCHIOClientException e1) {
//			e1.printStackTrace();
//		}
    	  
      }
  	
      if(time_shift.equals(e.getActionCommand()))
      {
    	  try {
	         System.out.println(e.getActionCommand());
	         TimeShiftDialog d = new TimeShiftDialog();
	         d.setVisible(true);
    	  }
    	  catch (SPECCHIOClientException ex) {
    		  JOptionPane.showMessageDialog(
    				  SPECCHIOApplication.getInstance().get_frame(),
    				  ex.getMessage(),
    				  "Error",
    				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
    			);
    	  }
      }
      
           
      
      
      if (sun_angle_calc.equals(e.getActionCommand()))
      {
    	  try {
 	         SunAngleCalcDialog d = new SunAngleCalcDialog(SPECCHIOApplication.getInstance().get_frame(), false);
 	         d.setVisible(true);
     	  }
     	  catch (SPECCHIOClientException ex) {
     		  JOptionPane.showMessageDialog(
     				  SPECCHIOApplication.getInstance().get_frame(),
     				  ex.getMessage(),
     				  "Error",
     				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
     			);
     	  }
       }
      
      if (gonio_angle_calc.equals(e.getActionCommand()))
      {
    	  try {
 	         GonioAngleCalcDialog d = new GonioAngleCalcDialog(SPECCHIOApplication.getInstance().get_frame(), false);
 	         d.setVisible(true);
     	  }
     	  catch (SPECCHIOClientException ex) {
     		  JOptionPane.showMessageDialog(
     				  SPECCHIOApplication.getInstance().get_frame(),
     				  ex.getMessage(),
     				  "Error",
     				  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
     			);
     	  }
       }
    	  
      
      

   }
   

protected ImageIcon createImageIcon(String path) {
       java.net.URL imgURL = getClass().getResource(path);
       if (imgURL != null) {
           return new ImageIcon(imgURL);
       } else {
           System.err.println("Couldn't find file: " + path);
           return null;
       }
   }

   public void itemStateChanged(ItemEvent e) {

   }
   

   
   
   // URL example from: http://stackoverflow.com/questions/527719/how-to-add-hyperlink-in-jlabel
   


   class OpenUrlAction implements ActionListener {
	      @Override public void actionPerformed(ActionEvent e) {
	    	  SPECCHIOApplication.openInDesktop(uri);
	      }
	    }


}

