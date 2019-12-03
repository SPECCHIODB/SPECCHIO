package ch.specchio.spaces;

import java.util.ArrayList;

import javax.xml.bind.annotation.*;

@XmlRootElement(name="space")
@XmlSeeAlso({RefPanelCalSpace.class,SensorAndInstrumentSpace.class,SpectralSpace.class})
public abstract class Space {
	
	ArrayList<Integer> spectrum_ids;
	Integer dimensionality;
	Integer selectedBand;
	ArrayList<double[]> vectors;
	String SpaceTypeName;
	String space_name_str = "";
	int measurement_unit;
	int all_data_read_cnt = 0;
	ArrayList<Integer> following_modules_data_ready_status = new ArrayList<Integer>(); // true means data can be read, false means no data available (or already processed)
	
	boolean wvls_are_known;
	boolean dimensionality_is_set = false;
	
	private String order_by;
	
	public Space()
	{
		this.spectrum_ids = new ArrayList<Integer>();
		this.vectors = new ArrayList<double[]>();
		this.order_by = "date";
		this.selectedBand = 0;
	}
	
	
	@XmlElement(name="dimensionality")
	public Integer getDimensionality() { return this.dimensionality; }
	public void setDimensionality(Integer dimensionality) { this.dimensionality = dimensionality; setDimensionalityIsSet(true); }
	
	@XmlElement(name="dimensionality_is_set")
	public boolean getDimensionalityIsSet() { return this.dimensionality_is_set; }
	public void setDimensionalityIsSet(boolean dimensionality_is_set) { this.dimensionality_is_set = dimensionality_is_set; }
	
	@XmlElement(name="order_by")
	public String getOrderBy() { return this.order_by; }
	public void setOrderBy(String order_by) { this.order_by = order_by; }
	
	@XmlElement(name="space_name_str")
	public String getSpaceNameString() { return this.space_name_str; }
	public void setSpaceNameString(String space_name_str) { this.space_name_str = space_name_str; }
	
	@XmlElement(name="SpaceTypeName")
	public String getSpaceTypeName() { return this.SpaceTypeName; }
	public void setSpaceTypeName(String SpaceTypeName) { this.SpaceTypeName = SpaceTypeName; }
	
	@XmlElement(name="spectrum_ids")
	public ArrayList<Integer> getSpectrumIds() { return this.spectrum_ids; }
	public void setSpectrumIds(ArrayList<Integer> spectrum_ids) { this.spectrum_ids = spectrum_ids; }
	
	@XmlElement(name="vectors")
	public ArrayList<double[]> getVectors() { return this.vectors; }
	public void setVectors(ArrayList<double[]> vectors) { this.vectors = vectors; }
	public void setSelectedBand(Integer selectedBand) {	this.selectedBand = selectedBand; }
	public void addVector(double[] vector) { vectors.add(vector); }
	public void clearDataVectors() { vectors.clear(); }
	public int getNumberOfDataPoints() { return vectors.size(); }
	public double[] getVector(int spectrum_id) { return vectors.get(spectrum_ids.indexOf(spectrum_id)); }
	public double[][] getVectorsAsArray(ArrayList<Integer> in_spectrum_ids) { 
		
		double[][] array = new double[in_spectrum_ids.size()][getDimensionality()];		
		
		int i = 0;
		int j = 0;
		for(double[] v : vectors)
		{
			if(in_spectrum_ids.contains(spectrum_ids.get(j)))
				array[i++] = v;
			j++;
		}	

		return array;

	}
	public Integer getSelectedBand() { return selectedBand; }
	public double[][] getVectorsAsArray()
	{
		double[][] array = new double[getNumberOfDataPoints()][getDimensionality()];		
		
		int i = 0;
		for(double[] v : vectors)
		{
			array[i++] = v;
		}	
		
		return array;
	}	
	
	@XmlElement(name="wvls_are_known")
	public boolean getWvlsAreKnown() { return this.wvls_are_known; }
	public void setWvlsAreKnown(boolean wvls_are_known) { this.wvls_are_known = wvls_are_known; }
	
	
	public abstract int getSpaceType();
	
	
	public void add_unique_spectrum_id(Integer id)
	{
		if(!spectrum_ids.contains(id))
		{
			spectrum_ids.add(id);
		}

	}
	
	public void clear_space()
	{
		spectrum_ids.clear();
		vectors.clear();
	}
	
	// for spectral space, this should be the wavelength, for other spaces, just a number given to the dimension
	public double get_dimension_number(int dim_index)
	{
		return dim_index + 1; // start counting at 1
	}
	
	public double get_vector_element(int spectrum_id, int band_index)
	{
		int vector_index = spectrum_ids.indexOf(spectrum_id);		
		return vectors.get(vector_index)[band_index];
	}

	public String get_filename_addon() {
		// TODO Auto-generated method stub
		return "";
	}

}
