package ch.specchio.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import ch.specchio.metadata.MD_EAV_Field;
import ch.specchio.metadata.MD_Field;
import ch.specchio.types.MetaParameter;


/**
 * Dialogue for selecting the action to be taken when updating or deleting metadata items
 * that are shared between more than one record.
 */
public class SharedMD_Dialog extends JDialog implements ActionListener {
	
	/** serialisation version identifier */
	private static final long serialVersionUID = 1L;
	
	/** list of fields */
	private JList<String> fieldList;
	
	/** "apply to all" button */
	private JButton applyToAllButton;

	/** "apply to selection" button */
	private JButton applyToSelectionButton;
	
	/** "apply to none" button */
	private JButton applyToNoneButton;
	
	/** constant for the "delete" action */
	public final static int DELETE = 0;
	
	/** constant for the "update" action */
	public final static int UPDATE = 1;	
	
	/** constant for the "apply to all" decision */
	public final static int APPLY_TO_ALL = 1;
	
	/** constant for the "apply to selection" decision */
	public final static int APPLY_TO_SELECTION = 2;
	
	/** constant for cancelling the dialogue */
	public final static int APPLY_TO_NONE = 3;
	
	/** text for the "apply to all" button */
	private final static String APPLY_TO_ALL_CMD = "Apply to all spectra";
	
	/** text for the "apply to selection" button */
	private final static String APPLY_TO_SELECTION_CMD = "Selected spectra only";
	
	/** text for the "apply to none" button */
	private final static String APPLY_TO_NONE_CMD = "Cancel";
	
	/** the option selected by the user */
	private int decision = APPLY_TO_ALL;
	
	/**
	 * Constructor.
	 * 
	 * @param owner		the owner of this dialogue
	 * @param op		SharedMD_Dialog.UPDATE or SharedMD_Dialog.DELETE
	 * @param fields	the fields to be updated or deleted
	 * @param hierarchyLevel 
	 */
	public SharedMD_Dialog(Frame owner, int op, List<MD_Field> fields, int hierarchyLevel) {
		
		super(owner, ((op == DELETE)? "Delete" : "Update") + " shared metaparameter", true);
		
		String verb = (op == DELETE)? "delete" : "update";
		
		String level_name, explanation, action, inherit_prefix, inherit_postfix, parallel_sharing_prefix = "", parallel_sharing_postfix = "", prefix, postfix, midfix = " ";
		if(hierarchyLevel == MetaParameter.SPECTRUM_LEVEL)
		{
			level_name = "spectra";
			explanation = "The data in the fields listed below are referred to by multiple " +level_name + ".";
			action = "You can " + verb + " the metadata of all of these " +level_name + ", or only of the " +level_name + " selected in the browser.";
			inherit_prefix = "Shared by ";
			inherit_postfix = "";
		}
		else
		{
			level_name = "hierarchies";
			explanation = "The data in the fields listed below are inherited from a hierarchy further up or shared by parallel hierarchies.";
			action = "You can only " + verb + " the metadata of all hierarchies.";
			inherit_prefix = "Inherited from N=";
			inherit_postfix = " further up";
			
			parallel_sharing_prefix = "Shared with N=";
			parallel_sharing_postfix = " parallel ";
			
		}
		
		// set up the root pane with a border layout
		JPanel rootPanel = new JPanel();
		rootPanel.setLayout(new BorderLayout());
		getContentPane().add(rootPanel);
		
		// create a panel for the explanatory text
		JPanel textPanel = new JPanel();
		textPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
		
		textPanel.add(new JLabel(explanation));
		textPanel.add(new JLabel(action));
		rootPanel.add(textPanel, BorderLayout.NORTH);
		
		// add the list of fields
		String fieldDescriptions[] = new String[fields.size()];
		int i = 0;
		for (MD_Field field : fields) {
			
			if(field instanceof MD_EAV_Field && field.getConflict().getConflictData(((MD_EAV_Field) field).getMetaParameter().getEavId()).isInherited() && hierarchyLevel != MetaParameter.SPECTRUM_LEVEL)
			{
				prefix = inherit_prefix;
				postfix = inherit_postfix;
			}
			else
			{
				prefix = parallel_sharing_prefix;
				postfix = "";	
				midfix = parallel_sharing_postfix;
			}

			fieldDescriptions[i++] = field.getLabel() + " (" + prefix + field.getNoOfSharingRecords() + midfix + level_name + postfix + ")";
		}
		fieldList = new JList<String>(fieldDescriptions);
		fieldList.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		rootPanel.add(fieldList, BorderLayout.CENTER);
		
		// create a panel for the buttons
		JPanel buttonPanel = new JPanel();
		rootPanel.add(buttonPanel, BorderLayout.SOUTH);
		
		// create the "apply to all" button
		applyToAllButton = new JButton(APPLY_TO_ALL_CMD);
		applyToAllButton.setActionCommand(APPLY_TO_ALL_CMD);
		applyToAllButton.addActionListener(this);
		buttonPanel.add(applyToAllButton);
		
		// create the "apply to selection" button
		if(hierarchyLevel == MetaParameter.SPECTRUM_LEVEL)
		{
			applyToSelectionButton = new JButton(APPLY_TO_SELECTION_CMD);
			applyToSelectionButton.setActionCommand(APPLY_TO_SELECTION_CMD);
			applyToSelectionButton.addActionListener(this);
			buttonPanel.add(applyToSelectionButton);
		}
		
		// create the "apply to none" button
		applyToNoneButton = new JButton(APPLY_TO_NONE_CMD);
		applyToNoneButton.setActionCommand(APPLY_TO_NONE_CMD);
		applyToNoneButton.addActionListener(this);
		buttonPanel.add(applyToNoneButton);
		
		// lay out the dialogue
		pack();
			
		
	}
	
	
	/**
	 * Button handler.
	 * 
	 * @param event	the event to be handled
	 */
	public void actionPerformed(ActionEvent event) {
		
		if (APPLY_TO_ALL_CMD.equals(event.getActionCommand())) {
			
			decision = APPLY_TO_ALL;
			setVisible(false);
			
		} else if (APPLY_TO_SELECTION_CMD.equals(event.getActionCommand())) {
			
			decision = APPLY_TO_SELECTION;
			setVisible(false);
			
		} else if (APPLY_TO_NONE_CMD.equals(event.getActionCommand())) {
			
			decision = APPLY_TO_NONE;
			setVisible(false);
			
		}
		
	}
	
	
	/**
	 * Get the action selected by the user.
	 * 
	 * @return SharedMD_Dialog.APPLY_TO_ALL, SharedMD_Dialog.APPLY_TO_SELECTION or SharedMdDialog.APPLY_TO_NONE
	 */
	public int getSelectedAction() {
		
		return decision;
		
	}

}
