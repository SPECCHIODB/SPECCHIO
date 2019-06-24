package ch.specchio.gui;

import java.awt.BorderLayout;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;


import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.client.SPECCHIOWebClientException;
import ch.specchio.metadata.MDE_Controller;
import ch.specchio.metadata.MDE_Hierarchy_Controller;
import ch.specchio.metadata.MDE_Spectrum_Controller;
import ch.specchio.metadata.MD_ChangeListener;
import ch.specchio.metadata.MD_Field;
import ch.specchio.proc_modules.VisualisationSelectionDialog;
import ch.specchio.types.ArrayListWrapper;
import ch.specchio.types.Campaign;
import ch.specchio.types.MatlabAdaptedArrayList;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.MetaSpatialGeometry;
import ch.specchio.types.Point2D;
import ch.specchio.types.hierarchy_node;
import ch.specchio.types.spectral_node_object;

public class MetaDataEditorView extends MetaDataEditorBase implements MD_ChangeListener, TreeSelectionListener, PropertyChangeListener {
	
	MDE_Spectrum_Controller mdec_s;
	MDE_Hierarchy_Controller mdec_h;
	ArrayList<MDE_Controller> MDE_Controllers;
	JTabbedPane metadata_tabs;
	CampaignMetadataPanel campaign_panel;
	HierarchyMetadataPanel hierarchy_panel;
	SpectrumMetadataPanel spectrum_panel;
	SpectrumMetadataCategoryList category_list;
	int spectrum_tab_index;
	int hierarchy_tab_index;
	int campaign_tab_index;
	ArrayList<JPanel> category_panels;
	GridBagConstraints constraints;
	GridBagConstraints category_container_constraints;
	JButton update, reset;
	Boolean update_reset_state = false;
	
	private JCheckBox conflict_detection_checkbox;

	String measurement_support = "Compute measurement support";
	String altitude_augmentation = "Augment altitude (api.geonames.org/astergdem)";
	String E_W_Switch = "Switch longitude E-W";
	String RadiometricCalibration = "Radiometric Calibration";
	private JMenuBar menuBar;
	//private ArrayList<MD_Field> shared_fields;
	private ArrayList<MD_Field> shared_fields_spectrum_level;
	//private int shared_field_opt;
	private ArrayList<MD_Field> shared_fields_hierarchy_level;
	private ArrayList<MD_Field> nonshared_fields_spectrum_level;
	private ArrayList<MD_Field> nonshared_fields_hierarchy_level;
	private ProgressMonitor progressMonitor;
	private MeasurementSupportWorker measurement_support_worker;

	public MetaDataEditorView(JFrame jFrame) throws SPECCHIOClientException {
		super("Metadata Editor");
		
		// set up controllers
		mdec_s = new MDE_Spectrum_Controller(specchio_client, this);		
		mdec_h = new MDE_Hierarchy_Controller(specchio_client, this);
		
		MDE_Controllers = new ArrayList<MDE_Controller>();
		MDE_Controllers.add(mdec_s);
		MDE_Controllers.add(mdec_h);
		
		// create menu
		JMenu menu;
		JMenuItem menuItem;

		menuBar = new JMenuBar();
		menu = new JMenu("Compute");
		
		menuItem = new JMenuItem(measurement_support);
		menuItem.addActionListener(this);
		menu.add(menuItem);
		
		menuItem = new JMenuItem(altitude_augmentation);
		menuItem.addActionListener(this);
		menu.add(menuItem);

		menuItem = new JMenuItem(E_W_Switch);
		menuItem.addActionListener(this);
		menu.add(menuItem);
		
		menuItem = new JMenuItem(RadiometricCalibration);
		menuItem.addActionListener(this);
		menuItem.setEnabled(false); // under development
		//menu.add(menuItem);		
		
		menuBar.add(menu);
		
		// create visualisation menu
		menu = new JMenu("Visualisations");
	    menuItem = new JMenuItem(VisualisationSelectionDialog.spectral_multiplot);
	    menuItem.addActionListener(this);
	    menu.add(menuItem);	    
	    
	    menuItem = new JMenuItem(VisualisationSelectionDialog.spectral_scatter_multiplot);
	    menuItem.addActionListener(this);
	    menu.add(menuItem);	   
	    
	    menuItem = new JMenuItem(VisualisationSelectionDialog.time_line_plot);
	    menuItem.addActionListener(this);
	    menu.add(menuItem);		    
	    
	    menuItem = new JMenuItem(VisualisationSelectionDialog.time_line_expl);
	    menuItem.addActionListener(this);
	    menu.add(menuItem);		
	    
	    menuItem = new JMenuItem(VisualisationSelectionDialog.sampling_points_plot);
	    menuItem.addActionListener(this);
	    menuItem.setEnabled(false);
	    menuItem.setToolTipText("Disabled - this feature will reappear in future versions");	    
	    menu.add(menuItem);	
	    
	    menuItem = new JMenuItem(VisualisationSelectionDialog.gonio_hem_expl);
	    menuItem.addActionListener(this);
	    menuItem.setEnabled(false);
	    menuItem.setToolTipText("Disabled - this feature will reappear in future versions");	    
	    menu.add(menuItem);		    
	    
	    menuBar.add(menu);		
		
		this.setJMenuBar(menuBar);
		
		// create panels
		spectrum_panel = new SpectrumMetadataPanel(this, specchio_client, mdec_s);
		hierarchy_panel  = new HierarchyMetadataPanel(this, specchio_client, mdec_h);
		campaign_panel = new CampaignMetadataPanel(this, specchio_client);
		spectrum_panel.addMetadataChangeListener(this);
		hierarchy_panel.addMetadataChangeListener(this);		
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		constraints = new GridBagConstraints();
		
		constraints.gridwidth = 1;
		constraints.insets = new Insets(4, 4, 4, 4);
		constraints.gridheight = 1;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.fill = GridBagConstraints.BOTH;
		
		category_container_constraints = (GridBagConstraints) constraints.clone();
			
		// set border layout for this dialog
		setLayout(new BorderLayout());
			
		// create scroll panes to hold metadata and category panel
		JScrollPane campaign_scroll_pane = new JScrollPane();
		campaign_scroll_pane.getViewport().add(campaign_panel);
		campaign_scroll_pane.getVerticalScrollBar().setUnitIncrement(10);
		JScrollPane hierarchy_scroll_pane = new JScrollPane();
		hierarchy_scroll_pane.getViewport().add(hierarchy_panel);
		hierarchy_scroll_pane.getVerticalScrollBar().setUnitIncrement(10);		
			
		JPanel control_panel = new JPanel();
			
		// create tabbed pane
		metadata_tabs = new JTabbedPane();

		int tab = 0;
		metadata_tabs.addTab("Campaign", campaign_scroll_pane);
		campaign_tab_index = tab++;
		metadata_tabs.addTab("Hierarchy", hierarchy_scroll_pane);
		hierarchy_tab_index = tab++;		
		metadata_tabs.addTab("Spectrum", spectrum_panel);
		spectrum_tab_index = tab++;
			
		// create browser and add to control panel			
		sdb = new SpectralDataBrowser(specchio_client, true);
		
		if (specchio_client.isLoggedInWithRole("admin"))
		{
			sdb.restrict_to_view = false;
		}
			
		// load currently selected campaign
		sdb.build_tree();
		
		control_panel.setLayout(new BorderLayout());
		control_panel.add("Center", sdb);


		// add tree listener
		sdb.tree.addTreeSelectionListener(this);
		sdb.tree.putClientProperty("browser", sdb);
		
		// add checkbox to toggle conflict detection
		conflict_detection_checkbox = new JCheckBox("Do conflict detection");
		conflict_detection_checkbox.setSelected(true);
		conflict_detection_checkbox.setActionCommand("conflict_detection_switch");
		conflict_detection_checkbox.addActionListener((ActionListener) this);
		conflict_detection_checkbox.setToolTipText("Switching off metadata conflict detection will increase selection speed,\n but only show metadata info for the first spectrum in the list");
		control_panel.add("North", conflict_detection_checkbox);
			
		// add update and reset buttons
		JPanel button_panel = new JPanel();
		GridbagLayouter button_panel_l = new GridbagLayouter(button_panel);
		constraints.gridwidth = 1;
		constraints.gridy++;
		constraints.gridx = 0;
		update = new JButton("Update");	
		update.addActionListener(this);
		button_panel_l.insertComponent(update, constraints);
		constraints.gridx++;
		reset = new JButton("Reset");
		reset.addActionListener(this);
		button_panel_l.insertComponent(reset, constraints);	
		control_panel.add("South", button_panel);

		setUpdateResetButtonsState();
		
		add("West", new JScrollPane(control_panel));
		add("Center", metadata_tabs);
		pack();
		
		setLocationRelativeTo(jFrame);
		
		setVisible(true);
		
	}

