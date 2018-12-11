package ch.specchio.plots.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.explorers.Explorer;
import ch.specchio.plots.GonioPosition;
import ch.specchio.plots.GonioSamplingPoints;
import ch.specchio.plots.PlotsCallback;
import ch.specchio.spaces.SpectralSpace;

//import com.quinncurtis.chart3djava.Background;
//import com.quinncurtis.chart3djava.CartesianCoordinates;
//import com.quinncurtis.chart3djava.ChartAttribute;
//import com.quinncurtis.chart3djava.ChartConstants;
//import com.quinncurtis.chart3djava.ChartPoint3D;
//import com.quinncurtis.chart3djava.ChartTitle;
//import com.quinncurtis.chart3djava.ChartView;
//import com.quinncurtis.chart3djava.ContourDataset;
//import com.quinncurtis.chart3djava.DataCursor;
//import com.quinncurtis.chart3djava.Grid;
//import com.quinncurtis.chart3djava.LinearAxis;
//import com.quinncurtis.chart3djava.Marker;
//import com.quinncurtis.chart3djava.NearestPointData;
//import com.quinncurtis.chart3djava.NumericAxisLabels;
//import com.quinncurtis.chart3djava.NumericLabel;
//import com.quinncurtis.chart3djava.SimpleScatterPlot;


public class SamplingPoints2DPlot  extends Explorer{

	private static final long serialVersionUID = 1L;
	
	SpectralSpace space;

	int band;
	GonioSamplingPoints sampling_points;
//	ChartView cv;
//	ContourDataset dataset;
//	SimpleScatterPlot point_plot;
//	SimpleScatterPlot highlight_point;
//	ChartAttribute attribs[];
//	CartesianCoordinates pTransform1;
//	LinearAxis xAxis, yAxis;
//	CustomChartDataCursor dataCursorObj;
	PlotsCallback cb = null;
	int selected_point = 0;
	
//	class CustomChartDataCursor extends DataCursor {
//	    CartesianCoordinates chartscale;
//	    NumericLabel pointLabel;
//	    Font textCoordsFont = new Font("SansSerif", Font.PLAIN,10);
//	    Marker amarker = null;
//
//	   public CustomChartDataCursor(ChartView achartview, CartesianCoordinates thetransform,
//	                       int nmarkertype,
//	                       double rsize)
//	  {
//	        super(achartview, thetransform, nmarkertype, rsize);
//	  }
//
//	  // override mouseReleased method in order to add stuff
//	   public void mouseReleased (MouseEvent event)
//	    {  
//	       NearestPointData nearestPointObj =  new NearestPointData();
//	       ChartPoint3D nearestPoint = new ChartPoint3D(0,0);
//	       
//	       boolean bfound1 = false;
//
//	      super.mouseReleased(event);
//
//	      if ((event.getModifiers() & getButtonMask() ) != 0)
//	      {
//
//	       // Find nearest point for each line plot object
//	        ChartPoint3D location = getLocation();
//	        bfound1 = point_plot.calcNearestPoint(location,ChartConstants.FNP_NORMDIST, nearestPointObj);
//
//
//	      if (bfound1)
//	      {
//	      
//	      
//	        nearestPoint =  nearestPointObj.getNearestPoint();
//	        //System.out.println(nearestPointObj.nearestPointIndex);
//	        
//	        selected_point = nearestPointObj.nearestPointIndex;
//	        
//	        // inform callback object of point selection
//	        if(cb != null)
//	        	cb.data_point_selected(sampling_points.get_spectrum_id(selected_point));
//
//	        set_marker(nearestPoint.getX(), nearestPoint.getY());
//	        
//	        //cv.updateDraw();
//	        //System.out.println(sampling_points.pointarray.length);
//
//	     }
//	    }
//	  }
//	   
//	   
//	   public void set_marker(double x, double y)
//	   {
//		   ChartView chartview = getChartObjComponent();
//		      // create marker object at place it at the nearest point
//		      if(amarker == null)
//		      {
//		    	  amarker = new Marker(getChartObjScale(), MARKER_BOX, x, y,  10.0, PHYS_POS);
//		    	  chartview.addChartObject(amarker);
//		      }
//		      else
//		      {
//		    	  amarker.setLocation(x, y);
//		      }
//
//		      chartview.updateDraw();	   
//	   }
//	   
//	   
//	}
	
	
	
