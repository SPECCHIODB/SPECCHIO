package ch.specchio.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;


import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingWorker.StateValue;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.client.SPECCHIOClientFactory;
import ch.specchio.types.Capabilities;



public class DbUpgrade extends JFrame  implements ActionListener, PropertyChangeListener{


	private static final long serialVersionUID = 1L;

	JButton ok;

	GridbagLayouter l;
	GridBagConstraints constraints;
	Double current_version = 0.0;
	boolean upgrade_needed = false;
	int error_cnt = 0;
	Double most_recent_version = 0.0;
	TreeMap<Double, String> available_upgrades = new TreeMap<Double, String>();

	private File mysql_dir;

	private SPECCHIOClient specchio_client;

	private ProgressMonitor progressMonitor;

	private dbUpgradeWorker worker;
	
	



	public DbUpgrade()
	{
		super("DB Upgrade");

		String msg = null;
		String manual_action = null;

		constraints = new GridBagConstraints();

		// some default values. subclasses can always overwrite these
		constraints.gridwidth = 1;
		constraints.insets = new Insets(4, 4, 4, 4);
		constraints.gridheight = 1;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;


		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// build GUI

		this.setLayout(new BorderLayout());


		// new panel for settings widgets
		JPanel settings_panel = new JPanel();
		l = new GridbagLayouter(settings_panel);

		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = 1;


		// get the current schema version
		specchio_client = SPECCHIOApplication.getInstance().getClient();

		String version_str = specchio_client.getCapability(Capabilities.DB_VERSION);

		if(version_str != null)
		{

			current_version = Double.valueOf(version_str);

//			current_version = 3.31; // test setup

			get_most_recent_version();

			if (current_version < this.most_recent_version)
			{
				upgrade_needed = true;
				msg = "Upgrade from Version " + Double.toString(current_version) + " to Version " + Double.toString(most_recent_version);
				
				if(current_version > 3.31 && current_version <= 3.32)
				{
					manual_action = "Please update the sdb_admin rights manually before running the automatic update." +
							"\n" +
							"Login as MySQL root and execute these statements:"+
							"\n" +"\n" +
							"GRANT SELECT, DELETE, INSERT, UPDATE, ALTER, DROP, CREATE, CREATE VIEW, GRANT OPTION, TRIGGER, REFERENCES ON `specchio`.* TO 'sdb_admin'@'localhost';\n" + 
							"GRANT SELECT, DELETE, INSERT, UPDATE, DROP, CREATE TEMPORARY TABLES, GRANT OPTION ON `specchio_temp`.* TO 'sdb_admin'@'localhost';\n" + 
							"FLUSH PRIVILEGES;";
				}
				
				
			}
			else
			{
				msg = "Database is up to date (current version = " + Double.toString(current_version) + ")";
				upgrade_needed = false;
			}



			constraints.gridx = 0;
			l.insertComponent(new JLabel(msg), constraints);	


			// create new panel for ok and cancel buttons
			JPanel control = new JPanel();
			l = new GridbagLayouter(control);

			ok = new JButton("Proceed with upgrade");
			ok.setActionCommand("ok");
			//ok.setEnabled(false); // default: not enabled
			ok.addActionListener(this);		

			constraints.gridwidth = 1;
			constraints.gridx = 0;
			constraints.gridy = 0;
			constraints.anchor = GridBagConstraints.EAST;
			l.insertComponent(ok, constraints);

			JButton cancel = new JButton("Cancel");
			cancel.setActionCommand("cancel");
			cancel.addActionListener(this);	

			constraints.gridx = 1;
			l.insertComponent(cancel, constraints);


			// new panel to hold the control and setting panel
			JPanel cs_panel = new JPanel();
			l = new GridbagLayouter(cs_panel);

			constraints.gridx = 0;
			constraints.gridy = 0;

			l.insertComponent(settings_panel, constraints);
			constraints.gridy = 1;
			l.insertComponent(control, constraints);

			// add control panel to dialog
			this.add("Center", cs_panel);
			
			if(manual_action != null)
			{
				JTextArea jp = new JTextArea(manual_action);
				this.add("South", jp);
			}

			pack();	
		}
		else
		{
			JOptionPane.showMessageDialog(
					(Frame)getOwner(),
					"Automatic DB upgrade is not possible. Please upgrade your SPECCHIO server WAR file first.",
					"Upgrade not supported by current SPECCHIO Server.",
					JOptionPane.INFORMATION_MESSAGE, SPECCHIOApplication.specchio_icon
					);						
		}

	}


