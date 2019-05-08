package ch.specchio.gui;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

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
	
	/** command for a combo box selection */
	private static final String SELECT_CAMPAIGN = "Select campaign";
	
	/** text for the "load" button */
	private static final String LOAD = "Load";
	
	/** text for the "cancel" button */
	private static final String CANCEL = "Cancel";
	
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
		
		// add the campaign selection combo box
		constraints.gridx = 0;
		campaignLabel = new JLabel("Campaign name:");
		rootPanel.add(campaignLabel, constraints);
		constraints.gridx = 1;
		campaignCombo = new JComboBox();
		campaignCombo.addActionListener(this);
		campaignCombo.setActionCommand(SELECT_CAMPAIGN);
		rootPanel.add(campaignCombo, constraints);
		constraints.gridy++;
		
		// add the path selection list
		constraints.gridx = 0;
		pathListLabel = new JLabel("Available Paths:");
		rootPanel.add(pathListLabel, constraints);
		constraints.gridx = 1;
		pathPanel = new CampaignPathPanel(this, true, false);
		rootPanel.add(pathPanel, constraints);
		constraints.gridy++;
		
		// add the information label
		constraints.gridx = 0;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.anchor = GridBagConstraints.CENTER;
		JLabel infoLabel = new JLabel("Spectral data of the selected campaign will be loaded from the selected directory.");
		rootPanel.add(infoLabel, constraints);
		constraints.gridy++;
		
		// add the unavailable path list
		constraints.gridx = 0;
		constraints.anchor = GridBagConstraints.WEST;
		UnavailablePathListLabel = new JLabel("Unavailable Paths:");
		UnavailablePathListLabel.setEnabled(false);
		rootPanel.add(UnavailablePathListLabel, constraints);
		constraints.gridx = 1;
		unavailablePathPanel = new CampaignPathPanel(this, false, false);
		unavailablePathPanel.setEnabled(false);
		rootPanel.add(unavailablePathPanel, constraints);
		constraints.gridy++;		
		
		// add a panel for the buttons
		constraints.gridx = 0;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		constraints.anchor = GridBagConstraints.CENTER;
		JPanel buttonPanel = new JPanel();
		rootPanel.add(buttonPanel, constraints);
		
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
			
		} else if (CANCEL.equals(event.getActionCommand())) {
			
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
		} else {
			campaign = null;
		}
		
		// update the path selection panel
		pathPanel.setCampaign(campaign);
		unavailablePathPanel.setCampaign(campaign);
		
	}
	
}
