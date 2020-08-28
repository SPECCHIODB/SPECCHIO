package ch.specchio.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import ch.specchio.client.*;
import ch.specchio.file.reader.campaign.*;
import ch.specchio.types.Campaign;

/**
 * Dialogue for loading campaign data,
 */
public class LoadCampaignDataDialog extends JDialog implements ActionListener {
	
	/** serialisation version identifier */
	private static final long serialVersionUID = 1L;
	
	/** client object */
	private SPECCHIOClient specchioClient;
	
	/** the currently-selected campaign */
	private Campaign campaign;
	
	/** campaign selection label */
	private JLabel campaignLabel;
	
	/** campaign selection combo box */
	private JComboBox campaignCombo;
	
	/** path selection label */
	private JLabel pathListLabel, UnavailablePathListLabel;
	
	/** path selection panel */
	private CampaignPathPanel pathPanel, unavailablePathPanel;
	
	/** "load" button */
	private JButton loadButton;
	
	/** "cancel" button */
	private JButton cancelButton;

	private SpectralDataBrowser sdb;

	private JCheckBox simple_loading_checkbox;

	private JPanel pathroot_scroll_panel;

	private JPanel exist_h_loading_scroll_panel;
	
	/** command for a combo box selection */
	private static final String SELECT_CAMPAIGN = "Select campaign";
	
	/** text for the "load" button */
	private static final String LOAD = "Load";
	
	/** text for the "cancel" button */
	private static final String CANCEL = "Cancel";
	
