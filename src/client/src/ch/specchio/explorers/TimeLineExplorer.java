package ch.specchio.explorers;

import java.awt.*;

import javax.swing.*;

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
		
		// Time Line Plot
		pr.set_operation("Reading timeline from database.");
		MatlabAdaptedArrayList<Object> time_vector = specchio_client.getMetaparameterValues(space.getSpectrumIds(), "Acquisition Time");
		time_line_plot = new TimelinePlot(space, time_vector, pr);
		time_line_plot.set_callback(this);
		
		// Spectral Plot
		sp = new SpectralLinePlot(space, pr);
		sp.setShow_wvl_indicator(true);
		
		time_line_plot.enable_indicator(true);

		JPanel main_panel = new JPanel();
		main_panel.setLayout(new BorderLayout());
		main_panel.add(time_line_plot, BorderLayout.CENTER);
		main_panel.add(sp, BorderLayout.SOUTH);

		this.getViewport().add(main_panel);
		
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
