package ch.specchio.types;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.xml.bind.annotation.*;


/**
 * A single spectrum.
 */
@XmlRootElement(name="spectrum")
@XmlSeeAlso({ArrayList.class,SerialisableBufferedImage.class})
public class Spectrum implements MetadataInterface
{
	public static final String INSTRUMENT = "instrument";
	public static final String SENSOR = "sensor";
	public static final String CALIBRATION = "calibration"; // not yet used: calibrations do not have a useful name to be displayed
	public static final String MEASUREMENT_UNIT = "measurement_unit";
	public static final String FILE_FORMAT = "file_format";
	public static final String REFERENCE = "reference";
	
	/** spectrum metadata fields */
	public static final String[] METADATA_FIELDS = {
		SENSOR,
		FILE_FORMAT,
		INSTRUMENT, 
		MEASUREMENT_UNIT,
		REFERENCE,
		CALIBRATION
	};

	MetaDatatype<String> file_format;
	MetaDatatype<String> measurement_unit;
	Metadata smd;
	
	MetaDatatype<ArrayList<Integer>> pictures;

	Float[] measurement_vector = null;
	
	public int spectrum_id;
	int position_id;
	public int sensor_id;
	public int campaign_id;
	int landcover_id;
	int environmental_condition_id;
	int sampling_geometry_id;
	public int measurement_unit_id;
	public int measurement_type_id; 
	int sampling_env_id;
	public int instrument_id;
	int quality_level_id;
	int required_quality_level_id;
	public int file_format_id;
	public int reference_id;
	int hierarchy_level_id;
	public int calibration_id;
	
	private Sensor sensor;
	Instrument instrument;
	
	String capt_datetime = "";
	String load_datetime = "";	
	
	float stddev = -1;
	float mean;
	
	float vis_nir_stddev = -1;
	float vis_nir_mean;
	
	
	int report_attr_insert_pos;
	
	int output_timeformat;
	
	public Spectrum() {
		
	}
	
	
	public Spectrum(int spectrum_id) {
		
		this.spectrum_id = spectrum_id;
		
	}
	

	
	@XmlElement(name="campaign_id")
	public int getCampaignId() { return this.campaign_id; }
	public void setCampaignId(int campaign_id) { this.campaign_id = campaign_id; }

	@XmlElement(name="calibration_id")
	public int getCalibrationId() { return this.calibration_id; }
	public void setCalibrationId(int calibration_id) { this.calibration_id = calibration_id; }	
	
	public MetaDatatype<Integer> getCalibration() { return new MetaDatatype<Integer>("Calibration ID", calibration_id); }
	
	@XmlElement(name="eav_metadata")
	public Metadata getEavMetadata() { return this.smd; }
	public void setEavMetadata(Metadata smd) { this.smd = smd; }

	@XmlElement(name="file_format")
	public MetaDatatype<String> getFileFormat() { return this.file_format; }
	public void setFileFormat(MetaDatatype<String> file_format) { this.file_format = file_format; }
	
	@XmlElement(name="file_format_id")
	public int getFileFormatId() { return this.file_format_id; }
	public void setFileFormatId(int file_format_id) { this.file_format_id = file_format_id; }

	
	@XmlElement(name="hierarchy_level_id")
	public int getHierarchyLevelId() { return this.hierarchy_level_id; }
	public void setHierarchyLevelId(int hierarchy_level_id) { this.hierarchy_level_id = hierarchy_level_id; }
	
	@XmlElement(name="instrument")
	public Instrument getInstrument() { return this.instrument; }
	public void setInstrument(Instrument instrument) { this.instrument = instrument; }
	
	@XmlElement(name="instrument_id")
	public int getInstrumentId() { return this.instrument_id; }
	public void setInstrumentId(int instrument_id) { this.instrument_id = instrument_id; }
	
	@XmlElement(name="measurement_vector")
	public Float[] getMeasurementVector() {
		return measurement_vector;
	}
	public void setMeasurementVector(Float[] measurement_vector) {
		this.measurement_vector = measurement_vector;
	}	
	

	@XmlElement(name="measurement_type_id")
	public int getMeasurementTypeId() { return this.measurement_type_id; }
	public void setMeasurementTypeId(int measurement_type_id) { this.measurement_type_id = measurement_type_id; }
	
	@XmlElement(name="measurement_unit")
	public MetaDatatype<String> getMeasurementUnit() { return this.measurement_unit; }
	public void setMeasurementUnit(MetaDatatype<String> measurement_unit) { this.measurement_unit = measurement_unit; }
	
	@XmlElement(name="measurement_unit_id")
	public int getMeasurementUnitId() { return this.measurement_unit_id; }
	public void setMeasurementUnitId(int measurement_unit_id) { this.measurement_unit_id = measurement_unit_id; }
	
	@XmlElement(name="reference_id")
	public int getReferenceId() { return reference_id;}
	public void setReferenceId(int reference_id) { this.reference_id = reference_id;}

	@XmlElement(name="sampling_env_id")
	public int getSamplingEnvironmentId() { return this.sampling_env_id; }
	public void setSamplingEnvironmentId(int sampling_env_id) { this.sampling_env_id = sampling_env_id; }
	