	/** text for the "load" button */
	private static final String LOAD_SDB = "Load_from_SDB";
	
	
	/**
	 * Constructor.
	 * 
	 * @param owner	the dialogue's owner
	 * @param modal	make the dialogue modal?
	 * 
	 * @throws SPECCHIOClientException	could not get campaign information from the server
	 */
	public LoadCampaignDataDialog(Frame owner, boolean modal) throws SPECCHIOClientException {
		
		super(owner, "Load Spectral Data", modal);

		// get a reference to the application's client object
		specchioClient = SPECCHIOApplication.getInstance().getClient();
		
		// set up the root panel with a gridbag layout
		JPanel rootPanel = new JPanel();
		rootPanel.setLayout(new GridBagLayout());
		getContentPane().add(rootPanel);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(4, 4, 4, 4);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridx = 0;
		constraints.gridy = 0;
		
		// setup sub panels
		pathroot_scroll_panel = new JPanel();
		pathroot_scroll_panel.setLayout(new GridBagLayout());
		exist_h_loading_scroll_panel = new JPanel();
		exist_h_loading_scroll_panel.setLayout(new GridBagLayout());		
		
		// add the campaign selection combo box
		JPanel campaign_panel = new JPanel();
		campaign_panel.setLayout(new GridBagLayout());
		constraints.gridx = 0;
		campaignLabel = new JLabel("Campaign name:");
		campaign_panel.add(campaignLabel, constraints);
		constraints.gridx = 1;
		campaignCombo = new JComboBox();
		campaignCombo.addActionListener(this);
		campaignCombo.setActionCommand(SELECT_CAMPAIGN);
		campaign_panel.add(campaignCombo, constraints);
		constraints.gridy++;
		
		
		// add 'last modified' simple loading tickbox
		simple_loading_checkbox = new JCheckBox("Use 'last-modified' technique to find new files to load.");
		simple_loading_checkbox.setSelected(true);
		simple_loading_checkbox.setActionCommand("simple_loading_switch");
		simple_loading_checkbox.addActionListener((ActionListener) this);
		simple_loading_checkbox.setToolTipText("The 'last-modified' technique is faster than a full check on the database.\nSwitch off in case changes in files are not picked up by SPECCHIO.");
		
		campaign_panel.add(simple_loading_checkbox, constraints);
		constraints.gridy++;
		
		constraints.gridx = 0;
		constraints.gridy = 0;
		rootPanel.add(campaign_panel, constraints);
		constraints.gridy++;
		
		// tabbed panes for different loading options
		JScrollPane tabs_scroll_pane = new JScrollPane();
		JTabbedPane loading_tabs = new JTabbedPane();
		
		JScrollPane pathroot_scroll_pane = new JScrollPane();
		pathroot_scroll_pane.getViewport().add(pathroot_scroll_panel);
		pathroot_scroll_pane.getVerticalScrollBar().setUnitIncrement(10);
		JScrollPane exist_h_loading_scroll_pane = new JScrollPane();
		exist_h_loading_scroll_pane.getViewport().add(exist_h_loading_scroll_panel);
		exist_h_loading_scroll_pane.getVerticalScrollBar().setUnitIncrement(10);	
		
		Border blackline = BorderFactory.createLineBorder(Color.black);
		TitledBorder tb = BorderFactory.createTitledBorder(blackline);
		
		exist_h_loading_scroll_pane.setBorder(tb);

		loading_tabs.addTab("Root Path Loading", pathroot_scroll_pane);
		loading_tabs.addTab("Existing Hierarchy based Loading", exist_h_loading_scroll_pane);	


		tabs_scroll_pane.getViewport().add(loading_tabs);
		
		rootPanel.add(tabs_scroll_pane, constraints);
		
		
		// exist_h_loading_scroll_panel components
		sdb = new SpectralDataBrowser(specchioClient, true);
		sdb.setShow_only_hierarchies(true);
		sdb.order_by_box_enabled(false);
//		sdb.si
		
		
		if (specchioClient.isLoggedInWithRole("admin"))
		{
			sdb.restrict_to_view = false;
		}

		constraints.gridx = 0;
//		constraints.anchor = GridBagConstraints.WEST;
//		constraints.fill = GridBagConstraints.REMAINDER;
		exist_h_loading_scroll_panel.add(sdb, constraints);
		
		// add a panel for the buttons
		constraints.gridx = 1;
		constraints.anchor = GridBagConstraints.CENTER;
		JPanel buttonPanel = new JPanel();
		exist_h_loading_scroll_panel.add(buttonPanel, constraints);

		
		// add the "load" button
		loadButton = new JButton(LOAD);
		loadButton.setActionCommand(LOAD_SDB);
		loadButton.addActionListener(this);
		buttonPanel.add(loadButton);
		
		// add the "cancel" button
		cancelButton = new JButton(CANCEL);
		cancelButton.setActionCommand(CANCEL);
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);		
		
		
		// pathroot_scroll_panel components
		// add the path selection list
		constraints.gridx = 0;
		pathListLabel = new JLabel("Available Paths:");
		pathroot_scroll_panel.add(pathListLabel, constraints);
		constraints.gridx = 1;
		constraints.anchor = GridBagConstraints.CENTER;
		pathPanel = new CampaignPathPanel(this, true, false);
		pathroot_scroll_panel.add(pathPanel, constraints);
		constraints.gridy++;
		
		// add the information label
		constraints.gridx = 1;
		constraints.anchor = GridBagConstraints.WEST;
		JLabel infoLabel = new JLabel("Spectral data of the selected campaign will be loaded from the selected directory.");
		pathroot_scroll_panel.add(infoLabel, constraints);
		constraints.gridy++;
		
		// add a panel for the buttons
		constraints.gridx = 1;
		constraints.anchor = GridBagConstraints.CENTER;
		buttonPanel = new JPanel();
		pathroot_scroll_panel.add(buttonPanel, constraints);
		constraints.gridy++;
		
		// add the "load" button
		loadButton = new JButton(LOAD);
		loadButton.setActionCommand(LOAD);
		loadButton.addActionListener(this);
		buttonPanel.add(loadButton);
		
