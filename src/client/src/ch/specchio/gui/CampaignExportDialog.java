package ch.specchio.gui;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.prefs.BackingStoreException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.client.SPECCHIOPreferencesStore;
import ch.specchio.constants.FileTypes;
import ch.specchio.constants.HeaderBody;
import ch.specchio.types.Campaign;




public class CampaignExportDialog   extends JFrame implements ActionListener
{

	private static final long serialVersionUID = 1L;
	JComboBox campaign_combo;
	Connection db_conn;
	GridbagLayouter l;
	JTextField target_dir;
	JFileChooser fc;
	JButton load;
	SPECCHIOClient specchio_client;
	private File f;
	
	public CampaignExportDialog() throws SPECCHIOClientException
	{
		
		super("Campaign Export");
		
		// get a reference to the application's client object
		this.specchio_client = SPECCHIOApplication.getInstance().getClient();
		
		fc = new JFileChooser();
		
		SPECCHIOPreferencesStore prefs;
		try {
			prefs = new SPECCHIOPreferencesStore();
			
			fc = new JFileChooser(prefs.getStringPreference("OUTPUT_DIRECTORY"));
			
		} catch (BackingStoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		l = new GridbagLayouter(this);
		
		// create GUI
		GridBagConstraints constraints = new GridBagConstraints();
		
		constraints.gridwidth = 1;
		constraints.insets = new Insets(4, 4, 4, 4);
		constraints.gridheight = 1;
		constraints.anchor = GridBagConstraints.WEST;
		
		constraints.gridx = 0;
		constraints.gridy = 0;
		l.insertComponent(new JLabel("Campaign name:"), constraints);	
		
		constraints.gridwidth = 2;
		constraints.gridx = 0;
		constraints.gridy = 1;	
		campaign_combo = define_combo_box();	
		l.insertComponent(campaign_combo, constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 2;	
		l.insertComponent(new JLabel("All relational data of this campaign will be exported to an xml file."), constraints);
		
		constraints.gridy++;
		constraints.gridx = 0;	
		constraints.gridwidth = 1;
		l.insertComponent(new JLabel("Target directory:"), constraints);
		target_dir = new JTextField(fc.getCurrentDirectory().getAbsolutePath(), 40);
		target_dir.setEditable(false);
		constraints.gridx = 1;		
		l.insertComponent(target_dir, constraints);
		
		JButton browse = new JButton("Browse");
		browse.setActionCommand("browse");
		browse.addActionListener(this);
		constraints.gridx = 2;
		l.insertComponent(browse, constraints);
		
		constraints.gridwidth = 1;
		constraints.gridx = 0;
		constraints.gridy++;	
		load = new JButton("Export");
		l.insertComponent(load, constraints);
		load.setActionCommand("export");
		load.addActionListener(this);
		//load.setEnabled(false);
		
		constraints.gridx = 1;
		JButton cancel = new JButton("Cancel");
		l.insertComponent(cancel , constraints);
		cancel.setActionCommand("cancel");
		cancel.addActionListener(this);		
		
		pack();
		
	}
	
	private  JComboBox define_combo_box() throws SPECCHIOClientException
	{
		// get a list of campaigns from the server
		Campaign[] campaigns = specchio_client.getCampaigns();
		
		// add them to the combo box
		JComboBox combo_box = new JComboBox();
		for (Campaign c: campaigns) {
			combo_box.addItem(c);
		}
		
		return combo_box;
	}
	
	

	public void actionPerformed(ActionEvent e) {
		if ("cancel".equals(e.getActionCommand())) {
			this.setVisible(false);
		} 
		if ("export".equals(e.getActionCommand())) {
			System.out.println("export");
			
			try {
				// get currently selected campaign from combobox
				Campaign c = (Campaign)campaign_combo.getSelectedItem();
	
				// the file name consists of target directory, campaign name plus current date plus time
			    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		        Date date = new Date();         
				f = new File(
						this.target_dir.getText(),
						c.getName() + "_" + dateFormat.format(date) +
							FileTypes.get_filename_extension(FileTypes.CAMPAIGN_EXPORT_XML, HeaderBody.Body)
					);
				
				// launch a thread to handle the download
				FileOutputStream fos = new FileOutputStream(f);
				CampaignExportThread thread = new CampaignExportThread(c, fos);
				thread.start();
			}
			catch (IOException ex) {
				JOptionPane.showMessageDialog(
	    			SPECCHIOApplication.getInstance().get_frame(),
	    			ex.getMessage(),
	    			"Error",
	    			JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
	    		);
	    	}

//			this.setVisible(false);
//			this.dispose();
			
		} 	 
		if(e.getActionCommand().equals("browse"))
		{
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = fc.showOpenDialog(this);
			
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				this.target_dir.setText(file.getAbsolutePath());
				load.setEnabled(true);
			}	    	

		}
	
		
	}
	
	
	/**
	 * Dialog for handling the campaign export.
	 */
	private class CampaignExportThread extends Thread {
		
		/** the campaign to be written */
		private Campaign campaign;
		
		/** the stream to which the campaign will be written */
		private OutputStream out;
		
		/**
		 * Constructor.
		 * 
		 * @param out	the stream to which to write the campaign
		 */
		public CampaignExportThread(Campaign campaign, OutputStream out) {
			
			// save references to the input parameters
			this.campaign = campaign;
			this.out = out;
			
		}
		
		
		/**
		 * Thread entry point.
		 */
		synchronized public void run() {
			
			// create a progress report
			ProgressReportDialog pr = new ProgressReportDialog(CampaignExportDialog.this, "Downloading campaign data", false, 30);
			pr.set_operation("Waiting for the server to package the data...");
			pr.set_indeterminate(true);
			pr.setVisible(true);
			
			int cnt = 0;
			
			try {
				
				// ask the server for the campaign data
				InputStream is = specchio_client.getCampaignExportInputStream(campaign);
				
				// copy the response to a file
				pr.set_operation("Writing the campaign to a file...");
				int n;
				byte b[] = new byte[1024];
				do {
					n = is.read(b);
					if (n > 0) {
						out.write(b, 0, n);
						cnt += n;
					}
					pr.set_operation("Data written: " + cnt/1000/1000 + " MB");
				} while (n >= 0);
				out.close();
				
				// clean up
				is.close();
				
			}
			catch (IOException ex) {
				JOptionPane.showMessageDialog(
						SPECCHIOApplication.getInstance().get_frame(),
						ex.getMessage(),
						"Write error",
						JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon
					);
			}
			catch (SPECCHIOClientException ex) {
		  		ErrorDialog error = new ErrorDialog(
				    	SPECCHIOApplication.getInstance().get_frame(),
			    		"Error",
			    		ex.getUserMessage(),
			    		ex
				    );
			  		error.setVisible(true);
	    	}
			
			// close the progress report
			pr.setVisible(false);
			
			String filesize = String.format("%.2f", f.length()/1000.0/1000); // MB in SI units
			
			long len = (new File(f.getAbsolutePath()).length());
			
			// Display message that export has finished
			JOptionPane.showMessageDialog(
					(Frame)getOwner(),
					"XML file has been written, size is " + filesize + " MB",
					"Campaign Export has finished",
					JOptionPane.INFORMATION_MESSAGE, SPECCHIOApplication.specchio_icon
				);			
			
		}
		
	}


}