	@XmlElement(name="sampling_geometry_id")
	public int getSamplingGeometryId() { return this.sampling_geometry_id; }
	public void setSamplingGeometryId(int sampling_geometry_id) { this.sampling_geometry_id = sampling_geometry_id; }
	
	@XmlElement(name="sensor")
	public Sensor getSensor() { return this.sensor; }
	public void setSensor(Sensor sensor) { this.sensor = sensor; }

	@XmlElement(name="sensor_id")
	public int getSensorId() { return this.sensor_id; }
	public void setSensorId(int sensor_id) { this.sensor_id = sensor_id; }
	
	@XmlElement(name="smd")
	public Metadata getMetadata() { return this.smd; }
	public void setMetadata(Metadata smd) { this.smd = smd; }
	
	@XmlElement(name="spectrum_id")
	public int getSpectrumId() { return this.spectrum_id; }
	public void setSpectrumId(int spectrum_id) { this.spectrum_id = spectrum_id; }

	/**
	 * Convert an SQL-style field name into a Java-style getter or setter name
	 * 
	 * @param prefix	"get" or "set"
	 * @param fieldname	the field name
	 *
	 * @return a string consisting of the prefix, followed by the field name in camel case
	 */
	private String getAccessorName(String prefix, String fieldname) {
		
		// start with the prefix
		StringBuffer sbuf = new StringBuffer();
		sbuf.append(prefix);
		
		// split the fieldname on underscores
		String parts[] = fieldname.split("_");
		
		// convert each part into camel case
		for (String part : parts) {
			if (part.length() > 0) {
				sbuf.append(part.substring(0, 1).toUpperCase());
				if (part.length() > 1) {
					sbuf.append(part.substring(1));
				}
			}
		}
		
		return sbuf.toString();
		
	}
	
	
	/**
	 * Generic metadata identifier getter.
	 * 
	 * @param field	a field name from Spectrum.METADATA_FIELDS
	 * 
	 * @return the id assigned to this field
	 * 
	 * @throws NoSuchMethodException	the field does not exist
	 */
	public int getMetadataId(String field) throws NoSuchMethodException {
		
		Integer id = 0;
		
		try {
			
			// get getter method name
			Method getter = getClass().getMethod(getAccessorName("get", field) + "Id");
			
			// invoke the method
			id = (Integer)getter.invoke(this);
			
		} catch (IllegalArgumentException ex) {
			// should never happen because we asked for the correct method parameters
			ex.printStackTrace();
		} catch (IllegalAccessException ex) {
			// should never happen because getters are always public
			ex.printStackTrace();
		} catch (InvocationTargetException ex) {
			// should never happen because getters never throw exceptions
			ex.printStackTrace();
		} catch (SecurityException ex) {
			// should never happen because getters are always public
			ex.printStackTrace();
		}
		
		return id;
		
	}
	
	
	/**
	 * Generic metadata value getter.
	 * 
	 * @param field	a field name from Spectrum.METADATA_FIELDS
	 * 
	 * @return the value of this field, or null if it is not set
	 * 
	 * @throws NoSuchMethodException	the field does not exist
	 */
	public Object getMetadataValue(String field) throws NoSuchMethodException {
		
		Object value = null;
		
		try {
			
			// get the getter name
			Method getter = getClass().getMethod(getAccessorName("get", field));
			
			// invoke the method
			value = getter.invoke(this);
			
		} catch (IllegalArgumentException ex) {
			// should never happen because we asked for the correct method parameters
			ex.printStackTrace();
		} catch (IllegalAccessException ex) {
			// should never happen because getters are always public
			ex.printStackTrace();
		} catch (InvocationTargetException ex) {
			// should never happen because getters never throw exceptions
			ex.printStackTrace();
		} catch (SecurityException ex) {
			// should never happen because getters are always public
			ex.printStackTrace();
		}
		
		return value;
		
	}


	public int getId() {
		return this.spectrum_id;
	}
	
	
	/**
	 * Generic metadata identifier setter.
	 * 
	 * @param field	a field name from Spectrum.METADATA_FIELDS
	 * @param id	the id to assign to the field
	 * 
	 * @throws NoSuchMethodException	the field does not exist
	 */
	public void setMetadataId(String field, int id) throws NoSuchMethodException {

		try {
			
			// get setter method name
			Method setter = getClass().getMethod(getAccessorName("set", field) + "Id", Integer.TYPE);
			
			// invoke the method
			setter.invoke(this, new Integer(id));
			
		} catch (IllegalArgumentException ex) {
			// should never happen because we asked for the correct method parameters
			ex.printStackTrace();
		} catch (IllegalAccessException ex) {
			// should never happen because setters are always public
			ex.printStackTrace();
		} catch (InvocationTargetException ex) {
			// should never happen because setters never throw exceptions
			ex.printStackTrace();
		} catch (SecurityException ex) {
			// should never happen because setters are always public
			ex.printStackTrace();
		}
		
	}

	
}



