package ch.specchio.gui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.types.Campaign;
import ch.specchio.types.ResearchGroup;
import ch.specchio.types.SpecchioCampaign;
import ch.specchio.types.User;


/**
 * Panel for editig a campaign's metadata
 */
public class CampaignMetadataPanel extends JPanel implements KeyListener, ListDataListener {

	/** serialisation version ID */
	private static final long serialVersionUID = 1L;
	
	/** network/database client */
	private SPECCHIOClient specchioClient;
	
	/** the metadata editor to which this panel belongs */
	private MetaDataEditorView owner;
	
	/** true if any of the fields have been changed */
	private boolean changed;
	
	/** the type of campaign being edited */
	private String campaignType;
	
	/** the identifeir of the campaign being edited */
	private int campaignId;
	
	/** constraints descriptor for laying the dialogue out */
	private GridBagConstraints constraints;
	
	/** campaign name label */
	private JLabel nameLabel;
	
	/** campaign name text field */
	private JTextField nameField;
	
	/** campaign description label */
	private JLabel descriptionLabel;
	
	/** campaign description text field */
	private JTextArea descriptionField;
	
	/** investigator label */
	private JLabel investigatorLabel;
	
	/** investigator text field */
	private JTextField investigatorField;
	
	/** campaign path label */
	private JLabel pathLabel;
	
	/** campaign path panel */
	private CampaignPathPanel pathPanel;
	
	/** research group label */
	private JLabel researchGroupLabel;
	
	/** research group panel */
	private ResearchGroupPanel researchGroupPanel;
	
	
	/**
	 * Constructor.
	 */
	public CampaignMetadataPanel(MetaDataEditorView ownerIn, SPECCHIOClient specchioClientIn) {
		
		super();
		
		// initialise member variables
		owner = ownerIn;
		specchioClient = specchioClientIn;
		changed = false;
		campaignType = null;
		campaignId = 0;
		
		// set up the panel with a grid bag layout
		setLayout(new GridBagLayout());
		constraints = new GridBagConstraints();
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(4, 4, 4, 4);
		
		// add the campaign name field
		constraints.gridx = 0;
		nameLabel = new JLabel("Campaign name:");
		add(nameLabel, constraints);
		constraints.gridx = 1;
		nameField = new JTextField(35);
		nameField.addKeyListener(this);
		add(nameField, constraints);
		constraints.gridy++;
		
		// add the campaign description field
		constraints.gridx = 0;
		descriptionLabel = new JLabel("Description:");
		add(descriptionLabel, constraints);
		constraints.gridx = 1;
		descriptionField = new JTextArea(4, 35);
		descriptionField.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		descriptionField.addKeyListener(this);
		add(descriptionField, constraints);
		constraints.gridy++;
		
		// add the investigator field
		constraints.gridx = 0;
		investigatorLabel = new JLabel("Investigator:");
		add(investigatorLabel, constraints);
		constraints.gridx = 1;
		investigatorField = new JTextField(35);
		investigatorField.setEditable(false);
		add(investigatorField, constraints);
		constraints.gridy++;
		
		// add the path field
		constraints.gridx = 0;
		pathLabel = new JLabel("Paths:");
		add(pathLabel, constraints);
		constraints.gridx = 1;
		pathPanel = new CampaignPathPanel(owner, true, true);
		pathPanel.addListDataListener(this);
		add(pathPanel, constraints);
		constraints.gridy++;
		
		// add the research group member list
		constraints.gridx = 0;
		researchGroupLabel = new JLabel("Research Group Members:");
		add(researchGroupLabel, constraints);
		constraints.gridx = 1;
		researchGroupPanel = new ResearchGroupPanel();
		researchGroupPanel.addListDataListener(this);
		add(researchGroupPanel, constraints);
		constraints.gridy++;
		
		// disable the panel until a campaign is selected
		setEnabled(false);
		
	}
	
	
	/**
	 * Handle a change in the contents of a list panel.
	 * 
	 * @param event	the event to be handled
	 */
	public void contentsChanged(ListDataEvent event) {
		
		fieldChanged();
		
	}
	
	
	/**
	 * Handle changes to the panel.
	 */
	private void fieldChanged() {
		
		// update the changed state
		changed = true;
		
		// notify the panel's owner
		owner.campaignDataChanged();
		
	}
	
	
	/**
	 * Get a campaign object corresponding to the settings of the dialogue. The
	 * object will have the same campaign type and identifier as the object used
	 * in the last call to setCampaign().
	 * 
	 * @return a new Campaign object, or null if setCampaign() has never been called
	 */
	public Campaign getCampaign() {
		
		// sanity check
		if (campaignId == 0) {
			return null;
		}
		
		// construct a new campaign object of the same type as the input one
		Campaign campaign = null;
		if ("specchio".equals(campaignType)) {
			campaign = new SpecchioCampaign();
		}
		
		// set fields
		if (campaign != null) {
			// set simple fields
			campaign.setId(campaignId);
			campaign.setName(nameField.getText());
			campaign.setDescription(descriptionField.getText());
			campaign.setResearchGroup(researchGroupPanel.getResearchGroup());
			
			// set paths
			File selectedPath = pathPanel.getSelectedPath();
			if (selectedPath != null) {
				campaign.setPath(selectedPath.toString());
			} else {
				campaign.setPath("");
			}
			for (File path : pathPanel.getListedPaths()) {
				campaign.addKnownPath(path.toString());
			}
		}
		
		return campaign;
		
	}
	
	
	/**
	 * Test whether or not the data in the panel has any unsaved changes.
	 * 
	 * @return true if any fields of the panel have been changed since the last call to setCampaign(), and false otherwise
	 */
	public boolean hasUnsavedChanges() {
		
		return changed;
		
	}
	
	
	/**
	 * Handle an insertion of items into a list panel.
	 * 
	 * @param event	the event to be handled
	 */
	public void intervalAdded(ListDataEvent event) {
		
		fieldChanged();
		
	}
	
	
	/**
	 * Handle a deletion of items from a list panel.
	 * 
	 * @param event	the event to be handled
	 */
	public void intervalRemoved(ListDataEvent event) {
		
		fieldChanged();
		
	}
	
	
	/**
	 * Key press handler. Does nothing.
	 */
	public void keyPressed(KeyEvent event) {
		
		// ignore
		
	}


