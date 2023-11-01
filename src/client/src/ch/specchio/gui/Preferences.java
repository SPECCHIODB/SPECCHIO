package ch.specchio.gui;


import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.prefs.BackingStoreException;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.client.SPECCHIOPreferencesStore;

public class Preferences extends JFrame implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	SPECCHIOClient specchio_client;
	private JCheckBox asd_unit_folder;
	private JCheckBox asd_DN_folder;
	private JCheckBox asd_WR_DC;
	private JCheckBox SVC_Refl_insert;
	private JCheckBox file_loading_verbose_level;
	private JCheckBox db_config_file_creation_and_editing;
	private JCheckBox ShowJavaVersionWarning;
	private JTextField input_directory, output_directory, flox_cal_file, rox_cal_file;
	
	SPECCHIOPreferencesStore prefs;
	
	GridbagLayouter l;
	GridBagConstraints constraints;
	
	

	public Preferences() throws SPECCHIOClientException {
		super("SPECCHIO Preferences");
		
		try {
			prefs = new SPECCHIOPreferencesStore();
			
			asd_unit_folder = new JCheckBox("Create Unit Folders for old ASD file version");
			asd_unit_folder.setActionCommand("asd_unit_folder");
			asd_unit_folder.addActionListener((ActionListener) this);
			asd_unit_folder.setSelected(prefs.getBooleanPreference("CREATE_UNIT_FOLDER_FOR_OLD_ASD_FILES"));
			
			asd_DN_folder = new JCheckBox("Insert DN data for new ASD binary files");			
			asd_DN_folder.setActionCommand("asd_DN_folder");
			asd_DN_folder.addActionListener((ActionListener) this);
			asd_DN_folder.setSelected(prefs.getBooleanPreference("CREATE_DN_FOLDER_FOR_ASD_FILES"));
			asd_DN_folder.setToolTipText("DN data will be added in addition to the regular spectra.");
			
			asd_WR_DC = new JCheckBox("Insert WR and DC delta time for new ASD binary files");			
			asd_WR_DC.setActionCommand("asd_WR_DC_time");
			asd_WR_DC.addActionListener((ActionListener) this);
			asd_WR_DC.setSelected(prefs.getBooleanPreference("INSERT_WR_DC_FOR_ASD_FILES"));
			asd_WR_DC.setToolTipText("Delta time since last WR and DC will be inserted for new ASD Files; this is experimental and delta times may not be absolute due to UTC time zone problems.");


			SVC_Refl_insert = new JCheckBox("Insert reflectances for SVC files");
			SVC_Refl_insert.setActionCommand("SVC_refl_insert");
			SVC_Refl_insert.addActionListener((ActionListener) this);
			SVC_Refl_insert.setSelected(prefs.getBooleanPreference("INSERT_REFLECTANCES_FOR_SVC_FILES", true));
			SVC_Refl_insert.setToolTipText("Ignore reflectances in SVC files; useful when working in radiance or DN mode.");


			file_loading_verbose_level = new JCheckBox("File loading verbose level: include INFO messages");			
			file_loading_verbose_level.setActionCommand("VERBOSE_LEVEL_INFO");
			file_loading_verbose_level.addActionListener((ActionListener) this);
			file_loading_verbose_level.setSelected(prefs.getBooleanPreference("VERBOSE_LEVEL_INFO"));
			file_loading_verbose_level.setToolTipText("Show also info messages for each inserted file; this includes loading times per file.");			
			
			
			db_config_file_creation_and_editing = new JCheckBox("Enable editing of db_config file");
			db_config_file_creation_and_editing.setActionCommand("db_config_file_creation_and_editing");
			db_config_file_creation_and_editing.addActionListener((ActionListener) this);
			db_config_file_creation_and_editing.setSelected(prefs.getBooleanPreference("DB_CONFIG_FILE_CREATION_AND_EDITING"));
			db_config_file_creation_and_editing.setToolTipText("A text file will be created, containing your connection strings.");
	
			ShowJavaVersionWarning = new JCheckBox("Show Java version warning");
			ShowJavaVersionWarning.setActionCommand("ShowJavaVersionWarning");
			ShowJavaVersionWarning.addActionListener((ActionListener) this);
			ShowJavaVersionWarning.setSelected(prefs.getBooleanPreference("SHOW_JAVA_VERSION_WARNING", true));
			ShowJavaVersionWarning.setToolTipText("Java version warning will be shown when starting SPECCHIO.");
			
			
			input_directory = new JTextField();
			input_directory.setActionCommand("input_directory_definition");
			input_directory.addActionListener((ActionListener) this);
			input_directory.setText(prefs.getStringPreference("INPUT_DIRECTORY"));
			input_directory.setToolTipText("Defines where files, like XLS files or SPECCHIO XML files are read from by default.");

			
			output_directory = new JTextField();
			output_directory.setActionCommand("output_directory_definition");
			output_directory.addActionListener((ActionListener) this);
			output_directory.setText(prefs.getStringPreference("OUTPUT_DIRECTORY"));
			output_directory.setToolTipText("Defines where files, like XLS files or SPECCHIO XML files are written to by default.");

			flox_cal_file = new JTextField();
			flox_cal_file.setActionCommand("flox_cal_file_definition");
			flox_cal_file.addActionListener((ActionListener) this);
			flox_cal_file.setText(prefs.getStringPreference("FLOX_CAL_FILE"));
			flox_cal_file.setToolTipText("Defines the default FloX calibration file to be used if cal file is not stored alongside FloX data files.");
			
			rox_cal_file = new JTextField();
			rox_cal_file.setActionCommand("rox_cal_file_definition");
			rox_cal_file.addActionListener((ActionListener) this);
			rox_cal_file.setText(prefs.getStringPreference("ROX_CAL_FILE"));
			rox_cal_file.setToolTipText("Defines the default RoX calibration file to be used if cal file is not stored alongside RoX data files.");
			
			constraints = new GridBagConstraints();
			
			// some default values. subclasses can always overwrite these
			constraints.gridwidth = 1;
			constraints.insets = new Insets(4, 4, 4, 4);
			constraints.gridheight = 1;
			constraints.anchor = GridBagConstraints.WEST;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			
			l = new GridbagLayouter(this);
			
			
			// build GUI
			constraints.gridx = 1;
			l.insertComponent(asd_unit_folder, constraints);
			
			constraints.gridy = 2;	
			l.insertComponent(asd_DN_folder, constraints);
			
			constraints.gridy++;	
			l.insertComponent(asd_WR_DC, constraints);

			constraints.gridy++;
			l.insertComponent(SVC_Refl_insert, constraints);
			
			constraints.gridy++;	
			l.insertComponent(file_loading_verbose_level, constraints);						
						
			constraints.gridy++;	
			l.insertComponent(db_config_file_creation_and_editing, constraints);
			
			constraints.gridy++;	
			l.insertComponent(ShowJavaVersionWarning, constraints);			
			
			constraints.gridy++;	
			l.insertComponent(new JLabel("Input Directory"), constraints);
			
			constraints.gridx = 2;
			l.insertComponent(input_directory, constraints);

			constraints.gridy++;
			constraints.gridx = 1;
			l.insertComponent(new JLabel("Output Directory"), constraints);
			
			constraints.gridx = 2;
			l.insertComponent(output_directory, constraints);			
			
			constraints.gridy++;
			constraints.gridx = 1;
			l.insertComponent(new JLabel("FloX Cal. File"), constraints);
			
			constraints.gridx = 2;
			l.insertComponent(flox_cal_file, constraints);			
			
			constraints.gridy++;
			constraints.gridx = 1;
			l.insertComponent(new JLabel("RoX Cal. File"), constraints);			
			
			constraints.gridx = 2;
			l.insertComponent(rox_cal_file, constraints);


			pack();
			
			this.setVisible(true);
		
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	synchronized public void actionPerformed(ActionEvent e) 
	{

		if(e.getActionCommand().equals("asd_unit_folder"))
		{
			
			try {
				prefs.setBooleanPreference(this.asd_unit_folder.isSelected(), "CREATE_UNIT_FOLDER_FOR_OLD_ASD_FILES");				

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		if(e.getActionCommand().equals("asd_DN_folder"))
		{
			
			try {
				prefs.setBooleanPreference(this.asd_DN_folder.isSelected(), "CREATE_DN_FOLDER_FOR_ASD_FILES");				

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}	
		
		if(e.getActionCommand().equals("asd_WR_DC_time"))
		{
			
			try {
				prefs.setBooleanPreference(this.asd_WR_DC.isSelected(), "INSERT_WR_DC_FOR_ASD_FILES");				

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		if(e.getActionCommand().equals("SVC_refl_insert"))
		{

			try {
				prefs.setBooleanPreference(this.SVC_Refl_insert.isSelected(), "INSERT_REFLECTANCES_FOR_SVC_FILES");

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		if(e.getActionCommand().equals("VERBOSE_LEVEL_INFO"))
		{
			
			try {
				prefs.setBooleanPreference(this.file_loading_verbose_level.isSelected(), "VERBOSE_LEVEL_INFO");				

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}						
		
		if(e.getActionCommand().equals("db_config_file_creation_and_editing"))
		{
			
			try {
				prefs.setBooleanPreference(this.db_config_file_creation_and_editing.isSelected(), "DB_CONFIG_FILE_CREATION_AND_EDITING");				

				MainMenu menu = MainMenu.getInstance();
				menu.enable_db_config_tool(prefs.getBooleanPreference("DB_CONFIG_FILE_CREATION_AND_EDITING"));				
				
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}	
		
		if(e.getActionCommand().equals("ShowJavaVersionWarning"))
		{
			
			try {
				prefs.setBooleanPreference(this.ShowJavaVersionWarning.isSelected(), "SHOW_JAVA_VERSION_WARNING");				

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}			
		
		if(e.getActionCommand().equals("input_directory_definition"))
		{
			
			try {
				prefs.setStringPreference(this.input_directory.getText(), "INPUT_DIRECTORY");				
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}		

		if(e.getActionCommand().equals("output_directory_definition"))
		{
			
			try {
				prefs.setStringPreference(this.output_directory.getText(), "OUTPUT_DIRECTORY");				
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}		
		
		if(e.getActionCommand().equals("flox_cal_file_definition"))
		{
			
			try {
				prefs.setStringPreference(this.flox_cal_file.getText(), "FLOX_CAL_FILE");				
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}		
		
		if(e.getActionCommand().equals("rox_cal_file_definition"))
		{
			
			try {
				prefs.setStringPreference(this.rox_cal_file.getText(), "ROX_CAL_FILE");				
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}				


	}

}