	private static final long serialVersionUID = 1L;
	
	
	
	
	private void buildGUI(boolean manual_interaction) throws SPECCHIOClientException
	{
		category_panels = new ArrayList<JPanel>();
		constraints.gridy = 0;
		
		try {
		
		// set up campaign panel
		Campaign campaign = mdec_s.getCampaign();
		if (campaign != null) {
			campaign_panel.setCampaign(campaign);
		} else {
			campaign_panel.setCampaign(null);
		}
		campaign_panel.revalidate();
		campaign_panel.repaint();
		
		spectrum_panel.updateForm();
		hierarchy_panel.updateForm();
				
		}
		catch(java.lang.NoClassDefFoundError ex)
		{
			String version_error = "";
			if(!SPECCHIOApplication.getInstance().isJava_version_is_supported())
			{
				version_error = "\nLikely reason is a wrong Java version.";
			}
			
			SPECCHIOClientException s_ex = new SPECCHIOClientException(" Java class not found. " + version_error + " (" + ex.getMessage() + ")");
			throw s_ex;
		}
		
		
//		if(!manual_interaction && form != null)
//		{
//
//			MetaTaxonomy ap_domain = (MetaTaxonomy) form.getEavParameterFromContainer("Application Domain", "General");
//
//			if(ap_domain != null)
//			{
//				// control of application domain has moved to MDE_Controller
//
////				// update the categories according to the application domain
////				ArrayList<Integer> selected_categories = null;
////				Long taxonomy_id =  (Long) ap_domain.getValue();
////
////
////				selected_categories = specchio_client.getMetadataCategoriesForApplicationDomain(taxonomy_id.intValue());
////				if(selected_categories.size()>0)
////				{
////					category_list.setSelected(selected_categories);
////					TaxonomyNodeObject tmp = specchio_client.getTaxonomyNode(taxonomy_id.intValue());
////					category_list.setApplicationDomain(tmp.getName());
////				}
////				else
////				{
////					category_list.setAllSelected(true);
////					category_list.setApplicationDomain(null);
////				}
//			}
//			else
//			{
//				// enable all categories
//				if(category_list.isApplicationDomainEnabled())
//				{
//					category_list.setAllSelected(true);
//					category_list.setApplicationDomain(null);
//					mdec_s.set_form_descriptor(category_list.getFormDescriptor(), false);
//				}
//			}
//		}
//		
		
//		form = mdec_s.getForm();	
//		
//		// set up metadata panel
//		spectrum_panel.setForm(form);
		
		
		
	}
	
	
	private void updateCampaign() throws SPECCHIOClientException
	{
		// update the campaign on the server
		mdec_s.updateCampaign(campaign_panel.getCampaign());
		
		// reset the "changed" state of the campaign panel
		campaign_panel.setCampaign(mdec_s.getCampaign());
		
		// update button states
		setUpdateResetButtonsState();
	}


