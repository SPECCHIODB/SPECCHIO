package ch.specchio.plots.swing;


import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.JPanel;


import ptolemy.plot.Plot;

import ch.specchio.interfaces.ProgressReportInterface;
import ch.specchio.plots.VectorStatistics;
import ch.specchio.spaces.MeasurementUnit;
import ch.specchio.spaces.SpectralSpace;



public abstract class SpectralPlot extends JPanel
{

	private static final long serialVersionUID = 1L;
	SpectralSpace space;
	double wavelength;
	int band;
	int no_of_chunks;
	
	boolean plot_versus_wvl = true; // default behaviour is to plot versus wavelengths and not band numbers


	int numcontourlevels = 13;
	double contourlevels[];


	
	// Ptolemy plotting
	Plot plot;
	
	ProgressReportInterface pr;
	private int currentDataset;
	private int wvl_indicator_dataset_no;
	private boolean wvl_indicator_on = false;
	private boolean show_wvl_indicator = false;
	
	public SpectralPlot(SpectralSpace space, int x_size, int y_size, ProgressReportInterface pr)
	{
		this.space = space;
		this.pr = pr;
		
		if(pr != null) pr.set_component("Spectral Plot");
		
		if (space.getAverageWavelengths() != null) {
			if (space.getAverageWavelengths().length > 0) {
				wavelength = space.getAverageWavelengths()[0];
			}
		}		
		
		space.getMeasurementUnit();
		if(space.getMeasurementUnit().getUnitNumber() == MeasurementUnit.Wavelength)
		{
			plot_versus_wvl = false;
			space.setWvlsAreKnown(false); // forces plots versus bands label
		}
		
        plot = new Plot();    	
    	
        plot.setSize(x_size, y_size);
        plot.setButtons(true);
        plot.setTitle("Spectral Plot");
        plot.setXLabel(space.get_x_axis_label());
        plot.setMarksStyle("none");
		
		setup_plot();

	}
	
	
	public void setup_plot()
	{
				
		if(pr != null) pr.set_operation("Setting up plot.");				
		
		BorderLayout bl = new BorderLayout();
		this.setLayout(bl);
		this.add(plot, BorderLayout.CENTER);				
		
	}
	
	
	public void plot_all()
	{
//		System.out.println(space.getVectors().size());
		
		ArrayList<double[]> vectors = space.getVectors();		
    	ArrayList<Integer> ids = space.getSpectrumIds();
    	double[] vector;

    	for(currentDataset= 0; currentDataset < space.getNumberOfDataPoints();currentDataset++)
    	{
    		vector = space.getVector(ids.get(currentDataset));
    		
    		for ( int i = 0; i < vector.length; i++ ) {
    			if(plot_versus_wvl)
    				plot.addPoint(currentDataset,space.get_wvl_of_band(i), vector[i], true);
    			else
    				plot.addPoint(currentDataset,i+1, vector[i], true);
    		}   
    	}
    	
    	wvl_indicator_dataset_no = currentDataset;
		

		// adjust axis for new spectrum		
		if(space.getMeasurementUnit().getUnitNumber() == MeasurementUnit.Reflectance)
		{
			// do statistics for VNIR
			double vis_nir_start = 300;
			double vis_nir_end = 1300;
			
			int start_ind = space.get_index_of_band(vis_nir_start);
			int end_ind = space.get_index_of_band(vis_nir_end);
			
			if (start_ind >= 0 & end_ind > 0)
			{			
				VectorStatistics stats = new VectorStatistics();			
				stats.calc_stats(vectors, start_ind, end_ind);					
				plot.setYRange(0, stats.mean()+3*stats.standardDeviation());			
			}
			
		}

		plot.setYLabel(this.space.getMeasurementUnit().getUnitName());
		set_wvl_indicator(wavelength); // make sure the wavelength indicator is updated ...

	}
	
	
	
	public void plot(int spectrum_id)
	{

		double[] vector = space.getVector(spectrum_id);
		
		currentDataset= 0;
		plot.clear(true);
		
		for ( int i = 0; i < vector.length; i++ ) {
			if(plot_versus_wvl)
				plot.addPoint(currentDataset,space.get_wvl_of_band(i), vector[i], true);
			else
				plot.addPoint(currentDataset,i+1, vector[i], true);			
		}   
		
		wvl_indicator_dataset_no = 1;
		

		// adjust y axis for new spectrum if it is reflectances
		if(space.getMeasurementUnit().getUnitNumber() == MeasurementUnit.Reflectance)
		{

			double vis_nir_start = 500;
			double vis_nir_end = 1700;
			
			VectorStatistics stats = new VectorStatistics();
			
			stats.calc_stats(vector, space.get_index_of_band(vis_nir_start), space.get_index_of_band(vis_nir_end));
			
			plot.setYRange(0, stats.mean()+1*stats.standardDeviation());
			
		}
		
		plot.setXLabel(space.get_x_axis_label());
		plot.setYLabel(this.space.getMeasurementUnit().getUnitName());


		set_wvl_indicator(wavelength); // make sure the wavelength indicator is updated ...
		
		plot.repaint();

	}
	
	
	
	public void set_wvl_indicator(double wvl)
	{
		if(show_wvl_indicator)
		{
			//System.out.println("show_wvl_indicator");
			
			wavelength = wvl;
			double[] x = new double[1]; 
			double[] y = new double[1]; 
			x[0] = wvl;
			double[] tmp = plot.getYRange();
			y[0] = tmp[1];
			y[0] -= y[0]/10;
			
			
			if(wvl_indicator_on)
			{
				// remove previous wvl indicator
				plot.clear(wvl_indicator_dataset_no);
			}
			
			wvl_indicator_on = true;
						
			plot.addPoint(this.wvl_indicator_dataset_no,wvl, 0, true);
			plot.addPoint(this.wvl_indicator_dataset_no,wvl, y[0], true);	
		
		}
		
	}
	


	public boolean isShowing_wvl_indicator() {
		return show_wvl_indicator;
	}


	public void setShow_wvl_indicator(boolean show_wvl_indicator) {
		this.show_wvl_indicator = show_wvl_indicator;
	}

}
