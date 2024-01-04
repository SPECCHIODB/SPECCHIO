package ch.specchio.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.interfaces.ProgressReportInterface;
import ch.specchio.metadata.MDE_Controller;
import ch.specchio.metadata.MDE_Spectrum_Controller;
import ch.specchio.plots.swing.SpectralLinePlot;
import ch.specchio.plots.swing.SpectralPlot;
import ch.specchio.spaces.Space;
import ch.specchio.spaces.SpectralSpace;


/**
 * Spectrum report dialogue.
 */
public class SpectrumReportDialog extends JFrame implements ActionListener, ChangeListener, ListSelectionListener {

	/** serialisation version identifier */
	private static final long serialVersionUID = 1L;
	
	/** client object */
	private SPECCHIOClient specchioClient;
	
	/** progress report */
	private ProgressReportInterface pr;
	
	/** metadata controller */
	private MDE_Spectrum_Controller mdec;
	
	/** an enumeration of the spectrum identifiers in the dialogue */
	private List<Integer> spectrumEnum;
	
	/** the unloaded spaces corresponding to the indices of the spectrum enumeration */
	private List<Space> spectrumEnumSpaces;
	
	/** mapping of unloaded space objects to their loaded versions */
	private Hashtable<Space, SpectralSpace> loadedSpaces;
	
	/** mapping of loaded spaces to plot objects */
	private Hashtable<SpectralSpace, SpectralPlot> spectralPlots;
	
	/** spectrum chart panel */
	private JPanel spectralPlotPanel;
	
	/** spectrum selection spinner */
	private JSpinner spectrumSpinner;
	
	/** the category selection panel */
	private SpectrumMetadataCategoryList categoryList;
	
	/** spectrum metadata panel */
	private SpectrumMetadataPanel spectrumMetadataPanel;
	
	/** the "dismiss" button */
	private JButton dismissButton;
	
	/** the width of the spectrum plot */
	private static final int PLOT_WIDTH = 300;
	
	/** the height of the spectrum plot */
	private static final int PLOT_HEIGHT = 200;
	
