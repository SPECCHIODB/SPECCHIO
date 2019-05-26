package ch.specchio.gui;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.swing.*;

import org.apache.commons.io.FilenameUtils;

import net.iharder.dnd.FileDrop;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.client.SPECCHIOClientFactory;
import ch.specchio.client.SPECCHIOServerDescriptor;
import ch.specchio.types.Capabilities;


public class SPECCHIOApplication {

	
	private static JFrame frame;
	private static SPECCHIOApplication instance = null;
	private SPECCHIOClient client = null;
	public static Float min_db_version = 3.2F;
	public static ImageIcon specchio_icon;
	private static boolean java_version_is_supported = true;
	


	/* progress report in the operations pane */
	static ProgressReportTextPanel p_rep = null;
	static ProgressReportTextPanel db_rep = null;
	
	protected SPECCHIOApplication()
	{
//		GatewayServer gatewayServer = new GatewayServer(this);
//        gatewayServer.start();
//        System.out.println("Specchio - Python Gateway Server Started");		
	}
	
	public boolean isJava_version_is_supported() {
		return java_version_is_supported;
	}	
	
	public SPECCHIOClient getClient()
	{
		return client;
	}
	
	public SPECCHIOClientFactory getClientFactory()
	{
		return SPECCHIOClientFactory.getInstance();
	}	
	
	public static SPECCHIOApplication getInstance() 
	{
		if(instance == null) {
			instance = new SPECCHIOApplication();
		}
		return instance;
	}      
	
	public JFrame get_frame()
	{
		return frame;
	}
	

	public Component createComponents() {

		JPanel pane = new JPanel(new BorderLayout());
		OperationsPane op = OperationsPane.getInstance();
		specchio_icon = new ImageIcon(ClassLoader.getSystemClassLoader().getResource("SPECCHIO_Icon_Mid_Res_small.jpg"));

		JLabel test = new JLabel("Spectral Database System", specchio_icon, JLabel.CENTER);
		test.setVerticalTextPosition(JLabel.BOTTOM);
		test.setHorizontalTextPosition(JLabel.CENTER);
		test.setOpaque(true);
		test.setBackground(Color.WHITE);
		
			
		
		  
		JPanel mid_pane = new JPanel(new BorderLayout());
		
        new FileDrop(mid_pane, new FileDrop.Listener()
        {   public void filesDropped( java.io.File[] files )
            {   for( int i = 0; i < files.length; i++ )
                {   try
                    {   
//                		files[i].getCanonicalPath()
                	
                	File file = files[i];
                	String ext = FilenameUtils.getExtension(file.getCanonicalFile().toString());
                	
                	if(ext.equals("xml"))
                	{
                		CampaignImportDialog d = new CampaignImportDialog(files[i].getCanonicalPath());
                		d.setVisible(true);               		
                	}
                	

                	
                    }   // end try
                    catch( java.io.IOException e ) {}
                }   // end for: through each dropped file
            }   // end filesDropped
        }); // end FileDrop.Listener
		
			
		mid_pane.add(BorderLayout.CENTER, test);

		pane.add(mid_pane, BorderLayout.WEST);
		pane.add(op, BorderLayout.CENTER);
			
		op.validate();
	      
		pane.validate();

		return pane;
	}


	   /**
	    * Create the GUI and show it.  For thread safety,
	    * this method should be invoked from the
	    * event-dispatching thread.
	    */
	   private static void createAndShowGUI() 
	   {
		   System.out.println("Welcome to " + SPECCHIO_ReleaseInfo.getVersion());
	      //Create and set up the window.
	      frame = new JFrame(SPECCHIO_ReleaseInfo.getVersion());
	      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	      SPECCHIOApplication app = SPECCHIOApplication.getInstance();
	      Component contents = app.createComponents();
	      frame.getContentPane().add(contents, BorderLayout.CENTER);

	      /////////////// add the menu //////////////
	      MainMenu menu = MainMenu.getInstance();
	      try {
	    	  menu.enable_tools_for_client(null);
	      }
	      catch (SPECCHIOClientException ex) {
	    	  // enable_tools_for_client() never throws an exception for a null argument
	    	  ex.printStackTrace();
	      }
	     
	      frame.setJMenuBar(menu.getMenu());

	      //Display the window.
	      frame.pack();
	      frame.setSize(new Dimension(700, 350));

	      frame.setVisible(true);

	      System.out.println("Java version (major): " + getJavaVersion());
	      

	      if(getJavaVersion() != 8) {
	    	  java_version_is_supported = false;
	    	  JOptionPane.showMessageDialog(
	    			  SPECCHIOApplication.getInstance().get_frame(),
	    			  "This version of SPECCHIO is compiled for Java version 8.\n" +
	    					  "This computer runs Java version " + getJavaVersion() + ".\n" +
	    					  "Please install Java version 8 to avoid errors.",
	    					  "Warning",
	    					  JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
	    			  );    			  

	      }
	   }


	   public static void main(String[] args) {
		   //Schedule a job for the event-dispatching thread:
		   //creating and showing this application's GUI.
		   try {
			   UIManager.setLookAndFeel(
					   UIManager.getSystemLookAndFeelClassName());
		   } catch (Exception e) {
		   }
		   
		   javax.swing.SwingUtilities.invokeLater(new Runnable() {
			   public void run() {
				   createAndShowGUI();
			   }
		   }
		   );
	   }
	   
