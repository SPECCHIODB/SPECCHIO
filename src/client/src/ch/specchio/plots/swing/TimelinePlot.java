package ch.specchio.plots.swing;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import java.util.Date;


import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.joda.time.DateTime;

import ch.specchio.explorers.Explorer;
import ch.specchio.gui.GridbagLayouter;
import ch.specchio.interfaces.ProgressReportInterface;
import ch.specchio.plots.PlotsCallback;
import ch.specchio.plots.SPECCHIOPlotException;
import ch.specchio.spaces.SpectralSpace;
import ch.specchio.types.MatlabAdaptedArrayList;


public class TimelinePlot extends Explorer implements ListSelectionListener, ChartMouseListener{

	private static final long serialVersionUID = 1L;
	
	SpectralSpace space;
	Date[] time_vector;
	Integer spectrum_id_array[];
	Integer band = 0;
	int time_ind = 0;
	PlotsCallback cb;
	
	boolean indicator_enabled = false;
	
	DefaultListModel list_model;
	JList listbox;

	private JFreeChart chart;

	private TimeSeriesCollection dataset;

	private Crosshair crosshairX;

	private Crosshair crosshairY;

	private ChartPanel panel;
	
	

	
	public TimelinePlot(SpectralSpace space, MatlabAdaptedArrayList<Object> time_vector2, int x_size, int y_size, ProgressReportInterface pr) throws SPECCHIOPlotException
	{
		this.space = space;
		this.time_vector = new Date[time_vector2.size()];
		
		// convert to calendar objects
		int i = 0;
		for(Object t : time_vector2)
		{
			time_vector[i] = ((DateTime)t).toDate();	
			i++;
		}
		
		this.pr = pr;
		pr.set_component("Timeline Plot");
		
		ArrayList<Integer> spectrum_ids = space.getSpectrumIds();
		spectrum_id_array = spectrum_ids.toArray(new Integer[spectrum_ids.size()]);
		
		setup_plot();
		
		listbox.setSelectedIndex(0);
	}
	
	public void enable_indicator(boolean enable)
	{
		indicator_enabled = enable;
		
		if(indicator_enabled)
		{			
		    CrosshairOverlay overlay = new CrosshairOverlay();
		    crosshairX = new Crosshair(dataset.getStartXValue(0, 0));
		    crosshairX.setPaint(Color.BLACK);
		    crosshairY = new Crosshair(dataset.getStartYValue(0, 0));
		    crosshairY.setPaint(Color.BLACK);
		    overlay.addDomainCrosshair(crosshairX);
		    overlay.addRangeCrosshair(crosshairY);
		    panel.addOverlay(overlay);
		    
			if(cb != null)
				this.cb.data_point_selected(space.getSpectrumIds().get(0));		    
		    
//		    crosshairX.setLabelVisible(true);
//		    crosshairX.setLabelAnchor(RectangleAnchor.BOTTOM_RIGHT);
//		    crosshairX.setLabelBackgroundPaint(new Color(255, 255, 0, 100));
		    crosshairY.setLabelVisible(true);
		    crosshairY.setLabelBackgroundPaint(new Color(255, 255, 0, 100));		    
		}
		
	}
	
