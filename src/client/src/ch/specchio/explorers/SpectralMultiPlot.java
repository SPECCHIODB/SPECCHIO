package ch.specchio.explorers;

import javax.swing.JPanel;

import ch.specchio.gui.ErrorDialog;
import ch.specchio.gui.SPECCHIOApplication;
import ch.specchio.interfaces.ProgressReportInterface;
import ch.specchio.plots.PlotsCallback;
import ch.specchio.plots.swing.SpectralLinePlot;
import ch.specchio.plots.swing.SpectralPlot;
import ch.specchio.plots.swing.SpectralScatterPlot;
import ch.specchio.spaces.SpectralSpace;

public class SpectralMultiPlot  extends Explorer implements PlotsCallback
{
	private static final long serialVersionUID = 1L;
	
	SpectralSpace space;
	
	SpectralPlot sp;
	
	public SpectralMultiPlot(SpectralSpace space, ProgressReportInterface pr, int plot_type)
	{
		
		this.space = space;
		this.pr = pr;
		
		// Spectral Plot
		if(plot_type == 0)
			sp = new SpectralLinePlot(space, 700,500, pr);	
		else
			sp = new SpectralScatterPlot(space, 700,500, pr);	
		
		try
		{
			sp.plot_all();
					
			this.add(sp);
			
			plot_ready = true;
		}
		catch(Exception e)
		{
			ErrorDialog error = new ErrorDialog(
					SPECCHIOApplication.getInstance().get_frame(),
					"Plotting failed",
    				"For details see error message below.\n Increase the Java heap space in case of Out of Memory Error.",
    				e
    		);
    		error.setVisible(true);			
		}
		
	}

	public void band_selected(int band_id) {
		// TODO Auto-generated method stub
		
	}

	public void data_point_selected(int point_id) {
		// TODO Auto-generated method stub
		
	}
	
	

}
