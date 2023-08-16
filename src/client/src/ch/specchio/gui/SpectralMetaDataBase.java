package ch.specchio.gui;

import java.util.ArrayList;
import java.util.ListIterator;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JRadioButton;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.proc_modules.ModuleException;
import ch.specchio.proc_modules.SpaceProcessingChainComponent;
import ch.specchio.proc_modules.VisualisationModule;
import ch.specchio.queries.Query;
import ch.specchio.queries.QueryCondition;
import ch.specchio.query_builder.QueryController;
import ch.specchio.spaces.Space;

public class SpectralMetaDataBase extends JFrame {
	
	private static final long serialVersionUID = 1L;
	public SpectralDataBrowser sdb;
	SPECCHIOClient specchio_client;
	ArrayList<Integer> ids_matching_query;	
	protected ArrayList<Integer> unsorted_spectrum_ids;	
	boolean sorted_ids_ready = false;
	
	JRadioButton split_spaces_by_sensor_and_unit = new JRadioButton("Split spaces by sensor and unit");
	JRadioButton split_spaces_by_sensor = new JRadioButton("Split spaces by sensor");
	JRadioButton split_spaces_by_sensor_and_unit_and_instrument_and_cal = new JRadioButton("Split spaces by sensor, instrument, calibr_no and unit");
	
	ButtonGroup split_group = new ButtonGroup();
	
	public Query query;
	

	public SpectralMetaDataBase(String frame_title) throws SPECCHIOClientException
	{
		super(frame_title);
		
		// get a reference to the application's client object
		this.specchio_client = SPECCHIOApplication.getInstance().getClient();
		
		split_spaces_by_sensor_and_unit.setSelected(true);
	}
	
	


		public void set_ids_matching_query(ArrayList<Integer> ids)
		{
			ids_matching_query = ids;
		}
		
		public ArrayList<Integer> get_ids_matching_query_not_sorted()
		{	
			if (unsorted_spectrum_ids == null) // this is true for id selections not carried out in the spectral data browser
				return this.ids_matching_query;
			else
				return this.unsorted_spectrum_ids;
		}	
	
	/**
	 * Thread for building visualisations.
	 */
	class VisualisationThread extends Thread {
		
		/** the plot type */
		private String plotType;
		
		/** spectrum identifiers on which to report */
		private ArrayList<Integer> ids;
		
		/** split spaces by sensor */
		private boolean bySensor;
		
		/** split spaces by sensor and unit */
		private boolean bySensorAndUnit;
		
		/** field to order by */
		private String orderBy;
		
		
		/**
		 * Constructor.
		 * 
		 * @param plotTypeIn		the plot type
		 * @param idsIn				the spectrum identifiers to be visualised
		 * @param bySensorIn			split spaces by sensor
		 * @param bySensorAndUnitIn	split spaces by sensor and unit
		 * @param orderByIn			field to order by
		 */
		public VisualisationThread(String plotTypeIn, ArrayList<Integer> idsIn, boolean bySensorIn, boolean bySensorAndUnitIn, String orderByIn)
		{
			// save parameters for later
			plotType = plotTypeIn;
			ids = idsIn;
			bySensor = bySensorIn;
			bySensorAndUnit = bySensorAndUnitIn;
			orderBy = orderByIn;
		}
		
		
		
		
		/**
		 * Thread entry point.
		 */
		public void run()
		{
	  	    // create a progress report
			ProgressReportDialog pr = new ProgressReportDialog( SpectralMetaDataBase.this, plotType, false, 20);
			pr.set_operation("Opening " + plotType);
			pr.set_progress(0);
			pr.setVisible(true);
			
		  	try {
		      	VisualisationModule VM;
		      	
		      	pr.set_operation("Identifying spaces");
		      	Space[] spaces = specchio_client.getSpaces(
		      			ids,
		      			bySensor,
		      			bySensorAndUnit,
		      			orderBy
		      		);
		      	pr.set_progress(100);
					
		      	Integer i = 0;
				for (Space space : spaces)
				{
					pr.set_operation("Loading space " + i);
					pr.set_progress(0);
					space = specchio_client.loadSpace(space);
					pr.set_progress(50);
					
					//System.out.println(s.getNumberOfDataPoints()); // switch this on when debugging ; my machine got runtime issues in the debugger and not always plots the spectra ...
					
					pr.set_operation("Building plot");
					VM = new VisualisationModule(SpectralMetaDataBase.this, specchio_client);
					SpaceProcessingChainComponent c = new SpaceProcessingChainComponent(SpectralMetaDataBase.this, space);
					c.setNumber(i);
					VM.add_input_space(c, -1);
					VM.set_vis_module_type(plotType);
					//VM.transform();
					VM.process();
					pr.set_progress(100);
					
					i++;
				}
		  	}
			catch (SPECCHIOClientException ex) {
		  		ErrorDialog error = new ErrorDialog(
		  				SpectralMetaDataBase.this,
			    		"Server error",
			    		ex.getUserMessage(),
			    		ex
				    );
			  		error.setVisible(true);
		    }
		  	catch (ModuleException ex) {
		  		ErrorDialog error = new ErrorDialog(
		  				SpectralMetaDataBase.this,
		  				"Module error",
		  				ex.getMessage(),
		  				ex
		  			);
		  		error.setVisible(true);
		  	}
		  	
		  	pr.setVisible(false);
			
		}
		
	}

}