	/**
	 * Key released handler. Does nothing.
	 */
	public void keyReleased(KeyEvent event) {
		
		// does nothing
		
	}


	/**
	 * Key typed handler.
	 */
	public void keyTyped(KeyEvent event) {
		
		// indicate that the panel has been changed
		fieldChanged();
		
	}


	/**
	 * Configure the panel with a campaign object.
	 * 
	 * @param campaign	the campaign
	 */
	public void setCampaign(Campaign campaign) {
		
		if (campaign != null) {
			
			// fill the controls with information for the new campaign
			campaignId = campaign.getId();
			campaignType = campaign.getType();
			nameField.setText(campaign.getName());
			descriptionField.setText(campaign.getDescription());
			investigatorField.setText(campaign.getInvestigator());
			pathPanel.setCampaign(campaign);
			researchGroupPanel.setResearchGroup(campaign.getResearchGroup());
			
			// enable all controls
			setEnabled(true);
			
		} else {
			
			// empty all of controls
			campaignId = 0;
			campaignType = null;
			nameField.setText(null);
			descriptionField.setText(null);
			investigatorField.setText(null);
			pathPanel.setCampaign(null);
			researchGroupPanel.setResearchGroup(null);
			
			// disable all controls
			setEnabled(false);
		}
		
		// reset "changed" flag
		changed = false;
		
	}
	
	
	/**
	 * Enable or disable the panel.
	 *
	 * @param enabled	whether to enable or disable the panel
	 */
	public void setEnabled(boolean enabled) {
		
		// notify the super-class
		super.setEnabled(enabled);
		
		// propagate enablement to all of the panel's controls
		nameLabel.setEnabled(enabled);
		nameField.setEnabled(enabled);
		descriptionLabel.setEnabled(enabled);
		descriptionField.setEnabled(enabled);
		investigatorLabel.setEnabled(enabled);
		investigatorField.setEnabled(enabled);
		pathLabel.setEnabled(enabled);
		pathPanel.setEnabled(enabled);
		researchGroupLabel.setEnabled(enabled);
		researchGroupPanel.setEnabled(enabled);
		
	}
	
	
	/**
	 * A panel for displaying and manipulating research group members.
	 */
	private class ResearchGroupPanel extends JPanel implements ActionListener, ListSelectionListener {
		
		/** serial verison ID */
		private static final long serialVersionUID = 1L;
		
		/** the research group identifier */
		private int researchGroupId;
		
		/** the research group name */
		private String researchGroupName;
		
		/** research group member list model */
		private DefaultListModel memberList;
		
		/** research group member list control */
		private JList memberField;
		
		/** button for adding a research group member */
		private JButton addMemberButton;
		
		/** button for removing a research group member */
		private JButton removeMemberButton;
		
		/** the list of list data listeners */
		private List<ListDataListener> listeners;
		
		/** text for the "add member" button */
		private static final String ADD_MEMBER = "Add member";
		
		/** text for the "remove member" button */
		private static final String REMOVE_MEMBER = "Remove member";
		

