package ch.specchio.types;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.joda.time.DateTime;

import ch.specchio.jaxb.XmlDateAdapter;
//import ch.specchio.jaxb.XmlIntegerAdapter;
import ch.specchio.jaxb.XmlDateTimeAdapter;


/**
 * This class represents an instrument calibration.
 */
@XmlRootElement(name="calibration")
public class Calibration {
	
	public static final int SPECTRAL_CALIBRATION = 0;
	public static final int RADIOMETRIC_CALIBRATION = 1;	
	
	private boolean includes_uncertainty = true;
	private int reference_id; 
	private int instrument_id; 
	private SpectralFile spec_file = null; // used for loading calibration data from spectral files
	private int calibration_number;
	private int calibration_type;
	
	private DateTime calibration_date;
	protected Integer calibration_id;
	protected int cal_factors_id;
	protected int uncertainty_id;		
	//private int calibration_no;
	private String name = "";
	private String comments;	
	private double[] factors;
	private double[] uncertainty;
	private int measurement_unit_id;
	private int field_of_view; // indicates the FOV in degrees in case there is a FOV dependency

	private Metadata md;
	
	public Calibration() {
		calibration_id = 0;
	}
	
	
	@XmlElement(name="includes_uncertainty")
	public boolean getIncludesUncertainty() { return this.includes_uncertainty; }
	public void setIncludesUncertainty(boolean includes_uncertainty) { this.includes_uncertainty = includes_uncertainty; }
	
	@XmlElement(name="reference_id")
	public int getReferenceId() { return this.reference_id; }
	public void setReferenceId(int reference_id) { this.reference_id = reference_id; }
	
	@XmlElement(name="spec_file")
	public SpectralFile getSpectralFile() { return this.spec_file; }
	public void setSpectralFile(SpectralFile spec_file) { this.spec_file = spec_file; }
	
	@XmlElement(name="instrument_id")
	public int getInstrumentId() { return this.instrument_id; }
	public void setInstrumentId(int instrument_id) { this.instrument_id = instrument_id; }

	@XmlElement(name="comments")
	public String getComments() {
		return comments; }
	public void setComments(String comments) {
		this.comments = comments; }

	@XmlElement(name="calibration_number")
	public int getCalibrationNumber() {
		return calibration_number; }
	public void setCalibrationNumber(int calibration_number) {
		this.calibration_number = calibration_number; }

	@XmlElement(name="calibration_id")
	public int getCalibration_id() {
		return calibration_id; }
	public void setCalibration_id(int calibration_id) {
		this.calibration_id = calibration_id; }
	
	@XmlElement(name="calibration_date")
	@XmlJavaTypeAdapter(XmlDateTimeAdapter.class)
	public DateTime getCalibrationDate() { return this.calibration_date; }
	public void setCalibrationDate(DateTime date) { this.calibration_date = date; }
	
	@XmlElement(name="calibration_id")
	public Integer getCalibrationId() { return this.calibration_id; }
	public void setCalibrationId(Integer calibration_id) { this.calibration_id = calibration_id; }
	
//	@XmlElement(name="calibration_no")
//	public int getCalibrationNumber() { return this.calibration_no; }
//	public void setCalibrationNumber(int calibration_no) { this.calibration_no = calibration_no; }
	
	@XmlElement(name="uncertainty_id")
	public int getUncertainty_id() {
		return uncertainty_id;
	}
	public void setUncertainty_id(int uncertainty_id) {
		this.uncertainty_id = uncertainty_id;
	}
	
	@XmlElement(name="cal_factors_id")
	public int getCalFactorsId() {
		return cal_factors_id;
	}
	public void setCalFactorsId(int cal_factors_id) {
		this.cal_factors_id = cal_factors_id;	
	}

	@XmlElement(name="factors")
	public double[] getFactors() {
		return factors;
	}
	public void setFactors(double[] factors) {
		this.factors = factors;
	}

	public Float[] getFactorsAsFloat() {

		Float[] floatArray = new Float[factors.length];
		for (int i = 0 ; i < factors.length; i++)
		{
			floatArray[i] = (float) factors[i];
		}
		return floatArray;
	}

	@XmlElement(name="uncertainty")
	public double[] getUncertainty() {
		return uncertainty;
	}
	public void setUncertainty(double[] uncertainty) {
		this.uncertainty = uncertainty;
	}

	@XmlElement(name="measurement_unit_id")
	public int getMeasurement_unit_id() {
		return measurement_unit_id;
	}
	public void setMeasurement_unit_id(int measurement_unit_id) {
		this.measurement_unit_id = measurement_unit_id;
	}

	@XmlElement(name="md")
	public Metadata getMetadata() {
		return md;
	}

	public void setMetadata(Metadata md) {
		this.md = md;
	}
	

	public int getField_of_view() {
		return field_of_view;
	}
	public void setField_of_view(int field_of_view) {
		this.field_of_view = field_of_view;
	}	
	
	
	
	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public InputStream getFactorsInputStream()
	{
		return getInputStream(this.factors);
	}
	
	public InputStream getUncertaintyInputStream()
	{
		return getInputStream(this.getUncertainty());
	}	
	
	
	/**
	 * Write the floats into a ByteArrayOutputStream, store as byte array,
	 * the open as ByteArrayInputStream to use in an SQL statement
	 */
	private InputStream getInputStream(double[] data) {
		byte[] temp_buf;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutput dos = new DataOutputStream(baos);


			for (int i = 0; i < data.length; i++) {
				try {
					dos.writeFloat((float) data[i]);
				} catch (IOException e) {
					e.printStackTrace();
				}
				catch (NullPointerException e){
					//temp_buf = new byte[no_of_channels.get(0)];
					
//					// log the error
//					file_errors.add(new SpecchioMessage("Spectrum contains null values.", SpecchioMessage.ERROR));
					
					break;
				}
			}

		temp_buf = baos.toByteArray();

		InputStream refl = new ByteArrayInputStream(temp_buf);

		return refl;
	}


	public int getCalibration_type() {
		return calibration_type;
	}


	public void setCalibration_type(int calibration_type) {
		this.calibration_type = calibration_type;
	}



}
