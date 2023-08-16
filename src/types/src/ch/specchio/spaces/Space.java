package ch.specchio.spaces;

import org.ujmp.core.Matrix;
import org.ujmp.core.util.SerializationUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;

import javax.xml.bind.annotation.*;

import static org.ujmp.core.util.SerializationUtil.serialize;

@XmlRootElement(name="space")
@XmlSeeAlso({RefPanelCalSpace.class,SensorAndInstrumentSpace.class,SpectralSpace.class,UncertaintySpace.class})
public abstract class Space {
	
	ArrayList<Integer> spectrum_ids;
	Integer dimensionality;
	Integer selectedBand;
	float selectedWavelength;
	ArrayList<double[]> vectors;

	private ArrayList<Matrix> matrices = new ArrayList<>();
	private ArrayList<String> matrices_serialised = new ArrayList<String>();

	boolean UJMP_storage;
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
		this.selectedBand = null;
	}

	@XmlElement(name="UJMP_storage")
	public boolean isUJMP_storage() { return UJMP_storage; }
	public void setUJMP_storage(boolean UJMP_storage) { this.UJMP_storage = UJMP_storage; }
	
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
	public void setSelectedWavelength(Float selectedWavelength) {
		this.selectedWavelength = selectedWavelength;
	}
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
	public Float getSelectedWavelength() {
		return selectedWavelength;
	}
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


	@XmlTransient
	public void setMeasurementMatrices(ArrayList<Matrix> measurements) { this.matrices = measurements; }
	public Matrix getMeasurementMatrix(int i) { return this.matrices.get(i); }
	public void setMeasurementMatrix(int i, Matrix measurements) { this.matrices.set(i, measurements); }
	public void addMeasurementMatrix(Matrix measurements) { this.matrices.add(measurements); }


	@XmlElement(name="matrices_serialised")
	public ArrayList<String> getMeasurementMatricesSerialised() { return this.matrices_serialised; }
	public void setMeasurementMatricesSerialised(ArrayList<String> matrices_serialised) { this.matrices_serialised = matrices_serialised; }
	public void addMeasurementMatrixSerialised(String measurements) { this.matrices_serialised.add(measurements); }

	public void serialiseMatrices() {
		// prepare for JAXB transfer by serialising the UJMP matrices and later ignoring the Matrix objects in JAXB via @XmlTransient
		ListIterator<Matrix> li = this.matrices.listIterator();

		while(li.hasNext()) {
			try {
				byte[] byte_arr = serialize(li.next());
				this.matrices_serialised.add(javax.xml.bind.DatatypeConverter.printHexBinary(byte_arr));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void deserialiseMatrices() {
		// transform from HEX back to Matrix objects

		ListIterator<String> li = this.matrices_serialised.listIterator();

		while(li.hasNext()) {
			try {
				byte[] b = javax.xml.bind.DatatypeConverter.parseHexBinary(li.next());
				addMeasurementMatrix((Matrix) SerializationUtil.deserialize(b));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

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