	   public static void openInDesktop(File temp)
	   {
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

	 	  } catch (java.lang.IllegalArgumentException ex)	 
	 	  {
	 		  // something wrong with the temporary file
	 		  ErrorDialog error = new ErrorDialog(SPECCHIOApplication.getInstance().get_frame(), "Could not start viewer", ex.getMessage(), ex);
	 		  error.setVisible(true);
	 	  } catch (IOException ex) {
	 		  // something wrong with the temporary file
	 		  ErrorDialog error = new ErrorDialog(SPECCHIOApplication.getInstance().get_frame(), "Could not start viewer", ex.getMessage(), ex);
	 		  error.setVisible(true);

	 	  }catch (UnsupportedOperationException ex) {
	 		  // platform does not support desktop operations
	 		  ErrorDialog error = new ErrorDialog(SPECCHIOApplication.getInstance().get_frame(), "Could not start viewer", ex.getMessage(), ex);
	 		  error.setVisible(true);
	 	  }

	 	  
	   }		  	   
	   
	   // URL example from: http://stackoverflow.com/questions/527719/how-to-add-hyperlink-in-jlabel	   
	   public static void openInDesktop(URI uri) {
		   if (Desktop.isDesktopSupported()) {
			   try {
				   Desktop.getDesktop().browse(uri);
			   } catch (IOException e) { /* TODO: error handling */ }
		   } else {
			   JOptionPane.showMessageDialog(
					   SPECCHIOApplication.getInstance().get_frame(),
					   "This operating system/Java VM does not support the opening of URLs from Java in the Desktop.\n"
							   + "Please open this page manually: " + uri,
							   "Error",
							   JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
					   );    			  

		   }
	   }   	
	   
	   // from: https://stackoverflow.com/questions/2591083/getting-java-version-at-runtime
	   private static int getJavaVersion() {
		    String version = System.getProperty("java.version");
		    if(version.startsWith("1.")) {
		        version = version.substring(2, 3);
		    } else {
		        int dot = version.indexOf(".");
		        if(dot != -1) { version = version.substring(0, dot); }
		    } return Integer.parseInt(version);
		}	   

	   public void setClient(SPECCHIOClient client) {
		   
		   this.client = client;

		   OperationsPane op = OperationsPane.getInstance();
		   if (client != null) {
			   SPECCHIOServerDescriptor d = client.getServerDescriptor();
			   
			   	// add connection details to the operations pane
			   if (p_rep == null) {
				   p_rep = new ProgressReportTextPanel("Database connection status", d.getDisplayName(false, false));
	  		  		op.add_report(p_rep);	
			   }
			   p_rep.set_operation("Connected as " + d.getDisplayUser() + " to '" + d.getDataSourceName() + "' on:");
			   p_rep.set_component(d.getDisplayName(false, false));
			   
			   
			   String spat_ext = "";
			   if(client.getCapability(Capabilities.SPATIAL_EXTENSION) != null && client.getCapability(Capabilities.SPATIAL_EXTENSION).equals("true"))
				   spat_ext = " - Spatial DB Support";
			   
			   String db_version = "";
			   if(client.getCapability(Capabilities.DB_VERSION) != null)
				   db_version = client.getCapability(Capabilities.DB_VERSION);
			   
			   String op_string = "Database Info: " + (db_version.equals("") ? "" : "V" + db_version + spat_ext + "");
			   
			   if (db_rep == null) {
				   db_rep = new ProgressReportTextPanel( op_string,"");				   
	  		  		op.add_report(db_rep);	
			   }
			   
			   db_rep.set_operation_description(op_string);
			   db_rep.set_component("Number of spectra in database: " + client.getSpectrumCountInDB());
			   
			   if(client.getCapability(Capabilities.SERVER_VERSION) != null)
			   {
				   
				   db_rep.set_operation("Server version: " + client.getCapability(Capabilities.SERVER_VERSION) + " - Build " + client.getCapability(Capabilities.SERVER_BUILD_NUMBER));
			   }
			   db_rep.setPreferredSize(p_rep.getSize());
			   
			   
			   try {
				   // enable menu items appropriate for this connection
				   MainMenu.getInstance().enable_tools_for_client(client);
			   }
			   catch (SPECCHIOClientException ex) {
				   // lost the connection to the server already!
				   ErrorDialog error = new ErrorDialog(frame, "Could not establish user privileges", ex.getUserMessage(), ex);
				   error.setVisible(true);
			   }
		   } else {
			   // remove connection details from the operations pane
			   if (p_rep != null) {
				   op.remove_report(p_rep);
				   p_rep = null;
			   }
			   if (db_rep != null) {
				   op.remove_report(db_rep);
				   db_rep = null;
			   }
			   
			   
			   // disable menu items
			   try {
				   MainMenu.getInstance().enable_tools_for_client(null);
			   }
			   catch (SPECCHIOClientException ex) {
				   // enable_tools_for_client() never throws an exception for a null argument
				   ex.printStackTrace();
			   }
			   
		   }
		   
	   }

}