		/**
		 * Constructor.
		 */
		public ResearchGroupPanel() {
			
			// set up the panel with a flow layout
			super();
			
			// initialise member variables
			researchGroupId = 0;
			researchGroupName = "";
			listeners = new LinkedList<ListDataListener>();
			
			// create the research group list model and control
			memberList = new DefaultListModel();
			memberField = new JList(memberList);
			memberField.addListSelectionListener(this);
			JScrollPane memberFieldScrollPane = new JScrollPane(memberField);
			memberFieldScrollPane.setPreferredSize(new Dimension(150, 100));
			add(memberFieldScrollPane);
			
			// create a panel for the buttons with a vertical box layout
			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
			add(buttonPanel);
			
			// create a button for adding members
			addMemberButton = new JButton(ADD_MEMBER);
			addMemberButton.setActionCommand(ADD_MEMBER);
			addMemberButton.addActionListener(this);
			buttonPanel.add(addMemberButton);
			
			// create a button for removing members
			removeMemberButton = new JButton(REMOVE_MEMBER);
			removeMemberButton.setActionCommand(REMOVE_MEMBER);
			removeMemberButton.addActionListener(this);
			buttonPanel.add(removeMemberButton);
			
			// disable the "remove member" button until something is actually selected
			removeMemberButton.setEnabled(false);
			
		}
		
		
		/**
		 * Button handler.
		 * 
		 * @param event	the action to be handled
		 */
		public void actionPerformed(ActionEvent event) {
			
			if (ADD_MEMBER.equals(event.getActionCommand())) {
				
				// add a new research group member
				try {
					ResearchGroupMemberDialog d = new ResearchGroupMemberDialog(owner, getNonMemberUsers());
					d.setVisible(true);
					if (d.getName() != null) {
						// add the new name to the list model
						memberList.add(memberList.size(), d.getMember());
						
						// notify the main panel of a change
						fireListDataEvent(ListDataEvent.INTERVAL_ADDED, memberList.size() - 1, memberList.size() - 1);
					}
				}
				catch (SPECCHIOClientException ex) {
					ErrorDialog error = new ErrorDialog(owner, "Could not retrieve user list", ex.getUserMessage(), ex);
					error.setVisible(true);
				}
				
			} else if (REMOVE_MEMBER.equals(event.getActionCommand())) {
				
				// remove the currently-selected name from the list
				int i = memberField.getSelectedIndex();
				if (i != -1) {
					
					User user = (User)memberField.getSelectedValue();
					if (user.getUserId() != specchioClient.getLoggedInUser().getUserId()) {
						
						// remove the user from the list
						memberList.remove(i);
					
						// notify the main panel of a change
						fireListDataEvent(ListDataEvent.INTERVAL_REMOVED, i, i);
						
					} else {
						
						// the user is trying to remove him- or herself; this will get very confusing so don't allow it
						JOptionPane.showMessageDialog(owner, "You cannot remove yourself from the research group.", "Error", JOptionPane.ERROR_MESSAGE, SPECCHIOApplication.specchio_icon);
						
					}
				}
				
			}
			
		}
		
		
		/**
		 * Register for notifications when the list data changes.
		 * 
		 * @param listener	the listener
		 */
		public void addListDataListener(ListDataListener listener) {
			
			listeners.add(listener);
			
		}
		
		
		/**
		 * Notify all listeners of a change in the contents of the group.
		 * 
		 * @param typ		ListDataEvent.CONTENTS_CHANGED, LisDataEvent.INTERVAL_ADDED or ListDataEvent.INTERVAL_REMOVED
		 * @param index0	the index of the first inserted element
		 * @param index1	the index of the last inserted element
		 */
		private void fireListDataEvent(int type, int index0, int index1) {
			
			ListDataEvent event = new ListDataEvent(this, type, index0, index1);
			for (ListDataListener listener : listeners) {
				if (type == ListDataEvent.CONTENTS_CHANGED) {
					listener.contentsChanged(event);
				} else if (type == ListDataEvent.INTERVAL_ADDED) {
					listener.intervalAdded(event);
				} else if (type == ListDataEvent.INTERVAL_REMOVED) {
					listener.intervalRemoved(event);
				}
			}
			
		}
		
		
		/**
		 * Get an array of available users who are not already a member of the
		 * research group.
		 * 
		 * @return an array of User objects
		 * 
		 * @throws SPECCHIOClientException database error
		 */
		public User[] getNonMemberUsers() throws SPECCHIOClientException {
			
			// get the list of all users
			User allUsers[] = specchioClient.getUsers();
			
			// add all the non-member users to a list
			List<User> nonMembers = new ArrayList<User>(allUsers.length);
			for (User user : allUsers) {
				if (!memberList.contains(user) && !user.getUsername().equals("sdb_admin")) {
					nonMembers.add(user);
				}
			}
			
			return nonMembers.toArray(new User[nonMembers.size()]);
			
		}
		
		
		/**
		 * Get a research group object corresponding to the data in the panel.
		 * 
		 * @return a ResearchGroup object
		 */
		public ResearchGroup getResearchGroup() {
			
			// create a research group with the same identifier and name as the input one
			ResearchGroup group = new ResearchGroup(researchGroupId, researchGroupName);
			
			// add the group members from the list
			for (int i = 0; i < memberList.size(); i++) {
				group.addMember((User)memberList.get(i));
			}
			
			return group;
			
		}
		
		
		/**
		 * Enable or disable the panel.
		 *
		 * @param enabled	whether to enable or disable the panel
		 */
		public void setEnabled(boolean enabled) {
			
			// notify the super-class
			super.setEnabled(enabled);
			
			// propagate the enablement to all of the child components
			memberField.setEnabled(enabled);
			addMemberButton.setEnabled(enabled);
			removeMemberButton.setEnabled(enabled && memberField.getSelectedIndex() != -1);
			
		}
		
		
		/**
		 * Set the research group to be displayed by the panel.
		 * 
		 * @param group	the research group
		 */
		public void setResearchGroup(ResearchGroup group) {
			
			// save the research group identifier for later
			researchGroupId = (group != null)? group.getId() : 0;
			researchGroupName = (group != null)? group.getName() : "";
			
			memberList.clear();
			if (group != null) {
				List<User> members = group.getMembers();
				for (int i = 0; i < members.size(); i++) {
					memberList.add(i, members.get(i));
				}
			}
			
		}
		
		
		/**
		 * Handler for list selection changes.
		 */
		public void valueChanged(ListSelectionEvent event) {
			
			// enable the "remove member" button only if something is selected
			removeMemberButton.setEnabled(memberField.getSelectedIndex() != -1);
			
		}
		
	}
	
	
	/**
	 * Dialogue box for adding a research group member.
	 */
	private class ResearchGroupMemberDialog extends JDialog implements ActionListener {
		