	private void get_most_recent_version() {

		// parse available upgrade files to find the most recent version

		String path = SPECCHIOClientFactory.getApplicationFilepath("/mysql");


		// get all files conf/mysql directory
		mysql_dir = new File(path);

		if(!mysql_dir.exists())
		{
			// development environment
			String tmp = SPECCHIOClientFactory.getApplicationFilepath("");
			File dc10_dir = new File(tmp).getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
			mysql_dir = new File(dc10_dir.getPath() + File.separator + "conf" + File.separator + "mysql");

		}

		if(mysql_dir.exists())
		{
			class sql_filter implements FilenameFilter 
			{

				@Override
				public boolean accept(File arg0, String arg1) {

					if(arg1.endsWith(".sql") && arg1.startsWith("specchio_database_upgrade"))
					{
						return true;
					}

					return false;
				}

			}

			String[] whole_content = mysql_dir.list(new sql_filter());


			for(int i=0;i<whole_content.length;i++)
			{

				FileInputStream file_input;
				try {
					file_input = new FileInputStream ( mysql_dir.getPath() + File.separator + whole_content[i]);


					DataInputStream data_in = new DataInputStream(file_input);			

					BufferedReader d = new BufferedReader(new InputStreamReader(data_in));

					boolean not_eof = true;

					while(not_eof)
					{
						not_eof = skip_comments_and_empty_lines(d);

						if(not_eof)
						{
							String line = d.readLine();		

							if(line != null && line.startsWith("INSERT INTO `specchio`.`schema_info` (`version`, `date`) VALUES"))
							{
								// extract the version number
								String[] tokens = line.split("VALUES");

								tokens = tokens[1].split(",");
								tokens[0] = tokens[0].replace("'", "");
								String version_str = tokens[0].replace("(", "");

								Double version = Double.valueOf(version_str);

								this.available_upgrades.put(version, whole_content[i]);

								if(most_recent_version < version)
								{
									most_recent_version = version;
								}

							}

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

			}

		}
		else
		{
			// if no files are found
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



	public void actionPerformed(ActionEvent e) {
		if ("ok".equals(e.getActionCommand())) 
		{
			if (upgrade_needed)
			{
				try {
					progressMonitor = new ProgressMonitor(this,
			                "Upgrading Database",
			                "", 0, 0);
					progressMonitor.setProgress(0);
					worker = new dbUpgradeWorker(progressMonitor);
					worker.addPropertyChangeListener(this);
					worker.execute();
				}
				catch (SPECCHIOClientException ex) {
					progressMonitor.close();
					ErrorDialog error = new ErrorDialog(this, "Error", ex.getUserMessage(), ex);
					error.setVisible(true);
				}


				setVisible(false);
			}
			else
			{
				setVisible(false);
			}
		}

		if ("cancel".equals(e.getActionCommand())) {
			setVisible(false);
		}

	}

	class dbUpgradeWorker extends SwingWorker<Integer, Void> {

		private ProgressMonitor progressMonitor;

		public dbUpgradeWorker(ProgressMonitor progressMonitorIn) {

			progressMonitor = progressMonitorIn;

		}

		@Override
		public Integer doInBackground() throws SPECCHIOClientException {

			int progress = 0;
			int applicable_upgrades = 0;

			setProgress(progress);

			for(Map.Entry<Double,String> entry : available_upgrades.entrySet()) {
				Double version = entry.getKey();

				if(current_version < version)
				{
					applicable_upgrades++;
				}
			}
			

			progressMonitor.setMaximum(applicable_upgrades);
			progressMonitor.setMillisToDecideToPopup(0);
			progressMonitor.setMillisToPopup(0);
			
			
			// apply required upgrades sequentially
			for(Map.Entry<Double,String> entry : available_upgrades.entrySet()) {
				Double version = entry.getKey();
				String file = entry.getValue();

				if(current_version < version)
				{
					System.out.println("Upgrading from " + current_version + " to " + version + " using " + file);
					
					progressMonitor.setNote("Upgrading from " + current_version + " to " + version + " using " + file);
					setProgress(progress++);

					// server call for upgrade with SQL file supplied as stream
					FileInputStream fis;
					try {
						fis = new FileInputStream(mysql_dir.getPath() + File.separator + file);
						specchio_client.dbUpgrade(version, fis);
						fis.close();							
					} catch (FileNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					current_version = version;
				}

			}

			// update displayed DB info
			specchio_client.connect(); // reconnect to update capabilities
			SPECCHIOApplication.getInstance().setClient(specchio_client);
			return 1;
		}

		@Override
		public void done() {
		}
	}

	
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress".equals(evt.getPropertyName())) {
			
			progressMonitor.setProgress((Integer)evt.getNewValue());
		
		} else if ("state".equals(evt.getPropertyName())) {
			
			SwingWorker.StateValue state = (StateValue) evt.getNewValue();
	        if (state == StateValue.DONE) {

		        	progressMonitor.close();
		        	JOptionPane.showMessageDialog(
		        			this, 
		        			"Done",
		        			"Database upgraded",
		        			JOptionPane.INFORMATION_MESSAGE, SPECCHIOApplication.specchio_icon
		        		);

	        	
	        }
		}
		
	}
	

}
