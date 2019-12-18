package ch.specchio.gui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import ch.specchio.proc_modules.SunAngleCalcThread;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.constants.UserRoles;


/**
 * Dialogue for calculating sun angles.
 */
public class SunAngleCalcDialog extends JDialog implements ActionListener, TreeSelectionListener {

	/** serialisation version identifier */
	private static final long serialVersionUID = 1L;
	
	/** client object */
	private SPECCHIOClient specchioClient;
	

	/** spectral data browser */
	private SpectralDataBrowser sdb;
	
	/** label for the number of selected spectra */
	private JLabel numSelectedLabel;
	
	/** text field for the number of selected spectra */
	private JTextField numSelectedField;
	
	/** "okay" button */
	private JButton submitButton;
	
	/** "dismiss" button */
	private JButton dismissButton;
	
	/** text for the "okay" button */
	private static final String SUBMIT = "Apply";
	
	/** text for the "dismiss" button */
	private static final String DISMISS = "Close";
	
	
	/**
	 * Constructor.
	 * 
	 * @param owner	the frame that owns this dialogue
	 * @param modal	true if the dialogue should be modal
	 * 
	 * @throws SPECCHIOClientException	error contacting server
	 */
	public SunAngleCalcDialog(Frame owner, boolean modal) throws SPECCHIOClientException {
		
		super(owner, "Sun Angle Calculation", modal);
		
		// get a reference to the application's client object
		specchioClient = SPECCHIOApplication.getInstance().getClient();
		

		// set up the root panel with a vertical box layout
		JPanel rootPanel = new JPanel();
		rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
		getContentPane().add(rootPanel);
		
		// add a spectral data browser for selecting nodes
		sdb = new SpectralDataBrowser(specchioClient, !specchioClient.isLoggedInWithRole(UserRoles.ADMIN));
		sdb.build_tree();
		sdb.tree.addTreeSelectionListener(this);
		rootPanel.add(sdb);
		
		// create a panel for the selection information
		JPanel selectionPanel = new JPanel();
		rootPanel.add(selectionPanel);
		
		// add a field for displaying the number of selected spectra
		numSelectedLabel = new JLabel("Number of Selected Spectra:");
		selectionPanel.add(numSelectedLabel);
		numSelectedField = new JTextField(20);
		numSelectedField.setEditable(false);
		selectionPanel.add(numSelectedField);
		
		// create a panel for the buttons
		JPanel buttonPanel = new JPanel();
		rootPanel.add(buttonPanel);
		
		// create the "okay" button
		submitButton = new JButton(SUBMIT);
		submitButton.setActionCommand(SUBMIT);
		submitButton.addActionListener(this);
		buttonPanel.add(submitButton);
		
		// crate the "dismiss" button
		dismissButton = new JButton(DISMISS);
		dismissButton.setActionCommand(DISMISS);
		dismissButton.addActionListener(this);
		buttonPanel.add(dismissButton);
		
		// lay out the dialogue
		pack();
		
	}
	
	
	/**
	 * Button handler.
	 * 
	 * @param event	the event to be handled
	 */
	public void actionPerformed(ActionEvent event) {
		
		if (SUBMIT.equals(event.getActionCommand())) {
			
			try {
				// get the selected items
				List<Integer> spectrumIds = sdb.get_selected_spectrum_ids();
			
				if (spectrumIds.size() > 0) {
					// launch a thread to perform the actual work
					SunAngleCalcThread thread = new SunAngleCalcThread(spectrumIds, specchioClient, this);
					thread.start();
				}
				
			} catch (SPECCHIOClientException ex) {
				ErrorDialog error = new ErrorDialog((Frame)this.getOwner(), "Error", ex.getUserMessage(), ex);
				error.setVisible(true);
			}
			
		} else if (DISMISS.equals(event.getActionCommand())) {
			
			// close the dialogue
			setVisible(false);
			
		}
		
	}
	
	
	/**
	 * Tree selection handler.
	 * 
	 * @param event	the event to be handled
	 */
	public void valueChanged(TreeSelectionEvent event) {
		
		try {
			// get the selected items
			List<Integer> spectrumIds = sdb.get_selected_spectrum_ids();
			
			// display the number of selected spectra
			if (spectrumIds.size() > 0) {
				numSelectedField.setText(Integer.toString(spectrumIds.size()));
			} else {
				numSelectedField.setText(null);
			}
			
		} catch (SPECCHIOClientException ex) {
			ErrorDialog error = new ErrorDialog((Frame)this.getOwner(), "Error", ex.getUserMessage(), ex);
			error.setVisible(true);
		}
		
	}
	

}