		/** serial version ID */
		private static final long serialVersionUID = 1L;
		
		/** the new member user object */
		private User member;

		/** the new member label */
		private JLabel memberLabel;
		
		/** the new member combo box */
		private JComboBox memberField;
		
		/** button for submitting the dialogue */
		private JButton addButton;
		
		/** button for cancelling the dialogue */
		private JButton cancelButton;
		
		/** string for the submission button */
		private static final String ADD = "Add";
		
		/** string for the cancel button */
		private static final String CANCEL = "Cancel";
		
		/**
		 * Constructor.
		 * 
		 * @param owner	the frame that created this dialogue
		 * @param users	the et of user objects from the which new member can be chosen
		 */
		public ResearchGroupMemberDialog(Frame owner, User users[]) {
			
			super(owner, "Add research group member", true);
			
			// initialise member variables
			member = null;
			
			// set up a panel with a grid bag layout
			JPanel rootPanel = new JPanel();
			rootPanel.setLayout(new GridBagLayout());
			getContentPane().add(rootPanel);
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.insets = new Insets(4, 4, 4, 4);
			constraints.gridy = 0;
			constraints.anchor = GridBagConstraints.WEST;
			
			// add label and field for the member name
			constraints.gridx = 0;
			memberLabel = new JLabel("Name:");
			rootPanel.add(memberLabel, constraints);
			constraints.gridx = 1;
			memberField = new JComboBox(users);
			rootPanel.add(memberField, constraints);
			constraints.gridy++;
			
			// add a panel for the buttons
			constraints.gridx = 1;
			JPanel buttonPanel = new JPanel();
			rootPanel.add(buttonPanel, constraints);
			
			// add the "add" button
			addButton = new JButton(ADD);
			addButton.setActionCommand(ADD);
			addButton.addActionListener(this);
			buttonPanel.add(addButton);
			
			// add the "cancel" button
			cancelButton = new JButton(CANCEL);
			cancelButton.setActionCommand(CANCEL);
			cancelButton.addActionListener(this);
			buttonPanel.add(cancelButton);
			
			// lay out the dialogue
			pack();
			
		}

		
		/**
		 * Button handler.
		 */
		public void actionPerformed(ActionEvent event) {
			
			if (ADD.equals(event.getActionCommand())) {
				
				// save the name for later
				member = (User)memberField.getSelectedItem();
				
				// dismiss the dialogue
				setVisible(false);
				
			} else if (CANCEL.equals(event.getActionCommand())) {
				
				// set the name to null to indicate that the dialogue was cancelled
				member = null;
				
				// dismiss the dialogue
				setVisible(false);
				
			}
			
		}
		
		
		/**
		 * Get the name that was typed into the dialogue.
		 * 
		 * @return the name, or null if the dialogue was cancelled
		 */
		public User getMember() {
			
			return member;
			
		}
		
	}

}