	// check all fields for multiple updates and ask user if it should be applied
	private void updateChangedFields() throws SPECCHIOClientException
	{
		
		for (MDE_Controller mdec : MDE_Controllers)
		{

			// see if any of the metadata to be changed is shared between record
			shared_fields_spectrum_level = new ArrayList<MD_Field>();
			shared_fields_hierarchy_level = new ArrayList<MD_Field>();
			nonshared_fields_spectrum_level = new ArrayList<MD_Field>();
			nonshared_fields_hierarchy_level = new ArrayList<MD_Field>();			
			for (MD_Field field : mdec.getChanged_fields()) {
				if (field.getNoOfSharingRecords() != 1 && field.getNoOfSharingRecords() != field.getSelectedRecords()) {
					if(field.getLevel() == MetaParameter.SPECTRUM_LEVEL)
						shared_fields_spectrum_level.add(field);
					else
						shared_fields_hierarchy_level.add(field);
				}
				else
				{
					if(field.getLevel() == MetaParameter.SPECTRUM_LEVEL)
						nonshared_fields_spectrum_level.add(field);
					else
						nonshared_fields_hierarchy_level.add(field);					
				}
			}

//			if(shared_fields_spectrum_level.size() > 0 || mdec.getAdded_fields().size()>0 || nonshared_fields_spectrum_level.size() > 0)
			if((mdec.getAdded_fields().size()>0 || mdec.getChanged_fields().size() > 0) && (shared_fields_spectrum_level.size() > 0 || nonshared_fields_spectrum_level.size() > 0))
				doUpdate(mdec, shared_fields_spectrum_level, MetaParameter.SPECTRUM_LEVEL);

//			if(shared_fields_hierarchy_level.size() > 0 || mdec.getAdded_fields().size()>0 || nonshared_fields_hierarchy_level.size() > 0)
			if((mdec.getAdded_fields().size()>0  || mdec.getChanged_fields().size() > 0) && (shared_fields_hierarchy_level.size() > 0 || nonshared_fields_hierarchy_level.size() > 0))
				doUpdate(mdec, shared_fields_hierarchy_level, MetaParameter.HIERARCHY_LEVEL);	
			

		}
		// update button states
		setUpdateResetButtonsState();
		
	}
	
	private void doUpdate(MDE_Controller mdec, ArrayList<MD_Field> shared_fields, int level)
	{
		
		int shared_field_opt = SharedMD_Dialog.APPLY_TO_ALL;
		if (shared_fields.size() > 0) {
			// ask the user what to do with the shared fields
			SharedMD_Dialog shared_decision_dialog = new SharedMD_Dialog(this, SharedMD_Dialog.UPDATE, shared_fields, level);
			shared_decision_dialog.setVisible(true);
			shared_field_opt = shared_decision_dialog.getSelectedAction();
		}

		if (shared_field_opt != SharedMD_Dialog.APPLY_TO_NONE) {
		
		
		// Exception handling from a thread and GUI update of dialog box using swing worker
		final UpdateThread thread = new UpdateThread(this, mdec, shared_field_opt, shared_fields, level);

		Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {

			public void uncaughtException(Thread th, Throwable ex) {

				final SPECCHIOClientException e = (SPECCHIOClientException) ex;

				Runnable r = new Runnable() {

					@Override
					public void run() {
						ErrorDialog error = new ErrorDialog(SPECCHIOApplication.getInstance().get_frame(), "Error");
						error.init(e.getUserMessage(),e);							
						error.setVisible(true);	
					}

				};

				javax.swing.SwingUtilities.invokeLater(r) ; // execute r's run method on the swing thread


			};

		};					





		thread.setUncaughtExceptionHandler(h);
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}		
		
	}
	
	
	private void updateChangedAnnotations() throws SPECCHIOWebClientException {
		for (MDE_Controller mdec : MDE_Controllers)
		{
		
			for (MD_Field field : mdec.getChanged_annotations()) {
			
				mdec.updateAnnotation(field);
			}
			
			// reset changed lists
			mdec.getChanged_annotations().clear();		
		
		}
		
		// update button states
		setUpdateResetButtonsState();		
	}	
	
	// check all fields for multiple updates and ask user if it should be applied
	private void removeFields() throws SPECCHIOClientException
	{
		for (MDE_Controller mdec : MDE_Controllers)
		{

			// see if any of the metadata to be changed is shared between entities
			ArrayList<MD_Field> shared_spectrum_fields = new ArrayList<MD_Field>();
			ArrayList<MD_Field> shared_hierarchy_fields = new ArrayList<MD_Field>();
			for (MD_Field field : mdec.getRemoved_fields()) {
				if (field.getNoOfSharingRecords() > 1) {
					if(field.getLevel() == MetaParameter.SPECTRUM_LEVEL)
						shared_spectrum_fields.add(field);
					else
						shared_hierarchy_fields.add(field);
				}
			}

			int shared_spectrum_field_opt = SharedMD_Dialog.APPLY_TO_ALL;
			if (shared_spectrum_fields.size() > 0) {
				// ask the user what to do with the shared fields
				SharedMD_Dialog shared_decision_dialog = new SharedMD_Dialog(this, SharedMD_Dialog.DELETE, shared_spectrum_fields, MetaParameter.SPECTRUM_LEVEL);
				shared_decision_dialog.setVisible(true);
				shared_spectrum_field_opt = shared_decision_dialog.getSelectedAction();
			}
			
			removeFieldsBasedOnDecision(mdec, shared_spectrum_field_opt, MetaParameter.SPECTRUM_LEVEL);
			
			int shared_hierarchy_field_opt = SharedMD_Dialog.APPLY_TO_ALL;
			if (shared_hierarchy_fields.size() > 0) {
				// ask the user what to do with the shared fields
				SharedMD_Dialog shared_decision_dialog = new SharedMD_Dialog(this, SharedMD_Dialog.DELETE, shared_hierarchy_fields, MetaParameter.HIERARCHY_LEVEL);
				shared_decision_dialog.setVisible(true);
				shared_hierarchy_field_opt = shared_decision_dialog.getSelectedAction();
			}
						
			removeFieldsBasedOnDecision(mdec, shared_hierarchy_field_opt, MetaParameter.HIERARCHY_LEVEL);



		}
		
		// reset the redundancy buffer to avoid linking to deleted parameters.
		// ideally, this should only include the reset for the fields that were removed and not the whole buffer ...
		this.specchio_client.clearMetaparameterRedundancyList();		

		// update button states
		setUpdateResetButtonsState();
		
	}	
	
	
	public void removeFieldsBasedOnDecision(MDE_Controller mdec, int shared_field_opt, int level)
	{
		if (shared_field_opt != SharedMD_Dialog.APPLY_TO_NONE) {
			for (MD_Field field : mdec.getRemoved_fields(level)) {

				if(field.getNoOfSharingRecords() == 1 && mdec.getIds().size() == 1)
				{
					mdec.remove(field);
				}
				else if(field.getNoOfSharingRecords() == 1 && mdec.getIds().size() > 1)
				{
					// multiple metaparameters need removing (different parameters per spectrums)
					mdec.remove_all_mps_of_attribute(field);				
				}
				else
				{
					if(shared_field_opt == SharedMD_Dialog.APPLY_TO_ALL)
					{
						mdec.remove(field);
					}
					else if (shared_field_opt == SharedMD_Dialog.APPLY_TO_SELECTION)
					{
						mdec.remove_selection(field);
					}

				}

			}

			// reset the list of deleted fields
			mdec.clearRemoved_fields(level);

		}		
		
		
	}
	
	
	public ArrayList<Integer> get_ids_matching_query()
	{
		if(!this.sorted_ids_ready)
		{
			reloadGUI();
		}
		
		return ids_matching_query;
	}
		
	

