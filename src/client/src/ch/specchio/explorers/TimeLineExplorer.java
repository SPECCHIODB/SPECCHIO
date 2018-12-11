package ch.specchio.explorers;

import java.awt.GridBagConstraints;

import javax.swing.JPanel;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.gui.GridbagLayouter;
import ch.specchio.interfaces.ProgressReportInterface;
import ch.specchio.plots.PlotsCallback;
import ch.specchio.plots.SPECCHIOPlotException;
import ch.specchio.plots.swing.SpectralLinePlot;
import ch.specchio.plots.swing.SpectralPlot;
import ch.specchio.plots.swing.TimelinePlot;
import ch.specchio.spaces.SpectralSpace;
import ch.specchio.types.MatlabAdaptedArrayList;

public class TimeLineExplorer extends Explorer implements PlotsCallback
{
	private static final long serialVersionUID = 1L;
	
	SpectralSpace space;
	
	TimelinePlot time_line_plot;
	SpectralPlot sp;
	
	public TimeLineExplorer(SPECCHIOClient specchio_client , SpectralSpace space, ProgressReportInterface pr) throws SPECCHIOClientException, SPECCHIOPlotException
	{
		this.space = space;
		this.pr = pr;
			
		GridbagLayouter panel_l = new GridbagLayouter(this);
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;	
		
		// Time Line Plot
		pr.set_operation("Reading timeline from database.");
		MatlabAdaptedArrayList<Object> time_vector = specchio_client.getMetaparameterValues(space.getSpectrumIds(), "Acquisition Time");
		time_line_plot = new TimelinePlot(space, time_vector, 400, 400, pr);		
		time_line_plot.set_callback(this);
		panel_l.insertComponent(time_line_plot, constraints);
		
		// Spectral Plot
		sp = new SpectralLinePlot(space, 300,200, pr);
		constraints.gridx = 1;
		constraints.gridy = 0;		
		panel_l.insertComponent(sp, constraints);
		sp.setShow_wvl_indicator(true);
		
		time_line_plot.enable_indicator(true);
		
	}


	public void data_point_selected(int point_id) 
	{
		sp.plot(point_id);		
	}


	public void band_selected(int band_id) 
	{
		sp.set_wvl_indicator(space.get_wvl_of_band(band_id));
	}

}
