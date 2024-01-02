package ch.specchio.explorers;

import java.awt.Dimension;
import java.awt.GridBagConstraints;


import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.gui.GridbagLayouter;
import ch.specchio.interfaces.ProgressReportInterface;
import ch.specchio.plots.PlotsCallback;
import ch.specchio.plots.swing.DirectionalPlot;
import ch.specchio.plots.swing.GonioAnglePointInfoPanel;
import ch.specchio.plots.swing.SamplingPoints2DPlot;
import ch.specchio.plots.swing.SpectralLinePlot;
import ch.specchio.plots.swing.VectorStatisticsPanel;
import ch.specchio.spaces.SpectralSpace;

public class SingleHemisphereExplorer extends Explorer implements ChangeListener, PlotsCallback{

	private static final long serialVersionUID = 1L;
	
	SpectralSpace space;
	int band; // currently displayed spectral band
	DirectionalPlot dp;
	SpectralLinePlot sp;
	JSlider band_slider;
	JTextField wvl;
	VectorStatisticsPanel vsp;
	SamplingPoints2DPlot spp;
	GonioAnglePointInfoPanel gap;

	
	public SingleHemisphereExplorer(SpectralSpace space, ProgressReportInterface pr, SPECCHIOClient specchio_client) throws SPECCHIOClientException
	{
		this.space = space;
		this.pr = pr;
		
		Dimension d = new Dimension();		
		GridbagLayouter panel_l = new GridbagLayouter(this);
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;		
		
		// Hemispherical Plot
		dp = new DirectionalPlot(space, 400, 400, pr, specchio_client);			
		panel_l.insertComponent(dp, constraints);
		
		
		// Wavelength/Band Control Panel
		JPanel wvl_band_panel = new JPanel();
		GridbagLayouter wvl_band_panel_l = new GridbagLayouter(wvl_band_panel);
		
		JLabel sliderLabel = new JLabel("Band", JLabel.CENTER);
		band_slider = new JSlider(JSlider.HORIZONTAL, 1, space.getDimensionality(), space.getDimensionality()/2);
		System.out.println(space.getDimensionality());
		d.height = 50;
		d.width = 400;
		band_slider.setPreferredSize(d);
		band_slider.addChangeListener(this);
		band_slider.setMajorTickSpacing(space.getDimensionality()/5);
		//band_slider.setMinorTickSpacing(space.get_dimensionality()/10);
		band_slider.setPaintTicks(true);
		band_slider.setPaintLabels(true);
		
		JLabel wvl_label = new JLabel("Wvl [nm]", JLabel.CENTER);
		wvl = new JTextField(Double.toString(space.get_wvl_of_band(band_slider.getValue())), 30);
		wvl.setEditable(false);
		  
		wvl_band_panel_l.insertComponent(sliderLabel, constraints);
		constraints.gridx = 1;
		wvl_band_panel_l.insertComponent(band_slider, constraints);
		constraints.gridx = 0;
		constraints.gridy = 1;	
		wvl_band_panel_l.insertComponent(wvl_label, constraints);
		constraints.gridx = 1;
		wvl_band_panel_l.insertComponent(wvl, constraints);
		
		
		// insert Wavelength/Band Control Panel
		constraints.gridx = 0;
		constraints.gridy = 1;		
		panel_l.insertComponent(wvl_band_panel, constraints);
		
		// Spectral plot and sampling point position
		JPanel spec_plot_and_pos = new JPanel();
		GridbagLayouter spec_plot_and_pos_panel_l = new GridbagLayouter(spec_plot_and_pos);
		constraints.gridx = 1;
		constraints.gridy = 0;		
		panel_l.insertComponent(spec_plot_and_pos, constraints);
		
		
		// Spectral plot
		sp = new SpectralLinePlot(space, pr);
		sp.setShow_wvl_indicator(true);
		sp.set_wvl_indicator(space.get_wvl_of_band(band_slider.getValue()));
		constraints.gridx = 0;
		constraints.gridy = 0;		
		spec_plot_and_pos_panel_l.insertComponent(sp, constraints);
		
		// Pos Plot		
		spp = new SamplingPoints2DPlot(space, 300,200, specchio_client);
		spp.set_callback(this);
		constraints.gridx = 0;
		constraints.gridy = 1;		
		spec_plot_and_pos_panel_l.insertComponent(spp, constraints);
		
		// Angles of selected point
		gap  = new GonioAnglePointInfoPanel(space, spp.get_sampling_points(), specchio_client);
		
		constraints.gridx = 1;
		constraints.gridy = 1;		
		spec_plot_and_pos_panel_l.insertComponent(gap, constraints);
				
		// Spectrum statistics
		vsp = new VectorStatisticsPanel(space);
		constraints.gridx = 1;
		constraints.gridy = 0;		
		spec_plot_and_pos_panel_l.insertComponent(vsp, constraints);
		
		
		// plot the  spectrum according the selected gonio point
		sp.plot(spp.get_selected_point_spectrum_id());
		
		
		
	}
	
	public Dimension getPreferredSize()
	{
		return new Dimension(1000, 600);
	}

	public void stateChanged(ChangeEvent e) {
	    JSlider source = (JSlider)e.getSource();
	    if (!source.getValueIsAdjusting()) 
	    {
	    	band = (int)source.getValue() - 1;
	    	//System.out.println(band);
	    	
	    	// reflect change in wvl text field
	    	wvl.setText(Double.toString(space.get_wvl_of_band(band)));
	    	
	    	dp.change_wavelength(band);
	    	sp.set_wvl_indicator(space.get_wvl_of_band(band));
	    }
		
	}

	public void data_point_selected(int point_id) 
	{
		//System.out.println(point_id);
		sp.plot(point_id);
		this.vsp.set_context(point_id);
		gap.set_context(point_id);
		
	}

	public void band_selected(int band_id) {
		// TODO Auto-generated method stub
		
	}
	
	

}