	private void setup_plot() throws SPECCHIOPlotException
	{
		
		pr.set_operation("Setting up plot");		
		
		try {
			dataset = getDataSet();
		}
		catch (SeriesException ex) {
			// not a valid time series
			throw new SPECCHIOPlotException("Two spectra with the same timestamp cannot be included on a single plot.", ex);
		}
		
		chart = ChartFactory.createTimeSeriesChart(
				"Spectral Time Series",  // title
				"Date",             // x-axis label
				space.getMeasurementUnit().getUnitName(),   // y-axis label
				dataset,            // data
				true,               // create legend?
				true,               // generate tooltips?
				false               // generate URLs?
				);		
		
		XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer(true, false);
		
		renderer1.setSeriesPaint(0, Color.red);
//		renderer1.setSeriesStroke(0, new BasicStroke(4.0f,
//                BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
//		Shape arg1 = new Shape();
//
//		renderer1.setSeriesShape(0, arg1);
//		
		//renderer1.setShapesVisible(true);
		renderer1.setSeriesShapesVisible(3, true);
		renderer1.setSeriesShapesVisible(0, true);
		
		
		//renderer1.setS

        final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setRenderer(renderer1);
		
        panel = new ChartPanel(chart);
		panel.setFillZoomRectangle(true);
		panel.setMouseWheelEnabled(true);
		panel.addChartMouseListener(this);
		
		
		
		GridbagLayouter panel_l = new GridbagLayouter(this);
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;	
		panel_l.insertComponent(panel, constraints);
		
		// add a listbox with all wavelengths to the bottom of the graph
		pr.set_operation("Getting bands.");
		list_model = new DefaultListModel();
		NumberFormat f = NumberFormat.getInstance();
		f.setMaximumFractionDigits(2);

		double[] wvl = space.getAverageWavelengths();
		
		for(int i = 0; i < space.getDimensionality();i++)
		{
			list_model.addElement(f.format(wvl[i]));
		}
		
		listbox = new JList(list_model);
		
		listbox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listbox.addListSelectionListener(this);
		
		JScrollPane listScroller = new JScrollPane(listbox);
		listScroller.setPreferredSize(new Dimension(200, 80));
		
		JLabel label = new JLabel("Instrument Channels:");
		
		JPanel wvl_selection_panel = new JPanel();
		GridbagLayouter wvl_selection_panel_l = new GridbagLayouter(wvl_selection_panel);
		
		constraints.gridx = 0;
		constraints.gridy = 0;	
		
		wvl_selection_panel_l.insertComponent(label, constraints);
		constraints.gridy++;
		wvl_selection_panel_l.insertComponent(listScroller, constraints);

		panel_l.insertComponent(wvl_selection_panel, constraints);

		
	}
	
	
	
	private TimeSeriesCollection getDataSet() throws SeriesException
	{
		
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		
		Integer inc_band = band+ 1;
		
		TimeSeries s = new TimeSeries("Band " + inc_band.toString() + "(" + space.get_wvl_of_band(band) + "nm]" + " Time Plot");
		
		
		ArrayList<double[]> vectors = space.getVectors();

		
		for(int d = 0; d < vectors.size();d++)
		{			
			Millisecond time = new Millisecond(time_vector[d]);
			//s.addOrUpdate(time , vectors.get(d)[band]);
			s.add(time , vectors.get(d)[band]);
		}
		
		dataset.addSeries(s);		
		
		return dataset;
	}
	

	void plot()
	{		
		chart.getXYPlot().setDataset(0, getDataSet());
	}

	public void valueChanged(ListSelectionEvent e) 
	{
		band = listbox.getSelectedIndex();
		plot();
		if(cb != null)
			this.cb.band_selected(band);
	}
	
	public void set_callback(PlotsCallback cb)
	{
		this.cb = cb;
	}

	@Override
	public void chartMouseClicked(ChartMouseEvent arg0) {
		
		// get item number if we got an xy item
		if(arg0.getEntity() instanceof XYItemEntity)
		{
			XYItemEntity tmp = ((XYItemEntity) arg0.getEntity());
			int no = tmp.getItem();
			
			// get spectrum id of this item
			int spectrum_id = this.spectrum_id_array[no];
			
			if(cb != null)
				this.cb.data_point_selected(spectrum_id);
			
			if(indicator_enabled)
			{			
				this.crosshairX.setValue(tmp.getDataset().getXValue(0, no));
				this.crosshairY.setValue(tmp.getDataset().getYValue(0, no));
			}
		}

		
	}

	@Override
	public void chartMouseMoved(ChartMouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	

}