	public SamplingPoints2DPlot(SpectralSpace space, int x_size, int y_size, SPECCHIOClient specchio_client) throws SPECCHIOClientException
	{
		this.space = space;
		
//		cv = new ChartView();
//		cv.setPreferredSize(x_size, y_size);
		
		sampling_points = specchio_client.getSensorSamplingGeometry(space);			
		plot();
		this.highlight(selected_point);

	}
	
	public void plot()
	{	
		GonioPosition[] pos = sampling_points.get_positions();
//		ChartPoint3D[] pointarray = new ChartPoint3D[pos.length];
//		
//		for(int i=0;i<pos.length;i++)
//		{
//			GonioPosition cur_pos = pos[i];
//			ChartPoint3D p = new ChartPoint3D();
//			p.setLocation(cur_pos.x, cur_pos.y);
//			pointarray[i] = p;
//		}
//		
//	
//		dataset = new ContourDataset("",pointarray);
//		pTransform1 = new CartesianCoordinates(-1, -1, 0, 1, 1, 1);
//		pTransform1.setGraphBorderDiagonal(0.20, .10, .85, 0.8) ;
//		
//		   Background background = new Background( pTransform1, ChartConstants.GRAPH_BACKGROUND, Color.white);
//		   cv.addChartObject(background);
//
//		  xAxis = new LinearAxis(pTransform1, ChartConstants.X_AXIS);
//		  cv.addChartObject(xAxis);
//
//
//		  yAxis = new LinearAxis(pTransform1, ChartConstants.Y_AXIS);
//		  cv.addChartObject(yAxis);
//		  
//		xAxis.calcAutoAxis();
//		yAxis.calcAutoAxis();
//
//		  NumericAxisLabels xAxisLab = new NumericAxisLabels(xAxis );
//		  cv.addChartObject(xAxisLab);
//
//		  NumericAxisLabels yAxisLab = new NumericAxisLabels(yAxis);
//		  cv.addChartObject(yAxisLab);
//
//		  Grid xgrid = new Grid(xAxis, yAxis,ChartConstants.X_AXIS, ChartConstants.GRID_MAJOR);
//		  cv.addChartObject(xgrid);
//
//		  Grid ygrid = new Grid(xAxis, yAxis,ChartConstants.Y_AXIS, ChartConstants.GRID_MAJOR);
//		  cv.addChartObject(ygrid);
//
//		   ChartAttribute attrib1 = new ChartAttribute (Color.blue, 1,ChartConstants.LS_SOLID);
//
//		   attrib1.setFillColor(Color.blue);
//
//		   attrib1.setFillFlag(true);
//
//		   attrib1.setSymbolSize(5);
//
//
//		point_plot = new SimpleScatterPlot(pTransform1, dataset,  ChartConstants.PLUS3D, attrib1);
//
//		  
//		cv.addChartObject(point_plot);
//		
//		dataCursorObj = new CustomChartDataCursor( cv, pTransform1, ChartConstants.MARKER_CROSS, 24.0);
//		dataCursorObj.setDataCursorEnable(true);
//		dataCursorObj.addDataCursorListener();
//		
//		Font theTitleFont = new Font("SansSerif", Font.BOLD,12);
//
//		ChartTitle mainTitle = new ChartTitle( theTitleFont, "Goniometer Sampling points");
//		mainTitle.setTitleType(ChartConstants.CHART_HEADER);
//		mainTitle.setTitlePosition( ChartConstants.CENTER_GRAPH);
//
//		cv.addChartObject(mainTitle);
//		  
//
//
//		this.add(cv);
		  
		  
	}
	
	public void highlight(int ind)
	{
//		dataCursorObj.set_marker(sampling_points.get_points()[ind].x, sampling_points.get_points()[ind].y);		
	}
	
	public void set_callback(PlotsCallback cb)
	{
		this.cb = cb;
	}
	
	public GonioPosition get_angles_of_selected_point()
	{
//		return sampling_points.get_angles()[selected_point];
		return null;
	}
	
	public GonioSamplingPoints get_sampling_points()
	{
		return this.sampling_points;
	}
	
	public int get_selected_point_spectrum_id()
	{
		return sampling_points.get_spectrum_id(selected_point);
	}

}