		// add the "cancel" button
		cancelButton = new JButton(CANCEL);
		cancelButton.setActionCommand(CANCEL);
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);
				
		// add the unavailable path list
		constraints.gridx = 0;
		constraints.anchor = GridBagConstraints.WEST;
		UnavailablePathListLabel = new JLabel("Unavailable Paths:");
		UnavailablePathListLabel.setEnabled(false);
		pathroot_scroll_panel.add(UnavailablePathListLabel, constraints);
		constraints.gridx = 1;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		unavailablePathPanel = new CampaignPathPanel(this, false, false);
		unavailablePathPanel.setEnabled(false);
		pathroot_scroll_panel.add(unavailablePathPanel, constraints);
		constraints.gridy++;		
		

		// populate the campaign selection combo box
		Campaign[] campaigns = specchioClient.getCampaigns();
		for (Campaign c: campaigns) {
			campaignCombo.addItem(c);
		}
		campaignSelected((Campaign)campaignCombo.getSelectedItem());
		
		// lay out the dialogue and disable re-sizing
		pack();
		setResizable(false);
		
	}
	
	
	/**
	 * Button handler.
	 *
	 * @param event	the event to be handled
	 */
	public void actionPerformed(ActionEvent event) {
		
		if (SELECT_CAMPAIGN.equals(event.getActionCommand())) {
			
			try {
				campaignSelected((Campaign)campaignCombo.getSelectedItem());
			}
			catch (SPECCHIOClientException ex) {
				// server error
				ErrorDialog error = new ErrorDialog((Frame)getOwner(), "Error", ex.getUserMessage(), ex);
				error.setVisible(true);
			}
			
		} else if (LOAD.equals(event.getActionCommand())) {
			
			File file = pathPanel.getSelectedPath();
			if (campaign != null && file != null) {
				
				// set up a campaign data loader
				SpecchioCampaignDataLoader cdl = new SpecchioCampaignDataLoader(new LoadCampaignDataHandler(), specchioClient);
				cdl.setSimple_delta_loading(this.simple_loading_checkbox.isSelected());
				
				// load campaign data
				campaign.setPath(file.toString());
				cdl.set_campaign(campaign);
				cdl.start();
				
				this.setVisible(false);
				
			} else if (campaign == null) {
				
				JOptionPane.showMessageDialog(this, "You must select a campaign.", "Error", JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon);

			} else if (file == null) {

				JOptionPane.showMessageDialog(this, "You must select an input path.", "Error", JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon);

			}

		} else if (LOAD_SDB.equals(event.getActionCommand())) {
			
			SpecchioCampaignDataLoader cdl = new SpecchioCampaignDataLoader(new LoadCampaignDataHandler(), specchioClient);
			cdl.setSimple_delta_loading(this.simple_loading_checkbox.isSelected());
			
			// get hierarchy id to load
			ArrayList<Integer> hierarchy_ids = sdb.get_selected_hierarchy_ids();
			
			Iterator<Integer> it = hierarchy_ids.iterator();
			
			while(it.hasNext())
			{
				Integer hierarchy_id = it.next();
				cdl.set_hierarchy_to_load(hierarchy_id);
				
				// get the path if the selected hierarchy id
				String hierarchy_path = this.specchioClient.getHierarchyFilePath(hierarchy_id);
				
				campaign.setPath(hierarchy_path);
				cdl.set_campaign(campaign);
			
				cdl.start();
			
			}
			
			
		} else if ("simple_loading_switch".equals(event.getActionCommand()))
		{
			// nothing to do here ... status is injected when instantiating a SpecchioCampaignDataLoader

		}
		else if (CANCEL.equals(event.getActionCommand())) {

			setVisible(false);

		}

	}
	
	
	/**
	 * Handle selection of a campaign.
	 * 
	 * @param c	the selected campaign
	 */
	private void campaignSelected(Campaign c) throws SPECCHIOClientException {
		
		// get the complete campaign object from the server
		if (c != null) {
			campaign = specchioClient.getCampaign(c.getId());
			
			// update the path selection panel
			pathPanel.setCampaign(campaign);
			unavailablePathPanel.setCampaign(campaign);
			
			// update spectral databrowser
			sdb.build_tree(c.getId());			
			
		} else {
			campaign = null;
			JOptionPane.showMessageDialog(this, "You must create a campaign before loading data", "Error", JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon);
			
		}
		
		
	}
	
}