	public SpectrumMetadataCategoryList getCategory_list() {
		return category_list;
	}


	public void setCategory_list(SpectrumMetadataCategoryList category_list) {
		this.category_list = category_list;
	}


	public void actionPerformed(ActionEvent e) {
		
		Object c = e.getSource();
		
		if(measurement_support.equals(e.getActionCommand()))
		{
			try {
				System.out.println(e.getActionCommand());
				compute_measurement_support();
			}
			catch (SPECCHIOClientException ex) {
				ErrorDialog error = new ErrorDialog(this, "Error", ex.getUserMessage(), ex);
				error.setVisible(true);	
			}
		}    
		
		if(altitude_augmentation.equals(e.getActionCommand()))
		{
			try {
				System.out.println(e.getActionCommand());
				augment_altitude();
			}
			catch (SPECCHIOClientException ex) {
				ErrorDialog error = new ErrorDialog(this, "Error", ex.getUserMessage(), ex);
				error.setVisible(true);	
			}
		}    
		
		if(E_W_Switch.equals(e.getActionCommand()))
		{
			try {
				System.out.println(e.getActionCommand());
				switch_ew_lon();
			}
			catch (SPECCHIOClientException ex) {
				ErrorDialog error = new ErrorDialog(this, "Error", ex.getUserMessage(), ex);
				error.setVisible(true);	
			}
		}    
		
		if(RadiometricCalibration.equals(e.getActionCommand()))
		{
			try {
				System.out.println(e.getActionCommand());
				
				// requirements:
				// - DN data must be selected
				// - instrument must have a calibration available for the selected timeframe
				
				// for ASD spectrometer only at this point:
				
				// get the calibration number of the instrument used for these spectra 
				
				
				// get calibration ids
				//ArrayList<Integer> cal_ids = this.specchio_client.getCalibrationIds(mdec.getIds());
				
				// get calibrated instruments and coefficients
				
				int i = 0;
				
				
			}
			catch (SPECCHIOClientException ex) {
				ErrorDialog error = new ErrorDialog(this, "Error", ex.getUserMessage(), ex);
				error.setVisible(true);	
			}
		}    
		
		
		
		
	      if(VisualisationSelectionDialog.gonio_hem_expl.equals(e.getActionCommand()) 
	    		  || VisualisationSelectionDialog.sampling_points_plot.equals(e.getActionCommand())
	    	  || VisualisationSelectionDialog.time_line_plot.equals(e.getActionCommand())
	    	  || VisualisationSelectionDialog.time_line_expl.equals(e.getActionCommand()))
	      {	 
	    	  startOperation();
	    	  VisualisationThread thread = new VisualisationThread(
	    			  e.getActionCommand(),
	    			  get_ids_matching_query(),
    				  split_spaces_by_sensor.isSelected(),
    				  split_spaces_by_sensor_and_unit.isSelected(),
    				  sdb.get_order_by_field()
    			);
	    	  thread.start();
	    	  endOperation();	 			  
	      }	      
	      
	      
	      if(VisualisationSelectionDialog.spectral_multiplot.equals(e.getActionCommand())
	    	  || VisualisationSelectionDialog.spectral_scatter_multiplot.equals(e.getActionCommand()))
	      {	 
	    	  startOperation();
	    	  VisualisationThread thread = new VisualisationThread(
	    			  e.getActionCommand(),
	    			  this.get_ids_matching_query_not_sorted(),
    				  split_spaces_by_sensor.isSelected(),
    				  split_spaces_by_sensor_and_unit.isSelected(),
    				  sdb.get_order_by_field()
    			);
	    	  thread.start();
	    	  endOperation();	 			  
	      }			
		
		
		
		if(c == update)
		{
			startOperation();
			try {
				if (metadata_tabs.getSelectedIndex() == campaign_tab_index) {
					updateCampaign();
				} else if (metadata_tabs.getSelectedIndex() >= hierarchy_tab_index) {
					updateChangedFields();
					updateChangedAnnotations();
					removeFields();
				}
			}
	  		catch (SPECCHIOClientException ex) {
				ErrorDialog error = new ErrorDialog(this, "Error", ex.getUserMessage(), ex);
				error.setVisible(true);	
		    }
			endOperation();
		}
		else if(c == reset)
		{
			startOperation();
			if (metadata_tabs.getSelectedIndex() == campaign_tab_index) {
				
				// reset the campaign panel with the original campaign object
				campaign_panel.setCampaign(mdec_s.getCampaign());
			
			} else if (metadata_tabs.getSelectedIndex() >= hierarchy_tab_index) {
				
				try {
			
					for (MDE_Controller mdec : MDE_Controllers)
					{					
						// remove all new fields from the form
						mdec.remove_all_added_fields();
						mdec.clear_changed_field_lists();					
					}
					
					buildGUI(false);
					
				}
		  		catch (SPECCHIOClientException ex) {
					ErrorDialog error = new ErrorDialog(this, "Error", ex.getUserMessage(), ex);
					error.setVisible(true);	
			    }
			}
			
			// update button states
			setUpdateResetButtonsState();
			
			endOperation();
		}
		else if(c == this.conflict_detection_checkbox)
		{
			mdec_s.setDo_conflict_detection(conflict_detection_checkbox.isSelected());			
		}
		
	}
	