	/** text for the "dismiss" button */
	private static final String DISMISS = "Close";
	
	
	/**
	 * Constructor.
	 * 
	 * @param specchioClient	client object
	 * @param spaces			the spaces to be displayed in the dialogue
	 * @param pr				progress report (may be null)
	 * 
	 * @throws SPECCHIOClientException	error contacting the server
	 */
	public SpectrumReportDialog(SPECCHIOClient specchioClient, ArrayList<Space> spaces, ProgressReportInterface pr) throws SPECCHIOClientException {
		
		// initialise member variables
		this.specchioClient = specchioClient;
		this.pr = pr;
		
		if (pr != null) {
			pr.set_operation("Initialising");
			pr.set_progress(0);
		}
		
		// enumerate the spectrum identifiers in all spaces
		spectrumEnum = new ArrayList<Integer>();
		spectrumEnumSpaces = new ArrayList<Space>();
		loadedSpaces = new Hashtable<Space, SpectralSpace>();
		spectralPlots = new Hashtable<SpectralSpace, SpectralPlot>();
		for (Space space : spaces) {
			for (Integer id : space.getSpectrumIds()) {
				spectrumEnum.add(id);
				spectrumEnumSpaces.add(space);
			}
		}
		
		// set up a metadata controller and form
		mdec = new MDE_Spectrum_Controller(specchioClient);
		
		if (pr != null) {
			pr.set_operation("Building user interface");
			pr.set_progress(50);
		}
		
		// set up the root panel with a vertical box layout
		JPanel rootPanel = new JPanel();
		rootPanel.setLayout(new BorderLayout());
		this.add(rootPanel);
		
		// add the spectral plot panel
		JScrollPane spectralPlotPane = new JScrollPane();
		spectralPlotPanel = new JPanel();
		spectralPlotPanel.setLayout(new FlowLayout());
		spectralPlotPane.getViewport().add(spectralPlotPanel);
		rootPanel.add(spectralPlotPane, BorderLayout.NORTH);
		
		if (spectrumEnum.size() > 1) {
			// create a panel for the spinner
			JPanel spectrumSpinnerPanel = new JPanel();
			spectralPlotPanel.add(new JPanel(), 0); // placeholder for spectral plot
			spectralPlotPanel.add(spectrumSpinnerPanel, 1);
			
			// add the spectrum selector
			spectrumSpinner = new JSpinner(new SpinnerNumberModel(1, 1, spectrumEnum.size(), 1));
			((JSpinner.DefaultEditor)spectrumSpinner.getEditor()).getTextField().setEditable(true);
			spectrumSpinner.addChangeListener(this);
			spectrumSpinnerPanel.add(spectrumSpinner);
			
			// add a label for the spinner
			JLabel spectrumSpinnerLabel = new JLabel(" of " + spectrumEnum.size() + " spectra");
			spectrumSpinnerPanel.add(spectrumSpinnerLabel);
		}
		
		// add the spectrum metadata panel
		spectrumMetadataPanel = new SpectrumMetadataPanel(this, specchioClient, mdec);
		spectrumMetadataPanel.setEditable(false);
		rootPanel.add(spectrumMetadataPanel, BorderLayout.CENTER);
		

		if (pr != null) {
			pr.set_progress(100);
		}
		
		// display the first spectrum
		if (spectrumEnum.size() > 0) {
			setDisplayedIndex(1);
		}
		
		// lay out the dialogue
		pack();
		
	}
	
	
	/**
	 * Button handler.
	 * 
	 * @param event	the event generated by the button
	 */
	public void actionPerformed(ActionEvent event) {
		
		if (DISMISS.equals(event.getActionCommand())) {
			
			// dismiss the dialogue
			setVisible(false);
			
		}
		
	}
	
	
	/**
	 * Handler for ending a potentially long-running operation.
	 */
	private void endOperation() {
		
		// change the cursor to its default start
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		
	}
	
	
	/**
	 * Set the currently-displayed spectrum.
	 * 
	 * @param index		the position of the spectrum in the spectrumId enumeration
	 */
	private void setDisplayedIndex(int index) {
		
		startOperation();
		try {
			
			// get the space and spectrum identifier to which this index refers
			Space space = spectrumEnumSpaces.get(index - 1);
			Integer spectrumId = spectrumEnum.get(index - 1);
			
			// get the loaded space object for this spectrum
			if (!loadedSpaces.containsKey(space)) {
				// need to load the space from the server
				if (pr != null) {
					pr.set_operation("Loading spectral data");
					pr.set_progress(0);
				}
				loadedSpaces.put(space, (SpectralSpace)specchioClient.loadSpace(space));
			}
			SpectralSpace ss = loadedSpaces.get(space);
			
			// plot the spectrum
			if (pr != null) {
				pr.set_operation("Plotting");
				pr.set_progress(50);
			}

			if (!spectralPlots.containsKey(ss)) {
				// need to build the plot object for this space
				spectralPlots.put(ss, new SpectralLinePlot(ss, null));
			}
			SpectralPlot sp = spectralPlots.get(ss);
			sp.plot(spectrumId);
			spectralPlotPanel.add(sp, 0);
			
			// change the selected spectra
			ArrayList<Integer> spectrumIdList = new ArrayList<Integer>();
			spectrumIdList.add(spectrumId);
			mdec.set_spectrum_ids(spectrumIdList);

			// tell the metadata panel to display the new spectrum
			if (pr != null) {
				pr.set_operation("Loading metadata");
				pr.set_progress(75);
			}
			spectrumMetadataPanel.setForm(mdec.getForm());
			
			// force re-draw
			validate();
			this.repaint();
			
			if (pr != null) {
				pr.set_operation("Done.");
				pr.set_progress(100);
			}
			
		}
		catch (SPECCHIOClientException ex) {
			// error contacting the server
			ErrorDialog error = new ErrorDialog(this, "Error", ex.getUserMessage(), ex);
			error.setVisible(true);
		}
		endOperation();
		
	}
	
	
	/**
	 * Handler for starting a potentially long-running operation.
	 */
	private void startOperation() {
		
		// change the cursor to its "wait" state
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
	}
	
	
	/**
	 * Slider handler.
	 * 
	 * @param event	the event generated by the slider
	 */
	public void stateChanged(ChangeEvent event) {
	
		// display the spectrum corresponding to the position of the slider
		setDisplayedIndex((Integer)spectrumSpinner.getValue());
		
	}
	
	
	/**
	 * Category selection handler.
	 * 
	 * @param event	the event generated by the category list
	 */
	public void valueChanged(ListSelectionEvent event) {
		
		try {
			// change the form displayed by the metadata panel
			mdec.set_form_descriptor(categoryList.getFormDescriptor(), true);
			spectrumMetadataPanel.setForm(mdec.getForm());
		}
		catch (SPECCHIOClientException ex) {
			// error contacting the server
			ErrorDialog error = new ErrorDialog(this, "Error", ex.getUserMessage(), ex);
			error.setVisible(true);
		}
		
	}

}