	private void switch_ew_lon() {
		
		ArrayList<Integer> ids_with_lon = specchio_client.filterSpectrumIdsByHavingAttribute(mdec_s.getIds(), "Longitude");
		
		if(ids_with_lon.size() > 0)
		{
			// get values
			
			// get existing metaparameters
			
			ArrayList<MetaParameter> existing_parameters = specchio_client.getMetaparameters(ids_with_lon, "Longitude");
					
			for(int i=0;i<ids_with_lon.size();i++)
			{
				
				MetaParameter mp = existing_parameters.get(i);
				
				try {
					ArrayList<Integer> ids = new ArrayList<Integer>();
					ids.add(ids_with_lon.get(i));					
					mp.setValue((Double) mp.getValue()*(-1));
					
					specchio_client.updateEavMetadata(mp, ids);
					
					
				} catch (MetaParameterFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
			
			}
			
			//this.reloadGUI();
						
		
		}
		
		ArrayList<String> attribute_names = new ArrayList<String>();
		attribute_names.add("Spatial Extent");
		attribute_names.add("Spatial Position");
		attribute_names.add("Spatial Transect");
		
		Iterator<String> attr_it = attribute_names.iterator();
		
		while(attr_it.hasNext())
		{
			String attr_name = attr_it.next();
			ids_with_lon = specchio_client.filterSpectrumIdsByHavingAttribute(mdec_s.getIds(),attr_name);
			
			if(ids_with_lon.size() > 0)
			{
				// get values
				
				// get existing metaparameters				
				ArrayList<MetaParameter> existing_parameters = specchio_client.getMetaparameters(ids_with_lon, attr_name);
						
				for(int i=0;i<ids_with_lon.size();i++)
				{
					
					MetaSpatialGeometry mp = (MetaSpatialGeometry) existing_parameters.get(i);
					
						ArrayList<Integer> ids = new ArrayList<Integer>();
						ids.add(ids_with_lon.get(i));	
						
						@SuppressWarnings("unchecked")
						ArrayListWrapper<Point2D> coords = (ArrayListWrapper<Point2D>) mp.getValue();
						
						Iterator<Point2D> coords_it = coords.getList().iterator();
						
						while(coords_it.hasNext())
						{
							Point2D coord = coords_it.next();
							coord.setX(coord.getX()*(-1));
						}
						
						specchio_client.updateEavMetadata(mp, ids);
						

				
				}
				
				
							
			
			}	
			
			this.reloadGUI();
		
		}
		
		
	}


	private void augment_altitude() {
				
		ArrayList<Integer> ids_with_lat_lon = specchio_client.filterSpectrumIdsByHavingAttribute(mdec_s.getIds(), "Spatial Position");
		
		if(ids_with_lat_lon.size() > 0)
		{
			// get values
			MatlabAdaptedArrayList<Object> pos = specchio_client.getMetaparameterValues(ids_with_lat_lon, "Spatial Position");
			
			// get existing metaparameters
			ArrayList<Integer> attribute_ids = new ArrayList<Integer>();
			attribute_ids.add(specchio_client.getAttributesNameHash().get("Altitude").getId());
			
			ArrayList<MetaParameter> existing_parameters = specchio_client.getMetaparameters(ids_with_lat_lon, "Altitude");
			
			double prev_lat = 0;
			double prev_lon = 0;
			double prev_alt = 0;
			double altitude = 0;
			
			URL url;
			try {
				for(int i=0;i<ids_with_lat_lon.size();i++)
				{
					
					@SuppressWarnings("unchecked")
					ArrayListWrapper<Point2D> cur_pos_wrapper = (ArrayListWrapper<Point2D>) pos.get(i);
					Point2D cur_pos = cur_pos_wrapper.getList().get(0);

					if(prev_lat == (Double) cur_pos.getY() && prev_lon == (Double) cur_pos.getX())
					{
						prev_alt = altitude;
						
					}
					else
					{
						prev_lat = (Double) cur_pos.getY();
						prev_lon = (Double) cur_pos.getX();

						url = new URL("http://api.geonames.org/astergdem?lat=" +cur_pos.getY()+"&lng="+((Double) cur_pos.getX())+"&username=specchio");

						String alt = new Scanner(url.openStream(), "UTF-8").useDelimiter("\\A").next();

						altitude = Double.valueOf(alt);
						prev_alt = altitude;
					}

					ArrayList<Integer> ids = new ArrayList<Integer>();
					try {
						ids.add(ids_with_lat_lon.get(i));
						
						MetaParameter mp = existing_parameters.get(i);
						
						if(mp.getValue() != null)
						{
							mp.setValue(altitude);
						}
						else				
							mp = MetaParameter.newInstance(specchio_client.getAttributesNameHash().get("Altitude"), altitude);
						
						specchio_client.updateEavMetadata(mp, ids);
						
					} catch (SPECCHIOClientException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (MetaParameterFormatException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}				
					
				}
				
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			this.reloadGUI();
			
		}
		

	}
	
	



	private void compute_measurement_support() {
		
		// to compute a measurement support, we need the following parameters:
		// distance of sensor to target
		// sensor zenith
		// sensor fov
		
		
		
		progressMonitor = new ProgressMonitor(this,
                "Computing measurement support ...",
                "", 0, 0);
		progressMonitor.setProgress(0);
		measurement_support_worker = new MeasurementSupportWorker(progressMonitor);
		measurement_support_worker.addPropertyChangeListener((PropertyChangeListener) this);
		measurement_support_worker.execute();

		
		
			
			
			
		
	}
	
	
	class MeasurementSupportWorker extends SwingWorker<Integer, Void> {
		
		private ProgressMonitor progressMonitor;
		private int number_of_updated_spectra;
		
		public MeasurementSupportWorker(ProgressMonitor progressMonitorIn) {
			
			progressMonitor = progressMonitorIn;
			
		}

		@Override
		public Integer doInBackground() {

			int progress = 0;

			setProgress(progress);
			
			progressMonitor.setMaximum( mdec_s.getIds().size() + 1); // add 1 to ensure the progress monitor stays open till data reloaded in metadata editor
			progressMonitor.setMillisToDecideToPopup(0);
			progressMonitor.setMillisToPopup(0);	
			

			// check if calculations are to be done on hierarchy or spectrum level
			number_of_updated_spectra = 0;
			
			int level = MetaParameter.SPECTRUM_LEVEL;
			if(mdec_s.getOnlyHierarchiesAreSelected())
			{
				level = MetaParameter.HIERARCHY_LEVEL;
				
				
				
				// treat each hierarchy separately (helps likely to have distinct geometries per hierarchy, thus reducing the metadata entries
				for(int i=0;i<mdec_h.getIds().size();i++)
				{
					
					spectral_node_object sn = new hierarchy_node();
					sn.setId(mdec_h.getIds().get(i));
					
					List<Integer> initial_ids = specchio_client.getSpectrumIdsForNode(sn);
								
					ArrayList<Integer> initial_ids_ = new ArrayList<Integer>(initial_ids);
				
					number_of_updated_spectra += compute_and_insert_measurement_support(initial_ids_, sn.getId(), level);
					
					progressMonitor.setProgress(number_of_updated_spectra);
				}
				
				
			}
			else
			{
				
				number_of_updated_spectra = compute_and_insert_measurement_support(mdec_s.getIds(), 0, level);
				
			}	
			
			progressMonitor.setNote("Reloading metadata in editor ...");			
		
			reloadGUI();

			return number_of_updated_spectra;
	
		}
		
		
		private int compute_and_insert_measurement_support(ArrayList<Integer> initial_spectrum_ids, int hierarchy_id, int level) {
			
			
			
			boolean identical_geometry = false; // inital assumption: geometry differs between spectra
			
			int progress_increase = initial_spectrum_ids.size() / 3;
			
			
			// get hierarchy_name if this is a hierarchy update
			String hierarchy_node_info_str = "";
			if(hierarchy_id > 0)
			{
				hierarchy_node_info_str = " for " + specchio_client.getHierarchyName(hierarchy_id);
			}
			
			
			
			progressMonitor.setNote("Checking required parameters (sensor distance, zenith angle and  FOV)" + hierarchy_node_info_str);
			progressMonitor.setProgress(number_of_updated_spectra + progress_increase);
			
			ArrayList<Integer> ids_with_dist = specchio_client.filterSpectrumIdsByHavingAttribute(initial_spectrum_ids, "Sensor Distance");
			ArrayList<Integer> ids_with_dist_and_zenith = specchio_client.filterSpectrumIdsByHavingAttribute(ids_with_dist, "Sensor Zenith");
			ArrayList<Integer> ids_with_dist_and_zenith_and_fov = specchio_client.filterSpectrumIdsByHavingAttribute(ids_with_dist_and_zenith, "FOV");
			
			ArrayList<Double> radii = new ArrayList<Double>();
			ArrayList<Double> major_axes = new ArrayList<Double>();
			ArrayList<Double> minor_axes = new ArrayList<Double>();
			ArrayList<Double> areas_nadir = new ArrayList<Double>();
			ArrayList<Double> areas_oblique = new ArrayList<Double>();
//			ArrayList<Integer> ids_nadir = new ArrayList<Integer>();
//			ArrayList<Integer> ids_oblique = new ArrayList<Integer>();
			
			if(ids_with_dist_and_zenith_and_fov.size() > 0)
			{
				progressMonitor.setNote("Getting required parameters (sensor distance, zenith angle and  FOV)"  + hierarchy_node_info_str);
				progressMonitor.setProgress((number_of_updated_spectra + 2*progress_increase));
				// get values
				MatlabAdaptedArrayList<Object> dist = specchio_client.getMetaparameterValues(ids_with_dist_and_zenith_and_fov, "Sensor Distance");
				MatlabAdaptedArrayList<Object> zen = specchio_client.getMetaparameterValues(ids_with_dist_and_zenith_and_fov, "Sensor Zenith");
				MatlabAdaptedArrayList<Object> fov = specchio_client.getMetaparameterValues(ids_with_dist_and_zenith_and_fov, "FOV");
				
				// check if all parameters are the same
				MatlabAdaptedArrayList<Object> dist_distinct = specchio_client.getMetaparameterValues(ids_with_dist_and_zenith_and_fov, "Sensor Distance", true);
				MatlabAdaptedArrayList<Object> zen_distinct = specchio_client.getMetaparameterValues(ids_with_dist_and_zenith_and_fov, "Sensor Zenith", true);
				MatlabAdaptedArrayList<Object> fov_distinct = specchio_client.getMetaparameterValues(ids_with_dist_and_zenith_and_fov, "FOV", true);		
				
				if(dist_distinct.size()==1 && zen_distinct.size()==1 && fov_distinct.size()==1)
				{
					identical_geometry = true;
					
					dist = dist_distinct;
					zen = zen_distinct;
					fov = fov_distinct;
				}
				
				// get existing metaparameters
//				ArrayList<Integer> attribute_ids = new ArrayList<Integer>();
//				attribute_ids.add(specchio_client.getAttributesNameHash().get("Measurement Support Radius").getId());
//				attribute_ids.add(specchio_client.getAttributesNameHash().get("Measurement Support Area").getId());
//				attribute_ids.add(specchio_client.getAttributesNameHash().get("Measurement Support Major Axis").getId());
//				attribute_ids.add(specchio_client.getAttributesNameHash().get("Measurement Support Minor Axis").getId());
//				
//				ArrayList<ArrayList<MetaParameter>> existing_parameters = specchio_client.getMetaparameters(ids_with_dist_and_zenith_and_fov, attribute_ids);
				
				// calculate the footprint, where the circle is a special form of the ellipse
				progressMonitor.setNote("Calculating and inserting footprints" + hierarchy_node_info_str);
				progressMonitor.setProgress((number_of_updated_spectra + 2*progress_increase));
				for(int i=0;i<zen.size();i++)
				{
//					double d = (Double) dist.get(i);
//					double phi = Math.toRadians((Double) zen.get(i));
//					double c = d * Math.sin(phi);
//					double fov_ = Math.toRadians(((Integer) fov.get(i)).doubleValue());
//					double h = Math.cos(phi) * d;
//					double e = h  * Math.tan(phi - fov_/2);				
//					double a = round(c - e,3);				
//					double b = round(d * Math.tan(fov_ / 2),3);
//					
//					
//					double A = round(Math.PI * a * b, 3);
					
					double h = (Double) dist.get(i); // distance to target plane
					double phi = Math.toRadians((Double) zen.get(i));
					double c = Math.tan(phi)*h; // distance from centre of ellipse to nadir
					double fov_ = Math.toRadians(((Integer) fov.get(i)).doubleValue());
					double d = Math.sqrt(h*h + c*c); // distance of sensor to middle of projected FOV (i.e. ellipse)
					double e = h  * Math.tan(phi - fov_/2);	// distance from nadir to start of ellipse
					double a = round(c - e,3);	
					double b = round(d * Math.tan(fov_ / 2),3);
					
					double A = round(Math.PI * a * b, 3);
					
					MetaParameter mp;
					ArrayList<Integer> ids = new ArrayList<Integer>();
					if(identical_geometry && mdec_s.getOnlyHierarchiesAreSelected())
						ids.add(hierarchy_id);
					else if (identical_geometry)
						ids = ids_with_dist_and_zenith_and_fov; // single update call for all spectra
					else
					{
						ids.add(ids_with_dist_and_zenith_and_fov.get(i));
						
						// only single spectra update currently supported in case of mixed cases
						level =  MetaParameter.SPECTRUM_LEVEL;
					}
					
					
					if(phi == 0)
					{
//						ids_nadir.add(ids_with_dist_and_zenith_and_fov.get(i));
						radii.add(a);
						areas_nadir.add(A);
						
						try {
								
							
//							mp = existing_parameters.get(0).get(i);
//							
//							if(mp.getValue() != null)
//							{
//								mp.setValue(a);
//							}
//							else				
								mp = MetaParameter.newInstance(specchio_client.getAttributesNameHash().get("Measurement Support Radius"), a);
								mp.setLevel(level);
//							
//							specchio_client.updateEavMetadata(mp, ids);
							
							specchio_client.updateOrInsertEavMetadata(mp, ids);

//							mp = existing_parameters.get(1).get(i);
//							
//							if(mp.getValue() != null)
//							{
//								mp.setValue(A);
//							}
//							else							
								mp = MetaParameter.newInstance(specchio_client.getAttributesNameHash().get("Measurement Support Area"), A);
								mp.setLevel(level);
//							
//							specchio_client.updateEavMetadata(mp, ids);
								
								specchio_client.updateOrInsertEavMetadata(mp, ids);
							
						} catch (SPECCHIOClientException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (MetaParameterFormatException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}				
						
					}
					else
					{
//						ids_oblique.add(ids_with_dist_and_zenith_and_fov.get(i));				
						major_axes.add(a);
						minor_axes.add(b);
						areas_oblique.add(A);
						
						try {

							mp = MetaParameter.newInstance(specchio_client.getAttributesNameHash().get("Measurement Support Major Axis"), a);
							mp.setLevel(level);
							specchio_client.updateOrInsertEavMetadata(mp, ids);

							mp = MetaParameter.newInstance(specchio_client.getAttributesNameHash().get("Measurement Support Minor Axis"), b);
							mp.setLevel(level);
							specchio_client.updateOrInsertEavMetadata(mp, ids);
							
							mp = MetaParameter.newInstance(specchio_client.getAttributesNameHash().get("Measurement Support Area"), A);
							mp.setLevel(level);
							specchio_client.updateOrInsertEavMetadata(mp, ids);
							
						} catch (SPECCHIOClientException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (MetaParameterFormatException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}					
						
						
					}
				
				}
				
				// future batch update below ....
//				// insert 
//				if(ids_nadir.size()>0)
//				{
//					specchio_client.upda
//				}
//				
//				if(ids_oblique.size()>0)
//				{
//					
//				}
				
				
				
			}		
			
			return ids_with_dist_and_zenith_and_fov.size();
			
			
		}		
		
	}
	
	
	

	public static double round(double value, int places) {
		BigDecimal bd = new BigDecimal(value);
		bd = bd.round(new MathContext(places));
		double rounded = bd.doubleValue();				

		return rounded;
	}	

//	public void valueChanged(ListSelectionEvent arg0) {
//		
//		startOperation();
//		try {
//			mdec_s.set_form_descriptor(category_list.getFormDescriptor(), true);
//			form = mdec_s.getForm();
//			
//			buildGUI(true);
//		}
//  		catch (SPECCHIOClientException ex) {
//			ErrorDialog error = new ErrorDialog(this, "Error", ex.getUserMessage(), ex);
//			error.setVisible(true);
//
//	    }
//		endOperation();
//		
//	}

	public void valueChanged(TreeSelectionEvent arg0) {
		
		reloadGUI();
		
	}
	
	
	private void reloadGUI()
	{
		
		startOperation();
		try {
			ArrayList<Integer> spectrum_ids = sdb.get_selected_spectrum_ids();
			ArrayList<Integer> hierarchy_ids = sdb.get_selected_hierarchy_ids();
			Campaign campaign = sdb.get_selected_campaign();
			
			if(sdb.onlyCampaignNodeIsSelected())
			{
				// all top hierarchies need selecting
				hierarchy_ids = sdb.get_top_hierarchy_ids_of_campaign();
			}
	
			ids_matching_query = spectrum_ids;
			sorted_ids_ready = true;
			mdec_s.set_campaign(campaign);
			mdec_h.set_hierarchy_ids(hierarchy_ids);
			mdec_s.set_spectrum_ids(spectrum_ids);		
			mdec_s.set_hierarchy_ids(hierarchy_ids);
			mdec_s.setOnlyHierarchiesAreSelected(sdb.onlyHierarchiesAreSelected());
			
			for (MDE_Controller mdec : MDE_Controllers)
			{
				mdec.clear_changed_field_lists();
			}
			
			campaign_panel.setCampaign(campaign);
			this.setUpdateResetButtonsState();		
				
			buildGUI(false);
		}
  		catch (SPECCHIOClientException ex) {
			ErrorDialog error = new ErrorDialog(this, "Error", ex.getUserMessage(), ex);
			error.setVisible(true);			
	    }
		endOperation();				
		
	}
	
	
	public void campaignDataChanged() {
		
		setUpdateResetButtonsState();
		
	}
	
	
	private void setUpdateResetButtonsState()
	{
		if (metadata_tabs.getSelectedIndex() == campaign_tab_index) {
			update_reset_state = campaign_panel.hasUnsavedChanges();
		} else if (metadata_tabs.getSelectedIndex() >= hierarchy_tab_index) {
			update_reset_state = mdec_s.hasChanges() || mdec_h.hasChanges();
		}
		
		update.setEnabled(update_reset_state);
		if(update_reset_state)
		{				
			update.setForeground(Color.RED);
		}
		else
		{
			update.setForeground(Color.BLACK);
		}
		reset.setEnabled(update_reset_state);
	}
	
	
	public void metadataFieldAdded(MD_Field field)
	{
		setUpdateResetButtonsState();
	}
	
	
	public void metadataFieldChanged(MD_Field field, Object new_value)
	{	
		setUpdateResetButtonsState();
	}
	
	public void metadataFieldRemoved(MD_Field field)
	{
		setUpdateResetButtonsState();
	}
	
	
	@Override
	public void metadataFieldAnnotationChanged(MD_Field field, String annotation) 
	{	
		setUpdateResetButtonsState();		
	}	
	
	
	/**
	 * Handler for ending a potentially long-running operation.
	 */
	private void endOperation() {
		
		// change the cursor to its default start
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		
	}
	
	
	/**
	 * Handler for starting a potentially long-running operation.
	 */
	private void startOperation() {
		
		// change the cursor to its "wait" state
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
	}

	/**
	 * Worker thread for performing the actual calculations.
	 */
	private class UpdateThread extends Thread {
		
		MetaDataEditorView mde;
		MDE_Controller mdec;
		private int shared_field_opt;
		private ArrayList<MD_Field> shared_fields;
		private int level;
		
		/**
		 * Constructor.
		 * @param shared_fields 
		 * 
		 * @param spectrumIdsIn	the list of spectrum identifiers to be processed
		 */
		public UpdateThread(MetaDataEditorView mde, MDE_Controller mdec, int shared_field_opt, ArrayList<MD_Field> shared_fields, int level) {
						
			super();
			this.mde = mde;
			this.mdec = mdec;
			this.shared_field_opt = shared_field_opt;
			this.shared_fields = shared_fields;
			this.level = level;
		}
		
		
		/**
		 * Thread entry point.
		 */
		public void run() throws SPECCHIOClientException {
			
			sdb.tree.removeTreeSelectionListener(mde); // disable selections in the databrowser to avoid conflicts during updates
			

			ProgressReportDialog pr = new ProgressReportDialog(mde, "Metadata Update", true, 20);
			//pr.setToDocumentModal(); // prevent from other clicks in metadata editor during update
			pr.set_operation("Updating metadata ...");
			pr.setVisible(true);
			//pr.set_min_max(1, changed_fields.size());
			int progress = 0;
			
			// perform updates
			for (MD_Field field : mdec.getChanged_fields(level)) {
				pr.set_component(field.getLabel());
				
				if (shared_field_opt == SharedMD_Dialog.APPLY_TO_ALL) {
					mdec.update(field);
				} else if (shared_field_opt == SharedMD_Dialog.APPLY_TO_SELECTION) {
					mdec.update_selection(field);
				}
				pr.set_progress(++progress/mdec.getChanged_fields().size()*100);
			}


			progress = 0;

			// perform updates: it appeared at some point that some added fields were not contained in the changed fields list ....
//			for (MD_Field field : mdec.getAdded_fields()) {
//				pr.set_component(field.getLabel());
//
//				mdec.update(field);
//
//				pr.set_progress(++progress/mdec.getChanged_fields().size()*100);
//			}			

			pr.setVisible(false);

			sdb.tree.addTreeSelectionListener(mde); 
			
			// reset changed lists
			mdec.clearChanged_fields(level);
			mdec.clearAdded_fields(level);
			
			
			
		}
		
	}




	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress".equals(evt.getPropertyName())) {
			
			System.out.print("Progress: " + (Integer)evt.getNewValue());
			
			progressMonitor.setProgress((Integer)evt.getNewValue());
		
		} else if ("state".equals(evt.getPropertyName())) {
			
			SwingWorker.StateValue state = (StateValue) evt.getNewValue();
			if (state == StateValue.DONE) {
				
				int number_of_updated_spectra;
				try {
					number_of_updated_spectra = measurement_support_worker.get();

					progressMonitor.close();

					String message = "Measurement support of " + Integer.toString(number_of_updated_spectra) + " spectra successfully computed.";

					if(number_of_updated_spectra != mdec_s.getIds().size())
					{
						message = message + "\nMeasurement support of " + (mdec_s.getIds().size() - number_of_updated_spectra) + " spectra could not be computed due to missing metadata \n(Zenith angle, FOV and Sensor Distance are required).";
					}

					JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE, SPECCHIOApplication.specchio_icon);

					
					

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

	        }
		}
		
		
	}


}



